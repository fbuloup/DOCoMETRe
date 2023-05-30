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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;
import fr.univamu.ism.docometre.python.PythonController;
import fr.univamu.ism.docometre.python.PythonEntryPoint;

public class PythonEngine implements MathEngine {
	
	private List<MathEngineListener> mathEngineListeners = new ArrayList<MathEngineListener>();
	
	private PythonController pythonController;
	
	private boolean loadFromSavedFile;

	private String pythonLocation;
	
	public PythonEngine() {
		pythonController = PythonController.getInstance();
	}
	
	@Override
	public IStatus startEngine(IProgressMonitor monitor) {
		Activator.logInfoMessage(DocometreMessages.MathEngineStarting, PythonController.class);
		
		pythonLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_LOCATION);
		String pythonScriptsLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION);
		int timeOut = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PYTHON_TIME_OUT);
		
		// Set default python path if empty
		if("".equals(pythonLocation)) {
			if(Platform.getOS().equals(Platform.OS_WIN32)) pythonLocation = "py";
			else pythonLocation = "python";
		}

		Job startPythonInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				try {
					String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
					IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
					int javaPort = preferenceStore.getInt(MathEnginePreferencesConstants.PY4J_JAVA_PORT);
					int pythonPort = preferenceStore.getInt(MathEnginePreferencesConstants.PY4J_PYTHON_PORT);
					if(pythonController.startServer(pythonLocation, pythonScriptsLocation, timeOut, monitor, javaPort, pythonPort)) {
						if(!monitor.isCanceled()) {
							String cmd = "import os;os.chdir('" + workspacePath + "')";
							pythonController.getPythonEntryPoint().runScript(cmd, false);
						}
					}
					notifyListeners();
					if(monitor.isCanceled()) throw new InterruptedException(DocometreMessages.OperationCanceledByUser);
				} catch (Exception e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
					String message = e.getMessage();
					message = message == null ? e.getCause().getMessage() : message;
					status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
				}
				return status;
			}
		};
		
		startPythonInnerJob.schedule();
		long t0 = System.currentTimeMillis();
		long t1 = t0;
		
		monitor.beginTask(DocometreMessages.MathEngineStarting, timeOut*1000 + 1000);
		while(startPythonInnerJob.getState() != Job.RUNNING);
		while(startPythonInnerJob.getState() == Job.RUNNING) {
			t1 = System.currentTimeMillis();
			monitor.worked((int)(t1 - t0));
			t0 = t1;
		}
		String message = DocometreMessages.MathEngineStarted + " (Python)";
		if(startPythonInnerJob.getResult() != null && startPythonInnerJob.getResult().isOK()) {
			Activator.logInfoMessage(getPythonVersion(), PythonController.class);
			Activator.logInfoMessage(message, PythonEngine.class);
		}
		return Status.OK_STATUS;
	}
	
	private String getPythonVersion() {
		pythonController.getPythonEntryPoint().runScript("from platform import python_version; docometre.experiments['pythonVersion'] = python_version();", false);
		String response = "Python version : " + pythonController.getPythonEntryPoint().evaluate("docometre.experiments['pythonVersion']");
		return response;
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
		
		monitor.beginTask(DocometreMessages.MathEngineStopping, timeOut*1000 + 1000);
		while(stopPythonInnerJob.getState() != Job.RUNNING);
		while(stopPythonInnerJob.getState() == Job.RUNNING) {
			t1 = System.currentTimeMillis();
			monitor.worked((int)(t1 - t0));
			t0 = t1;
		}
		
		if(stopPythonInnerJob.getResult() != null && stopPythonInnerJob.getResult().isOK()) {
			Activator.logInfoMessage(DocometreMessages.MathEngineStopped, PythonController.class);
			MathEngineFactory.clear();
		}

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
//		try {
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
						String message = NLS.bind(DocometreMessages.LoadSubjectFromRawDataDialog_Message, subject.getFullPath().toPortableString());
						PythonEngine.this.loadFromSavedFile = !MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.LoadSubjectFromRawDataDialog_Title, message);
					}
				});
				
			}
			
			if(saveFile.exists() && PythonEngine.this.loadFromSavedFile) {
				pythonController.getPythonEntryPoint().loadSubject(saveFilesFullPath);
			} else {
				String loadName = getFullPath(subject);
				String dataFilesList = Analyse.getDataFiles(subject);
				//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);
				boolean isOptitrack = Analyse.isOptitrack(dataFilesList.split(";"), (IContainer) subject);
				if(isOptitrack) {
					// If all data files are OPTITRACK_TYPE_1
					pythonController.getPythonEntryPoint().loadData("OPTITRACK_TYPE_1", loadName, dataFilesList, null);
				} else {
					Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
					pythonController.getPythonEntryPoint().loadData("DOCOMETRE", loadName, dataFilesList, sessionsProperties);
				}
				
			}
			

			MathEngine.super.load(subject, loadFromSavedFile);
			
