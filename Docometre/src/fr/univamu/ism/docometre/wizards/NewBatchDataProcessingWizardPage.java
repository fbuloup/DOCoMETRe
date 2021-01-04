package fr.univamu.ism.docometre.wizards;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;

public class NewBatchDataProcessingWizardPage extends NewResourceWizardPage {

	protected NewBatchDataProcessingWizardPage() {
		super(DocometreMessages.NewBatchDataProcessingWizard_PageName, ResourceType.BATCH_DATA_PROCESSING);
		setTitle(DocometreMessages.NewBatchDataProcessingWizard_PageTitle);
		setMessage(DocometreMessages.NewBatchDataProcessingWizard_PageMessage);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		resourceType = ResourceType.BATCH_DATA_PROCESSING;
	}

}
