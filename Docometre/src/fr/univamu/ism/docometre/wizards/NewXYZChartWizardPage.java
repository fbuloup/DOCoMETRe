package fr.univamu.ism.docometre.wizards;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;

public class NewXYZChartWizardPage extends NewResourceWizardPage {

	protected NewXYZChartWizardPage(ResourceType chartType) {
		super(DocometreMessages.NewXYChartWizard_PageName, chartType);
		resourceType = chartType;
		if(ResourceType.XYCHART.equals(chartType)) {
			setTitle(DocometreMessages.NewXYChartWizard_PageTitle);
			setMessage(DocometreMessages.NewXYChartWizard_PageMessage);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		} else if(ResourceType.XYZCHART.equals(chartType)) {
			setTitle(DocometreMessages.NewXYZChartWizard_PageTitle);
			setMessage(DocometreMessages.NewXYZChartWizard_PageMessage);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		} else resourceType = ResourceType.ANY;
	}
	
	

}
