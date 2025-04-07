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

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoAnInChannelProperties extends Property {
	
	public static final ArduinoUnoAnInChannelProperties UNIT = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT", ArduinoUnoMessages.Unit_Label, ArduinoUnoMessages.Unit_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$");
	public static final ArduinoUnoAnInChannelProperties UNIT_MAX = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT_MAX", ArduinoUnoMessages.UnitMaxValue_Label, ArduinoUnoMessages.UnitMaxValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ArduinoUnoAnInChannelProperties UNIT_MIN = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT_MIN", ArduinoUnoMessages.UnitMinValue_Label, ArduinoUnoMessages.UnitMinValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ArduinoUnoAnInChannelProperties AMPLITUDE_MAX = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX", ArduinoUnoMessages.AmpMaxValue_Label, ArduinoUnoMessages.AmpMaxValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ArduinoUnoAnInChannelProperties AMPLITUDE_MIN = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN", ArduinoUnoMessages.AmpMinValue_Label, ArduinoUnoMessages.AmpMinValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	/*  GAIN PROPERTY IS ONLY USED WITH ADS1115 ANALOG INPUTS MODULES
		  
		0   (gain = 2/3)   ± 6.144 volts (valeur par défaut)
		1   (gain = 1)	    ± 4.096 volts
		2   (gain = 2)	    ± 2.048 volts
		4   (gain = 4)	    ± 1.024 volts
		8   (gain = 8)	    ± 0.512 volts
		16  (gain = 16)	± 0.256 volts 
		
		0 gives an actual gain of 2/3 with a precision of 0.1875mV and an range of [0-5]V under 5VCC or [0-3.3]V under 3.3VCC
		1 gives an actual gain of 1 with a precision of 0.125mV and an range of [0-4.096]V under 5VCC or [0-3.3]V under 3.3VCC
		2 gives an actual gain of 2 with a precision of 0.0625mV and an range of [0-2.048]V under 5VCC or 3.3VCC
		4 gives an actual gain of 4 with a precision of 0.03125mV and an range of [0-1.0248]V under 5VCC or 3.3VCC
		8 gives an actual gain of 8 with a precision of 0.015625mV and an range of [0-0.512]V under 5VCC or 3.3VCC
		16 gives an actual gain of 16 with a precision of 0.0078125mV and an range of [0-0.256]V under 5VCC or 3.3VCC
		
		C'est seulement en mode différentiel que l'on peut mesurer des tensions négatives.
	*/											
	public static final ArduinoUnoAnInChannelProperties GAIN = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.GAIN", ArduinoUnoMessages.Gain_Label, ArduinoUnoMessages.Gain_Tooltip, "^(2/3|1|2|4|8|16)$", "2/3:1:2:4:8:16");
	
	public static String GAIN_0 = "2/3";
	public static String GAIN_1 = "1";
	public static String GAIN_2 = "2";
	public static String GAIN_4 = "4";
	public static String GAIN_8 = "8";
	public static String GAIN_16 = "16";
	public static String[] GAINS = new String[] {GAIN_0, GAIN_1, GAIN_2, GAIN_4, GAIN_8, GAIN_16};
	
	public static float getMaxVoltageForADS1115Gain(String gain) {
		if(GAIN_0.equals(gain)) return (float) 6.144;
		if(GAIN_1.equals(gain)) return (float) 4.096;
		if(GAIN_2.equals(gain)) return (float) 2.048;
		if(GAIN_4.equals(gain)) return (float) 1.024;
		if(GAIN_8.equals(gain)) return (float) 0.512;
		if(GAIN_16.equals(gain)) return (float) 0.256;
		return 0;
	}
	
	public static void populateProperties(Channel channel) {
		ArduinoUnoChannelProperties.populateProperties(channel);
		channel.setProperty(UNIT, "Volt");
		channel.setProperty(UNIT_MAX, "5");
		channel.setProperty(UNIT_MIN, "0");
		channel.setProperty(AMPLITUDE_MAX, "5");
		channel.setProperty(AMPLITUDE_MIN, "0");
		channel.setProperty(GAIN, "0");
	}

	public static Channel cloneChannel(ArduinoUnoChannel channel) {
		ArduinoUnoChannel newChannel = new ArduinoUnoChannel(null);
		ArduinoUnoChannelProperties.cloneChannel(channel, newChannel);
		newChannel.setProperty(UNIT, new String(channel.getProperty(UNIT)));
		newChannel.setProperty(UNIT_MAX, new String(channel.getProperty(UNIT_MAX)));
		newChannel.setProperty(UNIT_MIN, new String(channel.getProperty(UNIT_MIN)));
		newChannel.setProperty(AMPLITUDE_MAX, new String(channel.getProperty(AMPLITUDE_MAX)));
		newChannel.setProperty(AMPLITUDE_MIN, new String(channel.getProperty(AMPLITUDE_MIN)));
		newChannel.setProperty(GAIN, new String(channel.getProperty(GAIN)));
		return channel;
	}
	
	public static double toVolt(ArduinoUnoChannel channel, double value) {
		if(channel.getModule() instanceof ArduinoUnoAnInModule) {
			double aMax = 5;
			double aMin = 0;
			return (aMax - aMin)/1024*value + aMin;
		}
		return 0;
	}
	
	private ArduinoUnoAnInChannelProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ArduinoUnoAnInChannelProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
