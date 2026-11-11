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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.Platform;

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;

/**
 * Upload of a compiled sketch on the Arduino Uno board, with avrdude or arduino-cli as configured
 * in the DACQ configuration : used to load the acquisition processes and the calibration firmware.
 */
public final class ArduinoUnoUploadDelegate {
	
	private ArduinoUnoUploadDelegate() {
	}
	
	/**
	 * @param dacqConfiguration the DACQ configuration (upload tool and its path, revision of the board)
	 * @param devicePath the serial port of the board
	 * @param binaryFilePath the file to upload : .hex file (R3), .bin file (R4 WiFi, arduino-cli only)
	 * @return the upload command line
	 */
	public static String getUploadCommand(DACQConfiguration dacqConfiguration, String devicePath, String binaryFilePath) {
		boolean useCLI = "true".equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI));
		if(!useCLI) {
			String avrDudePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH);
			String confFilePath = "";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = avrDudePath.replaceAll("avrdude.exe$", "");
			else confFilePath = avrDudePath.replaceAll("avrdude$", "");
			confFilePath = confFilePath + ".." + File.separator + "etc" + File.separator + "avrdude.conf";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = "\"" + confFilePath + "\"";
			return avrDudePath + " -C " + confFilePath +  " -p m328p -c arduino -P " + devicePath + " -U flash:w:" + binaryFilePath + ":i"; 
		}
		String arduinoUnoRelease = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		String fullQualifiedBoardName = "arduino:avr:uno";
		if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi";
		String arduinoCLIPATH = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
		return arduinoCLIPATH + " upload -p " + devicePath + " --fqbn " + fullQualifiedBoardName + " -i " + binaryFilePath;
	}
	
	/**
	 * Writes the upload command in an executable script (uploadSketch.bat or uploadSketch.sh)
	 * @param folder the folder of the script
	 * @return the path of the script
	 */
	public static String createUploadScript(String folder, String command) throws IOException, InterruptedException {
		// We need to create a bash file in order to launch Arduino uno's upload
		String bashFilePath = folder + File.separator;
		if(Platform.getOS().equals(Platform.OS_WIN32)) bashFilePath = bashFilePath + "uploadSketch.bat";
		if(Platform.getOS().equals(Platform.OS_MACOSX)) bashFilePath = bashFilePath + "uploadSketch.sh";
		if(Platform.getOS().equals(Platform.OS_LINUX)) bashFilePath = bashFilePath + "uploadSketch.sh";
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		fileWriter.write(command);
		fileWriter.close();
		if(!Platform.getOS().equals(Platform.OS_WIN32)) {
			// This bash file must be executable 
			java.lang.Process process = Runtime.getRuntime().exec(new String[]{"chmod", "777", bashFilePath});
			process.waitFor();
		}
		return bashFilePath;
	}
	
	/**
	 * Runs an upload script (see {@link #createUploadScript(String, String)})
	 * @return the error output of the upload tool, empty if there is none
	 */
	public static String runUploadScript(String scriptPath) throws IOException, InterruptedException {
		java.lang.Process processCMD = Runtime.getRuntime().exec(new String[] {scriptPath});
		// Error output read before waiting for the end : the tool could be blocked by a full pipe
		StringBuilder message = new StringBuilder();
		try (BufferedReader error = new BufferedReader(new InputStreamReader(processCMD.getErrorStream()))) {
			String line;
			while((line = error.readLine()) != null) message.append(line).append("\n");
		}
		processCMD.waitFor();
		return message.toString();
	}
	
	/**
	 * @param errorOutput the error output of the upload tool
	 * @return true if the upload failed
	 */
	public static boolean hasFailed(String errorOutput) {
		return errorOutput.toLowerCase().contains("error");
	}

}
