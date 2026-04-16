package fr.univamu.ism.docometre.analyse.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class MergeSubjectsWizard extends Wizard {

	private boolean anErrorOccured = false;
	private MergeSubjectsWizardPage mergeSubjectsWizardPage;
	
	public MergeSubjectsWizard() {
		setWindowTitle(DocometreMessages.MergeSubjectsDialogTitle);
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		mergeSubjectsWizardPage = new MergeSubjectsWizardPage();
		super.addPage(mergeSubjectsWizardPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// TODO Auto-generated method stub
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} 
		if(anErrorOccured) MessageDialog.openError(getShell(), DocometreMessages.MergeSubjectsErrorOccuredDialogTitle, DocometreMessages.MergeSubjectsErrorOccuredDialogMessage);
		return true;
	}

}
