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

import java.util.ArrayList;

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoADS1115ModuleProperties extends Property {
	
	public static final ArduinoUnoADS1115ModuleProperties ADDRESS = new ArduinoUnoADS1115ModuleProperties("ArduinoUnoADS1115ModuleProperties.ADDRESS", ArduinoUnoMessages.address_label, ArduinoUnoMessages.address_tooltip, "^(0x48|0x49|0x4A|0x4B)$", "0x48:0x49:0x4A:0x4B");
	public static final ArduinoUnoADS1115ModuleProperties MODE = new ArduinoUnoADS1115ModuleProperties("ArduinoUnoADS1115ModuleProperties.MODE", ArduinoUnoMessages.mode_label, ArduinoUnoMessages.mode_tooltip, "^(0|1)$", "0:1");
	public static final ArduinoUnoADS1115ModuleProperties DATA_RATE = new ArduinoUnoADS1115ModuleProperties("ArduinoUnoADS1115ModuleProperties.DATA_RATE", ArduinoUnoMessages.dataRate_label, ArduinoUnoMessages.dataRate_tooltip, "^(0|1|2|3|4|5|6|7)$", "0:1:2:3:4:5:6:7");
	
	private static String ADDRESS_0X48 = "0x48";
	private static String ADDRESS_0X49 = "0x49";
	private static String ADDRESS_0X4A = "0x4A";
	private static String ADDRESS_0X4B = "0x4B";
	public static String[] ADDRESSES = new String[] {ADDRESS_0X48, ADDRESS_0X49, ADDRESS_0X4A, ADDRESS_0X4B};
	public static String[] MODES = new String[] {"0", "1"};
	public static String[] DATA_RATES = new String[] {"0", "1", "2", "3", "4", "5", "6", "7"};

	public static void populateProperties(Module module){
		module.setProperty(ADDRESS, ADDRESS_0X48); 
		module.setProperty(MODE, "1"); 
		module.setProperty(DATA_RATE, "7"); 
		DACQConfiguration dacqConfiguration = module.getDACQConfiguration();
		if(dacqConfiguration != null) {
			Module[] modules = dacqConfiguration.getModules();
			ArrayList<String> usedAddress = new ArrayList<>();
			for (Module dacqModule : modules) {
				if(dacqModule instanceof ArduinoUnoADS1115Module && dacqModule != module)  {
					usedAddress.add(dacqModule.getProperty(ADDRESS));
				}
			}
			if(!usedAddress.contains(ADDRESS_0X48)) module.setProperty(ADDRESS, ADDRESS_0X48); 
			else if(!usedAddress.contains(ADDRESS_0X49)) module.setProperty(ADDRESS, ADDRESS_0X49); 
			else if(!usedAddress.contains(ADDRESS_0X4A)) module.setProperty(ADDRESS, ADDRESS_0X4A); 
			else if(!usedAddress.contains(ADDRESS_0X4B)) module.setProperty(ADDRESS, ADDRESS_0X4B); 
		}
	}
	
	public static Module cloneModule(Module module) {
		ArduinoUnoADS1115Module newModule = new ArduinoUnoADS1115Module(null);
		newModule.setProperty(ADDRESS, new String(module.getProperty(ADDRESS)));
		newModule.setProperty(MODE, new String(module.getProperty(MODE)));
		newModule.setProperty(DATA_RATE, new String(module.getProperty(DATA_RATE)));
		return newModule;
	}
	
	public ArduinoUnoADS1115ModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}

	public ArduinoUnoADS1115ModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
	
}
