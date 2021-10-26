package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;

public class SaveHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WorkbenchJob workbenchJob = new WorkbenchJob("Saving dirty editor.") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IEditorPart dirtyEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				monitor.beginTask("Please wait...", 1);
				dirtyEditor.doSave(monitor);
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		workbenchJob.schedule();
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		return editor != null && editor.isDirty();
	}

}
