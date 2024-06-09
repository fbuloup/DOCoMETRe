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
package fr.univamu.ism.docometre.dacqsystems.adwin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import de.adwin.driver.ADwinCommunicationError;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.functions.FunctionsUtil;
import fr.univamu.ism.docometre.dacqsystems.ui.ProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.ADWinDACQConfigurationEditor;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dialogs.ParametersDialog;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADWinProcessEditor;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

public class ADWinProcess extends Process {

	public static final long serialVersionUID = DACQConfiguration.serialVersionUID;
	
	public static File createADBasicFile(IResource processFile, Process process, String adbasicFilePath) {
		try {
			// Get process code
			String processCode = process.getCode(null);
			processCode = processCode.replaceAll("^\\n", "");
			
			// Create ADBasic file
			File file = new File(adbasicFilePath);
			file.getParentFile().mkdirs();
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(processCode);
			fileWriter.close();
			return file;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	transient private double startTimeDialog = 0;
	transient private double timeBefore;
	transient private double timeAfter;
	transient private LinkedHashMap<ADWinVariable, Float> currentTrialParameters = new LinkedHashMap<>();
	transient private LinkedHashMap<ADWinVariable, String> currentTrialParametersString = new LinkedHashMap<>();
	transient private IResource parametersFile;
	transient private int currentTrialNumber;
	transient private int response;

	private class RealTimeLoopJob extends Job {

		private float time = 0;
		
		public RealTimeLoopJob(String name) {
			super(name);
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				getThread().setPriority(Thread.MAX_PRIORITY);
				int processNumberInt = Integer.parseInt(ADWinProcess.this.getProperty(ADWinProcessProperties.PROCESS_NUMBER));	
				boolean active = ((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().Process_Status(processNumberInt) == 1;
				double processBeginTime = System.currentTimeMillis()/1000d;
				String date = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
				appendToEventDiary(date);
				appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_StartingAt,  ObjectsController.getResourceForObject(ADWinProcess.this).getName(), processBeginTime));
				while(active){
					
					active  = ((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().Process_Status(processNumberInt) == 1;
					
					if(startTimeDialog == 0) {
						startTimeDialog = (System.currentTimeMillis()/1000d);
						appendToEventDiary(Activator.NEW_LINE + NLS.bind(ADWinMessages.ADWinDiary_TimeBetween, "0"));
					} else {
						double currentTime = System.currentTimeMillis()/1000d;
						appendToEventDiary(Activator.NEW_LINE + NLS.bind(ADWinMessages.ADWinDiary_TimeBetween, (currentTime - startTimeDialog)));
						startTimeDialog =  currentTime;
					}
					timeBefore = System.currentTimeMillis()/1000d;
					
					//RECOVERY
//					for (int i = 0; i < ADWinProcess.this.getDACQConfiguration().getModulesNumber(); i++) {
//						ModuleBehaviour module = ADWinProcess.this.getDACQConfiguration().getModule(i);
//						module.recovery();
//					}
					ADWinProcess.this.recovery();
					timeAfter = System.currentTimeMillis()/1000d;
					appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_RecoveryTime, (timeAfter - timeBefore)));
					
					timeBefore = System.currentTimeMillis()/1000d;
					//GENERATION
//					for (int i = 0; i < ADWinProcess.this.getDACQConfiguration().getModulesNumber(); i++) {
//						ModuleBehaviour module = ADWinProcess.this.getDACQConfiguration().getModule(i);
//						module.generation();
//					}
					ADWinProcess.this.generation();
					
					timeAfter = System.currentTimeMillis()/1000d;
					appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_GenerationTime, (timeAfter - timeBefore)));
					
					//DISPLAY
					timeBefore = System.currentTimeMillis()/1000d;
					Channel[] channels = ADWinProcess.this.getDACQConfiguration().getChannels();
					for (Channel channel : channels) {
						channel.notifyChannelObservers();
					}
					for (int i = 0; i < ADWinProcess.this.getDACQConfiguration().getModulesNumber(); i++) {
						ModuleBehaviour module = ADWinProcess.this.getDACQConfiguration().getModule(i);
						for (int j = 0; j < ((Module)module).getChannelsNumber(); j++) {
							Channel channel = ((Module)module).getChannel(j);
							channel.notifyChannelObservers();
						}
					}
					timeAfter = System.currentTimeMillis()/1000d;
					appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_DisplayTime, (timeAfter - timeBefore)));
					
					time = ((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().Get_FPar(1);
					int workload = ((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().Workload();
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
//							StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//							statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, workload, time));
							ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, workload, time));
						}
					});
				}
				
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
//						StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//						statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, "0", time));
						ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, "0", time));
					}
				});
				
				// CLOSE
