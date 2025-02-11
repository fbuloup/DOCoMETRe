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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ArduinoUnoDACQConfigurationProperties extends Property {
	
	public static final ArduinoUnoDACQConfigurationProperties BUILDER_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.BUILDER_PATH", ArduinoUnoMessages.BuilderPath_Label, ArduinoUnoMessages.BuilderPath_Tooltip, "(.)+");
	public static final ArduinoUnoDACQConfigurationProperties AVRDUDE_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH", ArduinoUnoMessages.AVRDudePath_Label, ArduinoUnoMessages.AVRDudePath_Tooltip, "(.)+");
	public static final ArduinoUnoDACQConfigurationProperties ARDUINOCLI_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH", ArduinoUnoMessages.ArduinoCLIPath_Label, ArduinoUnoMessages.ArduinoCLIPath_Tooltip, "(.)+");
	public static final ArduinoUnoDACQConfigurationProperties USE_ARDUINOCLI = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI", ArduinoUnoMessages.UseArduinoCLI_Label, ArduinoUnoMessages.UseArduinoCLI_Tooltip, "^(true|false)$", "true:false");	
	public static final ArduinoUnoDACQConfigurationProperties DEVICE_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.DEVICE_PATH", ArduinoUnoMessages.DevicePath_Label, ArduinoUnoMessages.DevicePath_Tooltip, Platform.getOS().equals(Platform.OS_WIN32)?"^COM\\d+$":"(.)+");
	public static final ArduinoUnoDACQConfigurationProperties BAUD_RATE = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.BAUD_RATE", ArduinoUnoMessages.DeviceBaudRate_Label, ArduinoUnoMessages.DeviceBaudRate_Tooltip, "^(9600|115200|1000000|1500000|2000000)$", "9600:115200:1000000:1500000:2000000");
	public static final ArduinoUnoDACQConfigurationProperties GLOBAL_FREQUENCY = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY", ArduinoUnoMessages.GlobalFrequency_Label, ArduinoUnoMessages.GlobalFrequency_Tooltip, "(^\\d{1,10}$)");
	public static final ArduinoUnoDACQConfigurationProperties LIBRARIES_ABSOLUTE_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH", ArduinoUnoMessages.LibrariesAbsolutePath_Label, ArduinoUnoMessages.LibrariesAbsolutePath_Tooltip, "(.)+");
	public static final ArduinoUnoDACQConfigurationProperties USER_LIBRARIES_ABSOLUTE_PATH = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH", ArduinoUnoMessages.UserLibrariesAbsolutePath_Label, ArduinoUnoMessages.UserLibrariesAbsolutePath_Tooltip, "(.)+");
	public static final ArduinoUnoDACQConfigurationProperties REVISION = new ArduinoUnoDACQConfigurationProperties("ArduinoUnoDACQConfigurationProperties.REVISION", ArduinoUnoMessages.Revision_Label, ArduinoUnoMessages.Revision_Tooltip, "^(R3|R4)$", "R3:R4");

	
	public static String BAUD_RATE_9600 = "9600";
	public static String BAUD_RATE_115200 = "115200";
	public static String BAUD_RATE_1000000 = "1000000";
	public static String BAUD_RATE_1500000 = "1500000";
	public static String BAUD_RATE_2000000 = "2000000";
	public static String[] BAUD_RATES = new String[] { BAUD_RATE_9600, BAUD_RATE_115200, BAUD_RATE_1000000, BAUD_RATE_1500000, BAUD_RATE_2000000 };
	public static String REVISION_R3 = "R3";
	public static String REVISION_R4 = "R4";
	public static String[] REVISIONS = new String[] { REVISION_R3, REVISION_R4 };
	
	public static void populateProperties(DACQConfiguration daqGeneralConfiguration) {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		DACQConfigurationProperties.populateProperties(daqGeneralConfiguration);
		daqGeneralConfiguration.setProperty(BUILDER_PATH, "");
		daqGeneralConfiguration.setProperty(AVRDUDE_PATH, "");
		daqGeneralConfiguration.setProperty(ARDUINOCLI_PATH, "");
		daqGeneralConfiguration.setProperty(USE_ARDUINOCLI, "false");
		if(Platform.OS_WIN32.equals(Platform.getOS())) daqGeneralConfiguration.setProperty(DEVICE_PATH, "COM3");
		else daqGeneralConfiguration.setProperty(DEVICE_PATH, "/dev/tty.usb*");
		daqGeneralConfiguration.setProperty(BAUD_RATE, BAUD_RATE_2000000);
		daqGeneralConfiguration.setProperty(GLOBAL_FREQUENCY, "100");
		daqGeneralConfiguration.setProperty(LIBRARIES_ABSOLUTE_PATH, defaults.get(LIBRARIES_ABSOLUTE_PATH.getKey(), ""));
		daqGeneralConfiguration.setProperty(USER_LIBRARIES_ABSOLUTE_PATH, "");
		daqGeneralConfiguration.setProperty(REVISION, REVISION_R3);
	}

	public static ArduinoUnoDACQConfiguration cloneConfiguration(ArduinoUnoDACQConfiguration daqGeneralConfiguration) {
		ArduinoUnoDACQConfiguration clonedGeneralConfiguration = new ArduinoUnoDACQConfiguration();
		clonedGeneralConfiguration.setProperty(BUILDER_PATH, new String(daqGeneralConfiguration.getProperty(BUILDER_PATH)));
		clonedGeneralConfiguration.setProperty(AVRDUDE_PATH, new String(daqGeneralConfiguration.getProperty(AVRDUDE_PATH)));
		clonedGeneralConfiguration.setProperty(ARDUINOCLI_PATH, new String(daqGeneralConfiguration.getProperty(ARDUINOCLI_PATH)));
		clonedGeneralConfiguration.setProperty(USE_ARDUINOCLI, new String(daqGeneralConfiguration.getProperty(USE_ARDUINOCLI)));
		clonedGeneralConfiguration.setProperty(DEVICE_PATH, new String(daqGeneralConfiguration.getProperty(DEVICE_PATH)));
		clonedGeneralConfiguration.setProperty(BAUD_RATE, new String(daqGeneralConfiguration.getProperty(BAUD_RATE)));
		clonedGeneralConfiguration.setProperty(GLOBAL_FREQUENCY, new String(daqGeneralConfiguration.getProperty(GLOBAL_FREQUENCY)));
		clonedGeneralConfiguration.setProperty(LIBRARIES_ABSOLUTE_PATH, new String(daqGeneralConfiguration.getProperty(LIBRARIES_ABSOLUTE_PATH)));
		clonedGeneralConfiguration.setProperty(USER_LIBRARIES_ABSOLUTE_PATH, new String(daqGeneralConfiguration.getProperty(USER_LIBRARIES_ABSOLUTE_PATH)));
		clonedGeneralConfiguration.setProperty(REVISION, new String(daqGeneralConfiguration.getProperty(REVISION)));
		return clonedGeneralConfiguration;
	}
	
	private ArduinoUnoDACQConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ArduinoUnoDACQConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
