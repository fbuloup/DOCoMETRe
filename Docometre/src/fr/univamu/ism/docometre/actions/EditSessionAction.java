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

import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.docometre.wizards.NewResourceWizard;
import fr.univamu.ism.docometre.wizards.OrganizeSessionWizardPage;

public class EditSessionAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private static String ID = "EditSessionAction";
	private IWorkbenchWindow window;
	private IContainer resource;
	
	public EditSessionAction(IWorkbenchWindow window) {
		setId(ID);//$NON-NLS-1$
		setActionDefinitionId(ID);
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText("Edit session"/*DocometreMessages.CompileProcessAction_Text*/);
		setToolTipText("Edit session"/*DocometreMessages.CompileProcessAction_Text*/);
	}
	
	@Override
	public void run() {
		IFolder session = (IFolder)resource;
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		NewResourceWizard newResourceWizard = new NewResourceWizard(ResourceType.SESSION, session.getParent(), NewResourceWizard.MODIFY);
		newResourceWizard.setResource(session);
		WizardDialog wizardDialog = new WizardDialog(shell, newResourceWizard);
		wizardDialog.addPageChangedListener(newResourceWizard);
		if(wizardDialog.open() == Window.OK) {
			try {
				ResourceProperties.setDataFilesNamesPrefix(session, newResourceWizard.getPrefix());
				ResourceProperties.setUseSessionNameInDataFilesNamesAsFirstSuffix(session, newResourceWizard.useSessionNameSuffix());
				ResourceProperties.setUseTrialNumberInDataFilesNamesAsSecondSuffix(session, newResourceWizard.useTrialNumberSuffix());
				int nbTrials = newResourceWizard.getMinTrials();
				int newNbTrials = newResourceWizard.getTrialsNumber();
				
				for (int i = nbTrials + 1; i <= newNbTrials; i++) {
					IFolder trial = session.getFolder(DocometreMessages.Trial + i);
					trial.create(true, true, null);
					ResourceProperties.setDescriptionPersistentProperty(trial, "");
					ResourceProperties.setTypePersistentProperty(trial, ResourceType.TRIAL.toString());
				}
				
				session.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				
				OrganizeSessionWizardPage organizeSessionWizardPage = (OrganizeSessionWizardPage) newResourceWizard.getPage(OrganizeSessionWizardPage.PageName);
				HashMap<Integer, IResource> resultAssociation = organizeSessionWizardPage.getResultAssociation();
				Set<Integer> trialsNumbers = resultAssociation.keySet();
				for (Integer trialNumber : trialsNumbers) {
					IFolder trial = session.getFolder(DocometreMessages.Trial + trialNumber);
					IResource process = resultAssociation.get(trialNumber);
					ResourceProperties.setAssociatedProcessProperty(trial, process.getFullPath().toOSString());
				}
				
				ExperimentsView.refresh(session.getParent(), new IResource[]{session});
			} catch (CoreException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}
	

	@Override
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(((IStructuredSelection) selection).getFirstElement() instanceof IContainer) {
			resource = (IContainer)((IStructuredSelection) selection).getFirstElement();
			setEnabled(ResourceType.isSession(resource));
		}
	}

}
