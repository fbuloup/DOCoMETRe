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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.calibration.CalibrationTimerTask;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoUploadDelegate;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.calibration.ArduinoUnoCalibrationProtocol.Input;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.calibration.ArduinoUnoCalibrationProtocol.InputType;
import fr.univamu.ism.docometre.widgets.ChannelViewer;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Calibration and monitoring of an Arduino Uno board with the generic calibration firmware (see
 * ArduinoUnoCalibrationProtocol) : the constructor connects to the firmware (uploaded when the board does
 * not run it), configures it from the channels of the viewers and starts the measures. Then, run() (every
 * 10 ms, see CalibrationFactory) displays the measures in the viewers of the inputs and sends the values of
 * the viewers of the outputs when they change.
 */
public class ArduinoUnoCalibrationTask extends CalibrationTimerTask {
	
	// The R3 restarts when the serial port is opened (about 1.5 s in its bootloader), the R4 USB port is
	// available again some time after an upload
	private static final long IDENTIFICATION_TIMEOUT_MS = 5000;
	private static final long IDENTIFICATION_REPEAT_MS = 500;
	private static final long ANSWER_TIMEOUT_MS = 2000;
	
	private ArduinoUnoDACQConfiguration dacqConfiguration;
	private String devicePath;
	private String board;
	private String analogReference;
	private SerialPort serialPort;
	// Chars received, not yet read as lines
	private StringBuilder receivedChars = new StringBuilder();
	private volatile boolean terminate;
	
	// Inputs, in the order of the measures, and their viewers
	private List<Input> inputs = new ArrayList<>();
	private List<ChannelViewer> inputsViewers = new ArrayList<>();
	// Outputs viewers and their last values sent
	private Map<ChannelViewer, Object> outputsValues = new LinkedHashMap<>();
	private Map<ChannelViewer, Boolean> outputsAnalog = new HashMap<>();
	// ADS1115 modules : address, data rate
	private Map<Integer, Integer> ads1115Modules = new LinkedHashMap<>();
	private List<Integer> digitalOutputs = new ArrayList<>();
	private List<Integer> analogOutputs = new ArrayList<>();
	
