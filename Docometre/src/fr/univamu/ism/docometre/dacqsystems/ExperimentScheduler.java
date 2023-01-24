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
package fr.univamu.ism.docometre.dacqsystems;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.TrialStartMode;
import fr.univamu.ism.docometre.dialogs.NextTrialDialog;
import fr.univamu.ism.docometre.dialogs.ValidateTrialDialog;
import fr.univamu.ism.docometre.handlers.RunStopHandler;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.docometre.views.RealTimeChartsView;

public class ExperimentScheduler {

	public static String dataFilePathNameSeparator = ".";
	public static String dataFilePathNameSeparator_RegExpSplitter = "\\.";
	
	private RunProcessJob runProcessJob;
	
	private IResource selectedResource;
	private IResource currentSession;
	private IResource currentTrial;
	private  boolean runProcess;
	private boolean running;
	
	private class RunProcessJob extends Job {
		
		private IResource processFile;
		private IResource trial;
		private Process process;
		private boolean stop;

		public RunProcessJob(IResource processFile, IResource trial) {
			super("Scheduling : " + processFile.getName());
			this.processFile = processFile;
			this.trial = trial;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IStatus returnStatus = Status.OK_STATUS;
			boolean removeProcessHandle = false;
			Object object = ResourceProperties.getObjectSessionProperty(processFile);
			if(object == null) {
				object = ObjectsController.deserialize((IFile) processFile);
				ResourceProperties.setObjectSessionProperty(processFile, object);
				ObjectsController.addHandle(object);
				removeProcessHandle = true;
			}
			process = (Process) object;
			try {
				process.preExecute((trial==null)?processFile:trial);
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						RealTimeChartsView realTimeChartsView = (RealTimeChartsView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RealTimeChartsView.ID);
						if(realTimeChartsView != null) realTimeChartsView.updateCharts(process);
					}
				});
				
				// Manage suffix data files names
				String prefix = "";
				String suffix = "";
				if(currentSession != null && currentTrial != null) {
					prefix = ResourceProperties.getDataFilesNamesPrefix(currentSession);
					if(prefix == null) prefix= "";
					else prefix = prefix + dataFilePathNameSeparator;
					if(ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(currentSession)) suffix = dataFilePathNameSeparator + currentSession.getName();
					if(ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(currentSession)) suffix = suffix  + dataFilePathNameSeparator + "T" + currentTrial.getName().split("°")[1];
				}
				
				int priority = Thread.currentThread().getPriority();
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
				
