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

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ADWinAnInChannelProperties extends Property {
	
	public static final ADWinAnInChannelProperties GAIN = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.GAIN", ADWinMessages.Gain_Label, ADWinMessages.Gain_Tooltip,"^(1|2|4|8$)", "1:2:4:8");
	public static final ADWinAnInChannelProperties UNIT = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.UNIT", ADWinMessages.Unit_Label, ADWinMessages.Unit_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$");
	public static final ADWinAnInChannelProperties UNIT_MAX = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.UNIT_MAX", ADWinMessages.UnitMaxValue_Label, ADWinMessages.UnitMaxValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinAnInChannelProperties UNIT_MIN = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.UNIT_MIN", ADWinMessages.UnitMinValue_Label, ADWinMessages.UnitMinValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinAnInChannelProperties AMPLITUDE_MAX = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.AMPLITUDE_MAX", ADWinMessages.AmpMaxValue_Label, ADWinMessages.AmpMaxValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinAnInChannelProperties AMPLITUDE_MIN = new ADWinAnInChannelProperties("ADWinAnInChannelProperties.AMPLITUDE_MIN", ADWinMessages.AmpMinValue_Label, ADWinMessages.AmpMinValue_Tooltip, "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	
	public static String GAIN_1 = "1";
	public static String GAIN_2 = "2";
	public static String GAIN_4 = "4";
	public static String GAIN_8 = "8";
	public static String[] GAINS = new String[]{GAIN_1, GAIN_2, GAIN_4, GAIN_8};
	
	public static void populateProperties(Channel channel) {
		ChannelProperties.populateProperties(channel);
		channel.setProperty(UNIT, "Volt");
		channel.setProperty(GAIN, "1");
		channel.setProperty(UNIT_MAX, "10");
		channel.setProperty(UNIT_MIN, "-10");
		channel.setProperty(AMPLITUDE_MAX, "10");
		channel.setProperty(AMPLITUDE_MIN, "-10");
	}

	public static Channel cloneChannel(ADWinChannel channel) {
		ADWinChannel newChannel = new ADWinChannel(null);
		ChannelProperties.cloneChannel(channel, newChannel);
		newChannel.setProperty(GAIN, new String(channel.getProperty(GAIN)));
		newChannel.setProperty(UNIT, new String(channel.getProperty(UNIT)));
		newChannel.setProperty(UNIT_MAX, new String(channel.getProperty(UNIT_MAX)));
		newChannel.setProperty(UNIT_MIN, new String(channel.getProperty(UNIT_MIN)));
		newChannel.setProperty(AMPLITUDE_MAX, new String(channel.getProperty(AMPLITUDE_MAX)));
		newChannel.setProperty(AMPLITUDE_MIN, new String(channel.getProperty(AMPLITUDE_MIN)));
		return channel;
	}
	
	public static double toVolt(ADWinChannel channel, double value) {
		if(channel.getModule() instanceof ADWinAnInModule) {
			ADWinAnInModule adwinAnInModule = (ADWinAnInModule) channel.getModule();
			double aMax = Double.parseDouble(adwinAnInModule.getProperty(ADWinAnInModuleProperties.AMPLITUDE_MAX));
			double aMin = Double.parseDouble(adwinAnInModule.getProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN));
			return (aMax - aMin)/65535*value + aMin;
		}
		return 0;
	}
	
	private ADWinAnInChannelProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinAnInChannelProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
	
}
