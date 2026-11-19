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

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoAnInModuleProperties extends Property {
	
	public static final ArduinoUnoAnInModuleProperties ANALOG_REFERENCE = new ArduinoUnoAnInModuleProperties("ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE", ArduinoUnoMessages.AnalogReference_Label, ArduinoUnoMessages.AnalogReference_Tooltip, "^(DEFAULT|INTERNAL|EXTERNAL|AR_DEFAULT|AR_INTERNAL|AR_EXTERNAL)$", "DEFAULT:INTERNAL:EXTERNAL:AR_DEFAULT:AR_INTERNAL:AR_EXTERNAL");
	
	/**
	 * Portenta Machine Control : type of sensor of its three analog inputs (SensorType of the Arduino_PortentaMachineControl
	 * library), common to the three inputs. Not defined (null) for the configurations created before : V_0_10.
	 */
	public static final ArduinoUnoAnInModuleProperties SENSOR_TYPE = new ArduinoUnoAnInModuleProperties("ArduinoUnoAnInModuleProperties.SENSOR_TYPE", ArduinoUnoMessages.SensorType_Label, ArduinoUnoMessages.SensorType_Tooltip, "^(V_0_10|MA_4_20|NTC)$", "V_0_10:MA_4_20:NTC");
	public static String SENSOR_TYPE_V_0_10 = "V_0_10";
	public static String SENSOR_TYPE_MA_4_20 = "MA_4_20";
	public static String SENSOR_TYPE_NTC = "NTC";
	public static String[] SENSOR_TYPES = new String[] {SENSOR_TYPE_V_0_10, SENSOR_TYPE_MA_4_20, SENSOR_TYPE_NTC};
	// Displayed to the user, same order as SENSOR_TYPES
	public static String[] SENSOR_TYPES_LABELS = new String[] {ArduinoUnoMessages.SensorType_V_0_10, ArduinoUnoMessages.SensorType_MA_4_20, ArduinoUnoMessages.SensorType_NTC};
	
	/**
	 * @return the type of sensor of the analog inputs of the Portenta Machine Control (V_0_10 if it is not defined)
	 */
	public static String getSensorType(Module module) {
		String sensorType = module.getProperty(SENSOR_TYPE);
		for (String type : SENSOR_TYPES) if(type.equals(sensorType)) return type;
		return SENSOR_TYPE_V_0_10;
	}
	
	// Analog references of Arduino Uno R3 : DEFAULT, INTERNAL, EXTERNAL ; of Arduino Uno R4 WiFi : AR_DEFAULT, AR_INTERNAL, AR_EXTERNAL
	public static String DEFAULT = "DEFAULT";
	public static String INTERNAL = "INTERNAL";
	public static String EXTERNAL = "EXTERNAL";
	public static String AR_DEFAULT = "AR_DEFAULT";
	public static String AR_INTERNAL = "AR_INTERNAL";
	public static String AR_EXTERNAL = "AR_EXTERNAL";
	// Same order in both arrays : corresponding references of R3 and R4 WiFi
	public static String[] R3_REFERENCES = new String[] {DEFAULT, INTERNAL, EXTERNAL};
	public static String[] R4_REFERENCES = new String[] {AR_DEFAULT, AR_INTERNAL, AR_EXTERNAL};
	
	/**
	 * @param r4 true for Arduino Uno R4 WiFi, false for R3
	 * @return the analog references of this revision
	 */
	public static String[] getReferences(boolean r4) {
		return r4 ? R4_REFERENCES : R3_REFERENCES;
	}
	
	/**
	 * @param reference an analog reference of R3 or of R4 WiFi
	 * @param r4 true for Arduino Uno R4 WiFi, false for R3
	 * @return the corresponding reference of this revision (e.g. AR_DEFAULT for DEFAULT on R4 WiFi), its
	 * default reference if reference is not known
	 */
	public static String getReferenceOfRevision(String reference, boolean r4) {
		String[] references = getReferences(r4);
		reference = reference == null ? "" : reference.trim();
		for (int i = 0; i < references.length; i++) 
			if(R3_REFERENCES[i].equals(reference) || R4_REFERENCES[i].equals(reference)) return references[i];
		return references[0];
	}
	
	/**
	 * @return true if the revision of the DACQ configuration is Arduino Uno R4 WiFi
	 */
	public static boolean isR4(DACQConfiguration dacqConfiguration) {
		return ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION));
	}
	
	public static void populateProperties(Module module){
		module.setProperty(ANALOG_REFERENCE, DEFAULT); 
		module.setProperty(SENSOR_TYPE, SENSOR_TYPE_V_0_10);
	}
	
	public static Module cloneModule(Module module) {
		ArduinoUnoAnInModule newModule = new ArduinoUnoAnInModule(null);
		newModule.setProperty(ANALOG_REFERENCE, new String(module.getProperty(ANALOG_REFERENCE)));
		newModule.setProperty(SENSOR_TYPE, getSensorType(module));
		return newModule;
	}
	
	private ArduinoUnoAnInModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ArduinoUnoAnInModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
}
