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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
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
		Shell shell = HandlerUtil.getActiveShell(event);
		if(!MessageDialog.openQuestion(shell, DocometreMessages.RemoveLogAndDataDialogTitle, DocometreMessages.RemoveLogAndDataDialogMessage2)) return null;
		// The selection may change while the dialog is open : work on a copy
		IFolder[] folders = resources.clone();
		try {
			// Modal dialog : UI is blocked, work is done in a non UI thread and can be cancelled
			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						removeLogAndDataFiles(folders, shell.getDisplay(), monitor);
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		} catch (InterruptedException e) {
			// Cancelled by user : files already deleted stay deleted
		} finally {
			// Refresh UI only once, whatever the result
			for (IFolder folder : folders) ExperimentsView.refresh(folder, null);
			SubjectsView.refresh();
		}
		return null;
	}

	private void removeLogAndDataFiles(IFolder[] folders, Display display, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.RemoveLogAndDataFileTaskTitle, 10);

		// 1. Collect log and samples resources (no UI access)
		List<IResource> resourcesToDelete = new ArrayList<>();
		SubMonitor collectMonitor = subMonitor.split(1);
		for (IFolder folder : folders) {
			folder.accept(new IResourceProxyVisitor() {
				@Override
				public boolean visit(IResourceProxy proxy) throws CoreException {
					if(collectMonitor.isCanceled()) throw new OperationCanceledException();
					IResource resource = proxy.requestResource();
					// Read type persistent property only once
					ResourceType resourceType = ResourceType.getResourceType(resource);
					if(resourceType == ResourceType.SAMPLES || resourceType == ResourceType.LOG) {
						resourcesToDelete.add(resource);
						return false;
					}
					return true;
				}
			}, IResource.NONE);
		}
		if(subMonitor.isCanceled()) throw new OperationCanceledException();

		// 2. Close editors in UI thread, in one pass
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				for (IResource resource : resourcesToDelete) Activator.closeEditor(resource);
			}
		});

		// 3. Delete resources in one workspace operation : only one resource change notification at the end
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor deleteMonitor = SubMonitor.convert(monitor, resourcesToDelete.size());
				for (IResource resource : resourcesToDelete) {
					deleteMonitor.subTask(resource.getFullPath().toString());
					// split() throws OperationCanceledException if user cancelled
					resource.delete(true, deleteMonitor.split(1));
				}
			}
		}, MultiRule.combine(folders), IWorkspace.AVOID_UPDATE, subMonitor.split(9));
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
