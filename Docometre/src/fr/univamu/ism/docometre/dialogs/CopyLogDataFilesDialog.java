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
package fr.univamu.ism.docometre.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.DocometreMessages;

public class CopyLogDataFilesDialog extends Dialog {
	
	public static boolean saveChoice;
	public static int choiceID;

	public CopyLogDataFilesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		getShell().setText(DocometreMessages.CopyLogAndDataFilesDialogTitle);
		
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Label messageLabel = new Label(composite, SWT.NORMAL);
        messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        messageLabel.setText(DocometreMessages.CopyLogAndDataFilesDialogQuestion);
		
		Button saveThisStateButton = new Button(composite, SWT.CHECK);
		saveThisStateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        saveThisStateButton.setText(DocometreMessages.CopyLogDataFilesDontAsk);
        saveThisStateButton.setSelection(saveChoice);
        saveThisStateButton.addSelectionListener(new SelectionAdapter() {
	    		@Override
	    		public void widgetSelected(SelectionEvent e) {
	    			saveChoice = saveThisStateButton.getSelection();
	    		}
        });
        
		return composite;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
		createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	
	@Override
	protected void buttonPressed(int buttonId) {
		choiceID = buttonId;
		setReturnCode(buttonId);
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	public String toString() {
		return "Save choice : " + saveChoice + " - Default choice : " + choiceID;
	}
	
	public static boolean copyLogAndDataFile() {
		return choiceID == IDialogConstants.YES_ID;
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		
		Button b1 = new Button(shell, SWT.PUSH);
		b1.setText("Press me");
		b1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CopyLogDataFilesDialog copyLogDataFilesDialog = new CopyLogDataFilesDialog(shell);
				System.out.println(copyLogDataFilesDialog.toString());
				int returnCode = copyLogDataFilesDialog.open();
				System.out.println("returnCode : " + returnCode);
			}
		});
		
		shell.open();
		while (!shell.isDisposed()) { 
		    if (!display.readAndDispatch()) 
		     { display.sleep();} 
		}

		// disposes all associated windows and their components
		display.dispose();
	}

}
