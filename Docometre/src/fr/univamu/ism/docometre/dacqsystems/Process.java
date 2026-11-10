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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.ParametersFile.TrialValues;
import fr.univamu.ism.docometre.dacqsystems.adwin.DiaryLogLevel;
import fr.univamu.ism.docometre.dacqsystems.ui.ParametersDialog;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;

/**
 * The class <b>Process</b> holds the behavior of the acquired, processed and generated signals.
 * It depends on a DACQ general configuration.
 */
public abstract class Process extends AbstractElement implements ModuleBehaviour, PropertyObserver {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	public static double processEndTime = -1;
	
	// Size of the diary buffer (see preExecute)
	private static final int DIARY_BUFFER_SIZE = 64*1024;
	
	private Script script;
	
	private transient DACQConfiguration dacqConfiguration;
	protected transient int currentDiaryLine;
	protected transient HashMap<Integer, String> errorMarkers;

	private transient IResource logFile;

	private transient IResource outputFolder;

	protected boolean deletionDecision;
	
	// Parameters of the current trial or process test (see prepareParameters), in the order of
	// DACQConfiguration.getParameters() : values as read in the parameters file or entered by the user
	private transient LinkedHashMap<ParameterVariable, String> currentTrialParameters;
	// Parameters file of the session of the current trial (null if no parameter)
	private transient IResource parametersFile;
	// Number of the current trial
	private transient int currentTrialNumber;
	// Response of the parameters dialog (process test)
	private transient int parametersDialogResponse;

	public ScriptSegment getInitializeBlocksContainer() {
		return getScript().getInitializeBlocksContainer();
	}

	public ScriptSegment getLoopBlocksContainer() {
		return getScript().getLoopBlocksContainer();
	}

	public ScriptSegment getFinalizeBlocksContainer() {
		return getScript().getFinalizeBlocksContainer();
	}
	
	protected Process() {
		// TODO Auto-generated constructor stub
	}
	
	public Script getScript() {
		if(script == null) script = new Script();
		return script;
	}
	
	/**
	 * Return the DAQ configuration associated to this process
	 * @return the DAQ associated configuration 
	 */
	public DACQConfiguration getDACQConfiguration() {
		if(dacqConfiguration != null) return dacqConfiguration;
		dacqConfiguration = (DACQConfiguration) ObjectsController.getDACQConfiguration(this);
		if(dacqConfiguration != null) ObjectsController.addHandle(dacqConfiguration);
		return dacqConfiguration;
	}
	
	/**
	 * 
	 * @param daqConfigurationResource
	 */
	public void resetDACQConfiguration() {
		dacqConfiguration = null;
	}
	
	/**
	 * This method returns the DACQ code corresponding to the module or to the whole DACQ system
	 * @return the DACQ code corresponding to the module or to the whole DACQ system.
	 * @param module The module from which we need the code. If null, the code corresponding to the whole DACQ system is returned.
	 */
	public abstract String getCode(ModuleBehaviour module) throws Exception;
	
	/**
	 * This method compile the generated code
	 * @param code the code to be compiled
	 */
	public abstract void compile(IProgressMonitor progressMonitor) throws Exception;
	
	/**
	 * This method clean/remove compile/build files and folders
	 */
	public abstract void cleanBuild();
	
	/**
	 * This method can be used as a helper method to prepare the DAQ execution which can be launched by calling method {@link #execute(boolean)}
	 * @param wait true if the method must wait the end of the process to continue
	 * @param compile true if a compilation is needed
	 * @throws Exception 
	 */
	public abstract void run(boolean compile, String prefix, String suffix) throws Exception;
	
	/**
	 * This method ends the DAQ processing
	 * @throws Exception 
	 */
	public abstract void stop() throws Exception;
	
	/**
	 * This method starts the DAQ processing
	 * @param wait true if the method wait the end of process to continue
	 * @throws Exception 
	 */
	public abstract Job execute(String prefix, String suffix) throws Exception;
	
	/**
	 * Return current process
	 * @return the process
	 */
	public Process getCurrentProcess() {
		return this;
	}
	
	public void appendToEventDiary(String event) {
		currentDiaryLine++;
		Logger.getLogger(Process.class).log(DiaryLogLevel.DIARY, event);
	}
	
	public void appendErrorMarkerAtCurrentDiaryLine(String message) {
		errorMarkers.put(currentDiaryLine, message);
	}
	
