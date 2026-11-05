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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.history.GitResources;
import fr.univamu.ism.docometre.history.Snapshot;
import fr.univamu.ism.docometre.history.SnapshotException;
import fr.univamu.ism.docometre.history.SnapshotManager;
import fr.univamu.ism.docometre.history.SnapshotRepository;

/**
 * History status of experiments (last snapshot, changes since it) used by label decorations.
 * Statuses are computed in a background job : when first requested, when experiment content
 * changes and after history operations. Reading a status never blocks.
 */
public final class HistoryStatusCache implements IResourceChangeListener {
	
	/* Delay before computing status after experiment content changes */
	private static final long CHANGE_DELAY = 1000;
	
	/**
	 * History status of an experiment.
	 * @param historized true if experiment has a history
	 * @param lastSnapshot last snapshot, null if none
	 * @param changesNumber number of changes since the last snapshot
	 * @param changedPaths project relative paths of changed files and folders, with all their
	 *        parent folders ("" is the experiment itself)
	 */
	public record ExperimentStatus(boolean historized, Snapshot lastSnapshot, int changesNumber, Set<String> changedPaths) {
		public boolean isChanged(String projectRelativePath) {
			return changedPaths.contains(projectRelativePath);
		}
	}
	
	private static final ExperimentStatus NOT_HISTORIZED = new ExperimentStatus(false, null, 0, Collections.emptySet());
	
	private static HistoryStatusCache instance;
	
	private final ConcurrentHashMap<String, ExperimentStatus> statuses = new ConcurrentHashMap<>();
	private final Set<IProject> projectsToUpdate = ConcurrentHashMap.newKeySet();
	private final Job job;
	
	private HistoryStatusCache() {
		job = new Job(HistoryStatusCache.class.getSimpleName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<IProject> projects = new ArrayList<>(projectsToUpdate);
				projectsToUpdate.removeAll(projects);
				for (IProject project : projects) {
					if(monitor.isCanceled()) return Status.CANCEL_STATUS;
					ExperimentStatus status = computeStatus(project);
					if(status == null) statuses.remove(project.getName());
					else statuses.put(project.getName(), status);
				}
				Display display = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : null;
				if(display != null && !display.isDisposed()) display.asyncExec(() -> {
					if(PlatformUI.isWorkbenchRunning()) PlatformUI.getWorkbench().getDecoratorManager().update(HistoryLabelDecorator.ID);
				});
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
	}
	
	public static synchronized HistoryStatusCache getInstance() {
		if(instance == null) {
			instance = new HistoryStatusCache();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(instance, IResourceChangeEvent.POST_CHANGE);
		}
		return instance;
	}
	
	public static synchronized void dispose() {
		if(instance == null) return;
		try {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(instance);
		} catch (IllegalStateException e) {
			// Workspace already closed
		}
		instance.job.cancel();
		instance = null;
	}
	
	/*
	 * Update status of experiment if cache is used (after an history operation)
	 */
	public static synchronized void update(IProject project) {
		if(instance != null && project != null) instance.schedule(project, 0);
	}
	
	/*
	 * Return status of experiment, null if it is not known yet (it is then computed and
	 * decorations are updated)
	 */
	public ExperimentStatus getStatus(IProject project) {
		ExperimentStatus status = statuses.get(project.getName());
		if(status == null) schedule(project, 0);
		return status;
	}
	
	private void schedule(IProject project, long delay) {
		projectsToUpdate.add(project);
		job.schedule(delay);
	}
	
	private static ExperimentStatus computeStatus(IProject project) {
		if(!project.isOpen()) return null;
		try {
			SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
			if(repository == null) return NOT_HISTORIZED;
			int changesNumber = repository.getChanges().size();
			Set<String> changedPaths = repository.getChangedPaths();
			return new ExperimentStatus(true, repository.getLastSnapshot(), changesNumber, Collections.unmodifiableSet(changedPaths));
		} catch (SnapshotException e) {
			Activator.logErrorMessageWithCause(e);
			return NOT_HISTORIZED;
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if(delta == null) return;
		for (IResourceDelta projectDelta : delta.getAffectedChildren()) {
			IResource resource = projectDelta.getResource();
			if(!(resource instanceof IProject)) continue;
			IProject project = (IProject) resource;
			if(projectDelta.getKind() == IResourceDelta.REMOVED || !project.isOpen()) {
				statuses.remove(project.getName());
				continue;
			}
			// Changes inside .git folder are made by history operations, which update status themselves
			if(statuses.containsKey(project.getName()) && GitResources.hasRelevantChanges(projectDelta)) schedule(project, CHANGE_DELAY);
		}
	}

}
