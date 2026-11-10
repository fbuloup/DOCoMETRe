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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoParameterFrames.Frame;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoParameterFrames.Kind;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoParameterFrames.Reception;

/**
 * Tests of the parameters frames, without Eclipse : run them as plain JUnit 5 tests.
 */
public class ArduinoUnoParameterFramesTest {
	
	private static final String BOOL = ArduinoUnoVariableProperties.BOOL;
	private static final String CHAR = ArduinoUnoVariableProperties.CHAR;
	private static final String INT = ArduinoUnoVariableProperties.INT;
	private static final String LONG = ArduinoUnoVariableProperties.LONG;
	private static final String FLOAT = ArduinoUnoVariableProperties.FLOAT;
	private static final String STRING = ArduinoUnoVariableProperties.STRING;
	
	@Test
	public void framesAreWritten() {
		assertEquals("P1:12\n", ArduinoUnoParameterFrames.parameterFrame(1, "12"));
		assertEquals("P10:condition-1\n", ArduinoUnoParameterFrames.parameterFrame(10, "condition-1"));
		assertEquals("E3\n", ArduinoUnoParameterFrames.endFrame(3));
		assertEquals("A3\n", ArduinoUnoParameterFrames.acknowledgementFrame(3));
		assertEquals("N2\n", ArduinoUnoParameterFrames.errorFrame(2));
		assertEquals("N0\n", ArduinoUnoParameterFrames.errorFrame(ArduinoUnoParameterFrames.COUNT_ERROR));
	}
	
	@Test
	public void parameterFrameIsRead() {
		Frame frame = ArduinoUnoParameterFrames.parse("P12:-1.5\r\n");
		assertEquals(Kind.PARAMETER, frame.getKind());
		assertEquals(12, frame.getNumber());
		assertEquals("-1.5", frame.getValue());
		// Value is everything after the first ':'
		assertEquals("a:b", ArduinoUnoParameterFrames.parse("P2:a:b").getValue());
		assertEquals("", ArduinoUnoParameterFrames.parse("P2:").getValue());
	}
	
	@Test
	public void endAcknowledgementAndErrorFramesAreRead() {
		assertEquals(Kind.END, ArduinoUnoParameterFrames.parse("E3").getKind());
		assertEquals(3, ArduinoUnoParameterFrames.parse("E3\n").getNumber());
		assertEquals(Kind.ACKNOWLEDGEMENT, ArduinoUnoParameterFrames.parse("A3").getKind());
		Frame frame = ArduinoUnoParameterFrames.parse("N0");
		assertEquals(Kind.ERROR, frame.getKind());
		assertEquals(ArduinoUnoParameterFrames.COUNT_ERROR, frame.getNumber());
		assertNull(frame.getValue());
	}
	
	@Test
	public void otherLinesAreNotFrames() {
		// Data frames, time and workload, handshake chars
		for (String line : new String[] {"1:+1.234567E+00", "0:200000:12", "w", "s", "", "P", "P1", "P:1", "P0:1", "Px:1", "E", "Ex", "E-1", "A1.5", "X3", "P1234567890:1"}) {
			assertNull(ArduinoUnoParameterFrames.parse(line), line);
		}
		assertNull(ArduinoUnoParameterFrames.parse(null));
	}
	
	@Test
	public void valuesAreConvertedToFrameFormat() {
		assertEquals("1", ArduinoUnoParameterFrames.toFrameValue(BOOL, "true"));
		assertEquals("0", ArduinoUnoParameterFrames.toFrameValue(BOOL, "FALSE"));
		assertEquals("1", ArduinoUnoParameterFrames.toFrameValue(BOOL, "1"));
		assertEquals("x", ArduinoUnoParameterFrames.toFrameValue(CHAR, "x"));
		assertEquals("12", ArduinoUnoParameterFrames.toFrameValue(INT, "+12"));
		assertEquals("-2147483648", ArduinoUnoParameterFrames.toFrameValue(LONG, "-2147483648"));
		assertEquals("+1.500000E+00", ArduinoUnoParameterFrames.toFrameValue(FLOAT, "1.5"));
		assertEquals("-2.500000E-03", ArduinoUnoParameterFrames.toFrameValue(FLOAT, "-2.5e-3"));
		assertEquals("condition-1", ArduinoUnoParameterFrames.toFrameValue(STRING, "condition-1"));
	}
	
