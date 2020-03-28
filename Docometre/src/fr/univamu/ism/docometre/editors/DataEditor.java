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
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;
import org.eclipse.swtchart.extensions.charts.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;

public class DataEditor extends EditorPart implements PartNameRefresher, MouseMoveListener, PaintListener, Listener, MouseListener {
	
	public static String ID = "Docometre.DataEditor";
	
	private static Color RED_COLOR = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
	
	private InteractiveChart chart;
	private int currentX;
	private int currentY;
	private ILineSeries currentSeries;

	private int currentXMarker = -1;
	private int currentYMarker = -1;
	
	private boolean doubleClick;
	
	
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
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
			
		// Get data from file
		IFile dataFile = (IFile) ((ResourceEditorInput) getEditorInput()).getObject();
		
		// Create XY graph
		chart = new InteractiveChart(parent, SWT.BORDER);
		chart.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.getPlotArea().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		IAxis[] axes = chart.getAxisSet().getAxes();
		for (IAxis axe : axes) {
			axe.getTick().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		}
		chart.getLegend().setPosition(SWT.BOTTOM);
		chart.getLegend().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.getLegend().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		chart.setSelectionRectangelColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getXAxes()[0].getTitle().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		chart.getAxisSet().getXAxes()[0].getTitle().setText("Time (s)");
		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
		
