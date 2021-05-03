package fr.univamu.ism.docometre.wizards;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;

public class NewXYChartWizardPage extends NewResourceWizardPage {

	protected NewXYChartWizardPage() {
		super(DocometreMessages.NewXYChartWizard_PageName, ResourceType.XYCHART);
		setTitle(DocometreMessages.NewXYChartWizard_PageTitle);
		setMessage(DocometreMessages.NewXYChartWizard_PageMessage);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		resourceType = ResourceType.XYCHART;
	}
	
	

}
