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

import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoAnInModuleProperties extends Property {
	
	public static final ArduinoUnoAnInModuleProperties ANALOG_REFERENCE = new ArduinoUnoAnInModuleProperties("ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE", ArduinoUnoMessages.AnalogReference_Label, ArduinoUnoMessages.AnalogReference_Tooltip, "^(DEFAULT|INTERNAL|EXTERNAL|AR_DEFAULT|AR_EXTERNAL)$", "DEFAULT:INTERNAL:EXTERNAL:AR_DEFAULT:AR_EXTERNAL");
	
	public static String DEFAULT = "DEFAULT";
	public static String INTERNAL = "INTERNAL";
	public static String EXTERNAL = "EXTERNAL";
	public static String AR_DEFAULT = "AR_DEFAULT";
	public static String AR_EXTERNAL = "AR_EXTERNAL";
	
	public static void populateProperties(Module module){
		module.setProperty(ANALOG_REFERENCE, DEFAULT); 
	}
	
	public static Module cloneModule(Module module) {
		ArduinoUnoAnInModule newModule = new ArduinoUnoAnInModule(null);
		newModule.setProperty(ANALOG_REFERENCE, new String(module.getProperty(ANALOG_REFERENCE)));
		return newModule;
	}
	
	private ArduinoUnoAnInModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ArduinoUnoAnInModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
}
