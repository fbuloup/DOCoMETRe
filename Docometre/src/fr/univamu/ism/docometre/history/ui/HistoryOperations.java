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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [04/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.history.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.history.FileChange;
import fr.univamu.ism.docometre.history.GitResources;
import fr.univamu.ism.docometre.history.IncompleteRestoreException;
import fr.univamu.ism.docometre.history.Snapshot;
import fr.univamu.ism.docometre.history.SnapshotException;
import fr.univamu.ism.docometre.history.SnapshotManager;
import fr.univamu.ism.docometre.history.SnapshotRepository;
import fr.univamu.ism.docometre.history.SnapshotType;
import fr.univamu.ism.docometre.views.DescriptionView;
import fr.univamu.ism.docometre.views.ExperimentsLabelDecorator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;

/**
 * User operations on experiments histories, shared by menus and snapshots view.
 * Operations which modify an experiment run in a workspace job with experiment as
 * scheduling rule.
 */
public final class HistoryOperations {
	
	private HistoryOperations() {
	}
	
	/*
	 * Return opened experiment of selected element (selection, resource or adaptable),
	 * null if there is none
	 */
	public static IProject getExperiment(Object element) {
		try {
			if(element instanceof IStructuredSelection) {
				IStructuredSelection selection = (IStructuredSelection) element;
				if(selection.size() != 1) return null;
				element = selection.getFirstElement();
			}
			if(!(element instanceof IResource) && element instanceof IAdaptable) element = ((IAdaptable)element).getAdapter(IResource.class);
			if(!(element instanceof IResource)) return null;
			IProject project = ((IResource)element).getProject();
			return project != null && project.isOpen() ? project : null;
		} catch (RuntimeException e) {
			// e.g. channels which are not linked to a subject
			return null;
		}
	}
	
	public static boolean isHistorized(IProject project) {
		return project != null && SnapshotManager.getInstance().isHistorized(project);
	}
	
	private static boolean isAcquisitionRunning(Shell shell) {
		if(!ExperimentScheduler.getInstance().isRunning()) return false;
		MessageDialog.openInformation(shell, DocometreMessages.History_ErrorTitle, DocometreMessages.History_AcquisitionRunning);
		return true;
	}
	
	/*
	 * Ask user confirmation, create experiment history and its first snapshot
	 */
	public static void enableHistory(Shell shell, IProject project) {
		if(project == null || isHistorized(project) || isAcquisitionRunning(shell)) return;
		String message = NLS.bind(DocometreMessages.History_EnableMessage, project.getName());
		if(!MessageDialog.openQuestion(shell, DocometreMessages.History_EnableTitle, message)) return;
		// Snapshots contain files, not unsaved editors contents
		if(!PlatformUI.getWorkbench().saveAllEditors(true)) return;
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_EnableJob, project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					prepareExperiment(project, monitor);
					SnapshotRepository repository = SnapshotManager.getInstance().enableHistory(project);
					repository.createSnapshot(DocometreMessages.History_InitialSnapshotName, null, SnapshotType.AUTO, SnapshotManager.getDefaultAuthor());
					project.refreshLocal(IResource.DEPTH_ONE, monitor);
					Activator.logInfoMessage(NLS.bind(DocometreMessages.History_HistoryEnabled, project.getName()), HistoryOperations.class);
				} catch (SnapshotException e) {
					return getErrorStatus(project, e);
				} finally {
					SnapshotsView.show(project);
					HistoryStatusCache.update(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/*
	 * Ask snapshot name and description, then create a snapshot of experiment current content
	 */
	public static void createSnapshot(Shell shell, IProject project) {
		if(!isHistorized(project) || isAcquisitionRunning(shell)) return;
		// Snapshots contain files, not unsaved editors contents
		if(!PlatformUI.getWorkbench().saveAllEditors(true)) return;
		CreateSnapshotDialog dialog = new CreateSnapshotDialog(shell, project.getName());
		if(dialog.open() != Window.OK) return;
		String name = dialog.getSnapshotName();
		String description = dialog.getSnapshotDescription();
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_CreateSnapshotJob, project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
					if(repository == null) return Status.OK_STATUS;
					prepareExperiment(project, monitor);
					Snapshot snapshot = repository.createSnapshot(name, description, SnapshotType.MANUAL, SnapshotManager.getDefaultAuthor());
					if(snapshot == null) {
						String message = NLS.bind(DocometreMessages.History_NoChange, project.getName());
						Display.getDefault().asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(), DocometreMessages.History_CreateSnapshotTitle, message));
					} else {
						Activator.logInfoMessage(NLS.bind(DocometreMessages.History_SnapshotCreated, new Object[] {snapshot.number(), snapshot.name(), project.getName()}), HistoryOperations.class);
						scheduleOptimization(project);
					}
				} catch (SnapshotException e) {
					return getErrorStatus(project, e);
				} finally {
					historyChanged(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/*
	 * Ask user confirmation, then delete experiment history (all snapshots). Experiment files
	 * are not modified.
	 */
	public static void disableHistory(Shell shell, IProject project) {
		if(!isHistorized(project) || isAcquisitionRunning(shell)) return;
		int snapshotsNumber = 0;
		try {
			snapshotsNumber = SnapshotManager.getInstance().getRepository(project).getSnapshots().size();
		} catch (SnapshotException e) {
			Activator.logErrorMessageWithCause(e);
		}
		String message = NLS.bind(DocometreMessages.History_DisableMessage, project.getName(), snapshotsNumber);
		MessageDialog dialog = new MessageDialog(shell, DocometreMessages.History_DisableTitle, null, message, MessageDialog.WARNING, 1, DocometreMessages.History_DisableTitle, IDialogConstants.CANCEL_LABEL);
		if(dialog.open() != 0) return;
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_DisableJob, project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					File remainingFolder = SnapshotManager.getInstance().disableHistory(project);
					if(remainingFolder == null) {
						GitResources.includeGitFolder(project, monitor);
						Activator.logInfoMessage(NLS.bind(DocometreMessages.History_HistoryDisabled, project.getName()), HistoryOperations.class);
					} else {
						Activator.logWarningMessage(NLS.bind(DocometreMessages.History_HistoryNotCompletelyDeleted, project.getName(), remainingFolder.getAbsolutePath()));
					}
					project.refreshLocal(IResource.DEPTH_ONE, monitor);
				} catch (SnapshotException e) {
					return getErrorStatus(project, e);
				} finally {
					historyChanged(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Restoration
	
	/*
	 * Restore whole experiment content to the content of snapshot, after user confirmation :
	 *  - all editors are closed (modified ones may be saved),
	 *  - subjects loaded in math engine are unloaded (modified ones may be saved),
	 *  - changes since the last snapshot are saved in an automatic snapshot,
	 *  - a new snapshot with the content of restored snapshot is created,
	 *  - experiment properties and cached objects are read again, views are refreshed.
	 * No snapshot is ever deleted.
	 */
	public static void restoreSnapshot(Shell shell, IProject project, Snapshot snapshot) {
		if(!isHistorized(project) || snapshot == null || isAcquisitionRunning(shell)) return;
		
		// What will be done
		List<FileChange> restoreChanges;
		List<FileChange> currentChanges;
		Snapshot lastSnapshot;
		int nextNumber;
		try {
			SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
			restoreChanges = repository.getRestoreChanges(snapshot);
			currentChanges = repository.getChanges();
			lastSnapshot = repository.getLastSnapshot();
			nextNumber = repository.getNextNumber();
		} catch (SnapshotException e) {
			ErrorDialog.openError(shell, DocometreMessages.History_RestoreTitle, null, getErrorStatus(project, e));
			return;
		}
		if(restoreChanges.isEmpty()) {
			MessageDialog.openInformation(shell, DocometreMessages.History_RestoreTitle, NLS.bind(DocometreMessages.History_RestoreNothingToDo, project.getName(), snapshot.number()));
			return;
		}
		List<IResource> loadedSubjects = getLoadedSubjects(project);
		ArrayList<String> loadedSubjectsNames = new ArrayList<>();
		ArrayList<String> modifiedSubjectsNames = new ArrayList<>();
		for (IResource subject : loadedSubjects) {
			loadedSubjectsNames.add(subject.getName());
			if(ResourceProperties.isSubjectModified(subject)) modifiedSubjectsNames.add(subject.getName());
		}
		
		ArrayList<String> steps = new ArrayList<>();
		steps.add(DocometreMessages.History_RestoreStepEditors);
		if(!loadedSubjects.isEmpty()) steps.add(NLS.bind(DocometreMessages.History_RestoreStepUnload, String.join(", ", loadedSubjectsNames)));
		int autoSnapshotNumber = nextNumber;
		if(!currentChanges.isEmpty()) steps.add(NLS.bind(DocometreMessages.History_RestoreStepAutoSnapshot, currentChanges.size(), nextNumber++));
		int[] counts = new int[FileChange.Kind.values().length];
		int dataFiles = 0;
		for (FileChange change : restoreChanges) {
			counts[change.kind().ordinal()]++;
			if(change.kind() == FileChange.Kind.DELETED && isDataFile(change.path())) dataFiles++;
		}
		steps.add(NLS.bind(DocometreMessages.History_RestoreStepRestore, new Object[] {nextNumber, snapshot.number(), counts[FileChange.Kind.MODIFIED.ordinal()], counts[FileChange.Kind.DELETED.ordinal()], counts[FileChange.Kind.ADDED.ordinal()]}));
		if(dataFiles > 0) steps.add(NLS.bind(DocometreMessages.History_RestoreDataFilesWarning, dataFiles));
		steps.add(NLS.bind(DocometreMessages.History_RestoreNothingLost, currentChanges.isEmpty() ? lastSnapshot.number() : autoSnapshotNumber));
		
		String question = NLS.bind(DocometreMessages.History_RestoreQuestion, new Object[] {snapshot.number(), snapshot.name(), project.getName()});
		RestoreConfirmationDialog dialog = new RestoreConfirmationDialog(shell, question, dataFiles > 0, steps, restoreChanges, modifiedSubjectsNames.isEmpty() ? null : String.join(", ", modifiedSubjectsNames));
		if(dialog.open() != 0) return;
		boolean saveSubjects = dialog.isSaveSubjects();
		
		// Close all editors : their contents may be replaced (user may save modified ones or cancel)
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if(page != null && !page.closeAllEditors(true)) return;
		
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_RestoreJob, snapshot.number(), project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					unloadSubjects(loadedSubjects, saveSubjects);
					SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
					if(repository == null) return Status.OK_STATUS;
					prepareExperiment(project, monitor);
					PersonIdent author = SnapshotManager.getDefaultAuthor();
					if(repository.hasChanges()) repository.createSnapshot(DocometreMessages.History_AutoSnapshotBeforeRestore, null, SnapshotType.AUTO, author);
					Snapshot restoredSnapshot = repository.restore(snapshot, NLS.bind(DocometreMessages.History_RestoreSnapshotName, snapshot.name()), author);
					reloadExperiment(project, monitor);
					if(restoredSnapshot != null) Activator.logInfoMessage(NLS.bind(DocometreMessages.History_SnapshotRestored, new Object[] {project.getName(), snapshot.number(), snapshot.name(), restoredSnapshot.number()}), HistoryOperations.class);
					// Automatic snapshot and restoration snapshot
					scheduleOptimization(project);
				} catch (IncompleteRestoreException e) {
					Activator.logErrorMessageWithCause(e);
					reloadExperiment(project, monitor);
					askRetry(project, e);
				} catch (SnapshotException e) {
					reloadExperiment(project, monitor);
					return getErrorStatus(project, e);
				} finally {
					historyChanged(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Automatic snapshots
	
	/*
	 * Before a risky operation on resources (deletion, renaming), save unsaved changes of their
	 * historized experiments in automatic snapshots, if enabled in preferences. Experiments
	 * themselves are not concerned : deleting an experiment deletes its history.
	 * Runs in the calling thread, errors are logged and do not prevent the operation.
	 */
	public static void createAutomaticSnapshots(IResource[] resources, String nameMessage) {
		if(resources == null || !Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.HISTORY_AUTOMATIC_SNAPSHOTS)) return;
		LinkedHashMap<IProject, ArrayList<String>> resourcesNames = new LinkedHashMap<>();
		for (IResource resource : resources) {
			try {
				if(resource == null || resource instanceof IProject || resource.getProject() == null || resource.getProjectRelativePath() == null) continue;
				if(!isHistorized(resource.getProject())) continue;
				resourcesNames.computeIfAbsent(resource.getProject(), project -> new ArrayList<>()).add(resource.getName());
			} catch (RuntimeException e) {
				// e.g. channels which are not workspace resources
			}
		}
		for (IProject project : resourcesNames.keySet()) {
			ArrayList<String> names = resourcesNames.get(project);
			String resourcesLabel = names.size() <= 3 ? String.join(", ", names) : String.join(", ", names.subList(0, 3)) + "...";
			String name = NLS.bind(nameMessage, resourcesLabel);
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
				ResourceProperties.synchronizePropertiesFile(project);
				project.refreshLocal(IResource.DEPTH_ONE, null);
				Snapshot snapshot = SnapshotManager.getInstance().getRepository(project).createSnapshot(name, null, SnapshotType.AUTO, SnapshotManager.getDefaultAuthor());
				if(snapshot != null) {
					Activator.logInfoMessage(NLS.bind(DocometreMessages.History_AutomaticSnapshotCreated, new Object[] {project.getName(), snapshot.number(), snapshot.name()}), HistoryOperations.class);
					historyChanged(project);
					scheduleOptimization(project);
				}
			} catch (CoreException | SnapshotException | RuntimeException e) {
				Activator.logErrorMessageWithCause(e);
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// One file
	
	/*
	 * Show differences of file path from snapshot "from" to snapshot "to" (null : current state).
	 * "from" may also be null (first snapshot) : the whole content is then added.
	 */
	public static void showDifferences(Shell shell, IProject project, Snapshot from, Snapshot to, String path) {
		if(!isHistorized(project) || path == null) return;
		SnapshotRepository.FileDiff[] fileDiff = new SnapshotRepository.FileDiff[1];
		SnapshotException[] exception = new SnapshotException[1];
		BusyIndicator.showWhile(shell.getDisplay(), () -> {
			try {
				fileDiff[0] = SnapshotManager.getInstance().getRepository(project).getDiff(from, to, path);
			} catch (SnapshotException e) {
				exception[0] = e;
			}
		});
		if(exception[0] != null) {
			ErrorDialog.openError(shell, DocometreMessages.History_ShowDiffAction, null, getErrorStatus(project, exception[0]));
			return;
		}
		String fromLabel = from == null ? "\u2205" : NLS.bind(DocometreMessages.History_DiffSnapshot, from.number());
		String toLabel = to == null ? DocometreMessages.SnapshotsView_CurrentState : NLS.bind(DocometreMessages.History_DiffSnapshot, to.number());
		String title = NLS.bind(DocometreMessages.History_DiffTitle, SnapshotsView.getPathLabel(path));
		new DiffDialog(shell, title, fromLabel + "  \u2192  " + toLabel, fileDiff[0]).open();
	}
	
	/*
	 * Restore only file path as it was in snapshot (it is removed if it did not exist), after user
	 * confirmation. Like a whole restoration, changes since the last snapshot are first saved in an
	 * automatic snapshot and a new snapshot is created.
	 */
	public static void restoreFile(Shell shell, IProject project, Snapshot snapshot, String path) {
		if(!isHistorized(project) || snapshot == null || path == null || isAcquisitionRunning(shell)) return;
		FileChange.Kind kind = null;
		List<FileChange> currentChanges;
		Snapshot lastSnapshot;
		int nextNumber;
		try {
			SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
			for (FileChange change : repository.getRestoreChanges(snapshot)) {
				if(change.path().equals(path)) kind = change.kind();
			}
			currentChanges = repository.getChanges();
			lastSnapshot = repository.getLastSnapshot();
			nextNumber = repository.getNextNumber();
		} catch (SnapshotException e) {
			ErrorDialog.openError(shell, DocometreMessages.History_RestoreFileTitle, null, getErrorStatus(project, e));
			return;
		}
		String pathLabel = SnapshotsView.getPathLabel(path);
		if(kind == null) {
			MessageDialog.openInformation(shell, DocometreMessages.History_RestoreFileTitle, NLS.bind(DocometreMessages.History_RestoreFileNothingToDo, pathLabel, snapshot.number()));
			return;
		}
		// Subjects loaded in math engine which contain the file
		ArrayList<IResource> loadedSubjects = new ArrayList<>();
		ArrayList<String> loadedSubjectsNames = new ArrayList<>();
		for (IResource subject : getLoadedSubjects(project)) {
			if(path.startsWith(subject.getProjectRelativePath().toPortableString() + "/")) {
				loadedSubjects.add(subject);
				loadedSubjectsNames.add(subject.getName());
			}
		}
		
		StringBuilder message = new StringBuilder(NLS.bind(DocometreMessages.History_RestoreFileQuestion, new Object[] {pathLabel, snapshot.number(), snapshot.name()}));
		message.append("\n\n");
		if(kind == FileChange.Kind.ADDED) message.append(DocometreMessages.History_RestoreFileRecreated);
		else if(kind == FileChange.Kind.DELETED) message.append(DocometreMessages.History_RestoreFileRemoved);
		else message.append(DocometreMessages.History_RestoreFileReplaced);
		message.append("\n\n  \u2022 ").append(DocometreMessages.History_RestoreStepEditors);
		if(!loadedSubjects.isEmpty()) message.append("\n  \u2022 ").append(NLS.bind(DocometreMessages.History_RestoreStepUnload, String.join(", ", loadedSubjectsNames)));
		int autoSnapshotNumber = nextNumber;
		if(!currentChanges.isEmpty()) message.append("\n  \u2022 ").append(NLS.bind(DocometreMessages.History_RestoreStepAutoSnapshot, currentChanges.size(), nextNumber++));
		String snapshotName = NLS.bind(DocometreMessages.History_RestoreFileSnapshotName, pathLabel, snapshot.name());
		message.append("\n  \u2022 ").append(NLS.bind(DocometreMessages.History_RestoreFileStepRestore, nextNumber, snapshotName));
		message.append("\n  \u2022 ").append(NLS.bind(DocometreMessages.History_RestoreNothingLost, currentChanges.isEmpty() ? lastSnapshot.number() : autoSnapshotNumber));
		MessageDialog dialog = new MessageDialog(shell, DocometreMessages.History_RestoreFileTitle, null, message.toString(), MessageDialog.QUESTION, 1, DocometreMessages.History_RestoreButton, IDialogConstants.CANCEL_LABEL);
		if(dialog.open() != 0) return;
		
		// Close all editors : the restored file may be used by objects of other editors (e.g. a DACQ
		// configuration by processes), and deserialized objects are read again after restoration
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if(page != null && !page.closeAllEditors(true)) return;
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_RestoreJob, snapshot.number(), project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					unloadSubjects(loadedSubjects, true);
					SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
					if(repository == null) return Status.OK_STATUS;
					prepareExperiment(project, monitor);
					PersonIdent author = SnapshotManager.getDefaultAuthor();
					if(repository.hasChanges()) repository.createSnapshot(DocometreMessages.History_AutoSnapshotBeforeRestore, null, SnapshotType.AUTO, author);
					Snapshot restoredSnapshot = repository.restore(snapshot, path, snapshotName, author);
					reloadExperiment(project, monitor);
					if(restoredSnapshot != null) Activator.logInfoMessage(NLS.bind(DocometreMessages.History_FileRestored, new Object[] {project.getName(), pathLabel, snapshot.number(), restoredSnapshot.number()}), HistoryOperations.class);
					// Automatic snapshot and restoration snapshot
					scheduleOptimization(project);
				} catch (IncompleteRestoreException e) {
					Activator.logErrorMessageWithCause(e);
					reloadExperiment(project, monitor);
					askRetry(project, e);
				} catch (SnapshotException e) {
					reloadExperiment(project, monitor);
					return getErrorStatus(project, e);
				} finally {
					historyChanged(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Snapshot deletion
	
	/*
	 * Permanently delete snapshot after user confirmation (file versions which only exist in
	 * this snapshot are listed). Experiment files are not modified.
	 */
	public static void deleteSnapshot(Shell shell, IProject project, Snapshot snapshot) {
		if(!isHistorized(project) || snapshot == null) return;
		List<Snapshot> snapshots;
		List<String> lostFiles;
		List<FileChange> currentChanges = new ArrayList<>();
		try {
			SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
			if(repository == null) return;
			snapshots = repository.getSnapshots();
			lostFiles = repository.getFilesOnlyIn(snapshot);
			if(!snapshots.isEmpty() && snapshots.get(0).commitId().equals(snapshot.commitId())) {
				// Properties only modified in workspace (descriptions, trials states...) are changes too
				synchronizeProperties(project, null);
				currentChanges = repository.getChanges();
			}
		} catch (SnapshotException | CoreException e) {
			ErrorDialog.openError(shell, DocometreMessages.History_DeleteSnapshotTitle, null, getErrorStatus(project, e));
			return;
		}
		if(snapshots.size() <= 1) {
			MessageDialog.openInformation(shell, DocometreMessages.History_DeleteSnapshotTitle, NLS.bind(DocometreMessages.History_DeleteOnlySnapshot, snapshot.number(), project.getName()));
			return;
		}
		boolean isLast = snapshots.get(0).commitId().equals(snapshot.commitId());
		Snapshot previousSnapshot = snapshots.get(1);
		List<IResource> loadedSubjects = isLast ? getLoadedSubjects(project) : new ArrayList<>();
		ArrayList<String> loadedSubjectsNames = new ArrayList<>();
		ArrayList<String> modifiedSubjectsNames = new ArrayList<>();
		for (IResource subject : loadedSubjects) {
			loadedSubjectsNames.add(subject.getName());
			if(ResourceProperties.isSubjectModified(subject)) modifiedSubjectsNames.add(subject.getName());
		}
		// Editors are closed when going back : their modifications must have been saved before
		List<String> dirtyEditorsNames = isLast ? getDirtyEditorsNames(project) : new ArrayList<>();
		// Going back to previous content is only possible if nothing unsaved would be lost
		boolean goBackAvailable = isLast && currentChanges.isEmpty() && modifiedSubjectsNames.isEmpty() && dirtyEditorsNames.isEmpty() && !ExperimentScheduler.getInstance().isRunning();
		
		StringBuilder message = new StringBuilder(NLS.bind(DocometreMessages.History_DeleteSnapshotQuestion, new Object[] {snapshot.number(), snapshot.name(), project.getName()}));
		if(isLast) {
			message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteLastSnapshotChoice, previousSnapshot.number()));
			if(goBackAvailable) {
				message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteGoBackDetails, previousSnapshot.number()));
				message.append("\n  \u2022 ").append(DocometreMessages.History_RestoreStepEditors);
				if(!loadedSubjects.isEmpty()) message.append("\n  \u2022 ").append(NLS.bind(DocometreMessages.History_RestoreStepUnload, String.join(", ", loadedSubjectsNames)));
			} else if(!currentChanges.isEmpty()) {
				message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteGoBackUnavailableChanges, previousSnapshot.number(), currentChanges.size()));
			} else if(!modifiedSubjectsNames.isEmpty()) {
				message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteGoBackUnavailableSubjects, previousSnapshot.number(), String.join(", ", modifiedSubjectsNames)));
			} else if(!dirtyEditorsNames.isEmpty()) {
				message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteGoBackUnavailableEditors, previousSnapshot.number(), String.join(", ", dirtyEditorsNames)));
			} else {
				message.append("\n\n").append(DocometreMessages.History_AcquisitionRunning);
			}
		} else {
			message.append("\n\n").append(DocometreMessages.History_DeleteSnapshotKeep);
			if(lostFiles.isEmpty()) {
				message.append("\n\n").append(DocometreMessages.History_DeleteSnapshotNoLostFile);
			} else {
				message.append("\n\n").append(NLS.bind(DocometreMessages.History_DeleteSnapshotLostFiles, lostFiles.size()));
				for (int i = 0; i < Math.min(10, lostFiles.size()); i++) message.append("\n  ").append(SnapshotsView.getPathLabel(lostFiles.get(i)));
				if(lostFiles.size() > 10) message.append("\n  ...");
			}
		}
		message.append("\n\n").append(DocometreMessages.History_DeleteSnapshotIrreversible);
		
		MessageDialog dialog;
		if(isLast) {
			String[] buttons = new String[] {NLS.bind(DocometreMessages.History_DeleteAndGoBackButton, previousSnapshot.number()), DocometreMessages.History_DeleteAndKeepFilesButton, IDialogConstants.CANCEL_LABEL};
			dialog = new MessageDialog(shell, DocometreMessages.History_DeleteSnapshotTitle, null, message.toString(), MessageDialog.WARNING, goBackAvailable ? 0 : 2, buttons) {
				@Override
				protected void createButtonsForButtonBar(Composite parent) {
					super.createButtonsForButtonBar(parent);
					if(!goBackAvailable) getButton(0).setEnabled(false);
				}
			};
		} else {
			dialog = new MessageDialog(shell, DocometreMessages.History_DeleteSnapshotTitle, null, message.toString(), MessageDialog.WARNING, 1, DocometreMessages.History_DeleteSnapshotButton, IDialogConstants.CANCEL_LABEL);
		}
		int answer = dialog.open();
		boolean goBack = isLast && answer == 0;
		boolean keepFiles = isLast ? answer == 1 : answer == 0;
		if(!goBack && !keepFiles) return;
		
		// Files will be replaced : close all editors (user may save modified ones or cancel)
		if(goBack) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if(page != null && !page.closeAllEditors(true)) return;
		}
		WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_DeleteSnapshotJob, snapshot.number(), project.getName())) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
					if(repository == null) return Status.OK_STATUS;
					if(goBack) {
						// Nothing unsaved must be lost : experiment must not have changed since the dialog
						// (resource filters are not updated here : .project would then be changed)
						project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						synchronizeProperties(project, monitor);
						int changesNumber = repository.getChanges().size();
						if(changesNumber > 0) {
							String message = NLS.bind(DocometreMessages.History_DeleteGoBackCancelled, new Object[] {snapshot.number(), project.getName(), changesNumber});
							Display.getDefault().asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(), DocometreMessages.History_DeleteSnapshotTitle, message));
							return Status.OK_STATUS;
						}
						unloadSubjects(loadedSubjects, false);
						repository.deleteSnapshot(snapshot, true);
						reloadExperiment(project, monitor);
						Activator.logInfoMessage(NLS.bind(DocometreMessages.History_SnapshotDeletedAndWentBack, new Object[] {snapshot.number(), snapshot.name(), project.getName(), previousSnapshot.number()}), HistoryOperations.class);
					} else {
						repository.deleteSnapshot(snapshot);
						Activator.logInfoMessage(NLS.bind(DocometreMessages.History_SnapshotDeleted, new Object[] {snapshot.number(), snapshot.name(), project.getName()}), HistoryOperations.class);
					}
				} catch (IncompleteRestoreException e) {
					Activator.logErrorMessageWithCause(e);
					reloadExperiment(project, monitor);
					askRetry(project, e);
				} catch (SnapshotException e) {
					if(goBack) reloadExperiment(project, monitor);
					return getErrorStatus(project, e);
				} finally {
					historyChanged(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setUser(true);
		job.schedule();
	}
	
	/*
	 * Restoration could not write or delete all files : propose to retry
	 */
	private static void askRetry(IProject project, IncompleteRestoreException exception) {
		StringBuilder files = new StringBuilder();
		List<FileChange> remainingChanges = exception.getRemainingChanges();
		for (int i = 0; i < Math.min(10, remainingChanges.size()); i++) files.append(remainingChanges.get(i).path()).append('\n');
		if(remainingChanges.size() > 10) files.append("...");
		String message = NLS.bind(DocometreMessages.History_RestoreIncompleteMessage, new Object[] {exception.getRestoredSnapshot().number(), project.getName(), remainingChanges.size(), files.toString().strip()});
		Display.getDefault().asyncExec(() -> {
			MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), DocometreMessages.History_RestoreIncompleteTitle, null, message, MessageDialog.WARNING, 0, DocometreMessages.History_RetryButton, DocometreMessages.History_LaterButton);
			if(dialog.open() != 0) return;
			WorkspaceJob job = new WorkspaceJob(NLS.bind(DocometreMessages.History_RestoreJob, exception.getRestoredSnapshot().number(), project.getName())) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					try {
						SnapshotManager.getInstance().getRepository(project).resetToLastSnapshot();
						reloadExperiment(project, monitor);
					} catch (IncompleteRestoreException e) {
						reloadExperiment(project, monitor);
						askRetry(project, e);
					} catch (SnapshotException e) {
						reloadExperiment(project, monitor);
						return getErrorStatus(project, e);
					} finally {
						historyChanged(project);
					}
					return Status.OK_STATUS;
				}
			};
			job.setRule(project);
			job.setUser(true);
			job.schedule();
		});
	}
	
	/*
	 * Before reading or modifying history : synchronize workspace with file system, make sure the
	 * .git folder is excluded from workspace resources (filters of previous versions are updated)
	 * and write experiment properties into .metadata.properties
	 */
	private static void prepareExperiment(IProject project, IProgressMonitor monitor) throws CoreException {
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		GitResources.excludeGitFolder(project, monitor);
		synchronizeProperties(project, monitor);
	}
	
	/*
	 * Write DOCoMETRe properties of experiment resources (only modified in workspace until then)
	 * into .metadata.properties, so that they are historized and compared
	 */
	private static void synchronizeProperties(IProject project, IProgressMonitor monitor) throws CoreException {
		ResourceProperties.synchronizePropertiesFile(project);
		project.refreshLocal(IResource.DEPTH_ONE, monitor);
	}
	
	/*
	 * Names of modified editors which edit resources of experiment (editors whose resource is
	 * unknown are included)
	 */
	private static List<String> getDirtyEditorsNames(IProject project) {
		ArrayList<String> names = new ArrayList<>();
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorPart editor : page.getDirtyEditors()) {
					if(isEditing(editor, project)) names.add(editor.getTitle());
				}
			}
		}
		return names;
	}
	
	private static boolean isEditing(IEditorPart editor, IProject project) {
		try {
			IEditorInput input = editor.getEditorInput();
			Object object = input instanceof ResourceEditorInput ? ((ResourceEditorInput)input).getObject() : input.getAdapter(IResource.class);
			if(object instanceof java.nio.file.Path) {
				// Files which are not workspace resources (e.g. custom functions)
				return project.getLocation() != null && ((java.nio.file.Path)object).toAbsolutePath().normalize().startsWith(project.getLocation().toFile().toPath().toAbsolutePath().normalize());
			}
			IResource resource = ObjectsController.getResourceForObject(object);
			return resource == null || project.equals(resource.getProject());
		} catch (RuntimeException e) {
			return true;
		}
	}
	
	private static boolean isDataFile(String path) {
		return path.endsWith(Activator.samplesFileExtension) || path.endsWith(Activator.adwFileExtension);
	}
	
	/*
	 * Subjects of experiment loaded in math engine
	 */
	private static List<IResource> getLoadedSubjects(IProject project) {
		ArrayList<IResource> loadedSubjects = new ArrayList<>();
		try {
			MathEngine mathEngine = MathEngineFactory.getMathEngine();
			if(mathEngine == null || !mathEngine.isStarted()) return loadedSubjects;
			for (IResource subject : ResourceProperties.getAllTypedResources(ResourceType.SUBJECT, project, null)) {
				if(mathEngine.isSubjectLoaded(subject)) loadedSubjects.add(subject);
			}
		} catch (RuntimeException e) {
			// Math engine stopped meanwhile
			Activator.logErrorMessageWithCause(e);
		}
		return loadedSubjects;
	}
	
	/*
	 * Unload subjects from math engine, modified ones are saved first if requested
	 */
	private static void unloadSubjects(List<IResource> subjects, boolean saveModifiedSubjects) {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		for (IResource subject : subjects) {
			try {
				if(saveModifiedSubjects && ResourceProperties.isSubjectModified(subject)) mathEngine.saveSubject(subject);
				mathEngine.unload(subject);
				ResourceProperties.setSubjectModified(subject, false);
				ResourceProperties.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, subject, null);
			} catch (RuntimeException e) {
				Activator.logErrorMessageWithCause(e);
			}
		}
	}
	
	/*
	 * Experiment files have been replaced : synchronize workspace, read experiment properties
	 * again, forget deserialized objects and cached lists, refresh views.
	 * All editors must have been closed. Subjects still loaded in math engine (not concerned by a
	 * file restoration) keep their state : modified flag and channels.
	 */
	private static void reloadExperiment(IProject project, IProgressMonitor monitor) throws CoreException {
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		ResourceProperties.reloadPropertiesFromFile(project);
		List<IResource> loadedSubjects = getLoadedSubjects(project);
		project.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				if(GitResources.isGitMetadata(resource)) return false;
				Object object = ResourceProperties.getObjectSessionProperty(resource);
				if(object != null) {
					ObjectsController.forgetObject(object);
					ResourceProperties.setObjectSessionProperty(resource, null);
				}
				if(loadedSubjects.contains(resource)) return true;
				ResourceProperties.setSessionProperty(ResourceProperties.DATA_FILES_LIST_QN, resource, null);
				ResourceProperties.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, resource, null);
				ResourceProperties.setSubjectModified(resource, false);
				return true;
			}
		});
		Display.getDefault().asyncExec(() -> {
			// Views refresh methods need an active workbench window
			if(PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) return;
			ExperimentsView.refresh(project, null);
			SubjectsView.refresh(project, null);
			DescriptionView.refresh();
			PlatformUI.getWorkbench().getDecoratorManager().update(ExperimentsLabelDecorator.ID);
		});
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Storage optimization
	
	/*
	 * Optimize history storage (git gc) in background when N snapshots (preference, 0 : never) of
	 * any type have been created since the last optimization
	 */
	private static void scheduleOptimization(IProject project) {
		int optimizationPeriod = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.HISTORY_OPTIMIZATION_PERIOD);
		if(optimizationPeriod <= 0) return;
		Job job = new Job(NLS.bind(DocometreMessages.History_OptimizeJob, project.getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
					if(repository != null && repository.getSnapshotsNumberSinceOptimization() >= optimizationPeriod) repository.optimize();
				} catch (SnapshotException e) {
					Activator.logErrorMessageWithCause(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
		job.schedule();
	}
	
	/*
	 * History of experiment changed : refresh snapshots view and decorations
	 */
	private static void historyChanged(IProject project) {
		SnapshotsView.refresh(project);
		HistoryStatusCache.update(project);
	}
	
	private static IStatus getErrorStatus(IProject project, Exception exception) {
		Activator.logErrorMessageWithCause(exception);
		Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
		String message = NLS.bind(DocometreMessages.History_ErrorMessage, project.getName(), cause.getLocalizedMessage());
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception);
	}

}
