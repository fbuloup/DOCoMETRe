package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.python.PythonController;

public class PythonEngine implements MathEngine {
	
	private List<MathEngineListener> mathEngineListeners = new ArrayList<MathEngineListener>();
	
	private PythonController pythonController;
	
	public PythonEngine() {
		pythonController = PythonController.getInstance();
	}
	
	@Override
	public IStatus startEngine(IProgressMonitor monitor) {
		Activator.logInfoMessage(DocometreMessages.MathEngineStarting, PythonController.class);
		
		String pythonLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_LOCATION);
		String pythonScriptsLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_SCRIPT_LOCATION);
		int timeOut = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PYTHON_TIME_OUT);

		Job startPythonInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				try {
					pythonController.startServer(pythonLocation, pythonScriptsLocation, timeOut);
					notifyListeners();
				} catch (Exception e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
					status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
				}
				return status;
			}
		};
		
		startPythonInnerJob.schedule();
		long t0 = System.currentTimeMillis();
		long t1 = t0;
		
		monitor.beginTask("", timeOut*1000);
		while(startPythonInnerJob.getState() != Job.RUNNING);
		while(startPythonInnerJob.getState() == Job.RUNNING) {
			t1 = System.currentTimeMillis();
			monitor.worked((int)(t1 - t0));
			t0 = t1;
		}
		
		if(startPythonInnerJob.getResult() != null && startPythonInnerJob.getResult().isOK()) Activator.logInfoMessage(DocometreMessages.MathEngineStarted, PythonController.class);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus stopEngine(IProgressMonitor monitor) {
		Activator.logInfoMessage(DocometreMessages.MathEngineStopping, PythonController.class);
		
		int timeOut = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PYTHON_TIME_OUT);

		Job stopPythonInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				try {
					pythonController.stopServer(timeOut);
					notifyListeners();
				} catch (Exception e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
					status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
				}
				return status;
			}
		};
		
		stopPythonInnerJob.schedule();
		long t0 = System.currentTimeMillis();
		long t1 = t0;
		
		monitor.beginTask("", timeOut*1000);
		while(stopPythonInnerJob.getState() != Job.RUNNING);
		while(stopPythonInnerJob.getState() == Job.RUNNING) {
			t1 = System.currentTimeMillis();
			monitor.worked((int)(t1 - t0));
			t0 = t1;
		}
		
		if(stopPythonInnerJob.getResult() != null && stopPythonInnerJob.getResult().isOK())  Activator.logInfoMessage(DocometreMessages.MathEngineStopped, PythonController.class);
		return Status.OK_STATUS;
	}

	@Override
	public boolean isStarted() {
		return pythonController.isStarted();
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

	@Override
	public boolean isSubjectLoaded(IResource subject) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void load(IResource subject) {
		if(!ResourceType.isSubject(subject)) return;
		String experimentName = subject.getFullPath().segment(0);
		String subjectName = subject.getFullPath().segment(1);
		
		String loadName = experimentName + "." + subjectName;
		
		String dataFilesList = Analyse.getDataFiles(subject);
		//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);

		Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
		
		pythonController.getPythonEntryPoint().loadData("DOCOMETRE", loadName, dataFilesList, sessionsProperties);

	}

	@Override
	public void unload(IResource subject) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean exist(String variableName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStruct(String variableName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isField(String variableName, String fieldName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Channel[] getChannels(IResource subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel getChannelWithName(IResource subject, String channelName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel[] getSignals(IResource subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel[] getCategories(IResource subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel[] getEvents(IResource subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSignal(IResource channel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCategory(IResource channel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEvent(IResource channel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getCriteriaForCategory(IResource category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer[] getTrialsListForCategory(IResource category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getYValuesForSignal(Channel signal, int trialNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getTimeValuesForSignal(Channel signal, Integer trialNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTrialsNumber(Channel signal) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSampleFrequency(Channel signal) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSamplesNumber(Channel signal, int trialNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFrontCut(Channel signal, int trialNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getEndCut(Channel signal, int trialNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void runScript(String code) {
		// TODO Auto-generated method stub

	}

	@Override
	public Channel getChannelFromName(IResource experiment, String fullChannelName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteChannel(Channel resource) {
		// TODO Auto-generated method stub

	}

}
