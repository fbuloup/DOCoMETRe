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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
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
	private IUndoContext undoContext;
	
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
				
				IStatus status = operationHistory.execute(new RenameResourceOperation(DocometreMessages.RenameAction_Text, resource, inputDialog.getValue(), true, inputDialog.isAlsoRenameDataFiles(), undoContext), null, null);
				
				if(!status.isOK()) {
					if(status instanceof MultiStatus) {
						IStatus[] statuses = ((MultiStatus)status).getChildren();
						for (IStatus localStatus : statuses) {
							Activator.logErrorMessage(localStatus.getMessage());
						}
					} else {
						Activator.logErrorMessage(status.getMessage());
					}
				} 
			} catch (Exception e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}

	}
	
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if (part instanceof ExperimentsView || part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection)
				resource = (IResource) ((IStructuredSelection) selection).getFirstElement();
			setEnabled(resource != null && !ResourceType.isChannel(resource));
			undoContext = null;
			if(part instanceof ExperimentsView) undoContext = ExperimentsView.experimentsViewUndoContext;
			if(part instanceof SubjectsView) undoContext = SubjectsView.subjectsViewUndoContext;
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
			if (ResourceType.isBatchDataProcessing(resource) || ResourceType.isDataProcessing(resource)){
				regexp = "^([a-zA-Z]+(\\w|\\.|_)*)+$"; //$NON-NLS-1$
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
