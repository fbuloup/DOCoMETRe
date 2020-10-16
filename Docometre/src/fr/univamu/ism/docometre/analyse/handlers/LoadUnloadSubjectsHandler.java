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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditor;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class LoadUnloadSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private Set<IResource> selectedSubjects = new HashSet<IResource>(0);
	private boolean cancel;
	private static LoadUnloadSubjectsHandler loadUnloadSubjectsHandler;

	public static LoadUnloadSubjectsHandler getInstance() {
		return loadUnloadSubjectsHandler;
	}
	
	public LoadUnloadSubjectsHandler() {
		if(loadUnloadSubjectsHandler == null) {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
			loadUnloadSubjectsHandler = this;
		}
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
				if(ResourceProperties.isSubjectModified(subject)) {
					String message = NLS.bind(DocometreMessages.RecordSubjectDialogMessage, loadUnloadName);
					boolean response = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.RecordSubjectDialogTitle, message);
					if(response) {
						ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
						try {
							progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									monitor.beginTask(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
									Activator.logInfoMessage(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". ", LoadUnloadSubjectsHandler.this.getClass());
									MathEngineFactory.getMathEngine().saveSubject(subject);
									Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
									monitor.done();
								}
							});
						} catch (InvocationTargetException | InterruptedException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
				IEditorReference[] editorsReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (IEditorReference editorReference : editorsReferences) {
					if(editorReference.getId().equals(ChannelEditor.ID)) {
						try {
							Object object = ((ResourceEditorInput)editorReference.getEditorInput()).getObject();
							Channel channel = (Channel)object;
							if(channel.getParent().equals(subject)) {
								editorReference.getEditor(false).getSite().getPage().closeEditor(editorReference.getEditor(false), true);
							}
						} catch (PartInitException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
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
								subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, null);
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
							MathEngineFactory.getMathEngine().load(subject, false);
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
	
	public void resetSelection(Set<IResource> selectedSubjects) {
		this.selectedSubjects = selectedSubjects;
	}
	
}
