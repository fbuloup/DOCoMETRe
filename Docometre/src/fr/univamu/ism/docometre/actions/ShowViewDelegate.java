package fr.univamu.ism.docometre.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;

public final class ShowViewDelegate {
	
	public static void show(String viewID, boolean refreshWorkspace) {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							if(refreshWorkspace) ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e) {
							e.printStackTrace();
							Activator.logErrorMessageWithCause(e);
						} finally {
							PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
								@Override
								public void run() {
									try {
										PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewID).setFocus();
									} catch (PartInitException e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
									}
								}
							});
							
						}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

}
