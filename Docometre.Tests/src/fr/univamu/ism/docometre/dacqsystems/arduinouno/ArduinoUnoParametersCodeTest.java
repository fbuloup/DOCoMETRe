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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the generated Arduino Uno code to receive the parameters, without Eclipse : run them as plain JUnit 5 tests.
 * The generated code is compiled and run on the host with a mocked Serial when g++ is available.
 */
public class ArduinoUnoParametersCodeTest {
	
	private static final String[] NAMES = {"pBool", "pChar", "pInt", "pLong", "pFloat", "pString"};
	private static final String[] TYPES = {ArduinoUnoVariableProperties.BOOL, ArduinoUnoVariableProperties.CHAR, ArduinoUnoVariableProperties.INT,
										   ArduinoUnoVariableProperties.LONG, ArduinoUnoVariableProperties.FLOAT, ArduinoUnoVariableProperties.STRING};
	private static final int[] SIZES = {1, 1, 1, 1, 1, 8};
	// Strings are never propagated
	private static final boolean[] PROPAGATED = {true, true, true, true, true, false};
	
	@Test
	public void withoutParameterCodeIsUnchanged() {
		assertEquals("", ArduinoUnoParametersCode.getDeclarationCode(new String[0], new String[0], new int[0]));
		// Same wait for 'r' char as before parameters management
		String expected = "\t\twhile (!startLoop) {\n" + 
						  "\t\t\t\tif(Serial.available()) {\n" + 
						  "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) == 'r';\n" + 
						  "\t\t\t\t}\n" + 
						  "\t\t}\n";
		assertEquals(expected, ArduinoUnoParametersCode.getWaitForStartCode(0, "\t\t"));
	}
	
	@Test
	public void declarationCodeHasOneCasePerParameter() {
		String code = ArduinoUnoParametersCode.getDeclarationCode(NAMES, TYPES, SIZES);
		assertTrue(code.contains("const int PARAMETERS_COUNT = 6;"));
		for (int i = 1; i <= NAMES.length; i++) assertTrue(code.contains("case " + i + " : {"), "case " + i);
		assertTrue(code.contains("strcpy(pString, value);"));
		assertTrue(code.contains("if(strlen(value) > 8) return false;"));
	}
	
	@Test
	public void withoutPropagatedParameterNoPropagationCode() {
		assertEquals("", ArduinoUnoParametersCode.getPropagationDeclarationCode(NAMES, TYPES, new boolean[NAMES.length], false));
	}
	
	@Test
	public void floatsArePropagatedWithDtostreOnR3() {
		assertTrue(ArduinoUnoParametersCode.getPropagationDeclarationCode(NAMES, TYPES, PROPAGATED, true).contains("dtostre(pFloat, propagationBuffer, 6"));
		assertTrue(ArduinoUnoParametersCode.getPropagationDeclarationCode(NAMES, TYPES, PROPAGATED, false).contains("snprintf(propagationBuffer, sizeof(propagationBuffer), \"%+.6E\", pFloat);"));
	}
	
	@Test
	public void generatedCodeRunsOnHost(@TempDir Path directory) throws Exception {
		assumeTrue(run(directory, "g++", "--version") == 0, "g++ not available");
		String generated = ArduinoUnoParametersCode.getDeclarationCode(NAMES, TYPES, SIZES)
						 + ArduinoUnoParametersCode.getPropagationDeclarationCode(NAMES, TYPES, PROPAGATED, false)
						 + "void waitForStart() {\n" + ArduinoUnoParametersCode.getWaitForStartCode(NAMES.length, "\t\t") + "}\n";
		Files.writeString(directory.resolve("generated.inc"), generated, StandardCharsets.US_ASCII);
		try (InputStream harness = getClass().getResourceAsStream("ArduinoUnoParametersCodeHarness.cpp")) {
			Files.write(directory.resolve("harness.cpp"), harness.readAllBytes());
		}
		assertEquals(0, run(directory, "g++", "-Wall", "-DINT16", "-o", "harness", "harness.cpp"), "compilation");
		assertEquals(0, run(directory, directory.resolve("harness").toString()), "failures");
	}
	
	private static int run(Path directory, String... command) throws Exception {
		try {
			Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
														 .redirectOutput(new File(directory.toFile(), "output.txt")).start();
			if(!process.waitFor(60, TimeUnit.SECONDS)) return -1;
			int exitValue = process.exitValue();
			if(exitValue != 0) System.out.println(Files.readString(directory.resolve("output.txt")));
			return exitValue;
		} catch (java.io.IOException e) {
			return -1;
		}
	}

}
