/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.adwin;

import org.eclipse.osgi.util.NLS;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;

public class GenerationDelegate {
	
	/**
	 * Sends the next samples of each stimulus to its generation FIFO.
	 * <ul>
	 * <li>The FIFO is refilled only when about a quarter of it has been consumed since the previous
	 * refill (see ADWinChannel.isGenerationRefillDue()), not at each iteration of the real-time loop :
	 * a refill costs about four network round trips with ADwin (Fifo_Empty, SetFifo_Float).</li>
	 * <li>The underflow counter of the stimulus (PAR_<transfer number>, see STIMULUS.FUN) is read with
	 * all the data loss counters at the beginning of the iteration (see ADWinProcess.recovery()) :
	 * checked at each iteration without any request.</li>
	 * <li>The charts are given the samples played by ADwin since the start of the process (elapsed time
	 * multiplied by the actual frequency of the stimulus), at each iteration and until the end of the
	 * stimulus : the display does not depend on the refills.</li>
	 * </ul>
	 */
	public static void generate(Channel[] channels, Module module, ADWinProcess process) {
		if(channels != null && module != null) {
			Activator.logErrorMessage("Cannot generate channels and channels' module at same time !");
			return;
		}
		
		ADWinDACQConfiguration adWinDAQGeneralConfiguration = (ADWinDACQConfiguration) process.getDACQConfiguration();
		ADwinDevice adwinDevice = adWinDAQGeneralConfiguration.getADwinDevice();
		int processNumber = Integer.parseInt( process.getProperty(ADWinProcessProperties.PROCESS_NUMBER));	
		
		if(module != null) channels = module.getChannels();
		
		long now = System.nanoTime();
		for (Channel channel : channels) {
			ADWinChannel adwinChannel = (ADWinChannel) channel;
			if(!adwinChannel.isGenerationAllowed()) continue;
			// Display : samples played since the start of the process (all the samples sent if the
			// frequency is unknown), also after the last refill, until the end of the stimulus
			double frequency = adwinChannel.getGenerationFrequency();
			long playedSamples = frequency > 0 ? (long) ((now - process.getStartProcessNanos()) / 1e9 * frequency) + 1 : Long.MAX_VALUE;
			adwinChannel.notifyPlayedStimulusSamples(playedSamples);
			// When all the samples of the stimulus file have been sent, nothing more to send (as before,
			// the underflows at the end of the stimulus are not reported)
			if(adwinChannel.isGenerationFinished()) continue;
			int transferNum = adwinChannel.getTransferNumber();
			String name = adwinChannel.getProperty(ChannelProperties.NAME);
			try {
				// Underflow of the generation FIFO : counter read with all the others, no request
				RecoveryDelegate.checkDataLoss(process, adwinDevice, processNumber, channel, transferNum);
				
				if(!adwinChannel.isGenerationRefillDue(now)) continue;
				int nbData = adwinDevice.Fifo_Empty(transferNum);
				if (nbData > 0){
					float[] data = adwinChannel.getSamples(nbData);
					if (data.length == 0) {
						adwinChannel.setGenerationFinished();
						// Written once per channel and per execution
						if(process.diaryNoMoreToGenerateFirstTime(name)) {
							process.appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_NoMoreToGenerate, name, adwinDevice.Fifo_Full(transferNum)));
						}
						continue;
					}
					adwinDevice.SetFifo_Float(transferNum, data, data.length);
					process.diaryGenerated(name, data.length);
				}
				adwinChannel.generationRefilled(now);
			} catch (ADwinCommunicationError e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}	
		
	}
	
}