		chart.getPlotArea().setCursor(new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_CROSS));

		chart.getPlotArea().addMouseMoveListener(this);
		chart.getPlotArea().addPaintListener(this);
		chart.getPlotArea().addListener(SWT.MouseWheel, this);
		chart.getPlotArea().addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(event.keyCode == SWT.TAB) {
					
					if(chart.getSeriesSet().getSeries().length > 1 ) {
						ISeries[] series = chart.getSeriesSet().getSeries();
						for (int i = 0; i < series.length; i++) {
							if(currentSeries == series[i]) {
								int index = (i + 1) % series.length;
								currentSeries = (ILineSeries) series[index];
								break;
							}
						}
						mouseEventHandler(event);
					} 
				} else mouseEventHandler(event);
			}
		});
		MenuItem menuItem = chart.getMenuItem(null, Messages.ADJUST_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ADJUST_X_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ADJUST_Y_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMIN);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMIN_X);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMIN_Y);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMOUT);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMOUT_X);
		menuItem.addListener(SWT.Selection, this);
		menuItem = chart.getMenuItem(null, Messages.ZOOMOUT_Y);
		menuItem.addListener(SWT.Selection, this);
		chart.getPlotArea().addListener(SWT.Resize, this);
		chart.getPlotArea().addMouseListener(this);
		
		// Create trace and add it to graph
		createTrace(dataFile);
		
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
					typedEventHandler(event);
				 }
			}
		});
			
		
	}
	
	public void removeTrace(Object object) {
		if(!(object instanceof IFile)) return;
		IFile dataFile = (IFile)object;
		chart.getSeriesSet().deleteSeries(dataFile.getFullPath().toOSString());
		chart.getPlotArea().redraw();
	}
	
	private void createTrace(IFile dataFile) {
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
			if(processFile == null) {
				Activator.logErrorMessage("Unabled to find associated process file !");
				return;
			}
			IResource dacqConfigurationFile = ResourceProperties.getAssociatedDACQConfiguration(processFile);
			Object object = ResourceProperties.getObjectSessionProperty(dacqConfigurationFile);
			if(object == null) {
				dacqConfiguration = (DACQConfiguration) ObjectsController.deserialize((IFile) dacqConfigurationFile);
				ResourceProperties.setObjectSessionProperty(dacqConfigurationFile, dacqConfiguration);
				ObjectsController.addHandle(dacqConfiguration);
				removeDACQHandle = true;
			} else dacqConfiguration = (DACQConfiguration) object;
			String channelNameToFind = dataFile.getName().replaceAll(Activator.samplesFileExtension, "");
			
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
			if(channelFound == null) {
				Activator.logErrorMessage("Unabled to find channel : " + channelNameToFind);
				return;
			}
			
			double sf = Double.parseDouble(channelFound.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
			
			HashMap<String, double[]> xyValues  = createXYDoubleValues(values, sf, dacqConfiguration);
			// Create X and Y data arrays
			double[] yDoubleValues = xyValues.get("Y");
			double[] xDoubleValues = xyValues.get("X");
			
			ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, dataFile.getFullPath().toOSString());
			lineSeries.setXSeries(xDoubleValues);
			lineSeries.setYSeries(yDoubleValues);
			lineSeries.setAntialias(SWT.ON);
			lineSeries.setSymbolType(PlotSymbolType.NONE);
			lineSeries.setLineColor(ColorUtil.getColor());
			lineSeries.setLineWidth(3);
			
			currentSeries = lineSeries;
			
			chart.getAxisSet().adjustRange();
			
			refreshPartName();
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	private HashMap<String, double[]> createXYDoubleValues(float[] values, double sf, DACQConfiguration dacqConfiguration) {
		HashMap<String, double[]> xyValues = new HashMap<>();
		if(dacqConfiguration instanceof ADWinDACQConfiguration) {
			double[] yDoubleValues = new double[values.length];
			double[] xDoubleValues = new double[values.length];
			for (int i = 0; i < yDoubleValues.length; i++) {
				yDoubleValues[i] = values[i];
				xDoubleValues[i] = 1.0 * i / sf;
			}
			xyValues.put("X", xDoubleValues);
			xyValues.put("Y", yDoubleValues);
		}
		if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) {
			double[] yDoubleValues = new double[values.length / 2];
			double[] xDoubleValues = new double[values.length / 2];
			for (int i = 0; i < yDoubleValues.length; i++) {
				yDoubleValues[i] = values[2*i+1];
				xDoubleValues[i] = values[2*i] + (i == 0 ? 0 : xDoubleValues[i-1]);
			}
			xyValues.put("X", xDoubleValues);
			xyValues.put("Y", yDoubleValues);
		}
		return xyValues;
	}

	@Override
	public void setFocus() {
		((Control)chart.getPlotArea()).setFocus();
	}
	
	public InteractiveChart getChart() {
		return chart;
	}
	
	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		String partName = GetResourceLabelDelegate.getLabel((IResource) object);
		String toolTip = getEditorInput().getToolTipText();
		
		if(chart.getSeriesSet().getSeries().length > 1) {
			partName += " (...)";
			toolTip = "";
			ISeries[] series = chart.getSeriesSet().getSeries();
			for (int i = 0; i < series.length; i++) {
				if(i == series.length - 1) toolTip += series[i].getId();
				else toolTip += series[i].getId() + "\n";
				
			}
			((ResourceEditorInput)getEditorInput()).setTooltip(toolTip);
		}
		setPartName(partName);
		firePropertyChange(PROP_TITLE);
	}

	@Override
	public void mouseMove(MouseEvent event) {
		int previousCurrentX = currentX;
		currentX = event.x;
		double x = chart.getAxisSet().getXAxes()[0].getDataCoordinate(event.x);
		int index = Arrays.binarySearch(currentSeries.getXSeries(), x);
		double y = Double.NaN;
		if(index < 0) {
			index = - index - 1;
			if(index > 0 && index < currentSeries.getXSeries().length) {
				double y1  = currentSeries.getYSeries()[index - 1];
				double y2  = currentSeries.getYSeries()[index];
				double x1  = currentSeries.getXSeries()[index - 1];
				double x2  = currentSeries.getXSeries()[index];
				y = (y2 -y1)/(x2 - x1)*(x - x1) + y1; 
			}
		} else {
			if(index >= 0 && index < currentSeries.getXSeries().length) {
				y = currentSeries.getYSeries()[index];
			}
		}
		currentY = chart.getAxisSet().getYAxes()[0].getPixelCoordinate(y);
		
//		chart.getPlotArea().redraw();
		int min = Math.min(previousCurrentX, currentX);
		int max = Math.max(previousCurrentX, currentX);
		int width = max - min + 20;
		chart.getPlotArea().redraw(min - 10, 0, width,chart.getPlotArea().getBounds().height, false);
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(6);
		
		StringBuilder text = new StringBuilder();
		text.append("Cursor (");
		text.append(nf.format(x));
		text.append(" ; ");
		text.append(nf.format(y));
		text.append(")");
		ApplicationActionBarAdvisor.cursorContributionItem.setText(text.toString());
		
		if(currentXMarker != -1) {
			double mx = chart.getAxisSet().getXAxes()[0].getDataCoordinate(currentXMarker);
			double my = chart.getAxisSet().getYAxes()[0].getDataCoordinate(currentYMarker);
			text = new StringBuilder();
			text.append("Marker (");
			text.append(nf.format(mx));
			text.append(" ; ");
			text.append(nf.format(my));
			text.append(")");
			ApplicationActionBarAdvisor.markerContributionItem.setText(text.toString());
			x = x - mx;
			y = y - my;
			text = new StringBuilder();
			text.append("\u0394 (");
			text.append(nf.format(x));
			text.append(" ; ");
			text.append(nf.format(y));
			text.append(")");
			ApplicationActionBarAdvisor.deltaContributionItem.setText(text.toString());
			
			if(!ApplicationActionBarAdvisor.markerContributionItem.isVisible()) {
				ApplicationActionBarAdvisor.markerContributionItem.setVisible(true);
				ApplicationActionBarAdvisor.deltaContributionItem.setVisible(true);
				ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
			}
			
		}
		
		if(!ApplicationActionBarAdvisor.cursorContributionItem.isVisible()) {
			ApplicationActionBarAdvisor.cursorContributionItem.setVisible(true);
			ApplicationActionBarAdvisor.markerContributionItem.setVisible(true);
			ApplicationActionBarAdvisor.deltaContributionItem.setVisible(true);
			ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
		}
		
	}

	@Override
	public void paintControl(PaintEvent e) {
		Color oldColor = e.gc.getForeground();			
		
		// Draw cursor
		e.gc.setForeground(RED_COLOR);
		e.gc.setLineWidth(3);
		e.gc.drawLine(currentX, 0, currentX, currentY - 3);
		e.gc.drawLine(currentX, currentY + 3, currentX, chart.getPlotArea().getBounds().height);
		e.gc.drawRectangle(currentX - 3, currentY - 3, 6, 6);
		// Draw marker
		if(currentXMarker != -1) {
//			e.gc.drawLine(currentXMarker, 0, currentXMarker, chart.getPlotArea().getBounds().height);
			
			e.gc.drawLine(currentXMarker, 0, currentXMarker, currentYMarker - 3);
			e.gc.drawLine(currentXMarker, currentYMarker + 3, currentXMarker, chart.getPlotArea().getBounds().height);
			e.gc.drawRectangle(currentXMarker - 3, currentYMarker - 3, 6, 6);
			
			
		}
		
		
		e.gc.setForeground(oldColor);
	}
	
	public int getCurrentX() {
		return currentX;
	}

	@Override
	public void handleEvent(Event event) {
		mouseEventHandler(event);
	}
	
	private void mouseEventHandler(Event event) {
		if(!doubleClick) {
			currentXMarker = -1;
			ApplicationActionBarAdvisor.markerContributionItem.setVisible(false);
			ApplicationActionBarAdvisor.deltaContributionItem.setVisible(false);
			ApplicationActionBarAdvisor.cursorContributionItem.getParent().update(true);
		}
		doubleClick = false;
		event.x = currentX;
		MouseEvent mouseEvent = new MouseEvent(event);
		mouseMove(mouseEvent);
	}
	
	private void typedEventHandler(TypedEvent typedEvent) {
		Event e = new Event();
		e.widget = typedEvent.widget;
		e.type = SWT.Selection;
		mouseEventHandler(e);
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		currentXMarker = currentX;
		currentYMarker = currentY;
		doubleClick = true;
	}

	@Override
	public void mouseDown(MouseEvent e) {
	}

	@Override
	public void mouseUp(MouseEvent e) {
		typedEventHandler(e);
	}

	

}
