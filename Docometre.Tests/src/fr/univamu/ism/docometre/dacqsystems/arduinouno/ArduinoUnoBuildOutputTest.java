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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoBuildOutput.Error;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoBuildOutput.Kind;

/**
 * Tests of the analysis of the Arduino build output, without Eclipse : run them as plain JUnit 5 tests.
 */
public class ArduinoUnoBuildOutputTest {
	
	// Output of arduino-cli for a variable named "signal" (R4 WiFi) : the link fails
	private static final String LINKER_OUTPUT = 
			"c:/users/user/appdata/local/arduino15/packages/arduino/tools/arm-none-eabi-gcc/7-2017q4/bin/../lib/gcc/arm-none-eabi/7.2.1/../../../../arm-none-eabi/lib/thumb/v7e-m/fpv4-sp/hard\\libc_nano.a(lib_a-signal.o): In function `signal':\n" + 
			"signal.c:(.text.signal+0x0): multiple definition of `signal'\n" + 
			"C:\\Users\\user\\AppData\\Local\\Temp\\arduino\\sketches\\E7CD\\sketch\\procArduino.ino.cpp.o:C:\\Users\\user\\Desktop\\DOCO_W\\testParams\\BinSource\\procArduino/procArduino.ino:78: first defined here\n" + 
			"collect2.exe: error: ld returned 1 exit status\n" + 
			"\n" + 
			"Used library Version Path\n" + 
			"WDT          0.0.1   C:\\Users\\user\\AppData\\Local\\Arduino15\\packages\\arduino\\hardware\\renesas_uno\\1.5.2\\libraries\\WDT\n" + 
			"\n" + 
			"Error during build: exit status 1\n";
	
	// Output of arduino-cli for a variable named "F" (R4 WiFi) : the compilation fails
	private static final String COMPILER_OUTPUT = 
			"C:\\Users\\user\\Desktop\\DOCO_W\\testParams\\BinSource\\procArduino\\procArduino.ino:76:5: error: 'int F' redeclared as different kind of symbol\n" + 
			" int F;\n" + 
			"     ^\n" + 
			"C:\\Users\\user\\Desktop\\DOCO_W\\testParams\\BinSource\\procArduino\\procArduino.ino:125:11: error: overloaded function with no contextual type information\n" + 
			"Error during build: exit status 1\n";
	
	@Test
	public void linkerErrorIsFound() {
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze(LINKER_OUTPUT, 1);
		assertTrue(output.hasFailed());
		List<Error> errors = output.getErrors();
		assertEquals(1, errors.size());
		assertEquals(Kind.LINKER, errors.get(0).getKind());
		assertEquals("multiple definition of `signal'", errors.get(0).getMessage());
		assertEquals(78, errors.get(0).getLine());
	}
	
	@Test
	public void linkerErrorIsFoundWithoutExitCode() {
		// Exit code not given by the shell : the output is enough
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze(LINKER_OUTPUT, 0);
		assertTrue(output.hasFailed());
		assertEquals(Kind.LINKER, output.getErrors().get(0).getKind());
	}
	
	@Test
	public void undefinedReferenceIsAtItsSketchLine() {
		String text = "C:\\temp\\sketch\\proc.ino.cpp.o: In function `loop':\n" + 
					  "C:\\work\\BinSource\\proc/proc.ino:120: undefined reference to `myFunction()'\n" + 
					  "collect2.exe: error: ld returned 1 exit status\n";
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze(text, 1);
		assertTrue(output.hasFailed());
		assertEquals(1, output.getErrors().size());
		assertEquals(120, output.getErrors().get(0).getLine());
		assertEquals("undefined reference to `myFunction()'", output.getErrors().get(0).getMessage());
	}
	
	@Test
	public void compilerErrorsAreFound() {
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze(COMPILER_OUTPUT, 1);
		assertTrue(output.hasFailed());
		List<Error> errors = output.getErrors();
		assertEquals(2, errors.size());
		assertEquals(Kind.COMPILER, errors.get(0).getKind());
		assertEquals(76, errors.get(0).getLine());
		assertEquals(5, errors.get(0).getColumn());
		assertEquals("'int F' redeclared as different kind of symbol", errors.get(0).getMessage());
		assertEquals(125, errors.get(1).getLine());
	}
	
	@Test
	public void failureWithoutErrorMessage() {
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze("Error during build: exit status 1\n", 1);
		assertTrue(output.hasFailed());
		assertEquals(Kind.BUILD, output.getErrors().get(0).getKind());
		assertEquals(1, output.getErrors().get(0).getLine());
		assertEquals("Error during build: exit status 1", output.getErrors().get(0).getMessage());
		// Only the exit code
		output = ArduinoUnoBuildOutput.analyze("", 2);
		assertTrue(output.hasFailed());
		assertEquals("exit code 2", output.getErrors().get(0).getMessage());
	}
	
	@Test
	public void successfulBuild() {
		String text = "Sketch uses 51234 bytes (19%) of program storage space. Maximum is 262144 bytes.\n" + 
					  "Global variables use 6744 bytes (20%) of dynamic memory, leaving 26024 bytes for local variables. Maximum is 32768 bytes.\n" + 
					  "C:\\work\\proc.ino:12:3: warning: unused variable 'x' [-Wunused-variable]\n";
		ArduinoUnoBuildOutput output = ArduinoUnoBuildOutput.analyze(text, 0);
		assertFalse(output.hasFailed());
		assertTrue(output.getErrors().isEmpty());
		assertFalse(ArduinoUnoBuildOutput.analyze(null, 0).hasFailed());
	}

}
