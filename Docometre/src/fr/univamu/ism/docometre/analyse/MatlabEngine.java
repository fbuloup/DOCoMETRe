package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.matlab.MatlabController;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public final class MatlabEngine implements MathEngine {
	
	private List<MathEngineListener> mathEngineListeners = new ArrayList<MathEngineListener>();
	
	private MatlabController matlabController;
	
	public MatlabEngine() {
		matlabController = MatlabController.getInstance();
	}

	@Override
	public IStatus startEngine(IProgressMonitor monitor) {
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
				notifyListeners();
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
		return startMatlabInnerJob.getResult();
	}

	@Override
	public IStatus stopEngine(IProgressMonitor monitor) {
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
				notifyListeners();
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
		
		return stopMatlabInnerJob.getResult();
	}

	@Override
	public boolean isStarted() {
		return matlabController.isStarted();
	}

	@Override
	public void addListener(MathEngineListener listener) {
		if(!mathEngineListeners.contains(listener)) mathEngineListeners.add(listener);
	}

	private void notifyListeners() {
		for (MathEngineListener mathEngineListener : mathEngineListeners) {
			mathEngineListener.notifyListener();
		}
	}

}
