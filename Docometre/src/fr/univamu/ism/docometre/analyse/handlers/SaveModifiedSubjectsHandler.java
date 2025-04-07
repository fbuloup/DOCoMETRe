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
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

@SuppressWarnings("restriction")
public class SaveModifiedSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private static String COMMAND_ID = "org.eclipse.ui.file.save";
	
	private static SaveModifiedSubjectsHandler saveModifiedSubjectsHandler;
	private ArrayList<IResource> modifiedSubjects = new ArrayList<>();

	private boolean cancel;
	
	
	public SaveModifiedSubjectsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart subjectsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
		if(subjectsView != null) selectionChanged(subjectsView, subjectsView.getSite().getSelectionProvider().getSelection());
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler = this; 
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		cancel = false;
		for (IResource subject : modifiedSubjects) {
			try {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				progressMonitorDialog.run(true, modifiedSubjects.size()>1, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
						Activator.logInfoMessage(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". ", SaveModifiedSubjectsHandler.this.getClass());
						MathEngineFactory.getMathEngine().saveSubject(subject);
						Activator.logInfoMessage(DocometreMessages.Done, SaveModifiedSubjectsHandler.this.getClass());
						cancel = monitor.isCanceled();
						monitor.done();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			if(cancel) break;
		}
		
		for (IResource subject : modifiedSubjects) {
			try {
				subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
				SubjectsView.refresh(subject, null);
				ExperimentsView.refresh(subject, null);
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		updateBaseEnabled();
		refreshCommand();
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		modifiedSubjects.clear();
		if(part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				for (Object element : elements) {
					if(element instanceof IResource && ResourceType.isSubject((IResource) element)) {
						IResource subject = (IResource)element;
						try {
							Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
							if(value != null) {
								boolean modified = (boolean)value;
								if(modified) modifiedSubjects.add(subject);
							}
						} catch (CoreException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
			}
		}
		updateBaseEnabled();
		refreshCommand();
	}
	
	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}
	
	private void updateBaseEnabled() {
		boolean enabled = false;
		for (IResource subject : modifiedSubjects) {
			try {
				Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
				if(value != null) {
					boolean modified = (boolean)value;
					enabled = enabled || modified;
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		setBaseEnabled(enabled);
	}
	
	private static void refreshCommand() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			    ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
			    if (commandService != null) commandService.refreshElements(COMMAND_ID, null);
			    ((WorkbenchWindow)window).getActionBars().updateActionBars();
			}
		});
		
	}
	
	public static void refresh() {
		if(SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler == null) return;
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler.updateBaseEnabled();
		refreshCommand();
	}

}
