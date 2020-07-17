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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class ImportResourceWizard extends Wizard implements IWorkbenchWizard {
	
	protected static IContainer parentResource;
	private ImportResourceWizardPage importResourceWizardPage;
	
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
		Object[] elements = selection.toArray();
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
						// Import experiment 
						getContainer().run(true, false, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
									String experimentName = file.getName().replaceAll(".zip$", "").replaceAll(".tar$", "");
									
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
									
									readAndApplyPersitentProperties(newExperiment, subMonitor, nbPropertiesEntriesToApply);
									IFile propertiesFile = newExperiment.getFile(newExperiment.getName() + ".properties");
									if(propertiesFile != null) propertiesFile.delete(true, null);
									
									subMonitor.subTask(DocometreMessages.AddingProjectToBuilderAndRefreshingExperiment);
									DocometreBuilder.addProject(newExperiment);
									ExperimentsView.refresh(newExperiment.getParent(), new IResource[]{newExperiment});
									
									subMonitor.done();
									monitor.done();
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
				} else {
					// Import process or dacq file
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
							System.out.println(object.getClass().getCanonicalName());
							if(file.getName().endsWith(Activator.daqFileExtension)) {
								ResourceProperties.setTypePersistentProperty(newFile, ResourceType.DACQ_CONFIGURATION.toString());
								if(object instanceof ADWinDACQConfiguration) ResourceProperties.setSystemPersistentProperty(newFile, Activator.ADWIN_SYSTEM);
								if(object instanceof ArduinoUnoDACQConfiguration) ResourceProperties.setSystemPersistentProperty(newFile, Activator.ARDUINO_UNO_SYSTEM);
							}
							if(file.getName().endsWith(Activator.processFileExtension)) {
								ResourceProperties.setTypePersistentProperty(newFile, ResourceType.PROCESS.toString());
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
				}
			 
			} else {
				Activator.logWarningMessage(NLS.bind(DocometreMessages.ImportResourceWizardErrorMessage3, file.getAbsolutePath()));
			}
		} 
		return true;
	}
	
	protected int getNbProperties(File file, String experimentName, String rootPath) {
		int nbPropertiesEntries = 0;
		try {
			ZipFile zipFile = new ZipFile(file);
			FileInputStream fis = new FileInputStream(file);
	        ZipInputStream zis = new ZipInputStream(fis);
	        ZipEntry ze = zis.getNextEntry();
	        while(ze != null) {
	        	if(ze.getName().equals(experimentName  + File.separator + experimentName + ".properties")) {
	        		extractFile(zis, ze, rootPath + File.separator + "temp.properties");
	        		FileInputStream fileInputStream = new FileInputStream(rootPath + File.separator + "temp.properties");
	        		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
	        		Properties properties = new Properties();
	        		properties.load(inputStreamReader);
	        		Set<Object> keys = properties.keySet();
	        		nbPropertiesEntries = keys.size();
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

	private void readAndApplyPersitentProperties(IProject newExperiment, SubMonitor subMonitor, int nbProperties) {
		try {
			String propertiesFileFullPath = newExperiment.getFile(newExperiment.getName() + ".properties").getLocation().toOSString();
			FileInputStream fileInputStream = new FileInputStream(propertiesFileFullPath);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			Properties properties = new Properties();
			properties.load(inputStreamReader);
			Set<Object> keys = properties.keySet();
			int numProperty = 1;
			for (Object key : keys) {
				String value = (String) properties.get(key);
				String[] keyArray = key.toString().split(ResourceProperties.SEPARATOR);
				if(Platform.getOS().equals(Platform.OS_MACOSX)) keyArray[0] = keyArray[0].replace("\\", "/");
				if(Platform.getOS().equals(Platform.OS_WIN32)) keyArray[0] = keyArray[0].replace("/", "\\");
				if(Platform.getOS().equals(Platform.OS_LINUX)) keyArray[0] = keyArray[0].replace("\\", "/");
				IPath resourcePath = org.eclipse.core.runtime.Path.fromOSString(keyArray[0]);
				String resourceFullPath = newExperiment.getParent().getLocation().toOSString() + File.separator + keyArray[0];
				File resourceFile = new File(resourceFullPath);
				boolean isFolder = resourceFile.isDirectory();
				boolean isExperiment = keyArray[0].equals(newExperiment.getFullPath().toOSString());
				IResource resource = null;
				if(isExperiment) 
					resource = newExperiment;
				else if(isFolder) 
					resource = ResourcesPlugin.getWorkspace().getRoot().getFolder(resourcePath);
				else 
					resource = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);

				String message = NLS.bind(DocometreMessages.ApplyingProperty, new Object[] {numProperty, nbProperties, value, resource.getFullPath().toOSString()});
				subMonitor.subTask(message);
				QualifiedName QN = new QualifiedName(Activator.PLUGIN_ID, keyArray[1]);
				resource.refreshLocal(IResource.DEPTH_INFINITE, null);
				resource.setPersistentProperty(QN , value);
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
	        //buffer for read and write data to file
			byte[] buffer = new byte[1024];
			File newFile = new File(fileName);
			// create directories for sub directories in zip
			new File(newFile.getParent()).mkdirs();
			FileOutputStream fos = new FileOutputStream(newFile);
			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
			fos.close();
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
			if(!(tempResource instanceof IContainer)) parentResource = tempResource.getParent();
			else parentResource = (IContainer) tempResource;
		}
	}

	public static IResource getSelectedResource() {
		return parentResource;
	}
	
	

}
