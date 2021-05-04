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
package fr.univamu.ism.docometre.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public final class ExperimentPropertiesFileCreator {
	
	private static String toRootDirectory;
	private static String fromRootDirectory;

	public static IFile createPropertiesFile(IProject experiment, IPath destinationPath, SubMonitor subMonitor, boolean includeData) {
		FileOutputStream fileOutputStream = null;
		try {
			IFile propertiesFile = experiment.getFile(destinationPath.removeFileExtension().lastSegment() + ".properties");
			IPath path = propertiesFile.getLocation();
			fileOutputStream = new FileOutputStream(path.toOSString());
			toRootDirectory =  destinationPath.removeFileExtension().lastSegment();
			fromRootDirectory = experiment.getName();
			Properties properties = new Properties();
			writePropertiesRecursively(experiment, properties, includeData, subMonitor);
			properties.store(new OutputStreamWriter(fileOutputStream, "UTF-8"), null);
			propertiesFile.refreshLocal(IResource.DEPTH_INFINITE, null);
			return propertiesFile;
		} catch (IOException | CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
			return null;
		} finally {
			if(fileOutputStream != null)
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
		}
	}

	private static void writePropertiesRecursively(IResource resource, Properties properties, boolean includeData, SubMonitor subMonitor) throws CoreException {
		subMonitor.subTask(NLS.bind(DocometreMessages.WritingProperties, resource.getFullPath().toOSString()));
		String keyName =  resource.getFullPath().toPortableString().replaceFirst(fromRootDirectory, toRootDirectory);
		if(ResourceType.isDataFile(resource)) {
			if(includeData) ResourceProperties.writeResourcePropertiesToPropertiesFile(resource, properties, keyName);
		} else ResourceProperties.writeResourcePropertiesToPropertiesFile(resource, properties, keyName);
		if(resource instanceof IContainer) {
			IResource[] members = ((IContainer)resource).members();
			for (IResource member : members) {
				writePropertiesRecursively(member, properties,  includeData, subMonitor);
			}
		}
		
	}

}
