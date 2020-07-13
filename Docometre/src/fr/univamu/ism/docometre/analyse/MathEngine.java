package fr.univamu.ism.docometre.analyse;

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
	public Channel getChannelWithName(IResource subject, String channelName);
	public Channel[] getSignals(IResource subject);
	public Channel[] getCategories(IResource subject);
	public Channel[] getEvents(IResource subject);
	public boolean isSignal(IResource channel);
	public boolean isCategory(IResource channel);
	public boolean isEvent(IResource channel);
	public String getCriteriaForCategory(IResource category);
	Integer[] getTrialsListForCategory(IResource category);
	double[] getYValuesForSignal(Channel signal, int trialNumber);
	double[] getTimeValuesForSignal(Channel signal, Integer trialNumber);
	int getTrialsNumber(Channel signal);
	double getSampleFrequency(Channel signal);
	int getSamplesNumber(Channel signal, int trialNumber);
	int getFrontCut(Channel getchannel, int selection);
	int getEndCut(Channel getchannel, int selection);
	void runScript(String code);
	Channel getChannelFromName(IResource experiment, String fullChannelName);
	void deleteChannel(Channel resource);
}