//				for (int i = 0; i < ADWinProcess.this.getDACQConfiguration().getModulesNumber(); i++) {
//					ModuleBehaviour module = ADWinProcess.this.getDACQConfiguration().getModule(i);
//					module.close();
//				}
				ADWinProcess.this.close();
				double processEndTime = System.currentTimeMillis()/1000d;
				
				if(ObjectsController.getResourceForObject(ADWinProcess.this) != null) {
					appendToEventDiary(Activator.NEW_LINE + NLS.bind(ADWinMessages.ADWinDiary_Ending, ObjectsController.getResourceForObject(ADWinProcess.this).getName(), processEndTime));
					appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_Approximative, ObjectsController.getResourceForObject(ADWinProcess.this).getName(), (processEndTime-processBeginTime)));
				}
				
				
			} catch (ADwinCommunicationError e) {
				appendToEventDiary(e.getMessage());
			}
			return Status.OK_STATUS;
		}
	}
	
	public ADWinProcess() {
		ADWinProcessProperties.populateProperties(this);
	}
	
	@Override
	public void appendToEventDiary(String event) {
		if(event.startsWith(Activator.NEW_LINE)) {// + ) {) {
			currentDiaryLine++;
		}
		super.appendToEventDiary(event);
	}
	
	private void deleteAdbasicFile(String filePath) {
		File file = new File(filePath);
		if (file.exists()) if (!file.delete()) Activator.logWarningMessage("Clean build. File " + file + " not deleted !");

	}
	
	@Override
	public void cleanBuild() {
		IResource processResource = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.toOSString().replaceAll(File.separator + "$", "") + processResource.getParent().getFullPath().toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource";
		String adbasicFilePath = outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".BAS");
		deleteAdbasicFile(adbasicFilePath);
	}
	
	@Override
	public void compile(IProgressMonitor progressMonitor) throws Exception {
		cleanBuild();
		if(Platform.getOS().equals(Platform.OS_WIN32)) {
			Program programEditor = Program.findProgram (".bas");
			if(programEditor == null) {
				throw new Exception("Default ADBAsic Editor and compiler not found");
			}
		}
		
		IResource processResource = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.toOSString().replaceAll(File.separator + "$", "") + processResource.getParent().getFullPath().toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource";
		File outputFolderFile = new File(outputFolder);
		outputFolderFile.mkdirs();
		
		final File errorFile = new File(outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension + "$", ".ERR"));
		
		if (errorFile.exists()) {
			if (!errorFile.canWrite()) {
				String message = "No write permission on file : " + errorFile.getAbsolutePath();
//				MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", message);
				Activator.logErrorMessage(message);
			} else if (!errorFile.delete()) {
				String message = "Delete failed - reason unknown - File : " + errorFile.getAbsolutePath();
//				MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", message);
				Activator.logErrorMessage(message);
			}
		}
		
		final File generalErrorfile = new File(outputFolder + File.separator + processResource.getParent().getFullPath().lastSegment().replaceAll("/$", "") + File.separator + "ADBASIC.ERR");
		if(generalErrorfile.exists()) generalErrorfile.delete();
		final File warningFile = new File(outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension + "$", ".WRN"));
		if(warningFile.exists()) warningFile.delete();
		
		progressMonitor.subTask(DocometreMessages.ADWinProcess_CompileCMDLineMessage);
		String adbasicFilePath = outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".BAS");
		createADBasicFile(processResource, this, adbasicFilePath);
		String adbasicCompiler = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_COMPILER);
		String cmdLine = "";
		String outputFolderCmdLine = outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", "");
		if(Platform.getOS().equals(Platform.OS_WIN32)) cmdLine = createWindowsCompileProcess(currentFolder, outputFolderCmdLine, adbasicCompiler, adbasicFilePath);
		if(Platform.getOS().equals(Platform.OS_LINUX)) cmdLine = createLinuxCompileProcess(currentFolder, outputFolderCmdLine, adbasicCompiler, adbasicFilePath);
		if(Platform.getOS().equals(Platform.OS_MACOSX)) cmdLine = createOSXCompileProcess(currentFolder, outputFolderCmdLine, adbasicCompiler, adbasicFilePath);
		progressMonitor.worked(1);
		
		java.lang.Process process = Runtime.getRuntime().exec(cmdLine);
		
		String line;
//			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//			String errorString = "";
//			while((line = error.readLine()) != null){
//				errorString = errorString + line + "\n";
//			}
//			error.close();
//			if(!errorString.equals("")) {
//				IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
//				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
//				marker.setAttribute(IMarker.MESSAGE, errorString);
//				throw new Exception(DocometreMessages.ADWinProcess_CompileErrorsMessage + "\n" + errorString); 
//			}
		boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
		boolean firstLine = true;
		BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while((line=input.readLine()) != null){
			if(useDocker && firstLine) line += " (docker container ID)";
		    if(!"adwin".equals(line)) {
			    Activator.logInfoMessage(line, getClass());
		    }
		    firstLine = false;
		}
		input.close();
		
		boolean processError = false;
		input = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		while((line=input.readLine()) != null){
			if(!line.contains("0 error(s), 0 warning(s)")) {
				System.err.println("Error : " + line);
			    Activator.logErrorMessage(line);
			    processError = !line.contains(":err:explorer:initialize_display_settings Failed to query current display settings for");
			}
		}
		input.close();
		process.waitFor();
		
