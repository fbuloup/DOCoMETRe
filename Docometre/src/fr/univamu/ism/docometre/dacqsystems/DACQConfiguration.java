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
package fr.univamu.ism.docometre.dacqsystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariableProperties;
import fr.univamu.ism.docometre.dacqsystems.charts.Charts;

public abstract class DACQConfiguration extends AbstractElement {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	protected static String[] defaultProposal = new String[]{"time"};
	
	/**
	 * Collection of charts for this DACQ configuration
	 */
	private Charts charts;
	
	/**
	 * This class is composed of a collection of modules. No duplicates are allowed.
	 */
	private Set<ModuleBehaviour> moduleBehaviourSet = new LinkedHashSet<ModuleBehaviour>(0);

	
	/**
	 * Return charts configuration
	 */
	public Charts getCharts() {
		if(charts == null) charts = new Charts();
		return charts;
	}
	
	public void setCharts(Charts charts) {
		this.charts = charts;
	}
	
	/**
	 * This method adds a module to the collection of modules
	 * @param module the module to add
	 */
	public void addModule(ModuleBehaviour module){
		moduleBehaviourSet.add(module);
		((Module)module).setDACQConfiguration(this);
		addObserver((PropertyObserver) module);
		notifyObservers(DACQConfigurationProperties.UPDATE_MODULE, module, null);
	}
	
	/**
	 * This method removes a module to the collection of modules
	 * @param module the module to remove
	 */
	public void removeModule(ModuleBehaviour module){
		moduleBehaviourSet.remove(module);
		removeObserver((PropertyObserver) module);
		notifyObservers(DACQConfigurationProperties.UPDATE_MODULE, null, module);
	}
	
	/**
	 * This method returns the module of index <i>index<i>
	 * @param index the index of the module in the collection of modules
	 * @return the module of index <i>index</i>
	 */
	public ModuleBehaviour getModule(int index) {
		return getModules()[index];
	}
	
	/**
	 * This method returns the number of modules
	 * @return the number of modules which forms the configuration
	 */
	public int getModulesNumber() {
		return moduleBehaviourSet.size();
	}
	
	/**
	 * This method returns all modules as an array of modules
	 * @return the modules array
	 */
	public Module[] getModules() {
		return moduleBehaviourSet.toArray(new Module[moduleBehaviourSet.size()]);
	}
	
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(super.toString());
		for (int i = 0; i < getModulesNumber(); i++) {
			ModuleBehaviour module = getModule(i);
			stringBuffer.append(module.toString());
		}
		return stringBuffer.toString();
	}

	public boolean containModule(Module module) {
		return moduleBehaviourSet.contains(module);
	}

	/**
	 * This method compute the number of each transfered channel
	 */
	public abstract void updateChannelsTransferNumber();
	
	/**
	 * This method return all channels names that can be used in processes 
	 * @return channels names
	 */
	public String[] getProposal() {
		HashSet<String> proposalHashSet = new HashSet<>();
		proposalHashSet.addAll(Arrays.asList(defaultProposal));
		int nbModules = getModulesNumber();
		for (int i = 0; i < nbModules; i++) {
			Module module = (Module) getModule(i);
			List<Channel> channels = Arrays.asList(module.getChannels());
			for (Channel channel : channels) {
				proposalHashSet.add(channel.getProperty(ChannelProperties.NAME));
			}
		}
		
		Channel[] variables = getVariables();
		for (Channel variable : variables) {
			proposalHashSet.add(variable.getProperty(ChannelProperties.NAME));
		}
		
		String[] proposals = proposalHashSet.toArray(new String[proposalHashSet.size()]);
		Arrays.sort(proposals);
		return proposals;
	}
	
	public String[] getStimuliProposal() {
		HashSet<String> proposalHashSet = new HashSet<>();
		int nbModules = getModulesNumber();
		for (int i = 0; i < nbModules; i++) {
			Module module = (Module) getModule(i);
			boolean add = (module instanceof ADWinAnOutModule) || (module instanceof ADWinDigInOutModule);
			if(add) {
				List<Channel> channels = Arrays.asList(module.getChannels());
				for (Channel channel : channels) {
					add = true;
					if(module instanceof ADWinDigInOutModule) {
						String inputOrOutput = channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
						add = inputOrOutput.equals(ADWinDigInOutChannelProperties.OUTPUT);
						add = add && "true".equals(channel.getProperty(ADWinDigInOutChannelProperties.STIMULUS));
					} else add = add && "true".equals(channel.getProperty(ADWinAnOutChannelProperties.STIMULUS));
					if(add) proposalHashSet.add(channel.getProperty(ChannelProperties.NAME));
				}
			}
		}
		Channel[] variables = getVariables();
		for (Channel variable : variables) {
			boolean add = "true".equals(variable.getProperty(ADWinVariableProperties.STIMULUS));
			if(add) if(add) proposalHashSet.add(variable.getProperty(ChannelProperties.NAME));
		}
		
		String[] proposals = proposalHashSet.toArray(new String[proposalHashSet.size()]);
		Arrays.sort(proposals);
		return proposals;
	}
	
//	public String[] getOutputsProposal() {
//		HashSet<String> proposalHashSet = new HashSet<>();
//		int nbModules = getModulesNumber();
//		for (int i = 0; i < nbModules; i++) {
//			Module module = (Module) getModule(i);
//			boolean add = (module instanceof ADWinAnOutModule) || (module instanceof ADWinDigInOutModule);
//			if(add) {
//				List<Channel> channels = Arrays.asList(module.getChannels());
//				for (Channel channel : channels) {
//					add = true;
//					if(module instanceof ADWinDigInOutModule) {
//						String inputOrOutput = channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
//						add = inputOrOutput.equals(ADWinDigInOutChannelProperties.OUTPUT);
//					}
//					if(add) proposalHashSet.add(channel.getProperty(ChannelProperties.NAME));
//				}
//			}
//		}
//		String[] proposals = proposalHashSet.toArray(new String[proposalHashSet.size()]);
//		Arrays.sort(proposals);
//		return proposals;
//	}

	public abstract Channel[] getChannels();
	
	public abstract Channel[] getVariables();
	
	public Channel[] getTransferedChannels() {
		ArrayList<Channel> channelsArrayList = new ArrayList<>(0);
		Channel[] channels = getChannels();
		for (Channel channel : channels) if(channel.getProperty(ChannelProperties.TRANSFER).equals("true")) channelsArrayList.add(channel);
		return channelsArrayList.toArray(new Channel[channelsArrayList.size()]);
	}
	
	public String[] getAvailableFrequencies(String frequencyBase) {
		ArrayList<String> frequencies = new ArrayList<>();
		double frequencyBaseDouble = Double.valueOf(frequencyBase);
		double frequency = frequencyBaseDouble;
		int n = 1;
		boolean compute = true;
		while (compute) {
			frequency = frequencyBaseDouble / (1.f*n);
			if(100.0*frequency - (int)(100.0*frequency) == 0) frequencies.add(String.valueOf(frequency));
			n++;
			compute = frequency > .25;
			if(!compute) compute = n < 100;
		}
		 return frequencies.toArray(new String[frequencies.size()]);
	}
	
}
