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
package fr.univamu.ism.docometre.dacqsystems.adwin.calibration;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.calibration.CalibrationFactory;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutModule;
import fr.univamu.ism.docometre.widgets.ChannelViewer;

public class CalibrateMonitorDialog extends TitleAreaDialog {
	
	private ADWinDACQConfiguration adwinDACQConfiguration;
	private boolean started; 
	private ArrayList<ChannelViewer> channelsViewers = new ArrayList<>();

	public CalibrateMonitorDialog(Shell parentShell, ADWinDACQConfiguration adwinDACQConfiguration) {
		super(parentShell);
		this.adwinDACQConfiguration = adwinDACQConfiguration;
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
		getShell().setMaximized(true);
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

		ScrolledComposite inputsScrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL /*| SWT.H_SCROLL*/ | SWT.BORDER);
		inputsScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite inputsContainer = new Composite(inputsScrolledComposite, SWT.NONE);
		inputsScrolledComposite.setContent(inputsContainer);
		gl = new GridLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		inputsContainer.setLayout(gl);
		Label labelInputs = new Label(inputsContainer, SWT.NORMAL);
		labelInputs.setText(DocometreMessages.Inputs);
		labelInputs.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		labelInputs.setFont(Activator.getBoldFont(JFaceResources.DEFAULT_FONT));
		
		ScrolledComposite outputsScrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL /*| SWT.H_SCROLL*/ | SWT.BORDER);
		outputsScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite outputsContainer = new Composite(outputsScrolledComposite, SWT.NONE);
		outputsScrolledComposite.setContent(outputsContainer);
		gl = new GridLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		outputsContainer.setLayout(gl);
		Label labelOutputs = new Label(outputsContainer, SWT.NORMAL);
		labelOutputs.setText(DocometreMessages.Outputs);
		labelOutputs.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		labelOutputs.setFont(Activator.getBoldFont(JFaceResources.DEFAULT_FONT));

		Module[] modules = adwinDACQConfiguration.getModules();
		
		// Populate analog inputs
		for (Module module : modules) {
			if(module instanceof ADWinAnInModule) {
				module = (ADWinAnInModule)module; 
				Channel[] channels = module.getChannels();
				for (Channel channel : channels) {
					ChannelViewer channelViewer = new ChannelViewer(inputsContainer, SWT.NONE, channel, true, true);
					channelViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					channelsViewers.add(channelViewer);
				}
			}
		}
		
		// Populate digital inputs
		for (Module module : modules) {
			if(module instanceof ADWinDigInOutModule) {
				module = (ADWinDigInOutModule)module;
				Channel[] channels = module.getChannels();
				for (Channel channel : channels) {
					String inOut = channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
					if(inOut.equals(ADWinDigInOutChannelProperties.INPUT)) {
						ChannelViewer channelViewer = new ChannelViewer(inputsContainer, SWT.NONE, channel, true, false);
						channelViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						channelsViewers.add(channelViewer);
					}
				}
			}
		}
		
		// Populate analog outputs
		for (Module module : modules) {
			if(module instanceof ADWinAnOutModule) {
				module = (ADWinAnOutModule)module;
				Channel[] channels = module.getChannels();
				for (Channel channel : channels) {
					ChannelViewer channelViewer = new ChannelViewer(outputsContainer, SWT.NONE, channel, false, true);
					channelViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					channelsViewers.add(channelViewer);
				}
			}
		}
		
		// Populate digital outputs
		for (Module module : modules) {
			if(module instanceof ADWinDigInOutModule) {
				module = (ADWinDigInOutModule)module;
				Channel[] channels = module.getChannels();
				for (Channel channel : channels) {
					String inOut = channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
					if(inOut.equals(ADWinDigInOutChannelProperties.OUTPUT)) {
						ChannelViewer channelViewer = new ChannelViewer(outputsContainer, SWT.NONE, channel, false, false);
						channelViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						channelsViewers.add(channelViewer);
					}
					
				}
			}
		}
		
		inputsScrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				inputsContainer.setSize(inputsContainer.computeSize(inputsScrolledComposite.getSize().x, SWT.DEFAULT));
			}
		});
		
		outputsScrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				outputsContainer.setSize(outputsContainer.computeSize(outputsScrolledComposite.getSize().x, SWT.DEFAULT));
			}
		});
		
		getShell().addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				CalibrationFactory.stopCalibrationProcess();
				
			}
		});
		
		return container;
		
		
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button proceedButton = createButton(parent, IDialogConstants.PROCEED_ID, IDialogConstants.PROCEED_LABEL, false);
		proceedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if(!started) {
					try {
						CalibrationFactory.runCalibrationProcess(Activator.ADWIN_SYSTEM, channelsViewers.toArray(new ChannelViewer[channelsViewers.size()]));
						proceedButton.setText(IDialogConstants.STOP_LABEL);
						started = !started;
					} catch (Exception e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
						MessageDialog.openError(getShell(), "Error", e.getMessage());
					}
				} else {
					proceedButton.setText(IDialogConstants.PROCEED_LABEL);
					CalibrationFactory.stopCalibrationProcess();
					started = !started;
				}
			}
		});
		Button terminatedButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, false);
		terminatedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(started) {
					CalibrationFactory.stopCalibrationProcess();
				}
			}
		});
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
}
