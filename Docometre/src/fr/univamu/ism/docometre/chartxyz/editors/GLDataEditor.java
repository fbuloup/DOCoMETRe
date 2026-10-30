package fr.univamu.ism.docometre.chartxyz.editors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.editors.DataEditorActionBarContributor.IDataEditorActionBarContribution;
import fr.univamu.ism.nswtchart.Window;

public class GLDataEditor extends EditorPart implements PartNameRefresher/*, IPartListener2*/, IDataEditorActionBarContribution {
	
	public static String ID = "Docometre.GLDataEditor";
	
	private GLChart1D chart;
	private Curve1DData curveData;
	private DropTarget dropTarget;
	// Displayed view {xMin, xMax, yMin, yMax} (time in s since the first sample) when it was changed by the user
	// (zoom window, wheel, pan), null for the initial view: saved with the editor (see getWindow())
	private volatile double[] userView;

	public GLDataEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		IFile dataFile = (IFile) ((ResourceEditorInput) getEditorInput()).getObject();
		curveData = loadData(dataFile);
//		site.getPage().addPartListener(this);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		// Main signal and signals previously dropped in this editor (restored editor)
		List<Curve1DData> curves = new ArrayList<>();
		if (curveData != null) curves.add(curveData);
		for (Object object : ((ResourceEditorInput) getEditorInput()).getOtherEditedObjects()) {
			if (!(object instanceof IFile)) continue;
			Curve1DData otherCurveData = loadData((IFile) object);
			if (otherCurveData != null && curves.stream().noneMatch(curve -> curve.getName().equals(otherCurveData.getName()))) curves.add(otherCurveData);
		}
		if (curves.isEmpty()) {
			// No data (unknown sampling frequency and input cancelled, or read error): message instead of the chart
			Label label = new Label(parent, SWT.WRAP);
			label.setText("No data to display: the samples could not be loaded (see the error log).");
			refreshPartName();
			return;
		}
		// The window is the longest signal duration: all the signals are displayed from t = 0
		double windowTimeWidth = curves.stream().mapToDouble(Curve1DData::getWindowTimeWidth).max().getAsDouble();
		chart = GLChart1D.createChart(parent, windowTimeWidth, curves.toArray(new Curve1DData[curves.size()]));
		chart.setMarkersInteractive(false);
		chart.setMarkersMenuItemsVisible(false);
		chart.setRealTime(false); // signals loaded from files: scroll mode, no sweep mode nor pause
		ChartPartListener.install(this, chart);
		// The view listener is called by the rendering thread: only the view is stored
		chart.addViewListener((view, automatic) -> userView = automatic ? null : view);
		// View saved when the editor was closed (restored editor)
		Window window = ((ResourceEditorInput) getEditorInput()).getWindow();
		if (window != null) applyView(new double[] { window.getXMin(), window.getXMax(), window.getYMin(), window.getYMax() });
		createDropTarget(parent);
		refreshPartName();
	}
	
	/*
	 * Displays the view {xMin, xMax, yMin, yMax}, abscissas in s since the first sample. In scroll mode,
	 * the abscissas of the zoom window are relative to the last sample of all the curves.
	 */
	private void applyView(double[] view) {
		double lastSampleTime = chart.getCurves().stream().mapToDouble(Curve1DData::getLastSampleTime).max().orElse(0);
		try {
			chart.setZoomWindow(view[0] - lastSampleTime, view[1] - lastSampleTime, view[2], view[3]);
			userView = view.clone(); // known before the next frame (editor closed at once)
		} catch (IllegalArgumentException e) {
			// Invalid saved view (empty or too small): initial view
			Activator.logWarningMessage("View not restored for " + getPartName() + " : " + e.getMessage());
		}
	}
	
	/**
	 * @return the view changed by the user (zoom, wheel, pan), abscissas in s since the first sample,
	 * or null for the initial view : saved with the editor and restored when it is opened again
	 */
	public Window getWindow() {
		double[] view = userView;
		return view == null ? null : new Window(view[0], view[1], view[2], view[3]);
	}
	
	/*
	 * Samples files dragged from the experiments view (full path as text) are added to the chart
	 */
	private void createDropTarget(Composite parent) {
		// Allow data to be copied to the drop target
		DropTarget target = new DropTarget(chart.getControl(), DND.DROP_COPY);
		dropTarget = target;
		// The JOGL canvas disposes itself again while it is being disposed: the drop target is released before it
		// (here if the widgets are disposed first, otherwise in dispose() before the chart is closed)
		parent.addDisposeListener(e -> disposeDropTarget());

		// Receive data in Text format
		final TextTransfer textTransfer = TextTransfer.getInstance();
		target.setTransfer(new Transfer[] { textTransfer });

		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_COPY) != 0) event.detail = DND.DROP_COPY;
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (!textTransfer.isSupportedType(event.currentDataType) || !(event.data instanceof String)) return;
				org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path((String) event.data);
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (ResourceType.isSamples(resource)) addTrace((IFile) resource);
			}
		});
	}
	
	/*
	 * Adds a samples file to the chart (drop), unless it is already displayed
	 */
	private void addTrace(IFile dataFile) {
		if (chart == null || chart.isDisposed()) return;
		if (chart.getCurve(dataFile.getFullPath().toOSString()) != null) return;
		Curve1DData newCurveData = loadData(dataFile);
		if (newCurveData == null) return;
		// A curve added to the chart takes the chart window: it is first enlarged if the new signal is longer, so that no value is lost
		if (newCurveData.getWindowTimeWidth() > chart.getWindowTimeWidth()) chart.setWindowTimeWidth(newCurveData.getWindowTimeWidth());
		double[] view = userView;
		chart.addCurve(newCurveData);
		// The zoom window follows the last sample: the view of the user is kept if the new signal is longer
		if (view != null) applyView(view);
		((ResourceEditorInput) getEditorInput()).addEditedObject(dataFile);
		refreshPartName();
		chart.getControl().setFocus();
	}
	
	/**
	 * Removes the curve of a samples file (the file is no longer edited, e.g. deleted)
	 */
	public void removeTrace(Object object) {
		if (!(object instanceof IFile) || chart == null || chart.isDisposed()) return;
		chart.removeCurve(((IFile) object).getFullPath().toOSString());
	}

	@Override
	public void setFocus() {
		// Eclipse 3.x contract: the focus must be given to a control of the editor. The workbench activation
		// follows the SWT focus: if the focus stays in the experiments view, the workbench and the focus disagree
		// on the active part, and a later page.activate() of this editor or another one may do nothing
		if (chart != null && !chart.isDisposed()) chart.getControl().setFocus();
	}
	
	private Curve1DData loadData(IFile dataFile) {
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
				return null;
			}
			
			Curve1DData newCurveData = new Curve1DData(dataFile.getFullPath().toOSString(), sf, values);
			
			message = NLS.bind(DocometreMessages.NumberSamplesReadMessage, values.length, channelNameToFind);
			Activator.logInfoMessage(message, getClass());
			
			return newCurveData;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
			return null;
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
	
	@Override
	public void dispose() {
		disposeDropTarget();
		if (chart != null) chart.close();
//		getSite().getPage().removePartListener(this);
		super.dispose();
	}

	private void disposeDropTarget() {
		if (dropTarget != null && !dropTarget.isDisposed()) dropTarget.dispose();
		dropTarget = null;
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
