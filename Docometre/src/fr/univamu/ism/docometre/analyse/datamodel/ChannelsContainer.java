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
	
	public boolean updateChannelsCache(IResource subject) {
		Boolean updateCache = updateChannelsCache.get(subject.getFullPath().toPortableString());
		updateCache = (updateCache == null) ? true : updateCache;
		return updateCache;
	}
	
	public void setUpdateChannelsCache(IResource subject, boolean update) {
		if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject)) updateChannelsCache.put(subject.getFullPath().toPortableString(), update);
		else removeChannelsCache(subject);
	}
	
	private void removeChannelsCache(IResource subject) {
		channelsCache.remove(subject.getFullPath().toPortableString());
		channelsMarkersCache.remove(subject.getFullPath().toPortableString());
		channelsFeaturesCache.remove(subject.getFullPath().toPortableString());
		channelsFrontEndCutsCache.remove(subject.getFullPath().toPortableString());
		signalsCache.remove(subject.getFullPath().toPortableString());
		categoriesCache.remove(subject.getFullPath().toPortableString());
		eventsCache.remove(subject.getFullPath().toPortableString());
	}
	
	public ArrayList<Channel> manageChannelsCacheBefore(IResource subject) {
		ArrayList<Channel> channels = new ArrayList<>();
		if(!updateChannelsCache(subject)) channels = channelsCache.get(subject.getFullPath().toPortableString());
		return channels;
	}
	
	private void checkChannelsCache(IResource subject) {
		if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject) && updateChannelsCache(subject) == true) {
			MathEngineFactory.getMathEngine().getChannels(subject);
		}
	}
	
	public Channel[] getSignals(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> signals = signalsCache.get(subject.getFullPath().toPortableString());
		return signals.toArray(new Channel[signals.size()]);
	}

	
	public Channel[] getCategories(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> categories = categoriesCache.get(subject.getFullPath().toPortableString());
		return categories.toArray(new Channel[categories.size()]);
	}
	
	public Channel[] getEvents(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> events = eventsCache.get(subject.getFullPath().toPortableString());
		return events.toArray(new Channel[events.size()]);
	}
	
	public Channel[] getMarkers(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> markers = channelsMarkersCache.get(subject.getFullPath().toPortableString());
		return markers.toArray(new Channel[markers.size()]);
	}

	public Channel[] getFeatures(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> features = channelsFeaturesCache.get(subject.getFullPath().toPortableString());
		return features.toArray(new Channel[features.size()]);
	}
	
	public Channel[] getFrontEndCuts(IResource subject) {
		checkChannelsCache(subject);
		ArrayList<Channel> frontEndCuts = channelsFrontEndCutsCache.get(subject.getFullPath().toPortableString());
		return frontEndCuts.toArray(new Channel[frontEndCuts.size()]);
	}
	
	public void manageChannelsCacheAfter(IResource subject, ArrayList<Channel> channels) {
		if(updateChannelsCache(subject)) {
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
						String markersGroupLabel = MathEngineFactory.getMathEngine().getMarkersGroupLabel(i, channel);
						markers.add(new Channel((IFolder)subject, "MarkersGroup_" + markersGroupLabel));
					}
					// Features
					int nbFeatures = MathEngineFactory.getMathEngine().getNbFeatures(channel);
					for (int i = 0; i < nbFeatures; i++) {
						String featureLabel = MathEngineFactory.getMathEngine().getFeatureLabel(i, channel);
						features.add(new Channel((IFolder)subject, "Feature_" + featureLabel));
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
			setUpdateChannelsCache(subject, false);
		}
	}
	
}
