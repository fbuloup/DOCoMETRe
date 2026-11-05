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
package fr.univamu.ism.docometre.history;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.storage.file.WindowCacheConfig;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * Give access to experiments histories. An experiment is an Eclipse project, its
 * history is stored in a .git folder of the project folder.
 * 
 * Opened repositories are cached and closed when their experiment is closed, deleted
 * or renamed (Git files must not remain open, e.g. Windows would prevent deletion).
 */
public final class SnapshotManager implements IResourceChangeListener {
	
	private static SnapshotManager instance;
	
	// Project name -> opened repository
	private final HashMap<String, SnapshotRepository> repositories = new HashMap<>();
	// Names of experiments whose history must not be opened (e.g. while renaming them)
	private final Set<String> suspendedProjects = ConcurrentHashMap.newKeySet();
	
	private SnapshotManager() {
	}
	
	public static synchronized SnapshotManager getInstance() {
		if(instance == null) {
			instance = new SnapshotManager();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(instance, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
		}
		return instance;
	}
	
	/*
	 * Close all repositories and stop listening to workspace (on plugin stop)
	 */
	public static synchronized void dispose() {
		if(instance == null) return;
		try {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(instance);
		} catch (IllegalStateException e) {
			// Workspace already closed
		}
		instance.closeAll();
		instance = null;
	}
	
	/*
	 * Author used for snapshots created now
	 */
	public static PersonIdent getDefaultAuthor() {
		String name = "";
		String email = "";
		if(Activator.getDefault() != null) {
			name = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.HISTORY_AUTHOR_NAME).strip();
			email = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.HISTORY_AUTHOR_EMAIL).strip();
		}
		if(name.isEmpty()) name = System.getProperty("user.name", "DOCoMETRe");
		return new PersonIdent(name, email);
	}
	
	private static File getDirectory(IProject project) {
		if(project == null || project.getLocation() == null) return null;
		return project.getLocation().toFile();
	}
	
	/*
	 * Return true if experiment has a history
	 */
	public boolean isHistorized(IProject project) {
		if(project == null || !project.isOpen()) return false;
		File directory = getDirectory(project);
		// While experiment is renamed, its .git folder may be renamed for a short time (see suspend)
		if(suspendedProjects.contains(project.getName())) return directory != null && (new File(directory, GitResources.GIT_FOLDER_NAME).isDirectory() || new File(directory, SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME).isDirectory());
		return SnapshotRepository.isHistorized(directory);
	}
	
	/*
	 * Return history of experiment, null if experiment has no history
	 */
	public synchronized SnapshotRepository getRepository(IProject project) throws SnapshotException {
		if(project == null || suspendedProjects.contains(project.getName()) || !isHistorized(project)) return null;
		SnapshotRepository repository = repositories.get(project.getName());
		if(repository != null && !repository.getDirectory().equals(getDirectory(project))) {
			close(project.getName());
			repository = null;
		}
		if(repository == null) {
			repository = SnapshotRepository.open(getDirectory(project));
			repositories.put(project.getName(), repository);
		}
		return repository;
	}
	
	/*
	 * Create history of experiment and return it. No snapshot is created.
	 */
	public synchronized SnapshotRepository enableHistory(IProject project) throws SnapshotException {
		if(project == null || !project.isOpen()) throw new SnapshotException("Experiment is not open : " + (project == null ? null : project.getName()));
		if(isHistorized(project)) return getRepository(project);
		SnapshotRepository repository = SnapshotRepository.init(getDirectory(project));
		repositories.put(project.getName(), repository);
		return repository;
	}
	
	/*
	 * Close and delete history of experiment (.git folder), experiment files are not modified.
	 * Return null if history has been completely deleted, otherwise the folder which could
	 * not be completely deleted (history is disabled anyway).
	 */
	public synchronized File disableHistory(IProject project) throws SnapshotException {
		if(!isHistorized(project)) throw new SnapshotException("Experiment has no history : " + (project == null ? null : project.getName()));
		close(project.getName());
		return SnapshotRepository.deleteHistory(getDirectory(project));
	}
	
	/*
	 * Close repository of experiment if it is opened
	 */
	public synchronized void close(String projectName) {
		SnapshotRepository repository = repositories.remove(projectName);
		if(repository != null) {
			repository.close();
			// JGit keeps pack files of repositories in a global cache : release them
			releaseCachedFiles();
		}
	}
	
	private static void releaseCachedFiles() {
		new WindowCacheConfig().install();
	}
	
	/*
	 * Before moving the folder of experiment (renaming) : close its history, release files kept
	 * opened by JGit, prevent history from being opened until resume(), and check that the .git
	 * folder can be moved (no file opened by another program), retrying for 5 seconds.
	 * resume() must always be called afterwards, even if this method throws an exception.
	 */
	public synchronized void suspend(IProject project) throws SnapshotException {
		suspendedProjects.add(project.getName());
		if(!isHistorized(project)) return;
		close(project.getName());
		releaseCachedFiles();
		SnapshotRepository.checkMovable(getDirectory(project));
	}
	
	public synchronized void resume(String projectName) {
		suspendedProjects.remove(projectName);
	}
	
	public synchronized void closeAll() {
		for (String projectName : new ArrayList<>(repositories.keySet())) close(projectName);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
			if(event.getResource() instanceof IProject) close(event.getResource().getName());
			return;
		}
		// Renamed (moved) or removed experiments
		IResourceDelta delta = event.getDelta();
		if(delta == null) return;
		for (IResourceDelta projectDelta : delta.getAffectedChildren(IResourceDelta.REMOVED)) {
			IResource resource = projectDelta.getResource();
			if(resource instanceof IProject) close(resource.getName());
		}
	}

}
