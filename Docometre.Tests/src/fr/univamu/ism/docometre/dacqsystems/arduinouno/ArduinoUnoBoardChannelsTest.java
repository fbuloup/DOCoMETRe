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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [19/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;

/**
 * Channels of the modules available on each board of the Arduino system (see ArduinoUnoBoardChannels)
 */
public class ArduinoUnoBoardChannelsTest {
	
	private static final String R3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3;
	private static final String R4 = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI;
	private static final String PMC = ArduinoUnoDACQConfigurationProperties.REVISION_PMC;
	
	private static Channel channel(Module module, String channelNumber, boolean pmc) {
		Channel channel = new ArduinoUnoChannel(module);
		channel.setProperty(ChannelProperties.CHANNEL_NUMBER, channelNumber);
		if(pmc) channel.setProperty(ArduinoUnoChannelProperties.PMC, "true");
		return channel;
	}
	
	@Test
	public void analogInputsAIN0ToAIN2OnPMC() {
		Module module = new ArduinoUnoAnInModule(null);
		for (int i = 0; i < 6; i++) {
			Channel channel = channel(module, String.valueOf(i), false);
			assertTrue(ArduinoUnoBoardChannels.isAvailable(channel, R3));
			assertTrue(ArduinoUnoBoardChannels.isAvailable(channel, R4));
			assertEquals(i < ArduinoUnoBoardChannels.PMC_ANALOG_INPUTS, ArduinoUnoBoardChannels.isAvailable(channel, PMC), "AIN" + i);
		}
	}
	
	@Test
	public void analogOutputsOfEachBoard() {
		Module module = new ArduinoUnoAnOutModule(null);
		Channel pwm = channel(module, "3", false);
		Channel dac = channel(module, ArduinoUnoAnOutModule.DAC_CHANNEL_NUMBER, false);
		Channel ao = channel(module, "3", true);
		assertTrue(ArduinoUnoBoardChannels.isAvailable(pwm, R3));
		assertTrue(ArduinoUnoBoardChannels.isAvailable(pwm, R4));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(pwm, PMC));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(dac, R3));
		assertTrue(ArduinoUnoBoardChannels.isAvailable(dac, R4));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(dac, PMC));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(ao, R3));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(ao, R4));
		assertTrue(ArduinoUnoBoardChannels.isAvailable(ao, PMC));
		assertTrue(ArduinoUnoBoardChannels.isPMCChannel(ao));
		assertFalse(ArduinoUnoBoardChannels.isPMCChannel(pwm));
	}
	
	@Test
	public void digitalInputsOutputsNotYetOnPMC() {
		Channel channel = channel(new ArduinoUnoDigInOutModule(null), "2", false);
		assertTrue(ArduinoUnoBoardChannels.isAvailable(channel, R3));
		assertTrue(ArduinoUnoBoardChannels.isAvailable(channel, R4));
		assertFalse(ArduinoUnoBoardChannels.isAvailable(channel, PMC));
	}
	
	@Test
	public void sensorTypeOfTheAnalogInputs() {
		Module module = new ArduinoUnoAnInModule(null);
		assertEquals("V_0_10", ArduinoUnoAnInModuleProperties.getSensorType(module));
		module.setProperty(ArduinoUnoAnInModuleProperties.SENSOR_TYPE, "MA_4_20");
		assertEquals("MA_4_20", ArduinoUnoAnInModuleProperties.getSensorType(module));
		// Configurations created before : no sensor type
		module.setProperty(ArduinoUnoAnInModuleProperties.SENSOR_TYPE, null);
		assertEquals("V_0_10", ArduinoUnoAnInModuleProperties.getSensorType(module));
		assertEquals(ArduinoUnoAnInModuleProperties.SENSOR_TYPES.length, ArduinoUnoAnInModuleProperties.SENSOR_TYPES_LABELS.length);
		assertArrayEquals(ArduinoUnoAnInModuleProperties.SENSOR_TYPES, ArduinoUnoAnInModuleProperties.SENSOR_TYPE.getAvailableValues());
		for (String type : ArduinoUnoAnInModuleProperties.SENSOR_TYPES) assertTrue(type.matches(ArduinoUnoAnInModuleProperties.SENSOR_TYPE.getRegExp()));
	}
	
}
