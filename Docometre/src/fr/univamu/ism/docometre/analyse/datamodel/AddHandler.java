package fr.univamu.ism.docometre.analyse.datamodel;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.editors.DataProcessBatchEditor;

public class AddHandler extends SelectionAdapter {
	
	private final class ProcessesSubjectsLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if(element instanceof IResource) {
				if(ResourceType.isDataProcessing((IResource) element)) return ((IResource)element).getName().replaceAll(Activator.dataProcessingFileExtension, ""); 
				else return ((IResource)element).getName();
			}
			return super.getText(element);
		}
	}
	
	private final class AddOperation extends AbstractOperation {

		private IResource[] selection;
		private BatchDataProcessingItem[] addedBatchDataProcessing;

		public AddOperation(String label, IResource[] selection) {
			super(label);
			addContext(dataProcessBatchEditor.getUndoContext());
			this.selection = selection;
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			if(resourceType == ResourceType.PROCESS) {
				addedBatchDataProcessing = batchDataProcessing.addProcesses(selection);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				addedBatchDataProcessing = batchDataProcessing.addSubjects(selection);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			return execute(monitor, info);
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			if(resourceType == ResourceType.PROCESS) {
				batchDataProcessing.removeProcesses(addedBatchDataProcessing);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				batchDataProcessing.removeSubjects(addedBatchDataProcessing);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private DataProcessBatchEditor dataProcessBatchEditor;
	private IOperationHistory operationHistory;
	private BatchDataProcessing batchDataProcessing;
	private IResource resource;
	private IResource[] resources;
	private ResourceType resourceType;
	

	public AddHandler(DataProcessBatchEditor dataProcessBatchEditor, ResourceType resourceType) {
		this.dataProcessBatchEditor = dataProcessBatchEditor;
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		this.batchDataProcessing = dataProcessBatchEditor.getBatchDataProcessing();
		this.resource = ObjectsController.getResourceForObject(batchDataProcessing);
		this.resourceType = resourceType;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		try {
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(dataProcessBatchEditor.getSite().getShell());
			progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					AddHandler.this.resources = ResourceProperties.getAllTypedResources(resourceType, resource.getProject(), monitor);
				}
			});
			ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(dataProcessBatchEditor.getSite().getShell(), new ProcessesSubjectsLabelProvider());
			elementListSelectionDialog.setMultipleSelection(true);
			
			if(resourceType == ResourceType.PROCESS) {
				elementListSelectionDialog.setMessage(DocometreMessages.SelectProcessDialogMessage);
				elementListSelectionDialog.setTitle(DocometreMessages.SelectProcessDialogTitle);
			} else {
				elementListSelectionDialog.setMessage(DocometreMessages.SelectSubjectDialogMessage);
				elementListSelectionDialog.setTitle(DocometreMessages.SelectSubjectDialogTitle);
			}
			elementListSelectionDialog.setElements(AddHandler.this.resources);
			if(elementListSelectionDialog.open() == Dialog.OK) {
				IResource[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new IResource[elementListSelectionDialog.getResult().length]);
				if(resourceType == ResourceType.PROCESS) {
					operationHistory.execute(new AddOperation(DocometreMessages.AddProcessModifyOperationLabel, selection), null, null);
				} else {
					operationHistory.execute(new AddOperation(DocometreMessages.AddSubjectModifyOperationLabel, selection), null, null);
				}
				
			}
		} catch (InvocationTargetException | InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
			Activator.logErrorMessageWithCause(e1);
		}
	}

}
