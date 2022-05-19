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
package fr.univamu.ism.docometre.analyse.handlers;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public final class RunBatchDataProcessingDelegate {
	
	public static boolean run(BatchDataProcessing batchDataProcessing, IProgressMonitor monitor) {
		// Get all data processing
		monitor.subTask(DocometreMessages.GetAllDataProcessingLabel);
		BatchDataProcessingItem[] processes = batchDataProcessing.getProcesses();
		ArrayList<IResource> processesResource = new ArrayList<>();
		for (BatchDataProcessingItem batchDataProcessingItem : processes) {
			if(batchDataProcessingItem.isActivated()) {
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(batchDataProcessingItem.getPath());
				if(resource != null) processesResource.add(resource);
			}
		}
		if(monitor.isCanceled()) return true;
		// Get all subjects
		monitor.subTask(DocometreMessages.GetAllSubjectsLabel);
		BatchDataProcessingItem[] subjects = batchDataProcessing.getSubjects();
		ArrayList<IResource> subjectsResource = new ArrayList<>();
		for (BatchDataProcessingItem batchDataProcessingItem : subjects) {
			if(batchDataProcessingItem.isActivated()) {
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(batchDataProcessingItem.getPath());
				if(resource != null) subjectsResource.add(resource);
			}
		}
		if(monitor.isCanceled()) return true;
		// For each subject
		for (IResource subjectResource : subjectsResource) {
			// Load subject if necessary
			boolean loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subjectResource);
			boolean checkUnload = false;
			if(batchDataProcessing.loadSubject() && !loaded) {
				checkUnload = true;
				String message = NLS.bind(DocometreMessages.LoadingLabel, subjectResource.getName());
				monitor.subTask(message);
				boolean loadFromSavedFile = Activator.getDefault().getPreferenceStore().getBoolean(MathEnginePreferencesConstants.ALWAYS_LOAD_FROM_SAVED_DATA);
				MathEngineFactory.getMathEngine().load(subjectResource, loadFromSavedFile);
				loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subjectResource);
			}
			if(monitor.isCanceled()) return true;
			if(loaded) {
				// Generate global script
				monitor.subTask(DocometreMessages.GenerateGlobalScriptLabel);
				String code = "";
				for (IResource resource : processesResource) {
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
							code = code + script.getLoopCode(object, ScriptSegmentType.LOOP, subjectResource) + "\n";
						} catch (Exception e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
					if(removeHandle) ObjectsController.removeHandle(object);
				}
				if(monitor.isCanceled()) return true;
				// Run global script on current subject
				String message = NLS.bind(DocometreMessages.ProcessingLabel, subjectResource.getName());
				monitor.subTask(message);
				code = MathEngineFactory.getMathEngine().refactor(code, subjectResource);
				System.out.println(code);
				MathEngineFactory.getMathEngine().runScript(code);
				UpdateWorkbenchDelegate.update();
				if(monitor.isCanceled()) return true;
				// Save subject if auto unload
				if(batchDataProcessing.unloadSubject() && checkUnload) {
					boolean isModified = ResourceProperties.isSubjectModified(subjectResource);
					if(isModified) {
						message = NLS.bind(DocometreMessages.SavingLabel, subjectResource.getName());
						monitor.subTask(message);
						MathEngineFactory.getMathEngine().saveSubject(subjectResource);
						ResourceProperties.setSubjectModified(subjectResource, false);
					}
					message = NLS.bind(DocometreMessages.UnloadingLabel, subjectResource.getName());
					monitor.subTask(message);
					MathEngineFactory.getMathEngine().unload(subjectResource);
				}
			} 
		}
		return false;
	}

}
