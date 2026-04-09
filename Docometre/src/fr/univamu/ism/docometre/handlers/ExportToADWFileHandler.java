package fr.univamu.ism.docometre.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class ExportToADWFileHandler extends AbstractHandler implements ISelectionListener {
	
	protected IFolder[] subjects;
	
	private class ExportToADWJob extends Job {
		
		LinkedHashSet<String> channels = new LinkedHashSet<String>();
		LinkedHashSet<String> sessions = new LinkedHashSet<String>();
		LinkedHashSet<String> processes = new LinkedHashSet<String>();
		private int trialsNumber;
		private String outputFolder;
		private FileOutputStream inputFile;
		private FileChannel fileChannel;
		
		public ExportToADWJob(String name) {
			super(name);
		}
		
		private void gatherInformationsForHeader(IFolder subject) throws CoreException {
			// Get Channels number and names
			// Read through all Sessions
			IResource[] sessionsMembers = subject.members();
			for (IResource sessionsMember : sessionsMembers) {
				if(ResourceType.isSession(sessionsMember)) {
					// Read through all trials of this session
					IFolder session = (IFolder) sessionsMember;
					sessions.add(session.getName());
					String prefix = ResourceProperties.getDataFilesNamesPrefix(subject);
					prefix = prefix == null ? "" : prefix;
					IResource[] trialsMembers = session.members();
					for (IResource trialsMember : trialsMembers) {
						if(ResourceType.isTrial(trialsMember)) {
							IFolder trial = (IFolder) trialsMember;
							trialsNumber++;
							IResource process = ResourceProperties.getAssociatedProcess(trial);
							processes.add(process.getName().replaceAll(Activator.processFileExtension, ""));
							// Read through all data files of this session
							IResource[] dataFilesMembers = trial.members();
							for (IResource dataFilesMember : dataFilesMembers) {
								if(ResourceType.isDataFile(dataFilesMember)) {
									IFile dataFile = (IFile) dataFilesMember;
									String[] segments = dataFile.getName().split("\\.");
									String channelName = segments[0];
									if(!"".equals(prefix)) channelName = segments[1];
									channels.add(channelName);
								}
							}
						}
					}
				}
			}
		}
		
		private void writeChannels(FileChannel fileChannel) throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.asIntBuffer().put(channels.size());
			fileChannel.write(byteBuffer);
			byteBuffer.rewind();
			
			for (String channel : channels) {
				byteBuffer = ByteBuffer.allocate(channel.length());
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put(channel.getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
				byteBuffer = ByteBuffer.allocate(1);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put("|".getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
			}
		}
		
		private void writeSessions(FileChannel fileChannel) throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.asIntBuffer().put(sessions.size());
			fileChannel.write(byteBuffer);
			byteBuffer.rewind();
			
			for (String session : sessions) {
				byteBuffer = ByteBuffer.allocate(session.length());
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put(session.getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
				byteBuffer = ByteBuffer.allocate(1);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put("|".getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
			}
		}
		
		private void writeConditions(FileChannel fileChannel) throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.asIntBuffer().put(1);
			fileChannel.write(byteBuffer);
			byteBuffer.rewind();
			byteBuffer = ByteBuffer.allocate("Cond_Exp_1".length());
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.put("Cond_Exp_1".getBytes());
			byteBuffer.rewind();
			fileChannel.write(byteBuffer);
			byteBuffer = ByteBuffer.allocate(1);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.put("|".getBytes());
			byteBuffer.rewind();
			fileChannel.write(byteBuffer);
		}
		
		private void writeProcesses(FileChannel fileChannel) throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.asIntBuffer().put(processes.size());
			fileChannel.write(byteBuffer);
			byteBuffer.rewind();
			for (String process : processes) {
				byteBuffer = ByteBuffer.allocate(process.length());
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put(process.getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
				byteBuffer = ByteBuffer.allocate(1);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.put("|".getBytes());
				byteBuffer.rewind();
				fileChannel.write(byteBuffer);
			}
		}
		
		private void writeNumberOfTrials(FileChannel fileChannel) throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.asIntBuffer().put(trialsNumber);
			fileChannel.write(byteBuffer);
		}
		
		private void writeTrials(FileChannel fileChannel, IFolder subject) throws CoreException, IOException {
			
			ArrayList<String> channelsArrayList = new ArrayList<String>(channels);
			ArrayList<String> sessionsArrayList = new ArrayList<String>(sessions);
			ArrayList<String> processesArrayList = new ArrayList<String>(processes);
			
			IResource[] sessionsMembers = subject.members();
			for (IResource sessionsMember : sessionsMembers) {
				if(ResourceType.isSession(sessionsMember)) {
					IFolder session = (IFolder) sessionsMember;
					String prefix = ResourceProperties.getDataFilesNamesPrefix(subject);
					prefix = prefix == null ? "" : prefix;
					IResource[] trialsMembers = session.members();
					int nbTrialsInCurrentSession = 0;
					for (IResource trialsMember : trialsMembers) {
						if(ResourceType.isTrial(trialsMember)) nbTrialsInCurrentSession++;
					}
					
					int currentTrialNumber = 1;

					while (currentTrialNumber <= nbTrialsInCurrentSession) {
						for (IResource trialsMember : trialsMembers) {
							if(ResourceType.isTrial(trialsMember)) {
								IFolder trial = (IFolder) trialsMember;
								int trialNumber = Integer.parseInt(trial.getName().split("°")[1]);
								if(trialNumber != currentTrialNumber) continue;
								
								IResource process = ResourceProperties.getAssociatedProcess(trial);
								IResource dacqConfigurationFile = ResourceProperties.getAssociatedDACQConfiguration(process);
								DACQConfiguration dacqConfiguration = (DACQConfiguration) ObjectsController.deserialize((IFile) dacqConfigurationFile);
								ObjectsController.addHandle(dacqConfiguration);
								String processName = process.getName().replaceAll(Activator.processFileExtension, "");
								IResource[] dataFilesMembers = trial.members();
								int nbChannelsInTrial = 0;
								for (IResource dataFilesMember : dataFilesMembers) {
									if(ResourceType.isDataFile(dataFilesMember)) nbChannelsInTrial++;
								}
								// Write session number
								ByteBuffer byteBuffer = ByteBuffer.allocate(4);
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								byteBuffer.asIntBuffer().put(sessionsArrayList.indexOf(sessionsMember.getName()) + 1);
								fileChannel.write(byteBuffer);
								// Write condition number
								byteBuffer = ByteBuffer.allocate(4);
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								byteBuffer.asIntBuffer().put(1);
								fileChannel.write(byteBuffer);
								// Write process (seq. type) number
								byteBuffer = ByteBuffer.allocate(4);
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								byteBuffer.asIntBuffer().put(processesArrayList.indexOf(processName) + 1);
								fileChannel.write(byteBuffer);
								// Write total nb channels in trial
								byteBuffer = ByteBuffer.allocate(4);
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								byteBuffer.asIntBuffer().put(nbChannelsInTrial);
								fileChannel.write(byteBuffer);
								for (IResource dataFilesMember : dataFilesMembers) {
									if(ResourceType.isDataFile(dataFilesMember)) {
										IFile dataFile = (IFile) dataFilesMember;
										String[] segments = dataFile.getName().split("\\.");
										String channelName = segments[0];
										if(!"".equals(prefix)) channelName = segments[1];
										// Write channel number
										byteBuffer = ByteBuffer.allocate(4);
										byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
										byteBuffer.asIntBuffer().put(channelsArrayList.indexOf(channelName) + 1);
										fileChannel.write(byteBuffer);
										// Write sample freq.
										Channel channel = dacqConfiguration.getChannelFromName(channelName);
										String sfString =channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
										float sf = Float.parseFloat(sfString);
										byteBuffer = ByteBuffer.allocate(4);
										byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
										byteBuffer.asFloatBuffer().put(sf);
										fileChannel.write(byteBuffer);
										// Read data
										Path path = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + dataFile.getFullPath().toOSString());
										byte[] bytes = Files.readAllBytes(path);
										// Write samples number
										byteBuffer = ByteBuffer.allocate(4);
										byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
										int samplesNumber = bytes.length / 4;
										byteBuffer.asIntBuffer().put(samplesNumber);
										fileChannel.write(byteBuffer);
										// Write data
										byteBuffer = ByteBuffer.allocate(bytes.length);
										byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
										byteBuffer.put(bytes);
										byteBuffer.rewind();
										fileChannel.write(byteBuffer);
									}
								}
								ObjectsController.removeHandle(dacqConfiguration);
								currentTrialNumber++;
							}
						}
					}
				}
			}
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						DirectoryDialog directoryDialog = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
						outputFolder = directoryDialog.open();
					}
				});
				if(outputFolder == null) return Status.CANCEL_STATUS;
				monitor.beginTask(DocometreMessages.ExportToADWJob_Title2, subjects.length*7);
				for (IFolder subject : subjects) {
					monitor.subTask(subject.getName());
					
					boolean subjectExported = false;
					String fileName = outputFolder + File.separatorChar + subject.getName() + ".adw";
					
					Path path = Paths.get(fileName);
					if(Files.exists(path)) {
						String message = NLS.bind(DocometreMessages.ExportToADWJob_FileAlreadyExists, fileName);
						Activator.logErrorMessage(message);
						message = NLS.bind(DocometreMessages.ExportToADWJob_SubjectNotExported, subject.getName());
						Activator.logErrorMessage(message);
						monitor.worked(7);
						continue;
					}
						
					channels.clear();
					sessions.clear();
					processes.clear();
					trialsNumber = 0;
					
					gatherInformationsForHeader(subject);
					monitor.worked(1);
					try {
						
						inputFile = new FileOutputStream(fileName);
						fileChannel = inputFile.getChannel();
						
						writeChannels(fileChannel);
						monitor.worked(1);
						writeSessions(fileChannel);
						monitor.worked(1);
						writeConditions(fileChannel);
						monitor.worked(1);
						writeProcesses(fileChannel);
						monitor.worked(1);
						writeNumberOfTrials(fileChannel);
						monitor.worked(1);
						writeTrials(fileChannel, subject);
						monitor.worked(1);
						subjectExported = true;
						
					} catch (Exception e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
						subjectExported = false;
					} finally {
						try {
							inputFile.close();
							fileChannel.close();
						} catch (IOException e) {
							e.printStackTrace();
							Activator.logErrorMessageWithCause(e);
							subjectExported = false;
						} finally {
							if(subjectExported) {
								String message = NLS.bind(DocometreMessages.ExportToADWJob_SubjectExported, subject.getName(), fileName);
								Activator.logInfoMessage(message, getClass());
							}
						}
					}
										
					if(monitor.isCanceled()) throw new OperationCanceledException();
				}
			} catch (Exception e) {
				if(e instanceof OperationCanceledException) 
					return Status.CANCEL_STATUS;
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "ERROR", e);
			} 
			return Status.OK_STATUS;
		}
	}
	
	public ExportToADWFileHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(experimentsView != null) selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}

	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExportToADWJob exportToADWJob = new ExportToADWJob(DocometreMessages.ExportToADWJob_Title);
		exportToADWJob.setUser(true);
		exportToADWJob.setRule(subjects[0]);
		exportToADWJob.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return subjects != null && subjects.length > 0;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				subjects = new IFolder[0];
				for (int i = 0; i < elements.length; i++) {
					if(!(elements[i] instanceof IFolder)) return;
				}
				subjects = new IFolder[elements.length];
				for (int i = 0; i < elements.length; i++) {
					subjects[i] = (IFolder)elements[i];
				}
			}
		}
	}

}
