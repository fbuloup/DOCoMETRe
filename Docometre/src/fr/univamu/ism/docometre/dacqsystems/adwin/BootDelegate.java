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

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;

public class BootDelegate {
	
	public static String ALREADY_BOOTED = "ALREADY_BOOTED";
	public static String BOOTED = "BOOTED";
	
	public static String boot(ADWinDACQConfiguration adWinDACQConfiguration) throws UnknownHostException, ADwinCommunicationError {
		
		String ADWinBTL = adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.BTL_FILE);
		String IP = adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.IP_ADDRESS);
		int portNumber = Integer.valueOf(adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.PORT_NUMBER));
		// If there is no adwin device create and configure it
		if (adWinDACQConfiguration.getADwinDevice() == null){
			adWinDACQConfiguration.setADwinDevice(new ADwinDevice());
			adWinDACQConfiguration.getADwinDevice().Remove_Entry(0x01);
			adWinDACQConfiguration.getADwinDevice().Add_Entry(0x01, InetAddress.getByName(IP) , "", 1, portNumber);
			adWinDACQConfiguration.getADwinDevice().Set_DeviceNo(0x01);
	    }
		
		// test if ADwin is already booted
		boolean isAdwinBooted;
		try {
			@SuppressWarnings("unused")// Test if a call to Get_FPar responds, if not it means that ADWin is not booted
			float time = adWinDACQConfiguration.getADwinDevice().Get_FPar(1);
			isAdwinBooted = true;
		} catch (Exception e) {
			isAdwinBooted = false;
		}
		if(!isAdwinBooted){
			adWinDACQConfiguration.getADwinDevice().Boot(ADWinBTL);
			return BOOTED;
			//appendToEventDiary(ADWinMessages.ADWinDiary_Booted);
		} else return ALREADY_BOOTED; 
			//appendToEventDiary(ADWinMessages.ADWinDiary_AlreadyBooted);
	}

}
