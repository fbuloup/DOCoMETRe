package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public interface MathEngine {
	IStatus startEngine(IProgressMonitor monitor);
	IStatus stopEngine(IProgressMonitor monitor);
	boolean isStarted();
	void addListener(MathEngineListener listener);
	boolean isSubjectLoaded(IResource subject);
	void load(IResource subject);
	void unload(IResource subject);
	boolean exist(String variableName);
	boolean isStruct(String variableName);
	boolean isField(String variableName, String fieldName);
	public Channel[] getChannels(IResource subject);
	public boolean isSignalCategoryOrEvent(String fullName, String check);
	public String getCriteriaForCategory(IResource category);
	Integer[] getTrialsListForCategory(IResource category);
	double[] getYValuesForSignal(Channel signal, int trialNumber);
	int getTrialsNumber(Channel signal);
	double getSampleFrequency(Channel signal);
	int getSamplesNumber(Channel signal, int trialNumber);
	int getFrontCut(Channel signal, int trialNumber);
	int getEndCut(Channel signal, int trialNumber);
	void runScript(String code);
	void deleteChannel(Channel resource);
	
	int getNbMarkersGroups(Channel signal);
	void createNewMarkersGroup(Channel signal, String markersGroupLabel);
	String getMarkersGroupLabel(int markersGroupNumber, Channel signal);
	void deleteMarkersGroup(int markersGroupNumber, Channel signal);
	void addMarker(String markerLabel, int trialNumber, double xValue, double yValue, String fullSignalName);
	
	default String[] getMarkersGroupsLabels(Channel signal) {
		int nbMarkersGroups = getNbMarkersGroups(signal);
		String[] markersGroupsLabels = new String[nbMarkersGroups];
		for (int i = 0; i < markersGroupsLabels.length; i++) {
			markersGroupsLabels[i] = getMarkersGroupLabel(i + 1, signal);
		}
		return markersGroupsLabels;
	}
	
	default double[] getTimeValuesForSignal(Channel signal, Integer trialNumber) {
		try {
			double sf = getSampleFrequency(signal);
			int nbSamples = getSamplesNumber(signal, trialNumber);
			double[] times = new double[nbSamples];
			for (int i = 0; i < nbSamples; i++) {
				times[i] = 1f*i/sf;
			}
			return times;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default Channel getChannelWithName(IResource subject, String channelName) {
		Channel[] channels = getChannels(subject);
		for (Channel channel : channels) {
			if(channel.getName().equals(channelName)) return channel;
		}
		return null;
	}
	
	default String getFullPath(IResource resource) {
		return resource.getFullPath().toString().replaceAll("^/", "").replaceAll("/", ".");
	}
	
	default Channel[] getSignals(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> signals = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isSignal(channel)) signals.add(channel);
		}
		return signals.toArray(new Channel[signals.size()]);
	}

	default Channel[] getCategories(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> categories = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isCategory(channel)) categories.add(channel);
		}
		return categories.toArray(new Channel[categories.size()]);
	}
	
	default Channel[] getEvents(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> events = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isEvent(channel)) events.add(channel);
		}
		return events.toArray(new Channel[events.size()]);
	}
	
	
	default boolean isSignal(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isSignal");
	}
	
	default boolean isCategory(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isCategory");
	}
	
	default boolean isEvent(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isEvent");
	}
	
	default Channel getChannelFromName(IResource resource, String fullChannelName) {
		if(!(resource instanceof IContainer)) return null;
		if(fullChannelName == null || "".equals(fullChannelName)) return null;
		IContainer experiment = (IContainer)resource;
		String subjectName = fullChannelName.split("\\.")[1];
		IResource subject = experiment.findMember(subjectName);
		if(subject == null) return null; 
		Channel[] channels = getChannels(subject);
		if(channels != null)
			for (Channel channel : channels) {
				if(channel.getFullName().equals(fullChannelName)) return channel;
			}
		return null;
	}
	
	
	
}
