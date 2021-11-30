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
		
//		String ADSName = "ADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
		
		if(segment == ArduinoUnoCodeSegmentProperties.INCLUDE) {
			if(!libraryAlreadyIncluded) {
				code = code + "//include library for ADS1115 modules\n";
				//code = code + "#include \"ADS1X15.h\"\n\n";
				code = code + "#include <Wire.h>\n\n";
				libraryAlreadyIncluded = true;
			}
//			code = code + "//Declare ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
//			code = code + "ADS1115 " + ADSName + "(" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ");\n\n";
		}
		
		if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
//			String mode = getProperty(ArduinoUnoADS1115ModuleProperties.MODE);
//			String dataRate = getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
//			code = code + "\n\t\t//Initialize ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
//			code = code + "\t\t" + ADSName + ".begin();\n";
//			code = code + "\t\t" + ADSName + ".setMode(" + mode + "); // Set mode (0 : continue, 1 : single shot)\n";
//			code = code + "\t\t" + ADSName + ".setDataRate(" + dataRate + "); // Set speed convertion form 0 (#124ms) to 7 (#2.7ms)\n";
			
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
					String dataRate = getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
					String moduleAddress = getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
					String gain = channel.getProperty(ArduinoUnoAnInChannelProperties.GAIN);
					if(gain == ArduinoUnoAnInChannelProperties.GAIN_0) gain = "0";
					if(gain == ArduinoUnoAnInChannelProperties.GAIN_4) gain = "3";
					if(gain == ArduinoUnoAnInChannelProperties.GAIN_8) gain = "4";
					if(gain == ArduinoUnoAnInChannelProperties.GAIN_16) gain = "5";
					code = code + "\n\t\tif(acquire_" + name + "_index == " + frequencyRatio + ") {\n";
					code = code + "\t\t\t\tacquire_" + name + "_index = 0;\n";
					code = code + "\t\t\t\t" + name + " = acquireADS1115AnalogInput(" + moduleAddress + ", " + channelNumber + ", " + gain + ", " + dataRate + ", " + transfer + ", " + transferNumber +  ");\n";
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
