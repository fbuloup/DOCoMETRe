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
		
		if(segment == ArduinoUnoCodeSegmentProperties.INCLUDE) {
			if(!libraryAlreadyIncluded) {
				code = code + "//include library for ADS1115 modules\n";
				code = code + "#include \"ADS1X15.h\"\n\n";
				libraryAlreadyIncluded = true;
			}
			code = code + "//Declare ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
			code = code + "ADS1115 ADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "(" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ");\n\n";
		}
		
		if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			String mode = getProperty(ArduinoUnoADS1115ModuleProperties.MODE);
			String dataRate = getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
			code = code + "\n\t\t//Initialize ADS1115 module at " + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + "\n";
			code = code + "\t\tADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ".begin();\n";
			code = code + "\t\tADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ".setMode(" + mode + "); // Set mode (0 : continue, 1 : single shot)\n";
			code = code + "\t\tADS_" + getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) + ".setDataRate(" + dataRate + "); // Set speed convertion form 0 (#124ms) to 7 (#2.7ms)\n";
			
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
