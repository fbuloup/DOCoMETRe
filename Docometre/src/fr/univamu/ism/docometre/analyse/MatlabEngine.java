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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

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

	private boolean loadFromSavedFile;

	private String matlabLocation;
	
	public MatlabEngine() {
		matlabController = MatlabController.getInstance();
	}

	@Override
	public IStatus startEngine(IProgressMonitor monitor) {
		Activator.logInfoMessage(DocometreMessages.MathEngineStarting, MatlabController.class);
		
		matlabLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_LOCATION);
		String matlabScriptsLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION);
		final IPath matlabFunctionsLocation = Path.fromOSString(matlabScriptsLocation).removeLastSegments(1).append("MatlabFunctions");
		
		// Set default matlab path if empty
		if("".equals(matlabLocation)) matlabLocation = "matlab";
		
		int timeOut = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.MATLAB_TIME_OUT);
		boolean showWindow = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_MATLAB_WINDOW);
		
		Job startMatlabInnerJob = new Job(DocometreMessages.WaitingForMathEngine) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				try {
					matlabController.startMatlab(showWindow, timeOut, matlabLocation, matlabScriptsLocation, matlabFunctionsLocation.toOSString());
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
		String message = DocometreMessages.MathEngineStarted + " (Matlab)";
		if(startMatlabInnerJob.getResult() != null && startMatlabInnerJob.getResult().isOK()) {
			Activator.logInfoMessage(getMatlabVersion(), MatlabController.class);
			Activator.logInfoMessage(message, MatlabController.class);
		}
		return startMatlabInnerJob.getResult();
	}
	
	private String getMatlabVersion() {
		try {
			Object[] responses = matlabController.returningEval("version", 1);
			Object response = responses[0];
			if(response instanceof String) {
				String message = "Matlab version : " + response.toString();
				return message;
			}
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return "";
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
	public void load(IResource subject, boolean loadFromSavedFile) {
		try {
			if(!ResourceType.isSubject(subject)) return;
			
			String experimentName = subject.getFullPath().segment(0);
			String subjectName = subject.getFullPath().segment(1);
			
			String workpsacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			String fileName = workpsacePath + File.separator + experimentName + File.separator + subjectName + File.separator + "save.mat";
			File saveFile = new File(fileName);
			
			MatlabEngine.this.loadFromSavedFile = loadFromSavedFile;
			if(saveFile.exists() && !loadFromSavedFile) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						String message = NLS.bind(DocometreMessages.LoadSubjectFromRawDataDialog_Message, subject.getFullPath().toPortableString());
						MatlabEngine.this.loadFromSavedFile = !MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.LoadSubjectFromRawDataDialog_Title, message);
					}
				});
				
			}
			
			if(saveFile.exists() && MatlabEngine.this.loadFromSavedFile) {
				String cmd = "load '" + fileName + "';";
				matlabController.eval(cmd);
				cmd = experimentName + "." + subjectName + " = subjectName;";
				matlabController.eval(cmd);
				cmd = "clear subjectName;"; 
				matlabController.eval(cmd);
				
			} else {
				String dataFilesList = Analyse.getDataFiles(subject);
				
				boolean isOptitrack = Analyse.isOptitrack(dataFilesList.split(";"), (IContainer) subject);
				if(isOptitrack) {
					// If all data files are OPTITRACK_TYPE_1
					String cmd = experimentName + "." + subjectName + " = loadData('OPTITRACK_TYPE_1', '" + dataFilesList + "')";
					matlabController.eval(cmd);
				} else {
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
					System.out.println(cmd);
					matlabController.eval(cmd);
				}
				
			}
			
			MathEngine.super.load(subject, loadFromSavedFile);
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void unload(IResource resource) {
		try {
			if(!(ResourceType.isSubject(resource) || ResourceType.isExperiment(resource))) return;
			if(ResourceType.isSubject(resource)) {
				String experimentName = resource.getFullPath().segment(0);
				String subjectName = resource.getFullPath().segment(1);
				String cmd = experimentName + " = rmfield(" + experimentName + ", '" + subjectName + "');";
				matlabController.eval(cmd);
			}
			if(ResourceType.isExperiment(resource)) {
				String experimentName = resource.getFullPath().segment(0);
				String cmd = "clear " + experimentName + ";";
				matlabController.eval(cmd);
			}
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public Channel[] getChannels(IResource subject) {
		try {
			if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
				ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				ArrayList<Channel> channels = channelsContainer.manageChannelsCacheBefore();
				if(channelsContainer.updateChannelsCache()) {
					channels.clear();
					String expression = getFullPath(subject);
					Activator.logInfoMessage(NLS.bind(DocometreMessages.UpdateCacheChannels, expression), MatlabEngine.class);
					Object[] responses = matlabController.returningEval("fieldnames(" + expression + ")", 1);
					Object response = responses[0];
					String[] channelsNames = (String[]) response;
					for (String channelName : channelsNames) {
						Channel channel = new Channel((IFolder) subject, channelName);
						if(isSignal(channel) || isCategory(channel) || isEvent(channel)) channels.add(channel);
					}
					channelsContainer.manageChannelsCacheAfter(subject, channels);
				}
				return channels.toArray(new Channel[channels.size()]);
			}
			
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
			int endCut = getEndCut(signal, trialNumber);
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
			channel.setModified(true);
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
			signal.setModified(true);
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
			signal.setModified(true);
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
			signal.setModified(true);
			
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

			signal.setModified(true);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void saveSubject(IResource subject) {
		try {
			// Copy subject's data to 'subjectName' variable 
			String fullSubjectName = getFullPath(subject);
			String experimentName = subject.getFullPath().segment(0);
			String subjectName = subject.getFullPath().segment(1);
			String cmd = "subjectName = " + fullSubjectName + ";"; 
			matlabController.eval(cmd);
			// Save 'subjectName' variable to file
			String workpsacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			String fileName = workpsacePath + File.separator + experimentName + File.separator + subjectName + File.separator + "save.mat";
			cmd = "save('" + fileName + "', 'subjectName', '-v7.3');";			
			matlabController.eval(cmd);
			// Clear 'subjectName' variable
			cmd = "clear subjectName;";
			matlabController.eval(cmd);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public boolean renameExperiment(String oldName, String newName) {
		try {
			matlabController.eval(newName + " = " + oldName + ";");
			matlabController.eval("clear " + oldName + ";");
			
			double response1 = 0;
			Object[] responses = matlabController.returningEval("exist('" + oldName + "','var')", 1);
			Object response = responses[0];
			if(response instanceof double[]) {
				double[] values = (double[])response;
				response1 = values[0];
			}
			
			double response2 = 0;
			responses = matlabController.returningEval("exist('" + newName + "','var')", 1);
			response = responses[0];
			if(response instanceof double[]) {
				double[] values = (double[])response;
				response2 = values[0];
			}
			
			return response1 == 0 && response2 == 1;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		return false;
		
	}

	@Override
	public boolean renameSubject(String experimentName, String oldSubjectName, String newSubjectName) {
		try {
			String cmd = experimentName + " = " + "rnfield(" + experimentName + ",'" + oldSubjectName +"','" + newSubjectName + "');";
			matlabController.eval(cmd);
			boolean oldNameFound = false;
			boolean newNameFound = false;
			Object[] responses = matlabController.returningEval("fieldnames(" + experimentName + ")", 1);
			Object response = responses[0];
			String[] subjectsNames = (String[]) response;
			for (int i = 0; i < subjectsNames.length; i++) {
				if(subjectsNames[i].equals(newSubjectName)) newNameFound = true;
				if(subjectsNames[i].equals(oldSubjectName)) oldNameFound = true;
			}
			
			return newNameFound && !oldNameFound;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		
		return false;
	}

	@Override
	public String evaluate(String command) throws Exception {
		long timeStamp = (new Date()).getTime();
		command = command.replaceAll("'", "''");
		String variable = "Var_" + timeStamp;
		command = variable + " = evalc('" + command + "');";
		runScript(command);
		Object value = matlabController.getVariable(variable);
		runScript("clear " + variable + ";");
		String stringValue = value.toString();
		return stringValue.replaceAll("<a.*\">", "").replaceAll("</a>", "");
	}

	@Override
	public IResource[] getCreatedOrModifiedSubjects() {
		if(!exist("createdOrModifiedChannels")) return new IResource[0];
		Set<IResource> createdOrModifiedSubjects = new HashSet<>();
		try {
			String[] createdOrModifiedChannels = null;
			Object createdOrModifiedChannelsObject = matlabController.getVariable("createdOrModifiedChannels");
			if(createdOrModifiedChannelsObject instanceof String) {
				createdOrModifiedChannels = new String[] {(String)createdOrModifiedChannelsObject};
			}
			if(createdOrModifiedChannelsObject instanceof String[]) {
				createdOrModifiedChannels = (String[])createdOrModifiedChannelsObject;
			}
			if(createdOrModifiedChannels != null) {
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				for (String createdOrModifiedChannel : createdOrModifiedChannels) {
					String[] segments = createdOrModifiedChannel.split("\\.");
					String subjectPath = segments[0] + "/" + segments[1];
					IResource subject = workspaceRoot.findMember(subjectPath);
					createdOrModifiedSubjects.add(subject);
				}
				matlabController.evaluate("clear createdOrModifiedChannels;");
			}
			return createdOrModifiedSubjects.toArray(new IResource[createdOrModifiedSubjects.size()]);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return new IResource[0];
	}

	@Override
	public String getErrorMessages() {
		if(exist("ErrorMessages")) {
			try {
				String errorMessages = "\n";
				String cmd = "ErrorMessagesSize = size(ErrorMessages); nbErrorMessages = ErrorMessagesSize(2)";
				matlabController.eval(cmd);
				Object response = matlabController.getVariable("nbErrorMessages");
				int nbErrorMessages = (int) ((double[]) response)[0];
				cmd = "clear ErrorMessagesSize nbErrorMessages;";
				matlabController.eval(cmd);
				for (int i = 1; i <= nbErrorMessages; i++) {
					Object[] error = matlabController.returningEval("ErrorMessages{" + i + "}", 1);
					errorMessages = errorMessages + error[0] + "\n" ;
				}
				matlabController.eval("clear ErrorMessages;");
				return errorMessages;
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public int getNbFeatures(Channel signal) {
		try {
			if(!isStarted()) return 0;
			String channelName = getFullPath(signal);
			String expression = channelName + ".NbFeatures;";
			Object[] responses = matlabController.returningEval(expression, 1);
			int nbFeatures = (int) ((double[]) responses[0])[0];
			return nbFeatures;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public String getFeatureLabel(int featureNumber, Channel signal) {
		try {
			String fullSignalName = getFullPath(signal);
			Object[] responses = matlabController.returningEval(fullSignalName + ".FeaturesLabels{" + featureNumber + "}", 1);
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
	public void deleteFeature(int featureNumber, Channel signal) {
		try {
			String fullSignalName = getFullPath(signal);
			
			// Get feature label
			String featureLabel = getFeatureLabel(featureNumber, signal);
			
			// Remove feature label
			String expression = fullSignalName + ".FeaturesLabels(" + featureNumber + ") = []";
			matlabController.eval(expression);
			
			// Remove feature values
			expression = fullSignalName + " = rmfield(" + fullSignalName + ", 'Feature_" + featureLabel + "_Values')";
			matlabController.eval(expression);
			
			// decrease nb features
			int nbFeatures = getNbFeatures(signal);
			nbFeatures--;
			expression = fullSignalName + ".NbFeatures = " + nbFeatures;
			matlabController.eval(expression);
			signal.setModified(true);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public double[] getFeature(String featureLabel, Channel signal) {
		try {
			if("".equals(featureLabel)) return new double[0];
			String fullSignalName = getFullPath(signal);
			String expression = fullSignalName + ".Feature_" + featureLabel + "_Values";
			Object[] responses = matlabController.returningEval(expression, 1);
			Object response = responses[0];
			if(response instanceof double[]) {
				return (double[])response;
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		return new double[0];
	}

	@Override
	public String[] getLoadedSubjects() {
		try {
			matlabController.eval("clear ans;");
			ArrayList<String> loadedSubjects = new ArrayList<>();
			long timeStamp = (new Date()).getTime();
			String variablesString = "Var_" + timeStamp;
			String cmd = variablesString + " = who;";
			matlabController.eval(cmd);
			Object response = matlabController.getVariable(variablesString);
			cmd = "clear " + variablesString + ";";
			matlabController.eval(cmd);
			if(response instanceof String[]) {
				String[] variables = (String[]) response;
				for (String variable : variables) {
					if(isStruct(variable)) {
						Object[] responses = matlabController.returningEval("fieldnames(" + variable + ")", 1);
						response = responses[0];
						String[] subjectsNames = (String[]) response;
						for (String subjectName : subjectsNames) {
							loadedSubjects.add(variable + "." + subjectName);
						}
					}
				}
			}
			return loadedSubjects.toArray(new String[loadedSubjects.size()]);
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}

}
