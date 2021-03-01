package fr.univamu.ism.docometre.analyse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.analyse.wizard.ExportDataWizard;

public class ExportDataHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExportDataWizard exportDataWizard = new ExportDataWizard();
		WizardDialog wizardDialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), exportDataWizard);
		wizardDialog.open();
		return null;
	}

	

}
