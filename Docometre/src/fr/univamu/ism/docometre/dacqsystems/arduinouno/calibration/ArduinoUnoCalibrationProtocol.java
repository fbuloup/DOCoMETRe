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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computer side of the serial protocol of the generic calibration firmware for Arduino Uno R3 and
 * R4 WiFi (Libraries/includes/ArduinoUnoCalibrationFiles, see CalibrationProtocol.h), without Eclipse
 * dependencies : commands sent to the firmware, answers and measures received, conversion of the raw
 * values of the converters in volts.
 */
public final class ArduinoUnoCalibrationProtocol {
	
	/**
	 * Version of the protocol expected in the identification of the firmware
	 */
	public static final int VERSION = 1;
	
	/**
	 * Baud rate of the firmware (precompiled)
	 */
	public static final int BAUD_RATE = 115200;
	
	public static final String IDENTIFICATION = "I";
	public static final String CLEAR = "C";
	public static final String START = "G";
	public static final String STOP = "H";
	public static final String ACKNOWLEDGE = "K";
	public static final String ERROR_PREFIX = "E:";
	public static final String MEASURES_PREFIX = "M:";
	
	public static final String BOARD_R3 = "R3";
	public static final String BOARD_R4 = "R4";
	
	/**
	 * Period of the measures sent by the firmware (ms)
	 */
	public static final int MEASURES_PERIOD = 20;
	
	/**
	 * Maximum value of analog outputs (analogWrite, 8 bits as in acquisition processes)
	 */
	public static final int ANALOG_OUTPUT_MAXIMUM = 255;
	
	/**
	 * Inputs and outputs limits of the firmware (CAL_MAX_INPUTS, CAL_MAX_OUTPUTS)
	 */
	public static final int MAXIMUM_INPUTS = 24;
	public static final int MAXIMUM_OUTPUTS = 24;
	
	private static final Pattern IDENTIFICATION_PATTERN = Pattern.compile("^CAL(\\d+):(\\w+)$");
	
	public enum InputType { ANALOG, ADS1115, DIGITAL }
	
	/**
	 * An input read by the firmware
	 */
	public static final class Input {
		
		private final InputType type;
		private final int pin;
		private final int address;
		private final int gain;
		private final boolean pullUp;
		
		private Input(InputType type, int pin, int address, int gain, boolean pullUp) {
			this.type = type;
			this.pin = pin;
			this.address = address;
			this.gain = gain;
			this.pullUp = pullUp;
		}
		
		public static Input analog(int pin) {
			return new Input(InputType.ANALOG, pin, 0, 0, false);
		}
		
		/**
		 * @param gain gain of the ADS1X15 library : 0 (2/3), 1, 2, 4, 8 or 16 (see {@link #getADS1115Gain(String)})
		 */
		public static Input ads1115(int address, int channel, int gain) {
			return new Input(InputType.ADS1115, channel, address, gain, false);
		}
		
		public static Input digital(int pin, boolean pullUp) {
			return new Input(InputType.DIGITAL, pin, 0, 0, pullUp);
		}
		
		public InputType getType() {
			return type;
		}
		
		public int getGain() {
			return gain;
		}
		
		private String getCommand() {
			if(type == InputType.ANALOG) return "A" + pin;
			if(type == InputType.ADS1115) return "S" + getAddressText(address) + ":" + pin + ":" + gain;
			return "D" + pin + ":" + (pullUp ? 1 : 0);
		}
		
	}
	
	private ArduinoUnoCalibrationProtocol() {
	}
	
	/**
	 * @param address an I2C address
	 * @return its hexadecimal text, as in the DACQ configuration (e.g. 0x48)
	 */
	public static String getAddressText(int address) {
		return String.format("0x%02X", address);
	}
	
	/**
	 * @param addressText an I2C address in hexadecimal, as in the DACQ configuration (e.g. 0x48)
	 */
	public static int getAddress(String addressText) {
		return Integer.decode(addressText.trim());
	}
	
	/**
	 * @param gain gain of an ADS1115 input, as in the DACQ configuration (2/3, 1, 2, 4, 8 or 16)
	 * @return the gain of the ADS1X15 library : 0 for 2/3, the gain otherwise
	 */
	public static int getADS1115Gain(String gain) {
		if("2/3".equals(gain.trim())) return 0;
		return Integer.parseInt(gain.trim());
	}
	
	/**
	 * @param line a line received from the board
	 * @return the board of the firmware (BOARD_R3 or BOARD_R4) when line is its identification with the expected
	 * version of the protocol, null otherwise
	 */
	public static String getBoard(String line) {
		Matcher matcher = IDENTIFICATION_PATTERN.matcher(line.trim());
		if(!matcher.matches() || Integer.parseInt(matcher.group(1)) != VERSION) return null;
		String board = matcher.group(2);
		return BOARD_R3.equals(board) || BOARD_R4.equals(board) ? board : null;
	}
	
	/**
	 * @return true if line is the identification of the firmware, whatever its version
	 */
	public static boolean isIdentification(String line) {
		return IDENTIFICATION_PATTERN.matcher(line.trim()).matches();
	}
	
