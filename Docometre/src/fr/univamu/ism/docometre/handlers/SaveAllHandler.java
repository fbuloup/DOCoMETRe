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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

@SuppressWarnings("restriction")
public class SaveAllHandler extends AbstractHandler {
	
	private IWorkbenchPart activePart;
	private IEditorPart[] dirtyEditors;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
							}
						});
						// Active part is an editor
						if(activePart instanceof IEditorPart) {
							PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
								@Override
								public void run() {
									dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
								}
							});
							monitor.beginTask(DocometreMessages.SaveAllJobEditorsTaskName, dirtyEditors.length);
							for (IEditorPart dirtyEditor : dirtyEditors) {
								PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
									@Override
									public void run() {
										dirtyEditor.doSave(monitor);
									}
								});
								monitor.worked(1);
								if(monitor.isCanceled()) break;
							}
						}
						// Active part is SubjectsView or ExperimentsView
						if(activePart instanceof SubjectsView || activePart instanceof ExperimentsView) {
							IResource experiment = SelectedExprimentContributionItem.selectedExperiment;
							String[] loadedSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
							int nbModifiedSubjects = 0;
							for (String loadedSubject : loadedSubjects) {
								IResource subject = ((IContainer)experiment).findMember(loadedSubject.split("\\.")[1]);
								if(ResourceProperties.isSubjectModified(subject)) nbModifiedSubjects++;
							}
							monitor.beginTask(DocometreMessages.SaveAllJobSubjectsTaskName, nbModifiedSubjects);
							for (String loadedSubject : loadedSubjects) {
								IResource subject = ((IContainer)experiment).findMember(loadedSubject.split("\\.")[1]);
								if(ResourceProperties.isSubjectModified(subject)) {
									monitor.subTask(subject.getFullPath().toPortableString());
									MathEngineFactory.getMathEngine().saveSubject(subject);
									subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
									SubjectsView.refresh(subject, null);
									ExperimentsView.refresh(subject, null);
									Activator.logInfoMessage(DocometreMessages.Done, SaveAllHandler.this.getClass());
									monitor.worked(1);
								}
								if(monitor.isCanceled()) break;
							}
						}
						monitor.done();
						refreshCommand();
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
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		boolean enabled = false;
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if(activePart instanceof IEditorPart) {
			IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
			enabled = dirtyEditors.length > 0;
		}
		if(activePart instanceof SubjectsView || activePart instanceof ExperimentsView) {
			IResource experiment = SelectedExprimentContributionItem.selectedExperiment;
			String[] loadedSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
			for (String loadedSubject : loadedSubjects) {
				IResource subject = ((IContainer)experiment).findMember(loadedSubject.split("\\.")[1]);
				enabled = enabled || ResourceProperties.isSubjectModified(subject);
			}
		}
		return enabled;
	}
	
	private static void refreshCommand() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			    ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
			    if (commandService != null) commandService.refreshElements("SaveAllCommand", null);
			    ((WorkbenchWindow)window).getActionBars().updateActionBars();
			}
		});
		
	}

}
