package fr.univamu.ism.docometre.analyse.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

public class ExportDataWizard extends Wizard {
	
	private ExportDataWizardPage exportDataWizardPage;
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		exportDataWizardPage = new ExportDataWizardPage();
		super.addPage(exportDataWizardPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
