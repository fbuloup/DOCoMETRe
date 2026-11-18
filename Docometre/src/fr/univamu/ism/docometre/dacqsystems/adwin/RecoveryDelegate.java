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

import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.TrialTimings;

@SuppressWarnings("restriction")
public class RecoveryDelegate {
	
	public static void recover(Channel[] channels, Module module, ADWinProcess process) {
		if(channels != null && module != null) {
			Activator.logErrorMessage("Cannot recover channels and channels' module at same time !");
			return;
		}
		ADWinDACQConfiguration adWinDAQGeneralConfiguration = (ADWinDACQConfiguration) process.getDACQConfiguration();
		ADwinDevice adwinDevice = adWinDAQGeneralConfiguration.getADwinDevice();
		int processNumber = Integer.parseInt( process.getProperty(ADWinProcessProperties.PROCESS_NUMBER));	
		
		if(channels == null) channels = module.getChannels();
		
		for (Channel channel : channels) {
			ADWinChannel adwinChannel = (ADWinChannel)channel;
		
//		for (int j = 0; j < module.getChannelsNumber(); j++) {
//			ADWinChannel channel = (ADWinChannel)module.getChannel(j);
			// Channels read from the single transfer FIFO (see ADWinTransferFifo) are recovered by ADWinProcess.recovery()
			if(adwinChannel.isRecoveryAllowed() && !process.isReadFromTransferFifo(adwinChannel)) {
				int transferNum = adwinChannel.getTransferNumber();
				try {
					long start = TrialTimings.partStart();
					int nbData = adwinDevice.Fifo_Full(transferNum);
					TrialTimings.loopPart(TrialTimings.LoopPart.FIFO_FULL, start);
					if (nbData > 0){
						TrialTimings.dataReceived(channel.getProperty(ChannelProperties.NAME), module instanceof ADWinCANModule);
						start = TrialTimings.partStart();
						float[] signal = process.getData(transferNum, nbData);
						TrialTimings.getFifo(start, nbData);
						start = TrialTimings.partStart();
						channel.addSamples(signal);
						TrialTimings.loopPart(TrialTimings.LoopPart.ADD_SAMPLES, start);
						process.diaryRecovered(channel.getProperty(ChannelProperties.NAME), nbData);
						checkDataLoss(process, adwinDevice, processNumber, channel, transferNum);
					}
				} catch (ADwinCommunicationError e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				}
			}
		}
	}
	
	/**
	 * Reports a data loss for a channel if its counter (PAR_<transfer number>, read with all
	 * the others at the beginning of the iteration, see ADWinProcess.recovery()) is not null,
	 * and resets this counter.
	 */
	static void checkDataLoss(ADWinProcess process, ADwinDevice adwinDevice, int processNumber, Channel channel, int transferNum) throws ADwinCommunicationError {
		if(process.getDataLossCounter(transferNum) <= 0) return;
		float real_time = adwinDevice.Get_FPar(processNumber);
		String realTime = String.valueOf(real_time);
		process.appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_DataLoss, new Object[] {transferNum, channel.getProperty(ChannelProperties.NAME), realTime}));
		process.appendErrorMarkerAtCurrentDiaryLine(ADWinMessages.ADWinDiary_TryIncreaseBufferSize);
		adwinDevice.Set_Par(transferNum,0);
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
				statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), ADWinMessages.ProcessDataLoss_Label);
			}
		});
	}

}
