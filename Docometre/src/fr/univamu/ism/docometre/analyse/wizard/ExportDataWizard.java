package fr.univamu.ism.docometre.analyse.wizard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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
					if (singleFile) {
						try {
							String dataFileName = destination + File.pathSeparator + singleFileName;
							currentFileWriter = new FileWriter(dataFileName);
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
							anErrorOccured = true;
						}
					}
					
					monitor.beginTask(DocometreMessages.DataExportMainTaskTitle, selection.size()*100);
					
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
									currentFileWriter.write("Signal name" + separator + channel.getName() + "\n");
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
										currentFileWriter.write(String.valueOf(trialNumber) + separator + criteria + separator + stringValues + "\n");
										double worked = 1.0*trialNumber/nbTrials*100 - lastWorks;
										if(worked >= 1) {
											monitor.worked(1);
											lastWorks++;
										}
									}
									
								}
								if(EVENT_TYPE.equals(exportType)) {
									
								}
								if(MARKER_TYPE.equals(exportType)) {
									
								}
								if(FEATURE_TYPE.equals(exportType)) {
									
								}
							} catch (IOException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
								anErrorOccured = true;
								continue;
							}
							

							Thread.sleep(100);
						}
						monitor.worked(1);
						if (monitor.isCanceled()) break;
					}
					// Close data files
					Set<IResource> keys = dataFiles.keySet();
					for (IResource subject : keys) {
						FileWriter dataFile = dataFiles.get(subject);
						try {
							dataFile.close();
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
							anErrorOccured = true;
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
