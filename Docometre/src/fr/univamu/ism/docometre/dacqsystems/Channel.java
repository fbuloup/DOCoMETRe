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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represent a module channel 
 */
public abstract class Channel extends AbstractElement implements ChannelObservable, PropertyObserver, Comparable<Channel> {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	/**
	 * The module which contain this channel
	 */
	protected Module module;
	
	transient protected File file = null;
	
	/**
	 * File channel associated with input and output file
	 */
	transient protected FileChannel fileChannel = null;
	
	/**
	 * To be used when it is an acquired signal
	 */
	transient protected FileOutputStream outputFile = null;
	
	/**
	 * To be used when it is a generated signal : all samples will be read form the corresponding file
	 */
	transient protected FileInputStream inputFile = null;
	
	/**
	 * The list of channels observers.
	 */
	transient protected List<ChannelObserver> channelObserversList = new ArrayList<ChannelObserver>(0);

	public Channel(Module module) {
		this.module = module;
	}
	
	public Module getModule() {
		return module;
	}
	
	public void setModule(Module module) {
		this.module = module;
	}
	
	/**
	 * This method must be called to add acquired samples
	 * @param buffer corresponds to the signal which we must recovered
	 */
	public abstract void addSamples(float[] buffer);
	
	/**
	 * This method must be called to get samples for generation 
	 * @param nbData the number of signal samples to be generated
	 * @return the samples to be generated
	 */
	public abstract float[] getSamples(int nbData);

	/**
	 * This method must be called to correctly initialize a channel (e.g. data file name etc.)
	 * @param process the process for which the channel must be prepared
	 */
	public abstract void open(Process process, String prefix, String suffix);
	
	/**
	* This method must be called to correctly dispose a channel
	 */
	public abstract void close(Process process);
	
	/**
	 * This method adds an observer to the list of channels observers
	 */
	public void addChannelObserver(ChannelObserver channelObserver) {
		if(channelObserversList == null) channelObserversList = new ArrayList<>(0);
		channelObserversList.add(channelObserver);
	}
	
	/**
	 * This method removes an observer from list of channels observers
	 */
	public void removeChannelObserver(ChannelObserver channelObserver) {
		if(channelObserversList == null) return;
		channelObserversList.remove(channelObserver);
	}
	
	/**
	 * This method must return a unique identifier of the channel
	 * @return unique channel ID
	 */
	public abstract String getID();
	
	@Override
	public String toString() {
		return getProperty(ChannelProperties.NAME);
//		StringBuffer stringBuffer = new StringBuffer();
//		stringBuffer.append(super.toString());
//		return stringBuffer.toString();
	}
	
	@Override
	public int compareTo(Channel channel) {
		return getProperty(ChannelProperties.NAME).compareTo(channel.getProperty(ChannelProperties.NAME));
	}
	
}
