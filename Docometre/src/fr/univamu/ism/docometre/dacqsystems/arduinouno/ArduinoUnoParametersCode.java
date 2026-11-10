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
 * Generated Arduino Uno code to receive the values of the parameters of the process, sent by the PC
 * before the start of the process (see {@link ArduinoUnoParameterFrames}). Plain Java class (no Eclipse dependencies).
 * <p>
 * While it waits for the 'r' char (start), the board reads the received lines : a P frame writes the value
 * of a parameter directly in its variable, the E frame is answered with an A frame (all the parameters
 * received and read) or an N frame (error), following the same rules as {@link ArduinoUnoParameterFrames.Reception}.
 * The variables which are parameters are then not initialized to 0 at the start of the process.
 * <p>
 * The number of a parameter is its rank (starting at 1) in DACQConfiguration.getParameters() : the arrays
 * given to these methods must follow this order.
 */
public final class ArduinoUnoParametersCode {
	
	// Longest value in a frame for numeric types : "-2147483648" or "+1.234567E+00"
	private static final int NUMERIC_VALUE_LENGTH = 16;
	
	private ArduinoUnoParametersCode() {
	}
	
	/**
	 * @param names the names of the parameters
	 * @param types their types (see {@link ArduinoUnoVariableProperties#TYPES})
	 * @param sizes their sizes (maximum length for a string)
	 * @return the declarations and functions to add after the declarations of the variables, empty if there is no parameter
	 */
	public static String getDeclarationCode(String[] names, String[] types, int[] sizes) {
		int count = names.length;
		if(count == 0) return "";
		// Longest line : P<n>:<value>, plus terminating null character
		int maxValueLength = NUMERIC_VALUE_LENGTH;
		for (int i = 0; i < count; i++) {
			if(ArduinoUnoVariableProperties.STRING.equals(types[i])) maxValueLength = Math.max(maxValueLength, sizes[i]);
		}
		int lineLength = 1 + Integer.toString(count).length() + 1 + maxValueLength + 1;
		
		StringBuilder code = new StringBuilder();
		code.append("// ******** Parameters of the process : values sent by the PC before the start (see startProcess())\n");
		code.append("// Frames : P<n>:<value> (value of parameter n), E<count> (end), answered with A<count> (acknowledged)\n");
		code.append("// or N<n> (error on parameter n, N0 : number of parameters is not the one of this process)\n");
		code.append("const int PARAMETERS_COUNT = ").append(count).append(";\n");
		for (int i = 0; i < count; i++) {
			code.append("// Parameter ").append(i + 1).append(" : ").append(names[i]).append(" (").append(types[i]).append(")\n");
		}
		code.append("// Line being received\n");
		code.append("char parameterLine[").append(lineLength).append("];\n");
		code.append("int parameterLineLength;\n");
		code.append("bool parameterLineOverflow;\n");
		code.append("// Received parameters (one bit per parameter)\n");
		code.append("byte parametersReceived[").append((count + 7) / 8).append("];\n");
		code.append("// Number of the first parameter in error (unknown or unreadable value), -1 if none\n");
		code.append("int parameterError;\n");
		code.append("// True when the E frame has been answered\n");
		code.append("bool parametersEnded;\n");
		code.append("// True when the parameters have been acknowledged (A frame sent)\n");
		code.append("bool parametersAcknowledged;\n\n");
		
		code.append("// Initializes the reception of the parameters\n");
		code.append("void resetParametersReception() {\n");
		code.append("\t\tparameterLineLength = 0;\n");
		code.append("\t\tparameterLineOverflow = false;\n");
		code.append("\t\tmemset(parametersReceived, 0, sizeof(parametersReceived));\n");
		code.append("\t\tparameterError = -1;\n");
		code.append("\t\tparametersEnded = false;\n");
		code.append("\t\tparametersAcknowledged = false;\n");
		code.append("}\n\n");
		
		code.append("// Writes the value of parameter number n in its variable : returns false if n is unknown or the value can't be read\n");
		code.append("bool readParameterValue(int n, const char* value) {\n");
		code.append("\t\tchar* end;\n");
		code.append("\t\tswitch(n) {\n");
		for (int i = 0; i < count; i++) {
			code.append("\t\t\t\tcase ").append(i + 1).append(" : {\n");
			code.append(getReadValueCode(names[i], types[i], sizes[i], "\t\t\t\t\t\t"));
			code.append("\t\t\t\t}\n");
		}
		code.append("\t\t}\n");
		code.append("\t\treturn false;\n");
		code.append("}\n\n");
		
		code.append("// Processes a received line : P frame (value of a parameter) or E frame (end : answer A or N)\n");
		code.append("void processParameterLine() {\n");
		code.append("\t\tif(parameterLine[0] == 'P') {\n");
		code.append("\t\t\t\t// New sending of the parameters after an answer\n");
		code.append("\t\t\t\tif(parametersEnded) resetParametersReception();\n");
		code.append("\t\t\t\tchar* separator = strchr(parameterLine, ':');\n");
		code.append("\t\t\t\tint n = -1;\n");
		code.append("\t\t\t\tif(separator != NULL) {\n");
		code.append("\t\t\t\t\t\t*separator = '\\0';\n");
		code.append("\t\t\t\t\t\tn = atoi(parameterLine + 1);\n");
		code.append("\t\t\t\t}\n");
		code.append("\t\t\t\tif(n >= 1 && n <= PARAMETERS_COUNT && readParameterValue(n, separator + 1)) parametersReceived[(n - 1)/8] |= (1 << ((n - 1)%8));\n");
		code.append("\t\t\t\telse if(parameterError == -1) parameterError = (n < 0) ? 0 : n;\n");
		code.append("\t\t}\n");
		code.append("\t\tif(parameterLine[0] == 'E') {\n");
		code.append("\t\t\t\tint count = atoi(parameterLine + 1);\n");
		code.append("\t\t\t\t// -1 : acknowledged, otherwise number of the parameter in error\n");
		code.append("\t\t\t\tint answer = -1;\n");
		code.append("\t\t\t\tif(count != PARAMETERS_COUNT) answer = 0;\n");
		code.append("\t\t\t\telse if(parameterError != -1) answer = parameterError;\n");
		code.append("\t\t\t\telse {\n");
		code.append("\t\t\t\t\t\tfor(int i = 0; i < PARAMETERS_COUNT; i++) {\n");
		code.append("\t\t\t\t\t\t\t\tif(!(parametersReceived[i/8] & (1 << (i%8)))) {\n");
		code.append("\t\t\t\t\t\t\t\t\t\tanswer = i + 1;\n");
		code.append("\t\t\t\t\t\t\t\t\t\tbreak;\n");
		code.append("\t\t\t\t\t\t\t\t}\n");
		code.append("\t\t\t\t\t\t}\n");
		code.append("\t\t\t\t}\n");
		code.append("\t\t\t\tif(answer == -1) {\n");
		code.append("\t\t\t\t\t\tSerial.print('A');\n");
		code.append("\t\t\t\t\t\tSerial.println(count);\n");
		code.append("\t\t\t\t\t\tparametersAcknowledged = true;\n");
		code.append("\t\t\t\t} else {\n");
		code.append("\t\t\t\t\t\tSerial.print('N');\n");
		code.append("\t\t\t\t\t\tSerial.println(answer);\n");
		code.append("\t\t\t\t}\n");
		code.append("\t\t\t\tSerial.flush();\n");
		code.append("\t\t\t\tparametersEnded = true;\n");
		code.append("\t\t}\n");
		code.append("}\n\n");
		
		code.append("// Adds a received char to the line, the line is processed at its end\n");
		code.append("void readParameterChar(char c) {\n");
		code.append("\t\tif(c == '\\n' || c == '\\r') {\n");
		code.append("\t\t\t\tif(parameterLineLength > 0 && !parameterLineOverflow) {\n");
		code.append("\t\t\t\t\t\tparameterLine[parameterLineLength] = '\\0';\n");
		code.append("\t\t\t\t\t\tprocessParameterLine();\n");
		code.append("\t\t\t\t}\n");
		code.append("\t\t\t\tparameterLineLength = 0;\n");
		code.append("\t\t\t\tparameterLineOverflow = false;\n");
		code.append("\t\t\t\treturn;\n");
		code.append("\t\t}\n");
		code.append("\t\tif(parameterLineLength < (int)sizeof(parameterLine) - 1) parameterLine[parameterLineLength++] = c;\n");
		code.append("\t\telse parameterLineOverflow = true;\n");
		code.append("}\n\n");
		return code.toString();
	}
	
