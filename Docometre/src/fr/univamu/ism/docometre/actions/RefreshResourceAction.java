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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;


public class RefreshResourceAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private IResource[] selectedResources;
	private IWorkbenchWindow window;
	
	
	public RefreshResourceAction(IWorkbenchWindow window) {
		setId("RefreshResourceAction");
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
//		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
//		selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}
	
	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					for (IResource resource : selectedResources) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
							ExperimentsView.refresh(resource.getParent(), new IResource[]{resource});
							SubjectsView.refresh(resource.getParent(), new IResource[]{resource});
							FunctionsView.refresh(true);
						} catch (CoreException e) {
							e.printStackTrace();
							Activator.logErrorMessageWithCause(e);
						}
					}
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} 
		
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(true);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				selectedResources = new IResource[elements.length];
				for (int i = 0; i < elements.length; i++) selectedResources[i] = (IResource) elements[i]; 
			}
			if(selectedResources == null || selectedResources.length == 0) {
				selectedResources = new IResource[] {ResourcesPlugin.getWorkspace().getRoot()};
			}
			
			setEnabled(selectedResources != null && selectedResources.length > 0);
		}
	}
	
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}


}