	private void createErrorMarkers() {
		try {
			for (Map.Entry<Integer, String> entry : errorMarkers.entrySet()) {
				int lineNumber = entry.getKey();
				String message = entry.getValue();
				IMarker marker = logFile.createMarker(DocometreBuilder.MARKER_ID);
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	public IFolder getOutputFolder() {
		return (IFolder) outputFolder;
	}
	
	public void preExecute(IResource resource) throws Exception {
		// Initialize folder output
		IResource parent = resource;
		outputFolder = parent;
		if(ResourceType.isProcess(resource)) {
			// This is a process test
			parent = resource.getParent();
			outputFolder = ((IContainer)parent).findMember("test." + resource.getName().replaceAll(Activator.processFileExtension, ""));
			boolean askDeletion = false;
			if(outputFolder != null) {
				askDeletion = true;
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						String message = NLS.bind(DocometreMessages.DeleteProcessTestDialogMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
						deletionDecision = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.DeleteProcessTestDialogTitle, message);
						if(deletionDecision) ApplicationActionBarAdvisor.deleteResourcesAction.runOnThisResources(new IResource[] {outputFolder});
					}
				});
			}
			if(askDeletion && !deletionDecision) throw new OperationCanceledException(DocometreMessages.OperationCanceledByUser);
			if(askDeletion && deletionDecision && outputFolder.exists()) {
				String message = NLS.bind(DocometreMessages.OutputFolderStillExists, outputFolder.getFullPath().toOSString());
				throw new IOException(message + " still exists !");
			}
			if(outputFolder == null || !outputFolder.exists()) {
				outputFolder = ((IContainer)parent).getFolder(new Path("test." + resource.getName().replaceAll(Activator.processFileExtension, "")));
				((IFolder)outputFolder).create(true, true, null);
				ResourceProperties.setTypePersistentProperty(outputFolder, ResourceType.PROCESS_TEST.toString());
				ResourceProperties.setAssociatedProcessProperty(outputFolder, resource.getFullPath().toOSString());
			}
		}
		// Clear errors
		if(errorMarkers != null) errorMarkers.clear();
		else errorMarkers = new HashMap<>(0);
		// Initialize diary
		currentDiaryLine = 0;
		String fileName = DocometreMessages.Diary + Activator.logFileExtension;
		if(ResourceType.isProcess(resource)) fileName = resource.getName().replaceAll(Activator.processFileExtension, "") + "_" + DocometreMessages.Diary + Activator.logFileExtension;
		logFile = ((IContainer)parent).findMember(fileName);
		if(logFile != null) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					Activator.closeEditor(logFile);
				}
			});
			logFile.delete(true, null);
			logFile.refreshLocal(IResource.DEPTH_ZERO, null);
		}