	@Test
	public void frameValuesAreConvertedToFileFormat() {
		assertEquals("5", ArduinoUnoParameterFrames.fromFrameValue(INT, "+5"));
		assertEquals("-7", ArduinoUnoParameterFrames.fromFrameValue(LONG, "-7"));
		assertEquals("1.234567", ArduinoUnoParameterFrames.fromFrameValue(FLOAT, "+1.234567E+00"));
		assertEquals("-0.0025", ArduinoUnoParameterFrames.fromFrameValue(FLOAT, "-2.500000E-03"));
		assertEquals("1", ArduinoUnoParameterFrames.fromFrameValue(BOOL, "1"));
		// Unreadable value is kept as is
		assertEquals("abc", ArduinoUnoParameterFrames.fromFrameValue(INT, "abc"));
	}
	
	@Test
	public void valuesRoundTrip() {
		String[][] values = {{BOOL, "1"}, {CHAR, "z"}, {INT, "-32768"}, {LONG, "2147483647"}, {FLOAT, "0.1"}, {FLOAT, "-123.456"}, {STRING, "abc"}};
		for (String[] typeValue : values) {
			String frameValue = ArduinoUnoParameterFrames.toFrameValue(typeValue[0], typeValue[1]);
			Frame frame = ArduinoUnoParameterFrames.parse(ArduinoUnoParameterFrames.parameterFrame(1, frameValue));
			assertEquals(typeValue[1], ArduinoUnoParameterFrames.fromFrameValue(typeValue[0], frame.getValue()), typeValue[0]);
		}
	}
	
	private static Reception reception() {
		return new Reception(new String[] {INT, FLOAT, STRING}, new int[] {1, 1, 5}, false);
	}
	
	private static String send(Reception reception, String... lines) {
		String answer = null;
		for (String line : lines) {
			String result = reception.accept(ArduinoUnoParameterFrames.parse(line));
			if(result != null) answer = result;
		}
		return answer;
	}
	
	@Test
	public void completeReceptionIsAcknowledged() {
		Reception reception = reception();
		// Any order, data frames ignored
		assertEquals("A3\n", send(reception, "P2:+1.500000E+00", "1:+2.0E+00", "P3:abc", "P1:42", "E3"));
		assertTrue(reception.isEnded());
		assertTrue(reception.isAcknowledged());
		assertEquals("42", reception.getValue(1));
		assertEquals("1.5", reception.getValue(2));
		assertEquals("abc", reception.getValue(3));
		assertNull(reception.getValue(4));
	}
	
	@Test
	public void lastValueOfAParameterIsKept() {
		Reception reception = reception();
		assertEquals("A3\n", send(reception, "P1:1", "P1:2", "P2:0", "P3:", "E3"));
		assertEquals("2", reception.getValue(1));
		assertEquals("", reception.getValue(3));
	}
	
	@Test
	public void missingParameterIsAnError() {
		Reception reception = reception();
		assertEquals("N2\n", send(reception, "P1:1", "P3:abc", "E3"));
		assertTrue(reception.isEnded());
		assertFalse(reception.isAcknowledged());
		assertNull(reception.getValue(1));
	}
	
	@Test
	public void invalidValueIsAnError() {
		// int on 16 bits, string longer than 5 characters
		assertEquals("N1\n", send(reception(), "P1:40000", "P2:1", "P3:abc", "E3"));
		assertEquals("N3\n", send(reception(), "P1:1", "P2:1", "P3:abcdef", "E3"));
		assertEquals("N2\n", send(reception(), "P1:1", "P2:abc", "P3:abc", "E3"));
	}
	
	@Test
	public void unknownParameterIsAnError() {
		assertEquals("N4\n", send(reception(), "P1:1", "P2:1", "P3:abc", "P4:1", "E3"));
	}
	
	@Test
	public void wrongCountIsACountError() {
		assertEquals("N0\n", send(reception(), "P1:1", "P2:1", "E2"));
		assertEquals("N0\n", send(reception(), "P1:1", "P2:1", "P3:abc", "E4"));
	}
	
	@Test
	public void framesAfterEndAreIgnored() {
		Reception reception = reception();
		assertEquals("A3\n", send(reception, "P1:1", "P2:1", "P3:abc", "E3"));
		assertNull(reception.accept(ArduinoUnoParameterFrames.parse("E3")));
		assertNull(reception.accept(ArduinoUnoParameterFrames.parse("P1:2")));
		assertEquals("1", reception.getValue(1));
	}
	
	@Test
	public void receptionWithoutParameter() {
		Reception reception = new Reception(new String[0], new int[0], false);
		assertEquals("A0\n", send(reception, "E0"));
	}

}
