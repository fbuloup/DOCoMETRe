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

import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.DocometreMessages;

public final class ADWinDACQConfigurationProperties extends Property {
	
//	UPDATE_VARIABLE("", "", ""),
	public static final ADWinDACQConfigurationProperties ADBASIC_COMPILER = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.ADBASIC_COMPILER", ADWinMessages.ADBasicCompilerPath_Label, ADWinMessages.ADBasicCompilerPath_Tooltip, "(.)+");
	public static final ADWinDACQConfigurationProperties BTL_FILE = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.BTL_FILE", ADWinMessages.BootloaderPath_Label, ADWinMessages.BootloaderPath_Tooltip, "(.)+");
	public static final ADWinDACQConfigurationProperties IP_ADDRESS = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.IP_ADDRESS", ADWinMessages.IPAddress_Label, "", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}" );
	public static final ADWinDACQConfigurationProperties DEVICE_NUMBER = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.DEVICE_NUMBER", ADWinMessages.DeviceNumber_Label, "", "\\d+");
	public static final ADWinDACQConfigurationProperties TCPIP_SERVER_DEVICE_NUMBER = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER", ADWinMessages.TCPIPServerDeviceNumber_Label, "", "\\d+");
	public static final ADWinDACQConfigurationProperties PORT_NUMBER = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.PORT_NUMBER", ADWinMessages.TCPIPDevicePortNumber_Label, "", "\\d{1,5}");
	public static final ADWinDACQConfigurationProperties TIME_OUT = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.TIME_OUT", ADWinMessages.TimeOut_Label, ADWinMessages.TimeOut_Tooltip, "\\d+");
	public static final ADWinDACQConfigurationProperties SYSTEM_TYPE = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.SYSTEM_TYPE", ADWinMessages.SytemType_Label, ADWinMessages.SystemType_Tooltip, "^(Gold|Pro)$", "Gold:Pro");
	public static final ADWinDACQConfigurationProperties CPU_TYPE = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.CPU_TYPE", ADWinMessages.CPUType_Label, ADWinMessages.CPUType_Tooltip, "^(I|II)$", "I:II");
	public static final ADWinDACQConfigurationProperties ADBASIC_VERSION = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.ADBASIC_VERSION", ADWinMessages.ADBasicVersion_Label, ADWinMessages.ADBasicVersion_Tooltip, "^(>=5|<=4)$", ">=5:<=4");
	public static final ADWinDACQConfigurationProperties GLOBAL_FREQUENCY = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY", DocometreMessages.GlobalFrequency_Label, DocometreMessages.GlobalFrequency_Tooltip, "(^\\d{1,10}$)");
	public static final ADWinDACQConfigurationProperties LIBRARIES_ABSOLUTE_PATH = new ADWinDACQConfigurationProperties("ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH", ADWinMessages.LibrariesAbsolutePath_Label, ADWinMessages.LibrariesAbsolutePath_Tooltip, "(.)+");
	
	public static String GOLD = "Gold";
	public static String PRO = "Pro";
	public static String I = "I";
	public static String II = "II";
	public static String VSUP5 = ">=5";
	public static String VINF4 = "<=4";
	public static String[] ADBasicVersions = new String[] { VINF4, VSUP5 };
	public static String[] SystemsTypes = new String[] { GOLD, PRO };
	public static String[] CPUTypes = new String[] { I, II };

	public static void populateProperties(DACQConfiguration daqGeneralConfiguration) {
		DACQConfigurationProperties.populateProperties(daqGeneralConfiguration);
		daqGeneralConfiguration.setProperty(ADBASIC_COMPILER, "C:\\ADwin\\ADbasic\\ADbasicCompiler.exe");
		daqGeneralConfiguration.setProperty(BTL_FILE, "C:\\ADwin\\adwin9.btl");
		daqGeneralConfiguration.setProperty(IP_ADDRESS, "192.168.0.2");
		daqGeneralConfiguration.setProperty(DEVICE_NUMBER, "1");
		daqGeneralConfiguration.setProperty(PORT_NUMBER, "6543");
		daqGeneralConfiguration.setProperty(TCPIP_SERVER_DEVICE_NUMBER, "1");
		daqGeneralConfiguration.setProperty(TIME_OUT, "1000");
		daqGeneralConfiguration.setProperty(SYSTEM_TYPE, "Gold");
		daqGeneralConfiguration.setProperty(CPU_TYPE, "I");
		daqGeneralConfiguration.setProperty(ADBASIC_VERSION, ">=5");
		daqGeneralConfiguration.setProperty(GLOBAL_FREQUENCY, "2000");
		daqGeneralConfiguration.setProperty(LIBRARIES_ABSOLUTE_PATH, "");
	}

	public static ADWinDACQConfiguration cloneConfiguration(ADWinDACQConfiguration daqGeneralConfiguration) {
		ADWinDACQConfiguration clonedGeneralConfiguration = new ADWinDACQConfiguration();
		clonedGeneralConfiguration.setProperty(ADBASIC_COMPILER, daqGeneralConfiguration.getProperty(ADBASIC_COMPILER));
		clonedGeneralConfiguration.setProperty(BTL_FILE, daqGeneralConfiguration.getProperty(BTL_FILE));
		clonedGeneralConfiguration.setProperty(IP_ADDRESS, daqGeneralConfiguration.getProperty(IP_ADDRESS));
		clonedGeneralConfiguration.setProperty(DEVICE_NUMBER, daqGeneralConfiguration.getProperty(DEVICE_NUMBER));
		clonedGeneralConfiguration.setProperty(TCPIP_SERVER_DEVICE_NUMBER, daqGeneralConfiguration.getProperty(TCPIP_SERVER_DEVICE_NUMBER));
		clonedGeneralConfiguration.setProperty(PORT_NUMBER, daqGeneralConfiguration.getProperty(PORT_NUMBER));
		clonedGeneralConfiguration.setProperty(TIME_OUT, daqGeneralConfiguration.getProperty(TIME_OUT));
		clonedGeneralConfiguration.setProperty(SYSTEM_TYPE, daqGeneralConfiguration.getProperty(SYSTEM_TYPE));
		clonedGeneralConfiguration.setProperty(CPU_TYPE, daqGeneralConfiguration.getProperty(CPU_TYPE));
		clonedGeneralConfiguration.setProperty(ADBASIC_VERSION, daqGeneralConfiguration.getProperty(ADBASIC_VERSION));
		clonedGeneralConfiguration.setProperty(GLOBAL_FREQUENCY, daqGeneralConfiguration.getProperty(GLOBAL_FREQUENCY));
		clonedGeneralConfiguration.setProperty(LIBRARIES_ABSOLUTE_PATH, daqGeneralConfiguration.getProperty(LIBRARIES_ABSOLUTE_PATH));
		return clonedGeneralConfiguration;
	}
	
	private ADWinDACQConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinDACQConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
}
