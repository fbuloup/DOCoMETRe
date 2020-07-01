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
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class CleanHandler extends AbstractHandler implements ISelectionListener {
	
	private IFolder[] resources;
	
	public CleanHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(experimentsView != null) selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(MessageDialog.openQuestion(PlatformUI.getWorkbench().getDisplay().getActiveShell(), DocometreMessages.RemoveLogAndDataDialogTitle, DocometreMessages.RemoveLogAndDataDialogMessage2)) {
			Job removeLogAndDataFilesJob = new Job(DocometreMessages.RemoveLogAndDataFileJobTitle) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					for (IFolder folder : resources) {
						monitor.beginTask(DocometreMessages.RemoveLogAndDataFileTaskTitle, IProgressMonitor.UNKNOWN);
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								try {
									removeLogAndDataFiles(folder);
									ApplicationActionBarAdvisor.refreshResourceAction.run();
								} catch (CoreException e) {
									e.printStackTrace();
									Activator.logErrorMessageWithCause(e);
								}
								
							}
						});
						
					}
					return Status.OK_STATUS;
				}
			};
			removeLogAndDataFilesJob.schedule();
		}
		
		for (IFolder folder : resources) ExperimentsView.refresh(folder, null);
		
		
		return null;
	}
	
	private void removeLogAndDataFiles(IContainer resource) throws CoreException {
	IResource[] childResources = resource.members();
	for (IResource childResource : childResources) {
		if(ResourceType.isSamples(childResource) || ResourceType.isLog(childResource)) {
			Activator.closeEditor(childResource);
			childResource.delete(true, null);
		}
		if(childResource instanceof IContainer) removeLogAndDataFiles((IContainer)childResource);
	}
}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				resources = new IFolder[0];
				for (int i = 0; i < elements.length; i++) {
					if(!(elements[i] instanceof IFolder)) return;
				}
				resources = new IFolder[elements.length];
				for (int i = 0; i < elements.length; i++) {
					resources[i] = (IFolder)elements[i];
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
	public boolean isEnabled() {
		return resources != null && resources.length > 0;
	}

}
