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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A module is attached to a DACQ configuration and contains channels. When a module is running,
 * it means that it is used in the context of a specific process : this process can be retrieved 
 * at any time during running. When the module is not running, this process is null.
 */
public abstract class Module extends AbstractElement implements ModuleBehaviour, Serializable, PropertyObserver {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	/**
	 * A <b>Module</b> can holds one or more <i>Channels</i>.
	 */
	private List<Channel> channels = new ArrayList<Channel>(0);
	
	/**
	 * The DAQ configuration that contains this module
	 */
	protected DACQConfiguration dacqConfiguration;

	/**
	 * The attached process for which the module is currently running
	 */
	protected Process process;
	
	public Module(DACQConfiguration dacqConfiguration) {
		this.dacqConfiguration = dacqConfiguration;
		if(dacqConfiguration != null) dacqConfiguration.addModule(this);
	}
	
	/**
	 * This method returns the general DAQ configuration that holds this module
	 * @return
	 */
	public DACQConfiguration getDACQConfiguration() {
		return dacqConfiguration;
	}
	
	/**
	 * Set the general configuration in which the module is
	 * @param daqGeneralConfiguration the daq general configuration
	 */
	public void setDACQConfiguration(DACQConfiguration dacqConfiguration) {
		this.dacqConfiguration = dacqConfiguration;
	}

	/**
	 * This method adds a channel to the list of channels module.
	 * This method is declared private to avoid adding uninitialized channels. 
	 * Use instead method {@link #createChannel()} to add a new channel to the module.
	 * @param channel
	 */
	public boolean addChannel(Channel channel){
		boolean succeed = channels.add(channel);
		channel.setModule(this);
		return succeed;
	}
	
	/**
	 * This method removes a channel from the module
	 * @param channel the channel to be removed
	 */
	public boolean removeChannel(Channel channel){
		boolean succeed = channels.remove(channel);
		channel.setModule(null);
		return succeed;
	}
	/**
	 * This method returns the channel with specified index 
	 * @param index the index in the set of channels
	 * @return the channel of index <i>index</i>
	 */
	public Channel getChannel(int index) {
		return channels.get(index);
	}
	
	/**
	 * @return the number of channels which forms the module
	 */
	public int getChannelsNumber() {
		return channels.size();
	}
	
	/**
	 * @return the channels as an array of channels
	 */
	public Channel[] getChannels() {
		return channels.toArray(new Channel[channels.size()]);
	}
	
	/**
	 * Always use this method when you want to add a new channel in a concrete module as
	 * it calls abstract method {@link Module#initializeChannelProperties()}
	 * @return channel A correctly initialized channel relatively to its concrete module.
	 * @see Module#initializeChannelProperties()
	 */
	public Channel createChannel() {
		Channel channel = initializeChannelProperties();
		addChannel(channel);
		return channel;
	}

	/**
	 * Clients must implement this method in order to correctly initialized channel's properties.
	 * In order to add a new channel to the module, clients must call {@link Module#createChannel()}, this
	 * method calls {@link Module#initializeChannelProperties()}
	 */
	public abstract Channel initializeChannelProperties();

	/**
	 * This method opens each module's channels
	 */
	public void open(Process process, String prefix, String suffix) {
		this.process = process;
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			channel.open(process, prefix, suffix);
		}
	}
	
	/**
	 * This method closes each module's channels
	 */
	public void close(){
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			channel.close(process);
		}
	}
	
	/**
	 * Return the current process
	 * @return the process for which the module is currently running
	 */
	public Process getCurrentProcess() {
		return process;
	}
	
	@Override
	public String toString() {
		
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(getClass().getSimpleName()); //$NON-NLS-1$
//		stringBuffer.append(super.toString());
//		for (int i = 0; i < getChannelsNumber(); i++) {
//			Channel channel = getChannel(i);
//			stringBuffer.append(channel.toString());
//		}
		return stringBuffer.toString();
	}
	
}
