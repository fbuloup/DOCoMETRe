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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analysis of the output of the Arduino build (arduino-cli or arduino-builder) : errors of the compiler,
 * errors of the linker and failure of the build. Plain Java class (no Eclipse dependencies).
 * <p>
 * The build has failed if the exit code of the build is not 0, or if an error is found in the output.
 * Errors of the linker (e.g. a variable named as a function of the C library) have no column and
 * no "error:" : they are found by their message (multiple definition, undefined reference).
 */
public final class ArduinoUnoBuildOutput {
	
	/**
	 * Kinds of errors
	 */
	public enum Kind {
		/** Error of the compiler, at a line and a column */
		COMPILER,
		/** Error of the linker, at a line of the sketch if known (1 otherwise) */
		LINKER,
		/** Failure of the build without any other error found : at line 1 */
		BUILD
	}
	
	/**
	 * An error found in the output
	 */
	public static final class Error {
		
		private final Kind kind;
		private final int line;
		private final int column;
		private final String message;
		
		private Error(Kind kind, int line, int column, String message) {
			this.kind = kind;
			this.line = line;
			this.column = column;
			this.message = message;
		}
		
		public Kind getKind() {
			return kind;
		}
		
		public int getLine() {
			return line;
		}
		
		public int getColumn() {
			return column;
		}
		
		public String getMessage() {
			return message;
		}
		
	}
	
	// file:line:column: error: message
	private static final Pattern COMPILER_ERROR = Pattern.compile("^(.+):(\\d+):(\\d+):\\s+error:\\s+(.*)$");
	// Errors of the linker
	private static final Pattern LINKER_ERROR = Pattern.compile("(multiple definition of .*|undefined reference to .*)$");
	// Line of the sketch in a message of the linker : ...sketch.ino:78: ...
	private static final Pattern SKETCH_LINE = Pattern.compile("\\.ino:(\\d+):");
	// Lines which tell that the build has failed
	private static final Pattern BUILD_FAILURE = Pattern.compile("(collect2(\\.exe)?: error: .*|ld returned \\d+ exit status.*|^Error during build.*)$");
	
	private final List<Error> errors;
	private final boolean failed;
	
	private ArduinoUnoBuildOutput(List<Error> errors, boolean failed) {
		this.errors = Collections.unmodifiableList(errors);
		this.failed = failed;
	}
	
	/**
	 * @param output the output of the build
	 * @param exitCode the exit code of the build
	 * @return the analysis of the output
	 */
	public static ArduinoUnoBuildOutput analyze(String output, int exitCode) {
		List<Error> errors = new ArrayList<>();
		String failureLine = null;
		String[] lines = output == null ? new String[0] : output.split("\\R");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			Matcher matcher = COMPILER_ERROR.matcher(line);
			if(matcher.matches()) {
				errors.add(new Error(Kind.COMPILER, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), matcher.group(4)));
				continue;
			}
			matcher = LINKER_ERROR.matcher(line);
			if(matcher.find()) {
				// Line of the sketch : in this line (undefined reference) or in the next one (multiple definition : first defined here)
				int sketchLine = 1;
				Matcher sketchLineMatcher = SKETCH_LINE.matcher(line);
				if(sketchLineMatcher.find()) sketchLine = Integer.parseInt(sketchLineMatcher.group(1));
				else if(i + 1 < lines.length && lines[i + 1].contains("first defined here")) {
					sketchLineMatcher = SKETCH_LINE.matcher(lines[i + 1]);
					if(sketchLineMatcher.find()) sketchLine = Integer.parseInt(sketchLineMatcher.group(1));
				}
				errors.add(new Error(Kind.LINKER, sketchLine, 0, matcher.group(1).trim()));
				continue;
			}
			matcher = BUILD_FAILURE.matcher(line);
			if(failureLine == null && matcher.find()) failureLine = matcher.group(1).trim();
		}
		boolean failed = exitCode != 0 || failureLine != null || !errors.isEmpty();
		if(failed && errors.isEmpty()) {
			String message = failureLine != null ? failureLine : "exit code " + exitCode;
			errors.add(new Error(Kind.BUILD, 1, 0, message));
		}
		return new ArduinoUnoBuildOutput(errors, failed);
	}
	
	/**
	 * @return true if the build has failed
	 */
	public boolean hasFailed() {
		return failed;
	}
	
	/**
	 * @return the errors found, in the order of the output (one BUILD error if the build has failed without any other error found)
	 */
	public List<Error> getErrors() {
		return errors;
	}

}
