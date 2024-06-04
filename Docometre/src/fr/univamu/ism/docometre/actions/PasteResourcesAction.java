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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class PasteResourcesAction extends Action implements ISelectionListener, IWorkbenchAction {

	private IWorkbenchWindow window;
	private CopyResourcesAction copyResourcesAction;
	private IContainer destinationResource;
	private IPath newResourcePath;
	private boolean copyLogAndDataFiles;
	private Object clipboardData;
	private Clipboard clipboard;
	
	public PasteResourcesAction(IWorkbenchWindow window, CopyResourcesAction copyResourcesAction) {
		clipboard = new Clipboard(Display.getCurrent());
		setId("PasteResourcesAction"); //$NON-NLS-1$
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		this.copyResourcesAction = copyResourcesAction;
		setText(DocometreMessages.PasteAction_Text);
        setToolTipText(DocometreMessages.PasteAction_Text);
        setImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_PASTE)); 
        setDisabledImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED)); 
	}
	
	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if(!destinationResource.exists()) return;
					try {
						destinationResource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					clipboardData = null;
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							clipboardData = clipboard.getContents(FileTransfer.getInstance());
							if(clipboardData == null) clipboardData = clipboard.getContents(TextTransfer.getInstance());
						}
					});
					IResource[] resources = new IResource[0];
					boolean fromDocometre = (clipboardData instanceof String) && CopyResourcesAction.UUID.equals(clipboardData);
					if(!fromDocometre) {
						// Copy from clip board if data available
						if(clipboardData instanceof String[]) {
							String[] filesPath = (String[])clipboardData;
							try {
								copyFromClipboard(destinationResource, filesPath, monitor);
								destinationResource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
								ExperimentsView.refresh(destinationResource, null);
							} catch (Exception e) {
								e.printStackTrace();
								Activator.logErrorMessageWithCause(e);
							}
						}
					} else {
						// Copy from internal resources
						//One or more resources already exist in destination
						resources = copyResourcesAction.getCopiedResources();
						ArrayList<IResource> newResources = new ArrayList<IResource>();
						for (IResource resource : resources) {
							try {
								resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
								newResourcePath = destinationResource.getFullPath().append(resource.getName());
								IResource newResource = destinationResource.findMember(newResourcePath.makeRelativeTo(destinationResource.getFullPath()));
								int n = 1;
								while (newResource != null) {
									String fileExtension = resource.getFullPath().getFileExtension();
									String resourceName = resource.getFullPath().removeFileExtension().lastSegment();
									newResourcePath = destinationResource.getFullPath().append(resourceName + "_Copy_" + n + ((fileExtension == null) ? "" : "." + fileExtension));
									newResource = destinationResource.findMember(newResourcePath.makeRelativeTo(destinationResource.getFullPath()));
									n++;
								}
								copyLogAndDataFiles = ResourceType.isSamples(resource) || ResourceType.isLog(resource);
								if(ResourceType.isContainer(resource)) {
									PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
										@Override
										public void run() {
											copyLogAndDataFiles = MessageDialog.openQuestion(PlatformUI.getWorkbench().getDisplay().getActiveShell(), DocometreMessages.CopyLogAndDataFilesDialogTitle, DocometreMessages.CopyLogAndDataFilesDialogQuestion);
										}
									});
								}
								doCopy(resource, newResourcePath, destinationResource, copyLogAndDataFiles, monitor);
								newResource = destinationResource.findMember(newResourcePath.makeRelativeTo(destinationResource.getFullPath()));
								newResources.add(newResource);
							} catch (CoreException e) {
								e.printStackTrace();
								Activator.logErrorMessageWithCause(e);
							}
						}
						resources = newResources.toArray(new IResource[newResources.size()]);
						ExperimentsView.refresh(destinationResource, resources);
					}
					
				}

			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private void copyFromClipboard(IContainer destinationResource, String[] filesPath, IProgressMonitor monitor) throws Exception {
		for (String filePath : filesPath) {
			IPath source = Path.fromOSString(filePath);
			IPath destination = Path.fromOSString(destinationResource.getLocation().toOSString()); 
			destination = destination.append(Path.fromOSString(source.lastSegment()));
			if(source.toFile().isDirectory()) {
				// If source file is folder, create it in destination if not exists and then copy content
				if(!destination.toFile().exists()) destination.toFile().mkdirs();
				IFolder folder = destinationResource.getFolder(destination.makeRelativeTo(destinationResource.getLocation()));
				folder.create(true, true, monitor);
				String[] files = source.toFile().list();
				for (int i = 0; i < files.length; i++) {
					files[i] = source.toOSString() + File.separator + files[i];
				}
				copyFromClipboard(folder, files, monitor);
			} else {
				java.nio.file.Path src = Paths.get(filePath);
				java.nio.file.Path dest = Paths.get(destination.toPortableString());
				Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
	
	protected void doCopy(IResource originResource, IPath newResourcePath, IContainer destinationResource, boolean copyLogAndDataFiles, IProgressMonitor progressMonitor) throws CoreException {
		if(ResourceType.isContainer(originResource)) {
			// Create new container
			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(newResourcePath);
			folder.create(true, true, progressMonitor);
			ResourceProperties.clonePersistentProperties(originResource, folder);
			// Copy members
			IResource[] resources = ((IContainer)originResource).members();
			for (IResource resource : resources) {
				newResourcePath = folder.getFullPath().append(resource.getName());
				doCopy(resource, newResourcePath, folder, copyLogAndDataFiles, progressMonitor);
			}
		} else {
			// Copy file
			boolean doCopy = ResourceType.isLog(originResource) || ResourceType.isSamples(originResource) ?  copyLogAndDataFiles : true;
			if(doCopy) {
				originResource.copy(newResourcePath, false, progressMonitor);
			}
		}
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			// If clipboard not empty, copy from it
			Clipboard clipboard = new Clipboard(Display.getCurrent());
			Object fileData = clipboard.getContents(FileTransfer.getInstance());
			clipboard.dispose();
			destinationResource = null;
			if(selection.isEmpty() && part instanceof ExperimentsView) 
				destinationResource = ResourcesPlugin.getWorkspace().getRoot();
			else if(selection.isEmpty() && part instanceof SubjectsView) {
				destinationResource = ((SubjectsView)part).getInput();
			} else if (selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection).getFirstElement();
				if(element instanceof IContainer) destinationResource = (IContainer) element;
			}
			setEnabled(destinationResource != null && (copyResourcesAction.notEmpty() || fileData != null));
		}
	}
	
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
		clipboard.dispose();
	}
	
}
