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

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;

/**
 * Channels of the modules of an Arduino DACQ configuration available on its board. The modules have the channels of
 * all the boards (created with the configuration, or added when its board changes) :
 * <ul>
 * <li>analog inputs AIN0 to AIN5 : AIN0 to AIN2 only on the Portenta Machine Control (its inputs AI0 to AI2) ;</li>
 * <li>analog outputs : PWM pins of the Uno, DAC of the Uno R4 WiFi only, AO0 to AO3 of the Portenta Machine Control
 * only (channels marked with {@link ArduinoUnoChannelProperties#PMC}) ;</li>
 * <li>digital inputs/outputs and ADS1115 modules : Uno only (not supported yet on the Portenta Machine Control).</li>
 * </ul>
 * The channels which are not available on the board are not shown, not used, not transferred and their code is not
 * generated.
 */
public final class ArduinoUnoBoardChannels {
	
	/** Number of analog inputs of the Portenta Machine Control (AI0 to AI2) */
	public static final int PMC_ANALOG_INPUTS = 3;
	/** Number of analog outputs of the Portenta Machine Control (AO0 to AO3) */
	public static final int PMC_ANALOG_OUTPUTS = 4;
	/** Prefix of the names of the analog outputs of the Portenta Machine Control (AO0 to AO3) */
	public static final String PMC_ANALOG_OUTPUT_NAME = "AO";
	/** Maximum voltage of the analog outputs of the Portenta Machine Control */
	public static final double PMC_ANALOG_OUTPUT_MAXIMUM = 10.5;
	
	private ArduinoUnoBoardChannels() {
	}
	
	/**
	 * @return true if the channel is a channel of the Portenta Machine Control only (see ArduinoUnoChannelProperties.PMC)
	 */
	public static boolean isPMCChannel(Channel channel) {
		return "true".equals(channel.getProperty(ArduinoUnoChannelProperties.PMC));
	}
	
	/**
	 * @return true if the channel exists on the board of the DACQ configuration of its module
	 */
	public static boolean isAvailable(Channel channel) {
		Module module = channel.getModule();
		if(module == null || module.getDACQConfiguration() == null) return true;
		return isAvailable(channel, module.getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION));
	}
	
	/**
	 * @return true if the channel exists on the board of the revision
	 */
	public static boolean isAvailable(Channel channel, String revision) {
		boolean pmc = ArduinoUnoDACQConfigurationProperties.isPMC(revision);
		Module module = channel.getModule();
		if(module instanceof ArduinoUnoAnInModule) {
			if(!pmc) return true;
			int channelNumber = getChannelNumber(channel);
			return channelNumber >= 0 && channelNumber < PMC_ANALOG_INPUTS;
		}
		if(module instanceof ArduinoUnoAnOutModule) {
			if(isPMCChannel(channel)) return pmc;
			if(ArduinoUnoAnOutModule.isDACChannel(channel)) return ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(revision);
			return !pmc;
		}
		// Digital inputs/outputs, ADS1115 modules
		return !pmc;
	}
	
	/**
	 * @return true if the channel is used and exists on the board of the DACQ configuration of its module
	 */
	public static boolean isUsed(Channel channel) {
		return Boolean.valueOf(channel.getProperty(ArduinoUnoChannelProperties.USED)) && isAvailable(channel);
	}
	
	/**
	 * Channels of the board of the DACQ configuration : channels of the Portenta Machine Control added to its modules
	 * if it is the board, channels which do not exist on the board no more used and transferred.
	 * @return true if channels have been added or modified
	 */
	public static boolean update(DACQConfiguration dacqConfiguration) {
		boolean modified = false;
		String revision = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		for (Module module : dacqConfiguration.getModules()) {
			if(module instanceof ArduinoUnoAnOutModule && ArduinoUnoDACQConfigurationProperties.isPMC(revision)) 
				modified = addPMCAnalogOutputs((ArduinoUnoAnOutModule)module) || modified;
		}
		boolean transferModified = false;
		for (Module module : dacqConfiguration.getModules()) {
			for (Channel channel : module.getChannels()) {
				if(isAvailable(channel, revision)) continue;
				if(Boolean.valueOf(channel.getProperty(ArduinoUnoChannelProperties.USED))) {
					channel.setProperty(ArduinoUnoChannelProperties.USED, "false");
					modified = true;
				}
				if(Boolean.valueOf(channel.getProperty(ChannelProperties.TRANSFER))) {
					channel.setProperty(ChannelProperties.TRANSFER, "false");
					transferModified = true;
				}
			}
		}
		if(transferModified) dacqConfiguration.updateChannelsTransferNumber();
		return modified || transferModified;
	}
	
	/*
	 * Analog outputs AO0 to AO3 of the Portenta Machine Control added to the module if it has none
	 */
	private static boolean addPMCAnalogOutputs(ArduinoUnoAnOutModule module) {
		for (Channel channel : module.getChannels()) if(isPMCChannel(channel)) return false;
		for (int i = 0; i < PMC_ANALOG_OUTPUTS; i++) {
			Channel channel = module.createChannel();
			channel.setProperty(ChannelProperties.NAME, PMC_ANALOG_OUTPUT_NAME + i);
			channel.setProperty(ChannelProperties.CHANNEL_NUMBER, String.valueOf(i));
			channel.setProperty(ArduinoUnoChannelProperties.PMC, "true");
		}
		return true;
	}
	
	/*
	 * Channel number, -1 if it is not a number
	 */
	private static int getChannelNumber(Channel channel) {
		try {
			return Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER));
		} catch (NumberFormatException | NullPointerException e) {
			return -1;
		}
	}
	
}