	/**
	 * @param reference an analog reference
	 * @param board BOARD_R3 or BOARD_R4
	 * @return true if the reference is known by the board : DEFAULT, INTERNAL or EXTERNAL for R3, AR_DEFAULT,
	 * AR_INTERNAL or AR_EXTERNAL for R4
	 */
	public static boolean isReferenceOfBoard(String reference, String board) {
		reference = reference.trim();
		if(BOARD_R4.equals(board)) return "AR_DEFAULT".equals(reference) || "AR_INTERNAL".equals(reference) || "AR_EXTERNAL".equals(reference);
		return "DEFAULT".equals(reference) || "INTERNAL".equals(reference) || "EXTERNAL".equals(reference);
	}
	
	/**
	 * @param reference the analog reference, as in the analog inputs module (see {@link #isReferenceOfBoard(String, String)}),
	 * null when there is no analog input (not sent)
	 * @param ads1115Modules addresses and data rates of the ADS1115 modules used by inputs ({address, data rate} pairs)
	 * @param inputs the inputs, in the order of the measures
	 * @param digitalOutputs pins of the digital outputs
	 * @param analogOutputs pins of the analog outputs
	 * @return the commands which configure the firmware, each one answered by ACKNOWLEDGE
	 */
	public static List<String> getConfigurationCommands(String reference, List<int[]> ads1115Modules, List<Input> inputs, List<Integer> digitalOutputs, List<Integer> analogOutputs) {
		List<String> commands = new ArrayList<>();
		commands.add(CLEAR);
		if(reference != null) commands.add("F" + reference.trim());
		commands.add("T" + MEASURES_PERIOD);
		for (int[] module : ads1115Modules) commands.add("B" + getAddressText(module[0]) + ":" + module[1]);
		for (Input input : inputs) commands.add(input.getCommand());
		for (Integer pin : digitalOutputs) commands.add("P" + pin);
		for (Integer pin : analogOutputs) commands.add("Q" + pin);
		return commands;
	}
	
	public static String getDigitalOutputCommand(int pin, boolean high) {
		return "W" + pin + ":" + (high ? 1 : 0);
	}
	
	/**
	 * @param value the value of the output, limited to 0 - ANALOG_OUTPUT_MAXIMUM
	 */
	public static String getAnalogOutputCommand(int pin, double value) {
		return "O" + pin + ":" + getAnalogOutputValue(value);
	}
	
	/**
	 * @return the value written by analogWrite for this value : rounded and limited to 0 - ANALOG_OUTPUT_MAXIMUM
	 */
	public static int getAnalogOutputValue(double value) {
		long digitValue = Math.round(value);
		return (int) Math.max(0, Math.min(ANALOG_OUTPUT_MAXIMUM, digitValue));
	}
	
	/**
	 * @param line a line received from the board
	 * @param inputsNumber the number of inputs
	 * @return the raw values of the inputs if line is a measures line with this number of values, null otherwise
	 */
	public static long[] getMeasures(String line, int inputsNumber) {
		line = line.trim();
		if(!line.startsWith(MEASURES_PREFIX)) return null;
		String values = line.substring(MEASURES_PREFIX.length());
		if(inputsNumber == 0) return values.isEmpty() ? new long[0] : null;
		String[] texts = values.split(";", -1);
		if(texts.length != inputsNumber) return null;
		long[] measures = new long[inputsNumber];
		try {
			for (int i = 0; i < texts.length; i++) measures[i] = Long.parseLong(texts[i]);
		} catch (NumberFormatException e) {
			return null;
		}
		return measures;
	}
	
	/**
	 * @param reference the analog reference (see {@link #isReferenceOfBoard(String, String)})
	 * @param board BOARD_R3 or BOARD_R4
	 * @return the voltage of this reference, NaN when it is not known (EXTERNAL, AR_EXTERNAL : voltage of the AREF
	 * pin ; reference of the other board)
	 */
	public static double getReferenceVoltage(String reference, String board) {
		reference = reference.trim();
		if(!isReferenceOfBoard(reference, board)) return Double.NaN;
		if("DEFAULT".equals(reference) || "AR_DEFAULT".equals(reference)) return 5;
		if("INTERNAL".equals(reference)) return 1.1;
		if("AR_INTERNAL".equals(reference)) return 1.5;
		return Double.NaN;
	}
	
	/**
	 * @param raw a raw value of analogRead() (10 bits on R3, 14 bits on R4 as in acquisition processes)
	 * @return the voltage, NaN when the voltage of the reference is not known
	 */
	public static double getAnalogVoltage(long raw, String reference, String board) {
		double levels = BOARD_R4.equals(board) ? 16384 : 1024;
		return raw * getReferenceVoltage(reference, board) / levels;
	}
	
	/**
	 * @param raw a raw value of an ADS1115 input (16 bits signed)
	 * @param gain the gain of the ADS1X15 library (0 for 2/3, 1, 2, 4, 8 or 16)
	 * @return the voltage
	 */
	public static double getADS1115Voltage(long raw, int gain) {
		double fullScale;
		switch (gain) {
		case 1: fullScale = 4.096; break;
		case 2: fullScale = 2.048; break;
		case 4: fullScale = 1.024; break;
		case 8: fullScale = 0.512; break;
		case 16: fullScale = 0.256; break;
		default: fullScale = 6.144; break;
		}
		return raw * fullScale / 32768;
	}

}
