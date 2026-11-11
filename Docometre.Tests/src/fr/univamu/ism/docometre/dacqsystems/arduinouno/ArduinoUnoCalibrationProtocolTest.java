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
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the serial protocol of the generic calibration firmware (Libraries/includes/ArduinoUnoCalibrationFiles),
 * without Eclipse : run them as plain JUnit 5 tests. CalibrationProtocol.h is compiled and run on the host with
 * mocked hardware functions when g++ is available.
 */
public class ArduinoUnoCalibrationProtocolTest {
	
	private static final String PROTOCOL_FILE = "Libraries/includes/ArduinoUnoCalibrationFiles/ArduinoUnoCalibration/CalibrationProtocol.h";
	
	@Test
	public void protocolRunsOnHost(@TempDir Path directory) throws Exception {
		assumeTrue(run(directory, "g++", "--version") == 0, "g++ not available");
		Path protocolFile = findProtocolFile();
		assumeTrue(protocolFile != null, PROTOCOL_FILE + " not found");
		Files.copy(protocolFile, directory.resolve("CalibrationProtocol.h"));
		try (InputStream harness = getClass().getResourceAsStream("ArduinoUnoCalibrationProtocolHarness.cpp")) {
			Files.write(directory.resolve("harness.cpp"), harness.readAllBytes());
		}
		assertEquals(0, run(directory, "g++", "-Wall", "-Wextra", "-o", "harness", "harness.cpp"), "compilation");
		assertEquals(0, run(directory, directory.resolve("harness").toString()), "failures");
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