//		} catch (CoreException e) {
//			e.printStackTrace();
//			Activator.logErrorMessageWithCause(e);
//		}

	}

	@Override
	public void unload(IResource resource) {
		if(!(ResourceType.isSubject(resource) || ResourceType.isExperiment(resource))) return;
		if(!isStarted()) return;
		if(ResourceType.isSubject(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			String subjectName = resource.getFullPath().segment(1);
			String prefixKey = experimentName + "\\." + subjectName + "\\.";
			pythonController.getPythonEntryPoint().unload(prefixKey);
		}
		if(ResourceType.isExperiment(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			String prefixKey = experimentName + "\\.";
			pythonController.getPythonEntryPoint().unload(prefixKey);
		}
	}

	@Override
	public boolean exist(String variableName) {
		String cmd = "'" + variableName + "'" + " in docometre.experiments.keys()";
		String response = evaluate(cmd);
		return "True".equals(response);
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
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				ArrayList<Channel> channels = channelsContainer.manageChannelsCacheBefore();
				if(channelsContainer.updateChannelsCache()) {
					channels.clear();
					String key = getFullPath(subject);
					Activator.logInfoMessage(NLS.bind(DocometreMessages.UpdateCacheChannels, key), PythonEngine.class);
					String channelsNamesString = pythonController.getPythonEntryPoint().getChannels(key);
					String[] channelsNames = channelsNamesString.split(",");
					for (String channelName : channelsNames) {
						if("".equals(channelName)) continue;
						Channel channel = new Channel((IFolder) subject, channelName);
						if(isSignal(channel) || isCategory(channel) || isEvent(channel)) channels.add(channel);
					}
					channelsContainer.manageChannelsCacheAfter(subject, channels);
				}
				return channels.toArray(new Channel[channels.size()]);
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
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
		int endCut = getEndCut(signal, trialNumber);
		if(frontCut > endCut) {
			Activator.logErrorMessage("Error in getYValuesForSignal() from PythonEngine.java : frontCut (" + frontCut + ") is greater than endCut (" + endCut + ")");
			return null;
		}
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
//		String expression = "docometre.experiments[\"" + fullName + ".NbSamples." + trialNumber + "\"]";
		String expression = "docometre.experiments[\"" + fullName + ".NbSamples\"][" + (trialNumber-1) + "]" ;
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return (int) Double.parseDouble(value);
	}

	@Override
	public int getFrontCut(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
//		String expression = "docometre.experiments[\"" + fullName + ".FrontCut." + trialNumber + "\"]";
		String expression = "docometre.experiments[\"" + fullName + ".FrontCut\"][" + (trialNumber-1) + "]" ;
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return (int) Double.parseDouble(value);
	}

	@Override
	public int getEndCut(Channel signal, int trialNumber) {
		String fullName = getFullPath(signal);
//		String expression = "docometre.experiments[\"" + fullName + ".EndCut." + trialNumber + "\"]";
		String expression = "docometre.experiments[\"" + fullName + ".EndCut\"][" + (trialNumber-1) + "]" ;
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		return (int) Double.parseDouble(value);
	}

	@Override
	public void runScript(String code, boolean runInMainThread) {
		pythonController.getPythonEntryPoint().runScript(code, runInMainThread);
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
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
		if(nbMarkersGroups == 1) expression = "docometre.experiments['" + channelName + ".MarkersGroupsLabels'] = ['" + markersGroupLabel + "']";
		else expression ="docometre.experiments['" + channelName + ".MarkersGroupsLabels'].append('" + markersGroupLabel + "')";
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
		expression = "docometre.experiments[\"" + channelName + ".MarkersGroup_"+ markersGroupLabel + "_Values\"] = numpy.zeros((0,3));";
		pythonController.getPythonEntryPoint().runScript(expression, false);
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
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
		// decrease nb markers groups
		int nbMarkersGroups = getNbMarkersGroups(signal);
		nbMarkersGroups--;
		expression = "docometre.experiments[\"" + channelName + ".NbMarkersGroups\"] = " + nbMarkersGroups;
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
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
		pythonController.getPythonEntryPoint().runScript(expression, false);
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
		pythonController.getPythonEntryPoint().runScript(expression, false);
		double[][] values = getMarkers(markersGroupLabel, signal);
		int markersGroupNumber = getMarkersGroupNumber(markersGroupLabel, signal) + 1;
		if(values.length == 0) deleteMarkersGroup(markersGroupNumber, signal);
		signal.setModified(true);
		
	}
	
	@Override
	public int getNbFeatures(Channel signal) {
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + channelName + ".NbFeatures\"]";
		String NbFeatures = pythonController.getPythonEntryPoint().evaluate(expression);
		return Integer.parseInt(NbFeatures);
	}

	@Override
	public String getFeatureLabel(int featureNumber, Channel signal) {
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments['" + channelName + ".FeaturesLabels'][" + (featureNumber-1) + "]";
		return pythonController.getPythonEntryPoint().evaluate(expression);
	}

	@Override
	public void deleteFeature(int featureNumber, Channel signal) {
		String channelName = getFullPath(signal);
		
		// Get feature label
		String featureLabel = getFeatureLabel(featureNumber, signal);
		
		// Remove feature label
		String expression = "docometre.experiments['" + channelName + ".FeaturesLabels'].pop(" + (featureNumber - 1) + ")";
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
		// decrease nb features
		int nbFeatures = getNbFeatures(signal);
		nbFeatures--;
		expression = "docometre.experiments[\"" + channelName + ".NbFeatures\"] = " + nbFeatures;
		pythonController.getPythonEntryPoint().runScript(expression, false);
		
		// Remove feature values
		String experimentName = signal.getFullPath().segment(0);
		String subjectName = signal.getFullPath().segment(1);
		channelName = signal.getFullPath().segment(2);
		channelName = experimentName + "\\." + subjectName + "\\." + channelName;
		expression = channelName + "\\.Feature_" + featureLabel; 
		pythonController.getPythonEntryPoint().unload(expression);
		signal.setModified(true);
		
	}

	@Override
	public double[][] getFeature(String featureLabel, Channel signal) {
		if("".equals(featureLabel)) return new double[0][0];
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + channelName + ".Feature_" + featureLabel + "_Values\"]";
		byte[] byteValues = pythonController.getPythonEntryPoint().getVector(expression, PythonEntryPoint.DATA_TYPE_DOUBLE, -1, -1, -1);
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteValues);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    	DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
    	double[] values = new double[doubleBuffer.capacity()];
    	doubleBuffer.get(values);
    	int featureDimension = getFeatureDimension(featureLabel, signal);
    	double[][] featuresValues = new double[values.length / featureDimension][featureDimension];
    	for (int i = 0; i < values.length; i++) {
    		featuresValues[i / featureDimension][i % featureDimension] = values[i];
		}
    	return featuresValues;
	}

	private int getFeatureDimension(String featureLabel, Channel signal) {
		if("".equals(featureLabel)) return 1;
		String channelName = getFullPath(signal);
		String expression = "docometre.experiments[\"" + channelName + ".Feature_" + featureLabel + "_Values\"][0].ndim";
		String value = pythonController.getPythonEntryPoint().evaluate(expression);
		int intValue = Integer.parseInt(value);
		if(intValue == 0) return 1;
		expression = "len(docometre.experiments[\"" + channelName + ".Feature_" + featureLabel + "_Values\"][0])";
		value = pythonController.getPythonEntryPoint().evaluate(expression);
		intValue = Integer.parseInt(value);
		return intValue;
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
		if(!exist("createdOrModifiedChannels")) return new IResource[0];
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

	@Override
	public String getErrorMessages() {
		try {
			if(!exist("ErrorMessages")) return null;
			String errorMessages = "\n" + evaluate("docometre.experiments['ErrorMessages']");
			errorMessages = errorMessages.replaceAll("\\|", "\n");
			return errorMessages;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} finally {
			evaluate("docometre.experiments.pop('ErrorMessages', None)");
		}
		return null;
	}

	@Override
	public String[] getLoadedSubjects() {
		String loadedSubjects = pythonController.getPythonEntryPoint().getLoadedSubjects();
		if("".equals(loadedSubjects)) return new String[0];
		return loadedSubjects.split(":");
	}

	@Override
	public String getCommentCharacter() {
		return "#";
	}

	@Override
	public String getCommandLineToLoadSubjectFromRawData(IResource subject) throws Exception {	
		String loadName = getFullPath(subject);
		String dataFilesList = Analyse.getDataFiles(subject);
		//String dataFilesList = (String)subject.getSessionProperty(ResourceProperties.DATA_FILES_LIST_QN);
		boolean isOptitrack = Analyse.isOptitrack(dataFilesList.split(";"), (IContainer) subject);
		if(isOptitrack) {
			// If all data files are OPTITRACK_TYPE_1
			return "docometre.loadData(\"OPTITRACK_TYPE_1\", \"" + loadName + "\", r\"" + dataFilesList + "\", None);";
		} else {
			Map<String, String> sessionsProperties = Analyse.getSessionsInformations(subject);
			String sessionsPropertiesString = sessionsProperties.toString().replaceAll("\\{", "{\"");
			sessionsPropertiesString = sessionsPropertiesString.replaceAll("=", "\":");
			sessionsPropertiesString = sessionsPropertiesString.replaceAll(", ", ",\"");
			return "docometre.loadData(\"DOCOMETRE\", \"" + loadName + "\", r\"" + dataFilesList + "\", " + sessionsPropertiesString + ");\n";
		}
	}

	@Override
	public String getCommandLineToUnloadSubject(IResource resource) throws Exception {
		if(!(ResourceType.isSubject(resource) || ResourceType.isExperiment(resource))) return "";
		String prefixKey = "";
		if(ResourceType.isSubject(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			String subjectName = resource.getFullPath().segment(1);
			prefixKey = experimentName + "\\." + subjectName + "\\.";
			
		}
		if(ResourceType.isExperiment(resource)) {
			String experimentName = resource.getFullPath().segment(0);
			prefixKey = experimentName + "\\.";
		}
		return "docometre.unload(\"" + prefixKey + "\")";
	}

}
