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

import fr.univamu.ism.docometre.calibration.ChannelCalibration;
import fr.univamu.ism.docometre.calibration.RawCalibration;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoAnInChannelProperties extends Property {
	
	public static final ArduinoUnoAnInChannelProperties UNIT = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT", ArduinoUnoMessages.Unit_Label, ArduinoUnoMessages.Unit_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$");
	public static final ArduinoUnoAnInChannelProperties UNIT_MAX = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT_MAX", ArduinoUnoMessages.UnitMaxValue_Label, ArduinoUnoMessages.UnitMaxValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ArduinoUnoAnInChannelProperties UNIT_MIN = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.UNIT_MIN", ArduinoUnoMessages.UnitMinValue_Label, ArduinoUnoMessages.UnitMinValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	// Raw values of the converter (see RawCalibration) : integers
	public static final ArduinoUnoAnInChannelProperties AMPLITUDE_MAX = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX", ArduinoUnoMessages.AmpMaxValue_Label, ArduinoUnoMessages.AmpMaxValue_Tooltip, "^[+-]?\\d+$");
	public static final ArduinoUnoAnInChannelProperties AMPLITUDE_MIN = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN", ArduinoUnoMessages.AmpMinValue_Label, ArduinoUnoMessages.AmpMinValue_Tooltip, "^[+-]?\\d+$");
	// True when AMPLITUDE_MIN/MAX or UNIT_MIN/MAX have been set by the user, false for the default calibration of the
	// hardware, not defined for the configurations saved before the calibration from the raw values (see ChannelCalibration)
	public static final ArduinoUnoAnInChannelProperties CALIBRATED = new ArduinoUnoAnInChannelProperties("ArduinoUnoAnInChannelProperties.CALIBRATED", ArduinoUnoMessages.Calibrated_Label, ArduinoUnoMessages.Calibrated_Tooltip, "^(true|false)$", "true:false");
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
		return (float) getADS1115FullScale(gain);
	}
	
	/**
	 * @param gain the gain of an ADS1115 input : 2/3 (or 0, gain of the ADS1X15 library for 2/3), 1, 2, 4, 8 or 16
	 * @return the full scale of the input (V), 0 if the gain is not known
	 */
	public static double getADS1115FullScale(String gain) {
		gain = gain == null ? "" : gain.trim();
		if(GAIN_0.equals(gain) || "0".equals(gain)) return 6.144;
		if(GAIN_1.equals(gain)) return 4.096;
		if(GAIN_2.equals(gain)) return 2.048;
		if(GAIN_4.equals(gain)) return 1.024;
		if(GAIN_8.equals(gain)) return 0.512;
		if(GAIN_16.equals(gain)) return 0.256;
		return 0;
	}
	
	/**
	 * Raw values of the converters for the minimum and maximum voltages : the line of the conversion of the
	 * acquisition processes and of the calibration (see ArduinoUnoCalibrationProtocol#getAnalogVoltage and
	 * #getADS1115Voltage) : voltage = raw * reference / levels
	 */
	public static final int R3_LEVELS = 1024; // 10 bits
	public static final int R4_LEVELS = 16384; // 14 bits
	public static final int ADS1115_LEVELS = 32768; // 16 bits signed
	
	/**
	 * Default calibration of an analog input : raw values of the converter <-> voltages, given by
	 * <ul>
	 * <li>analog inputs of the board : 0 / levels <-> 0 V / voltage of the analog reference of the module (DEFAULT,
	 * INTERNAL or EXTERNAL), levels of the revision of the board (1024 on R3, 16384 on R4) ;</li>
	 * <li>ADS1115 inputs : -32768 / 32768 <-> -/+ full scale of the gain of the input.</li>
	 * </ul>
	 * @return null if the channel is not in an analog inputs module or if its configuration is not known
	 */
	public static RawCalibration.Range getDefaultRange(Channel channel) {
		Module module = channel.getModule();
		if(module instanceof ArduinoUnoADS1115Module) {
			double fullScale = getADS1115FullScale(channel.getProperty(GAIN));
			if(fullScale <= 0) return null;
			return new RawCalibration.Range(-ADS1115_LEVELS, ADS1115_LEVELS, -fullScale, fullScale);
		}
		if(module instanceof ArduinoUnoAnInModule) {
			DACQConfiguration dacqConfiguration = module.getDACQConfiguration();
			if(dacqConfiguration == null) return null;
			int levels = ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration) ? R4_LEVELS : R3_LEVELS;
			return new RawCalibration.Range(0, levels, 0, ArduinoUnoAnInModuleProperties.getReferenceVoltage(module));
		}
		return null;
	}
	
	/**
	 * Calibration of the analog inputs : default values, changes of the hardware, conversion of the configurations
	 * saved before the calibration from the raw values
	 */
	public static final ChannelCalibration CALIBRATION = new ChannelCalibration(AMPLITUDE_MIN, AMPLITUDE_MAX, UNIT_MIN, UNIT_MAX, CALIBRATED, ArduinoUnoAnInChannelProperties::getDefaultRange);
	
	// Calibration of a channel created without analog inputs module : R3, reference of 5 V
	private static final RawCalibration.Range FALLBACK_RANGE = new RawCalibration.Range(0, R3_LEVELS, 0, 5);
	
	/**
	 * Conversion of the calibration of an analog input saved before the calibration from the raw values (see
	 * ChannelCalibration#migrate). AMPLITUDE_MIN/MAX have been captured in volts by the calibration dialog, except
	 * with the EXTERNAL reference, whose voltage was not known : raw values.
	 */
	public static void migrate(Channel channel) {
		Module module = channel.getModule();
		boolean amplitudesInVolts = !(module instanceof ArduinoUnoAnInModule) || !ArduinoUnoAnInModuleProperties.isExternalReference(module);
		CALIBRATION.migrate(channel, amplitudesInVolts);
	}
	
	public static void populateProperties(Channel channel) {
		ArduinoUnoChannelProperties.populateProperties(channel);
		channel.setProperty(UNIT, "Volt");
		channel.setProperty(GAIN, "0");
		// Default calibration : raw values of the converter <-> voltages
		CALIBRATION.applyDefault(channel, FALLBACK_RANGE);
	}

	public static Channel cloneChannel(ArduinoUnoChannel channel) {
		ArduinoUnoChannel newChannel = new ArduinoUnoChannel(null);
		ArduinoUnoChannelProperties.cloneChannel(channel, newChannel);
		newChannel.setProperty(UNIT, new String(channel.getProperty(UNIT)));
		newChannel.setProperty(GAIN, new String(channel.getProperty(GAIN)));
		CALIBRATION.copy(channel, newChannel);
		return newChannel;
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