				Job realTimeLoopJob = process.execute(prefix, suffix);
				realTimeLoopJob.schedule();
				boolean timeOut = false;
				long startTime = System.currentTimeMillis();
				while(realTimeLoopJob.getState() != Job.RUNNING || timeOut) {
					timeOut = (System.currentTimeMillis() - startTime > 1000);
				};
				if(timeOut) Activator.logErrorMessage("Time out on scheduling realTimeLoopJob");
				while(realTimeLoopJob.getState() == Job.RUNNING) {
					if(stop) {
						process.stop();
						returnStatus = new Status(IStatus.CANCEL, Activator.PLUGIN_ID, null);
					}
//					if(monitor.isCanceled()) { CANNOT CANCEL THIS JOB - ONLY Real time Job can be canceled
//						stop = true;
//						monitor.setCanceled(false);
//					}
				}
				Thread.currentThread().setPriority(priority);
				process.postExecute((trial==null)?processFile:trial);
//				process.refreshLogFile((trial==null)?processFile:trial);
				
			} catch (Exception e) {
				e.printStackTrace();
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Activator.getLogErrorMessageWithCause(e));
			} finally {
				if(removeProcessHandle) {
					ObjectsController.removeHandle(process.getDACQConfiguration());
					ObjectsController.removeHandle(process);
				}
			}
			return returnStatus;
		}
		
		public void stop() {
			stop = true;
		}
		
	}
	
	class SeekNextProcess extends Job {
		
		private boolean redoTrial;
		private boolean nextTrial;

		public SeekNextProcess(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			while(running) {
				IResource process = selectedResource;
				if(!runProcess && currentTrial == null) running = false;
				if(!runProcess && running) process = ResourceProperties.getAssociatedProcess(currentTrial);
				if(process == null) {
					running = false;
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							String message = NLS.bind(DocometreMessages.NoAssociatedProcess, currentTrial.getLocation().toOSString());
							MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.Error, message);
							MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.Information, DocometreMessages.SchedulerStopped);
						}
					});
				}
				if(running) {
					try {
						runProcessJob = new RunProcessJob(process, currentTrial);
						runProcessJob.schedule();
						runProcessJob.join();
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								ApplicationActionBarAdvisor.pausePendingContributionItem.setText("");
							}
						});
						IStatus status = runProcessJob.getResult();
						if(status.isOK()) if(!runProcess) {
							boolean autoValidateTrial =Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.AUTO_VALIDATE_TRIALS);
							if(autoValidateTrial) ResourceProperties.setTrialState((IFolder) currentTrial, true);
							else {
								PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
									@Override
									public void run() {
										ValidateTrialDialog validateTrialDialog = new ValidateTrialDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), currentTrial);
										if(validateTrialDialog.open() == Window.OK) {
											ResourceProperties.setTrialState( (IFolder) currentTrial, validateTrialDialog.getValidateTrial());
											redoTrial = validateTrialDialog.getRedoTrial();
										}
										
									}
								});
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					} finally {
						if(!runProcess) {
							if(!redoTrial) {
								nextTrial = true;
								boolean autoStartTrial = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.AUTO_START_TRIALS);
								
								nextTrial();
								if(running) autoStartTrial = autoStartTrial || ResourceProperties.getTrialStartMode(currentTrial).equals(TrialStartMode.AUTO);
								
								if(!autoStartTrial && running) {
									PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
										@Override
										public void run() {
											NextTrialDialog nextTrialDialog = new NextTrialDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
											if(nextTrialDialog.open() == IDialogConstants.YES_ID) nextTrial = true;
											else nextTrial = false;
										}
									});
								}
								if(!nextTrial) running = false;
							}
							else running = true;
						} else running = false;
					}
				}
			}
			RunStopHandler.refresh();
			return Status.OK_STATUS;
		}
		
	}
	
	private static ExperimentScheduler experimentScheduler;
	
	public static ExperimentScheduler getInstance() {
		if(experimentScheduler == null) experimentScheduler = new ExperimentScheduler();
		return experimentScheduler;
	}
	
	private ExperimentScheduler() {
		
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void stop(boolean pause) {
		if(!pause) {
			boolean stopNow = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.STOP_TRIAL_NOW);
			if(stopNow) runProcessJob.stop();
		}
		running = false;
	}
	
	public void run() {
		running = true;
		SeekNextProcess seekNextProcess = new SeekNextProcess(DocometreMessages.RunningTrials);
		seekNextProcess.schedule();
	}

	public void initialize() {
		selectedResource = ExperimentsView.currentSelectedResource;
//		selectedResource = ApplicationActionBarAdvisor.runStopAction.getSelectedResource();
		
		RealTimeChartsView realTimeChartsView = (RealTimeChartsView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RealTimeChartsView.ID);
		if(realTimeChartsView != null) realTimeChartsView.updateValues(null);

		currentTrial = null;
		currentSession = null;
		runProcess = false;
		
		if(selectedResource == null || !(selectedResource instanceof IResource)) return;
		if(realTimeChartsView != null) realTimeChartsView.updateValues(selectedResource);
		if(ResourceType.isProcess(selectedResource)) {
			runProcess = true;
			currentTrial = null;
			currentSession = null;
		} else if(ResourceType.isTrial(selectedResource)) {
			if(realTimeChartsView != null) {
				realTimeChartsView.updateValues(selectedResource.getParent());
				realTimeChartsView.updateValues(selectedResource.getParent().getParent());
				realTimeChartsView.updateValues(selectedResource.getParent().getParent().getParent());
			}
			IResource processFile = ResourceProperties.getAssociatedProcess(selectedResource);
			if(processFile != null && realTimeChartsView != null) realTimeChartsView.updateValues(processFile);
			currentTrial = selectedResource;
			currentSession = selectedResource.getParent();
		} else if(ResourceType.isSession(selectedResource)) {
			if(realTimeChartsView != null) {
				realTimeChartsView.updateValues(selectedResource.getParent());
				realTimeChartsView.updateValues(selectedResource.getParent().getParent());
			}
			
			IResource trial = getFirstRunnableUndoneTrial(selectedResource);
			if(trial != null) {
				realTimeChartsView.updateValues(trial);
				realTimeChartsView.updateValues(ResourceProperties.getAssociatedProcess(trial));
			}
			currentTrial = trial;			
			currentSession = selectedResource;
		} else if(ResourceType.isSubject(selectedResource)) {
			realTimeChartsView.updateValues(selectedResource.getParent());
			IResource trial = getFirstRunnableUndoneTrial(selectedResource);
			currentTrial = trial;
			if(trial != null && realTimeChartsView != null) {
				realTimeChartsView.updateValues(trial);
				realTimeChartsView.updateValues(trial.getParent());
				currentSession = trial.getParent();
				realTimeChartsView.updateValues(ResourceProperties.getAssociatedProcess(trial));
			}
		} 
	}
	
	private IResource getTrial(IResource trial, boolean next) {
		String name = trial.getName();
		int currentTrialNumber = Integer.parseInt(name.split("°")[1]);
		int nextTrialNumber = next?currentTrialNumber + 1:currentTrialNumber - 1;
		IResource nextTrial = ((IFolder)currentSession).findMember(DocometreMessages.Trial + nextTrialNumber);
		while(nextTrial != null && ResourceProperties.isTrialDone((IFolder) nextTrial)) {
			nextTrialNumber = next?nextTrialNumber + 1:nextTrialNumber - 1;
			nextTrial = ((IFolder)currentSession).findMember(DocometreMessages.Trial + nextTrialNumber);
		}
		return nextTrial;
    }

	public void nextTrial() {
		if(currentTrial != null) {
			IResource nextTrial = getTrial(currentTrial, true);
			if(nextTrial == null) nextTrial = getFirstRunnableUndoneTrial(currentTrial.getParent());
			if(nextTrial == null) {
				running = false;
				displayMessage(DocometreMessages.CantFindNextTrialMessage);
			}
			else {
				currentTrial = nextTrial;
				updateRealTimeChartsView();
			}
		}
	}

	public void previousTrial() {
		if(currentTrial != null) {
			IResource previousTrial = getTrial(currentTrial, false);
			if(previousTrial == null) {
				running = false;
				displayMessage(DocometreMessages.CantFindPreviousTrialMessage);
			}
			else {
				currentTrial = previousTrial;
				updateRealTimeChartsView();
			}
		}
	}

	public void gotoLastTrial() {
		if(currentTrial != null) computeTrial(true);
	}

	public void gotoFirstTrial() {
		if(currentTrial != null) computeTrial(false);
	}
	
	private void computeTrial(boolean next) {
		IResource originalTrial = currentTrial;
		IResource trial = currentTrial;
		while (trial != null) {
			trial = getTrial(trial, next);
			if(trial != null) originalTrial = trial;
		}
		if(originalTrial == currentTrial) {
			String message = DocometreMessages.CantFindFirstTrialMessage;
			if(next) message = DocometreMessages.CantFindLastTrialMessage;
			MessageDialog.openWarning(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.Warning, message);
		}
		else {
			currentTrial = originalTrial;
			updateRealTimeChartsView();
		}
	}
	
	private IResource getFirstRunnableUndoneTrial(IResource parentResource) {
		if(ResourceType.isSubject(parentResource) || ResourceType.isSession(parentResource)) {
			try {
				IResource[] resources = ((IContainer)parentResource).members();
				List<IResource> resourcesList = Arrays.asList(resources);
				if (resourcesList.size() > 0) {
					Collections.sort(resourcesList, new Comparator<IResource>() {
						@Override
						public int compare(final IResource resource1, final IResource resource2) {
							if(resource1.getName().matches("^.*\\d+$") && resource2.getName().matches("^.*\\d+$")) {
								String prefix = resource1.getName().replaceAll("\\d+$", "");
								int number1 = Integer.parseInt(resource1.getName().replaceAll(prefix, ""));
								prefix = resource2.getName().replaceAll("\\d+$", "");
								int number2 = Integer.parseInt(resource2.getName().replaceAll(prefix, ""));
								return number1 - number2;
							}
							return resource1.getName().compareTo(resource2.getName());
						}
					});
				}
				for (IResource currentResource : resourcesList) {
					IResource resource = getFirstRunnableUndoneTrial(currentResource);
					if(resource != null) return resource;
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		} else if(ResourceType.isTrial(parentResource)) {
			// This is a trial ....
			boolean trialDone = ResourceProperties.isTrialDone((IFolder) parentResource);
			IResource process = ResourceProperties.getAssociatedProcess(parentResource);
			if(!trialDone && process != null) return parentResource;
		}
		return null;
	}
	
	private void displayMessage(String message) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openWarning(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.Warning, message);
			}
		});
	}
	
	private void updateRealTimeChartsView() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				RealTimeChartsView realTimeChartsView = (RealTimeChartsView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RealTimeChartsView.ID);
				realTimeChartsView.updateValues(currentTrial);
				realTimeChartsView.updateValues(ResourceProperties.getAssociatedProcess(currentTrial));
			}
		});
	}
	
	public int getCurrentTrialNumber() {
		if(currentTrial == null) return 0;
		String trialNumber = currentTrial.getName().split("°")[1];
		return Integer.parseInt(trialNumber);
	}

}
