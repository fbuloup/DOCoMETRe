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
package fr.univamu.ism.docometre.dacqsystems.adwin;

import java.util.ArrayList;

import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;

public final class VariablesCodeGenerationDelegate {
	
	private static ADWinProcess process;
	
	public static String getCode(ADWinProcess process, ADWinCodeSegmentProperties codeSegment) {
		VariablesCodeGenerationDelegate.process = process;
		if(codeSegment.equals(ADWinCodeSegmentProperties.DECLARATION)) return getDeclarationCode();
		if(codeSegment.equals(ADWinCodeSegmentProperties.INITIALIZATION)) return getInitialisationCode();
		if(codeSegment.equals(ADWinCodeSegmentProperties.TRANSFER)) return getTransferCode();
		if(codeSegment.equals(ADWinCodeSegmentProperties.FINISH)) return getFinalisationCode();
		return "";
	}
	
	private static String getTransferCode() {
		String  code = "";
		ADWinVariable[] variables = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getVariables();
		if(variables.length > 0) {
			String gsfProcess = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			for (ADWinVariable variable : variables) {
				String sfVariable = variable.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				float sfFloat = Float.parseFloat(sfVariable);
				int frequencyRatio = (int) (gsfFloat/sfFloat);
				String name = variable.getProperty(ChannelProperties.NAME);
				String isParameter = variable.getProperty(ADWinVariableProperties.PARAMETER);
				String transfer = variable.getProperty(ChannelProperties.TRANSFER);
				String auto_transfer = variable.getProperty(ChannelProperties.AUTO_TRANSFER);
				String transferNumber = variable.getProperty(ChannelProperties.TRANSFER_NUMBER);
				boolean isTransfered = Boolean.valueOf(transfer);
				boolean isAutoTransfered = Boolean.valueOf(auto_transfer);
				if(!Boolean.parseBoolean(isParameter) && isTransfered && isAutoTransfered) {
					code = code + "\nIF (TRANSFERT_" + name + " = " + frequencyRatio + ") THEN\n";
					code = code + "\tTRANSFERT_" + name + " = 0\n";
					code = code + "\tIF (FIFO_EMPTY(" +  transferNumber + ") = 0) THEN\n";
					code = code + "\t\tPAR_" +  transferNumber + " = PAR_" +  transferNumber + " + 1\n";
					code = code + "\t\tFIFO_CLEAR(" +  transferNumber + ")\n";
					code = code + "\tENDIF\n";
					code = code + "\t" + name + "_TAB = " + name + "\n";
					code = code + "ENDIF\n";
					code = code + "INC(TRANSFERT_" + name + ")\n";
				} 
			}
		}
		return code;
	}

