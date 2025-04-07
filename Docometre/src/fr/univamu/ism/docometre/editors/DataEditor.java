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
package fr.univamu.ism.docometre.editors;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.nswtchart.CursorMarkerListener;
import fr.univamu.ism.nswtchart.Window;
import fr.univamu.ism.nswtchart.XYSWTChart;
import fr.univamu.ism.nswtchart.XYSWTSerie;

public class DataEditor extends EditorPart implements PartNameRefresher, CursorMarkerListener {
	
	public static String ID = "Docometre.DataEditor";
	
//	private InteractiveChart chart;
	XYSWTChart chart;

	public DataEditor() {
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
//		IResource resource = ((IResource)((ResourceEditorInput)input).getObject());
//		setPartName(GetResourceLabelDelegate.getLabel(resource));
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
	public void dispose() {
		ApplicationActionBarAdvisor.cursorContributionItem.setVisible(false);
		ApplicationActionBarAdvisor.markerContributionItem.setVisible(false);
		ApplicationActionBarAdvisor.deltaContributionItem.setVisible(false);
		ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
		chart.dispose();
		super.dispose();
	}
	
	public Window getWindow() {
		return chart.getWindow();
	}

	public void setShowCursor(boolean value) {
		chart.setShowCursor(value);
	}
//	
//	public void setShowMarker(boolean value) {
//		chart.setShowMarker(value);
//	}
	
	@Override
	public void createPartControl(Composite parent) {
			
		// Get data from file
		IFile dataFile = (IFile) ((ResourceEditorInput) getEditorInput()).getObject();
		
		// Create XY graph
//		chart = new InteractiveChart(parent, SWT.BORDER);
		FontData fontData = parent.getFont().getFontData()[0];
		chart = new XYSWTChart(parent, SWT.DOUBLE_BUFFERED,fontData.getName(), fontData.getStyle(), fontData.getHeight());
		chart.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.addCursorMarkerListener(this);
		boolean showCursor = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_CURSOR);
//		boolean showMarker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_MARKER);
		chart.setShowCursor(showCursor);
//		chart.setShowMarker(showMarker);
//		chart.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
//		chart.getPlotArea().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
//		chart.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
//		IAxis[] axes = chart.getAxisSet().getAxes();
//		for (IAxis axe : axes) {
//			axe.getTick().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
//		}
		chart.setLegendPosition(SWT.BOTTOM);
//		chart.getLegend().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
//		chart.getLegend().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
//		chart.setSelectionRectangleColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
//		chart.getTitle().setVisible(false);
//		chart.getAxisSet().getXAxes()[0].getTitle().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
//		chart.getAxisSet().getXAxes()[0].getTitle().setText("Time (s)");
//		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
		
//		chart.getPlotArea().addMouseMoveListener(this);
//		chart.getPlotArea().addPaintListener(this);
//		chart.getPlotArea().addListener(SWT.MouseWheel, this);
//
//		chart.getPlotArea().addListener(SWT.Resize, this);
//		chart.getPlotArea().addMouseListener(this);
		
		
		
//		// Create trace and add it to graph
		createTrace(dataFile);
		
		Object[] objects = ((ResourceEditorInput) getEditorInput()).getOtherEditedObjects();
		for (Object object : objects) {
			createTrace((IFile) object);
		}
		
		Window window = ((ResourceEditorInput) getEditorInput()).getWindow();
		if(window != null) {
			chart.setWindow(window);
			chart.redraw();
			chart.update();
		}
//		
		// Allow data to be copied or moved to the drop target
		int operations = DND.DROP_COPY;
		DropTarget target = new DropTarget(chart, operations);

		// Receive data in Text or File format
		final TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] types = new Transfer[] { textTransfer };
		target.setTransfer(types);

