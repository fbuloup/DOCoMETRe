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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.adwin.DiaryLogLevel;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;

/**
 * The class <b>Process</b> holds the behavior of the acquired, processed and generated signals.
 * It depends on a DACQ general configuration.
 */
public abstract class Process extends AbstractElement implements ModuleBehaviour, PropertyObserver {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	private Script script;
	
	private transient DACQConfiguration dacqConfiguration;
	protected transient int currentDiaryLine;
	protected transient HashMap<Integer, String> errorMarkers;

	private transient IResource logFile;

	private transient IResource outputFolder;

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
//				System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
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
			if(outputFolder != null) {
				
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					
					@Override
					public void run() {
						String message = NLS.bind(DocometreMessages.DeleteProcessTestDialogMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
						if(MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.DeleteProcessTestDialogTitle, message)) {
							try {
								outputFolder.delete(true, null);
							} catch (CoreException e) {
								e.printStackTrace();
								Activator.logErrorMessage(e.getMessage());
							}
						}
					}
				});
				
				
			}
//			if(outputFolder == null) {
				outputFolder = ((IContainer)parent).getFolder(new Path("test." + resource.getName().replaceAll(Activator.processFileExtension, "")));
				((IFolder)outputFolder).create(true, true, null);
				ResourceProperties.setTypePersistentProperty(outputFolder, ResourceType.PROCESS_TEST.toString());
				ResourceProperties.setAssociatedProcessProperty(outputFolder, resource.getFullPath().toOSString());
//			}
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
		fileAppender.activateOptions();
		logger.addAppender(fileAppender);
	}
	
	private void refreshLogFile(IResource resource) throws Exception {
		Logger logger = Logger.getLogger(Process.class);
		logger.getAppender(resource.getFullPath().toOSString() + " Diary").close();
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
	
	public void postExecute(IResource resource) throws Exception {
		refreshLogFile(resource);
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub
	}

}
