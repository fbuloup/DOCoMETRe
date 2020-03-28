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

public final class ADWinRS232ModuleProperties extends Property {
	
//	UPDATE_CHANNEL("UPDATE_CHANNEL", "", ""),
	public static final ADWinRS232ModuleProperties NAME = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.NAME", "ADWinRS232ModuleProperties.NAME", ADWinMessages.ADWinRS232Name_Label, ADWinMessages.ADWinRS232Name_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$");
	public static final ADWinRS232ModuleProperties INTERFACE_NUMBER = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.INTERFACE_NUMBER", ADWinMessages.ADWinRS232InterfaceNumber_Label, ADWinMessages.ADWinRS232InterfaceNumber_Tooltip, "^(1|2|3|4|5|6|7|8|9)$", "1:2:3:4:5:6:7:8:9");
	public static final ADWinRS232ModuleProperties SYSTEM_TYPE = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.SYSTEM_TYPE", ADWinMessages.ADWinRS232SystemType_Label, ADWinMessages.ADWinRS232SystemType_Tooltip, "^(ICE|NOT SPECIFIED)$", "ICE:NOT SPECIFIED");
	public static final ADWinRS232ModuleProperties FREQUENCY = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.FREQUENCY", ADWinMessages.ADWinRS232Frequency_Label, ADWinMessages.ADWinRS232Frequency_Tooltip, "(^[+]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinRS232ModuleProperties BAUD_RATE = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.BAUD_RATE", ADWinMessages.ADWinRS232BaudRate_Label, ADWinMessages.ADWinRS232BaudRate_Tooltip, "^(115200|57600|38400|19200|9600|4800|2400|1200|600|300)$", "115200:57600:38400:19200:9600:4800:2400:1200:600:300");
	public static final ADWinRS232ModuleProperties NB_DATA_BITS = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.NB_DATA_BITS", ADWinMessages.ADWinRS232DataBits_Label, ADWinMessages.ADWinRS232DataBits_Tooltip, "^(8|7|6|5$", "8:7:6:5");
	public static final ADWinRS232ModuleProperties NB_STOP_BITS = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.NB_STOP_BITS", ADWinMessages.ADWinRS232StopBits_Label, ADWinMessages.ADWinRS232StopBits_Tooltip, "^(2|1.5|1)$", "2:1.5:1");
	public static final ADWinRS232ModuleProperties PARITY = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.PARITY", ADWinMessages.ADWinRS232Parity_Label, ADWinMessages.ADWinRS232Parity_Tooltip, "^(No parity|Odd|Even)$", "No parity:Odd:Even");
	public static final ADWinRS232ModuleProperties FLOW_CONTROL = new ADWinRS232ModuleProperties("ADWinRS232ModuleProperties.FLOW_CONTROL", ADWinMessages.ADWinRS232FlowControl_Label, ADWinMessages.ADWinRS232FlowControl_Tooltip, "^(No handshake|Sotfware handshake|Hardware handshake)$", "No handshake:Sotfware handshake:Hardware handshake");
	
	public static String ICE_SYSTEM_TYPE = "ICE";
	public static String NOT_SPECIFIED_SYSTEM_TYPE = "NOT SPECIFIED";
	
	public static String NO_PARITY = "No parity";
	public static String PARITY_ODD = "Odd";
	public static String PARITY_EVEN = "Even";
	
	public static String NO_HANDSHAKE = "No handshake";
	public static String SOFTWARE_HANDSHAKE = "Sotfware handshale";
	public static String HARDWARE_HANDSHAKE = "Hardware handshake";
	
	public static String STOPBIT_2 = "2";
	public static String STOPBIT_15 = "1.5";
	public static String STOPBIT_1 = "1";
	
	public static void populateProperties(Module module){
		ADWinModuleProperties.populateProperties(module);
		module.setProperty(INTERFACE_NUMBER, "1"); 
		module.setProperty(NAME, "RS232"); 
		module.setProperty(SYSTEM_TYPE, "NOT SPECIFIED"); 
		module.setProperty(FREQUENCY, "");
		module.setProperty(BAUD_RATE, "115200");
		module.setProperty(NB_DATA_BITS, "8");
		module.setProperty(NB_STOP_BITS, "1");
		module.setProperty(PARITY, "No parity");
		module.setProperty(FLOW_CONTROL, "No handshake");
	}
	
	public static Module cloneModule(Module module) {
		ADWinCANModule newModule = new ADWinCANModule(null);
		ADWinModuleProperties.cloneModule(module, newModule);
		newModule.setProperty(INTERFACE_NUMBER, new String(module.getProperty(INTERFACE_NUMBER)));
		newModule.setProperty(NAME, new String(module.getProperty(NAME)));
		newModule.setProperty(SYSTEM_TYPE, new String(module.getProperty(SYSTEM_TYPE)));
		newModule.setProperty(FREQUENCY, new String(module.getProperty(FREQUENCY)));
		newModule.setProperty(BAUD_RATE, new String(module.getProperty(BAUD_RATE)));
		newModule.setProperty(NB_DATA_BITS, new String(module.getProperty(NB_DATA_BITS)));
		newModule.setProperty(NB_STOP_BITS, new String(module.getProperty(NB_STOP_BITS)));
		newModule.setProperty(PARITY, new String(module.getProperty(PARITY)));
		newModule.setProperty(FLOW_CONTROL, new String(module.getProperty(FLOW_CONTROL)));
		return newModule;
	}
	
	private ADWinRS232ModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinRS232ModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
