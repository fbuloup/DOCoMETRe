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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.Activator;

public class BootDelegate {
	
	public enum ADwinBootState { 
		NOT_RESPONDING, 
		NOT_BOOTED,
		WRONG_BTL_FOR_CPU, 
		BOOTED_UNKNOWN_BTL, 
		BOOTED_WITH_THIS_BTL, 
		WAS_ALREADY_BOOTED_UNKNOWN_BTL, 
		WAS_ALREADY_BOOTED_WITH_THIS_BTL
	}
	
	private static final int BOOT_SIGNATURE_PAR = 79;// par_80 is used with CAN module
	
	public static ADwinDevice adwinDevice;

	private static int processorFromBTLName(String btlPath) {
		Matcher m = Pattern.compile("adwin(\\d+)\\.btl$").matcher(new File(btlPath).getName().toLowerCase());
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}
	
	public static void bootAndSign(String btlPath) throws ADwinCommunicationError, IOException {
		ADWinProcess.previousADWINBinFilePath = "";
		adwinDevice.Boot(btlPath);
		adwinDevice.Set_Par(BOOT_SIGNATURE_PAR, btlSignature(btlPath));
	}

	private static int btlSignature(String btlPath) throws IOException {
		CRC32 crc = new CRC32();
		crc.update(Files.readAllBytes(Paths.get(btlPath)));
		int s = (int) crc.getValue();
		return s == 0 ? 1 : s;                                     // 0 = value after a fresh boot
	}
	
	private static ADwinBootState getBootState(ADwinDevice device, String btlPath, boolean firstAttempt) throws IOException {
		int expectedCPU = processorFromBTLName(btlPath);
		try {
			try {
				device.Test_Version();
				device.Processor_Type();
				if(device.Process_Status(1) == -1) return ADwinBootState.NOT_BOOTED;
			} catch (Exception e) {
				return ADwinBootState.NOT_BOOTED;
			}
			if (expectedCPU > 0 && device.Processor_Type() != expectedCPU) return ADwinBootState.WRONG_BTL_FOR_CPU;
			int signature = device.Get_Par(BOOT_SIGNATURE_PAR);
			if(!firstAttempt) return signature == btlSignature(btlPath) ? ADwinBootState.BOOTED_WITH_THIS_BTL : ADwinBootState.BOOTED_UNKNOWN_BTL;
			else return signature == btlSignature(btlPath) ? ADwinBootState.WAS_ALREADY_BOOTED_WITH_THIS_BTL : ADwinBootState.WAS_ALREADY_BOOTED_UNKNOWN_BTL;
		} catch (ADwinCommunicationError e) {
			Activator.logErrorMessageWithCause(e);
			return ADwinBootState.NOT_RESPONDING;
		}
	}
	
	public static ADwinBootState boot(ADWinDACQConfiguration adWinDACQConfiguration) throws ADwinCommunicationError, IOException {
		if(adwinDevice == null) {
			String IP = adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.IP_ADDRESS);
			int portNumber = Integer.valueOf(adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.PORT_NUMBER));
			adwinDevice = new ADwinDevice();
			adwinDevice.Remove_Entry(0x01);
			adwinDevice.Add_Entry(0x01, InetAddress.getByName(IP) , "", 1, portNumber);
			adwinDevice.Set_DeviceNo(0x01);
		}
		adWinDACQConfiguration.setADwinDevice(adwinDevice);
		String ADWinBTL = adWinDACQConfiguration.getProperty(ADWinDACQConfigurationProperties.BTL_FILE);
		ADwinBootState bootState = getBootState(adwinDevice, ADWinBTL, true);
		if(bootState == ADwinBootState.NOT_BOOTED) {
			bootAndSign(ADWinBTL);
			bootState = getBootState(adwinDevice, ADWinBTL, false);
		}
		return bootState;
	}

}
