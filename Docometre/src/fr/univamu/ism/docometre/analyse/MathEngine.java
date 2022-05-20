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
			// Adjust for front and end cut
			int frontCut = MathEngineFactory.getMathEngine().getFrontCut(signal, trialNumber);
			int endCut = MathEngineFactory.getMathEngine().getEndCut(signal, trialNumber);
			if(frontCut > endCut) {
				Activator.logErrorMessage("Error in getTimeValuesForSignal() from MathEngine.java : frontCut (" + frontCut + ") is greater than endCut (" + endCut + ")");
				return null;
			}
			double[] times = new double[endCut - frontCut];
			for (int i = frontCut; i < endCut; i++) {
				times[i-frontCut] = 1f*i/sf;
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
		// Replace all Exp.SubjectA. by Exp.SubjectB.
		String replaceRegExp = projectName + "\\.\\w+\\.";
		String replaceBy = projectName + "." + subjectName +".";
		code = code.replaceAll(replaceRegExp, replaceBy);
		// Replace all Exp.SubjectA by Exp.SubjectB
		replaceRegExp = projectName + "\\.\\w+";
		replaceBy = projectName + "." + subjectName;
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
