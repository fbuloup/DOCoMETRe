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
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;

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
			if(resourceType == ResourceType.DATA_PROCESSING) {
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
			if(resourceType == ResourceType.DATA_PROCESSING) {
				batchDataProcessing.removeProcesses(addedBatchDataProcessing);
				dataProcessBatchEditor.refreshProcesses();
			} else {
				batchDataProcessing.removeSubjects(addedBatchDataProcessing);
				dataProcessBatchEditor.refreshSubjects();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private BatchDataProcessingEditor dataProcessBatchEditor;
	private IOperationHistory operationHistory;
	private BatchDataProcessing batchDataProcessing;
	private IResource resource;
	private IResource[] resources;
	private ResourceType resourceType;
	

	public AddHandler(BatchDataProcessingEditor dataProcessBatchEditor, ResourceType resourceType) {
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
			
			if(resourceType == ResourceType.DATA_PROCESSING) {
				elementListSelectionDialog.setMessage(DocometreMessages.SelectProcessDialogMessage);
				elementListSelectionDialog.setTitle(DocometreMessages.SelectProcessDialogTitle);
			} else {
				elementListSelectionDialog.setMessage(DocometreMessages.SelectSubjectDialogMessage);
				elementListSelectionDialog.setTitle(DocometreMessages.SelectSubjectDialogTitle);
			}
			elementListSelectionDialog.setElements(AddHandler.this.resources);
			if(elementListSelectionDialog.open() == Dialog.OK) {
				IResource[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new IResource[elementListSelectionDialog.getResult().length]);
				if(resourceType == ResourceType.DATA_PROCESSING) {
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
