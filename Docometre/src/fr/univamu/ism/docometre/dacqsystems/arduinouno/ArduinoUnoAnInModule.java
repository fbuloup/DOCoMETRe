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

public class ArduinoUnoAnInModule extends Module {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	public ArduinoUnoAnInModule(DACQConfiguration daqGeneralConfiguration) {
		super(daqGeneralConfiguration);
		ArduinoUnoAnInModuleProperties.populateProperties(this);
	}
	
	/*
	 * Analog reference or voltage of the EXTERNAL reference changed : default calibration of the analog inputs which
	 * are not calibrated
	 */
	@Override
	public void setProperty(Property property, String value) {
		super.setProperty(property, value);
		if(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE.equals(property) || ArduinoUnoAnInModuleProperties.EXTERNAL_REFERENCE_VOLTAGE.equals(property))
			updateDefaultCalibration();
	}
	
	/**
	 * Default calibration of the analog inputs which are not calibrated updated (change of the analog reference, of
	 * the voltage of the EXTERNAL reference or of the revision of the board)
	 */
	public void updateDefaultCalibration() {
		for (Channel channel : getChannels()) ArduinoUnoAnInChannelProperties.CALIBRATION.updateDefault(channel);
	}

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			// Reference of the revision of the board (configurations saved with a reference of the other revision)
			String analogReference = ArduinoUnoAnInModuleProperties.getReferenceOfRevision(getProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE), ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration));
			code = code +  "\t\t// Set analog ref.\n";
			code = code + "\t\tanalogReference(" + analogReference + ");\n";
			if(ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration) && hasUsedChannel()) {
				// Uno R4 : a first read of each input used opens the ADC and adds the input to its scan (analogRead() of the core
				// converts all the inputs already read) : one scan per period then converts all the inputs (see ACQUISITION)
				code = code + "\t\t// Analog inputs used added to the scan of the ADC\n";
				for (Channel channel : getChannels()) 
					if(isUsed(channel)) code = code + "\t\tanalogRead(" + channel.getProperty(ChannelProperties.CHANNEL_NUMBER) + ");\n";
				if(ArduinoUnoAnInModuleProperties.isMemoryEffectReduced(this)) {
					// Sampling time of the ADC lengthened against the memory effect of its sample-and-hold, written after the
					// opening of the ADC (analogRead() keeps it afterwards)
					int states = ArduinoUnoAnInModuleProperties.R4_SAMPLING_STATES;
					code = code + "\t\t// Memory effect of the sample-and-hold reduced : sampling time of " + states + " states instead of 13\n";
					code = code + "\t\tfor(int i = 0; i < 16; i++) R_ADC0->ADSSTR[i] = " + states + ";\n";
					code = code + "\t\tR_ADC0->ADSSTRL = " + states + ";\n";
				}
			}
		}
		
		if (segment == ArduinoUnoCodeSegmentProperties.ACQUISITION && ArduinoUnoAnInModuleProperties.isR4(dacqConfiguration) && hasUsedChannel()) {
			// Uno R4 : one scan of the ADC per period, when at least one input is acquired, converts all the inputs used. Started
			// in its control register : analogRead() of the core would reconfigure the ADC, convert all the inputs and clear the
			// data register of its input (cleared after reading). The values are read in the data registers by acquireAnalogInput()
			String condition = "";
			for (Channel channel : getChannels()) {
				if(!isUsed(channel)) continue;
				String name = channel.getProperty(ChannelProperties.NAME);
				int frequencyRatio = getFrequencyRatio(channel);
				condition = condition + (condition.isEmpty() ? "" : " || ") + "acquire_" + name + "_index == " + frequencyRatio;
			}
			code = code + "\n\t\t// Analog inputs : one scan of the ADC converts all the inputs used\n";
			code = code + "\t\tif(" + condition + ") {\n";
			code = code + "\t\t\t\tR_ADC0->ADCSR_b.ADST = 1;\n";
			code = code + "\t\t\t\twhile(R_ADC0->ADCSR_b.ADST);\n";
			code = code + "\t\t}\n";
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
					code = code + "// ******** Entree analogique : " + name + "\n";
					// Physical value : calibration from the raw value of the converter (see acquireAnalogInput())
					code = code + "float " + name + ";\n";
					code = code + "byte acquire_" + name + "_index = " + frequencyRatio + ";\n";
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
				if(isUsed) {
					// Initial values, as after a board reset : the process can be restarted by software
					code = code + "\t\t" + name + " = 0;\n";
					code = code + "\t\tacquire_" + name + "_index = " + frequencyRatio + ";\n";
				}
			}
			
			if (segment == ArduinoUnoCodeSegmentProperties.ACQUISITION) {
				if(isUsed) {
					code = code + "\n\t\tif(acquire_" + name + "_index == " + frequencyRatio + ") {\n";
					code = code + "\t\t\t\tacquire_" + name + "_index = 0;\n";
					// Physical value = k * raw value + b (calibration from the raw values, see RawCalibration)
					String[] coefficients = ArduinoUnoAnInChannelProperties.getCoefficientsCode(channel);
					code = code + "\t\t\t\t" + name + " = acquireAnalogInput(" + channelNumber + ", " + coefficients[0] + ", " + coefficients[1] + ", " + transfer + ", " + transferNumber + ");\n";
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

	/*
	 * True if a channel of the module is used
	 */
	private boolean hasUsedChannel() {
		for (Channel channel : getChannels()) if(isUsed(channel)) return true;
		return false;
	}
	
	private static boolean isUsed(Channel channel) {
		return Boolean.valueOf(channel.getProperty(ArduinoUnoChannelProperties.USED));
	}
	
	/*
	 * Number of periods of the real time loop between two acquisitions of the channel
	 */
	private int getFrequencyRatio(Channel channel) {
		float gsfFloat = Float.parseFloat(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY));
		float sfFloat = Float.parseFloat(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
		return (int) (gsfFloat/sfFloat);
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
		// Analog reference of the new revision of the board (DEFAULT <-> AR_DEFAULT...)
		if(property == ArduinoUnoDACQConfigurationProperties.REVISION) {
			String reference = getProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE);
			boolean newValueIsR4 = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(newValue);
			newValueIsR4 = newValueIsR4 || ArduinoUnoDACQConfigurationProperties.REVISION_R4_MINIMA.equals(newValue);
			String newReference = ArduinoUnoAnInModuleProperties.getReferenceOfRevision(reference, newValueIsR4);
			if(!newReference.equals(reference)) setProperty(ArduinoUnoAnInModuleProperties.ANALOG_REFERENCE, newReference);
		}
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
		Module newModule = ArduinoUnoAnInModuleProperties.cloneModule(this);
		addClonedChannels(newModule);
		return newModule;
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
