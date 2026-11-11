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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.calibration.AbstractCalibrateMonitorDialog;
import fr.univamu.ism.docometre.calibration.CalibrationFactory;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionDialog;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import fr.univamu.ism.docometre.widgets.ChannelViewer;
import jssc.SerialPortList;

/**
 * Calibration and monitoring dialog of Arduino Uno systems : used channels of the DACQ configuration
 * (analog inputs of the board, ADS1115 inputs, digital inputs ; analog and digital outputs). The
 * generic calibration firmware is uploaded on the board when it does not run it.
 */
public class ArduinoUnoCalibrateMonitorDialog extends AbstractCalibrateMonitorDialog {
	
	private ArduinoUnoDACQConfiguration dacqConfiguration;
	
	public ArduinoUnoCalibrateMonitorDialog(Shell parentShell, ArduinoUnoDACQConfiguration dacqConfiguration) {
		super(parentShell, Activator.ARDUINO_UNO_SYSTEM);
		this.dacqConfiguration = dacqConfiguration;
	}
	
	private static boolean isUsed(Channel channel) {
		return Boolean.valueOf(channel.getProperty(ArduinoUnoChannelProperties.USED));
	}
	
	private static boolean isDigitalOutput(Channel channel) {
		return ArduinoUnoDigInOutChannelProperties.OUTPUT.equals(channel.getProperty(ArduinoUnoDigInOutChannelProperties.IN_OUT));
	}
	
	@Override
	protected void populate(Composite inputsContainer, Composite outputsContainer) {
		Module[] modules = dacqConfiguration.getModules();
		// Analog inputs of the board, then ADS1115 inputs
		for (Module module : modules) 
			if(module instanceof ArduinoUnoAnInModule)
				for (Channel channel : module.getChannels()) if(isUsed(channel)) createChannelViewer(inputsContainer, channel, true, true);
		for (Module module : modules) 
			if(module instanceof ArduinoUnoADS1115Module)
				for (Channel channel : module.getChannels()) if(isUsed(channel)) createChannelViewer(inputsContainer, channel, true, true);
		// Digital inputs
		for (Module module : modules) 
			if(module instanceof ArduinoUnoDigInOutModule)
				for (Channel channel : module.getChannels()) if(isUsed(channel) && !isDigitalOutput(channel)) createChannelViewer(inputsContainer, channel, true, false);
		// Analog outputs (analogWrite values) : PWM outputs, and DAC of R4 WiFi (12 bits)
		boolean r4 = ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration);
		for (Module module : modules) 
			if(module instanceof ArduinoUnoAnOutModule)
				for (Channel channel : module.getChannels()) {
					boolean isDAC = ArduinoUnoAnOutModule.isDACChannel(channel);
					if(!isUsed(channel) || (isDAC && !r4)) continue;
					int maximum = isDAC ? ArduinoUnoCalibrationProtocol.DAC_OUTPUT_MAXIMUM : ArduinoUnoCalibrationProtocol.ANALOG_OUTPUT_MAXIMUM;
					createChannelViewer(outputsContainer, channel, false, true).setOutputRange(0, maximum);
				}
		// Digital outputs
		for (Module module : modules) 
			if(module instanceof ArduinoUnoDigInOutModule)
				for (Channel channel : module.getChannels()) if(isUsed(channel) && isDigitalOutput(channel)) createChannelViewer(outputsContainer, channel, false, false);
	}
	
	@Override
	protected void startCalibrationProcess() throws Exception {
		if(channelsViewers.isEmpty()) throw new Exception(ArduinoUnoMessages.Calibration_NoChannel);
		// Serial port of the board, chosen by the user if it is not connected
		String devicePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH);
		if(!Arrays.asList(SerialPortList.getPortNames()).contains(devicePath)) {
			DeviceSelectionDialog deviceSelectionDialog = new DeviceSelectionDialog(getShell(), DeviceType.USB);
			if(deviceSelectionDialog.open() != Window.OK || deviceSelectionDialog.getSelection() == null) 
				throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_NoAnswer, devicePath));
			devicePath = deviceSelectionDialog.getSelection();
			dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH, devicePath);
			ObjectsController.serialize(dacqConfiguration);
		}
		// Connection (and upload of the firmware) with a progress dialog
		final String selectedDevicePath = devicePath;
		final ChannelViewer[] viewers = channelsViewers.toArray(new ChannelViewer[channelsViewers.size()]);
		final ArduinoUnoCalibrationTask[] task = new ArduinoUnoCalibrationTask[1];
		try {
			new ProgressMonitorDialog(getShell()).run(true, false, monitor -> {
				monitor.beginTask(ArduinoUnoMessages.Calibration_TaskName, IProgressMonitor.UNKNOWN);
				try {
					task[0] = new ArduinoUnoCalibrationTask(viewers, selectedDevicePath, monitor);
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			});
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if(cause instanceof Exception) throw (Exception) cause;
			throw e;
		}
		CalibrationFactory.runCalibrationTask(task[0]);
	}
	
}
