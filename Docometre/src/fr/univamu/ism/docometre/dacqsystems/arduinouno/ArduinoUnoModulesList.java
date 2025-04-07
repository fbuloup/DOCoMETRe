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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;

public enum ArduinoUnoModulesList {
	
	ANALOG_INPUT,
	ANALOG_OUTPUT,
	DIO,
	ADS1115;
	
	public static ArduinoUnoModulesList[] getModules(boolean withADS1115) {
		if(withADS1115) return new ArduinoUnoModulesList[] {ADS1115};
		return new ArduinoUnoModulesList[] {};
	}
	
	public static String getDescription(ArduinoUnoModulesList arduinoModulesList) {
		switch (arduinoModulesList) {
		case ANALOG_INPUT:
			return ArduinoUnoMessages.ANALOG_INPUT;
		case ANALOG_OUTPUT:
			return ArduinoUnoMessages.ANALOG_OUTPUT;
		case DIO:
			return ArduinoUnoMessages.DIO;
		case ADS1115:
			return NLS.bind(ArduinoUnoMessages.ADS1115, "0x4x");
		default:
			return ArduinoUnoMessages.UNKNOWN;
		}
	}
	
	public static String getClassName(ArduinoUnoModulesList adwinModulesList) {
		switch (adwinModulesList) {
		case ANALOG_INPUT:
			return null;//ADWinAnInModule.class.getCanonicalName();
		case ANALOG_OUTPUT:
			return null;//ADWinAnOutModule.class.getCanonicalName();
		case DIO:
			return null;//ADWinDigInOutModule.class.getCanonicalName();
		case ADS1115:
			return ArduinoUnoADS1115Module.class.getCanonicalName();
		default:
			return "";
		}
	}
	
	public static String getDescription(Module module) {
		if(module instanceof ArduinoUnoAnInModule) return ArduinoUnoMessages.ANALOG_INPUT;
		if(module instanceof ArduinoUnoAnOutModule) return ArduinoUnoMessages.ANALOG_OUTPUT;
		if(module instanceof ArduinoUnoDigInOutModule) return ArduinoUnoMessages.DIO;
		if(module instanceof ArduinoUnoADS1115Module) {
			ArduinoUnoADS1115Module ads1115Module = (ArduinoUnoADS1115Module)module;
			String address = ads1115Module.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
			return NLS.bind(ArduinoUnoMessages.ADS1115, address);
		}
		return ArduinoUnoMessages.UNKNOWN;
		
	}
	
	public static Module createModule(ArduinoUnoModulesList adwinModulesList, DACQConfiguration configuration) {
		Module module = null;
		switch (adwinModulesList) {
		case ANALOG_INPUT:
			module = null;//new ADWinAnInModule(configuration);
			break;
		case ANALOG_OUTPUT:
			module = null;//new ADWinAnOutModule(configuration);
			break;
		case DIO:
			module = null;//new ADWinDigInOutModule(configuration);
			break;
		case ADS1115:
			module = new ArduinoUnoADS1115Module(configuration);
			break;
		default:
			return null;
		}
		return module;
	}

	public static String getDescription(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(DACQConfiguration.class);
			Module module = (Module) constructor.newInstance(new ArduinoUnoDACQConfiguration());
			return getDescription(module);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ArduinoUnoMessages.UNKNOWN; 
	}

}
