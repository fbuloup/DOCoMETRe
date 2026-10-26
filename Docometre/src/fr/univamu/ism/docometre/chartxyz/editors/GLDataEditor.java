package fr.univamu.ism.docometre.chartxyz.editors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.chartxyz.Chart1DRenderer;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.editors.DataEditorActionBarContributor.IDataEditorActionBarContribution;

public class GLDataEditor extends EditorPart implements PartNameRefresher/*, IPartListener2*/, IDataEditorActionBarContribution {
	
	public static String ID = "Docometre.GLDataEditor";
	
	private GLChart1D chart;
	private Curve1DData curveData;

	public GLDataEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		IFile dataFile = (IFile) ((ResourceEditorInput) getEditorInput()).getObject();
		loadData(dataFile);
//		site.getPage().addPartListener(this);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		if (curveData == null) {
			// No data (unknown sampling frequency and input cancelled, or read error): message instead of the chart
			Label label = new Label(parent, SWT.WRAP);
			label.setText("No data to display: the samples could not be loaded (see the error log).");
			refreshPartName();
			return;
		}
		chart = GLChart1D.createChart(parent, curveData.getWindowTimeWidth(), curveData);
		chart.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL); 
		ChartPartListener.install(this, chart);
		refreshPartName();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	private void loadData(IFile dataFile) {
		try {
			Path path = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + dataFile.getFullPath().toOSString());
			byte[] bytes = Files.readAllBytes(path);
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
			float[] values = new float[floatBuffer.capacity()];
			floatBuffer.get(values);
			
			// Get sample frequency
			boolean removeDACQHandle = false;
			IContainer container = dataFile.getParent();
			IResource processFile = null;
			DACQConfiguration dacqConfiguration = null; 
			if(ResourceType.isTrial(container)) {
				// If this container is a trial
				processFile = ResourceProperties.getAssociatedProcess(container);
				
			} else if(ResourceType.isProcessTest(container)) {
				// If this container is a process test, retrieve process file
				String processFilePath = ResourceProperties.getAssociatedProcessProperty(container);
				String rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				if(Platform.getOS().equals(Platform.OS_WIN32)) {
					processFilePath = processFilePath.replaceAll("\\\\", "/");
					rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString().replaceAll("\\\\", "/");
				}
				processFilePath = processFilePath.replaceFirst(rootLocation, "");
				processFile = ResourcesPlugin.getWorkspace().getRoot().findMember(processFilePath);
			}
			
			double sf = -1;
			String channelNameToFind = dataFile.getName().replaceAll(Activator.samplesFileExtension, "");
			
			if(processFile != null) {
				IResource dacqConfigurationFile = ResourceProperties.getAssociatedDACQConfiguration(processFile);
				Object object = ResourceProperties.getObjectSessionProperty(dacqConfigurationFile);
				if(object == null) {
					dacqConfiguration = (DACQConfiguration) ObjectsController.deserialize((IFile) dacqConfigurationFile);
					ResourceProperties.setObjectSessionProperty(dacqConfigurationFile, dacqConfiguration);
					ObjectsController.addHandle(dacqConfiguration);
					removeDACQHandle = true;
				} else dacqConfiguration = (DACQConfiguration) object;
				
				
				// Compute data file name to remove prefix and suffix
				IContainer session = dataFile.getParent().getParent();
				boolean usePrefix = ResourceProperties.getDataFilesNamesPrefix(session) == null ? false : true;
				if(usePrefix) channelNameToFind = channelNameToFind.split(ExperimentScheduler.dataFilePathNameSeparator_RegExpSplitter)[1];
				else channelNameToFind = channelNameToFind.split(ExperimentScheduler.dataFilePathNameSeparator_RegExpSplitter)[0];
				
				Channel[] channels = dacqConfiguration.getChannels();
				Channel channelFound = null;
				for (Channel channel : channels) {
					String channelName = channel.getProperty(ChannelProperties.NAME);
					if(channelName.equals(channelNameToFind)) {
						channelFound = channel;
						break;
					}
				}
				if(removeDACQHandle) ObjectsController.removeHandle(dacqConfiguration);
				if(channelFound != null) sf = Double.parseDouble(channelFound.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
			}
			
			String message;
			
			if(sf == -1) {
				message = NLS.bind(DocometreMessages.SampleFrequencyDialogMessage, channelNameToFind);
				Activator.logWarningMessage(message);
				sf = getSampleFrequencyDialog();
				if(sf > 0) {
					message = NLS.bind(DocometreMessages.SampleFrequencyDialogUsed, sf, channelNameToFind);
					Activator.logWarningMessage(message);
				}
			}
			
			if(sf == -1) {
				return;
			}
			
			curveData = new Curve1DData(dataFile.getFullPath().toOSString(), sf, values);
			
			message = NLS.bind(DocometreMessages.NumberSamplesReadMessage, values.length, channelNameToFind);
			Activator.logInfoMessage(message, getClass());
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		String partName = GetResourceLabelDelegate.getLabel((IResource) object);
		String toolTip = getEditorInput().getToolTipText();
		
		if(chart != null && chart.getCurvesCount() > 1) {
			partName += " (...)";
			toolTip = "";
			List<String> names = chart.getRenderer().getCurves().stream().map(Curve1DData::getName).collect(Collectors.toList());
			for (int i = 0; i < names.size(); i++) {
				if(i == chart.getCurvesCount() - 1) toolTip += names.get(i);
				else toolTip += names.get(i) + "\n";
			}
			((ResourceEditorInput)getEditorInput()).setTooltip(toolTip);
		}
		setPartName(partName);
		firePropertyChange(PROP_TITLE);
	}
	
	private double getSampleFrequencyDialog() {
		double sf = -1;
		IInputValidator inputValidator = new IInputValidator() {
			@Override
			public String isValid(String newText) {
				/*Check pattern resource name*/
				Pattern pattern = Pattern.compile("^[+]?([1-9][0-9]*(?:[\\.][0-9]*)?|0*\\.0*[1-9][0-9]*)(?:[eE][+-]?[0-9]+)?$");
				Matcher matcher = pattern.matcher(newText);
				if(!matcher.matches()) return DocometreMessages.SampleFrequencyErrorMessage;
				return null;
			}
		};
		InputDialog inputDialog = new InputDialog(getSite().getShell(), DocometreMessages.SampleFrequencyDialogTitle, DocometreMessages.SampleFrequencyDialogLabel, "1", inputValidator);
		if(inputDialog.open() == Dialog.OK) {
			sf = Double.parseDouble(inputDialog.getValue());
		}
		return sf;
	}
	
//	@Override
//	public void partActivated(IWorkbenchPartReference partRef) {
//		if(chart != null && partRef == getSite().getPage().getReference(this)) {
//			chart.startAnimator();
//		}
//	}
//	
//	@Override
//	public void partDeactivated(IWorkbenchPartReference partRef) {
//		if(chart != null && partRef == getSite().getPage().getReference(this)) {
//			chart.stopAnimator();
//		}
//	}
	
	@Override
	public void dispose() {
		if (chart != null) chart.close();
//		getSite().getPage().removePartListener(this);
		super.dispose();
	}

	@Override
	public void panLeft() {
		if (chart != null) chart.getRenderer().pan(-0.1f, 0);
	}

	@Override
	public void panRight() {
		if (chart != null) chart.getRenderer().pan(0.1f, 0);
	}

	@Override
	public void panUp() {
		if (chart != null) chart.getRenderer().pan(0, -0.1f);
	}

	@Override
	public void panDown() {
		if (chart != null) chart.getRenderer().pan(0, 0.1f);
	}

	@Override
	public void zoomIn() {
		if (chart != null) chart.getRenderer().zoom(1f);
	}

	@Override
	public void zoomOut() {
		if (chart != null) chart.getRenderer().zoom(-1f);
	}

	@Override
	public void reset() {
		if (chart != null) chart.getRenderer().resetView();
	}

}
