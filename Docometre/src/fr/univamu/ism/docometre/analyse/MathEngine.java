package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

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
	double[] getTimeValuesForSignal(Channel signal, Integer trialNumber);
	int getTrialsNumber(Channel signal);
	double getSampleFrequency(Channel signal);
	int getSamplesNumber(Channel signal, int trialNumber);
	int getFrontCut(Channel signal, int trialNumber);
	int getEndCut(Channel signal, int trialNumber);
	void runScript(String code);
	Channel getChannelFromName(IResource experiment, String fullChannelName);
	void deleteChannel(Channel resource);
	
	default public Channel getChannelWithName(IResource subject, String channelName) {
		Channel[] channels = getChannels(subject);
		for (Channel channel : channels) {
			if(channel.getName().equals(channelName)) return channel;
		}
		return null;
	}
	
	default public String getFullPath(IResource resource) {
		return resource.getFullPath().toString().replaceAll("^/", "").replaceAll("/", ".");
	}
	
	default public Channel[] getSignals(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> signals = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isSignal(channel)) signals.add(channel);
		}
		return signals.toArray(new Channel[signals.size()]);
	}

	default public Channel[] getCategories(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> categories = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isCategory(channel)) categories.add(channel);
		}
		return categories.toArray(new Channel[categories.size()]);
	}
	
	default public Channel[] getEvents(IResource subject) {
		Channel[] channels = getChannels(subject);
		ArrayList<Channel> events = new ArrayList<Channel>();
		for (Channel channel : channels) {
			if(isEvent(channel)) events.add(channel);
		}
		return events.toArray(new Channel[events.size()]);
	}
	
	
	default public boolean isSignal(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isSignal");
	}
	
	default public boolean isCategory(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isCategory");
	}
	
	default public boolean isEvent(IResource channel) {
		return isSignalCategoryOrEvent(getFullPath(channel), ".isEvent");
	}
	
}
