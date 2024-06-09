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
package fr.univamu.ism.docometre.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class OpenProcessWithSystemEditorAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private static String ID = "OpenProcessWithSystemEditorAction";
	
	private class OpenInSystemEditorJob extends Job {
		
		private IFile resource;
		private Process process;
		private boolean removeProcessHandle;
		private boolean removeDACQHandle;

		public OpenInSystemEditorJob(String name, IFile resource, Process process, boolean removeProcessHandle, boolean removeDACQHandle) {
			super(name);
			this.resource = resource;
			this.process = process;
			this.removeProcessHandle = removeProcessHandle;
			this.removeDACQHandle = removeDACQHandle;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IStatus status = Status.OK_STATUS;
			try {
				
				if(process instanceof ADWinProcess) {
					String message = NLS.bind(DocometreMessages.OpenProcessWithADWinSystemEditorAction_WaitTaskMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
					monitor.beginTask(message, 2);
					// The workspace absolute path
					IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
					// Create ADBasic file
					IResource processResource = ObjectsController.getResourceForObject(process);
					String adbasicFilePath = wsPath.toOSString().replaceAll("/$", "") + processResource.getParent().getFullPath().toOSString() + File.separator + "BinSource" + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".BAS");
					final File adbasicFile = ADWinProcess.createADBasicFile(resource, process, adbasicFilePath);
					
					// ADBasic absolute file path
					adbasicFilePath = adbasicFile.getAbsolutePath();

					monitor.worked(1);
					if(Platform.getOS().equals(Platform.OS_WIN32)) {
						boolean result = false;
						Program programEditor = Program.findProgram (".bas");
						if (programEditor != null) result = programEditor.execute(adbasicFilePath);
						if(!result) {
							message = NLS.bind(DocometreMessages.OpenProcessWithSystemEditorAction_EditorNotLaunched, adbasicFilePath);
//							Activator.logErrorMessage(message);
							status = new Status(Status.ERROR, Activator.PLUGIN_ID, message, null);
						}
					}
					
					if(Platform.getOS().equals(Platform.OS_MACOSX) || Platform.getOS().equals(Platform.OS_LINUX)) {
						// Replace all spaces by "\ " in adbasicfilePath
						String[] fullPathSplitted = adbasicFilePath.split("\\s");
						adbasicFilePath = "";
						for (int i = 0; i < fullPathSplitted.length; i++) {
							adbasicFilePath = adbasicFilePath + fullPathSplitted[i];
							if(i < fullPathSplitted.length - 1) adbasicFilePath = adbasicFilePath + "\\" + " ";
						}
						// We need to create a bash file in order to launch ADBasic with wine
						// Bash file path
						final String bashFilePath = wsPath.toOSString().replaceAll("/$", "") + resource.getParent().getFullPath().toOSString().replaceAll("/$", "") + File.separator + "startADBasic_" + resource.getName().replaceAll(Activator.processFileExtension +"$", "") + ".sh";
						// Create Bash file
						final File bashFile = new File(bashFilePath);
						if(bashFile.exists()) bashFile.delete();
						FileWriter fileWriter = new FileWriter(bashFile);
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
						// Get ADBasic.exe full path from DACQ config pref.
						String adbasicExeFullPath = process.getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_COMPILER).replaceAll("(?i)ADbasicCompiler.exe$", "ADbasic.exe");
						// Replace all spaces by "\ "
						fullPathSplitted = adbasicExeFullPath.split("\\s");
						adbasicExeFullPath = "";
						for (int i = 0; i < fullPathSplitted.length; i++) {
							adbasicExeFullPath = adbasicExeFullPath + fullPathSplitted[i];
							if(i < fullPathSplitted.length - 1) adbasicExeFullPath = adbasicExeFullPath + "\\" + " ";
						}
						adbasicExeFullPath = adbasicExeFullPath.replaceAll("\\\\", "/");
						String cmd = wineFullPath + " " + adbasicExeFullPath;
						cmd = cmd + " " + adbasicFilePath;
						fileWriter.write(cmd);
						fileWriter.close();
						// This bash file must be executable 
						Runtime.getRuntime().exec("chmod +x " + bashFilePath);
						// The runnable for thread ADBasic editor in wine
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								try {
									java.lang.Process wineProcess = Runtime.getRuntime().exec("/bin/sh " + bashFilePath);
									wineProcess.waitFor();
									// Delete bash file once editing is done
									if(bashFile.exists()) bashFile.delete();
									File bakFile = new File(adbasicFile.getAbsolutePath().replaceAll("(?i).bas", ".bak"));
									if(bakFile.exists()) bakFile.delete();
								} catch (IOException | InterruptedException e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
							}
						};
						// Create and start wine thread
						Thread wineProcessThread = new Thread(runnable);
						wineProcessThread.start();
						
					}
					monitor.worked(2);
				}
				if(process instanceof ArduinoUnoProcess) {
					
					String message = NLS.bind(DocometreMessages.OpenProcessWithADWinSystemEditorAction_WaitTaskMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
					monitor.beginTask(message, 2);
					// The workspace absolute path
					IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
					// Create ADBasic file
					IResource processResource = ObjectsController.getResourceForObject(process);
					String sketchFilePath = wsPath.toOSString().replaceAll("/$", "") + processResource.getParent().getFullPath().toOSString() + File.separator + "BinSource" + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".ino");
					final File sketchFile = ArduinoUnoProcess.createSketchFile(resource, process, sketchFilePath);
					// ADBasic absolute file path
					sketchFilePath = sketchFile.getAbsolutePath();
					
					
					boolean result = false;
					Program programEditor = Program.findProgram (".ino");
					if (programEditor != null) result = programEditor.execute(sketchFilePath);
					if(!result) {
						message = NLS.bind(DocometreMessages.OpenProcessWithSystemEditorAction_EditorNotLaunched, sketchFilePath);
//						Activator.logErrorMessage(message);
						status = new Status(Status.ERROR, Activator.PLUGIN_ID, message, null);
					}
				}
				
				
			} catch (IOException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			} finally {
				if(removeDACQHandle) ObjectsController.removeHandle(process.getDACQConfiguration());
				if(removeProcessHandle) ObjectsController.removeHandle(process);
				String message = NLS.bind(DocometreMessages.OpenProcessWithSystemEditorAction_WaitMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
				Activator.logInfoMessage(message, OpenProcessWithSystemEditorAction.class);
			}
			monitor.done();
			return status;
		}
		
	}
	
	private IWorkbenchWindow window;
	private IFile[] resources;
	
	public OpenProcessWithSystemEditorAction(IWorkbenchWindow window) {
		setId(ID);//$NON-NLS-1$
		setActionDefinitionId(ID);
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.OpenWithSystemEditorAction_Text);
		setToolTipText(DocometreMessages.OpenWithSystemEditorAction_Text);
	}
	
	@Override
	public void run() {
		
		for (int n = 0; n < resources.length; n++) {
			IFile resource = resources[n];
			// if Process object has not been yet deserialised, will need to decrease handle
			boolean removeProcessHandle = false;
			boolean removeDACQHandle = false;
			Object object = ResourceProperties.getObjectSessionProperty(resource);
			if(object == null) {
				object = ObjectsController.deserialize(resource);
				ResourceProperties.setObjectSessionProperty(resource, object);
				ObjectsController.addHandle(object);
				removeProcessHandle = true;
				removeDACQHandle = true;
			}
			Process process = (Process) object;
			// If DACQConfiguration object has not been yet deserialized, will need to decrease handle
			String fullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
			IFile daqConfigurationFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fullPath)) ;
			if(daqConfigurationFile.exists())
				if(ResourceProperties.getObjectSessionProperty(daqConfigurationFile) == null) 
					removeDACQHandle = true;
			
			Job openInSystemEditorJob = new OpenInSystemEditorJob("Opening process", resource, process, removeProcessHandle, removeDACQHandle);
			openInSystemEditorJob.setUser(true);
			openInSystemEditorJob.setRule(resource.getWorkspace().getRoot());
			openInSystemEditorJob.schedule();
			
			
			
		}
		
		
		
		
	}

	@Override
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView) {
			resources = null;
			if (selection instanceof IStructuredSelection) {
				Object[] selectedObjects = ((IStructuredSelection) selection).toArray();
				ArrayList<IFile> files = new ArrayList<>();
				for (Object object : selectedObjects) {
					if(object instanceof IFile) {
						if(ResourceType.isProcess((IResource) object)) files.add((IFile) object);
					}
				}
				if(files.size() > 0) resources = files.toArray(new IFile[files.size()]);
			}
			setEnabled(resources != null);
		}
	}

}


