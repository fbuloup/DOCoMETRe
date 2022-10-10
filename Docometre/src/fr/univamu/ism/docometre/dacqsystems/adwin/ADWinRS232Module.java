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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ADWinRS232Module extends Module {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	private List<Channel> backedChannels = new ArrayList<Channel>(0);
	private boolean includeSegmentPassed;
	
	public ADWinRS232Module(DACQConfiguration dacqConfiguration) {
		super(dacqConfiguration);
		ADWinRS232ModuleProperties.populateProperties(this);
	}

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		String sampleFrequencyString = getProperty(ADWinRS232ModuleProperties.FREQUENCY);
		String gfString = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
		double gf = Double.parseDouble(gfString);
		double sf = Double.parseDouble(sampleFrequencyString);
		int sampleRate = (int) (gf/sf);
		String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
		String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
		String interfaceNumber = getProperty(ADWinRS232ModuleProperties.INTERFACE_NUMBER);
		
		if (segment == ADWinCodeSegmentProperties.INCLUDE && !includeSegmentPassed){
			includeSegmentPassed = true;
			boolean addInclude = true;
			Module[] modules = dacqConfiguration.getModules();
			for (Module module : modules) {
				if(module instanceof ADWinCANModule) addInclude = false;
			}
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && addInclude) code = code + "#INCLUDE ADwpEXT.INC\n";
			
			if(getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
				String temp = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
				if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) temp = temp + "CONVIEEE754SENDICE.INC\n";
				if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) temp = temp + "CONVIEEE754SENDICEGold.INC\n";
				code = code + "#INCLUDE " + temp + "\n";
			}
		}	
		
		if (segment == ADWinCodeSegmentProperties.DECLARATION){
			if(getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
				code = code + "\nREM ******* Variable pour l'envoi des données vers ICE\n";
				code = code + "DIM SEND_ICE_" + hashCode() + " AS INTEGER\n";
			}
		}
		
		if (segment == ADWinCodeSegmentProperties.INITIALIZATION){
			String baudRate = getProperty(ADWinRS232ModuleProperties.BAUD_RATE);
			String flowControl = getProperty(ADWinRS232ModuleProperties.FLOW_CONTROL);
			String nbDataBits = getProperty(ADWinRS232ModuleProperties.NB_DATA_BITS);
			String nbStopBits = getProperty(ADWinRS232ModuleProperties.NB_STOP_BITS);
			String parity = getProperty(ADWinRS232ModuleProperties.PARITY);
			
			if(parity.equals(ADWinRS232ModuleProperties.NO_PARITY)) parity = "0";
			if(parity.equals(ADWinRS232ModuleProperties.PARITY_ODD)) parity = "2";
			if(parity.equals(ADWinRS232ModuleProperties.PARITY_EVEN)) parity = "1";
			
			if(flowControl.equals(ADWinRS232ModuleProperties.NO_HANDSHAKE)) flowControl = "0";
			if(flowControl.equals(ADWinRS232ModuleProperties.SOFTWARE_HANDSHAKE)) flowControl = "2";
			if(flowControl.equals(ADWinRS232ModuleProperties.HARDWARE_HANDSHAKE)) flowControl = "1";
			
			if(nbStopBits.equals(ADWinRS232ModuleProperties.STOPBIT_1)) nbStopBits = "0";
			if(nbStopBits.equals(ADWinRS232ModuleProperties.STOPBIT_15)) nbStopBits = "1";
			if(nbStopBits.equals(ADWinRS232ModuleProperties.STOPBIT_2)) nbStopBits = "1";
			
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) {
				code = code + "RS_RESET(" + moduleNumber + ") ' Reset is allowed only once on a module !\n";
				code = code + "RS_INIT(" + moduleNumber + ", " + interfaceNumber + ", " + baudRate + ", " + parity + ", " + nbDataBits + ", " + nbStopBits + ", " + flowControl  + ")";
			}
			
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) {
				code = code + "RS_RESET() ' Reset is allowed only once on a module !\n";
				code = code + "RS_INIT(" + interfaceNumber + ", " + baudRate + ", " + parity + ", " + nbDataBits + ", " + nbStopBits + ", " + flowControl  + ")";
			}
			
			if(getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
				code = code + "\nREM ******* Initialisation variable pour l'envoi des données vers ICE\n";
				code = code + "SEND_ICE_" + hashCode() + " =  " + (sampleRate) + "\n";
			}
			
		}	
		if (segment == ADWinCodeSegmentProperties.GENERATION){
			if(getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
				code = code + "\nREM Envoi des données vers ICE";
				code = code + "\n\nIF (SEND_ICE_" + hashCode() + " =  " + (sampleRate) + ") THEN\n";
				code = code + "\tSEND_ICE_" + hashCode() + " =  0\n";
				Channel[] channels = getChannels();
				for (Channel channel : channels) {
					String trfrNumber = channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
					String channelName = channel.getProperty(ChannelProperties.NAME);
					code = code + "\n\tREM Envoi de " + channel.getProperty(ChannelProperties.NAME);
					if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) {
						code = code + "\n\tCall_ConvIEEE754SendICE(" + moduleNumber + ", " + interfaceNumber +  ", " + trfrNumber + ", " + channelName + ")";
					}
					if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) {
						code = code + "\n\tCall_ConvIEEE754SendICE(" + interfaceNumber +  ", " + trfrNumber + ", " + channelName + ")";
					}
					
				}
				code = code + "\n\nENDIF\n";
				code = code + "INC(SEND_ICE_" + hashCode() + ")\n";
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
		
	}

	@Override
	public void reset() {
		includeSegmentPassed = false;
	}
	
	private Channel getChannelFromName(String nameToFind) {
		Channel[] channels = getChannels();
		for (Channel channel : channels) {
			String name = channel.getProperty(ChannelProperties.NAME);
			if(name.equals(nameToFind)) return channel;
		}
		return null;
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == ChannelProperties.NAME &&  backedChannels.contains(element)) {
			Channel channel = getChannelFromName((String)oldValue);
			channel.setProperty(ChannelProperties.NAME, element.getProperty(ChannelProperties.NAME));
			notifyObservers(ChannelProperties.NAME, newValue, oldValue);
		}
		if(property == ChannelProperties.REMOVE &&  backedChannels.contains(oldValue)) {
			backedChannels.remove(oldValue);
			Channel channel = getChannelFromName(((AbstractElement)oldValue).getProperty(ChannelProperties.NAME));
			removeChannel(channel);
			notifyObservers(ChannelProperties.REMOVE, newValue, oldValue);
		}
	}

	@Override
	public boolean addChannel(Channel channel) {
		boolean succeed = super.addChannel(channel);
		notifyObservers(ChannelProperties.ADD, channel, null);
		return succeed;
	}
	
	public Channel createChannel(Channel fromChannel, int rs232TransfertNumber) {
		Channel channel = super.createChannel();
		backedChannels.add(fromChannel);
		initializeObservers();
		channel.setProperty(ChannelProperties.TRANSFER_NUMBER, String.valueOf(rs232TransfertNumber));
		channel.setProperty(ChannelProperties.NAME, fromChannel.getProperty(ChannelProperties.NAME));
		return channel;
	}
	
	@Override
	public Channel initializeChannelProperties() {
		Channel channel = new ADWinChannel(this);
		ChannelProperties.populateProperties(channel);
		if(dacqConfiguration != null) {
			String property = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, property);
		}
		return channel;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Module newModule = ADWinRS232ModuleProperties.cloneModule(this);
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			Channel newChannel = (Channel) channel.clone();
			newModule.addChannel(newChannel);
		}
		return newModule;
	}

	@Override
	public void initializeObservers() {
		Channel[] channels = backedChannels.toArray(new Channel[backedChannels.size()]);
		for (Channel channel : channels) {
			channel.addObserver(this);
		}
		dacqConfiguration.addObserver(this);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initializeObservers();
	}

}
