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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class RenameResourceOperation extends AbstractOperation {

	private IResource resource;
	private String oldName;
	private String newName;
	private boolean refreshUI;
	private Status renameInMathEngineStatus;
	private boolean renameDataFiles;
	private Status renameDataFilesStatus;
	private IUndoContext undoContext;
	protected String performOldName;
	protected String performNewName;

	public RenameResourceOperation(String label, IResource resource, String newName, boolean refreshUI, boolean renameDataFiles, IUndoContext undoContext) {
		super(label);
		addContext(undoContext);
		this.undoContext = undoContext;
		this.resource = resource;
		oldName = resource.getName().replaceAll("\\.\\w*$", ""); // Remove possible extension
		this.newName = newName;
		this.refreshUI = refreshUI;
		this.renameDataFiles = renameDataFiles;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return performOperation(newName);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return performOperation(oldName);
	}
	
	private IStatus performOperation(String name) {
		try {
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					performOldName = resource.getFullPath().toOSString();
					performNewName = name;
					try {
						String message = NLS.bind(DocometreMessages.RenameRessourceTaskTitle, performOldName, performNewName);
						monitor.beginTask(message, IProgressMonitor.UNKNOWN);
						boolean wasSubject = ResourceType.isSubject(resource);
						boolean wasLoaded = MathEngineFactory.getMathEngine().isSubjectLoaded(resource);
						boolean wasExperiment = ResourceType.isExperiment(resource);
//						boolean updateSelectedExperiment = SelectedExprimentContributionItem.selectedExperiment == resource;
						String fileExtension = "";
						// Detect if it is a dacq default renaming
						monitor.subTask(DocometreMessages.CheckIsDacqSubTaskTitle);
						boolean setDACQAsDefault = false;
						if(ResourceType.isDACQConfiguration(resource)) {
							String fullPath = ResourceProperties.getDefaultDACQPersistentProperty(resource);
							if(fullPath != null) if(fullPath.equals(resource.getFullPath().toOSString())) setDACQAsDefault = true;
						}
						// Compute file extension
						monitor.subTask(DocometreMessages.ComputeFileExtensionSubTaskTitle);
						if(resource.getFileExtension() != null) fileExtension = "." + resource.getFileExtension();
						// Rename resource and get new resource
						monitor.subTask(DocometreMessages.RenameAndGetNewResourceSubTaskTitle);
						IContainer parentResource = resource.getParent();
						Object associatedObject = ResourceProperties.getObjectSessionProperty(resource);
						// if associatedObject is null, we must try to deserialize resource
						boolean removeHandle = false;
						if(associatedObject == null && resource instanceof IFile) {
							associatedObject = ObjectsController.deserialize((IFile)resource);
							removeHandle = true;
						}
						// If resource is process, delete build folder
						if(associatedObject != null && ResourceType.isProcess(resource)) ((Process)associatedObject).cleanBuild();
						resource.move(parentResource.getFullPath().append(name + fileExtension), true, monitor);
						IResource newResource = parentResource.findMember(name + fileExtension);
						if(associatedObject != null) ObjectsController.setResourceForObject(associatedObject, newResource);
						if(removeHandle && associatedObject != null) ObjectsController.removeHandle(associatedObject);
						performNewName = newResource.getFullPath().toOSString();
						newResource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						// Update default DACQ if necessary
						monitor.subTask(DocometreMessages.UpdateDefaultDacqSubTaskTitle);
						if(setDACQAsDefault) ResourceProperties.setDefaultDACQPersistentProperty(newResource, newResource.getFullPath().toOSString());
						
						// Update all process resources affected by this path renaming and update experiments view
						monitor.subTask(DocometreMessages.UpdateAffectedProcessesSubTaskTitle);
						updateProcesses(resource, newResource);
						// Update all trials and process test resources affected by this path renaming and update experiments view
						monitor.subTask(DocometreMessages.UpdateAffectedProcessTestsAndTrialsSubTaskTitle);
						if(ResourceType.isProcess(newResource)) {
							updateTrials(resource, newResource);
							updateProcessTest(resource, newResource);
						}
						if(ResourceType.isDataProcessing(newResource)) updateBatchDataProcessingForProcessOrSubject(resource, newResource, true);
						if(ResourceType.isSubject(newResource)) updateBatchDataProcessingForProcessOrSubject(resource, newResource, false);
						// Get editors (dacq configuration and process) to update part names when necessary
						monitor.subTask(DocometreMessages.UpdateEditorsSubTaskTitle);
						updateEditorsPartName(resource, newResource);
						// If Math engine is started check if renaming is necessary
						monitor.subTask(DocometreMessages.RenameInMathengineSubTaskTitle);
						renameInMathEngineStatus = (Status) Status.OK_STATUS;
						if(MathEngineFactory.getMathEngine().isStarted()) {
							if(wasSubject) {
								if(wasLoaded) {
									// If it's a loaded subject, must be renamed in mathengine
									if(!MathEngineFactory.getMathEngine().renameSubject(resource.getParent().getName(), resource.getName(), name)) {
										renameInMathEngineStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, DocometreMessages.ErrorRenamingSubjectInMathengine);
									}
									Object sessionProperty = newResource.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
									if(sessionProperty != null && sessionProperty instanceof ChannelsContainer) {
										ChannelsContainer channelsContainer = (ChannelsContainer)sessionProperty;
										channelsContainer.setSubject((IFolder) newResource);
									}
								}
								
							}
							// If it's an experiment, brute force rename in mathengine
							if(wasExperiment) {
								if(!MathEngineFactory.getMathEngine().renameExperiment(resource.getName(), name)) {
									renameInMathEngineStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, DocometreMessages.ErrorRenamingEsxperimentInMathengine);
								}
								IResource[] subjects = ResourceProperties.getAllTypedResources(ResourceType.SUBJECT, (IContainer) newResource, monitor);
								for (IResource subject : subjects) {
									if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject) ) {
										Object sessionProperty = subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
										if(sessionProperty != null && sessionProperty instanceof ChannelsContainer) {
											ChannelsContainer channelsContainer = (ChannelsContainer)sessionProperty;
											channelsContainer.setSubject((IFolder) subject);
										}
									}
								}
							}
						}
						if(refreshUI) {
							// Refresh experiments view for this resource
							monitor.subTask(DocometreMessages.RefreshExperimentsSubjectsViewsSubTaskTitle);
							ExperimentsView.refresh(newResource.getParent(), new IResource[]{newResource});
							SubjectsView.refresh();
							FunctionsView.refresh(true);
						}
						
						renameDataFilesStatus = (Status) Status.OK_STATUS;
						if(renameDataFiles) {
							if((ResourceType.isSubject(newResource) || ResourceType.isSession(newResource) || ResourceType.isTrial(newResource))) {
								monitor.subTask(DocometreMessages.CollectDataFileTaskTitle);
								ArrayList<IResource> dataFiles = new ArrayList<IResource>();
								RenameDataFilesHelper.populateDataFilesToRename((IContainer)newResource, dataFiles);
								monitor.subTask(DocometreMessages.RenameDataFileTaskTitle);
								renameDataFilesStatus = (Status) RenameDataFilesHelper.renameDataFiles(newResource, resource, monitor, undoContext);
							}
						}
						resource = newResource;
						monitor.done();
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					
				}
			});
			if(renameDataFilesStatus.isOK() && renameInMathEngineStatus.isOK()) return Status.OK_STATUS;
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		String message = NLS.bind(DocometreMessages.ErrorRenamingResource, performOldName, performNewName);
		MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, message);
		if(!renameDataFilesStatus.isOK()) multiStatus.add(renameDataFilesStatus);
		if(!renameInMathEngineStatus.isOK()) multiStatus.add(renameInMathEngineStatus);
		return multiStatus;
	}
	
	/*
	 * This method update all editors part names when renamed resource affects their path, including DACQ associated file
	 */
	private void updateEditorsPartName(IResource resource, IResource newResource) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (int i = 0; i < editorReferences.length; i++) {
					try {
						boolean refreshPartName = false;
						// Get edited object
						ResourceEditorInput resourceEditorInput = ((ResourceEditorInput)editorReferences[i].getEditorInput());
						Object object = resourceEditorInput.getObject();
						// Get edited resource
						IResource editedResource = ObjectsController.getResourceForObject(object);
						
						// If renamed resource is the edited resource
						if(newResource.equals(editedResource)) {
							// Force part name refresh
							refreshPartName = true;
							// Update resource editor input if necessary (only the case when object is resource) 
							if(object instanceof IResource) resourceEditorInput.setObject(newResource);
						}
						//
						if(object == editedResource && resource.equals(editedResource)) {
							// Force part name refresh
							refreshPartName = true;
							// Update resource editor input if necessary (only the case when object is resource) 
							if(object instanceof IResource) resourceEditorInput.setObject(newResource);
						}
						// If edited resource is process and renamed resource is its associated dacq file, force part refresh 
						if(ResourceType.isProcess(editedResource)) {
							// If editor resource is a process, check if associated DACQ file has been renamed
							String fullPathAssociatedDAQ = ResourceProperties.getAssociatedDACQConfigurationProperty((IFile) editedResource);
							String fullNewPath = newResource.getFullPath().toOSString();
							// If yes mark this editor to be refreshed
							if(fullPathAssociatedDAQ.startsWith(fullNewPath)) refreshPartName = true;
						}
						if(refreshPartName) ((PartNameRefresher)editorReferences[i].getEditor(false)).refreshPartName();
					} catch (PartInitException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				}
			}
		});
	}
	
	/*
	 * This method update associated batch data processing when renamed resource (process) is used 
	 * It also refreshes editors if necessary
	 */
	protected void updateBatchDataProcessingForProcessOrSubject(IResource oldResource, IResource newResource, boolean forProcess) {
		IResource[] dataProcessesResources = ResourceProperties.getAllTypedResources(ResourceType.BATCH_DATA_PROCESSING, newResource.getProject(), null);
		for (IResource dataProcessingResource : dataProcessesResources) {
			Object object = ResourceProperties.getObjectSessionProperty(dataProcessingResource);
			boolean removeHandle = false;
			if(object == null) {
				object = ObjectsController.deserialize((IFile) dataProcessingResource);
				ResourceProperties.setObjectSessionProperty(dataProcessingResource, object);
				ObjectsController.addHandle(object);
				removeHandle = true;
			}
			BatchDataProcessing batchDataProcessing = (BatchDataProcessing) object;
			BatchDataProcessingItem[] batchDataProcessingItems = new BatchDataProcessingItem[0];
			if(forProcess) batchDataProcessingItems = batchDataProcessing.getProcesses();
			else batchDataProcessingItems = batchDataProcessing.getSubjects();
			for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
				if(oldResource.getFullPath().toPortableString().equals(batchDataProcessingItem.getPath())) {
					batchDataProcessingItem.setPath(newResource.getFullPath().toPortableString());
					// Update editor 
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
							for (int i = 0; i < editorReferences.length; i++) {
								ResourceEditorInput resourceEditorInput;
								try {
									resourceEditorInput = ((ResourceEditorInput)editorReferences[i].getEditorInput());
									Object editorObject = resourceEditorInput.getObject();
									if(editorObject == batchDataProcessing) {
										if(forProcess) ((BatchDataProcessingEditor)editorReferences[i].getEditor(false)).refreshProcesses();
										else ((BatchDataProcessingEditor)editorReferences[i].getEditor(false)).refreshSubjects();
									}
								} catch (PartInitException e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
							}
							
						}
					});
				}
			}
			if(removeHandle) {
				ObjectsController.serialize(object);
				ObjectsController.removeHandle(object);
			}
		}
	}
	
	/*
	 * This method update associated processes dacq when renamed resource affects dacq path 
	 * It also refreshes experiment view to reflect these changes
	 */
	private void updateProcesses(IResource oldResource, IResource newResource) {
		String fullOldPath = oldResource.getFullPath().toOSString();
		String fullNewPath = newResource.getFullPath().toOSString();
		IResource[] processes = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, newResource.getProject(), null);
		for (IResource process : processes) {
			String fullPathAssociatedDAQ = ResourceProperties.getAssociatedDACQConfigurationProperty((IFile) process);
			if((fullPathAssociatedDAQ != null) && fullPathAssociatedDAQ.startsWith(fullOldPath)) {
				String newFullPathAssociatedDAQ = fullPathAssociatedDAQ.replaceAll(fullOldPath, fullNewPath);
				ResourceProperties.setAssociatedDACQConfigurationProperty((IFile) process, newFullPathAssociatedDAQ);
				ExperimentsView.refresh(process.getParent(), new IResource[]{newResource});
			}
		}
	}
	
	/*
	 * This method update associated processes when renamed resource affects trials  
	 * It also refreshes experiment view to reflect these changes
	 */
	private void updateTrials(IResource oldResource, IResource newResource) {
		String fullOldPath = oldResource.getFullPath().toOSString();
		String fullNewPath = newResource.getFullPath().toOSString();
		IResource[] trials = ResourceProperties.getAllTypedResources(ResourceType.TRIAL, newResource.getProject(), null);
		for (IResource trial : trials) {
			String fullPathAssociatedProcess = ResourceProperties.getAssociatedProcessProperty(((IFolder) trial));
			if((fullPathAssociatedProcess != null) && fullPathAssociatedProcess.equals(fullOldPath)) {
				ResourceProperties.setAssociatedProcessProperty((IFolder) trial, fullNewPath);
				ExperimentsView.refresh(trial.getParent(), new IResource[]{newResource});
			}
		}
	}
	
	/*
	 * This method update associated process test folder when renamed resource affects  
	 * It also refreshes experiment view to reflect these changes
	 */
	private void updateProcessTest(IResource oldResource, IResource newResource) {
		String fullOldPath = oldResource.getFullPath().toOSString();
		String fullNewPath = newResource.getFullPath().toOSString();
		IResource[] processTests = ResourceProperties.getAllTypedResources(ResourceType.PROCESS_TEST, newResource.getProject(), null);
		for (IResource processTest : processTests) {
			String fullPathAssociatedProcess = ResourceProperties.getAssociatedProcessProperty(processTest);
			if((fullPathAssociatedProcess != null) && fullPathAssociatedProcess.equals(fullOldPath)) {
				ResourceProperties.setAssociatedProcessProperty(processTest, fullNewPath);
				ExperimentsView.refresh(processTest.getParent(), new IResource[]{newResource});
			}
		}
	}
	
}
