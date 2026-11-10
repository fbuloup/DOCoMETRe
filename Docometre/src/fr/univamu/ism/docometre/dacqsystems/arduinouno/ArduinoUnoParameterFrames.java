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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [10/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import java.util.Locale;

/**
 * Frames used to exchange the values of the parameters between the PC and the Arduino Uno,
 * on the serial port. Plain Java class (no Eclipse dependencies).
 * <p>
 * Each frame is a line ended by '\n' :
 * <ul>
 * <li><code>P&lt;n&gt;:&lt;value&gt;</code> : value of parameter number n (its rank, starting at 1, in
 * DACQConfiguration.getParameters()) ; the value is everything after the first ':' ;</li>
 * <li><code>E&lt;count&gt;</code> : end of the sending of count parameters ;</li>
 * <li><code>A&lt;count&gt;</code> : acknowledgement, all the count parameters have been received and read ;</li>
 * <li><code>N&lt;n&gt;</code> : error on parameter number n (missing, unknown or unreadable value), or
 * <code>N0</code> if count is not the number of parameters of the receiver (process to compile again).</li>
 * </ul>
 * Before the start of the process, the PC sends the P frames of all the parameters then the E frame,
 * the Arduino Uno answers with A or N. At the end of the process, the Arduino Uno sends the P frames
 * of the propagated parameters then the E frame, the PC answers with A or N.
 * <p>
 * Data frames sent by the Arduino Uno during the process (<code>&lt;transfer number&gt;:&lt;value&gt;</code>)
 * start with a digit : they can't be confused with these frames.
 * <p>
 * Values in the frames, by type :
 * <ul>
 * <li>bool : 0 or 1 ;</li>
 * <li>char : the character ;</li>
 * <li>int, long : integer in base 10, without + sign ;</li>
 * <li>float : %+.6E format (e.g. +1.234567E+00), as the transferred values ;</li>
 * <li>string : the characters.</li>
 * </ul>
 */
public final class ArduinoUnoParameterFrames {
	
	public static final char PARAMETER = 'P';
	public static final char END = 'E';
	public static final char ACKNOWLEDGEMENT = 'A';
	public static final char ERROR = 'N';
	public static final char SEPARATOR = ':';
	public static final char END_OF_FRAME = '\n';
	
	/**
	 * Number of the parameter of an N frame when the number of parameters is not the one of the receiver
	 */
	public static final int COUNT_ERROR = 0;
	
	/**
	 * Kinds of frames
	 */
	public enum Kind {
		PARAMETER, END, ACKNOWLEDGEMENT, ERROR
	}
	
	/**
	 * A frame read on the serial port
	 */
	public static final class Frame {
		
		private final Kind kind;
		private final int number;
		private final String value;
		
		private Frame(Kind kind, int number, String value) {
			this.kind = kind;
			this.number = number;
			this.value = value;
		}
		
		public Kind getKind() {
			return kind;
		}
		
		/**
		 * @return the parameter number (P and N frames) or the count of parameters (E and A frames)
		 */
		public int getNumber() {
			return number;
		}
		
		/**
		 * @return the value (P frame only, null otherwise)
		 */
		public String getValue() {
			return value;
		}
		
	}
	
	/**
	 * Reception of the values of the parameters, until the E frame : gives the answer to send (A or N frame).
	 * The receiver knows the types of the parameters it expects, in the order of their numbers.
	 */
	public static final class Reception {
		
		private final String[] types;
		private final int[] sizes;
		private final boolean int32;
		private final String[] values;
		private int errorNumber = -1;
		private boolean ended;
		private boolean acknowledged;
		
		/**
		 * @param types the types of the expected parameters (types[n - 1] for parameter number n)
		 * @param sizes the sizes of the expected parameters (maximum length for a string)
		 * @param int32 true if int is on 32 bits (R4 WiFi), false if on 16 bits (R3)
		 */
		public Reception(String[] types, int[] sizes, boolean int32) {
			this.types = types.clone();
			this.sizes = sizes.clone();
			this.int32 = int32;
			this.values = new String[types.length];
		}
		
		/**
		 * @param frame a received P or E frame (other frames are ignored)
		 * @return the answer to send when frame is the E frame (A or N frame, see {@link #isAcknowledged()}), null otherwise
		 */
		public String accept(Frame frame) {
			if(frame == null || ended) return null;
			if(frame.getKind() == Kind.PARAMETER) {
				int index = frame.getNumber() - 1;
				boolean valid = index >= 0 && index < types.length && ArduinoUnoParameterValues.isValid(types[index], sizes[index], int32, fromFrameValue(types[index], frame.getValue()));
				if(valid) values[index] = frame.getValue();
				else if(errorNumber == -1) errorNumber = frame.getNumber();
				return null;
			}
			if(frame.getKind() == Kind.END) {
				ended = true;
				if(frame.getNumber() != types.length) return errorFrame(COUNT_ERROR);
				if(errorNumber != -1) return errorFrame(errorNumber);
				for (int i = 0; i < values.length; i++) {
					if(values[i] == null) return errorFrame(i + 1);
				}
				acknowledged = true;
				return acknowledgementFrame(types.length);
			}
			return null;
		}
		
		/**
		 * @return true if the E frame has been received
		 */
		public boolean isEnded() {
			return ended;
		}
		
		/**
		 * @return true if all the parameters have been received and read (A frame answered)
		 */
		public boolean isAcknowledged() {
			return acknowledged;
		}
		
		/**
		 * @param number the number of a parameter (starting at 1)
		 * @return its received value, in the format of the parameters file (see {@link ArduinoUnoParameterFrames#fromFrameValue(String, String)}),
		 * null if not received or not acknowledged
		 */
		public String getValue(int number) {
			if(!acknowledged || number < 1 || number > values.length) return null;
			return fromFrameValue(types[number - 1], values[number - 1]);
		}
		
	}
	
	private ArduinoUnoParameterFrames() {
	}
	
	/**
	 * @param number the number of the parameter (starting at 1)
	 * @param frameValue the value in the frame format (see {@link #toFrameValue(String, String)})
	 * @return the P frame, ended by '\n'
	 */
	public static String parameterFrame(int number, String frameValue) {
		return PARAMETER + Integer.toString(number) + SEPARATOR + frameValue + END_OF_FRAME;
	}
	
	/**
	 * @return the E frame for count parameters, ended by '\n'
	 */
	public static String endFrame(int count) {
		return END + Integer.toString(count) + END_OF_FRAME;
	}
	
	/**
	 * @return the A frame for count parameters, ended by '\n'
	 */
	public static String acknowledgementFrame(int count) {
		return ACKNOWLEDGEMENT + Integer.toString(count) + END_OF_FRAME;
	}
	
	/**
	 * @return the N frame for parameter number (or {@link #COUNT_ERROR}), ended by '\n'
	 */
	public static String errorFrame(int number) {
		return ERROR + Integer.toString(number) + END_OF_FRAME;
	}
	
	/**
	 * @param line a line read on the serial port, without or with its '\r' and '\n' characters
	 * @return the frame, null if the line is not a parameters frame (e.g. a data frame)
	 */
	public static Frame parse(String line) {
		if(line == null) return null;
		int length = line.length();
		while(length > 0 && (line.charAt(length - 1) == '\n' || line.charAt(length - 1) == '\r')) length--;
		line = line.substring(0, length);
		if(line.length() < 2) return null;
		char kindChar = line.charAt(0);
		if(kindChar == PARAMETER) {
			int separatorIndex = line.indexOf(SEPARATOR);
			if(separatorIndex < 2) return null;
			Integer number = parseNumber(line.substring(1, separatorIndex));
			if(number == null || number < 1) return null;
			return new Frame(Kind.PARAMETER, number, line.substring(separatorIndex + 1));
		}
		Kind kind = null;
		if(kindChar == END) kind = Kind.END;
		if(kindChar == ACKNOWLEDGEMENT) kind = Kind.ACKNOWLEDGEMENT;
		if(kindChar == ERROR) kind = Kind.ERROR;
		if(kind == null) return null;
		Integer number = parseNumber(line.substring(1));
		if(number == null) return null;
		return new Frame(kind, number, null);
	}
	
	/**
	 * @param type the type of the parameter (see {@link ArduinoUnoVariableProperties#TYPES})
	 * @param value a valid value (see {@link ArduinoUnoParameterValues}), as read in the parameters file or entered by the user
	 * @return the value in the frame format
	 */
	public static String toFrameValue(String type, String value) {
		switch (type) {
		case ArduinoUnoVariableProperties.BOOL:
			return value.equals("1") || value.equalsIgnoreCase("true") ? "1" : "0";
		case ArduinoUnoVariableProperties.INT:
		case ArduinoUnoVariableProperties.LONG:
			return Long.toString(Long.parseLong(value.startsWith("+") ? value.substring(1) : value));
		case ArduinoUnoVariableProperties.FLOAT:
			return String.format(Locale.ROOT, "%+.6E", Float.parseFloat(value));
		default:
			// char and string
			return value;
		}
	}
	
	/**
	 * @param type the type of the parameter (see {@link ArduinoUnoVariableProperties#TYPES})
	 * @param frameValue the value in a frame
	 * @return the value in the format of the parameters file (e.g. 5 for an int, 1.5 for a float),
	 * the frame value itself if it can't be read
	 */
	public static String fromFrameValue(String type, String frameValue) {
		if(type == null || frameValue == null) return frameValue;
		try {
			switch (type) {
			case ArduinoUnoVariableProperties.INT:
			case ArduinoUnoVariableProperties.LONG:
				if(!frameValue.matches("^[+-]?\\d+$")) return frameValue;
				return Long.toString(Long.parseLong(frameValue.startsWith("+") ? frameValue.substring(1) : frameValue));
			case ArduinoUnoVariableProperties.FLOAT:
				if(!frameValue.matches("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$")) return frameValue;
				return String.valueOf(Float.parseFloat(frameValue));
			default:
				// bool, char and string
				return frameValue;
			}
		} catch (NumberFormatException e) {
			return frameValue;
		}
	}
	
	private static Integer parseNumber(String text) {
		if(!text.matches("^\\d{1,9}$")) return null;
		return Integer.parseInt(text);
	}

}
