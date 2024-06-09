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

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
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
import fr.univamu.ism.docometre.views.ExperimentsView;

public class CompileProcessAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private static String ID = "CompileProcessAction";
	
	private class CompileJob extends Job {
		
		private IFile resource;
		private Process process;
		private boolean removeProcessHandle;
		private boolean removeDACQHandle;

		public CompileJob(String name, IFile resource, Process process, boolean removeProcessHandle, boolean removeDACQHandle) {
			super(name);
			this.resource = resource;
			this.process = process;
			this.removeProcessHandle = removeProcessHandle;
			this.removeDACQHandle = removeDACQHandle;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Exception exception = null; 
			try {
				String message = NLS.bind(DocometreMessages.CompileProcessAction_JobMessage, resource.getName().replaceAll(Activator.processFileExtension, ""));
				monitor.beginTask(message, 3);
				process.getScript().clearCodeGenerationStatus();
				process.compile(monitor);
			} catch (Exception e) {
				exception = e;
			} finally {
				if(removeDACQHandle) ObjectsController.removeHandle(process.getDACQConfiguration());
				if(removeProcessHandle) ObjectsController.removeHandle(process);
				ExperimentsView.refresh(resource.getProject(), new IResource[] {resource});
			}
			if(exception == null) {
				String message = NLS.bind(DocometreMessages.CompileProcessAction_CompileOK, resource.getName().replaceAll(Activator.processFileExtension, ""));
				Activator.logInfoMessage(message, CompileProcessAction.class);
				return Status.OK_STATUS;
			} else {
				String message = NLS.bind(DocometreMessages.CompileProcessAction_CompileKO, resource.getName().replaceAll(Activator.processFileExtension, ""));
				return new Status(Status.ERROR, Activator.PLUGIN_ID, message, exception);
			}
		}
		
	}

	private IWorkbenchWindow window;
	private IFile[] resources;
	
	public CompileProcessAction(IWorkbenchWindow window) {
		setId(ID);//$NON-NLS-1$
		setActionDefinitionId(ID);
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.CompileProcessAction_Text);
		setToolTipText(DocometreMessages.CompileProcessAction_Text);
	}
	
	public void setSelection(IFile[] resources) {
		this.resources = resources;
	}
	
	@Override
	public void run() {
		
		for (int i = 0; i < resources.length; i++) {
			IFile resource = resources[i];
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
			String fullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
			if(fullPath != null) {
				IFile daqConfigurationFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fullPath)) ;
				if(daqConfigurationFile.exists())
					if(ResourceProperties.getObjectSessionProperty(daqConfigurationFile) == null) 
						removeDACQHandle = true;
				
				Job compileJob = new CompileJob(DocometreMessages.CompileProcessAction_JobTitle, resource, process, removeProcessHandle, removeDACQHandle);
				compileJob.setUser(true);
				compileJob.setRule(resource.getWorkspace().getRoot());
				compileJob.schedule();
			} else {
				Activator.logWarningMessage(DocometreMessages.CompileProcessAction_ImpossibleToCompileProcessWhenNoAssociatedDAQ);
				if(removeProcessHandle) ObjectsController.removeHandle(process);
			}
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
