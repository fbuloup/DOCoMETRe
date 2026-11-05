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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import fr.univamu.ism.docometre.Activator;

/**
 * Helpers about the Git metadata folder (.git) that the experiments history
 * stores inside each experiment. This folder is technical : it must not be
 * shown, traversed or trigger views refreshes.
 */
public final class GitResources {
	
	public static final String GIT_FOLDER_NAME = ".git";
	
	/* Resources filter matcher provided by org.eclipse.core.resources */
	private static final String REGEX_FILTER_MATCHER_ID = "org.eclipse.core.resources.regexFilterMatcher";
	private static final String GIT_FOLDER_REGEX = "\\.git(\\.deleted|\\.renaming)?";
	/* Filters created by previous versions, replaced by the current one */
	private static final List<String> PREVIOUS_GIT_FOLDER_REGEXES = List.of("\\.git(\\.deleted)?");
	
	private GitResources() {
	}
	
	/*
	 * Return true if path is the .git folder (or .git.deleted folder) or is inside it
	 */
	public static boolean isGitMetadata(IPath path) {
		if(path == null) return false;
		for (String segment : path.segments()) {
			if(GIT_FOLDER_NAME.equals(segment) || SnapshotRepository.DELETED_HISTORY_FOLDER_NAME.equals(segment) || SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME.equals(segment)) return true;
		}
		return false;
	}
	
	/*
	 * Return true if resource is the .git folder or is inside it
	 */
	public static boolean isGitMetadata(IResource resource) {
		return resource != null && isGitMetadata(resource.getFullPath());
	}
	
	/*
	 * Exclude .git folder (and temporary .git.deleted and .git.renaming folders) of experiment from
	 * workspace resources (resource filter stored in .project file) : Eclipse then neither indexes
	 * nor refreshes Git internal files.
	 * Nothing is done if filter already exists, filters of previous versions are replaced.
	 */
	public static void excludeGitFolder(IProject project, IProgressMonitor monitor) throws CoreException {
		boolean exists = false;
		ArrayList<IResourceFilterDescription> previousFilters = new ArrayList<>();
		for (IResourceFilterDescription filter : project.getFilters()) {
			String regex = getGitFolderRegex(filter);
			if(GIT_FOLDER_REGEX.equals(regex)) exists = true;
			else if(PREVIOUS_GIT_FOLDER_REGEXES.contains(regex)) previousFilters.add(filter);
		}
		if(!exists) {
			FileInfoMatcherDescription matcher = new FileInfoMatcherDescription(REGEX_FILTER_MATCHER_ID, GIT_FOLDER_REGEX);
			project.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcher, IResource.BACKGROUND_REFRESH, monitor);
		}
		for (IResourceFilterDescription filter : previousFilters) filter.delete(IResource.BACKGROUND_REFRESH, monitor);
	}
	
	/*
	 * Remove resource filters created by excludeGitFolder (current and previous versions), if any
	 */
	public static void includeGitFolder(IProject project, IProgressMonitor monitor) throws CoreException {
		for (IResourceFilterDescription filter : project.getFilters()) {
			String regex = getGitFolderRegex(filter);
			if(GIT_FOLDER_REGEX.equals(regex) || PREVIOUS_GIT_FOLDER_REGEXES.contains(regex)) filter.delete(IResource.BACKGROUND_REFRESH, monitor);
		}
	}
	
	/*
	 * Regular expression of filter if it is a regex filter, null otherwise
	 */
	private static String getGitFolderRegex(IResourceFilterDescription filter) {
		FileInfoMatcherDescription matcher = filter.getFileInfoMatcherDescription();
		if(matcher == null || !REGEX_FILTER_MATCHER_ID.equals(matcher.getId()) || !(matcher.getArguments() instanceof String)) return null;
		return (String) matcher.getArguments();
	}
	
	/*
	 * Return true if delta contains at least one change outside .git folders.
	 * A container delta of kind CHANGED without any flag only means that some of its
	 * descendants changed : it is not a relevant change by itself.
	 */
	public static boolean hasRelevantChanges(IResourceDelta delta) {
		if(delta == null) return false;
		boolean[] relevant = new boolean[] {false};
		try {
			delta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta resourceDelta) throws CoreException {
					if(relevant[0]) return false;
					if(isGitMetadata(resourceDelta.getFullPath())) return false;
					if(resourceDelta.getKind() != IResourceDelta.CHANGED || resourceDelta.getFlags() != 0) {
						relevant[0] = true;
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			return true;
		}
		return relevant[0];
	}

}
