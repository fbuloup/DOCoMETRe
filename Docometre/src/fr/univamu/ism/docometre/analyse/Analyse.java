package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

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
			if(ResourceType.isADWDataFile(member) || ResourceType.isSamples(member)) {
				dataFilesList.add(member.getLocation().toOSString());
			}
			if(ResourceType.isSession(member) || ResourceType.isTrial(member)) getData((IContainer)member, dataFilesList);
		}
	}
	
	
	public static Map<String, String> getSessionsInformations(IResource resource) {
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

					boolean value = ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(member);
					values.put(sessionName + "_USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES", Boolean.toString(value));
					
					value = ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(member);
					values.put(sessionName + "_USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES", Boolean.toString(value));
					
					String valueString = ResourceProperties.getDataFilesNamesPrefix(member);
					values.put(sessionName + "_DATA_FILES_NAMES_PREFIX", valueString);
					
					IResource[] sessionMembers = session.members();
					for (IResource sessionMember : sessionMembers) {
						if(ResourceType.isTrial(sessionMember)) {
							IResource processFile = ResourceProperties.getAssociatedProcess(sessionMember);
							IResource dacqFile = ResourceProperties.getAssociatedDACQConfiguration(processFile);
							String systemType = ResourceProperties.getSystemPersistentProperty(dacqFile);
							values.put(sessionMember.getFullPath().toOSString(), systemType);
							
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
							
							
						}
					}
					
				}
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return values;
		
	}
}
