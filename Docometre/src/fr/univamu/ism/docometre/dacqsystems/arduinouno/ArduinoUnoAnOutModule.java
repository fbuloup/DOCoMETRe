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

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ArduinoUnoAnOutModule extends Module {

	public ArduinoUnoAnOutModule(DACQConfiguration dacqConfiguration) {
		super(dacqConfiguration);
	}

	private static final long serialVersionUID = AbstractElement.serialVersionUID;;

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		int delay = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.ARDUINO_DELAY_TIME_AFTER_SERIAL_PRINT);
		
		for (int i = 0; i < getChannelsNumber(); i++) {
			
			Channel channel = getChannel(i);
			
			String used = channel.getProperty(ArduinoUnoChannelProperties.USED);
			boolean isUsed = Boolean.valueOf(used);
			String name = channel.getProperty(ChannelProperties.NAME);
			String transferNumber = channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
			String channelNumber = channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
			String gsfProcess = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
			String transfer = channel.getProperty(ChannelProperties.TRANSFER);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			String sfChannel = channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			float sfFloat = Float.parseFloat(sfChannel);
			boolean isTransfered = Boolean.valueOf(transfer);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
			
			if (segment == ArduinoUnoCodeSegmentProperties.DECLARATION) {
				if(isUsed) {
					code = code + "// ******** Sortie analogique : " + name + "\n";
					code = code + "unsigned int " + name + ";\n";
					code = code + "byte generate_" + name + "_index = " + frequencyRatio + ";\n";
//					code = code + "unsigned long lastGenerateTime_" + name + ";\n\n";
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
				if(isUsed) {
					code = code + "\t\tpinMode(" + channelNumber + ", OUTPUT);\n";
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.GENERATION) {
				if(isUsed) {
					code = code + "\n\t\tif(generate_" + name + "_index == " + frequencyRatio + ") {\n";
					code = code + "\t\t\t\tgenerate_" + name + "_index = 0;\n";
					code = code + "\t\t\t\tanalogWrite(" + channelNumber + ", " + name + ");\n";
					if(isTransfered) {
						code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%d\", " + transferNumber + ", " + name + ");\n";
						code = code + "\t\t\t\tSerial.println(serialMessage);\n";
						if(delay > 0)code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
					}
					code = code + "\t\t}\n";
					code = code + "\t\tgenerate_" + name + "_index += 1;\n\n";
				}
			}
			
		}
		
		return code;
	}

	@Override
	public void recovery() {
		// TODO Auto-generated method stub

	}

	@Override
	public void generation() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub

	}

	@Override
	public Channel initializeChannelProperties() {
		ArduinoUnoChannel arduinoUnoChannel = new ArduinoUnoChannel(this);
		ArduinoUnoAnOutChannelProperties.populateProperties(arduinoUnoChannel);
		String gsfProcess = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
		arduinoUnoChannel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, gsfProcess);
		arduinoUnoChannel.setProperty(ChannelProperties.NAME, "PIN");
		arduinoUnoChannel.setProperty(ChannelProperties.CHANNEL_NUMBER, "0");
		arduinoUnoChannel.setProperty(ChannelProperties.TRANSFER_NUMBER,"0");
		return arduinoUnoChannel;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
