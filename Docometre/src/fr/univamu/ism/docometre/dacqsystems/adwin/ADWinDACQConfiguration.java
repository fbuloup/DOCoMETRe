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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;

public class ADWinDACQConfiguration extends DACQConfiguration implements PropertyObserver {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	transient protected ADwinDevice adwinDevice;
	protected HashSet<ADWinVariable> variables = new HashSet<ADWinVariable>();
	
	public ADWinDACQConfiguration() {
		ADWinDACQConfigurationProperties.populateProperties(this);
	}
	
	public ADwinDevice getADwinDevice() {
		return adwinDevice;
	}
	
	public void setADwinDevice(ADwinDevice adwinDevice) {
		this.adwinDevice = adwinDevice;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		ADWinDACQConfiguration clonedConfiguration  = ADWinDACQConfigurationProperties.cloneConfiguration(this);
		ADWinVariable[] variablesArray = getVariables();
		for (int i = 0; i < variablesArray.length; i++) {
			ADWinVariable variable = (ADWinVariable) variablesArray[i].clone();
			clonedConfiguration.addVariable(variable);
		}
		for (int i = 0; i < getModulesNumber(); i++) {
			Module module = (Module)getModule(i);
			ModuleBehaviour newModule = (ModuleBehaviour)module.clone();
			clonedConfiguration.addModule(newModule);
		}
		return clonedConfiguration;
	}
	
	public ADWinVariable createVariable() {
		ADWinVariable variable = new ADWinVariable(this);
		addVariable(variable);
		return variable;
	}
	
	public boolean addVariable(ADWinVariable variable) {
		boolean succeed = variables.add(variable);
		notifyObservers(ChannelProperties.ADD, variable, null);
		return succeed; 
	}
	
	@Override
	public ADWinVariable[] getVariables() {
		ADWinVariable[] variablesArray = variables.toArray(new ADWinVariable[variables.size()]);
		Arrays.sort(variablesArray);
		return variablesArray;
	}
	
	public boolean removeVariable(ADWinVariable variable) {
		boolean succeed = variables.remove(variable);
		notifyObservers(ChannelProperties.REMOVE, null, variable);
		return succeed; 
	}
	
	public void update(Property property, Object newValue,  Object oldValue, AbstractElement element) {
		if(property == ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY){
			ADWinDACQConfiguration.changeChannelsFrequencies((String)newValue, (String)oldValue, getVariables());
			notifyObservers(ChannelProperties.SAMPLE_FREQUENCY, newValue, oldValue);
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
	public void initializeObservers() {
		addObserver(this);
	}
	
	public Channel[] getChannels() {
		ArrayList<Channel> channelsArrayList = new ArrayList<>(0);
		channelsArrayList.addAll(Arrays.asList(getVariables()));
		Module[] modules = getModules();
		for (Module module : modules) {
			boolean getChannels = !(module instanceof ADWinRS232Module && module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE));
			if(getChannels) channelsArrayList.addAll(Arrays.asList(module.getChannels()));
		}
		return channelsArrayList.toArray(new Channel[channelsArrayList.size()]);
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
					
					if(channel.getProperty(ChannelProperties.TRANSFER).equals("true")) 
						channel.setProperty(ChannelProperties.TRANSFER_NUMBER, Integer.toString(nbTransferedChannels++));
					else {
						String property = module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE);
						boolean isICESystemType = (property != null) && property.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE);
						if(!isICESystemType) {
							channel.setProperty(ChannelProperties.TRANSFER_NUMBER, "0");
							channel.setProperty(ChannelProperties.AUTO_TRANSFER, "false");
							channel.setProperty(ChannelProperties.RECORD, "false");
						}
					}
				}
			}
		}
	}
	
	public String[] getProposalImpl(boolean removeStrings, boolean removeFloats, boolean removeIntegers) {
		HashSet<String> proposalHashSet = new HashSet<>();
		ADWinVariable[] variables = getVariables();
		for (ADWinVariable variable : variables) {
			String type = variable.getProperty(ADWinVariableProperties.TYPE);
			if(!removeStrings && ADWinVariableProperties.STRING.equalsIgnoreCase(type)) proposalHashSet.add(variable.getProperty(ChannelProperties.NAME));
			if(!removeFloats && ADWinVariableProperties.FLOAT.equalsIgnoreCase(type)) proposalHashSet.add(variable.getProperty(ChannelProperties.NAME));
			if(!removeIntegers && ADWinVariableProperties.INT.equalsIgnoreCase(type)) proposalHashSet.add(variable.getProperty(ChannelProperties.NAME));
		}
		String[] proposal = super.getProposal();
		proposalHashSet.addAll(Arrays.asList(proposal));
		String[] proposals = proposalHashSet.toArray(new String[proposalHashSet.size()]);
		Arrays.sort(proposals);
		return proposals;
	}
	
	@Override
	public String[] getProposal() {
		return getProposalImpl(false, false, false);
	}
	
	public ADWinVariable[] getParameters() {
		ArrayList<ADWinVariable> parameters = new ArrayList<>();
		ADWinVariable[] variables = getVariables();
		for (ADWinVariable variable : variables) {
			if(variable.isParameter()) parameters.add(variable);
		}
		return parameters.toArray(new ADWinVariable[parameters.size()]);
	}

	
	public ADWinVariable[] getPropagatedParameters() {
		ADWinVariable[] parameters = getParameters();
		ArrayList<ADWinVariable> propagatedParameters = new ArrayList<>();
		for (ADWinVariable parameter : parameters) {
			if(parameter.isParameterPropagated()) propagatedParameters.add(parameter);
		}
		return propagatedParameters.toArray(new ADWinVariable[propagatedParameters.size()]);
	}
}
