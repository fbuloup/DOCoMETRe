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

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ADWinCANModuleProperties extends Property {
	
	public static String MESSAGE_OBJECT_REGEXP_1 = "^([1-9]|1[0-5])(,([1-9]|1[0-5]))*$";
	public static String MESSAGE_OBJECT_REGEXP_2 = "^R\\(([1-9]|1[0-5])(,([1-9]|1[0-5]))*\\);T\\(([1-9]|1[0-5])(,([1-9]|1[0-5]))*\\)$";
	public static String MESSAGE_ID_REGEXP_1 = "^([1-9]|[1-9][0-9])(,([1-9]|[1-9][0-9]))*$";
	public static String MESSAGE_ID_REGEXP_2 = "^R\\(([1-9]|[1-9][0-9])(,([1-9]|[1-9][0-9]))*\\);T\\(([1-9]|[1-9][0-9])(,([1-9]|[1-9][0-9]))*\\)$";
	
	public static final ADWinCANModuleProperties INTERFACE_NUMBER = new ADWinCANModuleProperties("ADWinCANModuleProperties.INTERFACE_NUMBER", ADWinMessages.ADWinCANInterfaceNumber_Label, ADWinMessages.ADWinCANInterfaceNumber_Tooltip, "^(1|2|3|4|5|6|7|8|9)$", "1:2:3:4:5:6:7:8:9");
	public static final ADWinCANModuleProperties NAME = new ADWinCANModuleProperties("ADWinCANModuleProperties.NAME", ADWinMessages.ADWinCANName_Label, ADWinMessages.ADWinCANName_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$");
	public static final ADWinCANModuleProperties SYSTEM_TYPE = new ADWinCANModuleProperties("ADWinCANModuleProperties.SYSTEM_TYPE", ADWinMessages.ADWinCANSystemType_Label, ADWinMessages.ADWinCANSystemType_Tooltip, "^(CODAMOTION|GYROSCOPE|TIMESTAMP|CODAMOTION AND GYROSCOPE|CODAMOTION AND TIMESTAMP|GYROSCOPE AND TIMESTAMP|CODAMOTION AND GYROSCOPE AND TIMESTAMP|NOT SPECIFIED)$", "CODAMOTION:GYROSCOPE:TIMESTAMP:CODAMOTION AND GYROSCOPE:CODAMOTION AND TIMESTAMP:GYROSCOPE AND TIMESTAMP:CODAMOTION AND GYROSCOPE AND TIMESTAMP:NOT SPECIFIED");
	public static final ADWinCANModuleProperties FREQUENCY = new ADWinCANModuleProperties("ADWinCANModuleProperties.FREQUENCY", ADWinMessages.ADWinCANFrequency_Label, ADWinMessages.ADWinCANFrequency_Tooltip, "(^[+]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinCANModuleProperties NB_SENSORS = new ADWinCANModuleProperties("ADWinCANModuleProperties.NB_SENSORS", ADWinMessages.ADWinCAN_NB_SENSORS_Label, ADWinMessages.ADWinCAN_NB_SENSORS_Toolip, "^(0|1|2|3|4|5|6|7|8|9)$", "0:1:2:3:4:5:6:7:8:9");
	public static final ADWinCANModuleProperties MODE = new ADWinCANModuleProperties("ADWinCANModuleProperties.MODE", ADWinMessages.ADWinCAN_MODE_Label, ADWinMessages.ADWinCAN_MODE_Toolip, "^(RECEIVE|TRANSMIT|RECEIVE & TRANSMIT)$", "RECEIVE:TRANSMIT:RECEIVE & TRANSMIT");
	public static final ADWinCANModuleProperties MESSAGE_OBJECT = new ADWinCANModuleProperties("ADWinCANModuleProperties.MESSAGE_OBJECT", ADWinMessages.ADWinCAN_MESSAGE_OBJECT_Label, ADWinMessages.ADWinCAN_MESSAGE_OBJECT_Toolip, MESSAGE_OBJECT_REGEXP_1 );
	public static final ADWinCANModuleProperties MESSAGE_ID = new ADWinCANModuleProperties("ADWinCANModuleProperties.MESSAGE_ID", ADWinMessages.ADWinCAN_MESSAGE_ID_Label, ADWinMessages.ADWinCAN_MESSAGE_ID_Toolip, MESSAGE_ID_REGEXP_1);
	public static final ADWinCANModuleProperties MESSAGE_ID_LENGTH = new ADWinCANModuleProperties("ADWinCANModuleProperties.MESSAGE_ID_LENGTH", ADWinMessages.ADWinCAN_MESSAGE_ID_LENGTH_Label, ADWinMessages.ADWinCAN_MESSAGE_ID_LENGTH_Toolip, "^(11 bits|29 bits)$", "11 bits:29 bits");
	
	public static String CODAMOTION_SYSTEM_TYPE = "CODAMOTION";
	public static String GYROSCOPE_SYSTEM_TYPE = "GYROSCOPE";
	public static String TIMESTAMP_SYSTEM_TYPE = "TIMESTAMP";
	public static String CODAMOTION_GYROSCOPE_SYSTEM_TYPE = "CODAMOTION AND GYROSCOPE";
	public static String CODAMOTION_TIMESTAMP_SYSTEM_TYPE = "CODAMOTION AND TIMESTAMP";
	public static String GYROSCOPE_TIMESTAMP_SYSTEM_TYPE = "GYROSCOPE AND TIMESTAMP";
	public static String CODAMOTION_GYROSCOPE_TIMESTAMP_SYSTEM_TYPE = "CODAMOTION AND GYROSCOPE AND TIMESTAMP";
	public static String NOT_SPECIFIED_SYSTEM_TYPE = "NOT SPECIFIED";
	public static String[] SYSTEMS_TYPES = new String[] {CODAMOTION_SYSTEM_TYPE, 
														 GYROSCOPE_SYSTEM_TYPE, 
														 TIMESTAMP_SYSTEM_TYPE, 
														 CODAMOTION_GYROSCOPE_SYSTEM_TYPE,
														 CODAMOTION_TIMESTAMP_SYSTEM_TYPE,
														 GYROSCOPE_TIMESTAMP_SYSTEM_TYPE,
														 CODAMOTION_GYROSCOPE_TIMESTAMP_SYSTEM_TYPE};
	
	public static String RECEIVE = "RECEIVE";
	public static String TRANSMIT = "TRANSMIT";
	public static String TRANSMIT_RECEIVE = "RECEIVE & TRANSMIT";
	public static String MESSAGE_ID_LENGTH_11 = "11 bits";
	public static String MESSAGE_ID_LENGTH_29 = "29 bits";
	
	
	
	public static void populateProperties(Module module){
		ADWinModuleProperties.populateProperties(module);
		module.setProperty(INTERFACE_NUMBER, "1"); 
		module.setProperty(NAME, "CAN"); 
		module.setProperty(SYSTEM_TYPE, "NOT SPECIFIED"); 
		module.setProperty(FREQUENCY, module.getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY));
		module.setProperty(NB_SENSORS, "0");
		module.setProperty(MODE, RECEIVE);
		module.setProperty(MESSAGE_OBJECT, "1");
		module.setProperty(MESSAGE_ID_LENGTH, MESSAGE_ID_LENGTH_11);
		module.setProperty(MESSAGE_ID, "1");
	}
	
	public static Module cloneModule(Module module) {
		ADWinCANModule newModule = new ADWinCANModule(null);
		ADWinModuleProperties.cloneModule(module, newModule);
		newModule.setProperty(INTERFACE_NUMBER, new String(module.getProperty(INTERFACE_NUMBER)));
		newModule.setProperty(NAME, new String(module.getProperty(NAME)));
		newModule.setProperty(SYSTEM_TYPE, new String(module.getProperty(SYSTEM_TYPE)));
		newModule.setProperty(FREQUENCY, new String(module.getProperty(FREQUENCY)));
		newModule.setProperty(NB_SENSORS, new String(module.getProperty(NB_SENSORS)));
		newModule.setProperty(MODE, new String(module.getProperty(MODE)));
		newModule.setProperty(MESSAGE_OBJECT, new String(module.getProperty(MESSAGE_OBJECT)));
		newModule.setProperty(MESSAGE_ID_LENGTH, new String(module.getProperty(MESSAGE_ID_LENGTH)));
		newModule.setProperty(MESSAGE_ID, new String(module.getProperty(MESSAGE_ID)));
		return newModule;
	}
	
	public static Channel cloneChannel(ADWinChannel channel) {
		ADWinChannel newChannel = new ADWinChannel(null);
		ChannelProperties.cloneChannel(channel, newChannel);
		return channel;
	}
	
	private ADWinCANModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinCANModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
