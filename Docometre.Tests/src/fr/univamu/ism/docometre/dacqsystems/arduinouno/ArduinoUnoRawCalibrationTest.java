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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [24/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Module;

/**
 * Calibration of the Arduino Uno analog inputs from the raw values of the converter (see ChannelCalibration).
 * The DACQ configuration reads the default preferences : to be run as a JUnit Plug-in Test.
 */
public class ArduinoUnoRawCalibrationTest {

	private static void assertCalibration(Channel channel, String aMin, String aMax, String uMin, String uMax, String calibrated) {
		assertEquals(aMin, channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN));
		assertEquals(aMax, channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX));
		assertEquals(uMin, channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN));
		assertEquals(uMax, channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX));
		assertEquals(calibrated, channel.getProperty(ArduinoUnoAnInChannelProperties.CALIBRATED));
	}

	private static ArduinoUnoAnInModule getAnalogInputsModule(ArduinoUnoDACQConfiguration dacqConfiguration) {
		for (Module module : dacqConfiguration.getModules()) if(module instanceof ArduinoUnoAnInModule) return (ArduinoUnoAnInModule) module;
		return null;
	}

	@Test
	public void defaultCalibrationFollowsTheReferenceAndTheRevision() {
		ArduinoUnoDACQConfiguration dacqConfiguration = new ArduinoUnoDACQConfiguration();
		dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.REVISION, ArduinoUnoDACQConfigurationProperties.REVISION_R3);
		ArduinoUnoAnInModule module = getAnalogInputsModule(dacqConfiguration);
		assertNotNull(module);
		Channel channel = module.getChannel(0);
		// R3 : 10 bits, DEFAULT reference of 5 V
		assertCalibration(channel, "0", "1024", "0", "5", "false");
		module.setProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE, ArduinoUnoAnInModuleProperties.INTERNAL);
		assertCalibration(channel, "0", "1024", "0", "1.1", "false");
		// R4 : 14 bits, INTERNAL reference of 1.5 V
		dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.REVISION, ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI);
		assertCalibration(channel, "0", "16384", "0", "1.5", "false");
		// EXTERNAL reference : voltage of the module
		module.setProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE, ArduinoUnoAnInModuleProperties.AR_EXTERNAL);
		assertCalibration(channel, "0", "16384", "0", "5", "false");
		module.setProperty(ArduinoUnoAnInModuleProperties.EXTERNAL_REFERENCE_VOLTAGE, "3.3");
		assertCalibration(channel, "0", "16384", "0", "3.3", "false");
	}

	@Test
	public void calibratedInputDoesNotFollowTheHardware() {
		ArduinoUnoDACQConfiguration dacqConfiguration = new ArduinoUnoDACQConfiguration();
		dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.REVISION, ArduinoUnoDACQConfigurationProperties.REVISION_R3);
		ArduinoUnoAnInModule module = getAnalogInputsModule(dacqConfiguration);
		Channel channel = module.getChannel(1);
		channel.setProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX, "50");
		module.setProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE, ArduinoUnoAnInModuleProperties.INTERNAL);
		dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.REVISION, ArduinoUnoDACQConfigurationProperties.REVISION_R4_MINIMA);
		assertCalibration(channel, "0", "1024", "0", "50", "true");
	}

	@Test
	public void defaultCalibrationOfADS1115FollowsTheGain() {
		ArduinoUnoDACQConfiguration dacqConfiguration = new ArduinoUnoDACQConfiguration();
		ArduinoUnoADS1115Module module = new ArduinoUnoADS1115Module(dacqConfiguration);
		Channel channel = module.getChannel(0);
		// Gain 2/3 by default : +/-6.144 V
		assertCalibration(channel, "-32768", "32768", "-6.144", "6.144", "false");
		channel.setProperty(ArduinoUnoAnInChannelProperties.GAIN, ArduinoUnoAnInChannelProperties.GAIN_1);
		assertCalibration(channel, "-32768", "32768", "-4.096", "4.096", "false");
		channel.setProperty(ArduinoUnoAnInChannelProperties.GAIN, ArduinoUnoAnInChannelProperties.GAIN_16);
		assertCalibration(channel, "-32768", "32768", "-0.256", "0.256", "false");
	}

	@Test
	public void formerCalibrationsAreConverted() {
		ArduinoUnoDACQConfiguration dacqConfiguration = new ArduinoUnoDACQConfiguration();
		dacqConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.REVISION, ArduinoUnoDACQConfigurationProperties.REVISION_R3);
		ArduinoUnoAnInModule module = getAnalogInputsModule(dacqConfiguration);
		// Identity (former default calibration) : default calibration
		Channel identity = module.getChannel(0);
		setLegacyCalibration(identity, "0", "5", "0", "5");
		ArduinoUnoAnInChannelProperties.migrate(identity);
		assertCalibration(identity, "0", "1024", "0", "5", "false");
		// DEFAULT reference : amplitudes captured in volts, converted to raw values
		Channel volts = module.getChannel(1);
		setLegacyCalibration(volts, "1", "4", "0", "300");
		ArduinoUnoAnInChannelProperties.migrate(volts);
		assertCalibration(volts, "204.8", "819.2", "0", "300", "true");
		// EXTERNAL reference : amplitudes captured as raw values (voltage not known), kept
		module.setProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE, ArduinoUnoAnInModuleProperties.EXTERNAL);
		Channel raw = module.getChannel(2);
		setLegacyCalibration(raw, "100", "900", "0", "50");
		ArduinoUnoAnInChannelProperties.migrate(raw);
		assertCalibration(raw, "100", "900", "0", "50", "true");
	}

	private static void setLegacyCalibration(Channel channel, String aMin, String aMax, String uMin, String uMax) {
		channel.setProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN, aMin);
		channel.setProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX, aMax);
		channel.setProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN, uMin);
		channel.setProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX, uMax);
		// Not defined in the configurations saved before the calibration from the raw values
		channel.setProperty(ArduinoUnoAnInChannelProperties.CALIBRATED, null);
	}

}
