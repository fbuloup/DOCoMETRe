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
package fr.univamu.ism.docometre.dacqsystems.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import jssc.SerialPortList;

public class DeviceSelectionDialog extends Dialog {

	protected IStructuredSelection selection;
	private ListViewer devicesListViewer;
	private DeviceType deviceType;
	protected ConnectedIPsGatherer connectedIPsGatherer;

	public DeviceSelectionDialog(Shell parentShell, DeviceType deviceType) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.deviceType = deviceType;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(DocometreMessages.DeviceSelectionDialog_Title);
		Composite container = (Composite) super.createDialogArea(parent);
		
		devicesListViewer = new ListViewer(container);
		devicesListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		devicesListViewer.setContentProvider(new ArrayContentProvider());
		devicesListViewer.setLabelProvider(new LabelProvider());
		devicesListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selection = (IStructuredSelection) event.getSelection();
				getButton(IDialogConstants.OK_ID).setEnabled(!selection.isEmpty());
			}
		});
		devicesListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				selection = (IStructuredSelection) event.getSelection();
				DeviceSelectionDialog.this.okPressed();
			}
		});
		updateDevices();
		return container;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control container =  super.createContents(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return container;
	}
	
	public String getSelection() {
		return selection.getFirstElement().toString();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Button refreshButton = createButton(parent, IDialogConstants.PROCEED_ID, ArduinoUnoMessages.Refresh, false);
		refreshButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateDevices();
			};
		});
	}
		
	private void updateDevices() {
		if(deviceType == DeviceSelectionHandler.DeviceType.USB) devicesListViewer.setInput(SerialPortList.getPortNames());
		if(deviceType == DeviceSelectionHandler.DeviceType.ETHERNET) {
			try {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(getShell());
				IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						connectedIPsGatherer = new ConnectedIPsGatherer(1, 254, 1, 10);
						connectedIPsGatherer.startGathering(monitor);
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								devicesListViewer.setInput(connectedIPsGatherer.getReachableIPs());
							}
						});
					}
				};
				progressMonitorDialog.run(true, true, runnableWithProgress);
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}

		}
	}
		
		
}
