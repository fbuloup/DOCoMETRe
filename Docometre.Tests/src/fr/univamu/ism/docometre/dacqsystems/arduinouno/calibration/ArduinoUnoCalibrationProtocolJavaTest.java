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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.univamu.ism.docometre.dacqsystems.arduinouno.calibration.ArduinoUnoCalibrationProtocol.Input;

/**
 * Tests of the computer side of the calibration firmware protocol, without Eclipse : run them as plain JUnit 5 tests.
 * The commands are also executed by the firmware protocol (CalibrationProtocol.h) compiled on the host when g++ is available.
 */
public class ArduinoUnoCalibrationProtocolJavaTest {
	
	private static final String PROTOCOL_FILE = "Libraries/includes/ArduinoUnoCalibrationFiles/ArduinoUnoCalibration/CalibrationProtocol.h";
	
	private static List<String> sampleCommands() {
		List<Input> inputs = Arrays.asList(Input.analog(0), Input.analog(5), Input.ads1115(0x49, 2, ArduinoUnoCalibrationProtocol.getADS1115Gain("2/3")),
										   Input.ads1115(0x49, 3, ArduinoUnoCalibrationProtocol.getADS1115Gain("16")), Input.digital(7, true), Input.digital(8, false));
		List<int[]> modules = new ArrayList<>();
		modules.add(new int[] {0x49, 7});
		return ArduinoUnoCalibrationProtocol.getConfigurationCommands("DEFAULT", modules, inputs, Arrays.asList(13, 12), Arrays.asList(3, 11));
	}
	
	@Test
	public void configurationCommands() {
		assertEquals(Arrays.asList("C", "FDEFAULT", "T20", "B0x49:7", "A0", "A5", "S0x49:2:0", "S0x49:3:16", "D7:1", "D8:0", "P13", "P12", "Q3", "Q11"), sampleCommands());
	}
	
	@Test
	public void withoutAnalogInputReferenceIsNotSent() {
		List<String> commands = ArduinoUnoCalibrationProtocol.getConfigurationCommands(null, new ArrayList<>(), Arrays.asList(Input.digital(2, false)), Arrays.asList(), Arrays.asList());
		assertEquals(Arrays.asList("C", "T20", "D2:0"), commands);
	}
	
	@Test
	public void referencesOfBoards() {
		for (String reference : new String[] {"DEFAULT", "INTERNAL", "EXTERNAL"}) {
			assertTrue(ArduinoUnoCalibrationProtocol.isReferenceOfBoard(reference, "R3"), reference);
			assertFalse(ArduinoUnoCalibrationProtocol.isReferenceOfBoard(reference, "R4"), reference);
		}
		for (String reference : new String[] {"AR_DEFAULT", "AR_INTERNAL", "AR_EXTERNAL"}) {
			assertTrue(ArduinoUnoCalibrationProtocol.isReferenceOfBoard(reference, "R4"), reference);
			assertFalse(ArduinoUnoCalibrationProtocol.isReferenceOfBoard(reference, "R3"), reference);
		}
	}
	
	@Test
	public void outputsCommands() {
		assertEquals("W13:1", ArduinoUnoCalibrationProtocol.getDigitalOutputCommand(13, true));
		assertEquals("W13:0", ArduinoUnoCalibrationProtocol.getDigitalOutputCommand(13, false));
		assertEquals("O3:128", ArduinoUnoCalibrationProtocol.getAnalogOutputCommand(3, 127.6));
		assertEquals("O3:0", ArduinoUnoCalibrationProtocol.getAnalogOutputCommand(3, -2));
		assertEquals("O3:255", ArduinoUnoCalibrationProtocol.getAnalogOutputCommand(3, 300));
	}
	
	@Test
	public void identification() {
		assertEquals("R3", ArduinoUnoCalibrationProtocol.getBoard("CAL1:R3"));
		assertEquals("R4", ArduinoUnoCalibrationProtocol.getBoard("CAL1:R4\r"));
		assertNull(ArduinoUnoCalibrationProtocol.getBoard("CAL2:R3"));
		assertTrue(ArduinoUnoCalibrationProtocol.isIdentification("CAL2:R3"));
		assertNull(ArduinoUnoCalibrationProtocol.getBoard("CAL1:R5"));
		assertNull(ArduinoUnoCalibrationProtocol.getBoard("w"));
	}
	
	@Test
	public void measures() {
		assertArrayEquals(new long[] {1023, -32768, 1}, ArduinoUnoCalibrationProtocol.getMeasures("M:1023;-32768;1\r", 3));
		assertArrayEquals(new long[0], ArduinoUnoCalibrationProtocol.getMeasures("M:", 0));
		assertNull(ArduinoUnoCalibrationProtocol.getMeasures("M:1;2", 3));
		assertNull(ArduinoUnoCalibrationProtocol.getMeasures("M:1;;3", 3));
		assertNull(ArduinoUnoCalibrationProtocol.getMeasures("M:1;a;3", 3));
		assertNull(ArduinoUnoCalibrationProtocol.getMeasures("K", 0));
	}
	
