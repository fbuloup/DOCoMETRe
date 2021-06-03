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
package fr.univamu.ism.docometre;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

public final class ResourceProperties {
	
	/*
	 * Qualified names for persistent resource properties 
	 */
	// Default DACQ file for project
	public static QualifiedName DEFAULT_DACQ_QN = new QualifiedName(Activator.PLUGIN_ID, "default.daq");
	// Resource type (Experiment, Subject etc.) see ResourceType class for details 
	public static QualifiedName TYPE_QN = new QualifiedName(Activator.PLUGIN_ID, "type");
	// Description
	public static QualifiedName DESCRIPTION_QN = new QualifiedName(Activator.PLUGIN_ID, "description");
	// System type (ADWin, NI etc.)
	public static QualifiedName SYSTEM_QN = new QualifiedName(Activator.PLUGIN_ID, "system");
	// Associated DACQ file
	public static QualifiedName ASSOCIATED_DACQ_CONFIGURATION_FILE_QN = new QualifiedName(Activator.PLUGIN_ID, "associated.dacq.configuration.file");
	// Associated process file
	public static QualifiedName ASSOCIATED_PROCESS_FILE_QN = new QualifiedName(Activator.PLUGIN_ID, "associated.process.file");
	// Trial state : 
	//    - if trial.state property doesn't exist or is not equal to "done", trial is undone
	//    - if trial.state property exists and is equal to "done" trial is done !
	public static QualifiedName TRIAL_STATE_QN = new QualifiedName(Activator.PLUGIN_ID, "trial.state");
	// Trial state : AUTO or MANUAL
	//    - if trial.start.mode property doesn't exist or is not equal to "AUTO", trial start mode is MANUAL
	//    - if trial.start.mode property exists and is equal to "AUTO" trial is done !
	public static QualifiedName TRIAL_START_MODE_QN = new QualifiedName(Activator.PLUGIN_ID, "trial.start.mode");
	// Prefix for data files names, if null, means prefix is not used
	public static QualifiedName DATA_FILES_NAMES_PREFIX_QN = new QualifiedName(Activator.PLUGIN_ID, "data.files.name.prefix");
	// First suffix, if null, means first suffix is not used : true or false
	public static QualifiedName USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN = new QualifiedName(Activator.PLUGIN_ID, "use.session.name.as.first.suffix.in.data.files.name");
	// Second suffix, if null, means Second suffix is not used : true or false
	public static QualifiedName USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN = new QualifiedName(Activator.PLUGIN_ID, "use.trial.number.as.second.suffix.in.data.files.name");
	// Separator used on properties file
	public static String SEPARATOR = ">>>";
	

	/*
	 * Qualified names for session resource properties 
	 */
	// Deserialized object associated to resource file 
	public static QualifiedName OBJECT_QN = new QualifiedName(Activator.PLUGIN_ID, "object");
	// Data files list
	public static QualifiedName DATA_FILES_LIST_QN = new QualifiedName(Activator.PLUGIN_ID, "data.files.list");
	// Channels list
	public static QualifiedName CHANNELS_LIST_QN = new QualifiedName(Activator.PLUGIN_ID, "channels.list");
	// Subject modified
	public static QualifiedName SUBJECT_MODIFIED_QN = new QualifiedName(Activator.PLUGIN_ID, "subject.modified");
	
