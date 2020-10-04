package fr.univamu.ism.docometre.analyse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.python.PythonController;
import fr.univamu.ism.docometre.python.PythonEntryPoint;

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
		
		monitor.beginTask("Waiting for Python to start. Please wait.", timeOut*1000 + 1000);
		while(startPythonInnerJob.getState() != Job.RUNNING);
		while(startPythonInnerJob.getState() == Job.RUNNING) {
			t1 = System.currentTimeMillis();
			monitor.worked((int)(t1 - t0));
			t0 = t1;
		}
		
		if(startPythonInnerJob.getResult() != null && startPythonInnerJob.getResult().isOK()) Activator.logInfoMessage(DocometreMessages.MathEngineStarted, PythonEngine.class);
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
		
		monitor.beginTask("Waiting for Python to stop. Please wait.", timeOut*1000 + 1000);
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
		if(!ResourceType.isSubject(subject)) return false;
		if(!isStarted()) return false;
		String experimentName = subject.getFullPath().segment(0);
		String subjectName = subject.getFullPath().segment(1);
		String prefixKey = experimentName + "\\." + subjectName;
		String expression = "len({k:v for k,v in docometre.experiments.items() if re.search(\"^" + prefixKey + "\\.\", k)}) > 0";
		return pythonController.getPythonEntryPoint().evaluate(expression).equalsIgnoreCase("true");
	}

	@Override
	public void load(IResource subject) {
		try {
			if(!ResourceType.isSubject(subject)) return;
			
			String loadName = getFullPath(subject);
			String dataFilesList = Analyse.getDataFiles(subject);
			//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);
			Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
			
			pythonController.getPythonEntryPoint().loadData("DOCOMETRE", loadName, dataFilesList, sessionsProperties);

			ChannelsContainer channelsContainer = new ChannelsContainer((IFolder) subject);
			subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, channelsContainer);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}

	}

	@Override
	public void unload(IResource subject) {
		if(!ResourceType.isSubject(subject)) return;
		if(!isStarted()) return;
		String experimentName = subject.getFullPath().segment(0);
		String subjectName = subject.getFullPath().segment(1);
		String prefixKey = experimentName + "\\." + subjectName;
		pythonController.getPythonEntryPoint().unload(prefixKey);
	}

	@Override
	public boolean exist(String variableName) {
		return false;
	}

	@Override
	public boolean isStruct(String variableName) {
		return false;
	}

	@Override
	public boolean isField(String variableName, String fieldName) {
		return false;
	}
	
	@Override
	public Channel[] getChannels(IResource subject) {
		String key = getFullPath(subject);
		String channelsNamesString = pythonController.getPythonEntryPoint().getChannels(key);
		String[] channelsNames = channelsNamesString.split(",");
		ArrayList<Channel> channels = new ArrayList<Channel>();
		for (String channelName : channelsNames) {
			Channel channel = new Channel((IFolder) subject, channelName);
			if(isSignal(channel) || isCategory(channel) || isEvent(channel)) channels.add(channel);
		}
		return channels.toArray(new Channel[channels.size()]);
	}
	
	
	@Override
	public boolean isSignalCategoryOrEvent(String fullName, String check) {
		String expression = fullName + check;
		expression = "docometre.experiments[\"" + expression + "\"]";
		String values = pythonController.getPythonEntryPoint().evaluate(expression);
		return values.equalsIgnoreCase("true") || values.equalsIgnoreCase("1");
	}

	@Override
	public String getCriteriaForCategory(IResource category) {
		String fullName = getFullPath(category);
		String expression = "docometre.experiments[\"" + fullName + ".Criteria\"]";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return value;
	}

	@Override
	public Integer[] getTrialsListForCategory(IResource category) {
		String fullName = getFullPath(category);
		String expression = "docometre.experiments[\"" + fullName + ".TrialsList\"]";
		byte[] byteValues = pythonController.getPythonEntryPoint().getVector(expression, PythonEntryPoint.DATA_TYPE_INT, -1, -1, -1);
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteValues);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		int[] values = new int[intBuffer.capacity()];
		intBuffer.get(values);
    	Arrays.sort(values);
    	return Arrays.stream(values).boxed().toArray(Integer[]::new);
	}

	@Override
	public double[] getYValuesForSignal(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
		int frontCut = getFrontCut(signal, trialNumber);
		int endCut = getEndCut(signal, trialNumber) - 1;
		String expression = "docometre.experiments[\"" + fullName + ".Values\"]";
		byte[] byteValues = pythonController.getPythonEntryPoint().getVector(expression, PythonEntryPoint.DATA_TYPE_DOUBLE, trialNumber-1, frontCut, endCut);
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteValues);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    	DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
    	double[] values = new double[doubleBuffer.capacity()];
    	doubleBuffer.get(values);
    	return values;
	}

	@Override
	public int getTrialsNumber(Channel signal) {
		String fullName = getFullPath(signal);
		String expression = "len(docometre.experiments[\"" + fullName + ".Values\"])";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(value);
	}

	@Override
	public double getSampleFrequency(Channel signal) {
		String fullName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + fullName + ".SampleFrequency\"]";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return Double.parseDouble(value);
	}

	@Override
	public int getSamplesNumber(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + fullName + ".NbSamples." + trialNumber + "\"]";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(value);
	}

	@Override
	public int getFrontCut(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + fullName + ".FrontCut." + trialNumber + "\"]";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(value);
	}

	@Override
	public int getEndCut(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + fullName + ".EndCut." + trialNumber + "\"]";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(value);
	}

	@Override
	public void runScript(String code) {
		pythonController.getPythonEntryPoint().runScript(code);
	}

	@Override
	public void deleteChannel(Channel channel) {
		String experimentName = channel.getFullPath().segment(0);
		String subjectName = channel.getFullPath().segment(1);
		String channelName = channel.getFullPath().segment(2);
		channelName = experimentName + "\\." + subjectName + "\\." + channelName;
		pythonController.getPythonEntryPoint().unload(channelName);
	}
	
	@Override
	public int getNbMarkersGroups(Channel signal) {
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + channelName + ".NbMarkersGroups\"]";
		String nbMarkersGroups = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(nbMarkersGroups);
	}

	@Override
	public void createNewMarkersGroup(Channel signal, String markersGroupLabel) {
		int nbMarkersGroups = getNbMarkersGroups(signal);
		nbMarkersGroups++;
		
		String channelName = getFullPath(signal);
		
		String expression = "docometre.experiments[\"" + channelName + ".NbMarkersGroups\"] = " + nbMarkersGroups;
		pythonController.getPythonEntryPoint().runScript(expression);
		
		if(nbMarkersGroups == 1) expression = "docometre.experiments['" + channelName + ".MarkersGroupsLabels'] = ['" + markersGroupLabel + "']";
		else expression ="docometre.experiments['" + channelName + ".MarkersGroupsLabels'].append('" + markersGroupLabel + "')";
		pythonController.getPythonEntryPoint().runScript(expression);
		
		expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_"+ markersGroupLabel + "_Values\"] = []";
		pythonController.getPythonEntryPoint().runScript(expression);
	}


	@Override
	public String getMarkersGroupLabel(int markersGroupNumber, Channel signal) {
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments['" + channelName + ".MarkersGroupsLabels'][" + (markersGroupNumber-1) + "]";
		return pythonController.getPythonEntryPoint().evaluate(expression);
	}

	@Override
	public void deleteMarkersGroup(int markersGroupNumber, Channel signal) {
		String channelName = getFullPath(signal);
		
		// Get markers group label
		String markersGroupLabel = getMarkersGroupLabel(markersGroupNumber, signal);
		
		// Remove markers group label
		String expression = "docometre.experiments['" + channelName + ".MarkersGroupsLabels'].pop(" + (markersGroupNumber - 1) + ")";
		pythonController.getPythonEntryPoint().runScript(expression);
		
		// decrease nb markers groups
		int nbMarkersGroups = getNbMarkersGroups(signal);
		nbMarkersGroups--;
		expression = "docometre.experiments[\"" + channelName + ".NbMarkersGroups\"] = " + nbMarkersGroups;
		pythonController.getPythonEntryPoint().runScript(expression);
		
		// Remove markers group value
		String experimentName = signal.getFullPath().segment(0);
		String subjectName = signal.getFullPath().segment(1);
		channelName = signal.getFullPath().segment(2);
		channelName = experimentName + "\\." + subjectName + "\\." + channelName;
		expression = channelName + "\\.MarkersGroup_" + markersGroupLabel; 
		pythonController.getPythonEntryPoint().unload(expression);
	}
	
	@Override
	public void addMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal) {
		String channelName = getFullPath(signal);
		String valuesString = "[" + trialNumber + "," + xValue + "," + yValue + "]";
		String expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_" + markersGroupLabel + "_Values\"].append(" + valuesString + ")";
		pythonController.getPythonEntryPoint().runScript(expression);
	}

	@Override
	public double[][] getMarkers(String markersGroupLabel, Channel signal) {
		if("".equals(markersGroupLabel)) return new double[0][0];
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_" + markersGroupLabel + "_Values\"]";
		byte[] byteValues = pythonController.getPythonEntryPoint().getVector(expression, PythonEntryPoint.DATA_TYPE_DOUBLE, -1, -1, -1);
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteValues);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    	DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
    	double[] values = new double[doubleBuffer.capacity()];
    	doubleBuffer.get(values);
    	
    	double[][] markersValues = new double[values.length / 3][3];
    	
    	for (int i = 0; i < values.length; i++) {
			markersValues[i / 3][i % 3] = values[i];
		}
    	
    	return markersValues;
	}
	
}
