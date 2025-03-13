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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;

public class ArduinoUnoDACQConfiguration extends DACQConfiguration implements PropertyObserver {
	
	protected static String[] defaultProposal = new String[]{"time", "rtTime"};
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;

	protected HashSet<ArduinoUnoVariable> variables = new HashSet<ArduinoUnoVariable>();
	
	public ArduinoUnoDACQConfiguration() {
		ArduinoUnoDACQConfigurationProperties.populateProperties(this);
		// For Arduino uno system, modules are statically added
		// Six Analog inputs
		ArduinoUnoAnInModule arduinoUnoAnInModule = new ArduinoUnoAnInModule(this);
		for (int i = 0; i < 6; i++) {
			Channel arduinoUnoChannel = arduinoUnoAnInModule.createChannel();
			arduinoUnoChannel.setProperty(ChannelProperties.NAME, "AIN" + i);
			arduinoUnoChannel.setProperty(ChannelProperties.CHANNEL_NUMBER, String.valueOf(i));
//			arduinoUnoChannel.setProperty(ChannelProperties.TRANSFER_NUMBER, String.valueOf(i));
		}
		// Six potential PWM analog outputs (when not used as digital input or output)
		ArduinoUnoAnOutModule arduinoUnoAnOutModule = new ArduinoUnoAnOutModule(this);
		//int n = 6; // Because six analog input
		for (int i = 0; i < 6; i++) {
			int n = 0;
			Channel arduinoUnoChannel = arduinoUnoAnOutModule.createChannel();
			if(i == 0) n = 3;
			if(i == 1) n = 5;
			if(i == 2) n = 6;
			if(i == 3) n = 9;
			if(i == 4) n = 10;
			if(i == 5) n = 11;
			arduinoUnoChannel.setProperty(ChannelProperties.NAME, "AOUT" + n);
			arduinoUnoChannel.setProperty(ChannelProperties.CHANNEL_NUMBER, String.valueOf(n));
//			arduinoUnoChannel.setProperty(ChannelProperties.TRANSFER_NUMBER, String.valueOf(n + 6));
		}
		// Digital inputs/outputs
		ArduinoUnoDigInOutModule arduinoUnoDigInOutModule = new ArduinoUnoDigInOutModule(this);
		for (int i = 0; i < 14; i++) {
			Channel arduinoUnoChannel = arduinoUnoDigInOutModule.createChannel();
			arduinoUnoChannel.setProperty(ChannelProperties.NAME, "DIGITAL" + i);
			arduinoUnoChannel.setProperty(ChannelProperties.CHANNEL_NUMBER, String.valueOf(i));
//			arduinoUnoChannel.setProperty(ChannelProperties.TRANSFER_NUMBER, String.valueOf(i + 6));
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		ArduinoUnoDACQConfiguration clonedConfiguration  = ArduinoUnoDACQConfigurationProperties.cloneConfiguration(this);
		ArduinoUnoVariable[] variablesArray = getVariables();
		for (int i = 0; i < variablesArray.length; i++) {
			ArduinoUnoVariable variable = (ArduinoUnoVariable) variablesArray[i].clone();
			clonedConfiguration.addVariable(variable);
		}
		for (int i = 0; i < getModulesNumber(); i++) {
			Module module = (Module)getModule(i);
			ModuleBehaviour newModule = (ModuleBehaviour)module.clone();
			clonedConfiguration.addModule(newModule);
		}
		return clonedConfiguration;
	}
	
	public ArduinoUnoVariable createVariable() {
		ArduinoUnoVariable variable = new ArduinoUnoVariable(this);
		addVariable(variable);
		return variable;
	}
	
	public boolean addVariable(ArduinoUnoVariable variable) {
		boolean succeed = variables.add(variable);
		notifyObservers(ChannelProperties.ADD, variable, null);
		return succeed; 
	}
	
	@Override
	public ArduinoUnoVariable[] getVariables() {
		ArduinoUnoVariable[] variablesArray = variables.toArray(new ArduinoUnoVariable[variables.size()]);
		Arrays.sort(variablesArray);
		return variablesArray;
	}
	
	public boolean removeVariable(ArduinoUnoVariable variable) {
		boolean succeed = variables.remove(variable);
		notifyObservers(ChannelProperties.REMOVE, null, variable);
		return succeed; 
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY){
			ArduinoUnoDACQConfiguration.changeChannelsFrequencies((String)newValue, (String)oldValue, getVariables());
			notifyObservers(ChannelProperties.SAMPLE_FREQUENCY, newValue, oldValue);
		}
	}

