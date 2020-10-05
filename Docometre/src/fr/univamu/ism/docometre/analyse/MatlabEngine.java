package fr.univamu.ism.docometre.analyse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
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
		
		Job startMatlabInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
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
		
		monitor.beginTask("Waiting for Matlab to start. Please wait.", timeOut*1000);
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
		
		Job stopMatlabInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
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
		
		monitor.beginTask("Waiting for Matlab to stop. Please wait.", 5*1000); // time out of 5 seconds on stopping Matlab
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
	
	@Override
	public boolean isSubjectLoaded(IResource subject) {
		if(!isStarted() || !ResourceType.isSubject(subject)) return false;
		String experimentName = subject.getFullPath().segment(0);
		String subjectName = subject.getFullPath().segment(1);
		return exist(experimentName) && isStruct(experimentName) && isField(experimentName, subjectName); 
	}
	
	@Override
	public boolean isField(String variableName, String fieldName) {
		try {
			Object[] responses = matlabController.returningEval("isfield(" + variableName + ",'" + fieldName + "')", 1);
			Object response = responses[0];
			if(response instanceof boolean[]) {
				boolean[] values = (boolean[])response;
				return values[0];
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean isStruct(String variableName) {
		try {
			Object[] responses = matlabController.returningEval("isstruct(" + variableName + ")", 1);
			Object response = responses[0];
			if(response instanceof boolean[]) {
				boolean[] values = (boolean[])response;
				return values[0];
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
		
	}

	@Override
	public boolean exist(String variableName) {
		try {
			Object[] responses = matlabController.returningEval("exist('" + variableName + "','var')", 1);
			Object response = responses[0];
			if(response instanceof double[]) {
				double[] values = (double[])response;
				return values[0] > 0;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}

//	private String getLastErrorMessage() {
//		Object o = null;
//		try {
//			matlabController.eval("ERROR = lasterr;");
//			o = matlabController.getVariable("ERROR");
//			matlabController.eval("clear ERROR;");
//			if(o != null && o instanceof String) {
//				String message = (String)o;
//				return message;
//			}
//		} catch (Exception e) {
//			if(o != null) {
//				Activator.logErrorMessageWithCause(e);
//				e.printStackTrace();
//			}
//		}
//		return "";
//	}

	@Override
	public void load(IResource subject) {
		try {
			if(!ResourceType.isSubject(subject)) return;
			String experimentName = subject.getFullPath().segment(0);
			String subjectName = subject.getFullPath().segment(1);
			
			String dataFilesList = Analyse.getDataFiles(subject);
			//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);

			Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
			
			Set<String> keys = sessionsProperties.keySet();
			Collection<String> values = sessionsProperties.values();
			
			String keysString = String.join("','", keys);
			String valuesString = String.join("','", values);

			StringBuffer stringBuffer = new StringBuffer(keysString);
			stringBuffer.append("'}");
			stringBuffer.insert(0, "{'");
			keysString = stringBuffer.toString();

			stringBuffer = new StringBuffer(valuesString);
			stringBuffer.append("'}");
			stringBuffer.insert(0, "{'");
			valuesString = stringBuffer.toString();
			
			String cmd = experimentName + "." + subjectName + " = loadData('DOCOMETRE', '" + dataFilesList + "', " + keysString + ", " + valuesString + ")";
			matlabController.eval(cmd);
			
			ChannelsContainer channelsContainer = new ChannelsContainer((IFolder) subject);
			subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, channelsContainer);
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void unload(IResource subject) {
		try {
			if(!ResourceType.isSubject(subject)) return;
			String experimentName = subject.getFullPath().segment(0);
			String subjectName = subject.getFullPath().segment(1);
			String cmd = experimentName + " = rmfield(" + experimentName + ", '" + subjectName + "');";
			matlabController.eval(cmd);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public Channel[] getChannels(IResource subject) {
		try {
			if(!ResourceType.isSubject(subject)) return null;
			String expression = getFullPath(subject);
			Object[] responses = matlabController.returningEval("fieldnames(" + expression + ")", 1);
			Object response = responses[0];
			String[] channelsNames = (String[]) response;
			ArrayList<Channel> channels = new ArrayList<Channel>();
			for (String channelName : channelsNames) {
				Channel channel = new Channel((IFolder) subject, channelName);
				if(isSignal(channel) || isCategory(channel) || isEvent(channel)) channels.add(channel);
			}
			return channels.toArray(new Channel[channels.size()]);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean isSignalCategoryOrEvent(String fullName, String check) {
		try {
			Object[] responses = matlabController.returningEval(fullName + check, 1);
			Object response = responses[0];
			if(response instanceof double[]) {
				return ((double[])response)[0] > 0;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public String getCriteriaForCategory(IResource category) {
		try {
			if(!isCategory(category)) return null;
			Object[] responses = matlabController.returningEval(getFullPath(category) + ".Criteria", 1);
			Object response = responses[0];
			if(response instanceof String) {
				return (String)response;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Integer[] getTrialsListForCategory(IResource category) {
		try {
			if(!isCategory(category)) return new Integer[0];
			Object[] responses = matlabController.returningEval(getFullPath(category) + ".TrialsList", 1);
			Object response = responses[0];
			if(response instanceof double[]) {
				double[] responseDouble = (double[])response;
				Integer[] trialsList = new Integer[responseDouble.length];
				for (int i = 0; i < trialsList.length; i++) {
					trialsList[i] = (int) responseDouble[i];
				}
				return trialsList;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public double[] getYValuesForSignal(Channel signal, int trialNumber) {
		try {
			String variableName = "yValues_" + (new Date()).getTime();
			int frontCut = getFrontCut(signal, trialNumber) + 1;
			int endCut = getEndCut(signal, trialNumber) - 1;
			String varCmd = getFullPath(signal) + ".Values(" + trialNumber + "," + frontCut + ":" + endCut + ");";
			String cmd = variableName + " = " + varCmd;
			matlabController.eval(cmd);
			Object valuesObject = matlabController.getVariable(variableName);
			matlabController.eval("clear " + variableName + ";");
			if(valuesObject instanceof double[]) return (double[])valuesObject;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public int getTrialsNumber(Channel signal) {
		try {
			String variableName = "nbTrials_" + (new Date()).getTime();
			matlabController.eval(variableName + " = size(" + getFullPath(signal) + ".Values" + ");");
			Object valuesObject = matlabController.getVariable(variableName);
			matlabController.eval("clear " + variableName + ";");
			int nbTrials = (int) ((double[]) valuesObject)[0];
			return nbTrials;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getSampleFrequency(Channel signal) {
		try {
			Object[] responses = matlabController.returningEval(getFullPath(signal) + ".SampleFrequency", 1);
			double sf = ((double[]) responses[0])[0];
			return sf;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getSamplesNumber(Channel signal, int trialNumber) {
		try {
			Object[] responses = matlabController.returningEval(getFullPath(signal) + ".NbSamples(" + trialNumber + ")", 1);
			int value = (int) ((double[]) responses[0])[0];
			return value;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getFrontCut(Channel signal, int trialNumber) {
		try {
			Object[] responses = matlabController.returningEval(getFullPath(signal) + ".FrontCut(" + trialNumber + ")", 1);
			int value = (int) ((double[]) responses[0])[0];
			return value;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getEndCut(Channel signal, int trialNumber) {
		try {
			Object[] responses = matlabController.returningEval(getFullPath(signal) + ".EndCut(" + trialNumber + ")", 1);
			int value = (int) ((double[]) responses[0])[0];
			return value;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void runScript(String code) {
		try {
			matlabController.evaluate(code);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void deleteChannel(Channel channel) {
		try {
			String channelName = channel.getName();
			String subjectFullPath = channel.getFullPath().removeLastSegments(1).toString().replaceAll("/", ".");
			String cmd = subjectFullPath + " = " + "rmfield(" + subjectFullPath + ", '" + channelName + "');";
			matlabController.evaluate(cmd);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}
	
	@Override
	public int getNbMarkersGroups(Channel signal) {
		try {
			if(!isStarted()) return 0;
			String channelName = getFullPath(signal);
			String expression = channelName + ".NbMarkersGroups;";
			Object[] responses = matlabController.returningEval(expression, 1);
			int nbMarkersGroups = (int) ((double[]) responses[0])[0];
			return nbMarkersGroups;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
		
	}

	@Override
	public void createNewMarkersGroup(Channel signal, String markersGroupLabel) {
		try {
			int nbMarkersGroups = getNbMarkersGroups(signal);
			nbMarkersGroups++;
			
			String channelName = getFullPath(signal);
			
			String expression = channelName + ".NbMarkersGroups = " + nbMarkersGroups + ";";
			matlabController.eval(expression);
			
			if(nbMarkersGroups == 1) expression = channelName + ".MarkersGroupsLabels = {'" + markersGroupLabel + "'};";
			else expression = channelName + ".MarkersGroupsLabels = [" + channelName + ".MarkersGroupsLabels, {'" + markersGroupLabel + "'}];";
			
			matlabController.eval(expression);
			
			expression = channelName + ".MarkersGroup_"+ markersGroupLabel + "_Values = [];";
			matlabController.eval(expression);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	@Override
	public String getMarkersGroupLabel(int markersGroupNumber, Channel signal) {
		try {
			String fullSignalName = getFullPath(signal);
			Object[] responses = matlabController.returningEval(fullSignalName + ".MarkersGroupsLabels{" + markersGroupNumber + "}", 1);
			Object response = responses[0];
			if(response instanceof String) {
				return (String)response;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public void deleteMarkersGroup(int markersGroupNumber, Channel signal) {
		
		try {
			String fullSignalName = getFullPath(signal);
			
			// Get markers group label
			String markersGroupLabel = getMarkersGroupLabel(markersGroupNumber, signal);
			
			// Remove markers group label
			String expression = fullSignalName + ".MarkersGroupsLabels(" + markersGroupNumber + ") = []";
			matlabController.eval(expression);
			
			// Remove markers group value
			expression = fullSignalName + " = rmfield(" + fullSignalName + ", 'MarkersGroup_" + markersGroupLabel + "_Values')";
			matlabController.eval(expression);
			
			// decrease nb markers groups
			int nbMarkersGroups = getNbMarkersGroups(signal);
			nbMarkersGroups--;
			expression = fullSignalName + ".NbMarkersGroups = " + nbMarkersGroups;
			matlabController.eval(expression);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	@Override
	public void addMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal) {
		try {
			String fullSignalName = getFullPath(signal);
			
			double[][] values = getMarkers(markersGroupLabel, signal);
			
			String expression = "";
			
			if(values.length == 0) {
				expression = "[" + trialNumber + "," + xValue + "," + yValue + "];";
				expression = fullSignalName + ".MarkersGroup_" + markersGroupLabel + "_Values = " + expression;
			} else {
				expression = "" + trialNumber + "," + xValue + "," + yValue;
				String markersValues = fullSignalName + ".MarkersGroup_" + markersGroupLabel + "_Values";
				expression = markersValues + " = " + "[" + markersValues + ";" + expression + "];";
			}
			matlabController.eval(expression);
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public double[][] getMarkers(String markersGroupLabel, Channel signal) {
		try {
			if("".equals(markersGroupLabel)) return new double[0][0];
			String fullSignalName = getFullPath(signal);
			String expression = fullSignalName + ".MarkersGroup_" + markersGroupLabel + "_Values";
			double[][] response = matlabController.getVariable2DArray(expression);
//			Object response = responses[0];
			if(response instanceof double[][]) {
				return (double[][])response;
			}
//			if(response instanceof double[]) {
//				double[][] values = new double[1][3];
//				values[0] = (double[])response;
//				return values;
//			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		return new double[0][0];
	}

	@Override
	public void deleteMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal) {
		try {
			String channelName = getFullPath(signal);
			String valuesString = "[" + trialNumber + "," + xValue + "," + yValue + "]";
			String id = channelName + ".MarkersGroup_" + markersGroupLabel + "_Values";
			String expression = id + " = " + id + "(~ismember(" + id + ", " + valuesString + ",'rows'), :)";
			matlabController.eval(expression);
			
			double[][] values = getMarkers(markersGroupLabel, signal);
			int markersGroupNumber = getMarkersGroupNumber(markersGroupLabel, signal) + 1;
			if(values.length == 0) deleteMarkersGroup(markersGroupNumber, signal);
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		
		
	}

}