	/**
	 * @param count the number of parameters
	 * @param indent the indentation of the code
	 * @return the code of startProcess() which waits for the 'r' char (start) and, if there are parameters, receives their values
	 */
	public static String getWaitForStartCode(int count, String indent) {
		StringBuilder code = new StringBuilder();
		if(count == 0) {
			code.append(indent).append("while (!startLoop) {\n");
			code.append(indent).append("\t\tif(Serial.available()) {\n");
			code.append(indent).append("\t\t\t\tstartLoop = ((char)Serial.read()) == 'r';\n");
			code.append(indent).append("\t\t}\n");
			code.append(indent).append("}\n");
			return code.toString();
		}
		code.append(indent).append("// Receive the values of the parameters (P and E frames) until 'r' char (start)\n");
		code.append(indent).append("resetParametersReception();\n");
		code.append(indent).append("while (!startLoop) {\n");
		code.append(indent).append("\t\tif(Serial.available()) {\n");
		code.append(indent).append("\t\t\t\tchar c = (char)Serial.read();\n");
		code.append(indent).append("\t\t\t\t// 'r' at the beginning of a line : start, other chars : parameters frames\n");
		code.append(indent).append("\t\t\t\tif(c == 'r' && parameterLineLength == 0) startLoop = true;\n");
		code.append(indent).append("\t\t\t\telse readParameterChar(c);\n");
		code.append(indent).append("\t\t}\n");
		code.append(indent).append("}\n");
		code.append(indent).append("// Parameters not received : the parameters of the process have changed since its compilation (see ArduinoUnoProcess)\n");
		code.append(indent).append("if(!parametersAcknowledged) {\n");
		code.append(indent).append("\t\tSerial.println(\"N0\");\n");
		code.append(indent).append("\t\tSerial.flush();\n");
		code.append(indent).append("}\n");
		return code.toString();
	}
	
