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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests of the values accepted for Arduino Uno parameters, without Eclipse : run them as plain JUnit 5 tests.
 */
public class ArduinoUnoParameterValuesTest {
	
	private static boolean valid(String type, String value) {
		return ArduinoUnoParameterValues.isValid(type, 1, false, value);
	}
	
	@Test
	public void bool() {
		for (String value : new String[] {"0", "1", "true", "FALSE"}) assertTrue(valid(ArduinoUnoVariableProperties.BOOL, value), value);
		for (String value : new String[] {"2", "-1", "yes", ""}) assertFalse(valid(ArduinoUnoVariableProperties.BOOL, value), value);
	}
	
	@Test
	public void character() {
		for (String value : new String[] {"a", "Z", "5", "-", " "}) assertTrue(valid(ArduinoUnoVariableProperties.CHAR, value), value);
		for (String value : new String[] {"", "ab", "é", "\n"}) assertFalse(valid(ArduinoUnoVariableProperties.CHAR, value), value);
	}
	
	@Test
	public void intOn16BitsForR3() {
		for (String value : new String[] {"0", "-32768", "32767", "+12"}) assertTrue(valid(ArduinoUnoVariableProperties.INT, value), value);
		for (String value : new String[] {"32768", "-32769", "1.5", "1e3", "abc", ""}) assertFalse(valid(ArduinoUnoVariableProperties.INT, value), value);
	}
	
	@Test
	public void intOn32BitsForR4() {
		assertTrue(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.INT, 1, true, "32768"));
		assertTrue(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.INT, 1, true, "-2147483648"));
		assertFalse(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.INT, 1, true, "2147483648"));
	}
	
	@Test
	public void longOn32Bits() {
		for (String value : new String[] {"-2147483648", "2147483647", "-1"}) assertTrue(valid(ArduinoUnoVariableProperties.LONG, value), value);
		for (String value : new String[] {"2147483648", "-2147483649", "99999999999999999999", "1.0"}) assertFalse(valid(ArduinoUnoVariableProperties.LONG, value), value);
	}
	
	@Test
	public void floatingPoint() {
		for (String value : new String[] {"0", "-1.5", "+2.", ".5", "1e3", "-2.5E-3", "+1.234567E+00"}) assertTrue(valid(ArduinoUnoVariableProperties.FLOAT, value), value);
		for (String value : new String[] {"", "abc", "NaN", "Infinity", "1e40", "0x10", "1.5f", "1,5"}) assertFalse(valid(ArduinoUnoVariableProperties.FLOAT, value), value);
	}
	
	@Test
	public void stringLengthIsLimitedBySize() {
		assertTrue(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.STRING, 11, false, "condition-1"));
		assertTrue(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.STRING, 11, false, ""));
		assertFalse(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.STRING, 10, false, "condition-1"));
		assertFalse(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.STRING, 20, false, "café"));
		assertFalse(ArduinoUnoParameterValues.isValid(ArduinoUnoVariableProperties.STRING, 20, false, "a\nb"));
	}
	
	@Test
	public void nullOrUnknownType() {
		assertFalse(valid(ArduinoUnoVariableProperties.INT, null));
		assertFalse(valid(null, "1"));
		assertFalse(valid("double", "1"));
	}

}
