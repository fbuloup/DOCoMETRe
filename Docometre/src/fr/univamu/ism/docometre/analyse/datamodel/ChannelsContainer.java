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
package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public final class ChannelsContainer {
	
	private IFolder subject;
	
	static HashMap<String, Boolean> updateChannelsCache = new HashMap<>();
	static HashMap<String,  ArrayList<Channel>> channelsCache = new HashMap<>();
	
	static HashMap<String,  ArrayList<Channel>> signalsCache = new HashMap<>();
	static HashMap<String,  ArrayList<Channel>> categoriesCache = new HashMap<>();
	static HashMap<String,  ArrayList<Channel>> eventsCache = new HashMap<>();
	
	static HashMap<String,  ArrayList<Channel>> channelsMarkersCache = new HashMap<>();
	static HashMap<String,  ArrayList<Channel>> channelsFeaturesCache = new HashMap<>();
	static HashMap<String,  ArrayList<Channel>> channelsFrontEndCutsCache = new HashMap<>();
	
	public ChannelsContainer(IFolder subject) {
		this.subject = subject;
	}

	public Channel[] getChannels() {
		return MathEngineFactory.getMathEngine().getChannels(subject);
	}

	public Channel getChannelFromName(String fullChannelName) {
		Channel[] channels = getChannels();
		for (Channel channel : channels) {
			if(channel.getFullName().equals(fullChannelName)) return channel;
		}
		return null;
	}

	public void setSubject(IFolder subject) {
		this.subject = subject;
	}
	
	public boolean updateChannelsCache() {
		Boolean updateCache = updateChannelsCache.get(subject.getFullPath().toPortableString());
		updateCache = (updateCache == null) ? true : updateCache;
		return updateCache;
	}
	
	public void setUpdateChannelsCache(boolean update) {
		if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject)) updateChannelsCache.put(subject.getFullPath().toPortableString(), update);
		else removeChannelsCache();
	}
	
	private void removeChannelsCache() {
		channelsCache.remove(subject.getFullPath().toPortableString());
		channelsMarkersCache.remove(subject.getFullPath().toPortableString());
		channelsFeaturesCache.remove(subject.getFullPath().toPortableString());
		channelsFrontEndCutsCache.remove(subject.getFullPath().toPortableString());
		signalsCache.remove(subject.getFullPath().toPortableString());
		categoriesCache.remove(subject.getFullPath().toPortableString());
		eventsCache.remove(subject.getFullPath().toPortableString());
	}
	
	public ArrayList<Channel> manageChannelsCacheBefore() {
		ArrayList<Channel> channels = new ArrayList<>();
		if(!updateChannelsCache()) channels = channelsCache.get(subject.getFullPath().toPortableString());
		return channels;
	}
	
	private void checkChannelsCache() {
		if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject) && updateChannelsCache() == true) {
			MathEngineFactory.getMathEngine().getChannels(subject);
		}
	}
	
	public Channel[] getSignals() {
		checkChannelsCache();
		ArrayList<Channel> signals = signalsCache.get(subject.getFullPath().toPortableString());
		return signals.toArray(new Channel[signals.size()]);
	}

	
	public Channel[] getCategories() {
		checkChannelsCache();
		ArrayList<Channel> categories = categoriesCache.get(subject.getFullPath().toPortableString());
		return categories.toArray(new Channel[categories.size()]);
	}
	
	public Channel[] getEvents() {
		checkChannelsCache();
		ArrayList<Channel> events = eventsCache.get(subject.getFullPath().toPortableString());
		return events.toArray(new Channel[events.size()]);
	}
	
	public Channel[] getMarkers() {
		checkChannelsCache();
		ArrayList<Channel> markers = channelsMarkersCache.get(subject.getFullPath().toPortableString());
		return markers.toArray(new Channel[markers.size()]);
	}

	public Channel[] getFeatures() {
		checkChannelsCache();
		ArrayList<Channel> features = channelsFeaturesCache.get(subject.getFullPath().toPortableString());
		return features.toArray(new Channel[features.size()]);
	}
	
	public Channel[] getFrontEndCuts() {
		checkChannelsCache();
		ArrayList<Channel> frontEndCuts = channelsFrontEndCutsCache.get(subject.getFullPath().toPortableString());
		return frontEndCuts.toArray(new Channel[frontEndCuts.size()]);
	}
	
	public void manageChannelsCacheAfter(IResource subject, ArrayList<Channel> channels) {
		if(updateChannelsCache()) {
			channelsCache.put(subject.getFullPath().toPortableString(), channels);
			// Update markers, features and front/end cuts cache
			ArrayList<Channel> markers = channelsMarkersCache.get(subject.getFullPath().toPortableString());
			ArrayList<Channel> features = channelsFeaturesCache.get(subject.getFullPath().toPortableString());
			ArrayList<Channel> frontEndCuts = channelsFrontEndCutsCache.get(subject.getFullPath().toPortableString());
			ArrayList<Channel> signals = signalsCache.get(subject.getFullPath().toPortableString());
			ArrayList<Channel> categories = categoriesCache.get(subject.getFullPath().toPortableString());
			ArrayList<Channel> events = eventsCache.get(subject.getFullPath().toPortableString());
			if(markers == null) markers = new ArrayList<>();
			if(features == null) features = new ArrayList<>();
			if(frontEndCuts == null) frontEndCuts = new ArrayList<>();
			if(signals == null) signals = new ArrayList<>();
			if(categories == null) categories = new ArrayList<>();
			if(events == null) events = new ArrayList<>();
			markers.clear();
			features.clear();
			frontEndCuts.clear();
			signals.clear();
			categories.clear();
			events.clear();
			for (Channel channel : channels) {
				if(channel.isSignal()) {
					// Signals
					signals.add(channel);
					// Markers
					int nbMarkersGroups = MathEngineFactory.getMathEngine().getNbMarkersGroups(channel);
					for (int i = 0; i < nbMarkersGroups; i++) {
						String markersGroupLabel = MathEngineFactory.getMathEngine().getMarkersGroupLabel(i+1, channel);
						Channel newMarker = new Channel((IFolder)subject, channel, "MarkersGroup_" + markersGroupLabel, true, false);
						markers.add(newMarker);
					}
					// Features
					int nbFeatures = MathEngineFactory.getMathEngine().getNbFeatures(channel);
					for (int i = 0; i < nbFeatures; i++) {
						String featureLabel = MathEngineFactory.getMathEngine().getFeatureLabel(i+1, channel);
						Channel newFeature = new Channel((IFolder)subject, channel, "Feature_" + featureLabel, false, true);
						features.add(newFeature);
					}
					// Front/End cuts
					if(channel.isSignal()) {
						frontEndCuts.add(new Channel((IFolder)subject, channel.getName() + ".FrontCut"));
						frontEndCuts.add(new Channel((IFolder)subject, channel.getName() + ".EndCut"));
					}
				}
				if(channel.isCategory()) {
					categories.add(channel);
				}
				if(channel.isEvent()) {
					events.add(channel);
				}
			}
			channelsMarkersCache.put(subject.getFullPath().toPortableString(), markers);
			channelsFeaturesCache.put(subject.getFullPath().toPortableString(), features);
			channelsFrontEndCutsCache.put(subject.getFullPath().toPortableString(), frontEndCuts);
			signalsCache.put(subject.getFullPath().toPortableString(), signals);
			categoriesCache.put(subject.getFullPath().toPortableString(), categories);
			eventsCache.put(subject.getFullPath().toPortableString(), events);
			setUpdateChannelsCache(false);
		}
	}
	
}
