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

import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ADWinAnInModuleProperties extends Property {

//	UPDATE_CHANNEL("UPDATE_CHANNEL", "", ""),
	public static final ADWinAnInModuleProperties AMPLITUDE_MAX = new ADWinAnInModuleProperties("ADWinAnInModuleProperties.AMPLITUDE_MAX", ADWinMessages.AmplitudeMax_Label, ADWinMessages.AmplitudeMax_Tooltip, "^(10|5)$", "10:5");
	public static final ADWinAnInModuleProperties AMPLITUDE_MIN = new ADWinAnInModuleProperties("ADWinAnInModuleProperties.AMPLITUDE_MIN", ADWinMessages.AmplitudeMin_Label, ADWinMessages.AmplitudeMin_Tooltip, "^(-10|-5|0)$", "0:-5:-10");
	public static final ADWinAnInModuleProperties SINGLE_ENDED_DIFFERENTIAL = new ADWinAnInModuleProperties("ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL", ADWinMessages.SeDiff_Label, ADWinMessages.SeDiff_Tooltip, "^(SE|DIFF)$", "SE:DIFF");
	
	public static String PLUS_10 = "10";   
	public static String PLUS_5 = "5";
	public static String MINUS_10 = "-10";
	public static String MINUS_5 = "-5";
	public static String ZERO = "0";
	public static String SE = "SE";
	public static String DIFF = "DIFF";
//	public static String PLUS_OR_MINUS = String.valueOf(Character.toChars(0xB1));
	
	public static void populateProperties(Module module){
		ADWinModuleProperties.populateProperties(module);
		module.setProperty(AMPLITUDE_MAX, "10"); 
		module.setProperty(AMPLITUDE_MIN, "-10"); 
		module.setProperty(SINGLE_ENDED_DIFFERENTIAL, "SE");
	}
	
	public static Module cloneModule(Module module) {
		ADWinAnInModule newModule = new ADWinAnInModule(null);
		ADWinModuleProperties.cloneModule(module, newModule);
		newModule.setProperty(AMPLITUDE_MAX, new String(module.getProperty(AMPLITUDE_MAX)));
		newModule.setProperty(AMPLITUDE_MIN, new String(module.getProperty(AMPLITUDE_MIN)));
		newModule.setProperty(SINGLE_ENDED_DIFFERENTIAL, new String(module.getProperty(SINGLE_ENDED_DIFFERENTIAL)));
		return newModule;
	}
	
	private ADWinAnInModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinAnInModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
	
}