	private static String getDeclarationCode() {
		String  code = "";
		ADWinVariable[] variables = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getVariables();
		if(variables.length > 0) {
			code = code + "\nREM ******** Début déclarations des variables\n\n";
			int nbParameters = 0;
			int nbParametersString = 0;
			for (ADWinVariable variable : variables) {
				if(variable.isParameter() && (variable.isFloat() || variable.isInt())) nbParameters++;
				if(variable.isParameter() && variable.isString()) nbParametersString++;
			}
			if(nbParameters > 0) {
				code = code + "REM ******** Tableau pour les paramètres\n";
				code = code + "#DEFINE TAB_PARAM DATA_200\n";
				code = code + "DIM TAB_PARAM[" + nbParameters + "] AS FLOAT\n\n";
			}
			if(nbParametersString > 0) {
				code = code + "REM ******** Tableau pour les paramètres chaine de charatères\n";
				code = code + "#DEFINE TAB_PARAM_STRING DATA_199\n";
				code = code + "DIM TAB_PARAM_STRING[1024] AS STRING\n";
				code = code + "DIM index1_" + process.hashCode() + " AS INTEGER\n";
				code = code + "DIM index2_" + process.hashCode() + " AS INTEGER\n";
				code = code + "DIM index3_" + process.hashCode() + " AS INTEGER\n\n";
			}
			for (ADWinVariable variable : variables) {
				String name = variable.getProperty(ChannelProperties.NAME);
				String type = variable.getProperty(ADWinVariableProperties.TYPE);
				String size = variable.getProperty(ADWinVariableProperties.SIZE);
				String isParameter = variable.getProperty(ADWinVariableProperties.PARAMETER);
				String transfer = variable.getProperty(ChannelProperties.TRANSFER);
				String transferNumber = variable.getProperty(ChannelProperties.TRANSFER_NUMBER);
				String bufferSize = variable.getProperty(ChannelProperties.BUFFER_SIZE);
				boolean isTransfered = Boolean.valueOf(transfer);
				if(!Boolean.parseBoolean(isParameter)) {
					code = code + "REM ******** >>>> Variable " + name;
					code = code + (size.equals("1")?" scalaire":" tableau");
					code = code + (type.equals(ADWinVariableProperties.INT)?", entier(s)":type.equals(ADWinVariableProperties.FLOAT)?", flottant(s)":", chaine de charactère(s)") ;
					
					code = code + "\n";
					
					type = type.equals(ADWinVariableProperties.INT)?"LONG":type.equals(ADWinVariableProperties.FLOAT)?"FLOAT":"STRING";
					size = size.equals("1")?type.equalsIgnoreCase(ADWinVariableProperties.STRING)?"[1]":"":"[" + size + "]";
					code = code + "DIM " + name + size + " AS " + type +  "\n";
					if(isTransfered){
						code = code + "\nREM ******** Variables pour transfert de: " + name + " \'Utilisées par Transfert\n";
						code = code + "#DEFINE " + name + "_TAB DATA_" + transferNumber + " \'Utilisé par Transfert\n";
						code = code + "DIM " + name + "_TAB[" + bufferSize + "] AS FLOAT AS FIFO \'Utilisé par Transfert\n";
						code = code + "DIM TRANSFERT_" + name + " AS LONG \'Utilisé par Transfert\n\n";
					}
					
				} else {
					if(variable.isFloat() || variable.isInt()) {
						code = code + "REM ******** >>>> Parametre " + name;
						code = code + (type.equals(ADWinVariableProperties.INT)?", entier":", flottant") + "\n";
						type = type.equals(ADWinVariableProperties.INT)?"LONG":"FLOAT";
						code = code + "DIM " + name + " AS " + type +  "\n";
					}
					if(variable.isString()) {
						code = code + "REM ******** >>>> Parametre chaine de charactères " + name;
						String value = variable.getProperty(ADWinVariableProperties.SIZE);
						code = code + "\nDIM " + name + "[" + value + "] AS STRING\n";
					}
				}
			}
			code = code + "\nREM ******** Fin déclarations des variables\n";
		}
		return code;
	}
	
