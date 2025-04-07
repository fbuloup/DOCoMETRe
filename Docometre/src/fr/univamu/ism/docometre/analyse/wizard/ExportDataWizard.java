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
package fr.univamu.ism.docometre.analyse.wizard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class ExportDataWizard extends Wizard {
	
	protected static String SIGNAL_TYPE = "Signal";
	protected static String EVENT_TYPE = "Event";
	protected static String MARKER_TYPE = "Marker";
	protected static String FEATURE_TYPE = "Feature";
	
	private ExportDataWizardPage exportDataWizardPage;
	private boolean anErrorOccured = false;
	
	public ExportDataWizard() {
		setWindowTitle(DocometreMessages.ExportDataDialogTitle);
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		exportDataWizardPage = new ExportDataWizardPage();
		super.addPage(exportDataWizardPage);
	}

	@Override
	public boolean performFinish() {
		
		try {
			
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					
					String destination = exportDataWizardPage.getDestination();
					String exportType = exportDataWizardPage.getExportType();
					boolean singleFile = exportDataWizardPage.isSingleFile();
					String singleFileName = exportDataWizardPage.getSingleFileName();
					String separator = exportDataWizardPage.getSeparator();
					LinkedHashSet<Object> selection = exportDataWizardPage.getSelection();
					HashMap<IResource, FileWriter> dataFiles = new HashMap<>();
					FileWriter currentFileWriter = null;
					HashMap<FileWriter, String> fileWriterToFileName = new HashMap<>();
					HashMap<FileWriter, HashMap<Integer, String>> currentMFHashMap = new HashMap<>();
					if (singleFile) {
						try {
							String dataFileName = destination + File.separator + singleFileName;
							currentFileWriter = new FileWriter(dataFileName);
							fileWriterToFileName.put(currentFileWriter, dataFileName);
							if(MARKER_TYPE.equals(exportType) || FEATURE_TYPE.equals(exportType)) {
								HashMap<Integer, String> currentMFValuesHashMap = new HashMap<>();
								currentMFHashMap.put(currentFileWriter, currentMFValuesHashMap);
								if(MARKER_TYPE.equals(exportType)) currentFileWriter.write("Trial Number;Session;Signal Name;Marker Label;Time Value;Value\n");
								if(FEATURE_TYPE.equals(exportType)) currentFileWriter.write("Trial Number;Session;Signal Name;Feature Label;Value\n");
							}
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
							anErrorOccured = true;
						}
					}
					
					if(MARKER_TYPE.equals(exportType) || FEATURE_TYPE.equals(exportType)) {
						int work = selection.size()*100;
						work = singleFile ? work + 10 : work + selection.size()*10;
						monitor.beginTask(DocometreMessages.DataExportMainTaskTitle, work);
					}
					
					MathEngine mathEngine = MathEngineFactory.getMathEngine();
					for (Object object : selection) {
						if (object instanceof Channel) {
							Channel channel = (Channel) object;
							IResource subject = channel.getSubject();
							if (!singleFile) {
								try {
									// Create data file for this subject in destination folder
									FileWriter dataFile = dataFiles.get(subject);
									if(dataFile == null) {
										String dataFileName = subject.getProject().getName() + "." + subject.getName() + "." + exportType;
										dataFileName = destination + File.separator + dataFileName + ".txt";
										dataFile = new FileWriter(dataFileName);
										dataFiles.put(subject, dataFile);
										fileWriterToFileName.put(dataFile, dataFileName);
										HashMap<Integer, String> currentMFValuesHashMap = new HashMap<>();
										currentMFHashMap.put(dataFile, currentMFValuesHashMap);
										if(MARKER_TYPE.equals(exportType)) dataFile.write("Trial Number;Session;Signal Name;Marker Label;Time Value;Value\n");
										if(FEATURE_TYPE.equals(exportType)) dataFile.write("Trial Number;Session;Signal Name;Feature Label;Value\n");
									}
									currentFileWriter = dataFile;
								} catch (IOException e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
									anErrorOccured = true;
									continue;
								}
							}
							// Write data to current file writer
							try {
								if(SIGNAL_TYPE.equals(exportType)) {
									// Signal name;name
									// Sample frequency;value
									// Trial number;Category;values
									// 1 ; cat1 ; values...
									// 2 ; cat2 ; values...
									// 3 ; cat1 ; values...
									// 4 ; cat3 ; values...
									// ...
									if(!singleFile) currentFileWriter.write("Signal name" + separator + channel.getName() + "\n");
									else currentFileWriter.write("Signal name" + separator + channel.getFullName() + "\n");
									double sf = mathEngine.getSampleFrequency(channel);
									currentFileWriter.write("Sample frequency" + separator + String.valueOf(sf) + "\n");
									int nbTrials = mathEngine.getTrialsNumber(channel);
									currentFileWriter.write("Trial number" + separator + "Category" + separator + "Values" + "\n");
									double lastWorks = 0;
									for (int trialNumber = 0; trialNumber < nbTrials; trialNumber++) {
										String message = NLS.bind(DocometreMessages.DataExportSignalSubtaskMessage, new Object[] {channel.getName(), trialNumber, nbTrials});
										monitor.subTask(message);
										double[] values = mathEngine.getYValuesForSignal(channel, trialNumber + 1);
										String criteria = mathEngine.getCategoryForTrialNumber(channel, trialNumber + 1);
										String stringValues = Arrays.toString(values).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", separator);
										currentFileWriter.write(String.valueOf(trialNumber+1) + separator + criteria + separator + stringValues + "\n");
										double worked = 1.0*trialNumber/nbTrials*100 - lastWorks;
										if(worked >= 1) {
											monitor.worked(1);
											lastWorks++;
										}
									}
									
								}
								if(EVENT_TYPE.equals(exportType)) {
									// TODO
								}
								if(MARKER_TYPE.equals(exportType)) {
									// Trial Number; Session Name ;Channel Name; Marker Group Name; time; Value; Channel Name; Marker Group Name; time; Value; etc.
									HashMap<Integer, String> currentMarkersValuesHashMap = currentMFHashMap.get(currentFileWriter);
									double[][] markersValues = MathEngineFactory.getMathEngine().getMarkers(channel.getLabel(), channel.getParentChannel());
									int totalTrials = markersValues.length;
									int numTrial = 0;
									double lastWorks = 0;
									monitor.subTask(channel.getFullName());
									for (double[] values : markersValues) {
										int trialNumber = (int)values[0];
										numTrial++;
										String markersLine = currentMarkersValuesHashMap.get(trialNumber);
										if(markersLine == null) {
											String category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(channel.getParentChannel(), trialNumber);
											markersLine = "" + trialNumber + separator + category;
										}
										if(!singleFile) markersLine += separator + channel.getParentChannel().getName();
										else markersLine += separator + channel.getParentChannel().getFullName();
										markersLine += separator + channel.getLabel() + separator + values[1] + separator + values[2];
										currentMarkersValuesHashMap.put(trialNumber, markersLine);
										double worked = 1.0*numTrial/totalTrials*100 - lastWorks;
										if(worked >= 1) {
											monitor.worked((int) worked);
											lastWorks += worked;
										}
									}
								}
								if(FEATURE_TYPE.equals(exportType)) {
									// Trial Number; Session Name ;Channel Name; Feature Name; Value; Channel Name; Feature Name; Value; etc.
									HashMap<Integer, String> currentFeaturesValuesHashMap = currentMFHashMap.get(currentFileWriter);
									double[][] featuresValues = MathEngineFactory.getMathEngine().getFeature(channel.getLabel(), channel.getParentChannel());
									int totalTrials = featuresValues.length;
									int numTrial = 0;
									double lastWorks = 0;
									monitor.subTask(channel.getFullName());
									for (double[] value : featuresValues) {
										int trialNumber = numTrial + 1;
										numTrial++;
										String featuresLine = currentFeaturesValuesHashMap.get(trialNumber);
										if(featuresLine == null) {
											String category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(channel.getParentChannel(), trialNumber);
											featuresLine = "" + trialNumber + separator + category;
										}
										if(!singleFile) featuresLine += separator + channel.getParentChannel().getName();
										else featuresLine += separator + channel.getParentChannel().getFullName();
										if(value.length == 1) featuresLine += separator + channel.getLabel() + separator + value[0];
										else featuresLine += separator + channel.getLabel() + separator + Arrays.toString(value);
										currentFeaturesValuesHashMap.put(trialNumber, featuresLine);
										double worked = 1.0*numTrial/totalTrials*100 - lastWorks;
										if(worked >= 1) {
											monitor.worked((int) worked);
											lastWorks += worked;
										}
									}
								}
							} catch (IOException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
								anErrorOccured = true;
								continue;
							}
							

						}
						//monitor.worked(1);
						if (monitor.isCanceled()) break;
					}
					// if export markers or features, write data to file
					if(MARKER_TYPE.equals(exportType) || FEATURE_TYPE.equals(exportType)) {
						Set<FileWriter> fileWriters = currentMFHashMap.keySet();
						for (FileWriter fileWriter : fileWriters) {
							try {
								String fileName = fileWriterToFileName.get(fileWriter);
								monitor.subTask(fileName);
								HashMap<Integer, String> mfValuesHashMap = currentMFHashMap.get(fileWriter);
								Collection<String> mfValues = mfValuesHashMap.values();
								for (String mfValue : mfValues) {
									fileWriter.write(mfValue + "\n");
								}
								fileWriter.flush();
								monitor.worked(100);
							} catch (IOException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
								anErrorOccured = true;
							}
						}
					}
					
					// Close data files
					if(singleFile) {
						try {
							currentFileWriter.close();
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
							anErrorOccured = true;
						}
					} else {
						Set<IResource> keys = dataFiles.keySet();
						for (IResource subject : keys) {
							FileWriter dataFile = dataFiles.get(subject);
							try {
								dataFile.flush();
								dataFile.close();
							} catch (IOException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
								anErrorOccured = true;
							}
						}
					}
					
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		if(anErrorOccured) MessageDialog.openError(getShell(), DocometreMessages.DataExportErrorOccuredDialogTitle, DocometreMessages.DataExportErrorOccuredDialogMessage);
		return true;
	}

}
