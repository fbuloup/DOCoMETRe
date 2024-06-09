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
package fr.univamu.ism.docometre.actions;

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;

public class RenameDataFilesHelper {
	
	private static IStatus status;

	public static IStatus renameDataFiles(IResource newResource, IResource oldResource, IProgressMonitor monitor, IUndoContext undoContext) throws CoreException {
		status = Status.OK_STATUS;
		// Collect all data files
		ArrayList<IResource> dataFiles = new ArrayList<IResource>();
		populateDataFilesToRename((IContainer)newResource, dataFiles);
		// Rename data files
		for (IResource dataFile : dataFiles) {
			boolean rename = false;
			String[] segments = dataFile.getName().split("\\.");
			
			// If only three segments : 
			//      channelName.Tnn.samples -> nothing to rename
			// continue to next data file
			if(segments.length == 3 ) continue;
			
			// If resource is trial, rename segment n-2 and continue to next data file
			if (ResourceType.isTrial(newResource)) {
				String trialNumber = oldResource.getName().replaceAll(DocometreMessages.Trial, "");
				String trialSegment = segments[segments.length - 2];
				
				if(trialSegment.replaceAll("T", "").equals(trialNumber)) {
					rename = true;
					trialNumber = newResource.getName().replaceAll(DocometreMessages.Trial, "");
					trialSegment = trialSegment.replaceAll("\\d+$", trialNumber);
					segments[segments.length - 2] = trialSegment;
				}
			}
			// If four segments : 
			//      Prefix.channelName.Tnn.samples
			// or 
			//      channelName.SessionName.Tnn.samples
			if(segments.length == 4) {
				// if resource is subject, try to rename first segment
				if (ResourceType.isSubject(newResource)) {
					if(oldResource.getName().equals(segments[0])) {
						rename = true;
						segments[0] = newResource.getName();
					}
				}
				// if resource is session, try to rename second segment
				if (ResourceType.isSession(newResource)) {
					if(oldResource.getName().equals(segments[1])) {
						rename = true;
						segments[1] = newResource.getName();
					}
				}
			}
			// If five segments : 
			// 		Prefix.channelName.SessionName.Tnn.samples
			if (segments.length == 5) {
				// if resource is subject, rename first segment
				if (ResourceType.isSubject(newResource)) {
					if(oldResource.getName().equals(segments[0])) {
						rename = true;
						segments[0] = newResource.getName();
					}
				}
				// if resource is session, rename third segment
				if (ResourceType.isSession(newResource)) {
					if(oldResource.getName().equals(segments[2])) {
						rename = true;
						segments[2] = newResource.getName();
					}
				}
			}
			if(rename) {
				String newName = "";
				for (int i = 0; i < segments.length; i++) {
					newName += segments[i];
					if(i < segments.length - 1) newName += ".";
				}
				IPath newPath = dataFile.getParent().getFullPath().append(newName);
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						try {
							IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
							status = operationHistory.execute(new RenameResourceOperation(DocometreMessages.RenameAction_Text, dataFile, newPath.removeFileExtension().lastSegment(), false, false, undoContext), monitor, null);
						} catch (ExecutionException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				});
				if(!status.isOK()) return status;
			}
			monitor.worked(1);
		}
		return status;
	}

	public static void populateDataFilesToRename(IContainer resource, ArrayList<IResource> dataFiles) throws CoreException {
		IResource[] childrenResources = resource.members();
		for (IResource childResource : childrenResources) {
			if(ResourceType.isSamples(childResource)) dataFiles.add(childResource);
			if((ResourceType.isSession(resource) || ResourceType.isSubject(resource)) && (childResource instanceof IContainer)) populateDataFilesToRename((IContainer)childResource, dataFiles);
		}
	}

	
}
