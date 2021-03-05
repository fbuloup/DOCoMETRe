package fr.univamu.ism.docometre.analyse;

import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;

public interface MathEngine {
	IStatus startEngine(IProgressMonitor monitor);
	IStatus stopEngine(IProgressMonitor monitor);
	boolean isStarted();
	void addListener(MathEngineListener listener);
	boolean isSubjectLoaded(IResource subject);
	default void load(IResource subject, boolean fromRawData) {
		try {
			ChannelsContainer channelsContainer = new ChannelsContainer((IFolder) subject);
			subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, channelsContainer);
			getChannels(subject);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	default void unload(IResource subject) {
		try {
			subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, null);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	String[] getLoadedSubjects();
	boolean exist(String variableName);
	boolean isStruct(String variableName);
	boolean isField(String variableName, String fieldName);
	Channel[] getChannels(IResource subject);
	boolean isSignalCategoryOrEvent(String fullName, String check);
	String getCriteriaForCategory(IResource category);
	Integer[] getTrialsListForCategory(IResource category);
	double[] getYValuesForSignal(Channel signal, int trialNumber);
	int getTrialsNumber(Channel signal);
	double getSampleFrequency(Channel signal);
	int getSamplesNumber(Channel signal, int trialNumber);
	int getFrontCut(Channel signal, int trialNumber);
	int getEndCut(Channel signal, int trialNumber);
	void runScript(String code);
	void deleteChannel(Channel resource);
	void saveSubject(IResource subject);
	String evaluate(String command) throws Exception;
	IResource[] getCreatedOrModifiedSubjects();
	
	// Markers
	int getNbMarkersGroups(Channel signal);
	void createNewMarkersGroup(Channel signal, String markersGroupLabel);
	String getMarkersGroupLabel(int markersGroupNumber, Channel signal);
	void deleteMarkersGroup(int markersGroupNumber, Channel signal);
	void addMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal);
	double[][] getMarkers(String markersGroupLabel, Channel signal);
	void deleteMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal);
	
	// Features
	int getNbFeatures(Channel signal);
	String getFeatureLabel(int featureNumber, Channel signal);
	void deleteFeature(int featureNumber, Channel signal);
	double[] getFeature(String featureLabel, Channel signal);
	
	boolean renameExperiment(String oldName, String newName);
	boolean renameSubject(String experimentName, String oldSubjectName, String newSubjectName);
	
	String getErrorMessages();
	
	default int getMarkersGroupNumber(String markersGroupLabel, Channel signal) {
		String[] labels = getMarkersGroupsLabels(signal);
		return Arrays.binarySearch(labels, markersGroupLabel);
	}
	
	default String[] getMarkersGroupsLabels(Channel signal) {
		int nbMarkersGroups = getNbMarkersGroups(signal);
		String[] markersGroupsLabels = new String[nbMarkersGroups];
		for (int i = 0; i < markersGroupsLabels.length; i++) {
			markersGroupsLabels[i] = getMarkersGroupLabel(i + 1, signal);
		}
		return markersGroupsLabels;
	}
	
	default String[] getFeaturesLabels(Channel signal) {
		int nbFeatures = getNbFeatures(signal);
		String[] featuresLabels = new String[nbFeatures];
		for (int i = 0; i < featuresLabels.length; i++) {
			featuresLabels[i] = getFeatureLabel(i + 1, signal);
		}
		return featuresLabels;
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
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getSignals();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}

	
	default Channel[] getCategories(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getCategories();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default Channel[] getEvents(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getEvents();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default Channel[] getMarkers(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getMarkers();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default Channel[] getFeatures(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getFeatures();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default Channel[] getFrontEndCuts(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				return channelsContainer.getFrontEndCuts();
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default void setUpdateChannelsCache(IResource subject, boolean update) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				channelsContainer.setUpdateChannelsCache(update);
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
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
		try {
			if(!(resource instanceof IContainer)) return null;
			if(fullChannelName == null || "".equals(fullChannelName)) return null;
			
			if(Channel.fromBeginningChannel.getName().equals(fullChannelName)) 
				return Channel.fromBeginningChannel;
			else if(Channel.toEndChannel.getName().equals(fullChannelName)) 
				return Channel.toEndChannel;
			
			IContainer experiment = (IContainer)resource;
			
			String subjectName = fullChannelName.split("\\.")[1];
			IResource subject = experiment.findMember(subjectName);
			if(subject == null) return null; 
			
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				if(Channel.matchMarker(fullChannelName)) {
					Channel[] markers = channelsContainer.getMarkers();
					for (Channel marker : markers) {
						if(fullChannelName.equals(marker.getFullName())) return marker;
					}
				} else if(Channel.matchFeature(fullChannelName)) {
					Channel[] features = channelsContainer.getFeatures();
					for (Channel feature : features) {
						if(fullChannelName.equals(feature.getFullName())) return feature;
					}
				} else if(Channel.matchFrontEndCut(fullChannelName)) {
					Channel[] frontEndCuts = channelsContainer.getFrontEndCuts();
					for (Channel frontEndCut : frontEndCuts) {
						if(fullChannelName.equals(frontEndCut.getFullName())) return frontEndCut;
					}
				} else if(fullChannelName.split("\\.").length > 1) {
					Channel[] channels = getChannels(subject);
					if(channels != null)
						for (Channel channel : channels) {
							if(channel.getFullName().equals(fullChannelName)) return channel;
						}
				}
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	default String refactor(String code, IResource subjectResource) {
		String projectName = subjectResource.getProject().getName();
		String subjectName = subjectResource.getName();
		String replaceRegExp = projectName + "\\.\\w+\\.";
		String replaceBy = projectName + "." + subjectName +".";
		code = code.replaceAll(replaceRegExp, replaceBy);
		return code;
	}
	
	default String getCategoryForTrialNumber(Channel channel, int trialNumber) {
		IResource subject = channel.getSubject();
		Channel[] categories = getCategories(subject);
		for (Channel category : categories) {
			Integer[] trials = getTrialsListForCategory(category);
			if(Arrays.asList(trials).contains(trialNumber)) {
				return getCriteriaForCategory(category);
			}
		}
		return "";
	}
	
	
	
	
}
