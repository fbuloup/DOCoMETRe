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

import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ADWinVariableProperties extends Property {

	public static final ADWinVariableProperties PARAMETER = new ADWinVariableProperties("ADWinVariableProperties.PARAMETER", ADWinMessages.Parameter_Label, ADWinMessages.Parameter_Tooltip, "^(true|false)$", "true:false"); 
	public static final ADWinVariableProperties PARAMETER_VALUE = new ADWinVariableProperties("ADWinVariableProperties.PARAMETER_VALUE", "", "", "(^[+-]?\\\\d*\\\\.?\\\\d*[1-9]+\\\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\\\d*\\\\.\\\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinVariableProperties PROPAGATE = new ADWinVariableProperties("ADWinVariableProperties.PROPAGATE", ADWinMessages.Propagate_Label, ADWinMessages.Propagate_Tooltip, "^(true|false)$", "true:false");
	public static final ADWinVariableProperties SIZE = new ADWinVariableProperties("ADWinVariableProperties.SIZE", ADWinMessages.Size_Label, ADWinMessages.Size_Tooltip, "^\\d+$");
	public static final ADWinVariableProperties TYPE = new ADWinVariableProperties("ADWinVariableProperties.TYPE", ADWinMessages.Type_Label, ADWinMessages.Type_Tooltip, "^float|int|string$", "float:int:string");

	public static final String FLOAT = "float";
	public static final String INT = "int";
	public static final String STRING = "string";
	public static final String[] TYPES = new String[] { FLOAT, INT, STRING };

	public static void populateProperties(ADWinVariable variable) {
		ChannelProperties.populateProperties(variable);
		variable.setProperty(PARAMETER, "false");
		variable.setProperty(PROPAGATE, "false");
		variable.setProperty(SIZE, "1");
		variable.setProperty(TYPE, "float");
	}

	public static ADWinVariable cloneVariable(ADWinVariable variable) {
		ADWinVariable newVariable = new ADWinVariable(null);
		ChannelProperties.cloneChannel(variable, newVariable);
		newVariable.setProperty(PARAMETER, new String(variable.getProperty(PARAMETER)));
		newVariable.setProperty(PARAMETER_VALUE, new String(variable.getProperty(PARAMETER_VALUE)));
		newVariable.setProperty(PROPAGATE, new String(variable.getProperty(PROPAGATE)));
		newVariable.setProperty(SIZE, new String(variable.getProperty(SIZE)));
		newVariable.setProperty(TYPE, new String(variable.getProperty(TYPE)));
		return newVariable;
	}

	private ADWinVariableProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinVariableProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
