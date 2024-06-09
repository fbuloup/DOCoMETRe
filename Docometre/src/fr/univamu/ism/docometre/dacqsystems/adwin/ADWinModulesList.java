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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;

public enum ADWinModulesList {
	
	ANALOG_INPUT,
	ANALOG_OUTPUT,
	DIO,
	CAN,
	RS232;
	
	public static ADWinModulesList[] getModules() {
		return new ADWinModulesList[] {ANALOG_INPUT, ANALOG_OUTPUT, DIO, CAN, RS232};
	}
	
	public static String getDescription(ADWinModulesList adwinModulesList) {
		switch (adwinModulesList) {
		case ANALOG_INPUT:
			return ADWinMessages.ANALOG_INPUT;
		case ANALOG_OUTPUT:
			return ADWinMessages.ANALOG_OUTPUT;
		case DIO:
			return ADWinMessages.DIO;
		case RS232:
			return ADWinMessages.RS232;
		case CAN:
			return ADWinMessages.CAN;
		default:
			return ADWinMessages.UNKNOWN;
		}
	}
	
	public static String getClassName(ADWinModulesList adwinModulesList) {
		switch (adwinModulesList) {
		case ANALOG_INPUT:
			return ADWinAnInModule.class.getCanonicalName();
		case ANALOG_OUTPUT:
			return ADWinAnOutModule.class.getCanonicalName();
		case DIO:
			return ADWinDigInOutModule.class.getCanonicalName();
		case RS232:
			return ADWinRS232Module.class.getCanonicalName();
		case CAN:
			return ADWinCANModule.class.getCanonicalName();
		default:
			return "";
		}
	}
	
	public static String getDescription(Module module) {
		if(module instanceof ADWinAnInModule) return ADWinMessages.ANALOG_INPUT;
		if(module instanceof ADWinAnOutModule) return ADWinMessages.ANALOG_OUTPUT;
		if(module instanceof ADWinDigInOutModule) return ADWinMessages.DIO;
		if(module instanceof ADWinRS232Module) return ADWinMessages.RS232;
		if(module instanceof ADWinCANModule) return ADWinMessages.CAN;
		return ADWinMessages.UNKNOWN;
		
	}
	
	public static Module createModule(String className, DACQConfiguration configuration) {
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(DACQConfiguration.class);
			Module module = (Module) constructor.newInstance(new ADWinDACQConfiguration());
			return module;
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
		return null; 
	}
	
	public static Module createModule(ADWinModulesList adwinModulesList, DACQConfiguration configuration) {
		Module module = null;
		switch (adwinModulesList) {
		case ANALOG_INPUT:
			module = new ADWinAnInModule(configuration);
			break;
		case ANALOG_OUTPUT:
			module = new ADWinAnOutModule(configuration);
			break;
		case DIO:
			module = new ADWinDigInOutModule(configuration);
			break;
		case RS232:
			module = new ADWinRS232Module(configuration);
			break;
		case CAN:
			module = new ADWinCANModule(configuration);
			break;
		default:
			return null;
		}
		setModuleNumber(module, configuration);
		return module;
	}
	
	private static void setModuleNumber(Module currentModule, DACQConfiguration configuration) {
		Module[] modules = configuration.getModules();
		ArrayList<Integer> notAllowedModuleNumber = new ArrayList<Integer>(0);
		for (Module module : modules) {
			if(currentModule.getClass().getCanonicalName().equals(module.getClass().getCanonicalName())) {
				notAllowedModuleNumber.add(Integer.parseInt(module.getProperty(ADWinModuleProperties.MODULE_NUMBER)));
			}
		}
		int currentModuleNumber = 1;
		boolean moduleNumberFound = false;
		while(!moduleNumberFound) {
			moduleNumberFound = true;
			for (Integer moduleNumber : notAllowedModuleNumber) {
				if(moduleNumber == currentModuleNumber) {
					moduleNumberFound = false;
					currentModuleNumber++;
					break;
				}
			}
		}
		currentModule.setProperty(ADWinModuleProperties.MODULE_NUMBER, String.valueOf(currentModuleNumber));
	}

	public static String getDescription(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(DACQConfiguration.class);
			Module module = (Module) constructor.newInstance(new ADWinDACQConfiguration());
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
		return ADWinMessages.UNKNOWN; 
	}

}
