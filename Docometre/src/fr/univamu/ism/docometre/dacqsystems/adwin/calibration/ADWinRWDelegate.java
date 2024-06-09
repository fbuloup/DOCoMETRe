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
package fr.univamu.ism.docometre.dacqsystems.adwin.calibration;

import de.adwin.driver.ADwinCommunicationError;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinChannel;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;

public class ADWinRWDelegate {
	
	private static int moduleNumber;
	private static int channelNumber;
	private static ADWinDACQConfiguration adwinDacqConfiguration;
	
	private static void update(Channel channel) {
		moduleNumber = Integer.parseInt(channel.getModule().getProperty(ADWinModuleProperties.MODULE_NUMBER));
		channelNumber = Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER));
		adwinDacqConfiguration = (ADWinDACQConfiguration) channel.getModule().getDACQConfiguration();
	}
	
	public static double getAnalogValue(Channel channel) throws ADwinCommunicationError {
		update(channel);
		adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
		int gain = Integer.parseInt(channel.getProperty(ADWinAnInChannelProperties.GAIN));
		adwinDacqConfiguration.getADwinDevice().Set_Par(14, gain);
		adwinDacqConfiguration.getADwinDevice().Set_Par(1, channelNumber);
		do {
		} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(1) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
		double value = adwinDacqConfiguration.getADwinDevice().Get_Par(2);
		value = ADWinAnInChannelProperties.toVolt((ADWinChannel) channel, value);
		return value;
	}

	public static void putAnalogValue(Channel channel, double value) throws ADwinCommunicationError {
		update(channel);
		int digitValue = ADWinAnOutChannelProperties.toDigit((ADWinChannel) channel, value);
		adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
		adwinDacqConfiguration.getADwinDevice().Set_Par(6, digitValue);
		adwinDacqConfiguration.getADwinDevice().Set_Par(5, channelNumber);
		do {
		} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(5) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
	}
	
	public static int getDigitalValue(Channel channel) throws ADwinCommunicationError {
		update(channel);
		adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
		adwinDacqConfiguration.getADwinDevice().Set_Par(3, (int) Math.pow(2, channelNumber - 1));
		do {
		} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(3) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
		return adwinDacqConfiguration.getADwinDevice().Get_Par(4) >= 1 ? 1 : 0;
	}
	
	public static void putDigitalValue(Channel channel, int value) throws ADwinCommunicationError {
		update(channel);
		adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
		adwinDacqConfiguration.getADwinDevice().Set_Par(8, value);
		adwinDacqConfiguration.getADwinDevice().Set_Par(7, channelNumber-1);
		do {
		} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(7) != -1) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
	}

}
