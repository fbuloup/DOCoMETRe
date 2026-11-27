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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;

/**
 * Clone of an Arduino Uno DACQ configuration : same modules and channels, no object shared with the original.
 * The DACQ configuration reads the default preferences : to be run as a JUnit Plug-in Test.
 */
public class ArduinoUnoCloneTest {
	
	@Test
	public void cloneOfAConfiguration() throws CloneNotSupportedException {
		ArduinoUnoDACQConfiguration dacqConfiguration = new ArduinoUnoDACQConfiguration();
		new ArduinoUnoADS1115Module(dacqConfiguration);
		ArduinoUnoAnInModule analogInputs = null;
		for (Module module : dacqConfiguration.getModules()) if(module instanceof ArduinoUnoAnInModule) analogInputs = (ArduinoUnoAnInModule) module;
		assertNotNull(analogInputs);
		analogInputs.setProperty(ArduinoUnoAnInModuleProperties.EXTERNAL_REFERENCE_VOLTAGE, "3.3");
		analogInputs.getChannel(0).setProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX, "50");
		
		ArduinoUnoDACQConfiguration clone = (ArduinoUnoDACQConfiguration) dacqConfiguration.clone();
		
		Module[] modules = dacqConfiguration.getModules();
		Module[] clonedModules = clone.getModules();
		// Same modules (the default modules of the constructor are not added twice), in the same order
		assertEquals(modules.length, clonedModules.length);
		for (int i = 0; i < modules.length; i++) {
			assertNotNull(clonedModules[i]);
			assertEquals(modules[i].getClass(), clonedModules[i].getClass());
			assertNotSame(modules[i], clonedModules[i]);
			assertSame(clone, clonedModules[i].getDACQConfiguration());
			// Same channels (the four default channels of the ADS1115 module are not added twice)
			assertEquals(modules[i].getChannelsNumber(), clonedModules[i].getChannelsNumber());
			for (int j = 0; j < modules[i].getChannelsNumber(); j++) {
				Channel channel = modules[i].getChannel(j);
				Channel clonedChannel = clonedModules[i].getChannel(j);
				assertNotSame(channel, clonedChannel);
				assertSame(clonedModules[i], clonedChannel.getModule());
				assertEquals(channel.getProperty(ChannelProperties.NAME), clonedChannel.getProperty(ChannelProperties.NAME));
			}
		}
		// Properties and calibration copied
		ArduinoUnoAnInModule clonedAnalogInputs = null;
		for (Module module : clonedModules) if(module instanceof ArduinoUnoAnInModule) clonedAnalogInputs = (ArduinoUnoAnInModule) module;
		assertEquals("3.3", clonedAnalogInputs.getProperty(ArduinoUnoAnInModuleProperties.EXTERNAL_REFERENCE_VOLTAGE));
		assertEquals("50", clonedAnalogInputs.getChannel(0).getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX));
		assertEquals("true", clonedAnalogInputs.getChannel(0).getProperty(ArduinoUnoAnInChannelProperties.CALIBRATED));
		assertEquals("false", clonedAnalogInputs.getChannel(1).getProperty(ArduinoUnoAnInChannelProperties.CALIBRATED));
		// Modifying the clone does not modify the original
		clonedAnalogInputs.getChannel(0).setProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX, "60");
		assertEquals("50", analogInputs.getChannel(0).getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX));
	}

}
