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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ValidateTrialDialog extends Dialog {

	private boolean saveChoice;
	private boolean validateTrial = true;
	private boolean redoTrial = false;
	private IResource trial;

	public ValidateTrialDialog(Shell parentShell, IResource currentTrial) {
		super(parentShell);
		this.trial = currentTrial;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		getShell().setText(DocometreMessages.ValidateTrialDialogTitle);
		
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Label messageLabel = new Label(composite, SWT.NORMAL);
        messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        messageLabel.setText(NLS.bind(DocometreMessages.DoYouWantValidTrial, trial.getName()));
		
		 Button validateTrialButton = new Button(composite, SWT.RADIO);
		 validateTrialButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		 validateTrialButton.setSelection(true);
		 validateTrialButton.setText(IDialogConstants.YES_LABEL);
		 validateTrialButton.addSelectionListener(new SelectionAdapter() {
	    		@Override
	    		public void widgetSelected(SelectionEvent e) {
	    			validateTrial = validateTrialButton.getSelection();
	    		}
        });
	        
        Button dontValidateTrialButton = new Button(composite, SWT.RADIO);
        dontValidateTrialButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        dontValidateTrialButton.setSelection(false);
        dontValidateTrialButton.setText(IDialogConstants.NO_LABEL);
        dontValidateTrialButton.addSelectionListener(new SelectionAdapter() {
        		@Override
        		public void widgetSelected(SelectionEvent e) {
	    			validateTrial = validateTrialButton.getSelection();
        		}
		});
		
        Button dontValidateRedoTrialButton = new Button(composite, SWT.RADIO);
        dontValidateRedoTrialButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        dontValidateRedoTrialButton.setSelection(false);
        dontValidateRedoTrialButton.setText(DocometreMessages.RedoTrialNow);
        dontValidateRedoTrialButton.addSelectionListener(new SelectionAdapter() {
        		@Override
        		public void widgetSelected(SelectionEvent e) {
        			redoTrial = dontValidateRedoTrialButton.getSelection();
        		}
		});
		
		
		Button saveThisStateButton = new Button(composite, SWT.CHECK);
		saveThisStateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        saveThisStateButton.setText(DocometreMessages.TrialDialogUseChoiceDontAsk);
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
		createButton(parent, IDialogConstants.OK_ID, DocometreMessages.ContinueLabel, true);
	}
	
	@Override
	protected void okPressed() {
		Activator.getDefault().getPreferenceStore().putValue(GeneralPreferenceConstants.AUTO_VALIDATE_TRIALS, Boolean.toString(saveChoice));
		super.okPressed();
	}
	
	public boolean getValidateTrial() {
		return validateTrial;
	}
	
	public boolean getRedoTrial() {
		return redoTrial;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
}
