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
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;
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
								UpdateWorkbenchDelegate.update();
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
							UpdateWorkbenchDelegate.update();
						}
					});
				} catch (InterruptedException | InvocationTargetException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				} 
			}
			if(removeHandle) ObjectsController.removeHandle(object);
			if(cancel) break;
		}

		// Get back potential error messages
		String errorMessages =  MathEngineFactory.getMathEngine().getErrorMessages();
		if(errorMessages != null) Activator.logErrorMessage(errorMessages);
		ExperimentsView.refresh(SelectedExprimentContributionItem.selectedExperiment, null);
		SubjectsView.refresh(SelectedExprimentContributionItem.selectedExperiment, null);
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
