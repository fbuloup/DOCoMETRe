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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;

public class VariablesCodeGenerationDelegate {
	
	private static ArduinoUnoProcess process;

	public static String getCode(ArduinoUnoProcess process, ArduinoUnoCodeSegmentProperties codeSegment) {
		VariablesCodeGenerationDelegate.process = process;
		if(codeSegment.equals(ArduinoUnoCodeSegmentProperties.DECLARATION)) return getDeclarationCode();
		if(codeSegment.equals(ArduinoUnoCodeSegmentProperties.INITIALIZATION)) return getInitialisationCode();
		if(codeSegment.equals(ArduinoUnoCodeSegmentProperties.TRANSFER)) return getTransferCode();
//		if(codeSegment.equals(ArduinoUnoCodeSegmentProperties.FINISH)) return getFinalisationCode();
		return "";
	}

	private static String getInitialisationCode() {
		String  code = "";
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)process.getDACQConfiguration()).getVariables();
		for (ArduinoUnoVariable variable : variables) {
			String name = variable.getProperty(ChannelProperties.NAME);
			String transfer = variable.getProperty(ChannelProperties.TRANSFER);
			boolean isTransfered = Boolean.valueOf(transfer);
			if(isTransfered) {
				code = code + "\t\tlastTransferTime_" + name + " = 0;\n";
			}
		}
		return code + "\n";
	}

	private static String getTransferCode() {
		String  code = "";
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)process.getDACQConfiguration()).getVariables();
		for (ArduinoUnoVariable variable : variables) {
			String name = variable.getProperty(ChannelProperties.NAME);
			String transfer = variable.getProperty(ChannelProperties.TRANSFER);
			String transferNumber = variable.getProperty(ChannelProperties.TRANSFER_NUMBER);
			boolean isTransfered = Boolean.valueOf(transfer);
			String gsfProcess = ((ArduinoUnoDACQConfiguration)process.getDACQConfiguration()).getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			String sfVariable = variable.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			float sfFloat = Float.parseFloat(sfVariable);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
			
			if(isTransfered) {
				String type = variable.getProperty(ArduinoUnoVariableProperties.TYPE);
				
				code = code + "\t\t\t\t\t\t// Transfer " + name + "\n";
				code = code + "\t\t\t\t\t\tif(transfer_" + name + " == " + frequencyRatio + ") {\n";
				
				if(type.equals(ArduinoUnoVariableProperties.CHAR)) {
					code = code + "\t\t\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%c\"," + transferNumber + ", (loopTime_MS - lastTransferTime_" + name + "), " + name + ");\n";
				}
				if(type.equals(ArduinoUnoVariableProperties.INT) || type.equals(ArduinoUnoVariableProperties.BOOL)) {
					code = code + "\t\t\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\"," + transferNumber + ", (loopTime_MS - lastTransferTime_" + name + "), " + name + ");\n";
				}
				if(type.equals(ArduinoUnoVariableProperties.LONG)) {
					code = code + "\t\t\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%lu\"," + transferNumber + ", (loopTime_MS - lastTransferTime_" + name + "), " + name + ");\n";
				}
				if(type.equals(ArduinoUnoVariableProperties.FLOAT)) {
					code = code + "\t\t\t\t\t\t\t\tdtostre(" + name + ", temporaryBuffer, 6, DTOSTR_ALWAYS_SIGN + DTOSTR_PLUS_SIGN + DTOSTR_UPPERCASE);\n";
					code = code + "\t\t\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%s\"," + transferNumber + ", (loopTime_MS - lastTransferTime_" + name + "), temporaryBuffer);\n";
				}
				
				code = code + "\t\t\t\t\t\t\t\tSerial.println(serialMessage);\n";
				code = code + "\t\t\t\t\t\t\t\tlastTransferTime_" + name + " = loopTime_MS;\n";
				code = code + "\t\t\t\t\t\t\t\ttransfer_" + name + " = 0;\n";
				code = code + "\t\t\t\t\t\t}\n";
				code = code + "\t\t\t\t\t\ttransfer_" + name + " += 1;\n\n";
			}
			
		}
		return code;
	}

	private static String getDeclarationCode() {
		String  code = "";
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)process.getDACQConfiguration()).getVariables();
		for (ArduinoUnoVariable variable : variables) {
			String name = variable.getProperty(ChannelProperties.NAME);
			String type = variable.getProperty(ArduinoUnoVariableProperties.TYPE);
			String size = variable.getProperty(ArduinoUnoVariableProperties.SIZE);
			String transfer = variable.getProperty(ChannelProperties.TRANSFER);
			boolean isTransfered = Boolean.valueOf(transfer);
			String gsfProcess = ((ArduinoUnoDACQConfiguration)process.getDACQConfiguration()).getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			String sfVariable = variable.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			float sfFloat = Float.parseFloat(sfVariable);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
			
			code = code + "// ******** >>>> Variable " + name;
			code = code + (size.equals("1")?" scalaire,":" tableau,");
			
			if(type.equals(ArduinoUnoVariableProperties.BOOL)) {
				code = code + " booléen(s)";
				type = "bool";
			}
			if(type.equals(ArduinoUnoVariableProperties.CHAR)) {
				code = code + " caractère(s)";
				type = "char";
			}
			if(type.equals(ArduinoUnoVariableProperties.INT)) {
				code = code + " entier(s)";
				type = "int";
			}
			if(type.equals(ArduinoUnoVariableProperties.LONG)) {
				code = code + " long(s)";
				type = "long";
			}
			if(type.equals(ArduinoUnoVariableProperties.FLOAT)) {
				code = code + " flottant(s)";
				type = "float";
			}
			size = size.equals("1") ? "" : "[" + size + "]" ;
			code = code + "\n" + type + " " + name + size + ";\n";
			
			if(isTransfered) {
				if(type.equals(ArduinoUnoVariableProperties.FLOAT)) code = code + "char temporaryBuffer[64];\n";
				code = code + "byte transfer_" + name + " = " + frequencyRatio + ";\n";
				code = code + "unsigned long lastTransferTime_" + name + ";\n";
			}
			
		}
		return code;
	}

}
