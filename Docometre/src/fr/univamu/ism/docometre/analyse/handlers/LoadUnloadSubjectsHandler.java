package fr.univamu.ism.docometre.analyse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class LoadUnloadSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private Set<IResource> selectedSubjects = new HashSet<IResource>(0);
	private boolean cancel;

	public LoadUnloadSubjectsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logInfoMessage(DocometreMessages.PleaseStartMathEngineFirst, getClass());
			return null;
		}
		cancel = false;
		for (IResource subject : selectedSubjects) {
			boolean loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subject);
			String loadUnloadName = subject.getFullPath().segment(0) + "." + subject.getFullPath().segment(1);
			if(loaded) {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					progressMonitorDialog.run(true, selectedSubjects.size()>1, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								monitor.beginTask(DocometreMessages.UnloadingSubject + "\"" + loadUnloadName + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
								Activator.logInfoMessage(DocometreMessages.UnloadingSubject + "\"" + loadUnloadName + "\". ", LoadUnloadSubjectsHandler.this.getClass());
								MathEngineFactory.getMathEngine().unload(subject);
								Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
								cancel = monitor.isCanceled();
								monitor.done();
								subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
							} catch (CoreException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
							}
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					progressMonitorDialog.run(true, selectedSubjects.size()>1, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(DocometreMessages.LoadingSubject+ "\"" + loadUnloadName + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
							Activator.logInfoMessage(DocometreMessages.LoadingSubject + "\"" + loadUnloadName + "\". ", LoadUnloadSubjectsHandler.this.getClass());
							MathEngineFactory.getMathEngine().load(subject);
							Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
							cancel = monitor.isCanceled();
							monitor.done();
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(cancel) break;
		}
		for (IResource subject : selectedSubjects) {
			ExperimentsView.refresh(subject, null);
			SubjectsView.refresh(subject, null);
		}
		SaveModifiedSubjectsHandler.refresh();
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedSubjects.clear();
		if(!(selection instanceof StructuredSelection)) return;
		StructuredSelection structuredSelection = (StructuredSelection)selection;
		for (Object element : structuredSelection) {
			if(element instanceof IResource) {
				IResource resource = (IResource)element;
				if(ResourceType.isSubject(resource)) selectedSubjects.add(resource);
			}
		}
		setBaseEnabled(selectedSubjects.size() > 0);
	}
	
}
