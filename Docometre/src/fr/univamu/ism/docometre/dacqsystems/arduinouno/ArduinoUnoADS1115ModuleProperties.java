package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import java.util.ArrayList;

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoADS1115ModuleProperties extends Property {
	
	public static final ArduinoUnoADS1115ModuleProperties ADDRESS = new ArduinoUnoADS1115ModuleProperties("ArduinoUnoADS1115ModuleProperties.ADDRESS", ArduinoUnoMessages.address_label, ArduinoUnoMessages.address_tooltip, "\"^(0x48|0x49|0x4A|0x4B)$\"", "0x48:0x49:0x4A:0x4B");
	
	public static String ADDRESS_0X48 = "0x48";
	public static String ADDRESS_0X49 = "0x49";
	public static String ADDRESS_0X4A = "0x4A";
	public static String ADDRESS_0X4B = "0x4B";
	public static String[] ADDRESSES = new String[] {ADDRESS_0X48, ADDRESS_0X49, ADDRESS_0X4A, ADDRESS_0X4B};

	public static void populateProperties(Module module){
		module.setProperty(ADDRESS, ADDRESS_0X48); 
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
		return newModule;
	}
	
	public ArduinoUnoADS1115ModuleProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}

	public ArduinoUnoADS1115ModuleProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
	
}
