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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;

public final class Analyse {

	public static String getDataFiles(IResource subject) {
		if(!ResourceType.isSubject(subject)) return "";
		try {
			List<String> dataFilesList = new ArrayList<String>();
			getData((IContainer) subject, dataFilesList);
			
			
//			IResource[] members = ((IContainer)subject).members();
//			for (IResource member : members) {
//				if(ResourceType.isADWDataFile(member) || ResourceType.isSamples(member)) {
//					dataFilesList.add(member.getLocation().toOSString());
//				}
//			}
			return String.join(";", dataFilesList); 
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return ""; 
	}
	
	private static void getData(IContainer resource, List<String> dataFilesList) throws CoreException {
		IResource[] members = resource.members();
		for (IResource member : members) {
			if(ResourceType.isADWDataFile(member) || ResourceType.isSamples(member) || ResourceType.isOptitrack_Type_1(member) || ResourceType.isColumnDataFile(member)) {
				dataFilesList.add(member.getLocation().toOSString());
			}
			if(ResourceType.isSession(member) || ResourceType.isTrial(member)) getData((IContainer)member, dataFilesList);
		}
	}
	
	
	public static Map<String, String> getSessionsInformations(IResource resource) {
		int baseTrialsNumber = 0;
		long maxSamples = 0;
		Map<String, String> values = new HashMap<String, String>();
		Map<IResource, DACQConfiguration> dacq = new HashMap<IResource, DACQConfiguration>();
		try {
			if(!ResourceType.isSubject(resource)) return values;
			IContainer subject = (IContainer)resource;
			IResource[] members = subject.members();
			for (IResource member : members) {
				if(ResourceType.isSession(member)) {
					IContainer session = (IContainer) member;
					
					String sessionName = session.getName();
					
					values.put(sessionName + "_BASE_TRIALS_NUMBER", String.valueOf(baseTrialsNumber));
					baseTrialsNumber += ResourceProperties.getTotalNumberOfTrials(session);

					boolean value = ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(member);
					values.put(sessionName + "_USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES", Boolean.toString(value));
					
					value = ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(member);
					values.put(sessionName + "_USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES", Boolean.toString(value));
					
					String valueString = ResourceProperties.getDataFilesNamesPrefix(member);
					valueString = valueString == null ? "" : valueString;
					values.put(sessionName + "_DATA_FILES_NAMES_PREFIX", valueString);
					
					IResource[] sessionMembers = session.members();
					for (IResource sessionMember : sessionMembers) {
						if(ResourceType.isTrial(sessionMember)) {
							IResource processFile = ResourceProperties.getAssociatedProcess(sessionMember);
							IResource dacqFile = ResourceProperties.getAssociatedDACQConfiguration(processFile);
							String systemType = ResourceProperties.getSystemPersistentProperty(dacqFile);
							values.put(sessionMember.getLocation().toOSString() + "_PROCESS", processFile.getName().replaceAll(Activator.processFileExtension, ""));
							values.put(sessionMember.getLocation().toOSString() + "_SYSTEM", systemType);
							
							Object object = dacq.get(dacqFile);
							if(object == null) {
								object = ObjectsController.deserialize((IFile)dacqFile);
							}
							DACQConfiguration dacqConfiguration = (DACQConfiguration) object;
							dacq.put(dacqFile, dacqConfiguration);
							Channel[] channels = dacqConfiguration.getChannels();
							
							for (Channel channel : channels) {
								String sf = channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
								values.put(channel.getProperty(ChannelProperties.NAME)+ "_SF", sf);
							}
							
							IResource[] trialMembers = ((IContainer) sessionMember).members();
							
							for (IResource trialMember : trialMembers) {
								if(ResourceType.isSamples(trialMember)) {
									long nbSamples = ResourceProperties.getSamplesNumber(trialMember);
									maxSamples = maxSamples < nbSamples ? nbSamples : maxSamples;
								}
							}
							
						}
					}
					
				}
			}
			values.put("TOTAL_TRIALS_NUMBER", String.valueOf(baseTrialsNumber));
			values.put("MAXIMUM_SAMPLES", String.valueOf(maxSamples));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return values;
		
	}
	
	public static boolean isColumnDataFile(String[] dataFiles, IContainer subject) {
		boolean response = dataFiles.length > 0;
		for (String dataFile : dataFiles) {
			if(Platform.getOS().equals(Platform.OS_WIN32)) {
				dataFile = dataFile.replaceAll("\\\\", "/");
				String path = subject.getLocation().toPortableString();
				dataFile = dataFile.replaceAll(path, "");
				dataFile  = dataFile.replaceAll("/", "\\\\");
			} else dataFile = dataFile.replaceAll(subject.getLocation().toOSString(), "");
			IResource resource = subject.findMember(dataFile);
			response = response && ResourceType.isColumnDataFile(resource);
		}
		return response;
	}
	
	public static boolean isOptitrack(String[] dataFiles, IContainer subject) {
		boolean response = dataFiles.length > 0;
		for (String dataFile : dataFiles) {
			if(Platform.getOS().equals(Platform.OS_WIN32)) {
				dataFile = dataFile.replaceAll("\\\\", "/");
				String path = subject.getLocation().toPortableString();
				dataFile = dataFile.replaceAll(path, "");
				dataFile  = dataFile.replaceAll("/", "\\\\");
			} else dataFile = dataFile.replaceAll(subject.getLocation().toOSString(), "");
			IResource resource = subject.findMember(dataFile);
			response = response && ResourceType.isOptitrack_Type_1(resource);
		}
		return response;
	}
}
