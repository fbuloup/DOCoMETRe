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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.handlers.LoadUnloadSubjectsHandler;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class DeleteResourcesAction extends Action implements ISelectionListener, IWorkbenchAction {

	private IResource[] resources;
	private IWorkbenchWindow window;
	private Object object;

	public DeleteResourcesAction(IWorkbenchWindow window) {
		setId("DeleteResourcesAction"); //$NON-NLS-1$
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.DeleteAction_Text);
		setToolTipText(DocometreMessages.DeleteAction_Text);
        setImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        setDisabledImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
	}

	@Override
	public void run() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = DocometreMessages.DeleteAction_Title;
		String message = DocometreMessages.DeleteAction_Message;
		if(MessageDialog.openQuestion(shell, title, message)) {
			
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							monitor.beginTask("Deleting " + resources.length + " resource(s)", resources.length + 3);
							boolean deleteChannel = false;
							boolean deleteResource = false;
							Set<IResource> parentResourcesToRefresh = new HashSet<IResource>();
							for (IResource resource : resources) {
								boolean updateEditors = false;
								monitor.subTask("Deleting " + resource.getFullPath().toOSString());
								if(resource instanceof IProject) {
									if(!((IProject) resource).isOpen()) {
										((IProject) resource).open(null);
									}
								}
								if(ResourceType.isDACQConfiguration(resource)) {
									String fullPath = ResourceProperties.getDefaultDACQPersistentProperty(resource); 
									if(fullPath != null && resource.getFullPath().equals(new Path(fullPath))) ResourceProperties.setDefaultDACQPersistentProperty(resource, null);
									updateEditors = true;
								}
								closeEditors(resource);
								if(ResourceType.isChannel(resource)) {
									IContainer subject = resource.getParent();
									MathEngineFactory.getMathEngine().deleteChannel((Channel)resource);
									try {
										if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
											ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
											channelsContainer.setUpdateChannelsCache(true);
										}
									} catch (CoreException e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
									}
									deleteChannel = true;
								} else if(ResourceType.isSubject(resource) && MathEngineFactory.getMathEngine().isSubjectLoaded(resource)) {
									try {
										ArrayList<IResource> resources = new ArrayList<>();
										resources.add(resource);
										LoadUnloadSubjectsHandler.getInstance().resetSelection(resources);
										LoadUnloadSubjectsHandler.getInstance().execute(null);
									} catch (ExecutionException e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
									}
									MathEngineFactory.getMathEngine().unload(resource);
								} else if(ResourceType.isExperiment(resource)) {
									try {
										IResource[] subjects = ResourceProperties.getAllTypedResources(ResourceType.SUBJECT, (IContainer) resource, null);
										ArrayList<IResource> resources = new ArrayList<>();
										for (IResource subject : subjects) {
											if(MathEngineFactory.getMathEngine().isSubjectLoaded(subject)) resources.add(subject);
										}
										if(LoadUnloadSubjectsHandler.getInstance() != null) {
											LoadUnloadSubjectsHandler.getInstance().resetSelection(resources);
											LoadUnloadSubjectsHandler.getInstance().execute(null);
										}
										MathEngineFactory.getMathEngine().unload(resource);
									} catch (ExecutionException e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
									}
								} else if(ResourceType.isProcess(resource)) {
									boolean removeProcessHandle = false;
									Object object = ResourceProperties.getObjectSessionProperty(resource);
									if(object == null) {
										object = ObjectsController.deserialize((IFile)resource);
										ResourceProperties.setObjectSessionProperty(resource, object);
										ObjectsController.addHandle(object);
										removeProcessHandle = true;
									}
									((Process)object).cleanBuild();
									if(removeProcessHandle) ObjectsController.removeHandle(object);
								}
								if (!deleteChannel) {
									IResource parentResource = resource.getParent();
									resource.delete(true, monitor);
									parentResourcesToRefresh.add(parentResource);
									deleteResource = true;
									if(updateEditors) updateEditors();
								}
								monitor.worked(1);
							}
							
							if(deleteChannel) {
								SubjectsView.refresh();
								monitor.worked(1);
							} 
							if(deleteResource) {
								for (IResource parentResourceToRefresh : parentResourcesToRefresh) {
									ExperimentsView.refresh(parentResourceToRefresh.getProject(), new IResource[]{});
									SubjectsView.refresh();
									FunctionsView.refresh(true);
								}
								monitor.worked(1);
							}
							monitor.done();
						} catch (CoreException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
						
						
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.getLogErrorMessageWithCause(e);
				e.printStackTrace();
			}
			
		}
	}
	
	private void updateEditors() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
					for (IEditorReference editorReference : editors) {
						ResourceEditorInput editorInput = (ResourceEditorInput)editorReference.getEditorInput();
						Object localObject = editorInput.getObject();
						IResource editedResource = ObjectsController.getResourceForObject(localObject);
						if(ResourceType.isProcess(editedResource)) ((PartNameRefresher)editorReference.getEditor(false)).refreshPartName();
					}
				} catch (CoreException e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				}
			}
		});
	}

	private void closeEditors(IResource resource) throws CoreException {
		object = ResourceProperties.getObjectSessionProperty(resource);
		if(ResourceType.isLog(resource) || ResourceType.isParameters(resource) || ResourceType.isSamples(resource) || ResourceType.isChannel(resource)) object = resource;
		if(object != null) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					Activator.closeEditor(object);
				}
			});
		}
		if(resource instanceof IContainer) {
			IResource[] childrenResources = ((IContainer)resource).members();
			for (IResource childResource : childrenResources) {
				closeEditors(childResource);
			}
		}
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			resources = null;
			if (selection instanceof IStructuredSelection) {
				Object[] objects = ((IStructuredSelection) selection).toArray();
				resources = new IResource[objects.length];
				for (int i = 0; i < objects.length; i++) resources[i] = (IResource) objects[i];
			}
			setEnabled(resources != null && resources.length > 0);
		}
	}

	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}

}
