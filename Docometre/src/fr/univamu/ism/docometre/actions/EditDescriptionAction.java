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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
//import fr.univamu.ism.docometre.model.Resource;
import fr.univamu.ism.docometre.views.DescriptionView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class EditDescriptionAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private static String ID = "EditDescriptionAction";

	private IResource selectedResource;
	private IWorkbenchWindow window;
	private IOperationHistory operationHistory;

	public EditDescriptionAction(IWorkbenchWindow window) {
		super(DocometreMessages.EditActionTitle, Action.AS_CHECK_BOX);
		setId(ID); //$NON-NLS-1$
		setActionDefinitionId(ID);
		setToolTipText(DocometreMessages.EditActionTitle);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.EDIT_DESCRIPTION_ICON));
		window.getSelectionService().addSelectionListener(this);
		this.window = window;
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		setEnabled(false);
	}

	@Override
	public void run() {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				DescriptionView descriptionView = (DescriptionView) page.findView(DescriptionView.ID);
				if(descriptionView == null) descriptionView = (DescriptionView) page.showView(DescriptionView.ID);
				if(!isChecked()) {
					try {
						if(selectedResource != null  && selectedResource.exists()) {
							String currentDescription = ResourceProperties.getDescriptionPersistentProperty(selectedResource);
							if(!descriptionView.getDescription().equals(currentDescription)) {
								IWorkbenchPart workbenchPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
								IUndoContext undoContext = null;
								if(workbenchPart instanceof ExperimentsView) undoContext = ExperimentsView.experimentsViewUndoContext;
								if(workbenchPart instanceof SubjectsView) undoContext = SubjectsView.subjectsViewUndoContext;
								if(undoContext != null) operationHistory.execute(new EditDescriptionOperation(DocometreMessages.EditActionTitle, selectedResource, descriptionView.getDescription(), undoContext), null, null);
							}
							
						}
					} catch (ExecutionException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				} 
			} catch (PartInitException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
	}

	@Override
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if (!selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if(!(element instanceof IResource)) return;
			setEnabled(true);
			selectedResource = (IResource) ((IStructuredSelection) selection).getFirstElement();
		}
	}

}