	public static void setSubjectModified(IResource resource, boolean modified) {
		try {
			if(!ResourceType.isSubject(resource)) return;
			resource.setSessionProperty(SUBJECT_MODIFIED_QN, modified);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static boolean isSubjectModified(IResource resource) {
		boolean modified = false;
		try {
			if(ResourceType.isSubject(resource)) {
				if(resource.getSessionProperty(SUBJECT_MODIFIED_QN) != null)
					modified = (boolean) resource.getSessionProperty(SUBJECT_MODIFIED_QN);
			}
			
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return modified;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/*
	* Methods for persistent resource properties 
	*/
	/*
	 * Return default dacq configuration file path 
	 */
	public static String getDefaultDACQPersistentProperty(IResource resource) {
		try {
			return resource.getProject().getPersistentProperty(DEFAULT_DACQ_QN);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return "";
	}
	
	/*
	 * Set default dacq configuration file path
	 */
	public static void setDefaultDACQPersistentProperty(IResource resource, String value) {
		try {
			resource.getProject().setPersistentProperty(DEFAULT_DACQ_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Return the type of the resource : Experiment, subject etc. See ResourceType class for details
	 */
	public static String getTypePersistentProperty(IResource resource) {
		try {
			if(resource.exists()) return resource.getPersistentProperty(TYPE_QN);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return "";
	}
	
	/*
	 * Set the type of the resource : Experiment, subject etc. See ResourceType class for details
	 */
	public static void setTypePersistentProperty(IResource resource, String value) {
		try {
			resource.setPersistentProperty(TYPE_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Get the resource description
	 */
	public static String getDescriptionPersistentProperty(IResource resource) {
		try {
			return resource.getPersistentProperty(DESCRIPTION_QN);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return "";
	}
	
	/*
	 * Set the resource description
	 */
	public static void setDescriptionPersistentProperty(IResource resource, String value) {
		try {
			resource.setPersistentProperty(DESCRIPTION_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Get the resource type (ADwin, NI etc.). See Activator for details.
	 */
	public static String getSystemPersistentProperty(IResource resource) {
		try {
			return resource.getPersistentProperty(SYSTEM_QN);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return null;
	}
	
	/*
	 * Set the resource type (ADwin, NI etc.). See Activator for details.
	 */
	public static void setSystemPersistentProperty(IResource resource, String value) {
		try {
			resource.setPersistentProperty(SYSTEM_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Set process file associated DACQ configuration file path
	 */
	public static void setAssociatedDACQConfigurationProperty(IResource processFile, String value) {
		try {
			processFile.setPersistentProperty(ASSOCIATED_DACQ_CONFIGURATION_FILE_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Get process file associated trial file path
	 */
	public static IResource getAssociatedDACQConfiguration(IResource processFile) {
		IResource dacqFile = null;
		String associatedDacqFullPath = getAssociatedDACQConfigurationProperty(processFile);
		if(associatedDacqFullPath != null) dacqFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedDacqFullPath);
		return dacqFile;
	}
	
	/*
	 * Get process file associated DACQ configuration file path
	 */
	public static String getAssociatedDACQConfigurationProperty(IResource processFile) {
		if(!processFile.exists()) return null;
		if(!ResourceType.isProcess(processFile)) return null;
		try {
			return processFile.getPersistentProperty(ASSOCIATED_DACQ_CONFIGURATION_FILE_QN);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return null;
	}
	
	/*
	 * Set process file associated trial file path
	 */
	public static void setAssociatedProcessProperty(IResource folder, String value) {
		try {
			folder.setPersistentProperty(ASSOCIATED_PROCESS_FILE_QN, value);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Get process file associated trial file path
	 */
	public static IResource getAssociatedProcess(IResource trialFolder) {
		IResource processFile = null;
		String associatedProcessFullPath = getAssociatedProcessProperty(trialFolder);
		if(associatedProcessFullPath != null) processFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedProcessFullPath);
		return processFile;
	}
	
	/*
	 * Return process file path associated with trial file 
	 */
	public static String getAssociatedProcessProperty(IResource folder) {
		try {
			if(!folder.exists()) return null;
			if(!(ResourceType.isTrial(folder) || ResourceType.isProcessTest(folder))) return null;
			return folder.getPersistentProperty(ASSOCIATED_PROCESS_FILE_QN);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Return trial execution state
	 */
	public static boolean isTrialDone(IResource trialFolder) {
		try {
			String state = trialFolder.getPersistentProperty(TRIAL_STATE_QN);
			if(state == null) return false;
			if(state.equals("done") || state.equals("true")) return true;
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * Set trial execution state
	 */
	public static void setTrialState(boolean done, IResource trialFolder) {
		try {
			if(done == true) trialFolder.setPersistentProperty(TRIAL_STATE_QN, "done");
			if(done == false) trialFolder.setPersistentProperty(TRIAL_STATE_QN, "undone");
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	/*
	 * Return number of trials associated to this session.
	 * If container is not a session, trials number will be zero.
	 */
	public static int getTrialsNumber(IContainer session) {
		int nbTrials = 0;
		try {
			IResource[] members  = session.members();
			for (IResource member : members) {
				if(ResourceType.isTrial(member)) {
					int trialNumber = Integer.parseInt(member.getName().split("°")[1]);
					if(trialNumber > nbTrials) nbTrials = trialNumber;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return nbTrials;
	}
	
	
	public static String getDataFilesNamesPrefix(IResource resource) {
		try {
			if(!resource.exists()) return null;
			if(!ResourceType.isSession(resource)) return null;
			return resource.getPersistentProperty(DATA_FILES_NAMES_PREFIX_QN);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	public static void setDataFilesNamesPrefix(IResource resource, String value) {
		try {
			resource.setPersistentProperty(DATA_FILES_NAMES_PREFIX_QN, value);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static boolean useSessionNameInDataFilesNamesAsFirstSuffix(IResource resource) {
		try {
			if(!resource.exists()) return false;
			if(!ResourceType.isSession(resource)) return false;
			return "true".equals(resource.getPersistentProperty(USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}
	
	public static void setUseSessionNameInDataFilesNamesAsFirstSuffix(IResource resource, boolean value) {
		try {
			resource.setPersistentProperty(USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN, String.valueOf(value));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static boolean useTrialNumberInDataFilesNamesAsSecondSuffix(IResource resource) {
		try {
			if(!resource.exists()) return false;
			if(!ResourceType.isSession(resource)) return false;
			return "true".equals(resource.getPersistentProperty(USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}
	
	public static void setUseTrialNumberInDataFilesNamesAsSecondSuffix(IResource resource, boolean value) {
		try {
			resource.setPersistentProperty(USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN, String.valueOf(value));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static TrialStartMode getTrialStartMode(IResource resource) {
		try {
			if(!resource.exists()) return TrialStartMode.MANUAL;
			if(!ResourceType.isTrial(resource)) return TrialStartMode.MANUAL;
			String startMode = resource.getPersistentProperty(TRIAL_START_MODE_QN);
			return TrialStartMode.getStartMode(startMode);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return TrialStartMode.MANUAL;
	}
	
	public static void setTrialStartMode(IResource resource, TrialStartMode startMode) {
		try {
			resource.setPersistentProperty(TRIAL_START_MODE_QN, startMode.getKey());
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static void clonePersistentProperties(IResource originResource, IResource detinationResource) {
		ResourceProperties.setDescriptionPersistentProperty(detinationResource, ResourceProperties.getDescriptionPersistentProperty(originResource));
		ResourceProperties.setTypePersistentProperty(detinationResource, ResourceType.getResourceType(originResource).toString());
		if(ResourceType.isSession(originResource)) {
			ResourceProperties.setDataFilesNamesPrefix(detinationResource, ResourceProperties.getDataFilesNamesPrefix(originResource));
			ResourceProperties.setUseSessionNameInDataFilesNamesAsFirstSuffix(detinationResource, ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(originResource));
			ResourceProperties.setUseTrialNumberInDataFilesNamesAsSecondSuffix(detinationResource, ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(originResource));
		}
		if(ResourceType.isTrial(originResource)) {
			ResourceProperties.setAssociatedProcessProperty(detinationResource, ResourceProperties.getAssociatedProcessProperty(originResource));
		}
		if(ResourceType.isDACQConfiguration(originResource)) {
			ResourceProperties.setSystemPersistentProperty(detinationResource, ResourceProperties.getSystemPersistentProperty(originResource));
		}
		if(ResourceType.isProcess(originResource)) {
			ResourceProperties.setAssociatedDACQConfigurationProperty(detinationResource, ResourceProperties.getAssociatedDACQConfigurationProperty(originResource));
		}
		if(ResourceType.isTrial(originResource)) {
			ResourceProperties.setAssociatedProcessProperty(detinationResource, ResourceProperties.getAssociatedProcessProperty(originResource));
		}
	}
	
	//////////////////////////////////////////////////////
	// Resources functions
	/*
	 * Get all resources of type resourceType in container
	 */
	public static IResource[] getAllTypedResources(ResourceType resourceType, IContainer resourceContainer, IProgressMonitor monitor) {
		ArrayList<IResource> resources = new ArrayList<>(0);
		try {
			IResource[] resourcesMember = resourceContainer.members();
			for (IResource currentResource : resourcesMember) {
				String currentResourceType = ResourceProperties.getTypePersistentProperty(currentResource);
				if (resourceType.toString().equals(currentResourceType))
					resources.add(currentResource);
				if (currentResource instanceof IContainer) {
					IResource[] allResources = getAllTypedResources(resourceType, (IContainer) currentResource, monitor);
					resources.addAll(Arrays.asList(allResources));
				}
				if(monitor != null && monitor.isCanceled()) return new IResource[0];
			}
			if(monitor != null && monitor.isCanceled()) return new IResource[0];
			return resources.toArray(new IResource[resources.size()]);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return new IResource[0];
	}

	
	public static void writeResourcePropertiesToPropertiesFile(IResource resource, Properties properties, String keyName) {
		System.out.println("ResourceProperties.writeResourcePropertiesToPropertiesFile : " + keyName + " for " + resource.getLocation().toOSString());
		if(ResourceProperties.getTypePersistentProperty(resource) != null) properties.put(keyName + SEPARATOR + TYPE_QN.getLocalName(), ResourceProperties.getTypePersistentProperty(resource));
		if(ResourceProperties.getDescriptionPersistentProperty(resource) != null) properties.put(keyName + SEPARATOR + DESCRIPTION_QN.getLocalName(), ResourceProperties.getDescriptionPersistentProperty(resource));
		if(ResourceType.isDACQConfiguration(resource)) properties.put(keyName + SEPARATOR + SYSTEM_QN.getLocalName(), ResourceProperties.getSystemPersistentProperty(resource));
		if(ResourceType.isProcess(resource)) properties.put(keyName + SEPARATOR + ASSOCIATED_DACQ_CONFIGURATION_FILE_QN.getLocalName(), ResourceProperties.getAssociatedDACQConfigurationProperty(resource));
		if(ResourceType.isProcessTest(resource)) properties.put(keyName + SEPARATOR + ASSOCIATED_PROCESS_FILE_QN.getLocalName(), ResourceProperties.getAssociatedProcessProperty(resource));
		if(ResourceType.isTrial(resource)) {
			properties.put(keyName + SEPARATOR + ASSOCIATED_PROCESS_FILE_QN.getLocalName(), ResourceProperties.getAssociatedProcessProperty(resource));
			properties.put(keyName + SEPARATOR + TRIAL_STATE_QN.getLocalName(), String.valueOf(ResourceProperties.isTrialDone(resource)));
			properties.put(keyName + SEPARATOR + TRIAL_START_MODE_QN.getLocalName(), ResourceProperties.getTrialStartMode(resource).toString());
		}
		if(ResourceType.isSession(resource)) {
			if(ResourceProperties.getDataFilesNamesPrefix(resource) != null) properties.put(keyName + SEPARATOR + DATA_FILES_NAMES_PREFIX_QN.getLocalName(), ResourceProperties.getDataFilesNamesPrefix(resource));
			properties.put(keyName + SEPARATOR + USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN.getLocalName(), String.valueOf(ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(resource)));
			properties.put(keyName + SEPARATOR + USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN.getLocalName(), String.valueOf(ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(resource)));
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/*
	* Methods for session resource properties 
	*/
	
	/*
	 * Return deserialized associated resource object
	 */
	public static Object getObjectSessionProperty(IResource resource) {
		try {
			Object object = resource.getSessionProperty(OBJECT_QN);
			return object;
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return null;
	}
	
	/*
	 * Set deserialized associated resource object
	 */
	public static void setObjectSessionProperty(IResource resource, Object object) {
		try {
			resource.setSessionProperty(OBJECT_QN, object);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	public static double getPercentDone(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return 0;
		int nbTrials = getTotalNumberOfTrials(resource);
		int[] undoneTrials = getUndoneTrialsNumbers(resource);
		return 1.0 * (nbTrials - undoneTrials.length)/nbTrials;
	}

	public static int getTotalNumberOfTrials(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return 0;
		try {
			IResource[] members = ((IContainer)resource).members();
			if(ResourceType.isSession(resource)) {
				int total = 0;
				for (IResource member : members) {
					if(ResourceType.isTrial(member)) {
						total += 1;
					}
				}
				return total;
			} else {
				int total = 0;
				for (IResource member : members) {
					if(ResourceType.isSession(member)) {
						total += getTotalNumberOfTrials(member);
					}
				}
				return total;
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	public static int[] getUndoneTrialsNumbers(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return new int[0];
		try {
			IResource[] members = ((IContainer)resource).members();
			if(ResourceType.isSession(resource)) {
				ArrayList<Integer> numbersArrayList = new ArrayList<Integer>(0);
				for (int i = 0; i < members.length; i++) {
					if(ResourceType.isTrial(members[i])) {
						if(!ResourceProperties.isTrialDone(members[i])) {
							String trialNumberString = members[i].getName().split("°")[1];//members[i].getName().replaceAll(DocometreMessages.Trial, "");
							numbersArrayList.add(Integer.parseInt(trialNumberString));
						}
					}
				}
				int[] numbers = new int[numbersArrayList.size()];
				for (int i = 0; i < numbers.length; i++) {
					numbers[i] = numbersArrayList.get(i);
				}
				return numbers;
			} else {
				int total = 0;
				ArrayList<Integer> numbersArrayList = new ArrayList<Integer>(0);
				for (IResource member : members) {
					if(ResourceType.isSession(member)) {
						int[] undoneTrials = getUndoneTrialsNumbers(member);
						for (int i = 0; i < undoneTrials.length; i++) {
							numbersArrayList.add(total + undoneTrials[i]);
						}
						total += getTotalNumberOfTrials(member);
					}
				}
				int[] numbers = new int[numbersArrayList.size()];
				for (int i = 0; i < numbers.length; i++) {
					numbers[i] = numbersArrayList.get(i);
				}
				return numbers;
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return new int[0];
	}
	
	public static long getSamplesNumber(IResource samplesFile) {
		if(!ResourceType.isSamples(samplesFile)) return 0;
		String wp = samplesFile.getWorkspace().getRoot().getLocation().toOSString();
		File trialFile = new File(wp + samplesFile.getFullPath().toOSString());
		return trialFile.length()/4;
	}

}
