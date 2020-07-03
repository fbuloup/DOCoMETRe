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

import java.text.DecimalFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

public class GetResourceLabelDelegate {
	
	public static String getLabel(IResource resource) {
		if(resource instanceof IProject && !((IProject) resource).isOpen()) return null;
		if(ResourceType.isDACQConfiguration(resource)) {
			String suffix = ResourceProperties.getSystemPersistentProperty(resource); 
			suffix = " [" + suffix + "]";
			return resource.getName().replaceAll(Activator.daqFileExtension + "$", "") + suffix;
		}
		if(ResourceType.isProcess(resource)) {
			String associatedDAQFullPath = ResourceProperties.getAssociatedDACQConfigurationProperty((IFile)resource);
			IResource associatedDAQFile = null;
			if(associatedDAQFullPath != null) associatedDAQFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedDAQFullPath);
			if(associatedDAQFullPath == null || associatedDAQFile == null) associatedDAQFullPath = "No associated DAQ file";
			else associatedDAQFullPath = associatedDAQFullPath.replaceAll(Activator.daqFileExtension + "$", "");
			return resource.getName().replaceAll(Activator.processFileExtension + "$", "") + " [" + associatedDAQFullPath + "]";
		}
		if(ResourceType.isTrial(resource)) {
			TrialStartMode startMode = ResourceProperties.getTrialStartMode(resource);
			IResource processFile = ResourceProperties.getAssociatedProcess((IFolder)resource);
			String associatedProcessFullPath = "";
			if(processFile != null) {
				associatedProcessFullPath = processFile.getFullPath().toOSString();
				associatedProcessFullPath = associatedProcessFullPath.replaceAll(Activator.processFileExtension + "$", "");
			} else associatedProcessFullPath = DocometreMessages.NoAssociatedProcessFile;
			return resource.getName() + " [" + startMode + "]" + " [" + associatedProcessFullPath + "]";
		}
		if(ResourceType.isLog(resource)) return resource.getName().replaceAll(Activator.logFileExtension + "$", "");
		if(ResourceType.isSamples(resource)) return resource.getName().replaceAll(Activator.samplesFileExtension + "$", "");
		if(ResourceType.isParameters(resource)) return resource.getName().replaceAll(Activator.parametersFileExtension + "$", "");
		if(ResourceType.isDataProcessing(resource)) return resource.getName().replaceAll(Activator.dataProcessingFileExtension + "$", "");
		
		if(ResourceType.isSession(resource) || ResourceType.isSubject(resource)) {
			double percent = ResourceProperties.getPercentDone(resource);
			String percentString = DecimalFormat.getPercentInstance().format(percent);
			if(Double.isNaN(percent)) percentString = Double.toString(percent);
			return resource.getName() + " [" + percentString + "]";
		}
		if(ResourceType.isChannel(resource)) return resource.toString();
		
		return resource.getName();
		
	}

}
