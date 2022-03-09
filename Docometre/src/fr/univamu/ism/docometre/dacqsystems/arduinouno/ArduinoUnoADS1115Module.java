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

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ArduinoUnoADS1115Module extends Module {
	
	private static boolean libraryAlreadyIncluded = false;
	
	public ArduinoUnoADS1115Module(DACQConfiguration dacqConfiguration) {
		super(dacqConfiguration);
		ArduinoUnoADS1115ModuleProperties.populateProperties(this);
		// Create the four channels
		for (int i = 0; i < 4; i++) {
			Channel arduinoUnoChannel = createChannel();
			arduinoUnoChannel.setProperty(ChannelProperties.NAME, "ADS1115_AIN" + i);
			arduinoUnoChannel.setProperty(ChannelProperties.CHANNEL_NUMBER, String.valueOf(i));
//			arduinoUnoChannel.setProperty(ChannelProperties.TRANSFER_NUMBER, String.valueOf(i));
		}
		
	}

	public static final long serialVersionUID = AbstractElement.serialVersionUID;

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		String ADSName = "ADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
		
		if(segment == ArduinoUnoCodeSegmentProperties.INCLUDE) {
			if(!libraryAlreadyIncluded) {
				code = code + "//include library for ADS1115 modules\n";
				code = code + "#include \"ADS1X15.h\"\n\n";
//				code = code + "#include <Wire.h>\n\n";
				libraryAlreadyIncluded = true;
			}
			code = code + "//Declare ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
			code = code + "ADS1115 " + ADSName + "(" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ");\n\n";
		}
		
		if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			String mode = getProperty(ArduinoUnoADS1115ModuleProperties.MODE);
			String dataRate = getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
			code = code + "\n\t\t//Initialize ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
			code = code + "\t\t" + ADSName + ".begin();\n";
			code = code + "\t\t" + ADSName + ".setMode(" + mode + "); // Set mode (0 : continue, 1 : single shot)\n";
			code = code + "\t\t" + ADSName + ".setDataRate(" + dataRate + "); // Set speed convertion form 0 (#124ms) to 7 (#2.7ms)\n";
			
		}
		
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
					code = code + "// ******** ADS1115 Entree analogique : " + name + "\n";
					code = code + "unsigned int " + name + ";\n";
					code = code + "byte acquire_" + name + "_index = " + frequencyRatio + ";\n";
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
				if(isUsed) {
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.ACQUISITION) {
				if(isUsed) {

//					String mode = getProperty(ArduinoUnoADS1115ModuleProperties.MODE);
//					String dataRate = getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
//					String moduleAddress = getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
					String gain = channel.getProperty(ArduinoUnoAnInChannelProperties.GAIN);
					if(gain.equals(ArduinoUnoAnInChannelProperties.GAIN_0)) gain = "0";
					else if(gain.equals(ArduinoUnoAnInChannelProperties.GAIN_4)) gain = "3";
					else if(gain.equals(ArduinoUnoAnInChannelProperties.GAIN_8)) gain = "4";
					else if(gain.equals(ArduinoUnoAnInChannelProperties.GAIN_16)) gain = "5";
					code = code + "\n\t\tif(acquire_" + name + "_index == " + frequencyRatio + ") {\n";
					code = code + "\t\t\t\tacquire_" + name + "_index = 0;\n";
					code = code + "\t\t\t\t" + name + " = acquireADS1115AnalogInput(" + ADSName + ", " + channelNumber + ", " + gain + ", " + transfer + ", " + transferNumber +  ");\n";
					code = code + "\t\t}\n";
					code = code + "\t\tacquire_" + name + "_index += 1;\n\n";
					
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.TRANSFER) {
				if(isTransfered) {
					
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
		libraryAlreadyIncluded = false;

	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub

	}

	@Override
	public Channel initializeChannelProperties() {
		ArduinoUnoChannel arduinoUnoChannel = new ArduinoUnoChannel(this);
		ArduinoUnoAnInChannelProperties.populateProperties(arduinoUnoChannel);
		String gsfProcess = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
		arduinoUnoChannel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, gsfProcess);
		arduinoUnoChannel.setProperty(ChannelProperties.NAME, "A");
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
