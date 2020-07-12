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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dialogs.RenameResourceDialog;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class RenameResourceAction extends Action implements ISelectionListener, IWorkbenchAction {

	private IOperationHistory operationHistory;
	private IResource resource;
	private IWorkbenchWindow window;
	protected IStatus status;

	public RenameResourceAction(IWorkbenchWindow window) {
		setId("RenameResourceAction"); //$NON-NLS-1$
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.RenameAction_Text);
		setToolTipText(DocometreMessages.RenameAction_Text);
	}

	@Override
	public void run() {
		String message = NLS.bind(DocometreMessages.RenameDialogMessage, resource.getName().replaceAll("." + resource.getFileExtension(), ""));
		ResourceNameValidator resourceNameValidator = new ResourceNameValidator();
		RenameResourceDialog inputDialog = new RenameResourceDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				DocometreMessages.RenameDialogTitle, message, resource.getName().replaceAll("." + resource.getFileExtension(), ""), resourceNameValidator, resource);
		if (inputDialog.open() == Window.OK) {
			try {
				// If resource is Arduino Process, remove ino source file before renaming process  
				if(ResourceType.isProcess(resource)) {
					String associatedDACQFullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
					if(associatedDACQFullPath != null) {
						IResource associatedDACQFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedDACQFullPath);	
						if(associatedDACQFile != null) {
							String system = ResourceProperties.getSystemPersistentProperty(associatedDACQFile); 
							if(Activator.ARDUINO_UNO_SYSTEM.equals(system)) {
								// delete current ino file if it exists
								String inoFileName = resource.getParent().getLocation().toOSString() + File.separatorChar + "BinSource" + File.separator;
								inoFileName = inoFileName + resource.getName().replaceAll(Activator.processFileExtension +"$", ".ino");
								File inoFile = new File(inoFileName);
								if(inoFile.exists()) inoFile.delete();
							}
						}
					}
				}
				
				IStatus status = operationHistory.execute(new RenameResourceOperation(DocometreMessages.RenameAction_Text, resource, inputDialog.getValue(), true), null, null);
				
				if(status.isOK()) {
					// Retrieve new resource
					String fileExtension = "";
					if(resource.getFileExtension() != null) fileExtension = "." + resource.getFileExtension();
					IResource oldResource = resource;
					resource = resource.getParent().findMember(inputDialog.getValue() + fileExtension);
					if(resource != null && resource.exists())
						if((ResourceType.isSubject(resource) || ResourceType.isSession(resource) || ResourceType.isTrial(resource)) && inputDialog.isAlsoRenameDataFiles()) {
							Job renameDataFilesJob = new Job(DocometreMessages.RenameDataFileJobTitle) {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									try {
										monitor.beginTask(DocometreMessages.CollectDataFileTaskTitle, IProgressMonitor.UNKNOWN);
										ArrayList<IResource> dataFiles = new ArrayList<IResource>();
										populateDataFilesToRename((IContainer)resource, dataFiles);
										monitor.beginTask(DocometreMessages.RenameDataFileTaskTitle, dataFiles.size());
										return renameDataFiles(resource, oldResource, monitor);
									} catch (CoreException e) {
										e.printStackTrace();
										Activator.logErrorMessageWithCause(e);
									}
									return Status.OK_STATUS;
								}
							};
							renameDataFilesJob.schedule();
						}
				} else {
					Activator.logErrorMessage(status.getMessage());
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}

	}
	
	private IStatus renameDataFiles(IResource newResource, IResource oldResource, IProgressMonitor monitor) throws CoreException {
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
							status = operationHistory.execute(new RenameResourceOperation(DocometreMessages.RenameAction_Text, dataFile, newPath.removeFileExtension().lastSegment(), false), monitor, null);
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
		
		return Status.OK_STATUS;
	}

	private void populateDataFilesToRename(IContainer resource, ArrayList<IResource> dataFiles) throws CoreException {
		IResource[] childrenResources = resource.members();
		for (IResource childResource : childrenResources) {
			if(ResourceType.isSamples(childResource)) dataFiles.add(childResource);
			if((ResourceType.isSession(resource) || ResourceType.isSubject(resource)) && (childResource instanceof IContainer)) populateDataFilesToRename((IContainer)childResource, dataFiles);
		}
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if (part instanceof ExperimentsView || part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection)
				resource = (IResource) ((IStructuredSelection) selection).getFirstElement();
			setEnabled(resource != null);
		}
	}

	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}
	
	private class ResourceNameValidator implements IInputValidator {

		private String regexp;
		private String errorMessage;

		public ResourceNameValidator() {
			regexp = "^([a-zA-Z]+\\d*\\s*)+$"; //$NON-NLS-1$
			errorMessage = DocometreMessages.NewResourceWizard_ErrorMessage;
			if(ResourceType.isTrial(resource)) {
				regexp = "^" + DocometreMessages.Trial + "\\d+$"; //$NON-NLS-1$ //$NON-NLS-2$
				errorMessage = DocometreMessages.RenameResourceAction_TrialErrorMessage;
			} 
			if (ResourceType.isSamples(resource)){
				regexp = "^([a-zA-Z]+(\\w|\\.)*)+$"; //$NON-NLS-1$
			} 
		}
		
		@Override
		public String isValid(String newText) {
			/*Check pattern resource name*/
			Pattern pattern = Pattern.compile(regexp);
			Matcher matcher = pattern.matcher(newText);
			if(!matcher.matches()) {
				String message = NLS.bind(errorMessage, newText);
				if(ResourceType.isTrial(resource)) message = NLS.bind(errorMessage, DocometreMessages.Trial);
				return message;
			} 
			/*Check if resource name already exists*/
			IContainer parent = resource.getParent();
			if(resource.getFileExtension() != null) {
				if(parent.findMember(newText + "." + resource.getFileExtension()) != null) return DocometreMessages.NewResourceWizard_ErrorMessage2;
			} else if(parent.findMember(newText) != null) return DocometreMessages.NewResourceWizard_ErrorMessage2;
			return null;
		}
		
	}
	
}
