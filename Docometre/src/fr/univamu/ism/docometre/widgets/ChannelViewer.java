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
package fr.univamu.ism.docometre.widgets;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.calibration.AbstractCalibrateMonitorDialog;
import fr.univamu.ism.docometre.calibration.CalibrationFactory;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.rtswtchart.IRTSWTOscilloChart;
import fr.univamu.ism.rtswtchart.IRTSWTSerie;
import fr.univamu.ism.rtswtchart.RTSWTChartsFactory;

/**
 * Viewer of a channel in the calibration and monitoring dialog : a {@link CollapsibleGroup} whose header 
 * bar contains, depending on the type of the channel :
 * <ul>
 * <li>analog input : current value in the title, "calibrated values" check box ; 
 * calibration fields (Amax, Amin, Umax, Umin) in the body</li>
 * <li>digital input : current value in the title</li>
 * <li>analog output : spinner of the value</li>
 * <li>digital output : ON/OFF switch</li>
 * </ul>
 * The graph of the channel is created in the body when the group is expanded and disposed when it is collapsed.
 */
public class ChannelViewer extends CollapsibleGroup {
	
	// Duration of the time window of the graph, in seconds
	private static final double WINDOW_TIME_WIDTH = 10;
	// Sampling frequency of the curves of the "SWT + OpenGL v2" library (Hz). The calibration/monitoring
	// processes deliver samples at an irregular rate, with their time : they are resampled at this frequency
	// (sample and hold), because the time of a Curve1DData is derived from its sample index.
	private static final double OPENGL_V2_SAMPLE_FREQUENCY = 100;
	// Maximum number of samples added at once (time window), e.g. after a long gap between two samples 
	private static final int OPENGL_V2_MAX_SAMPLES = (int)(WINDOW_TIME_WIDTH * OPENGL_V2_SAMPLE_FREQUENCY) + 1;
	// Displayed value of an input until the first sample is received
	private static final String UNKNOWN_VALUE = "?????";
	
	/**
	 * Time origin common to the graphs of the "SWT + OpenGL v2" library of several viewers (see 
	 * {@link ChannelViewer#setTimeBase(TimeBase)}) : set by the first sample received after a reset.
	 */
	public static final class TimeBase {
		private double origin = Double.NaN;
		/**
		 * @return time relative to the origin, which is set to time if there is none (first sample after a reset)
		 */
		public synchronized double getTime(double time) {
			if(Double.isNaN(origin)) origin = time;
			return time - origin;
		}
		/**
		 * The next sample received will be the new origin
		 */
		public synchronized void reset() {
			origin = Double.NaN;
		}
	}
	
	private boolean traceVisible ;
	private String title;
	private DecimalFormat decimalFormater;
	private IRTSWTSerie rtswtOscilloSerie;
	private IRTSWTOscilloChart rtswtOscilloChart; 
	// Container of the graph, disposed with it
	private Composite graphContainer;
	// Number of samples already added to the curve of the "SWT + OpenGL v2" library 
	private long openGLV2SamplesCount;
	// Time origin of the graph of the "SWT + OpenGL v2" library, own one by default
	private TimeBase timeBase = new TimeBase();
	// Called each time the graph is shown or hidden
	private Runnable graphVisibilityListener;
	private boolean firstSample = true;
	private double initialTime;
	private Image imageOn;
	private Image imageOff;
	private boolean input;
	private boolean analog;
	private Button showCalibratedValuesButton;
	private Channel channel;
	private double yComputed;
	
	// When analog output
	private SpinnerValue spinnerValue;
	// When digital output
	private boolean stateOn;
	private Label stateLabel;
	// When analog or digital input : last displayed value
	private volatile double inputValue = Double.NaN;
	
	public Object getValue() {
		if(analog && !input) return spinnerValue.getValue();
		if(!analog && !input) return stateOn;
		return inputValue;
	}
	
