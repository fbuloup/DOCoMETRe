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
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;

public class EnableDisableHandler extends SelectionAdapter {
	
	private final class ActivateDeactivateOperation extends AbstractOperation {
		
		private BatchDataProcessingItem[] items;
		
		public ActivateDeactivateOperation(String label, BatchDataProcessingItem[] items) {
			super(label);
			this.items = items;
			addContext(dataProcessBatchEditor.getUndoContext());
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			for (BatchDataProcessingItem item : items) {
				item.setActivated(!item.isActivated());
			}
			if(resourceType == ResourceType.DATA_PROCESSING) dataProcessBatchEditor.refreshProcesses();
			else dataProcessBatchEditor.refreshSubjects();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			return execute(monitor,info);
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			return execute(monitor,info);
		}
		
	}
	
	private BatchDataProcessingEditor dataProcessBatchEditor;
	private IOperationHistory operationHistory;
	private ResourceType resourceType;
	
	public EnableDisableHandler(BatchDataProcessingEditor dataProcessBatchEditor, ResourceType resourceType) {
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
				if(resourceType == ResourceType.DATA_PROCESSING) {
					operationHistory.execute(new ActivateDeactivateOperation(DocometreMessages.EnableDisableProcessModifyOperationLabel, selection), null, null);
				} else {
					operationHistory.execute(new ActivateDeactivateOperation(DocometreMessages.EnableDisableSubjectModifyOperationLabel, selection), null, null);
				}
			} catch (ExecutionException e1) {
				Activator.logErrorMessageWithCause(e1);
				e1.printStackTrace();
			}
		}
	}

}
