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
package fr.univamu.ism.docometre.wizards;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.signedcontent.InvalidContentException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.bytecode.opencsv.CSVReader;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceNames;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.TrialNames;
import fr.univamu.ism.docometre.TrialStartMode;
import fr.univamu.ism.docometre.TrialValidateMode;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.history.GitResources;
import fr.univamu.ism.docometre.history.SnapshotManager;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class ImportResourceWizard extends Wizard implements IWorkbenchWizard {
	
	protected static IContainer parentResource;
	private ImportResourceWizardPage importResourceWizardPage;
	private boolean configureHeader;
	private String separatorChar;
	private boolean abort;
	private int columnNumber;
	
	@Override
	public String getWindowTitle() {
		return DocometreMessages.ImportResourceWizardWindowTitle;
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		importResourceWizardPage = new ImportResourceWizardPage("ImportResourceWizardPage");
		addPage(importResourceWizardPage);
	}

	@Override
	public boolean performFinish() {
		ITreeSelection selection = importResourceWizardPage.getSelection();
		ResourceType resourceType = importResourceWizardPage.getResourceType();
		Object[] elements = selection.toArray();
		
		if(resourceType.equals(ResourceType.OPTITRACK_TYPE_1)) {
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(DocometreMessages.ImportResourceWizardOptitrack1Message, elements.length);
						for (Object element : elements) {
							File file = (File)element;
							if(file.isDirectory()) importSubjectsFormOptitrack1(file);
							monitor.worked(1);
						}
						monitor.done();
					}
				});
				parentResource.refreshLocal(IResource.DEPTH_INFINITE, null);
				ExperimentsView.refresh(parentResource, null);
			} catch (CoreException | InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			return true;
		} 
		
		if(resourceType.equals(ResourceType.OPTITRACK_TYPE_2)) {
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(DocometreMessages.ImportResourceWizardOptitrack1Message, elements.length);
						for (Object element : elements) {
							File file = (File)element;
							if(file.isDirectory()) importSubjectsFormOptitrack2(file);
							monitor.worked(1);
						}
						monitor.done();
					}
				});
				parentResource.refreshLocal(IResource.DEPTH_INFINITE, null);
				ExperimentsView.refresh(parentResource, null);
			} catch (CoreException | InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			return true;
		} 
		
		if(resourceType.equals(ResourceType.COLUMN_DATA_FILE)) {
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						int nbFiles = 0;
						for (Object element : elements) {
							File file = (File)element;
							nbFiles += file.list().length;
						}
						nbFiles += elements.length;
						monitor.beginTask(DocometreMessages.ImportResourceWizardDataColumnFilesMessage, nbFiles);
						for (Object element : elements) {
							File file = (File)element;
							if(file.isDirectory()) importSubjectsFormDataColumnFiles(file, monitor);
							monitor.worked(1);
						}
						monitor.done();
					}
				});
				parentResource.refreshLocal(IResource.DEPTH_INFINITE, null);
				ExperimentsView.refresh(parentResource, null);
			} catch (CoreException | InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			return true;
		}
		
		for (Object element : elements) {
			File file = (File)element;
			if(!file.isDirectory()) {
				String rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				if(file.getName().endsWith(Activator.adwFileExtension)) {
					try {
						// Import ADW File
						// First create new subject : subject name is file name without extension
						String subjectName = file.getName().replaceAll(Activator.adwFileExtension + "$", "");
						IFolder subject = parentResource.getFolder(new org.eclipse.core.runtime.Path(subjectName));
						subject.create(true, true, null);
						ResourceProperties.setDescriptionPersistentProperty(subject, "");
						ResourceProperties.setTypePersistentProperty(subject, ResourceType.SUBJECT.toString());
						// Then copy data file in this new created subject
						Path newPath = Paths.get(rootPath + parentResource.getFullPath().toOSString() + File.separator + subject.getName() + File.separator + file.getName());
						Path originalPath = file.toPath();
						Files.copy(originalPath, newPath, StandardCopyOption.REPLACE_EXISTING);
						IFile newFile = subject.getFile(new org.eclipse.core.runtime.Path(file.getName()));
						newFile.refreshLocal(IResource.DEPTH_ZERO, null);
						ResourceProperties.setDescriptionPersistentProperty(newFile, "");
						ResourceProperties.setTypePersistentProperty(newFile, ResourceType.ADW_DATA_FILE.toString());
						ExperimentsView.refresh(subject.getParent(), new IResource[]{subject});
						SubjectsView.refresh();
					} catch (CoreException | IOException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
				} else if(file.getName().endsWith(".zip") || file.getName().endsWith(".tar")) {
					// Experiment name : by default, archive file name without extension
					String experimentName = null;
					if(resourceType.equals(ResourceType.EXPERIMENT)) {
						experimentName = askExperimentName(file);
						// Import of this experiment canceled by user
						if(experimentName == null) continue;
					}
					String newExperimentName = experimentName;
					try {
						// Import experiment or subjects
						getContainer().run(true, false, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
									if(resourceType.equals(ResourceType.EXPERIMENT)) {
										// Import experiment
										
										//Check again if an experiment with same name already exists
										String errorMessage = validateExperimentName(newExperimentName);
										if(errorMessage != null) {
											getShell().getDisplay().syncExec(new Runnable() {
												@Override
												public void run() {
													MessageDialog.openError(getShell(), DocometreMessages.ImportErrorDialogTitle, errorMessage);
												}
											});
											return;
										}
										
										// Archives exported by previous versions : all entries are in a root folder named as the archive 
										// and experiment properties are in a "<root folder>.properties" file.
										// Otherwise, archive entries are relative to experiment and experiment name is not written in archive.
										String archiveRootFolder = getArchiveRootFolder(file);
										
										int nbFilesToExtract = getNbFiles(file);
										int nbPropertiesEntriesToApply = archiveRootFolder != null ? getNbProperties(file, archiveRootFolder, rootPath) : 0;
										
										SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.ImportingExperimentFromCompressedFile, nbFilesToExtract + nbPropertiesEntriesToApply + 4);

										subMonitor.subTask(DocometreMessages.CreatingNewExperimentInWorkspace);
										IProject newExperiment = ResourcesPlugin.getWorkspace().getRoot().getProject(newExperimentName);
										newExperiment.create(null);
										newExperiment.open(null);
										subMonitor.worked(1);
										
										unzip(file.getAbsolutePath(), newExperiment.getLocation().toOSString(), archiveRootFolder, subMonitor);
										
										subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
										newExperiment.refreshLocal(IResource.DEPTH_INFINITE, null);
										subMonitor.worked(1);
										
										// Experiments history imported with the experiment : .git folder is not a workspace resource
										if(SnapshotManager.getInstance().isHistorized(newExperiment)) GitResources.excludeGitFolder(newExperiment, null);
										
										// Adapt .metadata.properties file to experiment name and apply its properties
										ResourceProperties.importPropertiesFile(newExperiment, archiveRootFolder == null);
										subMonitor.worked(1);
										
										if(archiveRootFolder != null) {
											// Archive exported by a previous version : apply properties of "<root folder>.properties" file
											IFile propertiesFile = newExperiment.getFile(archiveRootFolder + ".properties");
											if(propertiesFile.exists()) {
												readAndApplyPersitentProperties(newExperiment, subMonitor, nbPropertiesEntriesToApply, propertiesFile.getLocation().toOSString());
												propertiesFile.delete(true, null);
											}
										}
										// Experiment exported by a previous version : trials folders named "Essai n°N"...
										TrialNames.migrate(newExperiment, null);
										
										subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
										newExperiment.refreshLocal(IResource.DEPTH_INFINITE, null);
										// .metadata.properties file is now complete and up to date with experiment name
										ResourceProperties.synchronizePropertiesFile(newExperiment);
										subMonitor.worked(1);
										
										subMonitor.subTask(DocometreMessages.AddingProjectToBuilderAndRefreshingExperiment);
										DocometreBuilder.addProject(newExperiment);
										ExperimentsView.refresh(newExperiment.getParent(), new IResource[]{newExperiment});
										
										subMonitor.done();
										monitor.done();
									} else if(resourceType.equals(ResourceType.SUBJECT)) {
										IProject experiment = (IProject) parentResource;
										String fromExperimentName = file.getName().replaceAll(".zip$", "").replaceAll(".tar$", "");
										int nbFilesToExtract = getNbFiles(file);
										int nbPropertiesEntriesToApply = getNbProperties(file, fromExperimentName, rootPath);
										
										SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.ImportingExperimentFromCompressedFile, nbFilesToExtract + nbPropertiesEntriesToApply + 2);
										
										// Extract to temporary folder
										String destinationFolder =  parentResource.getLocation().toPortableString();
										String id = String.valueOf(System.currentTimeMillis());
										String temporaryFolder =  destinationFolder + "/" + "Temp_" + id;
										unzip(file.getAbsolutePath(), temporaryFolder, null, subMonitor);
										
										Stream<Path> stream = null;
										String propertiesFileFullPath = null;
										try {
											// Move subjects folder to experiment folder
											stream = Files.walk(Paths.get(temporaryFolder), 2);
											List<Path> subjectsStream = stream.filter(folder -> Files.isDirectory(folder)).collect(Collectors.toList());
											for (Path path : subjectsStream) {
												if(Paths.get(temporaryFolder).getNameCount() + 2 == path.getNameCount()) {
													Files.move(path, Paths.get(destinationFolder + "/" + path.getName(path.getNameCount() - 1).toString()));
												}
												
											}
											// Move properties file to experiment folder
											stream.close();
											stream = Files.walk(Paths.get(temporaryFolder), 2);
											List<Path> propertiesStream = stream.filter(folder -> !Files.isDirectory(folder)).collect(Collectors.toList());
											for (Path path : propertiesStream) {
												if(Paths.get(temporaryFolder).getNameCount() + 2 == path.getNameCount()) {
													propertiesFileFullPath = destinationFolder + "/"+ path.getName(path.getNameCount() - 1).toString();
													Files.move(path, Paths.get(propertiesFileFullPath));
												}
												
											}
											// Delete temp folder
											Files.walk(Paths.get(temporaryFolder)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
											
											subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
											parentResource.refreshLocal(IResource.DEPTH_INFINITE, null);
											subMonitor.worked(1);
											
											readAndApplyPersitentProperties(experiment, subMonitor, nbPropertiesEntriesToApply, propertiesFileFullPath);
											String[] propertiesFileFullPathSegments = propertiesFileFullPath.split("/");
											IFile propertiesFile = experiment.getFile(propertiesFileFullPathSegments[propertiesFileFullPathSegments.length - 1]);
											if(propertiesFile != null) propertiesFile.delete(true, null);
											// Resources exported by a previous version : trials folders named "Essai n°N"...
											TrialNames.migrate(experiment, null);
											
											
										} catch (IOException e) {
											Activator.logErrorMessageWithCause(e);
											e.printStackTrace();
										} finally {
											if(stream != null) stream.close();
											subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
											parentResource.refreshLocal(IResource.DEPTH_INFINITE, null);
											ExperimentsView.refresh(parentResource, null);
											subMonitor.worked(1);
										}
										
									}
									
									
								} catch (CoreException e) {
									Activator.getLogErrorMessageWithCause(e);
									e.printStackTrace();
								}
								
							}
						});
						
						
					} catch (InvocationTargetException | InterruptedException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
				} else if(file.getName().endsWith(Activator.daqFileExtension) || file.getName().endsWith(Activator.processFileExtension) || file.getName().endsWith(Activator.dataProcessingFileExtension)) {
					// Import process or dacq file or data processing file
					Path newPath = Paths.get(rootPath + parentResource.getFullPath().toOSString() + File.separator + file.getName());
					Path originalPath = file.toPath();
					if(!originalPath.startsWith(rootPath)) {
					    try {
							Files.copy(originalPath, newPath, StandardCopyOption.REPLACE_EXISTING);
							IFile newFile = parentResource.getFile(new org.eclipse.core.runtime.Path(file.getName()));
							newFile.refreshLocal(IResource.DEPTH_ZERO, null);
							Object object = ObjectsController.deserialize(newFile);
							ResourceProperties.setObjectSessionProperty(newFile, object);
							ObjectsController.addHandle(object);
							if(file.getName().endsWith(Activator.daqFileExtension)) {
								ResourceProperties.setTypePersistentProperty(newFile, ResourceType.DACQ_CONFIGURATION.toString());
								if(object instanceof ADWinDACQConfiguration) ResourceProperties.setSystemPersistentProperty(newFile, Activator.ADWIN_SYSTEM);
								if(object instanceof ArduinoUnoDACQConfiguration) ResourceProperties.setSystemPersistentProperty(newFile, Activator.ARDUINO_UNO_SYSTEM);
							}
							if(file.getName().endsWith(Activator.processFileExtension)) {
								ResourceProperties.setTypePersistentProperty(newFile, ResourceType.PROCESS.toString());
							}
							if(file.getName().endsWith(Activator.dataProcessingFileExtension)) {
								ResourceProperties.setTypePersistentProperty(newFile, ResourceType.DATA_PROCESSING.toString());
							}
							ObjectsController.removeHandle(object);
							ExperimentsView.refresh(parentResource, new IResource[]{newFile});
							
						} catch (IOException | CoreException e) {
							e.printStackTrace();
							Activator.logErrorMessageWithCause(e);
						}
					} else {
						Activator.logWarningMessage(NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage1, file.getAbsolutePath()));
						Activator.logWarningMessage(DocometreMessages.ImportResourceWizardErrorMessage2);
					}
				} else if(file.getName().endsWith(".ini") || file.getName().endsWith(".txt") || file.getName().endsWith(".properties")) {
					// Open properties file
					try {
						ArrayList<IFolder> newSessions = new ArrayList<>();
						InputStream sessionsInputStream = new FileInputStream(file);
						Properties sessionsProperties = new Properties();
						sessionsProperties.load(sessionsInputStream);
						int numSession = 1;
						// Try to read "session1.name" key
						for (String sessionName = sessionsProperties.getProperty("session" + numSession + ".name"); sessionName != null; ) {
							IResource resource = parentResource.findMember(sessionName);
							// Check this resource does not exist
							if(sessionName.matches("^[a-zA-Z][a-zA-Z0-9_]*$") && resource == null) {
								try {
									// Add session
									IFolder newSession = parentResource.getFolder(new org.eclipse.core.runtime.Path(sessionName));
									newSession.create(true, true, null);
									newSessions.add(newSession);
									ResourceProperties.setTypePersistentProperty(newSession, ResourceType.SESSION.toString());
									// Read trials keys
									int numTrial = 1;
									for (String trialKey = sessionsProperties.getProperty("session" + numSession + ".trial" + numTrial); trialKey != null; ) {
										// Add Trial
										String[] values = trialKey.split(":");
										String startMode = values.length > 0 ? TrialStartMode.getStartMode(values[0]).getKey():TrialStartMode.MANUAL.getKey();
										String validateMode = values.length > 1 ? TrialValidateMode.getValidateMode(values[1]).getKey():TrialValidateMode.MANUAL.getKey();
										String process = values.length > 2 ? values[2]:null;
										if(process != null) process = process.endsWith(Activator.processFileExtension) ? process : process + Activator.processFileExtension;
										IFolder newTrial = TrialNames.getTrial(newSession, numTrial);
										newTrial.create(true, true, null);
										ResourceProperties.setTypePersistentProperty(newTrial, ResourceType.TRIAL.toString());
										ResourceProperties.setTrialStartMode(newTrial, TrialStartMode.getStartMode(startMode));
										ResourceProperties.setTrialValidateMode(newTrial, TrialValidateMode.getValidateMode(validateMode));
										ResourceProperties.setAssociatedProcessProperty(newTrial, process);
										numTrial++;
										trialKey = sessionsProperties.getProperty("session" + numSession + ".trial" + numTrial);
									}
								} catch (CoreException e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
								}
								
							} else {
								if(!sessionName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
									String message = NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage7, sessionName);
									Activator.logErrorMessage(message);
								} else {
									String message = NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage4, sessionName);
									Activator.logErrorMessage(message);
								}
							}
							numSession++;
							sessionName = sessionsProperties.getProperty("session" + numSession + ".name");
						}
						if(numSession == 1) {
							Throwable throwable = new Throwable(DocometreMessages.ImportResourceWizardErrorMessage5);
							String message = NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage6, file.getAbsolutePath());
							throw new InvalidContentException(message, throwable);
						} else {
							ExperimentsView.refresh(parentResource, newSessions.toArray(new IResource[newSessions.size()]));
						}
					} catch (IOException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				}
			 
			} else Activator.logWarningMessage(NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage3, file.getAbsolutePath()));
		} 
		return true;
	}
	
	private void importSubjectsFormOptitrack1(File file) {
		try {
			String fileName = file.getName();
			// Add subject and session
			Pattern pattern = Pattern.compile("^[a-zA-Z]+[0-9]+");
			Matcher matcher = pattern.matcher(fileName);
			while (matcher.find()) {
				String subjectName = matcher.group();
				String sessionName = fileName.substring(matcher.end());
				IResource newSubject = parentResource.findMember(subjectName);
				if(newSubject == null) {
					newSubject = parentResource.getFolder(new org.eclipse.core.runtime.Path(subjectName));
					((IFolder)newSubject).create(true, true, null);
					ResourceProperties.setTypePersistentProperty(newSubject, ResourceType.SUBJECT.toString());
				}
				IResource newSession = ((IFolder)newSubject).findMember(sessionName);
				if(newSession == null) {
					newSession = ((IFolder)newSubject).getFolder(new org.eclipse.core.runtime.Path(sessionName));
					((IFolder)newSession).create(true, true, null);
					ResourceProperties.setTypePersistentProperty(newSession, ResourceType.SESSION.toString());
				}
				// Then copy data file in this new created subject
				String rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				rootPath = rootPath + parentResource.getFullPath().toOSString() + File.separator + newSubject.getName();
				rootPath = rootPath + File.separator + newSession.getName();
				String[] sessionFilesList = file.list();
				for (String dataFile : sessionFilesList) {
					Path originalPath = file.toPath().resolve(dataFile);
					Path newPath = Paths.get(rootPath + File.separator + originalPath.getFileName().toString());
					Files.copy(originalPath, newPath, StandardCopyOption.REPLACE_EXISTING);
					IFile newFile = ((IFolder)newSession).getFile(newPath.getFileName().toString());
					newFile.refreshLocal(IResource.DEPTH_ZERO, null);
					ResourceProperties.setDescriptionPersistentProperty(newFile, "");
					ResourceProperties.setTypePersistentProperty(newFile, ResourceType.OPTITRACK_TYPE_1.toString());
				}
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private void importSubjectsFormOptitrack2(File file) {
		try {
			String fileName = file.getName();
			// Add subject and session
			Pattern pattern = Pattern.compile("^[a-zA-Z]+[0-9]+");
			Matcher matcher = pattern.matcher(fileName);
			while (matcher.find()) {
				String subjectName = matcher.group();
				String sessionName = fileName.substring(matcher.end());
				if("".equals(sessionName)) sessionName = "Session1";
				IResource newSubject = parentResource.findMember(subjectName);
				if(newSubject == null) {
					newSubject = parentResource.getFolder(new org.eclipse.core.runtime.Path(subjectName));
					((IFolder)newSubject).create(true, true, null);
					ResourceProperties.setTypePersistentProperty(newSubject, ResourceType.SUBJECT.toString());
				}
				IResource newSession = ((IFolder)newSubject).findMember(sessionName);
				if(newSession == null) {
					newSession = ((IFolder)newSubject).getFolder(new org.eclipse.core.runtime.Path(sessionName));
					((IFolder)newSession).create(true, true, null);
					ResourceProperties.setTypePersistentProperty(newSession, ResourceType.SESSION.toString());
				}
				// Then copy data file in this new created subject
				String rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				rootPath = rootPath + parentResource.getFullPath().toOSString() + File.separator + newSubject.getName();
				rootPath = rootPath + File.separator + newSession.getName();
				String[] sessionFilesList = file.list();
				for (String dataFile : sessionFilesList) {
					Path originalPath = file.toPath().resolve(dataFile);
					Path newPath = Paths.get(rootPath + File.separator + originalPath.getFileName().toString());
					Files.copy(originalPath, newPath, StandardCopyOption.REPLACE_EXISTING);
					IFile newFile = ((IFolder)newSession).getFile(newPath.getFileName().toString());
					newFile.refreshLocal(IResource.DEPTH_ZERO, null);
					ResourceProperties.setDescriptionPersistentProperty(newFile, "");
					ResourceProperties.setTypePersistentProperty(newFile, ResourceType.OPTITRACK_TYPE_2.toString());
				}
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private void importSubjectsFormDataColumnFiles(File file, IProgressMonitor monitor) {
		try {
			System.out.println("Import : " + file.getAbsolutePath());
			File[] textFiles = file.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".txt");
				}
			});
			abort = false;
			separatorChar = "\\t";
			configureHeader = false;		
			getShell().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					InputDialog inputDialog = new InputDialog(getShell(), DocometreMessages.ConfigureSeparatorTitle, NLS.bind(DocometreMessages.SeparatorForMessage, file.getName()), separatorChar, null);
					if(inputDialog.open() == Dialog.OK) {
						separatorChar = inputDialog.getValue().replaceAll("\\\\t", "\t");
						configureHeader = MessageDialog.openQuestion(getShell(), DocometreMessages.ConfigureHeaderTitle, NLS.bind(DocometreMessages.ConfigureHeaderMessage, file.getName()));
					} else {
						configureHeader = false;
						abort = true;
					}
				}
			});
			LinkedHashMap<String, String> channelsNameAndFrequency = new LinkedHashMap<>();
			if(textFiles.length > 0 && configureHeader && !abort) {
				BufferedReader reader = Files.newBufferedReader(textFiles[0].toPath());
				CSVReader csvReader  = new CSVReader(reader);
				List<String[]> values = csvReader.readAll();
				csvReader.close();
				String firstLine = values.get(0)[0];
				String[] firstLineSplitted = firstLine.split(separatorChar);
				columnNumber = 1;
				for (int n = 1; n <= firstLineSplitted.length; n++) {
					columnNumber = n;
					getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
							String channelName = preferenceStore.getString("LAST_CHANNEL_NAME_COLUMN_" + columnNumber);
							String frequency = preferenceStore.getString("LAST_CHANNEL_FREQUENCY_COLUMN_" + columnNumber);
							InputDialog inputDialog = new InputDialog(getShell(), DocometreMessages.ChannelNameTitle, NLS.bind(DocometreMessages.ChannelNameMessage, columnNumber), channelName, null);
							if(inputDialog.open() == Dialog.OK) {
								channelName = inputDialog.getValue();
								preferenceStore.putValue("LAST_CHANNEL_NAME_COLUMN_" + columnNumber, channelName);
							} else abort = true;
							
							inputDialog = new InputDialog(getShell(), DocometreMessages.ChannelFrequencyTitle, NLS.bind(DocometreMessages.ChannelFrequencyMessage, columnNumber), frequency, null);
							if(inputDialog.open() == Dialog.OK) {
								frequency = inputDialog.getValue();
								preferenceStore.putValue("LAST_CHANNEL_FREQUENCY_COLUMN_" + columnNumber, frequency);
							} else abort = true;
							if(!abort) {
								channelsNameAndFrequency.put(channelName, frequency);
							}
						}
					});
					if(abort) break;
				}
			}
			if(!abort) {
				// Create Subject
				String subjectName = file.getName().replaceAll("\\s+", "_");
				IResource newSubject = parentResource.findMember(subjectName);
				if(newSubject == null) {
					newSubject = parentResource.getFolder(new org.eclipse.core.runtime.Path(subjectName));
					((IFolder)newSubject).create(true, true, null);
					ResourceProperties.setTypePersistentProperty(newSubject, ResourceType.SUBJECT.toString());
				}
				// Copy all text files adding header if necessary
				String rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				rootPath = rootPath + parentResource.getFullPath().toOSString() + File.separator + newSubject.getName();
				String[] textFilesList = file.list();
				for (String dataFile : textFilesList) {
					// Create file with header
					Path originalPath = file.toPath().resolve(dataFile);
					Path newPath = Paths.get(rootPath + File.separator + originalPath.getFileName().toString());
					if(configureHeader) {
						String headerName = "";
						String headerFrequency = "";
						for (String name : channelsNameAndFrequency.keySet()) {
							headerName = headerName + name + separatorChar;
							headerFrequency = headerFrequency + channelsNameAndFrequency.get(name) + separatorChar;
						}
						headerName = headerName.replaceAll(separatorChar + "$", "\n");
						headerFrequency = headerFrequency.replaceAll(separatorChar + "$", "\n");
						Files.write(newPath, headerName.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
						Files.write(newPath, headerFrequency.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					}
					List<String> lines = Files.readAllLines(originalPath, StandardCharsets.UTF_8);
					Files.write(newPath, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					IFile newFile = ((IFolder)newSubject).getFile(newPath.getFileName().toString());
					newFile.refreshLocal(IResource.DEPTH_ZERO, null);
					ResourceProperties.setDescriptionPersistentProperty(newFile, "");
					ResourceProperties.setTypePersistentProperty(newFile, ResourceType.COLUMN_DATA_FILE.toString());
					monitor.worked(1);
				}
			}
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} 
	}

	protected int getNbProperties(File file, String experimentName, String rootPath) {
		int nbPropertiesEntries = 0;
		try {
			ZipFile zipFile = new ZipFile(file);
			FileInputStream fis = new FileInputStream(file);
	        ZipInputStream zis = new ZipInputStream(fis);
	        ZipEntry ze = zis.getNextEntry();
	        while(ze != null) {
	        	if(ze.getName().endsWith(".properties")) {
	        		extractFile(zis, ze, rootPath + File.separator + "temp.properties");
	        		File temporaryPropertiesFile = new File(rootPath + File.separator + "temp.properties");
	        		FileInputStream fileInputStream = new FileInputStream(temporaryPropertiesFile);
	        		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
	        		Properties properties = new Properties();
	        		properties.load(inputStreamReader);
	        		Set<Object> keys = properties.keySet();
	        		nbPropertiesEntries = keys.size();
	        		inputStreamReader.close();
	        		temporaryPropertiesFile.delete();
	        		break;
	        	}
                ze = zis.getNextEntry();
	        }
	        zipFile.close();
	        zis.close();
    		return nbPropertiesEntries;
		} catch (IOException e) {
			Activator.getLogErrorMessageWithCause(e);
			e.printStackTrace();
		}
        return nbPropertiesEntries;
	}

	protected int getNbFiles(File file) {
		try {
			ZipFile zipFile = new ZipFile(file);
			int nbFiles = zipFile.size(); 
	    	zipFile.close();
			return nbFiles;
		} catch (IOException e) {
			Activator.getLogErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return 0;
	}

	/*
	 * Apply properties of a properties file created by ExperimentPropertiesFileCreator to experiment resources.
	 * Keys are full paths of resources when exported : "/<archive root folder>/Subject/...>>>type".
	 * Root folder is replaced by experiment name, as well as first segment of paths values.
	 */
	private void readAndApplyPersitentProperties(IProject experiment, SubMonitor subMonitor, int nbProperties, String propertiesFileFullPath) {
		try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(propertiesFileFullPath), StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(inputStreamReader);
			int numProperty = 1;
			for (String key : properties.stringPropertyNames()) {
				if(key.equals("OnlySubjects")) continue;
				String value = properties.getProperty(key);
				String[] keyArray = key.split(ResourceProperties.SEPARATOR);
				if(keyArray.length != 2) continue;
				// Keys and paths values may have been written on another OS
				IPath exportedPath = IPath.fromPortableString(keyArray[0].replace('\\', '/'));
				IPath resourcePath = experiment.getFullPath().append(exportedPath.removeFirstSegments(1));
				if(isPathProperty(keyArray[1]) && value != null) {
					IPath valuePath = IPath.fromPortableString(value.replace('\\', '/'));
					if(valuePath.segmentCount() > 0) value = experiment.getFullPath().append(valuePath.removeFirstSegments(1)).toOSString();
				}
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
				
				String message = NLS.bind(DocometreMessages.ApplyingProperty, new Object[] {numProperty, nbProperties, value, resourcePath.toOSString(), keyArray[1]});
				subMonitor.subTask(message);
				
				if(resource != null && resource.exists()) {
					QualifiedName QN = new QualifiedName(Activator.PLUGIN_ID, keyArray[1]);
					ResourceProperties.setPersistentProperty(QN, resource, value);
				}

				subMonitor.worked(1);
				numProperty++;
			}
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	/*
	 * Return true if values of this property are resources full paths
	 */
	private static boolean isPathProperty(String localName) {
		return ResourceProperties.DEFAULT_DACQ_QN.getLocalName().equals(localName) 
				|| ResourceProperties.ASSOCIATED_DACQ_CONFIGURATION_FILE_QN.getLocalName().equals(localName) 
				|| ResourceProperties.ASSOCIATED_PROCESS_FILE_QN.getLocalName().equals(localName);
	}
	
	/*
	 * Archives exported by previous versions : all entries are in a root folder (archive file name when exported).
	 * Return this root folder name, or null if archive entries are relative to experiment, i.e. experiment 
	 * .metadata.properties file is at archive root.
	 */
	protected static String getArchiveRootFolder(File file) {
		String rootFolder = null;
		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName().replace('\\', '/');
				if(("/" + name).equals(ResourceProperties.metadataFileName)) return null;
				int index = name.indexOf('/');
				// A file at archive root : no root folder
				if(index <= 0) return null;
				String firstSegment = name.substring(0, index);
				if(rootFolder == null) rootFolder = firstSegment;
				else if(!rootFolder.equals(firstSegment)) return null;
			}
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return rootFolder;
	}
	
	/*
	 * Return null if experiment name is valid and if no experiment (or folder in workspace) has the same name, an error message otherwise
	 */
	protected static String validateExperimentName(String name) {
		// Same rules as when an experiment is created or renamed
		if(name == null || !name.matches(ResourceNames.REGULAR_EXPRESSION)) return NLS.bind(DocometreMessages.NewResourceWizard_ErrorMessage, name);
		String namesRulesErrorMessage = ResourceNames.getErrorMessage(name, false);
		if(namesRulesErrorMessage != null) return namesRulesErrorMessage;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.PROJECT);
		if(!status.isOK()) return status.getMessage();
		String message = NLS.bind(DocometreMessages.ImportExperimentNameAlreadyExists, name);
		// File systems may be case insensitive
		for (IProject project : workspaceRoot.getProjects()) {
			if(project.getName().equalsIgnoreCase(name)) return message;
		}
		String[] workspaceFilesNames = workspaceRoot.getLocation().toFile().list();
		if(workspaceFilesNames != null) {
			for (String fileName : workspaceFilesNames) {
				if(fileName.equalsIgnoreCase(name)) return message;
			}
		}
		return null;
	}
	
	/*
	 * Open a dialog to get the name of experiment imported from archive file. Default name is
	 * archive file name without extension. Return null if user cancels.
	 */
	private String askExperimentName(File file) {
		String defaultName = file.getName().replaceAll("\\.zip$", "").replaceAll("\\.tar$", "");
		String message = NLS.bind(DocometreMessages.ImportExperimentNameDialogMessage, file.getName());
		InputDialog inputDialog = new InputDialog(getShell(), DocometreMessages.ImportExperimentNameDialogTitle, message, defaultName, ImportResourceWizard::validateExperimentName);
		if(inputDialog.open() != Window.OK) return null;
		return inputDialog.getValue();
	}

	/*
	 * Extract archive in destination folder. If rootFolder is not null, it is removed from entries names.
	 */
	private static void unzip(String zipFilePath, String destDir, String rootFolder, IProgressMonitor monitor) {
		File dir = new File(destDir);
		// create output directory if it doesn't exist
		if(!dir.exists()) dir.mkdirs();
		try (ZipFile zipFile = new ZipFile(new File(zipFilePath)); ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
			int nbFiles = zipFile.size();
			Path destinationPath = dir.toPath().toAbsolutePath().normalize();
			ZipEntry ze = zis.getNextEntry();
			int numFile = 1;
			while(ze != null){
				String message = NLS.bind(DocometreMessages.UnzippingFile, new Object[] {numFile, nbFiles, ze.getName()});
				monitor.subTask(message);
				String entryName = ze.getName().replace('\\', '/');
				if(rootFolder != null && entryName.startsWith(rootFolder + "/")) entryName = entryName.substring(rootFolder.length() + 1);
				Path filePath = destinationPath.resolve(entryName).normalize();
				// Entries must be extracted in destination folder
				if(!entryName.isEmpty() && filePath.startsWith(destinationPath)) extractFile(zis, ze, filePath.toString());
				//close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
				numFile++;
				monitor.worked(1);
			}
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private static void extractFile(ZipInputStream zis, ZipEntry ze, String fileName) {
		try {
			File newFile = new File(fileName);
			// create directories for sub directories in zip
			if(ze.isDirectory()) newFile.mkdirs();
			else {
				newFile.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
		        //buffer for read and write data to file
				byte[] buffer = new byte[1024];
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			
			
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		parentResource = ResourcesPlugin.getWorkspace().getRoot();
		if(!selection.isEmpty()) {
			IResource tempResource = (IResource) selection.getFirstElement();
			if(!(tempResource instanceof IContainer) || ResourceType.isProcessTest(tempResource)) parentResource = tempResource.getParent();
			else parentResource = (IContainer) tempResource;
		}
	}

	public static IResource getSelectedResource() {
		return parentResource;
	}
	
	

}