	public ChannelViewer(Composite parent, int style, Channel channel, boolean input, boolean analog) {
		super(parent, style);
		this.channel = channel;
		this.title = channel.getProperty(ChannelProperties.NAME) + " (" + channel.getProperty(ChannelProperties.CHANNEL_NUMBER) + ")";
		this.analog = analog;
		this.input = input;
		if(analog) decimalFormater = new DecimalFormat("#.####");
		else decimalFormater = new DecimalFormat("#");
		
		// Title : "name (number)" for outputs, "name (number) - value" for inputs
		setText(input ? title + " - " + UNKNOWN_VALUE : title);
		setToggleToolTipText(DocometreMessages.ShowHideGraph);
		
		Composite toolBar = getToolBar();
		if(!this.input) {
			if(!this.analog) {
				// Digital output : ON/OFF switch
				imageOn = Activator.getImage(IImageKeys.ON_ICON);
				imageOff = Activator.getImage(IImageKeys.OFF_ICON);
				stateLabel = new Label(toolBar, SWT.NONE);
				stateLabel.setImage(imageOff);
				stateLabel.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
				stateLabel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if(stateOn) stateLabel.setImage(imageOff);
						else stateLabel.setImage(imageOn);
						stateOn = !stateOn;
					}
				});
			} else {
				// Analog output : spinner, wide enough for the values from -99.9999 to 99.9999
				spinnerValue = new SpinnerValue(toolBar, SWT.BORDER);
				spinnerValue.setValue(-10);
				Point size = spinnerValue.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				spinnerValue.setValue(0);
				spinnerValue.setLayoutData(new RowData(size.x + 10, SWT.DEFAULT));
			}
		} else if(this.analog) {
			// Analog input : "calibrated values" check box in the header bar...
			showCalibratedValuesButton = new Button(toolBar, SWT.CHECK);
			showCalibratedValuesButton.setText(DocometreMessages.CalibratedValues);
			showCalibratedValuesButton.setToolTipText(DocometreMessages.ShowCalibratedValues);
			// ... and calibration fields in the body
			Composite calibrationComposite = new Composite(getBody(), SWT.NONE);
			calibrationComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			CalibrationHeader calibrationHeader = CalibrationFactory.createHeader(calibrationComposite, channel);
			calibrationHeader.addAMaxListener(AbstractCalibrateMonitorDialog.getGlobalCalibrationListener());
			calibrationHeader.addAMinListener(AbstractCalibrateMonitorDialog.getGlobalCalibrationListener());
			calibrationHeader.addUMaxListener(AbstractCalibrateMonitorDialog.getGlobalCalibrationListener());
			calibrationHeader.addUMinListener(AbstractCalibrateMonitorDialog.getGlobalCalibrationListener());
			// One column for each widget of the calibration header
			GridLayout gl = new GridLayout(Math.max(1, calibrationComposite.getChildren().length), false);
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			gl.horizontalSpacing = 8;
			calibrationComposite.setLayout(gl);
		}
		
		// Graph created when the group is expanded, disposed when collapsed
		addExpandListener(new ExpandListener() {
			@Override
			public void itemExpanded(ExpandEvent e) {
				firstSample = true;
				createGraph();
				graphVisibilityChanged(true);
			}
			@Override
			public void itemCollapsed(ExpandEvent e) {
				disposeGraph();
				graphVisibilityChanged(false);
			}
		});
	}
	
	/*
	 * Resizes the content of the scrolled composite containing the viewers, then notifies the graph visibility listener
	 */
	private void graphVisibilityChanged(boolean visible) {
		// The body is laid out when the viewer is resized (with its new height : the chart must not be laid out with a null height)
		Composite sc = getParent().getParent();
		sc.layout(true);
		getParent().setSize(getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
		int width = getParent().getSize().y < sc.getSize().y ? sc.getSize().x : sc.getClientArea().width;
		getParent().setSize(getParent().computeSize(width, SWT.DEFAULT));
		traceVisible = visible;
		if(graphVisibilityListener != null) graphVisibilityListener.run();
	}
	
	/**
	 * Time origin of the graph of the "SWT + OpenGL v2" library, to be shared by the viewers 
	 * whose graphs must have the same time axis
	 */
	public void setTimeBase(TimeBase timeBase) {
		this.timeBase = timeBase == null ? new TimeBase() : timeBase;
	}
	
	/**
	 * Listener called each time the graph is shown or hidden (UI thread)
	 */
	public void setGraphVisibilityListener(Runnable graphVisibilityListener) {
		this.graphVisibilityListener = graphVisibilityListener;
	}
	
	/**
	 * Clears the curve of the graph of the "SWT + OpenGL v2" library : it restarts at time 0 of its time base
	 * (to be reset too). Nothing to do with the other libraries : their graphs are reset when they are resized,
	 * when a graph is shown or hidden.
	 */
	public void resetGraph() {
		if(rtswtOscilloSerie instanceof Curve1DData) {
			((Curve1DData) rtswtOscilloSerie).clear();
			openGLV2SamplesCount = 0;
		}
	}
	
	public void setInitialDigitalOutputStates(int dioValue) {
		if(analog || input) return;
		int channelNumber = Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER)) - 1;
		stateOn = (dioValue & (int)Math.pow(2, channelNumber)) > 0;
		if(stateOn) stateLabel.setImage(imageOn);
		else stateLabel.setImage(imageOff);
	}
	
	public void setInitialAnalogState(double value) {
		spinnerValue.setValue(value);
	}
	
	/**
	 * Range of the values of an analog output (-10 to 10 by default)
	 */
	public void setOutputRange(double minValue, double maxValue) {
		if(spinnerValue == null) return;
		spinnerValue.setMinValue(minValue);
		spinnerValue.setMaxValue(maxValue);
	}
	
	private void createGraph() {
		graphContainer = new Composite(getBody(), SWT.NONE);
		graphContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		graphContainer.setLayout(new GridLayout());
		GridLayout gl = (GridLayout)graphContainer.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		
		if(Activator.useOpenGLV2()) createOpenGLV2Graph(graphContainer);
		else createPureSWTGraph(graphContainer);
		
		GridData gd = rtswtOscilloChart.getLayoutData();
		gd.heightHint = 150;
	}
	
	/*
	 * Graph of the "Pure SWT" library (also used with "SWT + OpenGL v1" library)
	 */
	private void createPureSWTGraph(Composite container) {
		rtswtOscilloChart = RTSWTChartsFactory.createPureSWTOscilloChart(container, SWT.DOUBLE_BUFFERED, container.getFont().getFontData()[0].getName(), SWT.BOLD, 10);
		rtswtOscilloChart.setGridLinesColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		rtswtOscilloChart.setFontColor(container.getBackground());
		rtswtOscilloChart.setLegendVisibility(false);
		rtswtOscilloChart.setAutoScale(true);
		rtswtOscilloChart.setWindowTimeWidth(WINDOW_TIME_WIDTH);
		rtswtOscilloChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rtswtOscilloSerie = rtswtOscilloChart.createSerie(title, Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
	}
	
	/*
	 * Graph of the "SWT + OpenGL v2" library (package fr.univamu.ism.docometre.chartxyz) : 
	 * SWT GLCanvas on Windows and Linux, NEWT on macOS, with a dynamic vertical scale
	 */
	private void createOpenGLV2Graph(Composite container) {
		Curve1DData curve = new Curve1DData(title, OPENGL_V2_SAMPLE_FREQUENCY);
		curve.setColor(0, 255, 0);
		openGLV2SamplesCount = 0;
		GLChart1D glChart1D = GLChart1D.createChart(container, WINDOW_TIME_WIDTH, curve);
		glChart1D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// Real-time display : sweep mode and pause available (keyboard and context menu), no markers
		glChart1D.setRealTime(true);
		glChart1D.setMarkersInteractive(false);
		glChart1D.setMarkersMenuItemsVisible(false);
		glChart1D.setLegendVisible(false);
		glChart1D.setXAxisLabel(null);
		rtswtOscilloChart = glChart1D;
		rtswtOscilloSerie = curve;
	}
	
	/*
	 * Disposes the graph and its container
	 */
	private void disposeGraph() {
		if(rtswtOscilloChart != null) rtswtOscilloChart.dispose();
		if(graphContainer != null && !graphContainer.isDisposed()) graphContainer.dispose();
		rtswtOscilloChart = null;
		rtswtOscilloSerie = null;
		graphContainer = null;
	}
	
	public void addSample(double x, double y) {
		yComputed = y;
		if(input && analog && showCalibratedValuesButton!= null && !showCalibratedValuesButton.isDisposed() && showCalibratedValuesButton.getSelection()) 
			// Compute calibration
			yComputed = CalibrationFactory.compute(y, channel);
		
		if(input) {
			// Current value in the title of the inputs
			inputValue = yComputed;
			if(Display.getDefault() != null && !Display.getDefault().isDisposed())
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if(!isDisposed()) setText(title + " - " + decimalFormater.format(yComputed));
					}
				});
		}
		
		if(!traceVisible || rtswtOscilloSerie == null) return;
		if(rtswtOscilloSerie instanceof Curve1DData) {
			// Time common to the viewers sharing the time base
			addOpenGLV2Sample((Curve1DData) rtswtOscilloSerie, timeBase.getTime(x), yComputed);
			return;
		}
		if(firstSample) {
			initialTime = x;
			firstSample = false;
		}
		rtswtOscilloSerie.addPoints(new Double[] {x- initialTime}, new Double[] {yComputed});
	}
	
	/*
	 * Resamples at OPENGL_V2_SAMPLE_FREQUENCY the irregular samples of the calibration/monitoring
	 * process (sample and hold) : value is repeated until time, so that the time axis of the curve
	 * follows the real time. Nothing is added while time has not reached the next sampling period.
	 */
	private void addOpenGLV2Sample(Curve1DData curve, double time, double value) {
		long expectedSamplesCount = (long)Math.floor(time * OPENGL_V2_SAMPLE_FREQUENCY) + 1;
		long nbSamples = expectedSamplesCount - openGLV2SamplesCount;
		if(nbSamples <= 0) return;
		float[] values = new float[(int)Math.min(nbSamples, OPENGL_V2_MAX_SAMPLES)];
		Arrays.fill(values, (float) value);
		curve.addSamples(values);
		openGLV2SamplesCount = expectedSamplesCount;
	}
	
	public Channel getChannel() {
		return channel;
	}

}