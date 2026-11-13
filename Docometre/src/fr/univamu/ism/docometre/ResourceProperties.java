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
package fr.univamu.ism.docometre;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.history.GitResources;

public final class ResourceProperties {
	
	private static boolean usePropertyFile = true;
	private static HashMap<String, Properties> experimentsProperties = new HashMap<String, Properties>();
	public static String metadataFileName = "/.metadata.properties";
	
	/*
	 * Qualified names for persistent resource properties 
	 */
	// Separator used in properties file
	public static String SEPARATOR = ">>>";
	// Default DACQ file for project
	public static QualifiedName DEFAULT_DACQ_QN = new QualifiedName(Activator.PLUGIN_ID, "default.daq");
	// Resource type (Experiment, Subject etc.) see ResourceType class for details 
	public static QualifiedName TYPE_QN = new QualifiedName(Activator.PLUGIN_ID, "type");
	// Description
	public static QualifiedName DESCRIPTION_QN = new QualifiedName(Activator.PLUGIN_ID, "description");
	// System type (ADWin, NI etc.)
	public static QualifiedName SYSTEM_QN = new QualifiedName(Activator.PLUGIN_ID, "system");
	// Associated DACQ file
	public static QualifiedName ASSOCIATED_DACQ_CONFIGURATION_FILE_QN = new QualifiedName(Activator.PLUGIN_ID, "associated.dacq.configuration.file");
	// Associated process file
	public static QualifiedName ASSOCIATED_PROCESS_FILE_QN = new QualifiedName(Activator.PLUGIN_ID, "associated.process.file");
	// Whether a process may run in main thread 
	public static QualifiedName RUN_IN_MAIN_THREAD_QN =  new QualifiedName(Activator.PLUGIN_ID, "run.in.main.thread");
	// Signature of the source code of a process when it has been compiled for the last time, with the
	// shift between lines of the compiled file and lines of the source code : "shift:signature"
	public static QualifiedName COMPILED_CODE_SIGNATURE_QN =  new QualifiedName(Activator.PLUGIN_ID, "compiled.code.signature");
	// Trial state : 
	//    - if trial.state property doesn't exist or is not equal to "done", trial is undone
	//    - if trial.state property exists and is equal to "done" trial is done !
	public static QualifiedName TRIAL_STATE_QN = new QualifiedName(Activator.PLUGIN_ID, "trial.state");
	// Trial start : AUTO or MANUAL
	//    - if trial.start.mode property doesn't exist or is not equal to "AUTO", trial start mode is MANUAL
	//    - if trial.start.mode property exists and is equal to "AUTO" trial is started automatically !
	public static QualifiedName TRIAL_START_MODE_QN = new QualifiedName(Activator.PLUGIN_ID, "trial.start.mode");
	// Trial validate : AUTO or MANUAL
	//    - if trial.validate.mode property doesn't exist or is not equal to "AUTO", trial is validated manually
	//    - if trial.validate.mode property exists and is equal to "AUTO" trial is validated automatically !
	public static QualifiedName TRIAL_VALIDATE_MODE_QN = new QualifiedName(Activator.PLUGIN_ID, "trial.validate.mode");
	// Prefix for data files names, if null, means prefix is not used
	public static QualifiedName DATA_FILES_NAMES_PREFIX_QN = new QualifiedName(Activator.PLUGIN_ID, "data.files.name.prefix");
	// First suffix, if null, means first suffix is not used : true or false
	public static QualifiedName USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN = new QualifiedName(Activator.PLUGIN_ID, "use.session.name.as.first.suffix.in.data.files.name");
	// Second suffix, if null, means Second suffix is not used : true or false
	public static QualifiedName USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN = new QualifiedName(Activator.PLUGIN_ID, "use.trial.number.as.second.suffix.in.data.files.name");
	// Grid state in editor
	public static QualifiedName GRID_STATE_EDITOR = new QualifiedName(AbstractScriptSegmentEditor.class.getCanonicalName(), "gridState");
	// Snap state in editor
	public static QualifiedName SNAP_STATE_EDITOR = new QualifiedName(AbstractScriptSegmentEditor.class.getCanonicalName(), "snapState");
	// Zoom state in editor
	public static QualifiedName ZOOM_STATE_EDITOR_INIT = new QualifiedName(AbstractScriptSegmentEditor.class.getCanonicalName(), "zoomStateInit");
	public static QualifiedName ZOOM_STATE_EDITOR_EVENT = new QualifiedName(AbstractScriptSegmentEditor.class.getCanonicalName(), "zoomStateEvent");
	public static QualifiedName ZOOM_STATE_EDITOR_FINISH = new QualifiedName(AbstractScriptSegmentEditor.class.getCanonicalName(), "zoomStateFinish");
	
	/*
	 * Qualified names for session resource properties 
	 */
	// Deserialized object associated to resource file 
	public static QualifiedName OBJECT_QN = new QualifiedName(Activator.PLUGIN_ID, "object");
	// Data files list
	public static QualifiedName DATA_FILES_LIST_QN = new QualifiedName(Activator.PLUGIN_ID, "data.files.list");
	// Channels list
	public static QualifiedName CHANNELS_LIST_QN = new QualifiedName(Activator.PLUGIN_ID, "channels.list");
	// Subject modified
	public static QualifiedName SUBJECT_MODIFIED_QN = new QualifiedName(Activator.PLUGIN_ID, "subject.modified");
	
