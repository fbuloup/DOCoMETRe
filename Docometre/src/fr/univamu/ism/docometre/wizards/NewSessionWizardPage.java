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
package fr.univamu.ism.docometre.wizards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class NewSessionWizardPage extends NewResourceWizardPage {

	private Spinner trialsNumberSpinner;
	private ControlDecoration controlDecoration;
	private ControlDecoration controlDecoration2;
	private int trialsNumber;
	private int minTrial;
	private Button prefixButton;
	private Text prefixText;
	private Button firstSuffixbutton;
	private Button secondSuffixButton;
	
	private boolean usePrefix;
	private String prefix;
	private boolean useFirstSuffix;
	private boolean useSecondSuffix;
	private CLabel exampleNameLabel;

	protected NewSessionWizardPage() {
		super(DocometreMessages.NewSessionWizard_PageName, ResourceType.SESSION);
		
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		resourceType = ResourceType.SESSION;
	}
	
	

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		NewResourceWizard wizard = (NewResourceWizard)getWizard();
		setTitle(wizard.getMode() == NewResourceWizard.CREATE ? DocometreMessages.NewSessionWizard_PageTitle : DocometreMessages.ModifySessionPageTitle);
		setMessage(wizard.getMode() == NewResourceWizard.CREATE ? DocometreMessages.NewSessionWizard_PageMessage :  DocometreMessages.ModifySessionMessage);
		
		Composite container = (Composite) getControl();
		
		Label dataFilesNamesLabel = new Label(container, SWT.NORMAL);
		dataFilesNamesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		dataFilesNamesLabel.setText(DocometreMessages.ForDataFilesNamesLabel);
		
		// Prefix : use this as prefix for data files names (default subject name)
		prefixButton = new Button(container, SWT.CHECK);
		usePrefix = false;
		if(wizard.getMode() == NewResourceWizard.MODIFY) usePrefix = ResourceProperties.getDataFilesNamesPrefix(wizard.getResource()) !=  null;
		prefixButton.setSelection(usePrefix);
		prefixButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		prefixButton.setText(DocometreMessages.UsePrefixForDataFilesNamesLabel);
		
		prefixText = new Text(container, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		prefixText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		prefixText.setText("prefix");//wizard.getParentResource().getName());
		prefixText.setEnabled(usePrefix);
		prefixText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				prefix = prefixText.getText();
				modifyHandler();
			}
		});
		if(wizard.getMode() == NewResourceWizard.MODIFY && usePrefix) prefixText.setText(ResourceProperties.getDataFilesNamesPrefix(wizard.getResource()));
		prefix = prefixText.getText();
		
		controlDecoration2 = new ControlDecoration(prefixText, SWT.LEFT | SWT.TOP);
		controlDecoration2.setDescriptionText(DocometreMessages.NewSessionWizard_ErrorMessage);
		controlDecoration2.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		
		prefixButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				prefixText.setEnabled(prefixButton.getSelection());
				usePrefix = prefixButton.getSelection();
				modifyHandler();
			}
		});
		
		// Suffix : use session name as suffix for data files names
		firstSuffixbutton = new Button(container, SWT.CHECK);
		useFirstSuffix = true;
		if(wizard.getMode() == NewResourceWizard.MODIFY) useFirstSuffix = ResourceProperties.useSessionNameInDataFilesNamesAsFirstSuffix(wizard.getResource());
		firstSuffixbutton.setSelection(useFirstSuffix);
		firstSuffixbutton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
		firstSuffixbutton.setText(DocometreMessages.UseSessionNameAsSuffixForDataFilesNamesLabel);
		firstSuffixbutton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useFirstSuffix = firstSuffixbutton.getSelection();
				modifyHandler();
			}
		});
		
		// Suffix : use trial number as extra suffix for data files names
		secondSuffixButton = new Button(container, SWT.CHECK);
		useSecondSuffix = true;
		if(wizard.getMode() == NewResourceWizard.MODIFY) useSecondSuffix = ResourceProperties.useTrialNumberInDataFilesNamesAsSecondSuffix(wizard.getResource());
		secondSuffixButton.setSelection(useSecondSuffix);
		secondSuffixButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2,1));
		secondSuffixButton.setText(DocometreMessages.UseTrialNumberAsSuffixForDataFilesNamesLabel);
		secondSuffixButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useSecondSuffix = secondSuffixButton.getSelection();
				modifyHandler();
			}
		});
		// Always use trial number as suffix
		secondSuffixButton.setEnabled(false);
		
		exampleNameLabel = new CLabel(container, SWT.NORMAL);
		exampleNameLabel.setText(DocometreMessages.ExampleLabel);
		exampleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		CLabel trialsNumberLabel = new CLabel(container, SWT.NORMAL);
		trialsNumberLabel.setText(DocometreMessages.TrialsNumberLabel);
		trialsNumberLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		trialsNumberSpinner = new Spinner(container, SWT.BORDER);
		trialsNumberSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		trialsNumberSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int value = trialsNumberSpinner.getSelection();
				if(value < minTrial) {
					value = minTrial;
					trialsNumberSpinner.setSelection(value);
				} else modifyHandler();
			}
		});
		trialsNumberSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent event) {
				int value = trialsNumberSpinner.getSelection() + event.count;
				if(value < minTrial) value = minTrial;
				trialsNumberSpinner.setSelection(value);
			}
		});
		controlDecoration = new ControlDecoration(trialsNumberSpinner, SWT.LEFT | SWT.TOP);
		controlDecoration.setDescriptionText(DocometreMessages.NewSessionWizard_ErrorMessage);
		controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		trialsNumberSpinner.setSelection(0);
		trialsNumberSpinner.setMaximum(1000000);
		if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.MODIFY) trialsNumberSpinner.setSelection(getMinTrials());
		
		setPageComplete(false);
	}
	
	protected void modifyHandler() {
		/* Update example data file name */
		updateDataFileNameExampleHandler();
		/* Check if trials number is correct */
		if(trialsNumberSpinner != null) {
			String value = trialsNumberSpinner.getText();
			Pattern pattern = Pattern.compile("^\\d+$");
			Matcher matcher = pattern.matcher(value);
			if(!matcher.matches()) {
				String message = NLS.bind(DocometreMessages.NewSessionWizard_ErrorMessage, value);
				setErrorMessage(message);
				controlDecoration.show();
				setPageComplete(false);
			} else {
				controlDecoration.hide();
				value = prefixText.getText();
				pattern = Pattern.compile(regexp);
				matcher = pattern.matcher(value);
				if(!matcher.matches() && usePrefix) {
					String message = NLS.bind(DocometreMessages.NewSessionWizard_ErrorMessage2, value);
					setErrorMessage(message);
					controlDecoration2.show();
					setPageComplete(false);
				} else {
					setErrorMessage(null);
					controlDecoration.hide();
					controlDecoration2.hide();
					value = trialsNumberSpinner.getText();
					trialsNumber = Integer.parseInt(value);
					if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.CREATE) super.modifyHandler();
					else setPageComplete(true);
				}
			}
		}
	}
	
	private void updateDataFileNameExampleHandler() {
		if(exampleNameLabel == null) return;
		String preText = "Channel_Name";
		
		if(usePrefix) {
			preText = prefix + ".Channel_Name";
		}
		
		if(useFirstSuffix) {
			preText = preText + "." + resourceNameText.getText();// ".Session_Name";
		}
		
		if(useSecondSuffix) {
			preText = preText + ".T01";
		}
		
		String text = NLS.bind(DocometreMessages.ExampleLabel, preText);
		text = text + ".samples";
		exampleNameLabel.setText(text);
	}

	public int getTrialsNumber() {
		return trialsNumber;
	}
	
	public boolean getUsePrefix() {
		return usePrefix;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public boolean useSessionNameSuffix() {
		return useFirstSuffix;
	}
	
	public boolean useTrialNumberSuffix() {
		return useSecondSuffix;
	}
	
	public int getMinTrials() {
		IContainer session = (IContainer) ((NewResourceWizard)getWizard()).getResource();
		try {
			IResource[] members  = session.members();
			for (IResource member : members) {
				if(ResourceType.isTrial(member)) {
					int trialNumber = Integer.parseInt(member.getName().split("°")[1]);
					if(trialNumber > minTrial) minTrial = trialNumber;
				}
			}
			return minTrial;
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return 0;
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return true;
	}
	
}