	/**
	 * @param channelsViewers the viewers of the channels of the dialog (same DACQ configuration)
	 * @param devicePath the serial port of the board
	 * @param monitor progress of the connection
	 */
	public ArduinoUnoCalibrationTask(ChannelViewer[] channelsViewers, String devicePath, IProgressMonitor monitor) throws Exception {
		this.devicePath = devicePath;
		ArduinoUnoCalibration.clearLastValues();
		if(channelsViewers.length == 0) throw new Exception(ArduinoUnoMessages.Calibration_NoChannel);
		dacqConfiguration = (ArduinoUnoDACQConfiguration) channelsViewers[0].getChannel().getModule().getDACQConfiguration();
		// Reference of the revision of the board, as in acquisition processes (see ArduinoUnoAnInModule)
		analogReference = null;
		for (Module module : dacqConfiguration.getModules()) 
			if(module instanceof ArduinoUnoAnInModule) analogReference = module.getProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE);
		analogReference = ArduinoUnoAnInModuleProperties.getReferenceOfRevision(analogReference, ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration));
		for (ChannelViewer channelViewer : channelsViewers) addChannel(channelViewer);
		if(inputs.size() > ArduinoUnoCalibrationProtocol.MAXIMUM_INPUTS) 
			throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_TooManyInputs, inputs.size(), ArduinoUnoCalibrationProtocol.MAXIMUM_INPUTS));
		if(outputsValues.size() > ArduinoUnoCalibrationProtocol.MAXIMUM_OUTPUTS) 
			throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_TooManyOutputs, outputsValues.size(), ArduinoUnoCalibrationProtocol.MAXIMUM_OUTPUTS));
		try {
			connect(monitor);
			monitor.subTask(ArduinoUnoMessages.Calibration_Configuring);
			List<int[]> modules = new ArrayList<>();
			for (Map.Entry<Integer, Integer> module : ads1115Modules.entrySet()) modules.add(new int[] {module.getKey(), module.getValue()});
			List<String> commands = ArduinoUnoCalibrationProtocol.getConfigurationCommands(hasAnalogInput() ? analogReference : null, modules, inputs, digitalOutputs, analogOutputs);
			commands.add(ArduinoUnoCalibrationProtocol.START);
			for (String command : commands) sendCommand(command);
			if(Double.isNaN(ArduinoUnoCalibrationProtocol.getReferenceVoltage(analogReference, board)) && hasAnalogInput())
				Activator.logWarningMessage(NLS.bind(ArduinoUnoMessages.Calibration_ExternalReference, getAnalogMaximum()));
		} catch (Exception e) {
			closePort();
			throw e;
		}
	}
	
	/*
	 * Input or output of a viewer, by the type of its module
	 */
	private void addChannel(ChannelViewer channelViewer) {
		Channel channel = channelViewer.getChannel();
		Module module = channel.getModule();
		int channelNumber = Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER));
		if(module instanceof ArduinoUnoAnInModule) {
			inputs.add(Input.analog(channelNumber));
			inputsViewers.add(channelViewer);
		} else if(module instanceof ArduinoUnoADS1115Module) {
			int address = ArduinoUnoCalibrationProtocol.getAddress(module.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS));
			ads1115Modules.put(address, Integer.parseInt(module.getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE)));
			int gain = ArduinoUnoCalibrationProtocol.getADS1115Gain(channel.getProperty(ArduinoUnoAnInChannelProperties.GAIN));
			inputs.add(Input.ads1115(address, channelNumber, gain));
			inputsViewers.add(channelViewer);
		} else if(module instanceof ArduinoUnoAnOutModule) {
			analogOutputs.add(channelNumber);
			outputsValues.put(channelViewer, null);
			outputsAnalog.put(channelViewer, true);
		} else if(module instanceof ArduinoUnoDigInOutModule) {
			String inOut = channel.getProperty(ArduinoUnoDigInOutChannelProperties.IN_OUT);
			if(ArduinoUnoDigInOutChannelProperties.OUTPUT.equals(inOut)) {
				digitalOutputs.add(channelNumber);
				outputsValues.put(channelViewer, null);
				outputsAnalog.put(channelViewer, false);
			} else {
				inputs.add(Input.digital(channelNumber, ArduinoUnoDigInOutChannelProperties.INPUT_PULLUP.equals(inOut)));
				inputsViewers.add(channelViewer);
			}
		}
	}
	
	/*
	 * Board of the revision of the DACQ configuration
	 */
	private String getExpectedBoard() {
		return ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION)) ? ArduinoUnoCalibrationProtocol.BOARD_R4 : ArduinoUnoCalibrationProtocol.BOARD_R3;
	}
	
	private boolean hasAnalogInput() {
		for (Input input : inputs) if(input.getType() == InputType.ANALOG) return true;
		return false;
	}
	
	private int getAnalogMaximum() {
		return ArduinoUnoCalibrationProtocol.BOARD_R4.equals(board) ? 16383 : 1023;
	}
	
	/*
	 * Connection to the firmware, uploaded when the board does not run it (or another version)
	 */
	private void connect(IProgressMonitor monitor) throws Exception {
		String expectedBoard = getExpectedBoard();
		monitor.subTask(NLS.bind(ArduinoUnoMessages.Calibration_Connecting, devicePath));
		board = identify();
		if(board == null) {
			closePort();
			monitor.subTask(ArduinoUnoMessages.Calibration_Uploading);
			upload(expectedBoard);
			monitor.subTask(NLS.bind(ArduinoUnoMessages.Calibration_Connecting, devicePath));
			board = identify();
			if(board == null) throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_NoAnswer, devicePath));
		}
		// The process loaded on the board has been replaced by the firmware : it must be uploaded again
		ArduinoUnoProcess.forceUpload = true;
		if(!board.equals(expectedBoard)) throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_WrongBoard, new Object[] {devicePath, board, expectedBoard}));
	}
	
	/*
	 * Sends identification commands until the firmware answers : its board, null if it does not answer
	 * (other program on the board, or firmware of another version)
	 */
	private String identify() throws Exception {
		long start = System.currentTimeMillis();
		long lastRequest = 0;
		while (System.currentTimeMillis() - start < IDENTIFICATION_TIMEOUT_MS) {
			if(serialPort == null || !serialPort.isOpened()) {
				// The port can be unavailable for some time after an upload (R4 USB port)
				if(!openPort()) {
					Thread.sleep(100);
					continue;
				}
			}
			if(System.currentTimeMillis() - lastRequest >= IDENTIFICATION_REPEAT_MS) {
				serialPort.writeString(ArduinoUnoCalibrationProtocol.IDENTIFICATION + "\n");
				lastRequest = System.currentTimeMillis();
			}
			String line = readLine();
			if(line == null) {
				Thread.sleep(10);
				continue;
			}
			if(ArduinoUnoCalibrationProtocol.isIdentification(line)) return ArduinoUnoCalibrationProtocol.getBoard(line);
		}
		return null;
	}
	
	private void upload(String expectedBoard) throws Exception {
		boolean useCLI = "true".equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI));
		if(ArduinoUnoCalibrationProtocol.BOARD_R4.equals(expectedBoard) && !useCLI) throw new Exception(ArduinoUnoMessages.Calibration_R4RequiresArduinoCLI);
		File firmwareFile = ArduinoUnoCalibration.getFirmwareFile(expectedBoard);
		String command = ArduinoUnoUploadDelegate.getUploadCommand(dacqConfiguration, devicePath, firmwareFile.getAbsolutePath());
		String scriptPath = ArduinoUnoUploadDelegate.createUploadScript(ArduinoUnoCalibration.getWorkingFolder(), command);
		String errorOutput = ArduinoUnoUploadDelegate.runUploadScript(scriptPath);
		ArduinoUnoProcess.forceUpload = true;
		if(!errorOutput.isEmpty()) Activator.logInfoMessage(errorOutput, ArduinoUnoCalibrationTask.class);
		if(ArduinoUnoUploadDelegate.hasFailed(errorOutput)) throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_UploadFailed, errorOutput));
	}
	
	/*
	 * Sends a configuration command and waits for its acknowledge
	 */
	private void sendCommand(String command) throws Exception {
		serialPort.writeString(command + "\n");
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < ANSWER_TIMEOUT_MS) {
			String line = readLine();
			if(line == null) {
				Thread.sleep(5);
				continue;
			}
			if(ArduinoUnoCalibrationProtocol.ACKNOWLEDGE.equals(line)) return;
			if(line.startsWith(ArduinoUnoCalibrationProtocol.ERROR_PREFIX)) throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_CommandRefused, command));
			// Other lines (identification, measures) are ignored
		}
		throw new Exception(NLS.bind(ArduinoUnoMessages.Calibration_NoAnswer, devicePath));
	}
	
	private boolean openPort() {
		try {
			serialPort = new SerialPort(devicePath);
			serialPort.openPort();
			serialPort.setParams(ArduinoUnoCalibrationProtocol.BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, true, true);
			serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
			receivedChars.setLength(0);
			return true;
		} catch (SerialPortException e) {
			closePort();
			return false;
		}
	}
	
	private void closePort() {
		try {
			if(serialPort != null && serialPort.isOpened()) serialPort.closePort();
		} catch (SerialPortException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Next line received (without end of line chars), null if no complete line has been received
	 */
	private String readLine() throws SerialPortException {
		if(serialPort.getInputBufferBytesCount() > 0) {
			byte[] bytes = serialPort.readBytes();
			if(bytes != null) receivedChars.append(new String(bytes, StandardCharsets.US_ASCII));
		}
		while (receivedChars.length() > 0) {
			int end = 0;
			while (end < receivedChars.length() && receivedChars.charAt(end) != '\n' && receivedChars.charAt(end) != '\r') end++;
			if(end == receivedChars.length()) return null;
			String line = receivedChars.substring(0, end);
			receivedChars.delete(0, end + 1);
			if(!line.isEmpty()) return line;
		}
		return null;
	}
	
	/*
	 * Value displayed for an input : volts for analog inputs (raw value when the voltage of the reference is not known), 0 or 1 for digital inputs
	 */
	private double getValue(Input input, long raw) {
		if(input.getType() == InputType.ADS1115) return ArduinoUnoCalibrationProtocol.getADS1115Voltage(raw, input.getGain());
		if(input.getType() == InputType.ANALOG) {
			double voltage = ArduinoUnoCalibrationProtocol.getAnalogVoltage(raw, analogReference, board);
			return Double.isNaN(voltage) ? raw : voltage;
		}
		return raw;
	}

	@Override
	public void run() {
		if(terminate) {
			// Stop and clear : outputs reset, digital outputs in high impedance
			try {
				if(serialPort != null && serialPort.isOpened()) {
					serialPort.writeString(ArduinoUnoCalibrationProtocol.STOP + "\n" + ArduinoUnoCalibrationProtocol.CLEAR + "\n");
					Thread.sleep(50);
				}
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);
			} finally {
				closePort();
				cancel();
			}
			return;
		}
		try {
			// Measures : the last complete measures line received is displayed
			long[] measures = null;
			String line;
			while ((line = readLine()) != null) {
				long[] lineMeasures = ArduinoUnoCalibrationProtocol.getMeasures(line, inputs.size());
				if(lineMeasures != null) measures = lineMeasures;
			}
			if(measures != null && !inputs.isEmpty()) {
				double time = System.currentTimeMillis()/1000.0;
				double[] values = new double[measures.length];
				for (int i = 0; i < measures.length; i++) {
					values[i] = getValue(inputs.get(i), measures[i]);
					ArduinoUnoCalibration.setLastValue(inputsViewers.get(i).getChannel(), values[i]);
				}
				Display display = Display.getDefault();
				if(!terminate && display != null && !display.isDisposed()) display.syncExec(() -> {
					for (int i = 0; i < values.length; i++) {
						ChannelViewer channelViewer = inputsViewers.get(i);
						if(!channelViewer.isDisposed()) channelViewer.addSample(time, values[i]);
					}
				});
			}
			// Outputs : sent when they change
			for (Map.Entry<ChannelViewer, Object> output : outputsValues.entrySet()) {
				ChannelViewer channelViewer = output.getKey();
				if(channelViewer.isDisposed()) continue;
				Object value = channelViewer.getValue();
				if(Objects.equals(value, output.getValue())) continue;
				int channelNumber = Integer.parseInt(channelViewer.getChannel().getProperty(ChannelProperties.CHANNEL_NUMBER));
				String command;
				if(outputsAnalog.get(channelViewer)) command = ArduinoUnoCalibrationProtocol.getAnalogOutputCommand(channelNumber, (double) value);
				else command = ArduinoUnoCalibrationProtocol.getDigitalOutputCommand(channelNumber, (boolean) value);
				serialPort.writeString(command + "\n");
				output.setValue(value);
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			terminate = true;
		}
	}

	@Override
	public void terminate() {
		terminate = true;
	}

}