	/////////////////////////////////////////////////////////////////////////////
	/*
	* Methods for persistent resource properties 
	*/
	
	public static Properties loadPropertiesFromFile(IResource resource) {
		Properties properties = experimentsProperties.get(resource.getProject().getName());
		if(properties != null) return properties;
		String fullPath = resource.getProject().getLocation().toPortableString();
		properties = new Properties();
		if(Files.exists(Path.of(fullPath + metadataFileName))) {
			try(FileInputStream fis = new FileInputStream(fullPath + metadataFileName)) {
				properties.load(fis);
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			} 
		}
		experimentsProperties.put(resource.getProject().getName(), properties);
		return properties;
	}
	
	/*
	 * Save experiment properties to its .metadata.properties file.
	 * The content is written so that it only depends on the properties themselves
	 * (sorted entries, no date comment, '\n' line separator) : this file can then be
	 * historized and compared. The file is not rewritten if its content is unchanged.
	 */
	public static void savePropertiesToFile(IResource resource) {
		Properties properties = experimentsProperties.get(resource.getProject().getName());
		if(properties == null) return;
		String fullPath = resource.getProject().getLocation().toPortableString();
		Path filePath = Path.of(fullPath + metadataFileName);
		try {
			byte[] content = getPropertiesFileContent(properties);
			if(Files.exists(filePath) && Arrays.equals(Files.readAllBytes(filePath), content)) return;
			Files.write(filePath, content);
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	/*
	 * Return properties file content : Properties.store format (ISO 8859-1 with unicode escapes),
	 * one entry per line, lines sorted, without the date comment written by Properties.store.
	 */
	private static byte[] getPropertiesFileContent(Properties properties) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		properties.store(byteArrayOutputStream, null);
		String[] lines = byteArrayOutputStream.toString(StandardCharsets.ISO_8859_1).split("\\R");
		ArrayList<String> entries = new ArrayList<>(lines.length);
		for (String line : lines) {
			// Properties.store escapes leading '#' and '!' in keys : lines starting with them are comments
			if(line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue;
			entries.add(line);
		}
		Collections.sort(entries);
		StringBuilder content = new StringBuilder();
		for (String entry : entries) content.append(entry).append('\n');
		return content.toString().getBytes(StandardCharsets.ISO_8859_1);
	}
	
	/*
	 * Return true if qualified name is one of DOCoMETRe persistent properties mirrored in .metadata.properties
	 */
	private static boolean isDocometrePersistentProperty(QualifiedName qn) {
		String qualifier = qn.getQualifier();
		return Activator.PLUGIN_ID.equals(qualifier) || AbstractScriptSegmentEditor.class.getCanonicalName().equals(qualifier);
	}
	
	/*
	 * Key format in .metadata.properties : <resource full path>:<qualifier>:<local name>
	 * Return {resource full path, qualifier, local name} or null if key can't be parsed.
	 */
	private static String[] parsePropertyKey(String key) {
		int localNameIndex = key.lastIndexOf(':');
		if(localNameIndex <= 0) return null;
		int qualifierIndex = key.lastIndexOf(':', localNameIndex - 1);
		if(qualifierIndex <= 0) return null;
		String fullPath = key.substring(0, qualifierIndex);
		if(!fullPath.startsWith("/")) return null;
		return new String[] {fullPath, key.substring(qualifierIndex + 1, localNameIndex), key.substring(localNameIndex + 1)};
	}
	
	/*
	 * Return true if resource referenced by full path (/project/...) still exists, in the workspace or on disk
	 */
	private static boolean resourceExists(String fullPath) {
		IPath path = IPath.fromPortableString(fullPath);
		if(path.segmentCount() == 0) return false;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		if(root.findMember(path) != null) return true;
		IProject project = root.getProject(path.segment(0));
		if(!project.exists() || project.getLocation() == null) return false;
		return project.getLocation().append(path.removeFirstSegments(1)).toFile().exists();
	}
	
	/*
	 * Make .metadata.properties file of project complete and up to date before it is historized :
	 *  - all DOCoMETRe persistent properties of project resources are copied into the file
	 *    (some may only exist in the workspace, e.g. legacy or imported ones),
	 *  - entries of resources which no longer exist are removed,
	 *  - the file is saved.
	 * Resources must be synchronized with file system (refreshLocal) before calling this method.
	 */
	public static void synchronizePropertiesFile(IProject project) {
		if(!usePropertyFile || project == null || !project.isAccessible()) return;
		Properties properties = loadPropertiesFromFile(project);
		for (String key : properties.stringPropertyNames()) {
			String[] keyParts = parsePropertyKey(key);
			if(keyParts != null && !resourceExists(keyParts[0])) properties.remove(key);
		}
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if(GitResources.isGitMetadata(resource)) return false;
					for (Map.Entry<QualifiedName, String> entry : resource.getPersistentProperties().entrySet()) {
						if(!isDocometrePersistentProperty(entry.getKey())) continue;
						if(ResourceType.CHANNEL.toString().equals(entry.getValue())) continue;
						properties.put(resource.getFullPath().toPortableString() + ":" + entry.getKey().toString(), entry.getValue());
					}
					return true;
				}
			});
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		savePropertiesToFile(project);
	}
	
	/*
	 * Reload DOCoMETRe persistent properties of project resources from its .metadata.properties file.
	 * The file becomes the reference : workspace values absent from the file are removed.
	 * To be called when .metadata.properties has been replaced on disk (e.g. a snapshot has been restored)
	 * and after resources have been synchronized with file system (refreshLocal).
	 * Keys are matched with project relative paths, so the file remains valid if project has been renamed.
	 */
	public static void reloadPropertiesFromFile(IProject project) {
		if(!usePropertyFile || project == null || !project.isAccessible()) return;
		experimentsProperties.remove(project.getName());
		Properties properties = loadPropertiesFromFile(project);
		// Project relative path -> (qualified name -> value)
		HashMap<String, HashMap<QualifiedName, String>> fileProperties = new HashMap<>();
		for (String key : properties.stringPropertyNames()) {
			String[] keyParts = parsePropertyKey(key);
			if(keyParts == null) continue;
			IPath fullPath = IPath.fromPortableString(keyParts[0]);
			if(fullPath.segmentCount() == 0) continue;
			String relativePath = fullPath.removeFirstSegments(1).makeRelative().toPortableString();
			QualifiedName qn = new QualifiedName(keyParts[1], keyParts[2]);
			HashMap<QualifiedName, String> resourceProperties = fileProperties.computeIfAbsent(relativePath, k -> new HashMap<>());
			// Keys using current project name take precedence over keys using an old project name
			if(project.getName().equals(fullPath.segment(0))) resourceProperties.put(qn, properties.getProperty(key));
			else resourceProperties.putIfAbsent(qn, properties.getProperty(key));
		}
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if(GitResources.isGitMetadata(resource)) return false;
					HashMap<QualifiedName, String> resourceFileProperties = fileProperties.get(resource.getProjectRelativePath().toPortableString());
					for (Map.Entry<QualifiedName, String> entry : resource.getPersistentProperties().entrySet()) {
						if(!isDocometrePersistentProperty(entry.getKey())) continue;
						if(ResourceType.CHANNEL.toString().equals(entry.getValue())) continue;
						if(resourceFileProperties == null || !resourceFileProperties.containsKey(entry.getKey())) resource.setPersistentProperty(entry.getKey(), null);
					}
					if(resourceFileProperties != null) {
						for (Map.Entry<QualifiedName, String> entry : resourceFileProperties.entrySet()) {
							resource.setPersistentProperty(entry.getKey(), entry.getValue());
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	/*
	 * Return true if values of this property are resources full paths (e.g. \Experiment\process.process)
	 */
	private static boolean isPathProperty(String qualifier, String localName) {
		if(!Activator.PLUGIN_ID.equals(qualifier)) return false;
		return DEFAULT_DACQ_QN.getLocalName().equals(localName) || ASSOCIATED_DACQ_CONFIGURATION_FILE_QN.getLocalName().equals(localName)
				|| ASSOCIATED_PROCESS_FILE_QN.getLocalName().equals(localName);
	}

	/*
	 * Return content of experiment .metadata.properties file as it must be written in an export archive :
	 * experiment name is neither written in keys nor in paths values. Paths are relative to experiment
	 * root "/" : "/:Docometre:type", "/Subject/Session:Docometre:type", "/process.process" etc.
	 * So the archive can be imported with any experiment name (see importPropertiesFile).
	 * Properties of data files are not exported if includeData is false. Resources must be
	 * synchronized with file system (refreshLocal) before calling this method.
	 */
	public static byte[] getExportedPropertiesFileContent(IProject experiment, boolean includeData) throws IOException {
		synchronizePropertiesFile(experiment);
		Properties properties = loadPropertiesFromFile(experiment);
		Properties exportedProperties = new Properties();
		for (String key : properties.stringPropertyNames()) {
			String[] keyParts = parsePropertyKey(key);
			if(keyParts == null) continue;
			IPath fullPath = IPath.fromPortableString(keyParts[0]);
			// Keys using an old experiment name are not exported
			if(fullPath.segmentCount() == 0 || !experiment.getName().equals(fullPath.segment(0))) continue;
			IPath relativePath = fullPath.removeFirstSegments(1).makeRelative();
			if(!includeData && relativePath.segmentCount() > 0) {
				IResource resource = experiment.findMember(relativePath);
				if(resource != null && ResourceType.isDataFile(resource)) continue;
			}
			String value = properties.getProperty(key);
			if(isPathProperty(keyParts[1], keyParts[2])) {
				IPath valuePath = IPath.fromPortableString(value.replace('\\', '/'));
				// Path to a resource out of this experiment can't be exported
				if(valuePath.segmentCount() == 0 || !experiment.getName().equals(valuePath.segment(0))) continue;
				value = valuePath.removeFirstSegments(1).makeAbsolute().toPortableString();
			}
			String exportedKey = relativePath.makeAbsolute().toPortableString() + ":" + keyParts[1] + ":" + keyParts[2];
			exportedProperties.put(exportedKey, value);
		}
		return getPropertiesFileContent(exportedProperties);
	}

	/*
	 * Return the experiment name used in keys of a .metadata.properties file : the most used first segment of keys
	 */
	private static String getExperimentNameInProperties(Properties properties) {
		HashMap<String, Integer> occurrences = new HashMap<>();
		for (String key : properties.stringPropertyNames()) {
			String[] keyParts = parsePropertyKey(key);
			if(keyParts == null) continue;
			IPath fullPath = IPath.fromPortableString(keyParts[0]);
			if(fullPath.segmentCount() > 0) occurrences.merge(fullPath.segment(0), 1, Integer::sum);
		}
		String experimentName = null;
		int maxOccurrences = 0;
		for (Map.Entry<String, Integer> entry : occurrences.entrySet()) {
			if(entry.getValue() > maxOccurrences) {
				experimentName = entry.getKey();
				maxOccurrences = entry.getValue();
			}
		}
		return experimentName;
	}

	/*
	 * Adapt .metadata.properties file of an experiment just extracted from an export archive to the experiment name,
	 * then apply its properties to experiment resources (see reloadPropertiesFromFile).
	 * relativePaths is true when keys and paths values of the file are relative to experiment (see getExportedPropertiesFileContent),
	 * false when they contain the name of the experiment when it was exported (archives exported by previous versions).
	 * Resources must be synchronized with file system (refreshLocal) before calling this method.
	 */
	public static void importPropertiesFile(IProject experiment, boolean relativePaths) {
		if(!usePropertyFile || experiment == null || !experiment.isAccessible()) return;
		Path filePath = Path.of(experiment.getLocation().toPortableString() + metadataFileName);
		experimentsProperties.remove(experiment.getName());
		if(Files.exists(filePath)) {
			Properties properties = new Properties();
			try(FileInputStream fis = new FileInputStream(filePath.toFile())) {
				properties.load(fis);
			} catch (IOException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			String originalExperimentName = relativePaths ? null : getExperimentNameInProperties(properties);
			Properties importedProperties = new Properties();
			for (String key : properties.stringPropertyNames()) {
				String[] keyParts = parsePropertyKey(key);
				if(keyParts == null) continue;
				IPath fullPath = IPath.fromPortableString(keyParts[0]);
				IPath relativePath;
				if(relativePaths) relativePath = fullPath.makeRelative();
				else {
					// Keys using an old experiment name are ignored
					if(fullPath.segmentCount() == 0 || !fullPath.segment(0).equals(originalExperimentName)) continue;
					relativePath = fullPath.removeFirstSegments(1).makeRelative();
				}
				String value = properties.getProperty(key);
				if(isPathProperty(keyParts[1], keyParts[2])) {
					IPath valuePath = IPath.fromPortableString(value.replace('\\', '/'));
					if(relativePaths) value = experiment.getFullPath().append(valuePath.makeRelative()).toOSString();
					else if(valuePath.segmentCount() > 0 && valuePath.segment(0).equals(originalExperimentName))
						value = experiment.getFullPath().append(valuePath.removeFirstSegments(1)).toOSString();
				}
				String importedKey = experiment.getFullPath().append(relativePath).toPortableString() + ":" + keyParts[1] + ":" + keyParts[2];
				importedProperties.put(importedKey, value);
			}
			try {
				Files.write(filePath, getPropertiesFileContent(importedProperties));
			} catch (IOException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		reloadPropertiesFromFile(experiment);
	}

	private static String getPersistentPropertyFromFile(QualifiedName qn, IResource resource) {
		Properties properties = loadPropertiesFromFile(resource);
		return (String) properties.get(resource.getFullPath().toPortableString() + ":" + qn.toString());
	}
	
	public static void setPersistentProperty(QualifiedName qn, IResource resource, String value) {
		try {
			if(resource == null) return;
			resource.setPersistentProperty(qn, value);
			if(usePropertyFile) {
				Properties properties = loadPropertiesFromFile(resource);
				if(value == null) properties.remove(resource.getFullPath().toPortableString() + ":" + qn.toString());
				else properties.put(resource.getFullPath().toPortableString() + ":" + qn.toString(), value);
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static String getPersistentProperty(QualifiedName qn, IResource resource) {
		if(ResourcesPlugin.getWorkspace().getRoot() == resource) return null;
		String value = "";
		try {
			value = resource.getPersistentProperty(qn);
			if(value == null && usePropertyFile) {
				value = getPersistentPropertyFromFile(qn, resource);
				if(value != null) resource.setPersistentProperty(qn, value);
			} else if(usePropertyFile && !ResourceType.CHANNEL.toString().equals(value)) {
				Properties properties = loadPropertiesFromFile(resource);
				properties.put(resource.getFullPath().toPortableString() + ":" + qn.toString(), value);
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return value;
	}
	
	public static Map<QualifiedName, String> getPersistentProperties(IResource resource) throws CoreException {
		return resource.getPersistentProperties();
	}
	
	/*
	 * Return default dacq configuration file path 
	 */
	public static String getDefaultDACQPersistentProperty(IResource resource) {
		return getPersistentProperty(DEFAULT_DACQ_QN, resource.getProject());
	}
	
	/*
	 * Set default dacq configuration file path
	 */
	public static void setDefaultDACQPersistentProperty(IResource resource, String value) {
		setPersistentProperty(DEFAULT_DACQ_QN, resource.getProject(), value);
	}
	
	/*
	 * Return the type of the resource : Experiment, subject etc. See ResourceType class for details
	 */
	@SuppressWarnings("deprecation")
	public static String getTypePersistentProperty(IResource resource) {
		if(resource == null) return "";
		boolean resourceNonLocale = !resource.isLocal(IResource.DEPTH_ZERO);
		if(resourceNonLocale) Activator.logWarningMessage(NLS.bind(DocometreMessages.seemsNotLocale, resource.getFullPath().toPortableString()));
		String type = ResourceType.ANY.toString();
		if(resource.exists()) type = getPersistentProperty(TYPE_QN, resource);
		if(resourceNonLocale) Activator.logWarningMessage(DocometreMessages.becameLocale);
		return type;
	}
	
	/*
	 * Set the type of the resource : Experiment, subject etc. See ResourceType class for details
	 */
	public static void setTypePersistentProperty(IResource resource, String value) {
		setPersistentProperty(TYPE_QN, resource, value);
	}
	
	/*
	 * Get the resource description
	 */
	public static String getDescriptionPersistentProperty(IResource resource) {
		return getPersistentProperty(DESCRIPTION_QN, resource);
	}
	
	/*
	 * Set the resource description
	 */
	public static void setDescriptionPersistentProperty(IResource resource, String value) {
		setPersistentProperty(DESCRIPTION_QN, resource, value);
	}
	
	/*
	 * Get the resource type (ADwin, NI etc.). See Activator for details.
	 */
	public static String getSystemPersistentProperty(IResource resource) {
		return getPersistentProperty(SYSTEM_QN, resource);
	}
	
	/*
	 * Set the resource type (ADwin, NI etc.). See Activator for details.
	 */
	public static void setSystemPersistentProperty(IResource resource, String value) {
		setPersistentProperty(SYSTEM_QN, resource, value);
	}
	
	/*
	 * Set process file associated DACQ configuration file path
	 */
	public static void setAssociatedDACQConfigurationProperty(IResource processFile, String value) {
		setPersistentProperty(ASSOCIATED_DACQ_CONFIGURATION_FILE_QN, processFile, value);
	}
	
	/*
	 * Get process file associated trial file path
	 */
	public static IResource getAssociatedDACQConfiguration(IResource processFile) {
		IResource dacqFile = null;
		String associatedDacqFullPath = getAssociatedDACQConfigurationProperty(processFile);
		if(associatedDacqFullPath != null) dacqFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedDacqFullPath);
		return dacqFile;
	}
	
	/*
	 * Get process file associated DACQ configuration file path
	 */
	public static String getAssociatedDACQConfigurationProperty(IResource processFile) {
		if(!processFile.exists()) return null;
		if(!ResourceType.isProcess(processFile)) return null;
		return getPersistentProperty(ASSOCIATED_DACQ_CONFIGURATION_FILE_QN, processFile);
	}
	
	/*
	 * Set process file associated trial file path
	 */
	public static void setAssociatedProcessProperty(IResource folder, String value) {
		setPersistentProperty(ASSOCIATED_PROCESS_FILE_QN, folder, value);
	}
	
	/*
	 * Get process file associated trial file path
	 */
	public static IResource getAssociatedProcess(IResource trialFolder) {
		IResource processFile = null;
		String associatedProcessFullPath = getAssociatedProcessProperty(trialFolder);
		if(associatedProcessFullPath != null) processFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedProcessFullPath);
		return processFile;
	}
	
	/*
	 * Return process file path associated with trial file 
	 */
	public static String getAssociatedProcessProperty(IResource folder) {
		if(!folder.exists()) return null;
		if(!(ResourceType.isTrial(folder) || ResourceType.isProcessTest(folder))) return null;
		return getPersistentProperty(ASSOCIATED_PROCESS_FILE_QN, folder);
	}
	
	public static void setRunInMainThread(IResource process, boolean value) {
		setPersistentProperty(RUN_IN_MAIN_THREAD_QN, process, Boolean.toString(value));
	}
	
	public static boolean isRunInMainThread(IResource process) {
		String value = getPersistentProperty(RUN_IN_MAIN_THREAD_QN, process);
		return Boolean.parseBoolean(value) && MathEngineFactory.isPython();
	}
	
	/*
	 * Return trial execution state
	 */
	public static boolean isTrialDone(IResource trialFolder) {
		String state = getPersistentProperty(TRIAL_STATE_QN, trialFolder);
		if("done".equals(state) || "true".equals(state)) return true;
		return false;
	}
	
	/*
	 * Set trial execution state
	 */
	public static void setTrialState(IResource trialFolder, boolean done) {
		if(done == true) setPersistentProperty(TRIAL_STATE_QN, trialFolder, "done");
		else setPersistentProperty(TRIAL_STATE_QN, trialFolder, "undone");
	}
	
	/*
	 * Return number of trials associated to this session.
	 * If container is not a session, trials number will be zero.
	 */
	public static int getTrialsNumber(IContainer session) {
		int nbTrials = 0;
		try {
			IResource[] members  = session.members();
			for (IResource member : members) {
				if(ResourceType.isTrial(member)) {
					int trialNumber = TrialNames.getNumber(member);
					if(trialNumber > nbTrials) nbTrials = trialNumber;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return nbTrials;
	}
	
	
	public static String getDataFilesNamesPrefix(IResource resource) {
		if(!resource.exists()) return null;
		if(!ResourceType.isSession(resource)) return null;
		return getPersistentProperty(DATA_FILES_NAMES_PREFIX_QN, resource);
	}
	
	public static void setDataFilesNamesPrefix(IResource resource, String value) {
		setPersistentProperty(DATA_FILES_NAMES_PREFIX_QN, resource, value);
	}
	
	public static boolean useSessionNameInDataFilesNamesAsFirstSuffix(IResource resource) {
		if(!resource.exists()) return false;
		if(!ResourceType.isSession(resource)) return false;
		return "true".equals(getPersistentProperty(USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN, resource));
	}
	
	public static void setUseSessionNameInDataFilesNamesAsFirstSuffix(IResource resource, boolean value) {
		setPersistentProperty(USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN, resource, String.valueOf(value));
	}
	
	public static boolean useTrialNumberInDataFilesNamesAsSecondSuffix(IResource resource) {
		if(!resource.exists()) return false;
		if(!ResourceType.isSession(resource)) return false;
		return "true".equals(getPersistentProperty(USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN, resource));
	}
	
	public static void setUseTrialNumberInDataFilesNamesAsSecondSuffix(IResource resource, boolean value) {
		setPersistentProperty(USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN, resource, String.valueOf(value));
	}
	
	public static TrialStartMode getTrialStartMode(IResource resource) {
		if(!resource.exists()) return TrialStartMode.MANUAL;
		if(!ResourceType.isTrial(resource)) return TrialStartMode.MANUAL;
		String startMode = getPersistentProperty(TRIAL_START_MODE_QN, resource);
		return TrialStartMode.getStartMode(startMode);
	}
	
	public static void setTrialStartMode(IResource resource, TrialStartMode startMode) {
		setPersistentProperty(TRIAL_START_MODE_QN, resource, startMode.getKey());
	}
	
	public static TrialValidateMode getTrialValidateMode(IResource resource) {
		if(!resource.exists()) return TrialValidateMode.MANUAL;
		if(!ResourceType.isTrial(resource)) return TrialValidateMode.MANUAL;
		String validateMode = getPersistentProperty(TRIAL_VALIDATE_MODE_QN, resource);
		return TrialValidateMode.getValidateMode(validateMode);
	}
	
	public static void setTrialValidateMode(IResource resource, TrialValidateMode validateMode) {
		setPersistentProperty(TRIAL_VALIDATE_MODE_QN, resource, validateMode.getKey());
	}
	
	public static void clonePersistentProperties(IResource originResource, IResource detinationResource) {
		ResourceProperties.setDescriptionPersistentProperty(detinationResource, ResourceProperties.getDescriptionPersistentProperty(originResource));
		ResourceProperties.setTypePersistentProperty(detinationResource, ResourceType.getResourceType(originResource).toString());
		if(ResourceType.isSession(originResource)) {
			ResourceProperties.setDataFilesNamesPrefix(detinationResource, ResourceProperties.getDataFilesNamesPrefix(originResource));
			ResourceProperties.setUseSessionNameInDataFilesNamesAsFirstSuffix(detinationResource, ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(originResource));
			ResourceProperties.setUseTrialNumberInDataFilesNamesAsSecondSuffix(detinationResource, ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(originResource));
		}
		if(ResourceType.isTrial(originResource)) {
			ResourceProperties.setAssociatedProcessProperty(detinationResource, ResourceProperties.getAssociatedProcessProperty(originResource));
		}
		if(ResourceType.isDACQConfiguration(originResource)) {
			ResourceProperties.setSystemPersistentProperty(detinationResource, ResourceProperties.getSystemPersistentProperty(originResource));
		}
		if(ResourceType.isProcess(originResource)) {
			ResourceProperties.setAssociatedDACQConfigurationProperty(detinationResource, ResourceProperties.getAssociatedDACQConfigurationProperty(originResource));
		}
		if(ResourceType.isProcessTest(originResource)) {
			ResourceProperties.setAssociatedProcessProperty(detinationResource, ResourceProperties.getAssociatedProcessProperty(originResource));
		}
	}
	
	public static void setGridStateEditor(IResource resource, boolean gridState) {
		setPersistentProperty(GRID_STATE_EDITOR, resource, Boolean.toString(gridState));
	}
	
	public static boolean getGridStateEditor(IResource resource) {
		String gridStateString = getPersistentProperty(GRID_STATE_EDITOR, resource);
		return Boolean.parseBoolean(gridStateString);
	}
	
	public static void setSnapStateEditor(IResource resource, boolean snapState) {
		setPersistentProperty(SNAP_STATE_EDITOR, resource, Boolean.toString(snapState));
	}
	
	public static boolean getSnapStateEditor(IResource resource) {
		String gridStateString = getPersistentProperty(SNAP_STATE_EDITOR, resource);
		return Boolean.parseBoolean(gridStateString);
	}
	
	public static void setZoomScaleEditor(IResource resource, double zoomScale, QualifiedName zoomQN) {
		setPersistentProperty(zoomQN, resource, Double.toString(zoomScale));
	}
	
	public static double getZoomScaleEditor(IResource resource, QualifiedName zoomQN) {
		String zoomScaleString = getPersistentProperty(zoomQN, resource);
		if(zoomScaleString == null) return 1; 
		return Double.parseDouble(zoomScaleString);
	}
	
	//////////////////////////////////////////////////////
	// Resources functions
	/*
	 * Get all resources of type resourceType in container
	 */
	public static IResource[] getAllTypedResources(ResourceType resourceType, IContainer resourceContainer, IProgressMonitor monitor) {
		ArrayList<IResource> resources = new ArrayList<>(0);
		try {
			IResource[] resourcesMember = resourceContainer.members();
			for (IResource currentResource : resourcesMember) {
				String currentResourceType = ResourceProperties.getTypePersistentProperty(currentResource);
				if (resourceType.toString().equals(currentResourceType))
					resources.add(currentResource);
				if (currentResource instanceof IContainer) {
					IResource[] allResources = getAllTypedResources(resourceType, (IContainer) currentResource, monitor);
					resources.addAll(Arrays.asList(allResources));
				}
				if(monitor != null && monitor.isCanceled()) return new IResource[0];
			}
			if(monitor != null && monitor.isCanceled()) return new IResource[0];
			return resources.toArray(new IResource[resources.size()]);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return new IResource[0];
	}

	
	public static void writeResourcePropertiesToPropertiesFile(IResource resource, Properties properties, String keyName) {
		if(ResourceProperties.getTypePersistentProperty(resource) != null) properties.put(keyName + SEPARATOR + TYPE_QN.getLocalName(), ResourceProperties.getTypePersistentProperty(resource));
		if(ResourceProperties.getDescriptionPersistentProperty(resource) != null) properties.put(keyName + SEPARATOR + DESCRIPTION_QN.getLocalName(), ResourceProperties.getDescriptionPersistentProperty(resource));
		if(ResourceType.isDACQConfiguration(resource)) properties.put(keyName + SEPARATOR + SYSTEM_QN.getLocalName(), ResourceProperties.getSystemPersistentProperty(resource));
		if(ResourceType.isProcess(resource)) properties.put(keyName + SEPARATOR + ASSOCIATED_DACQ_CONFIGURATION_FILE_QN.getLocalName(), ResourceProperties.getAssociatedDACQConfigurationProperty(resource));
		if(ResourceType.isProcessTest(resource)) properties.put(keyName + SEPARATOR + ASSOCIATED_PROCESS_FILE_QN.getLocalName(), ResourceProperties.getAssociatedProcessProperty(resource));
		if(ResourceType.isTrial(resource)) {
			properties.put(keyName + SEPARATOR + ASSOCIATED_PROCESS_FILE_QN.getLocalName(), ResourceProperties.getAssociatedProcessProperty(resource));
			properties.put(keyName + SEPARATOR + TRIAL_STATE_QN.getLocalName(), String.valueOf(ResourceProperties.isTrialDone(resource)));
			properties.put(keyName + SEPARATOR + TRIAL_START_MODE_QN.getLocalName(), ResourceProperties.getTrialStartMode(resource).toString());
		}
		if(ResourceType.isSession(resource)) {
			if(ResourceProperties.getDataFilesNamesPrefix(resource) != null) properties.put(keyName + SEPARATOR + DATA_FILES_NAMES_PREFIX_QN.getLocalName(), ResourceProperties.getDataFilesNamesPrefix(resource));
			properties.put(keyName + SEPARATOR + USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES_QN.getLocalName(), String.valueOf(ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(resource)));
			properties.put(keyName + SEPARATOR + USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES_QN.getLocalName(), String.valueOf(ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(resource)));
		}
		if(ResourceType.isLog(resource)) {
			String system = ResourceProperties.getSystemPersistentProperty(resource);
			if(system != null) {
				properties.put(keyName + SEPARATOR + SYSTEM_QN.getLocalName(), system);
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/*
	* Methods for session resource properties 
	*/
	
	public static void setSessionProperty(QualifiedName qn, IResource resource, Object object) {
		try {
			resource.setSessionProperty(qn, object);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static Object getSessionProperty(QualifiedName qn, IResource resource) {
		Object value = null;
		try {
			value = resource.getSessionProperty(qn);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return value;
	}
	
	public static Map<QualifiedName, Object> getSessionProperties(IResource resource) throws CoreException {
		return resource.getSessionProperties();
	}
	
	/*
	 * Return deserialized associated resource object
	 */
	public static Object getObjectSessionProperty(IResource resource) {
		try {
			Object object = resource.getSessionProperty(OBJECT_QN);
			return object;
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return null;
	}
	
	/*
	 * Set deserialized associated resource object
	 */
	public static void setObjectSessionProperty(IResource resource, Object object) {
		try {
			resource.setSessionProperty(OBJECT_QN, object);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	public static double getPercentDone(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return 0;
		int nbTrials = getTotalNumberOfTrials(resource);
		int[] undoneTrials = getUndoneTrialsNumbers(resource);
		return 1.0 * (nbTrials - undoneTrials.length)/nbTrials;
	}

	public static int getTotalNumberOfTrials(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return 0;
		try {
			IResource[] members = ((IContainer)resource).members();
			if(ResourceType.isSession(resource)) {
				int total = 0;
				for (IResource member : members) {
					if(ResourceType.isTrial(member)) {
						total += 1;
					}
				}
				return total;
			} else {
				int total = 0;
				for (IResource member : members) {
					if(ResourceType.isSession(member)) {
						total += getTotalNumberOfTrials(member);
					}
				}
				return total;
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	public static int[] getUndoneTrialsNumbers(IResource resource) {
		if(!(ResourceType.isSession(resource) || ResourceType.isSubject(resource))) return new int[0];
		try {
			IResource[] members = ((IContainer)resource).members();
			if(ResourceType.isSession(resource)) {
				ArrayList<Integer> numbersArrayList = new ArrayList<Integer>(0);
				for (int i = 0; i < members.length; i++) {
					if(ResourceType.isTrial(members[i])) {
						if(!ResourceProperties.isTrialDone(members[i])) {
							int trialNumber = TrialNames.getNumber(members[i]);
							if(trialNumber > 0) numbersArrayList.add(trialNumber);
						}
					}
				}
				int[] numbers = new int[numbersArrayList.size()];
				for (int i = 0; i < numbers.length; i++) {
					numbers[i] = numbersArrayList.get(i);
				}
				return numbers;
			} else {
				int total = 0;
				ArrayList<Integer> numbersArrayList = new ArrayList<Integer>(0);
				for (IResource member : members) {
					if(ResourceType.isSession(member)) {
						int[] undoneTrials = getUndoneTrialsNumbers(member);
						for (int i = 0; i < undoneTrials.length; i++) {
							numbersArrayList.add(total + undoneTrials[i]);
						}
						total += getTotalNumberOfTrials(member);
					}
				}
				int[] numbers = new int[numbersArrayList.size()];
				for (int i = 0; i < numbers.length; i++) {
					numbers[i] = numbersArrayList.get(i);
				}
				return numbers;
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return new int[0];
	}
	
	public static long getSamplesNumber(IResource samplesFile) {
		if(!ResourceType.isSamples(samplesFile)) return 0;
		String wp = samplesFile.getWorkspace().getRoot().getLocation().toOSString();
		File trialFile = new File(wp + samplesFile.getFullPath().toOSString());
		return trialFile.length()/4;
	}
	
	public static void setSubjectModified(IResource resource, boolean modified) {
		try {
			if(!ResourceType.isSubject(resource)) return;
			resource.setSessionProperty(SUBJECT_MODIFIED_QN, modified);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public static boolean isSubjectModified(IResource resource) {
		boolean modified = false;
		try {
			if(ResourceType.isSubject(resource)) {
				if(resource.getSessionProperty(SUBJECT_MODIFIED_QN) != null)
					modified = (boolean) resource.getSessionProperty(SUBJECT_MODIFIED_QN);
			}
			
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return modified;
	}

	/*
	 * Signature (SHA-256) of a source code
	 */
	private static String getCodeSignature(String code) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(code.getBytes(StandardCharsets.UTF_8));
			StringBuilder signature = new StringBuilder();
			for (byte value : digest) signature.append(String.format("%02x", value));
			return signature.toString();
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			return null;
		}
	}
	
	/**
	 * Saves the signature of the source code of a process when it is compiled. Lines numbers of compilation markers 
	 * refer to the compiled file : they can be linked to the source code only while this source code is unchanged.
	 * Saved in workspace metadata only (not in properties file) as it is only a cache.
	 * @param process the process resource
	 * @param sourceCode the source code (as displayed in source code page)
	 * @param linesShift the number of lines removed at the beginning of the source code to get the compiled code
	 */
	public static void setCompiledCodeSignature(IResource process, String sourceCode, int linesShift) {
		try {
			if(process == null || !process.exists()) return;
			String signature = sourceCode == null ? null : getCodeSignature(sourceCode);
			process.setPersistentProperty(COMPILED_CODE_SIGNATURE_QN, signature == null ? null : linesShift + ":" + signature);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/**
	 * Returns the shift between lines of the compiled file and lines of a source code, if this source code
	 * is the one which has been compiled for the last time.
	 * @param process the process resource
	 * @param sourceCode the source code (as displayed in source code page)
	 * @return the number of lines to add to a compiled file line to get the source code line, 
	 * or -1 if this source code has not been compiled (modified since last compilation or unknown)
	 */
	public static int getCompiledCodeLinesShift(IResource process, String sourceCode) {
		try {
			if(process == null || !process.exists() || sourceCode == null) return -1;
			String value = process.getPersistentProperty(COMPILED_CODE_SIGNATURE_QN);
			if(value == null) return -1;
			int separatorIndex = value.indexOf(':');
			if(separatorIndex < 0) return -1;
			if(!value.substring(separatorIndex + 1).equals(getCodeSignature(sourceCode))) return -1;
			return Integer.parseInt(value.substring(0, separatorIndex));
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
		}
		return -1;
	}

}
