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

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.menus.UIElement;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DesignPerspective;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dialogs.StopTrialDialog;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;

@SuppressWarnings("restriction")
public class RunStopHandler extends AbstractHandler implements IElementUpdater, IPerspectiveListener {
	
	private static String COMMAND_ID = "RunStopCommand";
	
	private static boolean enabled;
	private static boolean stop = false;
	
	
	public RunStopHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		if (ExperimentScheduler.getInstance().isRunning()) {
			element.setIcon(Activator.getImageDescriptor(IImageKeys.STOP_ICON));
			element.setTooltip(DocometreMessages.StopAction_Tooltip);
		} else {
			element.setIcon(Activator.getImageDescriptor(IImageKeys.RUN_ICON));
			element.setTooltip(DocometreMessages.RunAction_Tooltip);
		}
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.removePerspectiveListener(this);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Exit handler if resource is null
		if(ExperimentsView.currentSelectedResource == null) return null;
		// Run Current process if not running
		if(!ExperimentScheduler.getInstance().isRunning()) {
			start();
			refreshCommand();
		}
		else stop();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
//	public static void setEnabled(boolean enabled) {
//		RunStopHandler.enabled = enabled;
//	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
	

	private void start() {
		// Run experiment scheduler
		ExperimentScheduler.getInstance().run();
	}
	
	public static void stop() {
		boolean saveChoice = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW);
		stop = false;
		if(!saveChoice) 
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					StopTrialDialog stopTrialDialog = new StopTrialDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
					if(stopTrialDialog.open() == Window.OK) stop = true;
				}
			});
		else stop = true;
		boolean stopNow = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.STOP_TRIAL_NOW);
		if(!stopNow && stop) {
			enabled = false;
			refreshCommand();
			ApplicationActionBarAdvisor.pausePendingContributionItem.setText(DocometreMessages.PausePending);
		}
		if(stop) ExperimentScheduler.getInstance().stop(false);
	}
	
	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		if(perspective.getId().equals(DesignPerspective.ID) && ExperimentScheduler.getInstance().isRunning()) stop();
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub
	}
	
	public static void refresh() {
		enabled = false;
		IResource resource = ExperimentsView.currentSelectedResource;
		if(resource != null) {
			enabled = ResourceType.isProcess(resource);
			enabled = enabled || ResourceType.isSubject(resource);
			enabled = enabled || ResourceType.isSession(resource);
			if(ResourceType.isTrial(resource)) enabled = ResourceProperties.getAssociatedProcess(resource) != null;
		}
		refreshCommand();
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
 
}
