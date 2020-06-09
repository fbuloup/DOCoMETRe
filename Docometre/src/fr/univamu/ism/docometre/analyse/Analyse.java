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
			IResource[] members = ((IContainer)subject).members();
			for (IResource member : members) {
				if(ResourceType.isADWDataFile(member) || ResourceType.isSamples(member)) {
					dataFilesList.add(member.getLocation().toOSString());
				}
			}
			return String.join(";", dataFilesList); 
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return ""; 
	}
}
