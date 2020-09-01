package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceType;

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
	
}
