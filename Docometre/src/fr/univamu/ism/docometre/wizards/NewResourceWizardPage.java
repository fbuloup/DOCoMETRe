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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public abstract class NewResourceWizardPage extends WizardPage implements IWizardPage {
	
	protected Text resourceNameText;
	private ControlDecoration controlDecoration;
	private String resourceName;
	private String resourceDescription;
	protected String regexp;
	private String errorMessage;
	protected ResourceType resourceType;
	protected NewResourceWizard newResourceWizard;

	protected NewResourceWizardPage(String pageName, ResourceType resourceType) {
		super(pageName);
		this.resourceType = resourceType;
		setResourceNameRegularExpression("^[a-zA-Z][a-zA-Z0-9_]*$");
		setResourceNameErrorMessage(DocometreMessages.NewResourceWizard_ErrorMessage);
	}
	
	protected void setResourceNameRegularExpression(String regexp) {
		this.regexp = regexp;
	}
	
	protected void setResourceNameErrorMessage(String message) {
		this.errorMessage = message;
	}

	
	@Override
	public void createControl(Composite parent) {
		newResourceWizard = (NewResourceWizard) getWizard();
		Composite container = new Composite(parent, SWT.NORMAL);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		container.setLayout(layout);
		
		/*Resource Name*/
		CLabel resourceNameLabel = new CLabel(container, SWT.NORMAL);
		resourceNameLabel.setText(DocometreMessages.NewResourceWizard_NameLabel);
		resourceNameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		resourceNameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.MODIFY) {
			resourceNameText.setText(((NewResourceWizard)getWizard()).getResource().getName());
			resourceNameText.setEnabled(false);
		}
		if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.CREATE) {
			resourceNameText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					modifyHandler();
				}
			});
		}
		resourceNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		controlDecoration = new ControlDecoration(resourceNameText, SWT.LEFT | SWT.TOP);
		controlDecoration.setDescriptionText(DocometreMessages.NewResourceWizard_DecorationErrorMessage);
		controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		controlDecoration.hide();
		
		if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.CREATE) {
			String text = (resourceType.equals(ResourceType.TRIAL))?DocometreMessages.NewResourceWizard_DefaultTrialName:DocometreMessages.NewResourceWizard_DefaultName;
			resourceNameText.setText(text);
			int startSelection = (resourceType.equals(ResourceType.TRIAL))?DocometreMessages.NewResourceWizard_DefaultTrialName.length()-1:0;
			int endSelection = (resourceType.equals(ResourceType.TRIAL))?DocometreMessages.NewResourceWizard_DefaultTrialName.length():DocometreMessages.NewResourceWizard_DefaultName.length();
			resourceNameText.setSelection(startSelection, endSelection);
		}
		
		/*Resource description*/
		CLabel resourceDescriptionLabel = new CLabel(container, SWT.NORMAL);
		resourceDescriptionLabel.setText(DocometreMessages.ExperimentDescriptionLabel);
		resourceDescriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		final Text resourceDescriptionText = new Text(container, SWT.MULTI | SWT.BORDER);
		resourceDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resourceDescriptionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				resourceDescription = resourceDescriptionText.getText();
			}
		});
		if(((NewResourceWizard)getWizard()).getMode() == NewResourceWizard.MODIFY) {
			try {
				resourceDescriptionText.setEnabled(false);
				String description = ((NewResourceWizard)getWizard()).getResource().getPersistentProperty(ResourceProperties.DESCRIPTION_QN);
				resourceDescriptionText.setText(description);
			} catch (CoreException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
			
		}
		
		setControl(container);
		setPageComplete(false);
	}
	
	protected void modifyHandler() {
		/*Check resource name*/
		String name = resourceNameText.getText();
		String fileExtension = "";
		if(this instanceof NewDACQConfigurationWizardPage) fileExtension = Activator.daqFileExtension;
		if(this instanceof NewProcessWizardPage) fileExtension = Activator.processFileExtension;
		if(this instanceof NewParametersFileWizardPage) fileExtension = Activator.parametersFileExtension;
		if(this instanceof NewDataProcessingWizardPage) fileExtension = Activator.dataProcessingFileExtension;
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(name);
		if(!matcher.matches()) {
			String message = NLS.bind(errorMessage, name);
			setErrorMessage(message);
			controlDecoration.show();
			setPageComplete(false);
		} else {
			if(this instanceof NewTrialWizardPage) {
				boolean error = false;
				String trialName = name;
				String trials = trialName.replaceAll(DocometreMessages.NewResourceWizard_DefaultTrialName.replaceAll("N", ""), "");
				String[] trialsSplitted = trials.split(":");
				int trialMin = Integer.parseInt(trialsSplitted[0]);
				int trialMax = trialMin;
				if(trialsSplitted.length > 1) trialMax = Integer.parseInt(trialsSplitted[1]);
				for (int n = trialMin; n <= trialMax; n++) {
					trialName = DocometreMessages.NewResourceWizard_DefaultTrialName.replaceAll("N", "") + n;
					if(newResourceWizard.getParentResource().findMember(trialName + fileExtension) != null) {
						setErrorMessage(DocometreMessages.NewResourceWizard_ErrorMessage2);
						controlDecoration.show();
						setPageComplete(false);
						error = true;
					}
				}
				if(!error) {
					setErrorMessage(null);
					controlDecoration.hide();
					resourceName = name;
					setPageComplete(true);
				}
				
			} else {
				if(newResourceWizard.getParentResource().findMember(name + fileExtension) != null) {
					setErrorMessage(DocometreMessages.NewResourceWizard_ErrorMessage2);
					controlDecoration.show();
					setPageComplete(false);
				} else {
					setErrorMessage(null);
					controlDecoration.hide();
					resourceName = name;
					setPageComplete(true);
				}
			}
		}
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public String getResourceDescription() {
		return resourceDescription==null?"":resourceDescription;
	}

}
