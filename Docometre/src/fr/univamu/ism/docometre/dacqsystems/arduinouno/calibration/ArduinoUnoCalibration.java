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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.calibration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.widgets.CalibrationHeader;
import fr.univamu.ism.docometre.widgets.CalibrationListener;

/**
 * Calibration of the analog inputs of Arduino Uno systems (analog inputs of the board and ADS1115 inputs) : same
 * calibration values as ADwin analog inputs (amplitudes in volts and corresponding values in unit). The values
 * are captured from the generic calibration firmware (see ArduinoUnoCalibrationTask).
 */
public final class ArduinoUnoCalibration {
	
	/**
	 * Folder of the calibration firmware in the Libraries bundle
	 */
	private static final String FIRMWARE_FOLDER = "includes/ArduinoUnoCalibrationFiles/";
	
	// Last value received for each input, in volts (see ArduinoUnoCalibrationTask), used to capture amplitudes
	private static final Map<Channel, Double> lastValues = new ConcurrentHashMap<>();
	
	private ArduinoUnoCalibration() {
	}
	
	public static CalibrationHeader createHeader(Composite container, Channel channel) {
		double uMax = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX));
		double uMin = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN));
		double ampMax = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX));
		double ampMin = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN));
		CalibrationHeader calibrationHeader = new CalibrationHeader(container, new double[] {ampMin, ampMax, uMin, uMax});
		calibrationHeader.addAMaxListener(new ChannelCalibrationListener(channel, ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX));
		calibrationHeader.addAMinListener(new ChannelCalibrationListener(channel, ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN));
		calibrationHeader.addUMaxListener(new ChannelCalibrationListener(channel, ArduinoUnoAnInChannelProperties.UNIT_MAX));
		calibrationHeader.addUMinListener(new ChannelCalibrationListener(channel, ArduinoUnoAnInChannelProperties.UNIT_MIN));
		return calibrationHeader;
	}
	
	/*
	 * Writes a calibration value in the channel (DACQ configuration saved), current value of the input for captures
	 */
	private static final class ChannelCalibrationListener implements CalibrationListener {
		
		private Channel channel;
		private Property property;
		
		public ChannelCalibrationListener(Channel channel, Property property) {
			this.channel = channel;
			this.property = property;
		}
		
		@Override
		public void push(double value) {
			channel.setProperty(property, Double.toString(value));
			ObjectsController.serialize(channel.getModule().getDACQConfiguration());
		}
		
		@Override
		public double get() throws Exception {
			Double value = lastValues.get(channel);
			if(value == null) throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_NoValue, channel.getProperty(ChannelProperties.NAME)));
			return value;
		}
		
	}
	
	public static double computeCalibration(double y, Channel channel) {
		double uMax = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX));
		double uMin = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN));
		double ampMax = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX));
		double ampMin = Double.parseDouble(channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN));
		return (uMax - uMin)/(ampMax - ampMin)*(y - ampMin) + uMin;
	}
	
	static void setLastValue(Channel channel, double value) {
		lastValues.put(channel, value);
	}
	
	static void clearLastValues() {
		lastValues.clear();
	}
	
	/**
	 * @param board ArduinoUnoCalibrationProtocol.BOARD_R3 or BOARD_R4
	 * @return a copy of the calibration firmware of this board in the state folder of DOCoMETRe (path without
	 * the folders of the installation, which can contain spaces)
	 */
	static File getFirmwareFile(String board) throws IOException {
		String fileName = ArduinoUnoCalibrationProtocol.BOARD_R4.equals(board) ? "ArduinoUnoCalibration_R4WiFi.bin" : "ArduinoUnoCalibration_R3.hex";
		Bundle librariesBundle = Platform.getBundle("Libraries");
		URL url = librariesBundle == null ? null : librariesBundle.getEntry(FIRMWARE_FOLDER + fileName);
		if(url == null) throw new IOException(NLS.bind(ArduinoUnoMessages.Calibration_FirmwareNotFound, FIRMWARE_FOLDER + fileName));
		File firmwareFile = new File(FileLocator.toFileURL(url).getPath());
		File copy = new File(getWorkingFolder(), fileName);
		Files.copy(firmwareFile.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return copy;
	}
	
	/**
	 * @return the folder of the firmware copy and of the upload script
	 */
	static String getWorkingFolder() {
		File folder = new File(Activator.getDefault().getStateLocation().toOSString(), "ArduinoUnoCalibration");
		folder.mkdirs();
		return folder.getAbsolutePath();
	}
	
}
