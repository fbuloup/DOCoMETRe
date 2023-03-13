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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.signedcontent.InvalidContentException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.bytecode.opencsv.CSVReader;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.TrialStartMode;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
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
					try {
						// Import experiment or subjects
						getContainer().run(true, false, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
									if(resourceType.equals(ResourceType.EXPERIMENT)) {
										// Import experiment
										
										// Get experiment name
										String experimentName = file.getName().replaceAll(".zip$", "").replaceAll(".tar$", "");
										
										//Check if an experiment with same name already exists
										if(ResourcesPlugin.getWorkspace().getRoot().findMember(experimentName) != null) {
											getShell().getDisplay().syncExec(new Runnable() {
												@Override
												public void run() {
													String message = NLS.bind(DocometreMessages.ImportErrorMessage, experimentName);
													MessageDialog.openError(getShell(), DocometreMessages.ImportErrorDialogTitle, message);
												}
											});
											return;
										}
										
										int nbFilesToExtract = getNbFiles(file);
										int nbPropertiesEntriesToApply = getNbProperties(file, experimentName, rootPath);
										
										SubMonitor subMonitor = SubMonitor.convert(monitor, DocometreMessages.ImportingExperimentFromCompressedFile, nbFilesToExtract + nbPropertiesEntriesToApply + 2);

										subMonitor.subTask(DocometreMessages.CreatingNewExperimentInWorkspace);
										IProject newExperiment = ResourcesPlugin.getWorkspace().getRoot().getProject(experimentName);
										newExperiment.create(null);
										newExperiment.open(null);
										subMonitor.worked(1);
										
										unzip(file.getAbsolutePath(), rootPath, subMonitor);
										
										subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
										newExperiment.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
										subMonitor.worked(1);
										
										String propertiesFileFullPath = newExperiment.getFile(newExperiment.getName() + ".properties").getLocation().toOSString();
										
										readAndApplyPersitentProperties(newExperiment, subMonitor, nbPropertiesEntriesToApply, propertiesFileFullPath);
										IFile propertiesFile = newExperiment.getFile(newExperiment.getName() + ".properties");
										if(propertiesFile != null) propertiesFile.delete(true, null);
										
										subMonitor.subTask(DocometreMessages.RefreshingWorkspace);
										newExperiment.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
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
										unzip(file.getAbsolutePath(), temporaryFolder, subMonitor);
										
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
										String manualAuto = values.length > 0 ? TrialStartMode.getStartMode(values[0]).getKey():TrialStartMode.MANUAL.getKey();
										String process = values.length > 1 ? values[1]:null;
										if(process != null) process = process.endsWith(Activator.processFileExtension) ? process : process + Activator.processFileExtension;
										String trialName = DocometreMessages.NewResourceWizard_DefaultTrialName.replaceAll("N", "") + numTrial;
										IFolder newTrial = newSession.getFolder(new org.eclipse.core.runtime.Path(trialName));
										newTrial.create(true, true, null);
										ResourceProperties.setTypePersistentProperty(newTrial, ResourceType.TRIAL.toString());
										ResourceProperties.setTrialStartMode(newTrial, TrialStartMode.getStartMode(manualAuto));
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

	private void readAndApplyPersitentProperties(IProject newExperiment, SubMonitor subMonitor, int nbProperties, String propertiesFileFullPath) {
		try {
//			String propertiesFileFullPath = newExperiment.getFile(newExperiment.getName() + ".properties").getLocation().toOSString();
			FileInputStream fileInputStream = new FileInputStream(propertiesFileFullPath);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			Properties properties = new Properties();
			properties.load(inputStreamReader);
			Set<Object> keys = properties.keySet();
			int numProperty = 1;
			for (Object key : keys) {
				if(key.equals("OnlySubjects")) continue;
				String value = (String) properties.get(key);
				String[] keyArray = key.toString().split(ResourceProperties.SEPARATOR);
				if(Platform.getOS().equals(Platform.OS_MACOSX)) {
					keyArray[0] = keyArray[0].replace("\\", "/");
					value = value.replace("\\", "/");
					
				}
				if(Platform.getOS().equals(Platform.OS_WIN32)) {
					keyArray[0] = keyArray[0].replace("/", "\\");
					value = value.replace("/", "\\");
				}
				if(Platform.getOS().equals(Platform.OS_LINUX)) {
					keyArray[0] = keyArray[0].replace("\\", "/");
					value = value.replace("\\", "/");
				}
				keyArray[0] = keyArray[0].replaceAll("^/\\w+/", "/" + newExperiment.getName() + "/");
				value = value.replaceAll("^/\\w+/", "/" + newExperiment.getName() + "/");
				
				IPath resourcePath = org.eclipse.core.runtime.Path.fromOSString(keyArray[0]);
				String resourceFullPath = newExperiment.getParent().getLocation().toOSString() + File.separator + keyArray[0];
				File resourceFile = new File(resourceFullPath);
				boolean isFolder = resourceFile.isDirectory();
				boolean isExperiment = keyArray[0].equals(newExperiment.getFullPath().toOSString());
				IResource resource = null;
				if(isExperiment) 
					resource = newExperiment;
				else if(isFolder) resource = ResourcesPlugin.getWorkspace().getRoot().getFolder(resourcePath);
				else resource = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);

				String message = NLS.bind(DocometreMessages.ApplyingProperty, new Object[] {numProperty, nbProperties, value, resource.getFullPath().toOSString()});
				subMonitor.subTask(message);
				
				if(resource.exists()) {
					QualifiedName QN = new QualifiedName(Activator.PLUGIN_ID, keyArray[1]);
					resource.refreshLocal(IResource.DEPTH_INFINITE, null);
					resource.setPersistentProperty(QN , value);
					resource.refreshLocal(IResource.DEPTH_INFINITE, null);
				}

				subMonitor.worked(1);
				numProperty++;
			}
			inputStreamReader.close();
		} catch (CoreException | IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	private static void unzip(String zipFilePath, String destDir, IProgressMonitor monitor) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        try {
        	ZipFile zipFile = new ZipFile(new File(zipFilePath));
        	int nbFiles = zipFile.size();
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            int numFile = 1;
            while(ze != null){
            	String message = NLS.bind(DocometreMessages.UnzippingFile, new Object[] {numFile, nbFiles, ze.getName()});
				monitor.subTask(message);
                String fileName = destDir + File.separator + ze.getName();
                
                extractFile(zis, ze, fileName);
                
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
                numFile++;
                monitor.worked(1);
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
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