//			ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
//			processBuilder.inheritIO();
//			processBuilder.directory(outputFolderFile);
//			processBuilder.start().waitFor();
		
		
		progressMonitor.worked(1);
		
		
		processResource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		ADWinDACQConfiguration dacqConfiguration = (ADWinDACQConfiguration) ObjectsController.getDACQConfiguration(this);
		IResource dacqConfigurationResource = ObjectsController.getResourceForObject(dacqConfiguration);
		dacqConfigurationResource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		
		//Get compile errors
		progressMonitor.subTask(DocometreMessages.ADWinProcess_GetCompileErrorsMessage);
		if(errorFile.exists()) {
			createMarker(IMarker.SEVERITY_ERROR, processResource, errorFile);
		}
		//Get more general compile errors
		if(generalErrorfile.exists()) {
			createMarker(IMarker.SEVERITY_ERROR, processResource, generalErrorfile);
		}
		//Get compile warnings
		if(warningFile.exists()) {
			createMarker(IMarker.SEVERITY_WARNING, processResource, warningFile);
		}

		Bundle librariesBundle = Platform.getBundle("Libraries");
		String message = NLS.bind(DocometreMessages.CurrentLibrariesVersion, librariesBundle.getVersion().toString());
		Activator.logInfoMessage(message, getClass());
		ADWinDACQConfiguration dacqConfig = (ADWinDACQConfiguration)getDACQConfiguration();
		String librariesAbsolutePath = dacqConfig.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
		Pattern pattern = Pattern.compile("Libraries_\\d\\.\\d.\\d.\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d");
		Matcher matcher = pattern.matcher(librariesAbsolutePath);
		if (matcher.find()) {
			String usedLibrariesVersion = librariesAbsolutePath.substring(matcher.start(), matcher.end());
			usedLibrariesVersion = usedLibrariesVersion.replaceAll("Libraries_", "");
			message = NLS.bind(DocometreMessages.DacqConfLibrariesVersion, usedLibrariesVersion);
			Activator.logInfoMessage(message, getClass());
			if(!usedLibrariesVersion.equals(librariesBundle.getVersion().toString())) {
				Activator.logWarningMessage(DocometreMessages.UpdateLibrariesVersion);
				IMarker marker = dacqConfigurationResource.createMarker(DocometreBuilder.MARKER_ID);
				marker.setAttribute(IMarker.MESSAGE, DocometreMessages.UpdateLibrariesVersion);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
		}
		
		if(getScript().getCodeGenerationStatus().length > 0) {
			IStatus[] statuses = getScript().getCodeGenerationStatus();
			for (IStatus status : statuses) {
				try {
					IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
					marker.setAttribute(IMarker.MESSAGE, status.getMessage());
					marker.setAttribute(IMarker.LINE_NUMBER, 1);
					marker.setAttribute(IMarker.SEVERITY, status.getSeverity()==IStatus.ERROR?IMarker.SEVERITY_ERROR:IMarker.SEVERITY_WARNING);
					if(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR) == IMarker.SEVERITY_ERROR) Activator.logErrorMessage(status.getMessage());
					if(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_WARNING) Activator.logWarningMessage(status.getMessage());
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		}
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchPart editorPart = Activator.getEditor(ADWinProcess.this, ADWinProcessEditor.ID);
				if(editorPart != null) {
					((ProcessEditor)editorPart).updateTitleImage();
					PageChangedEvent event = new PageChangedEvent((IPageChangeProvider) editorPart, ((ProcessEditor)editorPart));
					((ProcessEditor)editorPart).pageChanged(event);
				}
				ADWinDACQConfiguration dacqConfiguration = (ADWinDACQConfiguration) ObjectsController.getDACQConfiguration(ADWinProcess.this);
//				IResource dacqConfigurationResource = ObjectsController.getResourceForObject(dacqConfiguration);
				editorPart = Activator.getEditor(dacqConfiguration, ADWinDACQConfigurationEditor.ID);
				if(editorPart != null) {
					((ADWinDACQConfigurationEditor)editorPart).updateTitleImage();
				}
				
			}
		});
		
		progressMonitor.worked(1);
		
		if(processError) {
			throw new Exception(DocometreMessages.ADWinProcess_CompileErrorsMessage); 
		}
		
		//Get compile errors
		if(errorFile.exists()) {
			throw new Exception(DocometreMessages.ADWinProcess_CompileErrorsMessage); 
		}
		//Get more general compile errors
		if(generalErrorfile.exists()) {
			throw new Exception(DocometreMessages.ADWinProcess_CompileErrorsMessage); 
		}
		//Get compile warnings
		if(warningFile.exists()) {
			throw new Exception(DocometreMessages.ADWinProcess_CompileErrorsMessage); 
		}
		
	}
	
	private void createMarker(int severity, IResource processResource, final File file) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					InputStream inputStream = new FileInputStream(file.getAbsolutePath());
					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
					String line;
					while ((line = bufferedReader.readLine())!=null) {
						if(severity == IMarker.SEVERITY_ERROR) Activator.logErrorMessage(line);
						if(severity == IMarker.SEVERITY_WARNING) Activator.logWarningMessage(line);
						if(line.startsWith("Error:") || line.startsWith("Warning:") || line.startsWith("Error :") || line.startsWith("Warning :")) {
							try {
								boolean adbasic4 = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION).equals(ADWinDACQConfigurationProperties.VINF4);
								String lineNumber = "";
								IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
								String[] lines = line.split("\\.\\s");
								line = "";
								for (int i = 0; i < lines.length; i++) {
									line = line + lines[i];
									if(i < lines.length - 1) line = line + "\n";
								}
								if(!adbasic4) {
									marker.setAttribute(IMarker.MESSAGE, line);
									String[] lineSplitted = line.split(" line no.: ");				
									if(lineSplitted.length >= 2) lineNumber = lineSplitted[1].replaceAll("\\s+","");
								} else {
									marker.setAttribute(IMarker.MESSAGE, line);
									String[] lineSplitted = line.split(":");				
									lineNumber = lineSplitted[lineSplitted.length - 1].replaceAll("\\s+","");
								}
								
								try {
									int lineNumberInteger = Integer.parseInt(lineNumber);
									marker.setAttribute(IMarker.LINE_NUMBER, lineNumberInteger);
								} catch (Exception e) {
									// TODO: Nothing
								} 
								marker.setAttribute(IMarker.SEVERITY, severity);
							} catch (CoreException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
							}
						}
					}
					bufferedReader.close(); 
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		});
	}
	
	private String createOSXCompileProcess(String currentFolder, String outputFolder, String adbasicCompiler, String adbasicFilePath) throws IOException, InterruptedException {
		
		// We need to create a bash file in order to launch ADBasic with wine
		// Bash file path
		final String bashFilePath = currentFolder + File.separator + "BinSource" + File.separator + "compileADBasic.sh";
		// Create Bash file
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		
		boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);

		if(!useDocker) {
			// Replace all spaces by "\ " in adbasicfilePath
			String[] fullPathSplitted = adbasicFilePath.split("\\s");
			adbasicFilePath = "";
			for (int i = 0; i < fullPathSplitted.length; i++) {
				adbasicFilePath = adbasicFilePath + fullPathSplitted[i];
				if(i < fullPathSplitted.length - 1) adbasicFilePath = adbasicFilePath + "\\" + " ";
			}
			// Replace "/" by "\" and add Z: drive (wine) 
			adbasicFilePath = processPathForMacOSX(adbasicFilePath);
			outputFolder = processPathForMacOSX(outputFolder);
		
			// Populate this file with command string
			// Get wine full path from preferences
			String wineFullPath = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.WINE_FULL_PATH);
			// Replace all spaces by "\ "
			fullPathSplitted = wineFullPath.split("\\s");
			wineFullPath = "";
			for (int i = 0; i < fullPathSplitted.length; i++) {
				wineFullPath = wineFullPath + fullPathSplitted[i];
				if(i < fullPathSplitted.length - 1) wineFullPath = wineFullPath + "\\" + " ";
			}
			// Replace all spaces by "\ " form adbasicCompiler
			fullPathSplitted = adbasicCompiler.split("\\s");
			adbasicCompiler = "";
			for (int i = 0; i < fullPathSplitted.length; i++) {
				adbasicCompiler = adbasicCompiler + fullPathSplitted[i];
				if(i < fullPathSplitted.length - 1) adbasicCompiler = adbasicCompiler + "\\" + " ";
			}
			String cmd = wineFullPath + " \"" + adbasicCompiler + "\"";
			cmd = cmd + getCommandLineParameters(outputFolder, adbasicFilePath);
			fileWriter.write(cmd);
			fileWriter.close();
		} else {
			String librariesPath = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
			IPath adbasicPath = Path.fromOSString(adbasicFilePath);
			String volume1 = " -v " + adbasicPath.removeLastSegments(1).toPortableString() + ":" + adbasicPath.removeLastSegments(1).toPortableString() + ":rw";
			String volume2 = " -v " + librariesPath + ":" + librariesPath + ":ro";
			String incLibFoldersOptions = " /IP/opt/adwin/ADwin-linux_6.0.30.00/adwin-compiler-6.0.30/share/adbasic/Inc/";
			incLibFoldersOptions = incLibFoldersOptions +" /LP/opt/adwin/ADwin-linux_6.0.30.00/adwin-compiler-6.0.30/share/adbasic/Lib/";
			String cmd = "/usr/local/bin/docker run -w " + adbasicPath.removeLastSegments(1).toPortableString() + " -di" + volume1 + volume2 + " --name adwin ubuntu:adwin";
			cmd += "\n/usr/local/bin/docker cp /Users/frank/git/DOCoMETRe/Libraries/includes/ADWinIncludeFiles/. adwin:/opt/adwin/ADwin-linux_6.0.30.00/adwin-compiler-6.0.30/share/adbasic/Inc/";
			cmd += "\n/usr/local/bin/docker cp /Users/frank/git/DOCoMETRe/Libraries/includes/ADWinIncludeFiles/. adwin:/opt/adwin/ADwin-linux_6.0.30.00/adwin-compiler-6.0.30/share/adbasic/Lib/";
			cmd += "\n/usr/local/bin/docker exec adwin /opt/adwin/bin/adbasic " + adbasicPath.lastSegment() + getCommandLineParameters(outputFolder, adbasicFilePath) + incLibFoldersOptions;
			cmd += "\n/usr/local/bin/docker stop -t 1 adwin";
			cmd += "\n/usr/local/bin/docker rm adwin";
			fileWriter.write(cmd);
			fileWriter.close();
		}
		
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec("chmod 777 " + bashFilePath);
		process.waitFor();
		return "/bin/sh " + bashFilePath;
	}

	private String createLinuxCompileProcess(String currentFolder, String outputFolder, String adbasicCompiler, String adbasicFilePath) throws IOException, InterruptedException {
		return createOSXCompileProcess(currentFolder, outputFolder, adbasicCompiler, adbasicFilePath);
	}

	private String createWindowsCompileProcess(String currentFolder, String outputFolder, String adbasicCompiler, String adbasicFilePath) throws IOException {
		final String batFilePath = currentFolder + File.separator + "BinSource" + File.separator + "compileADBasic.bat";
		// Create Bash file
		final File batFile = new File(batFilePath);
		if(batFile.exists()) batFile.delete();
		FileWriter fileWriter = new FileWriter(batFile);
		String cmdLine = "\"" + adbasicCompiler + "\"";
		cmdLine = cmdLine + getCommandLineParameters(outputFolder, adbasicFilePath);
		fileWriter.write(cmdLine);
		fileWriter.close();
		return batFilePath;
	}

	private String getCommandLineParameters(String outputFolder, String adbasicFilePath) {
		String cmdParams = "";
		boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
		if(!useDocker) {
			String[] adbasicFilePathSplitted = adbasicFilePath.split("\\\\");
			String errorFileName = adbasicFilePathSplitted[adbasicFilePathSplitted.length - 1].replaceAll("\\.(?i)bas$", ".err");
			if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION).equals(ADWinDACQConfigurationProperties.VINF4)) {
				cmdParams = " /M \"" + adbasicFilePath + "\" /A " + outputFolder + " /SP /P9 2>" + errorFileName;
			} else {
				if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE).equals(ADWinDACQConfigurationProperties.GOLD)) {
					if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.I)) {
						cmdParams = " /M \"" + adbasicFilePath + "\" /A\"" + outputFolder + "\" /SG /P9 2>" + errorFileName;
					} else {
						cmdParams = " /M \"" + adbasicFilePath + "\" /A\"" + outputFolder + "\" /SGII /P11 2>" + errorFileName;
					}
					
				} else {
					if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.I)) {
						cmdParams = cmdParams + " /M \"" + adbasicFilePath + "\" /A\"" + outputFolder + "\" /SP /P9 2>" + errorFileName;
					} else {
						cmdParams = cmdParams + " /M \"" + adbasicFilePath + "\" /A\"" + outputFolder + "\" /SPII /P11 2>" + errorFileName;
					}
				}
			}
			return cmdParams;
		} else {
			if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION).equals(ADWinDACQConfigurationProperties.VINF4)) {
				cmdParams = " /M /SP /P9";
			} else {
				if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE).equals(ADWinDACQConfigurationProperties.GOLD)) {
					if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.I)) {
						cmdParams = " /M /SG /P9";
					} else {
						cmdParams = " /M /SGII /P11";
					}
					
				} else {
					if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.I)) {
						cmdParams = " /M /SP /P9";
					} else {
						cmdParams = " /M /SPII /P11";
					}
				}
			}
			return cmdParams;
		}
	}
	
	@Override
	public String getCode(ModuleBehaviour script) throws Exception {
		
		getCurrentProcess().getScript().setIndentCode(true);
		
//		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
//			getDACQConfiguration().getModule(i).reset();
//		}
		reset();
		
		String code = "";
		String codeModule = "";
		ADWinCodeSegmentProperties[] segments = ADWinCodeSegmentProperties.values();
		for (int i = 0; i < segments.length; i++) {
			code = code + getCodeSegment(segments[i]);
			
			codeModule = "";
			for (int j = 0; j < getDACQConfiguration().getModulesNumber(); j++) {
					ModuleBehaviour module = getDACQConfiguration().getModule(j);
					if((module instanceof Module) || (module == script && module != null))
					codeModule = codeModule + module.getCodeSegment(segments[i]);
			}
			
			code = code + codeModule;
			
			if(segments[i].equals(ADWinCodeSegmentProperties.DECLARATION)) {
				// Variables declaration
				code = code + VariablesCodeGenerationDelegate.getCode(this, ADWinCodeSegmentProperties.DECLARATION);
			}
			if(segments[i].equals(ADWinCodeSegmentProperties.INITIALIZATION)) {
				// Variables initialization
				code = code + VariablesCodeGenerationDelegate.getCode(this, ADWinCodeSegmentProperties.INITIALIZATION);
				
				code = code + "\nREM ******** Début algorithme initialisation\n\n";
				code = code + getCurrentProcess().getScript().getInitializeCode(this, ScriptSegmentType.INITIALIZE);
				code = code + "\nREM ******** Fin algorithme initialisation\n";
			}
			if(segments[i].equals(ADWinCodeSegmentProperties.TRANSFER)) {
				// Variables transfer
				code = code + VariablesCodeGenerationDelegate.getCode(this, ADWinCodeSegmentProperties.TRANSFER);
			}
			if (segments[i] == ADWinCodeSegmentProperties.EVENT){
				code = code + "REM ******** Début algorithme boucle\n\n";
				code = code + getCurrentProcess().getScript().getLoopCode(this, ScriptSegmentType.LOOP);
				code = code + "REM ******** Fin algorithme boucle\n\n";
			}
			if(segments[i].equals(ADWinCodeSegmentProperties.FINISH)) {
				code = code + "\nREM ******** Début algorithme finalisation\n\n";
				code = code + getCurrentProcess().getScript().getFinalizeCode(this, ScriptSegmentType.FINALIZE);
				code = code + "\nREM ******** Fin algorithme finalisation\n";
				// Parameters propagation
				code = code + VariablesCodeGenerationDelegate.getCode(this, ADWinCodeSegmentProperties.FINISH);
			}
			
//			codeModule = "";
//			for (int j = 0; j < getDACQConfiguration().getModulesNumber(); j++) {
//					ModuleBehaviour module = getDACQConfiguration().getModule(j);
//					if((module instanceof Module) || (module == script && module != null))
//					codeModule = codeModule + module.getCodeSegment(segments[i]);
//			}
//			
//			code = code + codeModule;
			
		}
		return code;
	}

	@Override
	public void run(boolean compile, String prefix, String suffix) throws Exception {
			if(compile) compile(null);
			execute(prefix, suffix);
	}

	@Override
	public void stop() throws ADwinCommunicationError {
		int processNumberInt = Integer.parseInt(getProperty(ADWinProcessProperties.PROCESS_NUMBER));	
		((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Stop_Process(processNumberInt);
	}
	
	public static String processPathForMacOSX(String path) {
		boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
		if(Platform.getOS().equals(Platform.OS_MACOSX) && !useDocker) {
			// Replace all spaces by "/" in adbasicfilePath by "\"
			String[] pathSplitted = path.split("/");
			path = "";
			for (int i = 0; i < pathSplitted.length; i++) {
				path = path + pathSplitted[i];
				if(i < pathSplitted.length - 1) path = path + "\\";
			}
			// Add drive at path beginning
			path = "Z:" + path;
		}
		return path;
	}
	
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		if (segment == ADWinCodeSegmentProperties.HEADER){
			if(getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION).equals(ADWinDACQConfigurationProperties.VSUP5)){			
				code = "\'<ADbasic Header, Headerversion 001.001>\n";
				code = code + "\'Process_Number                   = 1\n";
				code = code + "\'Initial_Processdelay             = 1000\n";
				code = code + "\'Eventsource                      = Timer\n";
				code = code + "\'Control_long_Delays_for_Stop     = No\n";
				code = code + "\'Priority                         = High\n";
				code = code + "\'Version                          = 1\n";
				code = code + "\'ADbasic_Version                  = 5.0.2\n";
				code = code + "\'Optimize                         = Yes\n";
				code = code + "\'Optimize_Level                   = 1\n";
				code = code + "\'Info_Last_Save                   = PC PC\\anonymous\n";
				code = code + "\'<Header End>\n";
				code = code + "\n";
			}
			else{
				//version <=4
				//TODO
			}
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.HEADER);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.HEADER);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.HEADER);
			
		}
		
		if (segment == ADWinCodeSegmentProperties.INCLUDE){
			String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
			String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			boolean importStringLibrary = false;
			// Import String lib if a a variable of type string is declared
			// or a serial function is used
			ADWinVariable[] variables = ((ADWinDACQConfiguration)getDACQConfiguration()).getVariables();
			for (ADWinVariable variable : variables) {
				if(variable.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.STRING)) {
					importStringLibrary = true;
					break;
				}
			}
			ScriptSegment  scriptSegment = getInitializeBlocksContainer();
			List<Block> blocks = scriptSegment.getBlocks();
			for (Block block : blocks) {
				if(FunctionsUtil.isSerialFuntion(block)) {
					importStringLibrary = true;
					break;
				}
			}
			scriptSegment = getLoopBlocksContainer();
			blocks = scriptSegment.getBlocks();
			for (Block block : blocks) {
				if(FunctionsUtil.isSerialFuntion(block)) {
					importStringLibrary = true;
					break;
				}
			}
			scriptSegment = getFinalizeBlocksContainer();
			blocks = scriptSegment.getBlocks();
			for (Block block : blocks) {
				if(FunctionsUtil.isSerialFuntion(block)) {
					importStringLibrary = true;
					break;
				}
			}
			
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.II)) {
				if(importStringLibrary) code = code + "IMPORT String.lib\n";
				String temp = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
				boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
				if(useDocker) temp = "";
				temp = temp + "DOCOLIBGoldII.LIB\n";
				temp =	ADWinProcess.processPathForMacOSX(temp);
				code = code + "IMPORT " + temp;
				code = code + "#INCLUDE ADwinGoldII.INC\n";
			}
				
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) {
				if(importStringLibrary) code = code + "IMPORT String.li9\n";
				String temp = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
				boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
				if(useDocker) temp = "";
				temp = temp + "DOCOLIBPro.LI9\n";
				temp =	ADWinProcess.processPathForMacOSX(temp);
				code = code + "IMPORT " + temp;
				code = code + "#INCLUDE ADwinPro.INC\n";
			}
				
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) {
				if(importStringLibrary) code = code + "IMPORT String.lib\n";
				String temp = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
				boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
				if(useDocker) temp = "";
				temp = temp + "DOCOLIBProII.LIB\n";
				temp =	ADWinProcess.processPathForMacOSX(temp);
				code = code + "IMPORT " + temp;
				code = code + "#INCLUDE ADwinPro2.INC\n";
			}
			
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.I)) {
				if(importStringLibrary) code = code + "IMPORT String.li9\n";
				
				String temp = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
				boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
				if(useDocker) temp = "";
				temp = temp + "DOCOLIBGold.LI9\n";
				temp =	ADWinProcess.processPathForMacOSX(temp);
				code = code + "IMPORT " + temp;
			}
				
