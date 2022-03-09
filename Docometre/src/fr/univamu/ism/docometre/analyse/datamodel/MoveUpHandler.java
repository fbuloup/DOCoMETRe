/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

public class MoveUpHandler extends SelectionAdapter {
	
	private final class MoveUpOperation extends AbstractOperation {
		
		private BatchDataProcessingItem[] items;
		
		public MoveUpOperation(String label, BatchDataProcessingItem[] items) {
			super(label);
			this.items = items;
			addContext(dataProcessBatchEditor.getUndoContext());
		}
		
		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			if(resourceType == ResourceType.DATA_PROCESSING) {
				for (BatchDataProcessingItem item : items) dataProcessBatchEditor.getBatchDataProcessing().moveProcessUp(item);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				for (BatchDataProcessingItem item : items) dataProcessBatchEditor.getBatchDataProcessing().moveSubjectUp(item);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			execute(monitor, info);
			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			BatchDataProcessingItem[] newItems = new BatchDataProcessingItem[items.length];
			System.arraycopy(items, 0, newItems, 0, items.length);
			List<Object> list = Arrays.asList((Object[])newItems);
			Collections.reverse(list);
			BatchDataProcessingItem[] batchDataProcessingItems = list.toArray(new BatchDataProcessingItem[items.length]);
			if(resourceType == ResourceType.DATA_PROCESSING) {
				for (BatchDataProcessingItem item : batchDataProcessingItems) dataProcessBatchEditor.getBatchDataProcessing().moveProcessDown(item);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				for (BatchDataProcessingItem item : batchDataProcessingItems) dataProcessBatchEditor.getBatchDataProcessing().moveSubjectDown(item);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private BatchDataProcessingEditor dataProcessBatchEditor;
	private IOperationHistory operationHistory;
	private ResourceType resourceType;
	
	public MoveUpHandler(BatchDataProcessingEditor dataProcessBatchEditor, ResourceType resourceType) {
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
					if(dataProcessBatchEditor.getBatchDataProcessing().canMoveProcessesUp(selection)) 
						operationHistory.execute(new MoveUpOperation(DocometreMessages.MoveUpProcessModifyOperationLabel, selection), null, null);
				} else {
					if(dataProcessBatchEditor.getBatchDataProcessing().canMoveSubjectsUp(selection)) 
						operationHistory.execute(new MoveUpOperation(DocometreMessages.MoveUpSubjectModifyOperationLabel, selection), null, null);
				}
			} catch (ExecutionException e1) {
				Activator.logErrorMessageWithCause(e1);
				e1.printStackTrace();
			}
		}
	}

}
