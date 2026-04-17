package fr.univamu.ism.docometre.analyse.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;

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
		if(!MathEngineFactory.getMathEngine().isStarted()) return true;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					String[] fromSubjects = mergeSubjectsWizardPage.getSelectedSubjectFrom();
					String[] toSubjects = mergeSubjectsWizardPage.getSelectedSubjectTo();
					monitor.beginTask(DocometreMessages.Merge, fromSubjects.length - 1);
					for (int i = 0; i < fromSubjects.length; i++) {
						String message = NLS.bind(DocometreMessages.MergeIn, fromSubjects[i], toSubjects[i]);
						monitor.subTask(message);
						if(fromSubjects[i].equals(toSubjects[i])) {
							message = NLS.bind(DocometreMessages.ImpossibleToMergeSubjectWithItself, fromSubjects[i]);
							Activator.logErrorMessage(message);
							continue;
						}
						IResource fromSubject = ResourcesPlugin.getWorkspace().getRoot().findMember(fromSubjects[i].replaceAll("\\.", "/"));
						IResource toSubject = ResourcesPlugin.getWorkspace().getRoot().findMember(toSubjects[i].replaceAll("\\.", "/"));
						Channel[] fromChannels = MathEngineFactory.getMathEngine().getSignals(fromSubject);
						Channel[] toChannels = MathEngineFactory.getMathEngine().getSignals(toSubject);
						boolean error = false;
						for (Channel fromChannel : fromChannels) {
							for (Channel toChannel : toChannels) {
								if(fromChannel.getName().equals(toChannel.getName())) {
									error = true;
									message = NLS.bind(DocometreMessages.AChannelWithThisNameAlreadyExist, fromChannel.getName(), toSubjects[i]);
									Activator.logErrorMessage(message);
									message = NLS.bind(DocometreMessages.ImpossibleToMerge, fromSubjects[i], toSubjects[i]);
									Activator.logErrorMessage(message);
									
								}
								if(error) break;
							}
							if(error) break;
						}
						
						int fromNbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(fromChannels[0]);
						int toNbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(toChannels[0]);
						if(fromNbTrials != toNbTrials) {
							error = true;
							Activator.logErrorMessage(DocometreMessages.NbTrialsNotEquals);
							message = NLS.bind(DocometreMessages.ImpossibleToMerge, fromSubjects[i], toSubjects[i]);
							Activator.logErrorMessage(message);
						}
						if(error) {
							monitor.worked(1);
							continue;
						}
						
						MathEngineFactory.getMathEngine().mergeSubject(fromSubjects[i], toSubjects[i]);
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								IViewPart subjectView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
								StructuredSelection selection = new StructuredSelection(toSubject);
								ApplicationActionBarAdvisor.refreshResourceAction.selectionChanged(subjectView, selection);
								ApplicationActionBarAdvisor.refreshResourceAction.run();
							}
						});
						message = NLS.bind(DocometreMessages.MergeDone, fromSubjects[i], toSubjects[i]);
						Activator.logInfoMessage(message, MergeSubjectsWizard.class);
						monitor.worked(1);
						if(monitor.isCanceled()) break;
					}
					monitor.done();
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