//			code = code + "#INCLUDE " + processPathForMacOSX(getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator +  "DOCOLIB.INC\n");
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.INCLUDE);
			
		}
		
		if (segment == ADWinCodeSegmentProperties.DECLARATION){
				code = "\n#DEFINE PI 4*ARCTAN(1)";
				code = code + "\n#DEFINE time FPAR_" + getProperty(ADWinProcessProperties.PROCESS_NUMBER) + "\n";
				code = code + "DIM FEG AS LONG\n";
				code = code + "DIM INDEX AS LONG\n";
				code = code + "DIM TERMINATE_PROCESS AS SHORT\n";
				
				code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.DECLARATION);
				code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.DECLARATION);
				code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.DECLARATION);
				
		}
		
		if (segment == ADWinCodeSegmentProperties.INITIALIZATION){
			String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			code = "\nLOWINIT:\n";
			code = code + "\nFEG = " + getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY) + "\n";
			if(cpuType.equals(ADWinDACQConfigurationProperties.I)) code = code + "GLOBALDELAY = 10^9/(FEG*25)\n";
			else if(cpuType.equals(ADWinDACQConfigurationProperties.II)) code = code + "GLOBALDELAY = 10^9/(FEG*3.3)\n";
			code = code + "INDEX = 0\n";
			code = code + "TERMINATE_PROCESS = 0\n";
			code = code + "time = 0\n";
			
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.INITIALIZATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.INITIALIZATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.INITIALIZATION);
			
		}
		
		if (segment == ADWinCodeSegmentProperties.ACQUISITION){
			code = "\nEVENT:\n";
			code = code + "\ntime = INDEX/FEG\n";
			if (!(getProperty(ADWinProcessProperties.DURATION).equals(ADWinProcessProperties.DURATION_NOT_SPECIFIED))){
				code = code + "IF (time > " + getProperty(ADWinProcessProperties.DURATION) + ") THEN\n";  
				code = code + "\tTERMINATE_PROCESS = 1\n";
				code = code + "ENDIF\n";
			}
			code = code + "INC(INDEX)\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.ACQUISITION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.ACQUISITION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.ACQUISITION);
		}
		
		if (segment == ADWinCodeSegmentProperties.RECOVERY){
//			code = "\nREM ******** Debut recuperation données du PC (Stimuli)\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.RECOVERY);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.RECOVERY);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.RECOVERY);
			
		}
		
		if (segment == ADWinCodeSegmentProperties.TRANSFER){
//			code = "\nREM ******** Fin recuperation données du PC (Stimuli)\n\n";
			code = code + "REM ******** Debut autotransfert donnees vers PC\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.TRANSFER);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.TRANSFER);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.TRANSFER);
		}
		
		if (segment == ADWinCodeSegmentProperties.EVENT){
			code = "\nREM ******** Fin autotransfert données vers PC\n\n";
//			code = code + "REM ******** Début algorithme boucle\n\n";
//			code = code + getCurrentProcess().getScript().getLoopCode(this, ScriptSegmentType.LOOP);
		}
		
		if (segment == ADWinCodeSegmentProperties.GENERATION){
			code = code + "REM ******** Début génération des signaux\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ADWinCodeSegmentProperties.GENERATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ADWinCodeSegmentProperties.GENERATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ADWinCodeSegmentProperties.GENERATION);
		}
		
		if (segment == ADWinCodeSegmentProperties.FINISH){
			code = "\nREM ******** Fin génération des signaux\n\n";
			//if (!(getProperty(ADWinProcessProperties.DURATION).equals(ADWinProcessProperties.DURATION_NOT_SPECIFIED))){
				code = code + "IF (TERMINATE_PROCESS = 1) THEN\n";
				code = code + "\tEND\n";
				code = code + "ENDIF\n\n";
			//}
			code = code + "\nFINISH:\n";
		}
		
		return code;
	}
	/**
	 * This method loads the ADWIN binary code to the device
	 */
	public void loadProcess() throws Exception {
		IResource processFile = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String binaryFilePath = wsPath.toOSString().replaceAll(File.separator + "$", "");
		binaryFilePath = binaryFilePath + processFile.getParent().getFullPath().toOSString();
		binaryFilePath = binaryFilePath + File.separator + "BinSource" + File.separator + processFile.getName().replaceAll(Activator.processFileExtension + "$", "");
		appendToEventDiary(NLS.bind(ADWinMessages.ADWinDiary_Loading, binaryFilePath));
		LoadProcessDelegate.loadProcess(binaryFilePath, getProperty(ADWinProcessProperties.PROCESS_NUMBER), (ADWinDACQConfiguration) getDACQConfiguration());
//		String filePath = null;
//		if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).contentEquals(ADWinDACQConfigurationProperties.I))
//			filePath = binaryFilePath + ".t9" + getProperty(ADWinProcessProperties.PROCESS_NUMBER);
//		if (getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).contentEquals(ADWinDACQConfigurationProperties.II)) 
//			filePath = binaryFilePath + ".tB" + getProperty(ADWinProcessProperties.PROCESS_NUMBER);
//		if(filePath != null) {
//			File file = new File(filePath);
//			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Load_Process(file.getAbsolutePath());
//		} else throw new Exception("ADBasic binary file \"" + filePath + "\" not found");	
	}
	
	/**
	 * This method allows to boot the ADWIN before loading any process
	 */
	public void boot() throws UnknownHostException, ADwinCommunicationError {
		
		String status = BootDelegate.boot((ADWinDACQConfiguration) getDACQConfiguration());
		if(status.equals(BootDelegate.BOOTED)) appendToEventDiary(ADWinMessages.ADWinDiary_Booted);
		if(status.equals(BootDelegate.ALREADY_BOOTED)) appendToEventDiary(ADWinMessages.ADWinDiary_AlreadyBooted);
		
//		String ADWinBTL = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.BTL_FILE);
//		String IP = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.IP_ADDRESS);
//		int portNumber = Integer.valueOf(getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.PORT_NUMBER));
//		// If there is no adwin device create and configure it
//		if (((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice() == null){
//			((ADWinDACQConfiguration)getDACQConfiguration()).setADwinDevice(new ADwinDevice());
//			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Remove_Entry(0x01);
//			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Add_Entry(0x01, InetAddress.getByName(IP) , "", 1, portNumber);
//			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Set_DeviceNo(0x01);
//	    }
//		
//		// test if ADwin is already booted
//		boolean isAdwinBooted;
//		try {
//			@SuppressWarnings("unused")// Test if a call to Get_FPar responds, if not it means that ADWin is not booted
//			float time = ((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Get_FPar(1);
//			isAdwinBooted = true;
//		} catch (Exception e) {
//			isAdwinBooted = false;
//		}
//		if(!isAdwinBooted){
//			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Boot(ADWinBTL);
//			appendToEventDiary(ADWinMessages.ADWinDiary_Booted);
//		} else appendToEventDiary(ADWinMessages.ADWinDiary_AlreadyBooted);
	}
	
	@Override
	public Job execute(String prefix, String suffix) throws Exception {
		try {
			boot();
			loadProcess();
			int processNumberInt = Integer.parseInt(getProperty(ADWinProcessProperties.PROCESS_NUMBER));	
			RealTimeLoopJob realTimeLoopJob = new RealTimeLoopJob(DocometreMessages.RunningProcess);
			open(this, prefix, suffix);
			generation();
//		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
//			ModuleBehaviour module = getDACQConfiguration().getModule(i);
//			module.open(this);
//			module.generation();
//		}
			pushParametersToADWinProcess();
			((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().Start_Process(processNumberInt);
			return realTimeLoopJob;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public float[] getData(int transferNumInt, int nbData) throws ADwinCommunicationError {
		float[] signal = new float[nbData];
		((ADWinDACQConfiguration)getDACQConfiguration()).getADwinDevice().GetFifo_Float(transferNumInt, signal, nbData);
		return signal;
	}

	public void reset() {
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.reset();
		}
	}
	
	public void recovery() {
		ADWinVariable[] variables = ((ADWinDACQConfiguration)getDACQConfiguration()).getVariables();
		RecoveryDelegate.recover(variables, null, this);
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.recovery();
		}
	}
	
	public void generation(){
		ADWinVariable[] variables = ((ADWinDACQConfiguration)getDACQConfiguration()).getVariables();
		GenerationDelegate.generate(variables, null, this);
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.generation();
		}
	}
	public void open(Process process, String prefix, String suffix) {
		ADWinVariable[] variables = ((ADWinDACQConfiguration)getDACQConfiguration()).getVariables();
		for (ADWinVariable variable : variables) {
			variable.open(this, prefix, suffix);
		}
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.open(this, prefix, suffix);
		}
	}
	
	public void close() {
		ADWinVariable[] variables = ((ADWinDACQConfiguration)getDACQConfiguration()).getVariables();
		for (ADWinVariable variable : variables) {
			variable.close(this);
		}
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.close();
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void initializeObservers() {
	}
	
	@Override
	public void preExecute(IResource resource) throws Exception {
		super.preExecute(resource);
		preManageParameters(resource);
	}
	
	@Override
	public void postExecute(IResource resource) throws Exception {
		super.postExecute(resource);
		postManageParameters(resource);
	}
	
	private void preManageParameters(IResource resource) throws Exception {
		if(currentTrialParameters == null) currentTrialParameters = new LinkedHashMap<>();
		if(currentTrialParametersString == null) currentTrialParametersString = new LinkedHashMap<>();
		currentTrialParameters.clear();
		currentTrialParametersString.clear();
		// Init Parameters from params file if resource is a trial
		if(ResourceType.isTrial(resource)) {
			// Get trial number from trial name
			currentTrialNumber = Integer.parseInt(resource.getName().split("°")[1]);
			// Try to find if there are any parameters in associated dacq conf
			ADWinDACQConfiguration adwinDacqConfiguration = (ADWinDACQConfiguration) getDACQConfiguration();
			ADWinVariable[] parameters = adwinDacqConfiguration.getParameters();
			if(parameters.length > 0) {
				// There are parameters, try to get their values for current trial from parameters file
				// Get parameters file
				parametersFile = null;
				IResource[] members = resource.getParent().members();
				for (IResource member : members) {
					if(ResourceType.isParameters(member)) {
						parametersFile = member;
						break;
					}
				}
				if(parametersFile != null) {
					// Retrieve values for this trial
					List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(parametersFile.getLocation().toOSString()), StandardCharsets.UTF_8);
					
					removeComments(lines);
					
					if(lines.size() - 1 >= currentTrialNumber) {
						String parametersNames = lines.get(0);
						String parametersValues = lines.get(currentTrialNumber);
						parametersNames = parametersNames.replaceAll("[\\s|\\u00A0]+", "");
						parametersValues = parametersValues.replaceAll("[\\s|\\u00A0]+", "");
						String[] parametersNamesArray = parametersNames.split(";|:|,");
						String[] parametersValuesArray = parametersValues.split(";|:|,");
						// Log infos
						String parametersString = "";
						int nbParmetersFound= 0;
						for (ADWinVariable parameter : parameters) {
							int index = 0;
							for (String parameterName : parametersNamesArray) {
								if(parameterName.equalsIgnoreCase(parameter.getProperty(ChannelProperties.NAME))) {
									parametersString = parametersString + parameterName + " = " + parametersValuesArray[index] ;
									nbParmetersFound++;
									if(nbParmetersFound < parameters.length) parametersString = parametersString + " - ";
									break;
								}
								index++;
							}
							if(index == parametersNamesArray.length) {
//								currentTrialParameters.add(0f);
								throw new Exception(NLS.bind(DocometreMessages.ParameterNameNotfound, parameter.getProperty(ChannelProperties.NAME), parametersFile.getLocation().toOSString()));
							}
						}
						Activator.logInfoMessage("Preparing paramaters array (parsing values) for trial (" + resource.getName() + ") : " + parametersString, ADWinProcess.class);
						
						// put parameters
						for (ADWinVariable parameter : parameters) {
							int index = 0;
							for (String parameterName : parametersNamesArray) {
								if(parameterName.equalsIgnoreCase(parameter.getProperty(ChannelProperties.NAME))) {
									if(parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.FLOAT) || parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.INT)) 
										currentTrialParameters.put(parameter, Float.parseFloat(parametersValuesArray[index]));
									if(parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.STRING)) 
										currentTrialParametersString.put(parameter, parametersValuesArray[index]);
									break;
								}
								index++;
							}
							if(index == parametersNamesArray.length) {
//								currentTrialParameters.add(0f);
								throw new Exception(NLS.bind(DocometreMessages.ParameterNameNotfound, parameter.getProperty(ChannelProperties.NAME), parametersFile.getLocation().toOSString()));
							}
						}
					} else {
						// Parameters file empty
//						for (int i = 0; i < parameters.length; i++) currentTrialParameters.add(0f);
						throw new Exception(NLS.bind(DocometreMessages.ParameterValueNotfound, currentTrialNumber, parametersFile.getLocation().toOSString()));
					}
				} else {
					// No parameters file found
//					for (int i = 0; i < parameters.length; i++) currentTrialParameters.add(0f);
					throw new Exception(DocometreMessages.NoParameterFileFound);
				}
			}
		}
		
		// If resource is a process 
		if(ResourceType.isProcess(resource)) {
			// Try to find if there are any parameters in associated dacq conf
			ADWinDACQConfiguration adwinDacqConfiguration = (ADWinDACQConfiguration) getDACQConfiguration();
			ADWinVariable[] parameters = adwinDacqConfiguration.getParameters();
			response = Dialog.OK;
			if(parameters.length > 0) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						ParametersDialog parametersDialog = new ParametersDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), parameters);
						parametersDialog.setBlockOnOpen(true);
						response = parametersDialog.open();
					}
				});
				if(response == Dialog.CANCEL) throw new Exception(DocometreMessages.ProcessLaunchCanceled);
				ObjectsController.serialize(adwinDacqConfiguration);
				String parametersString = "";
				int index = 0;
				for (ADWinVariable parameter : parameters) {
					String value = parameter.getProperty(ADWinVariableProperties.PARAMETER_VALUE);
					parametersString = parametersString + parameter.getProperty(ChannelProperties.NAME) + " = " + value ;
					index++;
					if(index < parameters.length) parametersString = parametersString + " - ";
				}
				Activator.logInfoMessage("Preparing parameters array (parsing values) for process test (" + resource.getName() + ") : " + parametersString, ADWinProcess.class);
				for (ADWinVariable parameter : parameters) {
					String value = parameter.getProperty(ADWinVariableProperties.PARAMETER_VALUE);
					try {
						//float floatValue =  Float.parseFloat(value);
						//currentTrialParameters.put(parameter, floatValue);
						if(parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.FLOAT) || parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.INT)) 
							currentTrialParameters.put(parameter, Float.parseFloat(value));
						if(parameter.getProperty(ADWinVariableProperties.TYPE).equals(ADWinVariableProperties.STRING)) 
							currentTrialParametersString.put(parameter, value);
						
					} catch (Exception e) {
						throw e;
					}
					
				}
			}
		}
	}
	
	private void removeComments(List<String> lines) {
		int n = 0;
		while (n < lines.size()) {
			String line = lines.get(n);
			if(line.startsWith("#")) lines.remove(n);
			else {
				if(line.indexOf("#") > -1) {
					line = line.replaceAll("#.*$", "");
					lines.set(n, line);
				}
				n++;
			}
		}
		
	}

	private void pushParametersToADWinProcess() throws Exception {
		if(currentTrialParameters.size() > 0) {
			float[] data = new float[currentTrialParameters.size()];
			Set<ADWinVariable> keys = currentTrialParameters.keySet();
			int i = 0;
			String parametersString = "";
			for (ADWinVariable variable : keys) {
				data[i] = currentTrialParameters.get(variable).floatValue();
				parametersString = parametersString + variable.getProperty(ChannelProperties.NAME) + " = " + data[i] ;
				i++;
				if(keys.size() > i) parametersString = parametersString + " - ";
			}
			Activator.logInfoMessage("Pushing paramaters to ADWin : " + parametersString, ADWinProcess.class);
			((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().SetData_Float(200, data, 1, data.length);
		}
		if(currentTrialParametersString.size() > 0) {
			String data = "";
			Set<ADWinVariable> keys = currentTrialParametersString.keySet();
			int i = 0;
			String parametersString = "";
			for (ADWinVariable variable : keys) {
				String dataString = currentTrialParametersString.get(variable);
				parametersString = parametersString + variable.getProperty(ChannelProperties.NAME) + " = " + dataString ;
				data = data + dataString + "\n";
				i++;
				if(keys.size() > i) parametersString = parametersString + " - ";
			}
			if(data.length() > 1024)
				Activator.logErrorMessage("Error : String parameters is too long (more than 1024 chars) !");
			else {
				Activator.logInfoMessage("Pushing parameters to ADWin (String) : " + parametersString, ADWinProcess.class);
				byte[] bytesArray = data.getBytes(StandardCharsets.US_ASCII.name());
				int[] intsArray = new int[bytesArray.length + 2];
				intsArray[0] = bytesArray.length;
				for (int j = 1; j < intsArray.length - 1; j++) {
					intsArray[j] = bytesArray[j - 1];
				}
				((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().SetData_Long(199, intsArray, 1, intsArray.length);
				
			}
		}
	}
	
	private float[] getParametersFromADWinProcess() throws Exception {
		if(currentTrialParameters.size() > 0) {
			float[] data = new float[currentTrialParameters.size()];
			((ADWinDACQConfiguration)ADWinProcess.this.getDACQConfiguration()).getADwinDevice().GetData_Float(200, data, 1, data.length);
			return data;
		}
		return null;
	}

	private void postManageParameters(IResource resource) throws Exception {
		if(ResourceType.isTrial(resource)) {
			ADWinDACQConfiguration adwinDacqConfiguration = (ADWinDACQConfiguration) getDACQConfiguration();
			ADWinVariable[] propagatedParameters = adwinDacqConfiguration.getPropagatedParameters();
			if(propagatedParameters.length > 0) {
				// Read parameters file and replace next trial line when parameter must be propagated
				List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(parametersFile.getLocation().toOSString()), StandardCharsets.UTF_8);
				if(lines.size() - 1 >= currentTrialNumber + 1) {
					String parametersNames = lines.get(0);
					String parametersValues = lines.get(currentTrialNumber + 1); 
					parametersNames = parametersNames.replaceAll("[\\s|\\u00A0]+", "");
					parametersValues = parametersValues.replaceAll("[\\s|\\u00A0]+", "");
					String[] parametersNamesArray = parametersNames.split(";|:|,");
					String[] parametersValuesArray = parametersValues.split(";|:|,");
					float[] data = getParametersFromADWinProcess();
					String dataString = "";
					int indexParameterName = 0;
					for (String parameterName : parametersNamesArray) {
						Set<ADWinVariable> variables = currentTrialParameters.keySet();
						int indexVariablesName = 0;
						for (ADWinVariable variable : variables) {
							if(variable.getProperty(ChannelProperties.NAME).equalsIgnoreCase(parameterName)) {
								if(variable.isParameterPropagated()) dataString = dataString + data[indexVariablesName] + ";";
								else dataString = dataString + parametersValuesArray[indexParameterName] + ";";
								break;
							} else indexVariablesName++;
						}
						indexParameterName++;
					}
					dataString = dataString.replaceAll(";$", "");
					lines.set(currentTrialNumber + 1, dataString);
					Files.write(Paths.get(parametersFile.getLocationURI()), String.join(System.getProperty("line.separator"), lines).getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
				}
			}
		}
	}

}