	@Test
	public void addressesAndGains() {
		assertEquals("0x4A", ArduinoUnoCalibrationProtocol.getAddressText(0x4A));
		assertEquals(0x4B, ArduinoUnoCalibrationProtocol.getAddress("0x4B"));
		assertEquals(0, ArduinoUnoCalibrationProtocol.getADS1115Gain("2/3"));
		assertEquals(4, ArduinoUnoCalibrationProtocol.getADS1115Gain("4"));
		assertEquals(16, ArduinoUnoCalibrationProtocol.getADS1115Gain("16"));
	}
	
	@Test
	public void voltages() {
		assertEquals(5.0 * 1023 / 1024, ArduinoUnoCalibrationProtocol.getAnalogVoltage(1023, "DEFAULT", "R3"), 1e-9);
		assertEquals(2.5, ArduinoUnoCalibrationProtocol.getAnalogVoltage(8192, "AR_DEFAULT", "R4"), 1e-9);
		assertEquals(1.1 / 2, ArduinoUnoCalibrationProtocol.getAnalogVoltage(512, "INTERNAL", "R3"), 1e-9);
		assertEquals(1.5 / 2, ArduinoUnoCalibrationProtocol.getAnalogVoltage(8192, "AR_INTERNAL", "R4"), 1e-9);
		assertTrue(Double.isNaN(ArduinoUnoCalibrationProtocol.getAnalogVoltage(512, "EXTERNAL", "R3")));
		assertTrue(Double.isNaN(ArduinoUnoCalibrationProtocol.getAnalogVoltage(8192, "AR_EXTERNAL", "R4")));
		// Reference of the other board
		assertTrue(Double.isNaN(ArduinoUnoCalibrationProtocol.getAnalogVoltage(8192, "DEFAULT", "R4")));
		assertTrue(Double.isNaN(ArduinoUnoCalibrationProtocol.getAnalogVoltage(512, "AR_DEFAULT", "R3")));
		assertEquals(-6.144, ArduinoUnoCalibrationProtocol.getADS1115Voltage(-32768, 0), 1e-9);
		assertEquals(0.128, ArduinoUnoCalibrationProtocol.getADS1115Voltage(16384, 16), 1e-9);
	}
	
	@Test
	public void firmwareAcceptsCommands(@TempDir Path directory) throws Exception {
		assumeTrue(run(directory, null, "g++", "--version") == 0, "g++ not available");
		Path protocolFile = findProtocolFile();
		assumeTrue(protocolFile != null, PROTOCOL_FILE + " not found");
		Files.copy(protocolFile, directory.resolve("CalibrationProtocol.h"));
		try (InputStream harness = getClass().getResourceAsStream("/fr/univamu/ism/docometre/dacqsystems/arduinouno/ArduinoUnoCalibrationProtocolHarness.cpp")) {
			Files.write(directory.resolve("harness.cpp"), harness.readAllBytes());
		}
		assertEquals(0, run(directory, null, "g++", "-Wall", "-o", "harness", "harness.cpp"), "compilation");
		List<String> commands = new ArrayList<>(sampleCommands());
		commands.add(ArduinoUnoCalibrationProtocol.START);
		commands.add(ArduinoUnoCalibrationProtocol.getDigitalOutputCommand(13, true));
		commands.add(ArduinoUnoCalibrationProtocol.getAnalogOutputCommand(11, 200));
		Path input = directory.resolve("commands.txt");
		Files.writeString(input, ArduinoUnoCalibrationProtocol.IDENTIFICATION + "\n" + String.join("\n", commands) + "\n", StandardCharsets.US_ASCII);
		assertEquals(0, run(directory, input, directory.resolve("harness").toString(), "--stdin"), "execution");
		List<String> answers = Files.readAllLines(directory.resolve("output.txt"));
		// Identification, one acknowledge per configuration command and for start, no answer for outputs, then measures
		assertEquals("R3", ArduinoUnoCalibrationProtocol.getBoard(answers.get(0)));
		for (int i = 1; i <= commands.size() - 2; i++) assertEquals(ArduinoUnoCalibrationProtocol.ACKNOWLEDGE, answers.get(i), commands.get(i - 1));
		assertEquals(commands.size(), answers.size());
		assertArrayEquals(new long[] {1023, 0, 0, 0, 0, 0}, ArduinoUnoCalibrationProtocol.getMeasures(answers.get(answers.size() - 1), 6));
	}
	
	/*
	 * The Libraries project is next to the tests project : searched from the working directory and its parents
	 */
	private static Path findProtocolFile() {
		Path folder = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		while (folder != null) {
			Path file = folder.resolve(PROTOCOL_FILE);
			if(Files.isRegularFile(file)) return file;
			folder = folder.getParent();
		}
		return null;
	}
	
	private static int run(Path directory, Path input, String... command) throws Exception {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
														 .redirectOutput(new File(directory.toFile(), "output.txt"));
			if(input != null) processBuilder.redirectInput(input.toFile());
			Process process = processBuilder.start();
			if(!process.waitFor(60, TimeUnit.SECONDS)) return -1;
			int exitValue = process.exitValue();
			if(exitValue != 0) System.out.println(Files.readString(directory.resolve("output.txt")));
			return exitValue;
		} catch (java.io.IOException e) {
			return -1;
		}
	}

}
