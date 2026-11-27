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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.calibration;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.widgets.CalibrationListener;
import fr.univamu.ism.docometre.widgets.ChannelViewer;

/**
 * Calibration and monitoring dialog, common to the DACQ systems : inputs on the left,
 * outputs on the right, a ChannelViewer (collapsible group) for each channel. The subclass of each system
 * creates the viewers of its channels (see {@link #populate(Composite, Composite)}) ;
 * the calibration process of the system is run by {@link CalibrationFactory}.
 */
public abstract class AbstractCalibrateMonitorDialog extends TitleAreaDialog {
	
	private static final class GlobalCalibrationListener implements CalibrationListener {
		public static boolean calibrationModified;
		@Override
		public void push(double value) {
			calibrationModified = true;
		}
		@Override
		public double get() throws Exception {
			// TODO nothing !
			return 0;
		}
		public boolean hasBeenModified() {
			return calibrationModified;
		}
	}
	
	public static GlobalCalibrationListener globalCalibrationListener = new GlobalCalibrationListener();
	
	private String system;
	private boolean started; 
	protected ArrayList<ChannelViewer> channelsViewers = new ArrayList<>();
	// Time origin common to the graphs of the viewers ("SWT + OpenGL v2" library)
	private ChannelViewer.TimeBase timeBase = new ChannelViewer.TimeBase();

	/**
	 * @param system the DACQ system (see Activator.ADWIN_SYSTEM, Activator.ARDUINO_UNO_SYSTEM)
	 */
	public AbstractCalibrateMonitorDialog(Shell parentShell, String system) {
		super(parentShell);
		this.system = system;
		GlobalCalibrationListener.calibrationModified = false;
	}
	
	public static CalibrationListener getGlobalCalibrationListener() {
		return globalCalibrationListener;
	}
	
	public static boolean hasCalibrationBeenModified() {
		return globalCalibrationListener.hasBeenModified();
	}
	
	/**
	 * Creates the ChannelViewer of each input in inputsContainer and of each output in outputsContainer,
	 * and adds them to {@link #channelsViewers}
	 */
	protected abstract void populate(Composite inputsContainer, Composite outputsContainer);
	
	/**
	 * Creates a viewer of a channel in container and adds it to {@link #channelsViewers}
	 */
	protected ChannelViewer createChannelViewer(Composite container, Channel channel, boolean input, boolean analog) {
		ChannelViewer channelViewer = new ChannelViewer(container, SWT.NONE, channel, input, analog);
		channelViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		channelViewer.setTimeBase(timeBase);
		channelViewer.setGraphVisibilityListener(this::resetGraphs);
		channelsViewers.add(channelViewer);
		return channelViewer;
	}
	
	/**
	 * Called each time a graph is shown or hidden : all the graphs restart together at time 0, with a 
	 * common time origin. The graphs of the "SWT + OpenGL v2" library are cleared here, the others are
	 * reset by the resizing due to the layout.
	 */
	private void resetGraphs() {
		timeBase.reset();
		for (ChannelViewer channelViewer : channelsViewers) {
			if(!channelViewer.isDisposed()) channelViewer.resetGraph();
		}
	}
	
	@Override
	public void create() {
		super.create();
		getShell().setText(DocometreMessages.CalibrationMonitoringDialogShellTitle);
		setTitle(DocometreMessages.CalibrationMonitoringDialogTitle);
		String message = DocometreMessages.CalibrationMonitoringDialogMessage;
		message = NLS.bind(message, IDialogConstants.PROCEED_LABEL);
		message = message.replaceAll("&", "");
		setMessage(message, IMessageProvider.INFORMATION);
		setTitleImage(Activator.getImage(IImageKeys.CALIBRATE_MONITOR_WIZBAN));
	}
	
	@Override
	protected void handleShellCloseEvent() {
		buttonPressed(IDialogConstants.OK_ID);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		Composite container = new Composite(parent, SWT.BORDER);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout(2, true);
		gl.horizontalSpacing = 2;
		gl.verticalSpacing = 0;
		gl.marginWidth = 2;
		gl.marginHeight = 2;
		container.setLayout(gl);

		ScrolledComposite inputsScrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL /*| SWT.H_SCROLL*/ | SWT.NORMAL);
		inputsScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite inputsContainer = new Composite(inputsScrolledComposite, SWT.NONE);
		inputsScrolledComposite.setContent(inputsContainer);
		gl = new GridLayout();
		gl.horizontalSpacing = 0;
		// Space between the channels viewers (collapsible groups)
		gl.verticalSpacing = 3;
		gl.marginWidth = 6;
		gl.marginHeight = 6;
		inputsContainer.setLayout(gl);
		Label labelInputs = new Label(inputsContainer, SWT.NORMAL);
		labelInputs.setText(DocometreMessages.Inputs);
		labelInputs.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		labelInputs.setFont(Activator.getBoldFont(JFaceResources.DEFAULT_FONT));
		
