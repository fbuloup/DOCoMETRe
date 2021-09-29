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
