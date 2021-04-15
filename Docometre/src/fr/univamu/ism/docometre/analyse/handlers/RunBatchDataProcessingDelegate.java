package fr.univamu.ism.docometre.analyse.handlers;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;
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
					code = code + script.getLoopCode(object, ScriptSegmentType.LOOP) + "\n";
				} catch (Exception e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(removeHandle) ObjectsController.removeHandle(object);
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
				// Run global script on current subject
				String message = NLS.bind(DocometreMessages.ProcessingLabel, subjectResource.getName());
				monitor.subTask(message);
				code = MathEngineFactory.getMathEngine().refactor(code, subjectResource);
				MathEngineFactory.getMathEngine().runScript(code);
				// Set subject as modified
				IResource[] modifiedSubjects = MathEngineFactory.getMathEngine().getCreatedOrModifiedSubjects();
				ResourceProperties.setSubjectModified(modifiedSubjects[0], true);// modifiedSubjects[0] must be equals to subjectResource
				// Update channels cache
				try {
					if(modifiedSubjects[0].getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && modifiedSubjects[0].getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
						ChannelsContainer channelsContainer = (ChannelsContainer)modifiedSubjects[0].getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
						channelsContainer.setUpdateChannelsCache(true);
					}
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				} 
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
		// Update GUI
		for (IResource subjectResource : subjectsResource) {
			ExperimentsView.refresh(subjectResource, null);
			SubjectsView.refresh(subjectResource, null);
		}
		
		return false;
	}

}