	private static String getInitialisationCode() {
		String  code = "";
		ADWinVariable[] variables = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getVariables();
		if(variables.length > 0) {
			String gsfProcess = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			code = code + "\nREM ******** Début initialisation des variables\n\n";
			ArrayList<ADWinVariable> parameters = new ArrayList<>();
			ArrayList<ADWinVariable> parametersString = new ArrayList<>();
			for (ADWinVariable variable : variables) {
				if(variable.isParameter() && (variable.isFloat() || variable.isInt())) parameters.add(variable);
				if(variable.isParameter() && variable.isString()) parametersString.add(variable);
			}
			if(parameters.size() > 0) code = code + "REM ******** Initialisation des paramètres\n";
			int numParam = 0;
			for (ADWinVariable parameter : parameters) {
				numParam++;
				String name = parameter.getProperty(ChannelProperties.NAME);
				code = code + name + " = TAB_PARAM[" + numParam + "]\n";
			}
			
			if(parametersString.size() > 0) {
				code = code + "REM ******** Initialisation des paramètres chaine de caractères\n";
				code = code + "index2_" + process.hashCode() + " = 2\n";
				code = code + "index3_" + process.hashCode() + " = 1\n";
				code = code + "FOR index1_" + process.hashCode() + " = 2 TO strlen(TAB_PARAM_STRING) + 1\n";
				code = code + "\tIF (TAB_PARAM_STRING[index1_" + process.hashCode() + "] = 58) THEN\n";
				numParam = 1;
				for (ADWinVariable parameter : parametersString) {
					String name = parameter.getProperty(ChannelProperties.NAME);
					code = code + "\t\tIF (index3_" + process.hashCode() + " = " + numParam + ") THEN\n";
					code = code + "\t\t\t" + name + "[1] = index2_" + process.hashCode() + " - 2\n";
					code = code + "\t\t\t" + name + "[index2_" + process.hashCode() + "] = 0\n";
					code = code + "\t\tENDIF\n";
					numParam++;
				}
				code = code + "\t\tINC(index3_" + process.hashCode() + ")\n";
				code = code + "\t\tindex2_" + process.hashCode() + " = 2\n";
				code = code + "\tELSE\n";
				numParam = 1;
				for (ADWinVariable parameter : parametersString) {
					String name = parameter.getProperty(ChannelProperties.NAME);
					code = code + "\t\tIF (index3_" + process.hashCode() + " = " + numParam + ") THEN\n";
					code = code + "\t\t\t" + name + "[index2_" + process.hashCode() + "] = TAB_PARAM_STRING[index1_" + process.hashCode() + "]\n";
					code = code + "\t\tENDIF\n";
					numParam++;
				}
				code = code + "\t\tINC(index2_" + process.hashCode() + ")\n";
				code = code + "\tENDIF\n";
				code = code + "NEXT index1_" + process.hashCode() + "\n";
			}
			
			for (ADWinVariable variable : variables) {
				String name = variable.getProperty(ChannelProperties.NAME);
				String transferNumber = variable.getProperty(ChannelProperties.TRANSFER_NUMBER);
				String transfer = variable.getProperty(ChannelProperties.TRANSFER);
				String parameter = variable.getProperty(ADWinVariableProperties.PARAMETER);
				String stimulus = variable.getProperty(ADWinVariableProperties.STIMULUS);
				boolean isParameter = Boolean.valueOf(parameter);
				boolean isTransfered = Boolean.valueOf(transfer);
				boolean isStimulus = Boolean.valueOf(stimulus);
				String sfVariable = variable.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				float sfFloat = Float.parseFloat(sfVariable);
				int frequencyRatio = (int) (gsfFloat/sfFloat);
				if(isStimulus) {
					code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
					code = code + "PAR_" + transferNumber + " = 0\n";
				}
				if(!isStimulus && isTransfered && !isParameter){
					code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
					code = code + "FIFO_CLEAR(" + transferNumber + ")\n";
					code = code + "PAR_" + transferNumber + " = 0\n";
				}
			}
			code = code + "\nREM ******** Fin initialisation des variables\n\n";
		}
		return code;
	}
	
	private static String getFinalisationCode() {
		String  code = "";
		ADWinVariable[] variables = ((ADWinDACQConfiguration)process.getDACQConfiguration()).getVariables();
		if(variables.length > 0) {
			ArrayList<ADWinVariable> parameters = new ArrayList<>();
			for (ADWinVariable variable : variables) {
				if(variable.isParameter() && (variable.isFloat() || variable.isInt())) parameters.add(variable);
			}
			if(parameters.size() > 0) code = code + "\nREM ******** Début finalisation des variables (propagation des parametres)\n\n";
			int numParam = 0;
			for (ADWinVariable parameter : parameters) {
				numParam++;
				String name = parameter.getProperty(ChannelProperties.NAME);
				code = code + "TAB_PARAM[" + numParam + "] = " + name + "\n";
			}
			if(parameters.size() > 0) code = code + "\nREM ******** Fin finalisation des variables (propagation des parametres)\n\n";
		}
		return code;
	}

}
