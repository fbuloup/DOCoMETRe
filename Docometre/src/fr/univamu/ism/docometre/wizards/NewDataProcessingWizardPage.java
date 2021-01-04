package fr.univamu.ism.docometre.wizards;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;

public class NewDataProcessingWizardPage extends NewResourceWizardPage {

	protected NewDataProcessingWizardPage() {
		super(DocometreMessages.NewDataProcessingWizard_PageName, ResourceType.DATA_PROCESSING);
		setTitle(DocometreMessages.NewDataProcessingWizard_PageTitle);
		setMessage(DocometreMessages.NewDataProcessingWizard_PageMessage);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		resourceType = ResourceType.DATA_PROCESSING;
	}
	
	

}
