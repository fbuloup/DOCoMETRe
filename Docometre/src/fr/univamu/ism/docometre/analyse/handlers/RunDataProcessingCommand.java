package fr.univamu.ism.docometre.analyse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditor;
import fr.univamu.ism.docometre.analyse.editors.DataProcessEditor;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class RunDataProcessingCommand extends AbstractHandler implements ISelectionListener {
	
	private Set<IResource>  selectedDataProcesses = new HashSet<IResource>(0);
	protected IResource[] modifiedSubjects;
	private Object object;
	private boolean cancel;

	public RunDataProcessingCommand() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logInfoMessage(DocometreMessages.PleaseStartMathEngineFirst, getClass());
			return null;
		}
		cancel = false;
		for (IResource dataProcessing : selectedDataProcesses) {
			boolean removeHandle = false;
			object = ResourceProperties.getObjectSessionProperty(dataProcessing);
			if(object == null) {
				object = ObjectsController.deserialize((IFile)dataProcessing);
				ResourceProperties.setObjectSessionProperty(dataProcessing, object);
				ObjectsController.addHandle(object);
				removeHandle = true;
			}
			if(object instanceof Script) {
				try {
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					pmd.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								String message = NLS.bind(DocometreMessages.RunningScriptLabel, dataProcessing.getFullPath().removeFileExtension().lastSegment());
								monitor.beginTask(message, IProgressMonitor.UNKNOWN);
								Script script = (Script)object;
								String code = script.getLoopCode(object, ScriptSegmentType.LOOP);
								MathEngineFactory.getMathEngine().runScript(code);
								monitor.done();
							} catch (Exception e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
							}
							
						}
					});
				} catch (InterruptedException | InvocationTargetException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(object instanceof BatchDataProcessing) {
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					pmd.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							String message = NLS.bind(DocometreMessages.RunningScriptLabel, dataProcessing.getFullPath().removeFileExtension().lastSegment());
							monitor.beginTask(message, IProgressMonitor.UNKNOWN);
							cancel = RunBatchDataProcessingDelegate.run((BatchDataProcessing) object, monitor);
							monitor.done();
						}
					});
				} catch (InterruptedException | InvocationTargetException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				} 
			}
			if(removeHandle) ObjectsController.removeHandle(object);
			modifiedSubjects = MathEngineFactory.getMathEngine().getCreatedOrModifiedSubjects();
			for (IResource modifiedSubject : modifiedSubjects) {
				ResourceProperties.setSubjectModified(modifiedSubject, true);
				ExperimentsView.refresh(modifiedSubject, null);
				SubjectsView.refresh(modifiedSubject, null);
			}
			if(modifiedSubjects.length > 0) {
				IEditorReference[] editorsRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (IEditorReference editorRef : editorsRefs) {
					IEditorPart editor = editorRef.getEditor(false);
					if(editor instanceof ChannelEditor) {
						((ChannelEditor)editor).update();
					}
				}
			}
			if(cancel) break;
		}

		// Get back potential error messages
		String errorMessages =  MathEngineFactory.getMathEngine().getErrorMessages();
		if(errorMessages != null) Activator.logErrorMessage(errorMessages);
		
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedDataProcesses.clear();
		if(!(selection instanceof StructuredSelection)) return;
		StructuredSelection structuredSelection = (StructuredSelection)selection;
		if(part instanceof SubjectsView) {
			for (Object element : structuredSelection) {
				if(element instanceof IResource) {
					IResource resource = (IResource)element;
					if(ResourceType.isDataProcessing(resource)) selectedDataProcesses.add(resource);
					if(ResourceType.isBatchDataProcessing(resource)) selectedDataProcesses.add(resource);
				}
			}
		}
		if(part instanceof BatchDataProcessingEditor || part instanceof DataProcessEditor) {
			ResourceEditorInput editorInput = (ResourceEditorInput) ((EditorPart) part).getEditorInput();
			IResource resource = ObjectsController.getResourceForObject(editorInput.getObject());
			if(ResourceType.isDataProcessing(resource)) selectedDataProcesses.add(resource);
			if(ResourceType.isBatchDataProcessing(resource)) selectedDataProcesses.add(resource);
		}
		setBaseEnabled(selectedDataProcesses.size() > 0);
	}

}
