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
package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class ResetTrialsHandler extends AbstractHandler implements ISelectionListener {

	private IFolder[] trialsOrSessions;
	
	public ResetTrialsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(experimentsView != null) selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				trialsOrSessions = new IFolder[0];
				for (int i = 0; i < elements.length; i++) {
					if(!(elements[i] instanceof IFolder)) return;
				}
				trialsOrSessions = new IFolder[elements.length];
				for (int i = 0; i < elements.length; i++) {
					trialsOrSessions[i] = (IFolder)elements[i];
				}
			}
		}
	}


	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExperimentsView view = (ExperimentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		for (IFolder trialOrSession : trialsOrSessions) {
			if(ResourceType.isTrial(trialOrSession)) {
				ResourceProperties.setTrialState(trialOrSession, !ResourceProperties.isTrialDone(trialOrSession));
				view.refreshInput(trialOrSession.getParent().getParent(), new Object[] {trialOrSession});
			}
			if(ResourceType.isSession(trialOrSession)) {
				IResource[] trials = new IResource[0];
				try {
					trials = trialOrSession.members();
					for (IResource trial : trials) ResourceProperties.setTrialState(trial, !ResourceProperties.isTrialDone(trial));
				} catch (CoreException e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				} finally {
					view.refreshInput(trialOrSession.getParent(), trials);
				}
			}
		}
		
		return null;
	}

	@Override
	public boolean isEnabled() {
		return trialsOrSessions != null && trialsOrSessions.length > 0;
	}


}
