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
package fr.univamu.ism.docometre.dacqsystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class DocometreBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = Activator.PLUGIN_ID + ".DocometreBuilder";
	public static final String MARKER_ID = Activator.PLUGIN_ID + ".DocometreMarker";
	
	public static void addProject(IProject project) {
		
		if(!project.isOpen()) return;
		
		try {
			IProjectDescription projectDescription = project.getDescription();
			ICommand[] commands = projectDescription.getBuildSpec();
			for (ICommand command : commands) if(command.getBuilderName().equals(BUILDER_ID)) return;
			ICommand newCommand = projectDescription.newCommand();
			newCommand.setBuilderName(BUILDER_ID);
			List<ICommand> newCommandsList = new ArrayList<>(Arrays.asList(commands));
			newCommandsList.add(newCommand);
			projectDescription.setBuildSpec(newCommandsList.toArray(new ICommand[newCommandsList.size()]));
			project.setDescription(projectDescription, null);
			
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	public DocometreBuilder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		HashSet<IFile> processesToBuild = new HashSet<>();
		IResourceDelta delta = getDelta(getProject());
		if(delta == null) {
			Activator.logWarningMessage("Delta is null for build !");
			return null;
		}
		IResourceDelta[] affectedChildren = delta.getAffectedChildren();
		for (IResourceDelta resourceDelta : affectedChildren) {
			IResource resource  = resourceDelta.getResource();
			int deltaKind = resourceDelta.getKind();
			if(deltaKind == IResourceDelta.REMOVED) {
				// Check if it is a process from file extension to remove compiled files
				// ...
			}
			// If it's a process 
			if(ResourceType.isProcess(resource)) {
				if(deltaKind == IResourceDelta.ADDED || resourceDelta.getKind() == IResourceDelta.CHANGED) {
					// Add process to compilation
					processesToBuild.add((IFile) resource);
				}
			}
			// If it's a dacq configuration 
			if(ResourceType.isDACQConfiguration(resource)) {
				if(resourceDelta.getKind() == IResourceDelta.CHANGED) {
					// Retrieve all attached processes
					// Add these processes to compilation
					IResource[] resources = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, getProject());
					for (IResource localResource : resources) {
						String fullPath = ResourceProperties.getAssociatedDACQConfigurationProperty((IFile) localResource);
						if(resource.getFullPath().toOSString().equals(fullPath)) processesToBuild.add((IFile) localResource);
					}
				}
			}
		}
		// Launch compilation
//		getProject().deleteMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);
		IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask(DocometreMessages.CompileProcessAction_JobMessage, 4*processesToBuild.size());
				for (IFile processFile : processesToBuild) {
					processFile.deleteMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);
					buildProcess(processFile, monitor);
				}
				monitor.done();
			}
		};
		ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
		return null;
	}
	
	private void buildProcess(IFile resource, IProgressMonitor monitor) {
		// if Process object has not been yet deserialised, will need to decrease handle
		boolean removeProcessHandle = false;
		boolean removeDACQHandle = false;
		boolean compile = false;
		Object object = ResourceProperties.getObjectSessionProperty(resource);
		if(object == null) {
			object = ObjectsController.deserialize(resource);
			ResourceProperties.setObjectSessionProperty(resource, object);
			ObjectsController.addHandle(object);
			removeProcessHandle = true;
			removeDACQHandle = true;
		}
		Process process = (Process) object;
		process.getScript().clearCodeGenerationStatus();
		String fullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
		if(fullPath != null) {
			IFile daqConfigurationFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fullPath));
			if(daqConfigurationFile.exists()) {
				if(ResourceProperties.getObjectSessionProperty(daqConfigurationFile) == null) removeDACQHandle = true;
				compile = true;
			}
		}
		
		Exception exception = null;
		try {
			if(checkCanceled(monitor)) return;
			if(compile) process.compile(monitor);
			else throw new Exception("No associated DACQ File for process " + resource.getFullPath());
			monitor.worked(1);
			if(checkCanceled(monitor)) return;
		} catch (Exception e) {
			exception = e;
		} finally {
			if(removeDACQHandle) ObjectsController.removeHandle(process.getDACQConfiguration());
			if(removeProcessHandle) ObjectsController.removeHandle(process);
			ExperimentsView.refresh(resource.getProject(), new IResource[] {resource});
		}
		if(exception == null) {
			String message = NLS.bind(DocometreMessages.CompileProcessAction_CompileOK, resource.getName().replaceAll(Activator.processFileExtension, ""));
			Activator.logInfoMessage(message, DocometreBuilder.class);
		} else {
			String message = NLS.bind(DocometreMessages.CompileProcessAction_CompileKO, resource.getName().replaceAll(Activator.processFileExtension, ""));
			Activator.logErrorMessage(message);
			Activator.logErrorMessageWithCause(exception);
		}
	}

	private boolean checkCanceled(IProgressMonitor monitor) {
		if(monitor.isCanceled()) throw new OperationCanceledException();
		if(isInterrupted()) return true;
		return false;
	}
	
}