		// Add drag/drop support
		target.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_COPY) != 0) event.detail = DND.DROP_COPY;
			}

			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			public void dragOperationChanged(DropTargetEvent event) {

			}

			public void dragLeave(DropTargetEvent event) {
			}

			public void dropAccept(DropTargetEvent event) {
			}

			public void drop(DropTargetEvent event) {
				if (textTransfer.isSupportedType(event.currentDataType)) {
					String text = (String) event.data;
					org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(text);
					IResource resource = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
					if (ResourceType.isSamples(resource)) {
						((ResourceEditorInput)getEditorInput()).addEditedObject(resource);
						createTrace((IFile) resource);
						setFocus();
					}
//					typedEventHandler(event);
				 }
			}
		});
		
	}
	
	public void removeTrace(Object object) {
		if(!(object instanceof IFile)) return;
		IFile dataFile = (IFile)object;
//		chart.getSeriesSet().deleteSeries(dataFile.getFullPath().toOSString());
//		chart.getPlotArea().redraw();
		chart.removeSerie(dataFile.getFullPath().toOSString());
	}
	
	private void createTrace(IFile dataFile) {
		try {
			Path path = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + dataFile.getFullPath().toOSString());
			byte[] bytes = Files.readAllBytes(path);
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
			float[] values = new float[floatBuffer.capacity()];
			floatBuffer.get(values);
			
			String message = NLS.bind(DocometreMessages.NumberSamplesReadMessage, values.length);
			Activator.logInfoMessage(message, getClass());
			
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
				try {
					String processFilePath = container.getPersistentProperty(ResourceProperties.ASSOCIATED_PROCESS_FILE_QN);
					String rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
					if(Platform.getOS().equals(Platform.OS_WIN32)) {
						processFilePath = processFilePath.replaceAll("\\\\", "/");
						rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString().replaceAll("\\\\", "/");
					}
					processFilePath = processFilePath.replaceFirst(rootLocation, "");
					processFile = ResourcesPlugin.getWorkspace().getRoot().findMember(processFilePath);
				} catch (CoreException e1) {
					Activator.logErrorMessageWithCause(e1);
					e1.printStackTrace();
				}
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
			
			if(sf == -1) {
				message = NLS.bind(DocometreMessages.SampleFrequencyDialogMessage, channelNameToFind);
				Activator.logWarningMessage(message);
				sf = getSampleFrequencyDialog();
			}
			
			HashMap<String, double[]> xyValues  = createXYDoubleValues(values, sf, dacqConfiguration);
			// Create X and Y data arrays
			double[] yDoubleValues = xyValues.get("Y");
			double[] xDoubleValues = xyValues.get("X");
			
//			ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, dataFile.getFullPath().toOSString());
//			lineSeries.setXSeries(xDoubleValues);
//			lineSeries.setYSeries(yDoubleValues);
//			lineSeries.setAntialias(SWT.ON);
//			lineSeries.setSymbolType(PlotSymbolType.NONE);
//			Byte index = getSeriesIndex(lineSeries);
//			lineSeries.setLineColor(DocometreApplication.getColor((byte) chart.getSeriesNumber()));
//			lineSeries.setLineWidth(3);
			
			chart.createSerie(xDoubleValues, yDoubleValues, dataFile.getFullPath().toOSString(), DocometreApplication.getColor((byte) chart.getSeriesNumber()), 1);
			
//			chart.getAxisSet().adjustRange();
			
			refreshPartName();
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}
	
	private double getSampleFrequencyDialog() {
		double sf = 1;
		IInputValidator inputValidator = new IInputValidator() {
			@Override
			public String isValid(String newText) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		InputDialog inputDialog = new InputDialog(getSite().getShell(), DocometreMessages.SampleFrequencyDialogTitle, DocometreMessages.SampleFrequencyDialogLabel, "1", inputValidator);
		if(inputDialog.open() == Dialog.OK) {
			sf = Double.parseDouble(inputDialog.getValue());
		}
		return sf;
	}
	
//	private Byte getSeriesIndex(ILineSeries series) {
//		ISeries[] seriesArray = chart.getSeriesSet().getSeries();
//		for (int i = 0; i < seriesArray.length; i++) {
//			if(series == seriesArray[i]) return (byte) i;
//		}
//		return 0;
//	}

	private HashMap<String, double[]> createXYDoubleValues(float[] values, double sf, DACQConfiguration dacqConfiguration) {
		HashMap<String, double[]> xyValues = new HashMap<>();
		double[] yDoubleValues = new double[values.length];
		double[] xDoubleValues = new double[values.length];
		for (int i = 0; i < yDoubleValues.length; i++) {
			yDoubleValues[i] = values[i];
			xDoubleValues[i] = 1.0 * i / sf;
		}
		xyValues.put("X", xDoubleValues);
		xyValues.put("Y", yDoubleValues);
		return xyValues;
	}

	@Override
	public void setFocus() {
//		((Control)chart.getPlotArea()).setFocus();
		chart.setFocus();
	}
	
	public XYSWTChart getChart() {
		return chart;
	}
	
	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		String partName = GetResourceLabelDelegate.getLabel((IResource) object);
		String toolTip = getEditorInput().getToolTipText();
		
		if(chart.getSeries().length > 1) {
			partName += " (...)";
			toolTip = "";
			XYSWTSerie[] series = chart.getSeries();
			for (int i = 0; i < series.length; i++) {
				if(i == series.length - 1) toolTip += series[i].getTitle();
				else toolTip += series[i].getTitle() + "\n";
				
			}
			((ResourceEditorInput)getEditorInput()).setTooltip(toolTip);
		}
		setPartName(partName);
		firePropertyChange(PROP_TITLE);
	}
	
	public void updateContribution() {
//		ApplicationActionBarAdvisor.cursorContributionItem.setText(chart.getCursorCoordinatesString());
//		ApplicationActionBarAdvisor.markerContributionItem.setText(chart.getMarkerCoordinatesString());
//		ApplicationActionBarAdvisor.deltaContributionItem.setText(chart.getDeltaCoordinateString());
//		
//
//		if(!ApplicationActionBarAdvisor.cursorContributionItem.isVisible()) {
//			ApplicationActionBarAdvisor.cursorContributionItem.setVisible(true);
//			ApplicationActionBarAdvisor.markerContributionItem.setVisible(true);
//			ApplicationActionBarAdvisor.deltaContributionItem.setVisible(true);
//			ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
//		}
//		
//		if(!ApplicationActionBarAdvisor.markerContributionItem.isVisible() && chart.isShowMarker()) {
//			ApplicationActionBarAdvisor.markerContributionItem.setVisible(true);
//			ApplicationActionBarAdvisor.deltaContributionItem.setVisible(true);
//			ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
//			
//		}
//		
//		if(ApplicationActionBarAdvisor.markerContributionItem.isVisible() && !chart.isShowMarker()) {
//			ApplicationActionBarAdvisor.markerContributionItem.setVisible(false);
//			ApplicationActionBarAdvisor.deltaContributionItem.setVisible(false);
//			ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
//			
//		}
	}

	@Override
	public void update(java.awt.geom.Point2D.Double cursor, java.awt.geom.Point2D.Double marker) {
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(6);
		StringBuilder text = new StringBuilder();
		
		text.append("Cursor (");
		text.append(nf.format(cursor.x));
		text.append(" ; ");
		text.append(nf.format(cursor.y));
		text.append(")");
		
		ApplicationActionBarAdvisor.cursorContributionItem.setText(text.toString());
		
		if(marker != null) {
			text = new StringBuilder();
			text.append("Marker (");
			text.append(nf.format(marker.x));
			text.append(" ; ");
			text.append(nf.format(marker.y));
			text.append(")");
			
			ApplicationActionBarAdvisor.markerContributionItem.setText(text.toString());
			
			double dx = cursor.x - marker.x;
			double dy = cursor.y - marker.y;
			text = new StringBuilder();
			text.append("\u0394 (");
			text.append(nf.format(dx));
			text.append(" ; ");
			text.append(nf.format(dy));
			text.append(")");
			
			ApplicationActionBarAdvisor.deltaContributionItem.setText(text.toString());
		}
		
	}

	@Override
	public void update(boolean showCursor) {
		ApplicationActionBarAdvisor.cursorContributionItem.setVisible(showCursor);
		ApplicationActionBarAdvisor.markerContributionItem.setVisible(showCursor);
		ApplicationActionBarAdvisor.deltaContributionItem.setVisible(showCursor);
		ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
		
	}

}
