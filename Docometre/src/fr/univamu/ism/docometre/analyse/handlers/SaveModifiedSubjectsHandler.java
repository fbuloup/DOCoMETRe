package fr.univamu.ism.docometre.analyse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

@SuppressWarnings("restriction")
public class SaveModifiedSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private static String COMMAND_ID = "org.eclipse.ui.file.save";
	
	private static SaveModifiedSubjectsHandler saveModifiedSubjectsHandler;
	private ArrayList<IResource> modifiedSubjects = new ArrayList<>();

	private boolean cancel;
	
	
	public SaveModifiedSubjectsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart subjectsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
		if(subjectsView != null) selectionChanged(subjectsView, subjectsView.getSite().getSelectionProvider().getSelection());
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler = this; 
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		cancel = false;
		for (IResource subject : modifiedSubjects) {
			try {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				progressMonitorDialog.run(true, modifiedSubjects.size()>1, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
						Activator.logInfoMessage(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". ", SaveModifiedSubjectsHandler.this.getClass());
						MathEngineFactory.getMathEngine().saveSubject(subject);
						Activator.logInfoMessage(DocometreMessages.Done, SaveModifiedSubjectsHandler.this.getClass());
						cancel = monitor.isCanceled();
						monitor.done();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			if(cancel) break;
		}
		
		for (IResource subject : modifiedSubjects) {
			try {
				subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
				SubjectsView.refresh(subject, null);
				ExperimentsView.refresh(subject, null);
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		updateBaseEnabled();
		refreshCommand();
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		modifiedSubjects.clear();
		if(part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				for (Object element : elements) {
					if(element instanceof IResource && ResourceType.isSubject((IResource) element)) {
						IResource subject = (IResource)element;
						try {
							Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
							if(value != null) {
								boolean modified = (boolean)value;
								if(modified) modifiedSubjects.add(subject);
							}
						} catch (CoreException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
			}
		}
		updateBaseEnabled();
		refreshCommand();
	}
	
	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}
	
	private void updateBaseEnabled() {
		boolean enabled = false;
		for (IResource subject : modifiedSubjects) {
			try {
				Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
				if(value != null) {
					boolean modified = (boolean)value;
					enabled = enabled || modified;
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		setBaseEnabled(enabled);
	}
	
	private static void refreshCommand() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			    ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
			    if (commandService != null) commandService.refreshElements(COMMAND_ID, null);
			    ((WorkbenchWindow)window).getActionBars().updateActionBars();
			}
		});
		
	}
	
	public static void refresh() {
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler.updateBaseEnabled();
		refreshCommand();
	}

}
