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
import java.util.concurrent.TimeUnit;

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
	 * @param binaryFilePath the file to upload : .hex file (R3), .bin file (R4 WiFi and Portenta Machine Control,
	 * arduino-cli only)
	 * @return the upload command line
	 */
	public static String getUploadCommand(DACQConfiguration dacqConfiguration, String devicePath, String binaryFilePath) {
		String arduinoUnoRelease = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean useCLI = "true".equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI));
		// Portenta Machine Control : arduino-cli only (avrdude uploads to AVR microcontrollers)
		if(!useCLI && !ArduinoUnoDACQConfigurationProperties.isPMC(arduinoUnoRelease)) {
			String avrDudePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH);
			String confFilePath = "";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = avrDudePath.replaceAll("avrdude.exe$", "");
			else confFilePath = avrDudePath.replaceAll("avrdude$", "");
			confFilePath = confFilePath + ".." + File.separator + "etc" + File.separator + "avrdude.conf";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = "\"" + confFilePath + "\"";
			return avrDudePath + " -C " + confFilePath +  " -p m328p -c arduino -P " + devicePath + " -U flash:w:" + binaryFilePath + ":i"; 
		}
		String fullQualifiedBoardName = ArduinoUnoDACQConfigurationProperties.getFullyQualifiedBoardName(arduinoUnoRelease);
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
	 * Maximum duration of an upload : the upload tool is stopped after it
	 */
	public static final long UPLOAD_TIMEOUT_MS = 120000;
	
	/**
	 * Result of an upload script : exit code of the script (exit code of the upload tool), error output of the upload
	 * tool, and whether the upload has been stopped because it lasted too long
	 */
	public static final class UploadResult {
		private final int exitCode;
		private final String errorOutput;
		private final boolean timedOut;
		
		public UploadResult(int exitCode, String errorOutput, boolean timedOut) {
			this.exitCode = exitCode;
			this.errorOutput = errorOutput;
			this.timedOut = timedOut;
		}
		
		public int getExitCode() {
			return exitCode;
		}
		
		public String getErrorOutput() {
			return errorOutput;
		}
		
		public boolean isTimedOut() {
			return timedOut;
		}
		
		/**
		 * @param exitCodeOnly true if the error output is not used : dfu-util (Portenta Machine Control) writes "Error
		 * during download get_status" when the board restarts at the end of a successful upload
		 * @return true if the upload failed : time out, exit code not 0 or, if not exitCodeOnly, "error" in the error output
		 */
		public boolean hasFailed(boolean exitCodeOnly) {
			if(timedOut || exitCode != 0) return true;
			return !exitCodeOnly && ArduinoUnoUploadDelegate.hasFailed(errorOutput);
		}
	}
	
	/**
	 * Runs an upload script (see {@link #createUploadScript(String, String)}). The standard output of the upload tool
	 * (progress of dfu-util, bossac...) is written in uploadOutput.txt, next to the script, and its error output is read
	 * while it runs : a full pipe, never read, would block the tool. The tool is stopped after timeOutMs.
	 */
	public static UploadResult runUpload(String scriptPath, long timeOutMs) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);
		processBuilder.redirectOutput(new File(new File(scriptPath).getParentFile(), "uploadOutput.txt"));
		java.lang.Process process = processBuilder.start();
		StringBuilder message = new StringBuilder();
		Thread errorReader = new Thread(() -> {
			try (BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while((line = error.readLine()) != null) {
					synchronized (message) {
						message.append(line).append("\n");
					}
				}
			} catch (IOException e) {
				// Stream closed : the tool has been stopped
			}
		}, "Upload error output");
		errorReader.setDaemon(true);
		errorReader.start();
		boolean finished = process.waitFor(timeOutMs, TimeUnit.MILLISECONDS);
		if(!finished) {
			// The script runs the upload tool (arduino-cli, avrdude) : both are stopped
			process.descendants().forEach(ProcessHandle::destroyForcibly);
			process.destroyForcibly();
		}
		errorReader.join(1000);
		synchronized (message) {
			return new UploadResult(finished ? process.exitValue() : -1, message.toString(), !finished);
		}
	}
	
	/**
	 * Runs an upload script (see {@link #runUpload(String, long)}, time out of {@link #UPLOAD_TIMEOUT_MS})
	 * @return the error output of the upload tool, empty if there is none
	 */
	public static String runUploadScript(String scriptPath) throws IOException, InterruptedException {
		return runUpload(scriptPath, UPLOAD_TIMEOUT_MS).getErrorOutput();
	}
	
	/**
	 * @param errorOutput the error output of the upload tool
	 * @return true if the upload failed
	 */
	public static boolean hasFailed(String errorOutput) {
		return errorOutput.toLowerCase().contains("error");
	}

}