//		logFile = ((IContainer)parent).getFile(new Path(fileName));
		fileName = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + parent.getFullPath().toOSString() + File.separator + fileName;
		//Files.write(Paths.get(fileName), Arrays.asList(new String[] {"#### DIARY"}), StandardCharsets.UTF_8);
		Logger logger = Logger.getLogger(Process.class);
		logger.removeAllAppenders();
		PatternLayout layout = new PatternLayout("%m%n");
		FileAppender fileAppender = new FileAppender();
		fileAppender.setName(resource.getFullPath().toOSString() + " Diary");
		fileAppender.setFile(fileName);
		fileAppender.setLayout(layout);
		fileAppender.setThreshold(DiaryLogLevel.DIARY);
		fileAppender.setAppend(true);
		fileAppender.setEncoding("UTF-8");
		// Buffered diary : during the trial, the real time loop writes several lines per
		// iteration. Without buffer, each line is written and flushed to the disk
		// immediately. The buffer is flushed when the appender is closed (see refreshLogFile).
		fileAppender.setBufferedIO(true);
		fileAppender.setBufferSize(DIARY_BUFFER_SIZE);
		fileAppender.setImmediateFlush(false);
		fileAppender.activateOptions();
		logger.addAppender(fileAppender);
		// Parameters values of the trial or process test
		prepareParameters(resource);
	}
	
	private void refreshLogFile(IResource resource) throws Exception {
		Logger logger = Logger.getLogger(Process.class);
		if(logger == null) return;
		Appender appender = logger.getAppender(resource.getFullPath().toOSString() + " Diary");
		if(appender != null) appender.close();
		IResource parent = resource;
		String fileName = DocometreMessages.Diary + Activator.logFileExtension;
		if(ResourceType.isProcess(resource)) {
			parent = resource.getParent();
			fileName = resource.getName().replaceAll(Activator.processFileExtension, "") + "_" + DocometreMessages.Diary + Activator.logFileExtension;
		}
		logFile = ((IContainer)parent).getFile(new Path(fileName));
		logFile.refreshLocal(IResource.DEPTH_ZERO, null);
		ResourceProperties.setTypePersistentProperty(logFile, ResourceType.LOG.toString());
		// Set system property to log file
		String dacqFullPath = "";
		if(ResourceType.isProcess(resource)) dacqFullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
		else if(ResourceType.isTrial(resource)) {
			IResource process = ResourceProperties.getAssociatedProcess(resource);
			dacqFullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(process);
		}
		IResource associatedDACQFile = ResourcesPlugin.getWorkspace().getRoot().findMember(dacqFullPath);
		String system = ResourceProperties.getSystemPersistentProperty(associatedDACQFile);
		ResourceProperties.setSystemPersistentProperty(logFile, system);
		createErrorMarkers();
	}
	
	/**
	 * Called after the execution of the process, before the next one can be started.
	 * All the workspace modifications of {@link #postExecuteInWorkspaceOperation(IResource)}
	 * are done in a single workspace operation : one resource change notification
	 * (deferred) instead of one per modification.
	 * This method returns when all these modifications are done.
	 */
	public void postExecute(IResource resource) throws Exception {
		Exception[] failure = new Exception[1];
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRunnable postExecuteRunnable = monitor -> {
			try {
				postExecuteInWorkspaceOperation(resource);
			} catch (Exception e) {
				failure[0] = e;
			}
		};
		workspace.run(postExecuteRunnable, resource.getProject(), IWorkspace.AVOID_UPDATE, null);
		if(failure[0] != null) throw failure[0];
	}
	
	/**
	 * Workspace modifications done after the execution of the process
	 * (diary refresh and markers by default). Runs within the single workspace
	 * operation of {@link #postExecute(IResource)} : sub classes override this method
	 * to add their own modifications to this operation.
	 */
	protected void postExecuteInWorkspaceOperation(IResource resource) throws Exception {
		refreshLogFile(resource);
		writePropagatedParameters(resource);
	}
	
	/**
	 * Values of the parameters of the current trial or process test, prepared by {@link #preExecute(IResource)},
	 * to be sent to the DACQ system by the sub class before the start of the process.
	 * @return the parameters and their values (as read in the parameters file or entered by the user),
	 * in the order of {@link DACQConfiguration#getParameters()}
	 */
	protected Map<ParameterVariable, String> getCurrentTrialParameters() {
		if(currentTrialParameters == null) return Collections.emptyMap();
		return Collections.unmodifiableMap(currentTrialParameters);
	}
	
	/**
	 * Checks the value of a parameter before the start of the process (called by {@link #preExecute(IResource)}).
	 * Does nothing by default.
	 * @param parameter the parameter
	 * @param value its value, as read in the parameters file or entered by the user (may be null for a process test)
	 * @throws Exception if the value can't be used by the DACQ system : the process is not started
	 */
	protected void checkParameterValue(ParameterVariable parameter, String value) throws Exception {
	}
	
	/**
	 * Values of the parameters at the end of the trial, read from the DACQ system. Called after the execution
	 * of a trial when there are propagated parameters and a line of values for the next trial in the parameters file.
	 * Only the values of the propagated parameters are written in the parameters file, as returned by this method.
	 * @return the parameters and their values, null if they can't be read : the parameters file is then not modified
	 * @throws Exception
	 */
	protected abstract Map<ParameterVariable, String> getPropagatedParametersValues() throws Exception;
	
	private void prepareParameters(IResource resource) throws Exception {
		if(currentTrialParameters == null) currentTrialParameters = new LinkedHashMap<>();
		currentTrialParameters.clear();
		// Init parameters from parameters file if resource is a trial
		if(ResourceType.isTrial(resource)) prepareTrialParameters(resource);
		// Init parameters from parameters dialog if resource is a process (process test)
		if(ResourceType.isProcess(resource)) prepareProcessTestParameters(resource);
	}
	
	private void prepareTrialParameters(IResource resource) throws Exception {
		// Get trial number from trial name
		currentTrialNumber = Integer.parseInt(resource.getName().split("°")[1]);
		ParameterVariable[] parameters = getDACQConfiguration().getParameters();
		if(parameters.length == 0) return;
		// There are parameters, try to get their values for current trial from parameters file
		parametersFile = null;
		IResource[] members = resource.getParent().members();
		for (IResource member : members) {
			if(ResourceType.isParameters(member)) {
				parametersFile = member;
				break;
			}
		}
		if(parametersFile == null) throw new Exception(DocometreMessages.NoParameterFileFound);
		String parametersFilePath = parametersFile.getLocation().toOSString();
		List<String> lines = Files.readAllLines(Paths.get(parametersFilePath), StandardCharsets.UTF_8);
		TrialValues trialValues = ParametersFile.getTrialValues(lines, currentTrialNumber);
		if(trialValues == null) throw new Exception(NLS.bind(DocometreMessages.ParameterValueNotfound, currentTrialNumber, parametersFilePath));
		String[] values = new String[parameters.length];
		StringBuilder parametersString = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			int index = trialValues.indexOf(parameters[i].getParameterName());
			if(index == -1) throw new Exception(NLS.bind(DocometreMessages.ParameterNameNotfound, parameters[i].getParameterName(), parametersFilePath));
			values[i] = trialValues.getValue(index);
			if(values[i] == null) throw new Exception(NLS.bind(DocometreMessages.ParameterValueNotfound, currentTrialNumber, parametersFilePath));
			if(i > 0) parametersString.append(" - ");
			parametersString.append(trialValues.getName(index)).append(" = ").append(values[i]);
		}
		Activator.logInfoMessage("Preparing paramaters array (parsing values) for trial (" + resource.getName() + ") : " + parametersString, getClass());
		for (int i = 0; i < parameters.length; i++) {
			checkParameterValue(parameters[i], values[i]);
			currentTrialParameters.put(parameters[i], values[i]);
		}
	}
	
	private void prepareProcessTestParameters(IResource resource) throws Exception {
		DACQConfiguration dacqConfiguration = getDACQConfiguration();
		ParameterVariable[] parameters = dacqConfiguration.getParameters();
		if(parameters.length == 0) return;
		parametersDialogResponse = Dialog.OK;
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ParametersDialog parametersDialog = new ParametersDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), parameters);
				parametersDialog.setBlockOnOpen(true);
				parametersDialogResponse = parametersDialog.open();
			}
		});
		if(parametersDialogResponse == Dialog.CANCEL) throw new Exception(DocometreMessages.ProcessLaunchCanceled);
		ObjectsController.serialize(dacqConfiguration);
		StringBuilder parametersString = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			if(i > 0) parametersString.append(" - ");
			parametersString.append(parameters[i].getParameterName()).append(" = ").append(parameters[i].getParameterValue());
		}
		Activator.logInfoMessage("Preparing parameters array (parsing values) for process test (" + resource.getName() + ") : " + parametersString, getClass());
		for (ParameterVariable parameter : parameters) {
			String value = parameter.getParameterValue();
			checkParameterValue(parameter, value);
			currentTrialParameters.put(parameter, value);
		}
	}
	
	/*
	 * Writes the values of the propagated parameters, at the end of the trial, in the line of the next trial
	 * of the parameters file
	 */
	private void writePropagatedParameters(IResource resource) throws Exception {
		if(!ResourceType.isTrial(resource)) return;
		ParameterVariable[] propagatedParameters = getDACQConfiguration().getPropagatedParameters();
		if(propagatedParameters.length == 0 || parametersFile == null) return;
		List<String> lines = Files.readAllLines(Paths.get(parametersFile.getLocationURI()), StandardCharsets.UTF_8);
		if(!ParametersFile.hasTrialLine(lines, currentTrialNumber + 1)) return;
		Map<ParameterVariable, String> values = getPropagatedParametersValues();
		if(values == null) return;
		Map<String, String> valuesByName = new LinkedHashMap<>();
		for (Map.Entry<ParameterVariable, String> entry : values.entrySet()) {
			if(entry.getKey().isParameterPropagated()) valuesByName.put(entry.getKey().getParameterName(), entry.getValue());
		}
		ParametersFile.setTrialValues(lines, currentTrialNumber + 1, valuesByName);
		Files.write(Paths.get(parametersFile.getLocationURI()), String.join(System.getProperty("line.separator"), lines).getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
		parametersFile.refreshLocal(IResource.DEPTH_ZERO, null);
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub
	}

}
