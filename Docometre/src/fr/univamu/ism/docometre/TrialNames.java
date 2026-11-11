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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [10/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.history.GitResources;

/*
 * Trials folders names.
 * On disk, trials folders are named "Trial_N" whatever the application language : names do not change
 * when language changes and an experiment can be shared between users working with different languages.
 * Only labels displayed to the user are translated ("Essai n°N", "Trial n°N", "Próba nr N"...).
 * Previous versions used translated names on disk : these legacy names are renamed by migrate().
 */
public final class TrialNames {
	
	/*
	 * Prefix of trials folders names, whatever the language
	 */
	public static final String PREFIX = "Trial_"; //$NON-NLS-1$
	
	private static final Pattern NAME_PATTERN = Pattern.compile("^" + PREFIX + "([1-9]\\d*)$"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/*
	 * Trials folders names prefixes used by previous versions, depending on language (english, french and polish)
	 */
	private static final String[] LEGACY_PREFIXES = new String[] {"Trial n°", "Essai n°", "Próba nr ", "Próba nr"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	
	private static final Pattern LEGACY_NAME_PATTERN = Pattern.compile("^" + getPrefixesRegularExpression(LEGACY_PREFIXES) + "(\\d+)$"); //$NON-NLS-1$ //$NON-NLS-2$
	
	private TrialNames() {
	}
	
	private static String getPrefixesRegularExpression(String... prefixes) {
		StringBuilder regexp = new StringBuilder("(?:"); //$NON-NLS-1$
		for (int i = 0; i < prefixes.length; i++) {
			if(i > 0) regexp.append("|"); //$NON-NLS-1$
			regexp.append(Pattern.quote(prefixes[i]));
		}
		return regexp.append(")").toString(); //$NON-NLS-1$
	}
	
	/*
	 * Folder name of trial number
	 */
	public static String getName(int number) {
		return PREFIX + number;
	}
	
	/*
	 * Return true if name is of the form "Trial_N"
	 */
	public static boolean isName(String name) {
		return name != null && NAME_PATTERN.matcher(name).matches();
	}
	
	/*
	 * Return true if name is a trial folder name of previous versions (e.g. "Essai n°N")
	 */
	public static boolean isLegacyName(String name) {
		return name != null && LEGACY_NAME_PATTERN.matcher(name).matches();
	}
	
	/*
	 * Trial number from a trial folder name ("Trial_N" or legacy name), -1 if name is not a trial name
	 */
	public static int getNumber(String name) {
		if(name == null) return -1;
		Matcher matcher = NAME_PATTERN.matcher(name);
		if(!matcher.matches()) matcher = LEGACY_NAME_PATTERN.matcher(name);
		if(!matcher.matches()) return -1;
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	/*
	 * Trial number of a trial folder, -1 if its name is not a trial name
	 */
	public static int getNumber(IResource trial) {
		if(trial == null) return -1;
		return getNumber(trial.getName());
	}
	
	/*
	 * Translated label of trial number, e.g. "Essai n°N" in french
	 */
	public static String getLabel(int number) {
		return DocometreMessages.Trial + number;
	}
	
	/*
	 * Translated label of a trial folder, e.g. "Essai n°N" in french
	 */
	public static String getLabel(IResource trial) {
		int number = getNumber(trial);
		if(number < 0) return trial.getName();
		return getLabel(number);
	}
	
	/*
	 * Trial folder of session with this number (it may not exist)
	 */
	public static IFolder getTrial(IContainer session, int number) {
		return session.getFolder(new Path(getName(number)));
	}
	
	/*
	 * Regular expression of trials typed by user : "Trial_N" or "Trial_N:M", or with the translated
	 * label, of current or previous languages : "Essai n°N" or "Essai n°N:M"...
	 */
	private static Pattern getInputPattern(boolean allowRange) {
		String prefixes = getPrefixesRegularExpression(PREFIX, DocometreMessages.Trial, LEGACY_PREFIXES[0], LEGACY_PREFIXES[1], LEGACY_PREFIXES[2], LEGACY_PREFIXES[3]);
		String range = allowRange ? "(?::([1-9]\\d*))?" : "()"; //$NON-NLS-1$ //$NON-NLS-2$
		return Pattern.compile("^" + prefixes + "([1-9]\\d*)" + range + "$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/*
	 * Regular expression of trials typed by user (see parseInput())
	 */
	public static String getInputRegularExpression(boolean allowRange) {
		return getInputPattern(allowRange).pattern();
	}
	
	/*
	 * Trials numbers range typed by user ("Trial_N", "Trial_N:M", "Essai n°N", "Essai n°N:M"...) :
	 * return {N, M} ({N, N} if no range), or null if text is not valid.
	 */
	public static int[] parseInput(String text, boolean allowRange) {
		if(text == null) return null;
		Matcher matcher = getInputPattern(allowRange).matcher(text.trim());
		if(!matcher.matches()) return null;
		try {
			int first = Integer.parseInt(matcher.group(1));
			int last = first;
			if(matcher.group(2) != null && !"".equals(matcher.group(2))) last = Integer.parseInt(matcher.group(2)); //$NON-NLS-1$
			if(last < first) return null;
			return new int[] {first, last};
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/*
	 * Rename trials folders of previous versions (e.g. "Essai n°N") to "Trial_N" in all open experiments.
	 */
	public static void migrateWorkspace(IProgressMonitor monitor) {
		migrate(ResourcesPlugin.getWorkspace().getRoot(), monitor);
	}
	
	/*
	 * Return true if open experiments contain trials folders of previous versions (e.g. "Essai n°N")
	 */
	public static boolean hasLegacyTrialsInWorkspace() {
		return !getLegacyTrials(ResourcesPlugin.getWorkspace().getRoot()).isEmpty();
	}
	
	/*
	 * Trials folders of previous versions (e.g. "Essai n°N") in container and its descendants
	 */
	private static ArrayList<IFolder> getLegacyTrials(IContainer container) {
		ArrayList<IFolder> legacyTrials = new ArrayList<>();
		if(container == null || !container.isAccessible()) return legacyTrials;
		try {
			container.accept(resource -> {
				if(GitResources.isGitMetadata(resource)) return false;
				// As in ExperimentsContentProvider, a folder named as a trial is a trial (it may not be typed yet,
				// e.g. a session copied from file system)
				if(resource instanceof IFolder && isLegacyName(resource.getName())) {
					legacyTrials.add((IFolder) resource);
					return false;
				}
				// Trials are only in sessions : no need to visit files
				return resource instanceof IContainer;
			});
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return legacyTrials;
	}
	
	/*
	 * Rename trials folders of previous versions (e.g. "Essai n°N") to "Trial_N" in container and its descendants.
	 * Folders are moved : DOCoMETRe persistent properties (trial type, associated process, done state...)
	 * are kept, and .metadata.properties experiment file is updated.
	 * Return true if at least one folder has been renamed.
	 */
	public static boolean migrate(IContainer container, IProgressMonitor monitor) {
		ArrayList<IFolder> legacyTrials = getLegacyTrials(container);
		if(legacyTrials.isEmpty()) return false;
		// Message displayed by progress dialog or job : "Migration to the new names of trials folders"
		SubMonitor subMonitor = SubMonitor.convert(monitor, legacyTrials.size());
		subMonitor.setTaskName(DocometreMessages.TrialNames_MigrationTaskTitle);
		Set<IProject> modifiedExperiments = new HashSet<>();
		try {
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) localMonitor -> {
				SubMonitor renameMonitor = SubMonitor.convert(localMonitor, legacyTrials.size());
				for (IFolder legacyTrial : legacyTrials) {
					renameMonitor.subTask(legacyTrial.getFullPath().toString());
					if(rename(legacyTrial, renameMonitor.split(1))) modifiedExperiments.add(legacyTrial.getProject());
				}
			}, container.getProject() == null ? ResourcesPlugin.getWorkspace().getRoot() : container.getProject(), 0, subMonitor.split(legacyTrials.size()));
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} finally {
			// Also when cancelled : folders already renamed keep their properties
			for (IProject experiment : modifiedExperiments) ResourceProperties.savePropertiesToFile(experiment);
		}
		return !modifiedExperiments.isEmpty();
	}
	
	private static boolean rename(IFolder legacyTrial, IProgressMonitor monitor) throws CoreException {
		int number = getNumber(legacyTrial);
		if(number < 1) return false;
		IFolder trial = getTrial(legacyTrial.getParent(), number);
		IPath oldFullPath = legacyTrial.getFullPath();
		if(trial.exists()) {
			// "Trial_N" already exists (e.g. a file of an old snapshot has been restored) : do not overwrite it
			Activator.logErrorMessage(NLS.bind(DocometreMessages.TrialNames_MigrationConflict, oldFullPath.toString(), trial.getFullPath().toString()));
			return false;
		}
		// DOCoMETRe properties may only be in .metadata.properties experiment file (e.g. imported experiment) :
		// their keys contain resources paths and must follow renaming
		Properties properties = ResourceProperties.loadPropertiesFromFile(legacyTrial);
		String oldPrefix = oldFullPath.toPortableString();
		String newPrefix = trial.getFullPath().toPortableString();
		for (String key : properties.stringPropertyNames()) {
			if(key.startsWith(oldPrefix + ":") || key.startsWith(oldPrefix + "/")) { //$NON-NLS-1$ //$NON-NLS-2$
				String newKey = newPrefix + key.substring(oldPrefix.length());
				properties.put(newKey, properties.getProperty(key));
				properties.remove(key);
			}
		}
		// Persistent properties are moved with the folder
		legacyTrial.move(trial.getFullPath(), true, monitor);
		Activator.logInfoMessage(NLS.bind(DocometreMessages.TrialNames_MigrationDone, oldFullPath.toString(), trial.getFullPath().toString()), TrialNames.class);
		return true;
	}

}
