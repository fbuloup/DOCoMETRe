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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.menus.UIElement;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.MathEngineListener;
import fr.univamu.ism.docometre.analyse.handlers.LoadUnloadSubjectsHandler;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;

@SuppressWarnings("restriction")
public class StartStopMathEngineHandler extends AbstractHandler implements IElementUpdater, MathEngineListener {
	
	private static String COMMAND_ID = "StartStopMathEngineCommand";
	
	private static StartStopMathEngineHandler startStopMathEngineHandler;
	
	public static StartStopMathEngineHandler getInstance() {
		return startStopMathEngineHandler;
	}
	
	private boolean cancelModified;
	
	public StartStopMathEngineHandler() {
		if(startStopMathEngineHandler == null) {
			startStopMathEngineHandler = this;
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		if (mathEngine.isStarted()) {
			element.setIcon(Activator.getImageDescriptor(IImageKeys.STOP_ICON));
			element.setTooltip(DocometreMessages.StopMathEngine_Tooltip);
		} else {
			element.setIcon(Activator.getImageDescriptor(IImageKeys.RUN_ICON));
			element.setTooltip(DocometreMessages.StartMathEngine_Tooltip);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job startStopMathEngineJob = new Job(DocometreMessages.MathEngineStartStop) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setBaseEnabled(false);
				refreshCommand();
				IStatus response;
				MathEngine mathEngine = MathEngineFactory.getMathEngine();
				mathEngine.addListener(StartStopMathEngineHandler.this);
				if (!mathEngine.isStarted()) {
					response = mathEngine.startEngine(monitor);
				} else {
					// Unload subjects :
					// Get all loaded subjects
					Set<IResource> loadedSubjects = new HashSet<IResource>(0);
					IProject[] experiments = ResourcesPlugin.getWorkspace().getRoot().getProjects();
					for (IProject experiment : experiments) {
						try {
							IResource[] members = experiment.members();
							for (IResource member : members) {
								if(ResourceType.isSubject(member) && MathEngineFactory.getMathEngine().isSubjectLoaded((IResource) member))
									loadedSubjects.add(member);
							}
						} catch (CoreException e) {
							loadedSubjects.clear();
							setBaseEnabled(true);
							Activator.getLogErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
					// Set selection in LoadUnloadSubjectsHandler
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							try {
								LoadUnloadSubjectsHandler.getInstance().resetSelection(loadedSubjects);
								// Launch LoadUnloadSubjectsHandler
								cancelModified = (boolean) LoadUnloadSubjectsHandler.getInstance().execute(new ExecutionEvent());
							} catch (ExecutionException e) {
								Activator.getLogErrorMessageWithCause(e);
								e.printStackTrace();
							}
							
						}
					});
					if(!cancelModified) {
						response = mathEngine.stopEngine(monitor);
//						MathEngineFactory.clear();
					} else response = Status.CANCEL_STATUS;
					
				}
				setBaseEnabled(true);
				SubjectsView.refresh(null, null);
				return response;
			}
		};
		
		startStopMathEngineJob.setUser(true);
		startStopMathEngineJob.schedule();
		return null;
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

	@Override
	public void notifyListener() {
		refreshCommand();
	}
	
}
