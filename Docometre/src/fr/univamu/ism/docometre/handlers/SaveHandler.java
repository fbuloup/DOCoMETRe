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
package fr.univamu.ism.docometre.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.progress.WorkbenchJob;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

@SuppressWarnings("restriction")
public class SaveHandler extends AbstractHandler {
	
	private ArrayList<Object> selectedElements;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WorkbenchJob workbenchJob = new WorkbenchJob("Saving dirty editor.") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					for (Object element : selectedElements) {
						if(element instanceof IEditorPart) {
							IEditorPart dirtyEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
							monitor.beginTask("Please wait...", 1);
							dirtyEditor.doSave(monitor);
							monitor.done();
						}
						if(element instanceof IResource) {
							IResource subject = (IResource)element;
							monitor.beginTask(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
							Activator.logInfoMessage(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". ", SaveHandler.this.getClass());
							MathEngineFactory.getMathEngine().saveSubject(subject);
							subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
							SubjectsView.refresh(subject, null);
							ExperimentsView.refresh(subject, null);
							Activator.logInfoMessage(DocometreMessages.Done, SaveHandler.this.getClass());
							monitor.done();
						}
						if(monitor.isCanceled()) break;
					}
					
					// Check if there are still unsaved elements (previous monitor canceled)
					boolean enabled = false;
					for (Object element : selectedElements) {
							if(element instanceof IResource) {
								IResource subject = (IResource)element;
								boolean isLoaded = MathEngineFactory.getMathEngine().isSubjectLoaded((IResource) element);
								if(isLoaded) enabled = enabled || ResourceProperties.isSubjectModified(subject);
							}
							if(element instanceof IEditorPart) {
								IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
								enabled = enabled || editor.isDirty();
							}
					}
					setBaseEnabled(enabled);
					
					// Update toolbar
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
					if (commandService != null) commandService.refreshElements("SaveCommand", null);
					((WorkbenchWindow)window).getActionBars().updateActionBars();
					
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
				
				return Status.OK_STATUS;
			}
		};
		workbenchJob.schedule();
		
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		if(selectedElements == null) selectedElements = new ArrayList<>();
		selectedElements.clear();
		
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if(selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection)selection;
			Object[] elements = structuredSelection.toArray();
			for (Object element : elements) {
				if(element instanceof IResource) {
					if(ResourceType.isSubject((IResource)element)) {
						boolean isLoaded = MathEngineFactory.getMathEngine().isSubjectLoaded((IResource) element);
						if(isLoaded) {
							boolean isModified = ResourceProperties.isSubjectModified((IResource)element);
							if(isModified) selectedElements.add(element);
						}
					}
				}
			}
		}
		
		if(selectedElements.size() > 0) return true;
		
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if(editor != null && editor.isDirty()) selectedElements.add(editor);
		return selectedElements.size() > 0;
	}

}