	private static String getReadValueCode(String name, String type, int size, String indent) {
		StringBuilder code = new StringBuilder();
		switch (type) {
		case ArduinoUnoVariableProperties.BOOL:
			code.append(indent).append("if(strcmp(value, \"1\") == 0) ").append(name).append(" = true;\n");
			code.append(indent).append("else if(strcmp(value, \"0\") == 0) ").append(name).append(" = false;\n");
			code.append(indent).append("else return false;\n");
			code.append(indent).append("return true;\n");
			break;
		case ArduinoUnoVariableProperties.CHAR:
			code.append(indent).append("if(strlen(value) != 1) return false;\n");
			code.append(indent).append(name).append(" = value[0];\n");
			code.append(indent).append("return true;\n");
			break;
		case ArduinoUnoVariableProperties.INT:
		case ArduinoUnoVariableProperties.LONG:
			String min = ArduinoUnoVariableProperties.INT.equals(type) ? "INT_MIN" : "LONG_MIN";
			String max = ArduinoUnoVariableProperties.INT.equals(type) ? "INT_MAX" : "LONG_MAX";
			code.append(indent).append("errno = 0;\n");
			code.append(indent).append("long longValue = strtol(value, &end, 10);\n");
			code.append(indent).append("if(end == value || *end != '\\0' || errno == ERANGE || longValue < ").append(min).append(" || longValue > ").append(max).append(") return false;\n");
			code.append(indent).append(name).append(" = longValue;\n");
			code.append(indent).append("return true;\n");
			break;
		case ArduinoUnoVariableProperties.FLOAT:
			code.append(indent).append("double doubleValue = strtod(value, &end);\n");
			code.append(indent).append("if(end == value || *end != '\\0') return false;\n");
			code.append(indent).append(name).append(" = doubleValue;\n");
			code.append(indent).append("return true;\n");
			break;
		case ArduinoUnoVariableProperties.STRING:
			code.append(indent).append("if(strlen(value) > ").append(size).append(") return false;\n");
			code.append(indent).append("strcpy(").append(name).append(", value);\n");
			code.append(indent).append("return true;\n");
			break;
		default:
			code.append(indent).append("return false;\n");
			break;
		}
		return code.toString();
	}

}
