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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.calibration.CalibrationFactory;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.rtswtchart.RTSWTChartFonts;
import fr.univamu.ism.rtswtchart.RTSWTOscilloChart;
import fr.univamu.ism.rtswtchart.RTSWTOscilloSerie;

public class ChannelViewer extends Composite {
	
	private boolean traceVisible ;
	private String title;
	private Label separator;
	private DecimalFormat decimalFormater;
	private RTSWTOscilloSerie rtswtOscilloSerie;
	private RTSWTOscilloChart rtswtOscilloChart; 
	private boolean firstSample = true;
	private double initialTime;
	private Image imageLeft;
	private Image imageDown;
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
	// When analog or digital input
	private Label valueLabel;
	private Label stateLabel;
	
	public Object getValue() {
		if(analog && !input) return spinnerValue.getValue();
		if(!analog && !input) return stateOn;
		String value = valueLabel.getText();
		return Double.parseDouble(value);
	}
	
	public ChannelViewer(Composite parent, int style, Channel channel, boolean input, boolean analog) {
		super(parent, style);
		this.channel = channel;
		this.title = channel.getProperty(ChannelProperties.NAME) + " (" + channel.getProperty(ChannelProperties.CHANNEL_NUMBER) + ")";
		this.analog = analog;
		this.input = input;
		if(analog) decimalFormater = new DecimalFormat("#.####");
		else decimalFormater = new DecimalFormat("#");

		GridLayout gl = new GridLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);
		
		imageLeft = Activator.getImage(IImageKeys.SHOW_GRAPH_ICON);
		imageDown = Activator.getImage(IImageKeys.HIDE_GRAPH_ICON);
		
		int nbColumn = (input && analog == true) ? 14 : 3;
		
		Composite titleComposite = new Composite(this, SWT.NONE);
		titleComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		titleComposite.setLayout(new GridLayout(nbColumn, false));
		
		Label titleLabel = new Label(titleComposite, SWT.NONE);
		titleLabel.setText(title);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		titleLabel.setFont(Activator.getBoldFont(JFaceResources.DEFAULT_FONT));
		titleLabel.setFont(Activator.getItalicFont(JFaceResources.DEFAULT_FONT));
		
		if(!this.input) {
			if(!this.analog) {
				// Digital output
				imageOn = Activator.getImage(IImageKeys.ON_ICON);
				imageOff = Activator.getImage(IImageKeys.OFF_ICON);
				stateLabel = new Label(titleComposite, SWT.NONE);
				stateLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				stateLabel.setImage(imageOff);
				stateLabel.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));//new Cursor(getDisplay(), SWT.CURSOR_HAND));
				stateLabel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if(stateOn) stateLabel.setImage(imageOff);
						else stateLabel.setImage(imageOn);
						stateOn = !stateOn;
					}
				});
			} else {
				// Analog output
				spinnerValue = new SpinnerValue(titleComposite, SWT.BORDER);
				spinnerValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			}
			
		}
		if(this.input) {
			if(this.analog) {
				// Analog input with calibration header
				titleLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));
				showCalibratedValuesButton = new Button(titleComposite, SWT.CHECK);
				showCalibratedValuesButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false, 13, 1));
				showCalibratedValuesButton.setText(DocometreMessages.ShowCalibratedValues);
				// Create calibrate header 
				CalibrationFactory.createHeader(titleComposite, channel);
			}
			// Analog or digital input
			valueLabel = new Label(titleComposite, SWT.NONE);
			valueLabel.setText("?????");
			valueLabel.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));
		}
		
		Label showGraphButton = new Label(titleComposite, SWT.NONE);
		showGraphButton.setImage(imageLeft);
		showGraphButton.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		
		showGraphButton.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));//new Cursor(getDisplay(), SWT.CURSOR_HAND));
		showGraphButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if(!traceVisible) {
					firstSample = true;
					createGraph();
					showGraphButton.setImage(imageDown);
				} else {
					rtswtOscilloChart.getParent().dispose();
//					rtswtOscilloChart.dispose();
					separator.dispose();
					showGraphButton.setImage(imageLeft);
				}
				Composite sc = getParent().getParent();
				sc.layout(true);
				getParent().setSize(getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
				int width = getParent().getSize().y < sc.getSize().y ? sc.getSize().x : sc.getClientArea().width;
				
				getParent().setSize(getParent().computeSize(width, SWT.DEFAULT));
				traceVisible = !traceVisible;
			}
		});
		
		Label separator2 = new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, nbColumn, 1));
		
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
	
	private void createGraph() {
		Composite container = new Composite(this, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout());
		GridLayout gl = (GridLayout)container.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 5;
		gl.marginWidth = 0;
		rtswtOscilloChart = new RTSWTOscilloChart(container, SWT.NONE, RTSWTChartFonts.BITMAP_HELVETICA_10, 10);
		rtswtOscilloChart.setBackGroundColor(rtswtOscilloChart.getParent().getBackground());
		rtswtOscilloChart.setGridLinesColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		rtswtOscilloChart.setFontColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		rtswtOscilloChart.setLegendVisibility(false);
		rtswtOscilloChart.setAutoScale(true);
		rtswtOscilloChart.setAntialias(SWT.ON);
		rtswtOscilloChart.setInterpolation(SWT.HIGH);
		rtswtOscilloChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rtswtOscilloSerie = rtswtOscilloChart.createSerie(title, Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED), SWT.LINE_SOLID, 2);
		
		GridData gd = (GridData) rtswtOscilloChart.getLayoutData();
		gd.heightHint = 150;

		separator = new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}
	
	public void addSample(double x, double y) {
		yComputed = y;
		if(input && analog && showCalibratedValuesButton!= null && !showCalibratedValuesButton.isDisposed() && showCalibratedValuesButton.getSelection()) 
			// Compute calibration
			yComputed = CalibrationFactory.compute(y, channel);
		
		if(Display.getDefault() != null && !Display.getDefault().isDisposed())
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if(valueLabel != null && !valueLabel.isDisposed()) {
						valueLabel.setText(decimalFormater.format(yComputed));
						valueLabel.getParent().layout();
					}
				}
			});
		
		if(!traceVisible) return;
		if(firstSample) {
			initialTime = x;
			firstSample = false;
		}
		rtswtOscilloSerie.addPoints(new Double[] {x- initialTime}, new Double[] {yComputed});
	}
	
	public Channel getChannel() {
		return channel;
	}

}
