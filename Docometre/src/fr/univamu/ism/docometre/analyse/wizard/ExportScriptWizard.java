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
package fr.univamu.ism.docometre.analyse.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class ExportScriptWizard extends Wizard {
	
	private ExportScriptWizardPage exportScriptWizardPage;
	private boolean anErrorOccured = false;
	private BatchDataProcessing batchDataProcessing;
	private IResource batchResource;
	
	public ExportScriptWizard(BatchDataProcessing batchDataProcessing, IResource batchResource) {
		this.batchDataProcessing = batchDataProcessing;
		this.batchResource = batchResource;
		setWindowTitle(DocometreMessages.ExportScriptDialogTitle);
	}

	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		exportScriptWizardPage = new ExportScriptWizardPage(batchResource);
		super.addPage(exportScriptWizardPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Export", 3);
					monitor.subTask(DocometreMessages.GetWholeProcessesScript);
					BatchDataProcessingItem[] subjectsItems = batchDataProcessing.getSubjects();
					BatchDataProcessingItem[] processesItems = batchDataProcessing.getProcesses();
					String processesCode = "";
					for (BatchDataProcessingItem processeItem : processesItems) {
						if(processeItem.isActivated()) {
							String path = processeItem.getPath();
							IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
							boolean removeHandle = false;
							Object object = ResourceProperties.getObjectSessionProperty(resource);
							if(object == null) {
								object = ObjectsController.deserialize((IFile)resource);
								ResourceProperties.setObjectSessionProperty(resource, object);
								ObjectsController.addHandle(object);
								removeHandle = true;
							}
							if(object instanceof Script) {
								try {
									Script script = (Script)object;
									processesCode = processesCode + script.getLoopCode(object, ScriptSegmentType.LOOP) + "\n";
								} catch (Exception e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
							}
							if(removeHandle) ObjectsController.removeHandle(object);
						}
					}
					monitor.worked(1);
					monitor.subTask(DocometreMessages.GenerateScriptForAllSubjects);
					SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
					Date date = new Date(System.currentTimeMillis());
					String scriptCode = MathEngineFactory.getMathEngine().getCommentCharacter() + " Generated script from DOCoMETRe - " + formatter.format(date) + "\n" ;
					scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Workspace : " + ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + "\n";
					scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Project : " + batchResource.getProject().getName() + "\n";
					String fullName = batchResource.getFullPath().toOSString();
					scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Batch data processing file : " + fullName + "\n";
					if(MathEngineFactory.isPython()) {
						IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
						String pythonScriptsPath = preferenceStore.getString(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION);						scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " If you want to use this script please start a python interpreter in interactive mode using :\n";
						scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " python -i " + pythonScriptsPath + File.separator + "DOCoMETRe.py\n";
						scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Then you can run this script file using this command in opened interpreter :\n";
						scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " exec(open(\"" + exportScriptWizardPage.getScriptFileFullPath() + "\").read())\n";
					}
					if(MathEngineFactory.isMatlab()) {
						String matlabScriptsLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION);
						String matlabFunctionsLocation = Path.fromOSString(matlabScriptsLocation).removeLastSegments(1).append("MatlabFunctions").toOSString();
						scriptCode = scriptCode + "addpath('" + matlabScriptsLocation + "');\n";
						scriptCode = scriptCode + "addpath('" + matlabFunctionsLocation + "');\n";
					}
					for (BatchDataProcessingItem subjectItem : subjectsItems) {
						if(subjectItem.isActivated()) {
							String path = subjectItem.getPath();
							IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
							if(resource == null) continue;
							scriptCode = scriptCode + "\n" + MathEngineFactory.getMathEngine().getCommentCharacter() + " -------------> ";
							scriptCode = scriptCode + " Process subject : " + resource.getName() + "\n";
							if(batchDataProcessing.loadSubject()) {
								try {
									if(MathEngineFactory.isMatlab()) {
										scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Unload subject just to be sure it is not loaded\n";
										scriptCode = scriptCode + "try\n";
										scriptCode = scriptCode + "\t" + MathEngineFactory.getMathEngine().getCommandLineToUnloadSubject(resource) + "\n";
									    scriptCode = scriptCode + "catch exception\n";
									    scriptCode = scriptCode + "\tif(~strcmp(exception.identifier, 'MATLAB:UndefinedFunction') && ~strcmp(exception.identifier, 'MATLAB:rmfield:InvalidFieldname'))\n";
									    scriptCode = scriptCode + "\t\trethrow(exception)\n";
									    scriptCode = scriptCode + "\tend\n";
									    scriptCode = scriptCode + "end\n";
									}
									if(MathEngineFactory.isPython()) {
										scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Unload subject just to be sure it is not loaded\n";
										scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommandLineToUnloadSubject(resource) + "\n";
									}
									scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Load subject\n";
									scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommandLineToLoadSubjectFromRawData(resource) + "\n";
								} catch (Exception e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
							}
							scriptCode = scriptCode + MathEngineFactory.getMathEngine().refactor(processesCode, resource);
							if(batchDataProcessing.unloadSubject()) {
								try {
									scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommentCharacter() + " Unload subject\n";
									scriptCode = scriptCode + MathEngineFactory.getMathEngine().getCommandLineToUnloadSubject(resource) + "\n";
								} catch (Exception e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
							}
						}
					}
					monitor.worked(1);
					monitor.subTask(DocometreMessages.SaveScriptFile);
					try {
						Files.write(Paths.get(exportScriptWizardPage.getScriptFileFullPath()), scriptCode.getBytes());
						Activator.logInfoMessage(DocometreMessages.ScriptSavedTo + exportScriptWizardPage.getScriptFileFullPath(), ExportScriptWizard.this.getClass());
					} catch (IOException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					monitor.worked(1);
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		if(anErrorOccured) MessageDialog.openError(getShell(), DocometreMessages.DataExportErrorOccuredDialogTitle, DocometreMessages.DataExportErrorOccuredDialogMessage);
		return true;
	}

}
