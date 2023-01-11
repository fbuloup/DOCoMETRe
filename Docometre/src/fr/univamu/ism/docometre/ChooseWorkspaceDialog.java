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
package fr.univamu.ism.docometre;

import java.io.File;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.util.Geometry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class ChooseWorkspaceDialog extends TitleAreaDialog {
	
	private ChooseWorkspaceData launchData;
	private Combo text;

	public ChooseWorkspaceDialog(ChooseWorkspaceData launchData) {
		super(null);
		this.launchData = launchData;
	}

	protected Control createDialogArea(Composite parent) {
		String productName = getWindowTitle();

		Composite composite = (Composite) super.createDialogArea(parent);
		setTitle(DocometreMessages.ChooseWorkspaceDialog_dialogTitle);
		setMessage(NLS.bind(DocometreMessages.ChooseWorkspaceDialog_dialogMessage, productName));

		createWorkspaceBrowseRow(composite);
		createShowDialogButton(composite);
		
		Dialog.applyDialogFont(composite);
		return composite;
	}

	public String getWindowTitle() {
		String productName = null;
		IProduct product = Platform.getProduct();
		if (product != null) productName = product.getName();
		if (productName == null) productName = DocometreMessages.ChooseWorkspaceDialog_defaultProductName;
		return productName;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(DocometreMessages.ChooseWorkspaceDialog_dialogName);
	}

	protected void okPressed() {
		this.launchData.setSelection(TextProcessor.deprocess(getWorkspaceLocation()));
		super.okPressed();
	}

	protected String getWorkspaceLocation() {
		return this.text.getText();
	}
	
	@Override
	public boolean close() {
		Rectangle bounds = getShell().getBounds();
		launchData.setWorkspaceDialogPosition(bounds.x, bounds.y, bounds.width, bounds.height);
		return super.close();
	}

	private void createWorkspaceBrowseRow(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout( new GridLayout(3, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		panel.setFont(parent.getFont());

		Label label = new Label(panel, SWT.NONE);
		label.setText(DocometreMessages.ChooseWorkspaceDialog_workspaceEntryLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		text = new Combo(panel, SWT.BORDER);
		text.setFocus();
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Button okButton = ChooseWorkspaceDialog.this.getButton(0);
				if ((okButton != null) && (!okButton.isDisposed())) {
					boolean nonWhitespaceFound = false;
					String characters = ChooseWorkspaceDialog.this.getWorkspaceLocation();
					for (int i = 0; (!nonWhitespaceFound) && (i < characters.length());) {
						if (!Character.isWhitespace(characters.charAt(i))) nonWhitespaceFound = true;
						i++;
					}

					okButton.setEnabled(nonWhitespaceFound);
				}
			}
		});
		setInitialTextValues(text);

		Button browseButton = new Button(panel, SWT.PUSH);
		browseButton.setText(DocometreMessages.ChooseWorkspaceDialog_browseLabel);
		browseButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(ChooseWorkspaceDialog.this.getShell(), 268435456);
				dialog.setText(DocometreMessages.ChooseWorkspaceDialog_directoryBrowserTitle);
				dialog.setMessage(DocometreMessages.ChooseWorkspaceDialog_directoryBrowserMessage);
				dialog.setFilterPath(ChooseWorkspaceDialog.this.getInitialBrowsePath());
				String dir = dialog.open();
				if (dir != null)
					ChooseWorkspaceDialog.this.text.setText(TextProcessor.process(dir));
			}
		});
	}
	
	private void createShowDialogButton(Composite parent) {
		Composite panel = new Composite(parent, 0);
		panel.setFont(parent.getFont());

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = convertHorizontalDLUsToPixels(7);
		panel.setLayout(layout);

		GridData data = new GridData(1808);
		data.verticalAlignment = 3;
		panel.setLayoutData(data);

		Button button = new Button(panel, 32);
		button.setText(DocometreMessages.ChooseWorkspaceDialog_useDefaultMessage);
		button.setSelection(!this.launchData.getShowDialog());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ChooseWorkspaceDialog.this.launchData.setShowDialog(!button.getSelection());
				ChooseWorkspaceDialog.this.launchData.save();
			}
		});
	}

	private String getInitialBrowsePath() {
		File dir = new File(getWorkspaceLocation());
		while ((dir != null) && (!dir.exists())) {
			dir = dir.getParentFile();
		}

		return dir != null ? dir.getAbsolutePath() : System.getProperty("user.dir"); //$NON-NLS-1$
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Rectangle r = launchData.getWorkspaceDialogPosition();
		Monitor[] monitors = getShell().getDisplay().getMonitors();
		Monitor currentMonitor = getShell().getDisplay().getPrimaryMonitor();
		for (int i = 0; i < monitors.length; i++) {
			if (monitors[i].getBounds().intersects(r)) {
				currentMonitor = monitors[i];
			}
		}
		
		Rectangle monitorBounds = currentMonitor.getClientArea();
		Point centerPoint = Geometry.centerPoint(monitorBounds);

		return new Point(centerPoint.x - initialSize.x / 2, Math.max(monitorBounds.y,
				Math.min(centerPoint.y - initialSize.y * 2 / 3, monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	private void setInitialTextValues(Combo text) {
		String[] recentWorkspaces = this.launchData.getRecentWorkspaces();
		for (int i = 0; i < recentWorkspaces.length; i++) {
			if (recentWorkspaces[i] != null) {
				text.add(recentWorkspaces[i]);
			}
		}
		if(launchData.getSelection() != null) text.select(text.indexOf(launchData.getSelection()));
	}

//	protected IDialogSettings getDialogBoundsSettings() {
//		if (this.centerOnMonitor) return null;
//		IDialogSettings settings = Activator.getDefault().getDialogSettings();
//		IDialogSettings section = settings.getSection("ChooseWorkspaceDialogSettings"); //$NON-NLS-1$
//		if (section == null) section = settings.addNewSection("ChooseWorkspaceDialogSettings"); //$NON-NLS-1$
//		return section;
//	}
}
