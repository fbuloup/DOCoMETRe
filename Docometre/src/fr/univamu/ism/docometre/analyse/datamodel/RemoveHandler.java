package fr.univamu.ism.docometre.analyse.datamodel;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.editors.DataProcessBatchEditor;

public class RemoveHandler extends SelectionAdapter {
	
	private final class RemoveOperation extends AbstractOperation {

		private BatchDataProcessingItem[] items;
		private int[] indexes;

		public RemoveOperation(String label, BatchDataProcessingItem[] items) {
			super(label);
			this.items = items;
			addContext(dataProcessBatchEditor.getUndoContext());
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			if(resourceType == ResourceType.PROCESS) {
				indexes = dataProcessBatchEditor.getBatchDataProcessing().removeProcesses(items);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				indexes = dataProcessBatchEditor.getBatchDataProcessing().removeSubjects(items);
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
				dataProcessBatchEditor.getBatchDataProcessing().addProcesses(items, indexes);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				dataProcessBatchEditor.getBatchDataProcessing().addSubjects(items, indexes);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}
		
	}

	private DataProcessBatchEditor dataProcessBatchEditor;
	private IOperationHistory operationHistory;
	private ResourceType resourceType;
	
	public RemoveHandler(DataProcessBatchEditor dataProcessBatchEditor, ResourceType resourceType) {
		this.dataProcessBatchEditor = dataProcessBatchEditor;
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		this.resourceType = resourceType;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		BatchDataProcessingItem[] selection = dataProcessBatchEditor.getSelectedProcesses();
		if(resourceType == ResourceType.SUBJECT) selection = dataProcessBatchEditor.getSelectedSubjects();
		if(selection.length > 0) {
			try {
				if(resourceType == ResourceType.PROCESS) {
					operationHistory.execute(new RemoveOperation(DocometreMessages.RemoveProcessModifyOperationLabel, selection), null, null);
				} else {
					operationHistory.execute(new RemoveOperation(DocometreMessages.RemoveSubjectModifyOperationLabel, selection), null, null);
				}
			} catch (ExecutionException e1) {
				Activator.logErrorMessageWithCause(e1);
				e1.printStackTrace();
			}
		}
	}

}
