package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.dialogs.CompileDialog;

public class CompileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CompileDialog compileDialog = new CompileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		if(compileDialog.open() == Window.OK) {
			IFile[] processes = compileDialog.getProcessesToCompile();
			ApplicationActionBarAdvisor.compileProcessAction.setSelection(processes);
			ApplicationActionBarAdvisor.compileProcessAction.run();
		}
		return null;
	}

}