		ScrolledComposite outputsScrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL /*| SWT.H_SCROLL*/ | SWT.NORMAL);
		outputsScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite outputsContainer = new Composite(outputsScrolledComposite, SWT.NONE);
		outputsScrolledComposite.setContent(outputsContainer);
		gl = new GridLayout();
		gl.horizontalSpacing = 0;
		// Space between the channels viewers (collapsible groups)
		gl.verticalSpacing = 3;
		gl.marginWidth = 6;
		gl.marginHeight = 6;
		outputsContainer.setLayout(gl);
		Label labelOutputs = new Label(outputsContainer, SWT.NORMAL);
		labelOutputs.setText(DocometreMessages.Outputs);
		labelOutputs.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		labelOutputs.setFont(Activator.getBoldFont(JFaceResources.DEFAULT_FONT));

		populate(inputsContainer, outputsContainer);
		
		if(inputsContainer.getChildren().length == 1) inputsScrolledComposite.dispose();
		else {
			inputsScrolledComposite.setExpandHorizontal(true);
			inputsScrolledComposite.setExpandVertical(true);
			inputsScrolledComposite.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					updateScrolledContent(inputsScrolledComposite);
				}
			});
		}
		if(outputsContainer.getChildren().length == 1) outputsScrolledComposite.dispose();
		else {
			outputsScrolledComposite.setExpandHorizontal(true);
			outputsScrolledComposite.setExpandVertical(true);
			outputsScrolledComposite.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					updateScrolledContent(outputsScrolledComposite);
				}
			});
		}
		
		if(inputsScrolledComposite.isDisposed() || outputsScrolledComposite.isDisposed()) {
			container.setLayout(new FillLayout());
		}
		
		getShell().addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				CalibrationFactory.stopCalibrationProcess();
				
			}
		});
		
		return container;
		
		
	}
	
	/**
	 * Updates the content of a scrolled composite of the dialog after a change of its size or of the size of its content
	 * (graph of a viewer shown or hidden). The width of the content is the width of the visible area, without the vertical
	 * scroll bar when it is visible (horizontal expansion of the scrolled composite) : the content must not be wider than
	 * the visible area, its right side would be hidden. The vertical scroll bar is shown when the content is higher than
	 * the visible area.
	 */
	public static void updateScrolledContent(ScrolledComposite scrolledComposite) {
		Control content = scrolledComposite.getContent();
		if(content == null || content.isDisposed()) return;
		scrolledComposite.setMinHeight(content.computeSize(scrolledComposite.getClientArea().width, SWT.DEFAULT).y);
		// Lays out the viewers even if the size of the content did not change (content smaller than the visible area)
		if(content instanceof Composite) ((Composite) content).layout(true);
	}
	
	/**
	 * Starts the calibration process of the system (Proceed button), on the UI thread
	 */
	protected void startCalibrationProcess() throws Exception {
		CalibrationFactory.runCalibrationProcess(system, channelsViewers.toArray(new ChannelViewer[channelsViewers.size()]));
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button proceedButton = createButton(parent, IDialogConstants.PROCEED_ID, DocometreMessages.StartMonitoring, false);
		proceedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if(!started) {
					try {
						startCalibrationProcess();
						proceedButton.setText(DocometreMessages.StopMonitoring);
						started = !started;
					} catch (Exception e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
						MessageDialog.openError(getShell(), "Error", e.getMessage());
					}
				} else {
					proceedButton.setText(DocometreMessages.StartMonitoring);
					CalibrationFactory.stopCalibrationProcess();
					started = !started;
					
				}
			}
		});
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OK_ID && started) {
			if(!MessageDialog.openQuestion(getShell(), DocometreMessages.StopMonitoring, DocometreMessages.AreYouSureStopMonitoring)) return;
			CalibrationFactory.stopCalibrationProcess();
			started = false;
		}
		super.buttonPressed(buttonId);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		Rectangle parentShellBounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
		int dx = parentShellBounds.width/10;
		int dy = parentShellBounds.height/10;
		int x = parentShellBounds.x + dx/2;
		int y = parentShellBounds.y + dy/2;
		int w = parentShellBounds.width - dx;
		int h = parentShellBounds.height - dy;
		newShell.setBounds(x, y, w, h);
	}
	
}
