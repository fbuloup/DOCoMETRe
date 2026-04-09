package fr.univamu.ism.docometre.handlers;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class ExportToADWFileHandler extends AbstractHandler implements ISelectionListener {
	
	protected IFolder[] resources;
	
	private class ExportToADWJob extends Job {
		
		HashSet<String> channels = new HashSet<String>();
		HashSet<String> sessions = new HashSet<String>();
		HashSet<String> processes = new HashSet<String>();
		private int trialsNumber;
		private String outputFolder;
		
		public ExportToADWJob(String name) {
			super(name);
		}
		
		private void gatherInformationsForHeader(IFolder subject, IProgressMonitor monitor) throws CoreException {
			monitor.subTask(subject.getName());
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
							processes.add(process.getName());
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
				if(outputFolder == null) return Status.CANCEL_STATUS;;
				monitor.beginTask(DocometreMessages.ExportToADWJob_Title2, resources.length);
				for (IFolder subject : resources) {
					channels.clear();
					sessions.clear();
					processes.clear();
					trialsNumber = 0;
					
					gatherInformationsForHeader(subject, monitor);
					
					// Continue ...
					
					
					ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
					byteBuffer.order(ByteOrder.BIG_ENDIAN);
					byteBuffer.putInt(10);
					FileChannel fileChannel = FileChannel.open(Paths.get(outputFolder + File.separatorChar + subject.getName() + ".ADW"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					fileChannel.write(byteBuffer);

					// should not forget to close manually the FileChannel if we are not choosing the try-with-resource in Java 7.
					fileChannel.close();
					
					System.out.println(channels.size());
					System.out.println(sessions.size());
					System.out.println(processes.size());
					System.out.println(trialsNumber);
					monitor.worked(1);
					
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
		exportToADWJob.setRule(resources[0]);
		exportToADWJob.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return resources != null && resources.length > 0;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				resources = new IFolder[0];
				for (int i = 0; i < elements.length; i++) {
					if(!(elements[i] instanceof IFolder)) return;
				}
				resources = new IFolder[elements.length];
				for (int i = 0; i < elements.length; i++) {
					resources[i] = (IFolder)elements[i];
				}
			}
		}
	}

}
