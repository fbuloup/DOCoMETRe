package fr.univamu.ism.docometre.dacqsystems.charts;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ImageChartConfigurationProperties extends Property {
	
	public static final ImageChartConfigurationProperties IMAGES_FILE_NAME = new ImageChartConfigurationProperties("ImageChartConfigurationProperties.IMAGES_FILE_NAME", DocometreMessages.ImageChartFileNameLabel, DocometreMessages.ImageChartFileNameTooltip, ".*");
	
	public static void populateProperties(ImageChartConfiguration imageChartConfiguration){
		ChartConfigurationProperties.populateProperties(imageChartConfiguration);
		imageChartConfiguration.setProperty(IMAGES_FILE_NAME, "images.txt");
	}
	
	public static ImageChartConfiguration clone(ImageChartConfiguration imageChartConfiguration) {
		ImageChartConfiguration newImageChartConfiguration = new ImageChartConfiguration();
		ChartConfigurationProperties.clone(imageChartConfiguration, newImageChartConfiguration);
		imageChartConfiguration.setProperty(IMAGES_FILE_NAME, new String(imageChartConfiguration.getProperty(IMAGES_FILE_NAME)));
		return imageChartConfiguration;
	}
	
	private ImageChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}

	public ImageChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
			
}
