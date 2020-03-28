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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.matlab.MatlabController;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class StartStopMatlabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job startMatlabJob = new Job(DocometreMessages.MathEngineStartStop) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MatlabController matlabController = MatlabController.getInstance();
				if (!matlabController.isStarted()) {
					Activator.logInfoMessage(DocometreMessages.MathEngineStarting, MatlabController.class);
					String matlabLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_LOCATION);
					String matlabScriptsLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPT_LOCATION);
					int timeOut = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.MATLAB_TIME_OUT);
					boolean showWindow = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_MATLAB_WINDOW);
					
					Job startMatlabInnerJob = new Job(DocometreMessages.WaitingForMatlab) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							IStatus status = Status.OK_STATUS;
							try {
								matlabController.startMatlab(showWindow, timeOut, matlabLocation, matlabScriptsLocation);
							} catch (Exception e) {
								e.printStackTrace();
								Activator.logErrorMessageWithCause(e);
								status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
							}
							return status;
						}
					};
					
					startMatlabInnerJob.schedule();
					long t0 = System.currentTimeMillis();
					long t1 = t0;
					
					monitor.beginTask("", timeOut*1000);
					while(startMatlabInnerJob.getState() != Job.RUNNING);
					while(startMatlabInnerJob.getState() == Job.RUNNING) {
						t1 = System.currentTimeMillis();
						monitor.worked((int)(t1 - t0));
						t0 = t1;
					}
					
					if(startMatlabInnerJob.getResult() != null && startMatlabInnerJob.getResult().isOK()) Activator.logInfoMessage(DocometreMessages.MathEngineStarted, MatlabController.class);
					
				} else {
					Activator.logInfoMessage(DocometreMessages.MathEngineStopping, MatlabController.class);
					
					Job stopMatlabInnerJob = new Job(DocometreMessages.WaitingForMatlab) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							IStatus status = Status.OK_STATUS;
							try {
								matlabController.stopMatlab();
							} catch (Exception e) {
								e.printStackTrace();
								Activator.logErrorMessageWithCause(e);
								status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
							}
							return status;
						}
					};
					
					stopMatlabInnerJob.schedule();
					long t0 = System.currentTimeMillis();
					long t1 = t0;
					
					monitor.beginTask("Task", 5*1000); // time out of 5 seconds on stopping Matlab
					while(stopMatlabInnerJob.getState() != Job.RUNNING);
					while(stopMatlabInnerJob.getState() == Job.RUNNING) {
						t1 = System.currentTimeMillis();
						monitor.worked((int)(t1 - t0));
						t0 = t1;
					}
					
					if(stopMatlabInnerJob.getResult() != null && stopMatlabInnerJob.getResult().isOK()) Activator.logInfoMessage(DocometreMessages.MathEngineStopped, MatlabController.class);
					
				}
				return Status.OK_STATUS;
			}
		};
		
		startMatlabJob.setUser(true);
		startMatlabJob.schedule();
		
		
		return null;
	}

}
