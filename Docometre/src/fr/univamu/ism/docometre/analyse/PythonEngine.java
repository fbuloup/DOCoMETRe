package fr.univamu.ism.docometre.analyse;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

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
	
	private boolean loadFromSavedFile;
	
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
					String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
					pythonController.startServer(pythonLocation, pythonScriptsLocation, timeOut);
					String cmd = "import os;os.chdir('" + workspacePath + "')";
					pythonController.getPythonEntryPoint().runScript(cmd);
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
	public void load(IResource subject, boolean loadFromSavedFile) {
		try {
			if(!ResourceType.isSubject(subject)) return;
			
			String experimentName = subject.getFullPath().segment(0);
			String subjectName = subject.getFullPath().segment(1);
			String workpsacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			String saveFilesFullPath = workpsacePath + File.separator + experimentName + File.separator + subjectName + File.separator;
			File saveFile = new File(saveFilesFullPath + "save.data");
			
			PythonEngine.this.loadFromSavedFile = loadFromSavedFile;
			if(saveFile.exists() && !loadFromSavedFile) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						PythonEngine.this.loadFromSavedFile = !MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.LoadSubjectFromRawDataDialog_Title, DocometreMessages.LoadSubjectFromRawDataDialog_Message);
					}
				});
				
			}
			
			if(saveFile.exists() && PythonEngine.this.loadFromSavedFile) {
				pythonController.getPythonEntryPoint().loadSubject(saveFilesFullPath);
			} else {
				String loadName = getFullPath(subject);
				String dataFilesList = Analyse.getDataFiles(subject);
				//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);
				Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
				
				pythonController.getPythonEntryPoint().loadData("DOCOMETRE", loadName, dataFilesList, sessionsProperties);
			}
			

			ChannelsContainer channelsContainer = new ChannelsContainer((IFolder) subject);
			subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, channelsContainer);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}

	}

	@Override
	public void unload(IResource resource) {
		if(!(ResourceType.isSubject(resource) || ResourceType.isExperiment(resource))) return;
		if(!isStarted()) return;
		if(ResourceType.isSubject(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			String subjectName = resource.getFullPath().segment(1);
			String prefixKey = experimentName + "\\." + subjectName;
			pythonController.getPythonEntryPoint().unload(prefixKey);
		}
		if(ResourceType.isExperiment(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			String prefixKey = experimentName;
			pythonController.getPythonEntryPoint().unload(prefixKey);
		}
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
			if("".equals(channelName)) continue;
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
		channel.setModified(true);
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
		
		expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_"+ markersGroupLabel + "_Values\"] = numpy.zeros((0,3));";
		pythonController.getPythonEntryPoint().runScript(expression);
		signal.setModified(true);
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
		signal.setModified(true);
	}
	
	@Override
	public void addMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal) {
		String channelName = getFullPath(signal);
		String valuesString = "[" + trialNumber + "," + xValue + "," + yValue + "]";
		String expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_" + markersGroupLabel + "_Values\"]";
		expression = expression + " = numpy.vstack([" + expression + ", " + valuesString + "]);";
		pythonController.getPythonEntryPoint().runScript(expression);
		signal.setModified(true);
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

	@Override
	public void deleteMarker(String markersGroupLabel, int trialNumber, double xValue, double yValue, Channel signal) {
		String channelName = getFullPath(signal);
		String x = "docometre.experiments[\"" + channelName + ".MarkersGroup_" + markersGroupLabel + "_Values\"]";
		String rowValues = "[" + trialNumber + "," + xValue + "," + yValue + "]";
		String expression = "tempValue = numpy.subtract(" + x + ", " + rowValues + ");\n";
		expression = expression + "tempValue = numpy.absolute(tempValue);\n";
		expression = expression + "tempValue = numpy.sum(tempValue, axis=1);\n";
		expression = expression + "tempValueBoolean = numpy.isclose(tempValue, 0);\n";
		expression = expression + "indexes = numpy.nonzero(tempValueBoolean);\n";
		expression = expression + "index = -1;\n";
		expression = expression + "if(indexes[0].size > 0):index = indexes[0][0];\n";
		expression = expression + "if(index > -1):" + x + " = numpy.delete(" + x + ", index, axis=0);";
		pythonController.getPythonEntryPoint().runScript(expression);
		double[][] values = getMarkers(markersGroupLabel, signal);
		int markersGroupNumber = getMarkersGroupNumber(markersGroupLabel, signal) + 1;
		if(values.length == 0) deleteMarkersGroup(markersGroupNumber, signal);
		signal.setModified(true);
		
	}

	@Override
	public void saveSubject(IResource subject) {
		String fullSubjectName = getFullPath(subject);
		String fullSubjectNameRegExp  = "^" + fullSubjectName.replaceAll("\\.", "\\\\.");
		
		String experimentName = subject.getFullPath().segment(0);
		String subjectName = subject.getFullPath().segment(1);
		String workpsacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		String dataFilesFullPath = workpsacePath + File.separator + experimentName + File.separator + subjectName + File.separator;
		
		pythonController.getPythonEntryPoint().saveSubject(fullSubjectNameRegExp, dataFilesFullPath);
		
		
	}

	@Override
	public boolean renameExperiment(String oldName, String newName) {
		String keyRegExp = "^" + oldName; 
		return pythonController.getPythonEntryPoint().rename(keyRegExp, newName);
	}

	@Override
	public boolean renameSubject(String experimentName, String oldSubjectName, String newSubjectName) {
		String keyRegExp = "^" + experimentName + "\\." + oldSubjectName + "\\."; 
		String keyReplace = experimentName + "." + newSubjectName + "."; 
		return pythonController.getPythonEntryPoint().rename(keyRegExp, keyReplace);
	}

	@Override
	public String evaluate(String command) {
		return pythonController.getPythonEntryPoint().evaluate(command);
	}

	@Override
	public IResource[] getCreatedOrModifiedSubjects() {
		Set<IResource> createdOrModifiedSubjects = new HashSet<>();
		try {
			String[] createdOrModifiedChannels = null;
			String createdOrModifiedChannelsString = evaluate("docometre.experiments['createdOrModifiedChannels']");
			if(createdOrModifiedChannelsString != null && !"".equals(createdOrModifiedChannelsString)) {
				createdOrModifiedChannels = createdOrModifiedChannelsString.split(":");
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				for (String createdOrModifiedChannel : createdOrModifiedChannels) {
					String[] segments = createdOrModifiedChannel.split("\\.");
					String subjectPath = segments[0] + "/" + segments[1];
					IResource subject = workspaceRoot.findMember(subjectPath);
					createdOrModifiedSubjects.add(subject);
				}
			}
			return createdOrModifiedSubjects.toArray(new IResource[createdOrModifiedSubjects.size()]);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} finally {
			evaluate("docometre.experiments.pop('createdOrModifiedChannels', None)");
		}
		
		return new IResource[0];
	}

	
	
}
