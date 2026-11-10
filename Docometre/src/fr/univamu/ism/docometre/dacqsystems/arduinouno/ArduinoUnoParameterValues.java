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

/**
 * Values accepted for the parameters of an Arduino Uno process, by type of variable.
 * Plain Java class (no Eclipse dependencies).
 * <ul>
 * <li>bool : 0, 1, true or false (case is ignored) ;</li>
 * <li>char : one printable ASCII character ;</li>
 * <li>int : integer on 16 bits for R3 (AVR), on 32 bits for R4 WiFi (ARM) ;</li>
 * <li>long : integer on 32 bits ;</li>
 * <li>float : decimal number, with optional exponent (e.g. -1.5, 2.5E-3) ;</li>
 * <li>string : printable ASCII characters, at most size characters.</li>
 * </ul>
 */
public final class ArduinoUnoParameterValues {
	
	private static final String BOOL_REGEXP = "(?i)^(0|1|true|false)$";
	private static final String INTEGER_REGEXP = "^[+-]?\\d+$";
	private static final String FLOAT_REGEXP = "^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$";
	
	private ArduinoUnoParameterValues() {
	}
	
	/**
	 * @param type the type of the variable (see {@link ArduinoUnoVariableProperties#TYPES})
	 * @param size the size of the variable (maximum length for a string)
	 * @param int32 true if int is on 32 bits (R4 WiFi), false if on 16 bits (R3)
	 * @param value the value to check
	 * @return true if the value can be used for a parameter of this type
	 */
	public static boolean isValid(String type, int size, boolean int32, String value) {
		if(type == null || value == null) return false;
		switch (type) {
		case ArduinoUnoVariableProperties.BOOL:
			return value.matches(BOOL_REGEXP);
		case ArduinoUnoVariableProperties.CHAR:
			return value.length() == 1 && isPrintableASCII(value);
		case ArduinoUnoVariableProperties.INT:
			return int32 ? isInteger(value, Integer.MIN_VALUE, Integer.MAX_VALUE) : isInteger(value, Short.MIN_VALUE, Short.MAX_VALUE);
		case ArduinoUnoVariableProperties.LONG:
			return isInteger(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
		case ArduinoUnoVariableProperties.FLOAT:
			if(!value.matches(FLOAT_REGEXP)) return false;
			float floatValue = Float.parseFloat(value);
			return !Float.isInfinite(floatValue);
		case ArduinoUnoVariableProperties.STRING:
			return value.length() <= size && isPrintableASCII(value);
		default:
			return false;
		}
	}
	
	private static boolean isInteger(String value, long min, long max) {
		if(!value.matches(INTEGER_REGEXP)) return false;
		try {
			long longValue = Long.parseLong(value.startsWith("+") ? value.substring(1) : value);
			return longValue >= min && longValue <= max;
		} catch (NumberFormatException e) {
			// More than 64 bits
			return false;
		}
	}
	
	private static boolean isPrintableASCII(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if(c < 0x20 || c > 0x7E) return false;
		}
		return true;
	}

}
