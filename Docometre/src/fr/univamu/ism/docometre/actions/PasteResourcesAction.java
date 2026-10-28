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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
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
import fr.univamu.ism.docometre.dialogs.CopyLogDataFilesDialog;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class PasteResourcesAction extends Action implements ISelectionListener, IWorkbenchAction {

	private IWorkbenchWindow window;
	private CopyResourcesAction copyResourcesAction;
	private IContainer destinationResource;
	private Clipboard clipboard;

	public PasteResourcesAction(IWorkbenchWindow window, CopyResourcesAction copyResourcesAction) {
		clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
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
		if(destinationResource == null || !destinationResource.exists()) return;
		// Keep destination : selection may change while dialogs are open
		IContainer destination = destinationResource;
		Shell shell = window.getShell();
		// We are in UI thread : read clipboard directly
		Object clipboardData = clipboard.getContents(FileTransfer.getInstance());
		if(clipboardData == null) clipboardData = clipboard.getContents(TextTransfer.getInstance());
		boolean fromDocometre = (clipboardData instanceof String) && clipboardData.equals(CopyResourcesAction.UUIDString);

		if(fromDocometre) {
			// Copy from internal resources
			ArrayList<IResource> resources = getResourcesToCopy(copyResourcesAction.getCopiedResources(), destination);
			// Ask all questions before starting the copy, not in the middle of it
			boolean[] copyLogAndDataFiles = askCopyLogAndDataFiles(shell, resources);
			if(copyLogAndDataFiles == null) return;
			ArrayList<IResource> newResources = new ArrayList<>();
			runWithProgress(shell, new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					copyResources(resources, copyLogAndDataFiles, destination, newResources, monitor);
				}
			});
			ExperimentsView.refresh(destination, newResources.toArray(new IResource[newResources.size()]));
		} else if(clipboardData instanceof String[]) {
			// Copy from clip board (files from OS)
			String[] filesPath = (String[])clipboardData;
			runWithProgress(shell, new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					copyFromClipboard(destination, filesPath, monitor);
				}
			});
			ExperimentsView.refresh(destination, null);
		}
	}

	/*
	 * Run workspace operation in a modal and cancelable progress dialog.
	 * All resource changes are notified only once, at the end of the operation.
	 */
	private void runWithProgress(Shell shell, IWorkspaceRunnable workspaceRunnable) {
		try {
			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						// Workspace root rule : sources and destination may be anywhere in the workspace
						ResourcesPlugin.getWorkspace().run(workspaceRunnable, ResourcesPlugin.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, monitor);
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		} catch (InterruptedException e) {
			// Cancelled by user : resources already copied are kept
		}
	}

	private ArrayList<IResource> getResourcesToCopy(IResource[] copiedResources, IContainer destination) {
		ArrayList<IResource> resources = new ArrayList<>();
		if(copiedResources == null) return resources;
		for (IResource resource : copiedResources) {
			if(!resource.exists()) continue;
			// Impossible to copy a resource in itself or in one of its children (infinite recursion)
			if(resource.getFullPath().isPrefixOf(destination.getFullPath())) {
				Activator.logWarningMessage(DocometreMessages.ImpossibleToCopyResourceInItself);
				continue;
			}
			resources.add(resource);
		}
		return resources;
	}

	/*
	 * Return, for each resource, true if log and data files must be copied, or null if user cancelled
	 */
	private boolean[] askCopyLogAndDataFiles(Shell shell, ArrayList<IResource> resources) {
		boolean[] copyLogAndDataFiles = new boolean[resources.size()];
		CopyLogDataFilesDialog.saveChoice = false;
		int choiceID = -1;
		for (int i = 0; i < copyLogAndDataFiles.length; i++) {
			IResource resource = resources.get(i);
			if(!ResourceType.isContainer(resource)) {
				// A log or data file explicitly selected is always copied
				copyLogAndDataFiles[i] = true;
				continue;
			}
			if(!CopyLogDataFilesDialog.saveChoice) choiceID = new CopyLogDataFilesDialog(shell).open();
			if(choiceID == IDialogConstants.CANCEL_ID) return null;
			copyLogAndDataFiles[i] = choiceID == IDialogConstants.YES_ID;
		}
		return copyLogAndDataFiles;
	}

	private void copyResources(ArrayList<IResource> resources, boolean[] copyLogAndDataFiles, IContainer destination, ArrayList<IResource> newResources, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.PasteAction_Text, 10*resources.size() + 1);
		// Only direct children of destination are needed to compute new names
		destination.refreshLocal(IResource.DEPTH_ONE, subMonitor.split(1));
		for (int i = 0; i < copyLogAndDataFiles.length; i++) {
			IResource resource = resources.get(i);
			try {
				resource.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.split(1));
				IPath newResourcePath = getNewResourcePath(resource, destination);
				doCopy(resource, newResourcePath, copyLogAndDataFiles[i], subMonitor.split(9));
				IResource newResource = destination.findMember(newResourcePath.makeRelativeTo(destination.getFullPath()));
				if(newResource != null) newResources.add(newResource);
			} catch (CoreException e) {
				// Log error and continue with next resource
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}

	private IPath getNewResourcePath(IResource resource, IContainer destination) {
		IPath newResourcePath = destination.getFullPath().append(resource.getName());
		IResource newResource = destination.findMember(resource.getName());
		int n = 1;
		while (newResource != null) {
			String fileExtension = resource.getFullPath().getFileExtension();
			String resourceName = resource.getFullPath().removeFileExtension().lastSegment();
			newResourcePath = destination.getFullPath().append(resourceName + "_Copy_" + n + ((fileExtension == null) ? "" : "." + fileExtension));
			newResource = destination.findMember(newResourcePath.makeRelativeTo(destination.getFullPath()));
			n++;
		}
		return newResourcePath;
	}

	protected void doCopy(IResource originResource, IPath newResourcePath, boolean copyLogAndDataFiles, IProgressMonitor progressMonitor) throws CoreException {
		if(ResourceType.isContainer(originResource)) {
			IResource[] resources = ((IContainer)originResource).members();
			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, resources.length + 1);
			subMonitor.subTask(originResource.getFullPath().toString());
			// Create new container
			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(newResourcePath);
			folder.create(true, true, subMonitor.split(1));
			ResourceProperties.clonePersistentProperties(originResource, folder);
			// Copy members : split() throws OperationCanceledException if user cancelled
			for (IResource resource : resources) {
				doCopy(resource, folder.getFullPath().append(resource.getName()), copyLogAndDataFiles, subMonitor.split(1));
			}
		} else {
			// Copy file
			boolean doCopy = ResourceType.isLog(originResource) || ResourceType.isSamples(originResource) ?  copyLogAndDataFiles : true;
			if(doCopy) {
				originResource.copy(newResourcePath, false, progressMonitor);
			}
		}
	}

	private void copyFromClipboard(IContainer destination, String[] filesPath, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.PasteAction_Text, 2*filesPath.length + 1);
		File destinationFolder = destination.getLocation().toFile();
		ArrayList<String> copiedNames = new ArrayList<>();
		for (String filePath : filesPath) {
			File source = new File(filePath);
			File target = new File(destinationFolder, source.getName());
			// Impossible to copy a folder in itself or in one of its children (infinite recursion)
			if(target.toPath().toAbsolutePath().normalize().startsWith(source.toPath().toAbsolutePath().normalize())) {
				Activator.logWarningMessage(DocometreMessages.ImpossibleToCopyResourceInItself);
				continue;
			}
			try {
				subMonitor.subTask(source.getName());
				copyFile(source, target, subMonitor.split(1));
				copiedNames.add(source.getName());
			} catch (IOException e) {
				// Log error and continue with next file
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
		// Synchronize workspace with copied files only, not whole destination tree
		destination.refreshLocal(IResource.DEPTH_ONE, subMonitor.split(1));
		for (String copiedName : copiedNames) {
			IResource newResource = destination.findMember(copiedName);
			if(newResource != null) newResource.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.split(1));
		}
	}

	private void copyFile(File source, File destination, IProgressMonitor monitor) throws IOException {
		if(monitor.isCanceled()) throw new OperationCanceledException();
		if(source.isDirectory()) {
			// If source file is folder, create it in destination if not exists and then copy content
			if(!destination.exists()) destination.mkdirs();
			File[] files = source.listFiles();
			if(files == null) return;
			SubMonitor subMonitor = SubMonitor.convert(monitor, files.length);
			for (File file : files) copyFile(file, new File(destination, file.getName()), subMonitor.split(1));
		} else {
			Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			destinationResource = null;
			if(selection.isEmpty() && part instanceof ExperimentsView)
				destinationResource = ResourcesPlugin.getWorkspace().getRoot();
			else if(selection.isEmpty() && part instanceof SubjectsView) {
				destinationResource = ((SubjectsView)part).getInput();
			} else if (selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection).getFirstElement();
				if(element instanceof IContainer) destinationResource = (IContainer) element;
			}
			setEnabled(destinationResource != null && (copyResourcesAction.notEmpty() || clipboardContainsFiles()));
		}
	}

	/*
	 * Only check available types : clipboard contents is not read on each selection change
	 */
	private boolean clipboardContainsFiles() {
		for (TransferData transferData : clipboard.getAvailableTypes()) {
			if(FileTransfer.getInstance().isSupportedType(transferData)) return true;
		}
		return false;
	}

	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
		clipboard.dispose();
	}

}