	@Override
	public void updateChannelsTransferNumber() {
		int nbTransferedChannels = 1;
		
		Channel[] variables = getVariables();
		for (Channel variable : variables) {
			if(variable.getProperty(ChannelProperties.TRANSFER).equals("true")) 
				variable.setProperty(ChannelProperties.TRANSFER_NUMBER, Integer.toString(nbTransferedChannels++));
			else {
				variable.setProperty(ChannelProperties.TRANSFER_NUMBER, "0");
				variable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");
				variable.setProperty(ChannelProperties.RECORD, "false");
			}
		}
		
		for (int j = 0; j < getModulesNumber(); j++) {
			if(getModule(j) instanceof Module){
				Module module = (Module) getModule(j);
				for (int i = 0; i < module.getChannelsNumber() ; i++) {
					Channel channel = module.getChannel(i);
					
					if(channel.getProperty(ArduinoUnoChannelProperties.USED).equals("false")) {
						channel.setProperty(ChannelProperties.TRANSFER, "false");
					}
					
					if(channel.getProperty(ChannelProperties.TRANSFER).equals("true")) 
						channel.setProperty(ChannelProperties.TRANSFER_NUMBER, Integer.toString(nbTransferedChannels++));
					else {
						channel.setProperty(ChannelProperties.TRANSFER_NUMBER, "0");
						channel.setProperty(ChannelProperties.AUTO_TRANSFER, "false");
						channel.setProperty(ChannelProperties.RECORD, "false");
					}
				}
			}
		}
	}
	
	public static void changeChannelsFrequencies(String newValue, String oldValue, Channel[] channels) {
		Double oldGolbalFrequency = Double.parseDouble(oldValue);
		Double newGlobalFrequency = Double.parseDouble(newValue);
		Double ratio = newGlobalFrequency / oldGolbalFrequency;
		//Change channels frequencies
		for (Channel channel : channels) {
			Double frequency = Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
			frequency = ratio * frequency;
			channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, Double.toString(frequency));
		}
	}

	@Override
	public Channel[] getChannels() {
		ArrayList<Channel> channelsArrayList = new ArrayList<>(0);
		channelsArrayList.addAll(Arrays.asList(getVariables()));
		Module[] modules = getModules();
		for (Module module : modules) {
			//boolean getChannels = !(module instanceof ADWinRS232Module && module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE));
			/*if(getChannels)*/ channelsArrayList.addAll(Arrays.asList(module.getChannels()));
		}
		return channelsArrayList.toArray(new Channel[channelsArrayList.size()]);
	}

	@Override
	public void initializeObservers() {
		addObserver(this);
	}
	
	@Override
	public String[] getProposal() {
		HashSet<String> proposalHashSet = new HashSet<>();
		proposalHashSet.addAll(Arrays.asList(defaultProposal));
		String arduinoUnoRelease = getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		boolean isRelease3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(arduinoUnoRelease);
		if(isRelease4Wifi) proposalHashSet.remove("time");
		if(isRelease3) proposalHashSet.remove("rtTime");
		ArduinoUnoVariable[] variables = getVariables();
		for (ArduinoUnoVariable arduinoUnoVariable : variables) {
			proposalHashSet.add(arduinoUnoVariable.getProperty(ChannelProperties.NAME));
		}
		int nbModules = getModulesNumber();
		for (int i = 0; i < nbModules; i++) {
			Module module = (Module) getModule(i);
			List<Channel> channels = Arrays.asList(module.getChannels());
			for (Channel channel : channels) {
				if("true".equals(channel.getProperty(ArduinoUnoChannelProperties.USED))) proposalHashSet.add(channel.getProperty(ChannelProperties.NAME));
			}
		}
		String[] proposals = proposalHashSet.toArray(new String[proposalHashSet.size()]);
		Arrays.sort(proposals);
		return proposals;
	}
	
	public boolean hasADS1115Module() {
		Module[] modules = getModules();
		for (Module module : modules) {
			if(module instanceof ArduinoUnoADS1115Module) return true;
		}
		return false;
	}

}
