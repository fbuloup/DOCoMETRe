package fr.univamu.ism.docometre.dacqsystems.charts;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class MeterChartConfigurationProperties extends Property {
	
	public static final MeterChartConfigurationProperties RANGE_MAX = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.RANGE_MAX", DocometreMessages.rangeMaxAmplitude_Title, DocometreMessages.rangeMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final MeterChartConfigurationProperties RANGE_MIN = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.RANGE_MIN", DocometreMessages.rangeMinAmplitude_Title, DocometreMessages.rangeMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	
	public static final MeterChartConfigurationProperties SHOW_LOW = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.SHOW_LOW", DocometreMessages.ShowLow_Title, DocometreMessages.ShowLow_Tooltip, "^(true|false)$", "true:false");
	public static final MeterChartConfigurationProperties SHOW_LOW_LOW = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.SHOW_LOW_LOW", DocometreMessages.ShowLowLow_Title, DocometreMessages.ShowLowLow_Tooltip, "^(true|false)$", "true:false");
	public static final MeterChartConfigurationProperties SHOW_HIGH = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.SHOW_HIGH", DocometreMessages.ShowHigh_Title, DocometreMessages.ShowHigh_Tooltip, "^(true|false)$", "true:false");
	public static final MeterChartConfigurationProperties SHOW_HIGH_HIGH = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.SHOW_HIGH_HIGH", DocometreMessages.ShowHighHigh_Title, DocometreMessages.ShowHighHigh_Tooltip, "^(true|false)$", "true:false");
	
	public static final MeterChartConfigurationProperties LEVEL_LOW = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.LEVEL_LOW", DocometreMessages.LevelLow_Title, DocometreMessages.LevelLow_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\\\.[0-9]+)?$");
	public static final MeterChartConfigurationProperties LEVEL_LOW_LOW = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.LEVEL_LOW_LOW", DocometreMessages.LevelLowLow_Title, DocometreMessages.LevelLowLow_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\\\.[0-9]+)?$");
	public static final MeterChartConfigurationProperties LEVEL_HIGH = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.LEVEL_HIGH", DocometreMessages.LevelHigh_Title, DocometreMessages.LevelHigh_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\\\.[0-9]+)?$");
	public static final MeterChartConfigurationProperties LEVEL_HIGH_HIGH = new MeterChartConfigurationProperties("MeterChartConfigurationProperties.LEVEL_HIGH_HIGH", DocometreMessages.LevelHighHigh_Title, DocometreMessages.LevelHighHigh_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\\\.[0-9]+)?$");
	
	public static void populateProperties(MeterChartConfiguration meterChartConfiguration){
		ChartConfigurationProperties.populateProperties(meterChartConfiguration);
		meterChartConfiguration.setProperty(RANGE_MAX, "10");
		meterChartConfiguration.setProperty(RANGE_MIN, "-10");
		meterChartConfiguration.setProperty(SHOW_LOW, "false");
		meterChartConfiguration.setProperty(SHOW_LOW_LOW, "false");
		meterChartConfiguration.setProperty(SHOW_HIGH, "false");
		meterChartConfiguration.setProperty(SHOW_HIGH_HIGH, "false");
		meterChartConfiguration.setProperty(LEVEL_LOW, "-5");
		meterChartConfiguration.setProperty(LEVEL_LOW_LOW, "-7.5");
		meterChartConfiguration.setProperty(LEVEL_HIGH, "5");
		meterChartConfiguration.setProperty(LEVEL_HIGH_HIGH, "7.5");
	}
	
	public static MeterChartConfiguration clone(MeterChartConfiguration meterChartConfiguration) {
		MeterChartConfiguration newMeterChartConfiguration = new MeterChartConfiguration(meterChartConfiguration.getChartType());
		ChartConfigurationProperties.clone(meterChartConfiguration, newMeterChartConfiguration);
		newMeterChartConfiguration.setProperty(RANGE_MAX, new String(meterChartConfiguration.getProperty(RANGE_MAX)));
		newMeterChartConfiguration.setProperty(RANGE_MIN, new String(meterChartConfiguration.getProperty(RANGE_MIN)));
		newMeterChartConfiguration.setProperty(SHOW_LOW, new String(meterChartConfiguration.getProperty(SHOW_LOW)));
		newMeterChartConfiguration.setProperty(SHOW_LOW_LOW, new String(meterChartConfiguration.getProperty(SHOW_LOW_LOW)));
		newMeterChartConfiguration.setProperty(SHOW_HIGH, new String(meterChartConfiguration.getProperty(SHOW_HIGH)));
		newMeterChartConfiguration.setProperty(SHOW_HIGH_HIGH, new String(meterChartConfiguration.getProperty(SHOW_HIGH_HIGH)));
		newMeterChartConfiguration.setProperty(LEVEL_LOW, new String(meterChartConfiguration.getProperty(LEVEL_LOW)));
		newMeterChartConfiguration.setProperty(LEVEL_LOW_LOW, new String(meterChartConfiguration.getProperty(LEVEL_LOW_LOW)));
		newMeterChartConfiguration.setProperty(LEVEL_HIGH, new String(meterChartConfiguration.getProperty(LEVEL_HIGH)));
		newMeterChartConfiguration.setProperty(LEVEL_HIGH_HIGH, new String(meterChartConfiguration.getProperty(LEVEL_HIGH_HIGH)));
		CurveConfiguration[] curveConfigurations = meterChartConfiguration.getCurvesConfiguration();
		for (CurveConfiguration curveConfiguration : curveConfigurations) {
			try {
				MeterCurveConfiguration meterCurveConfiguration = (MeterCurveConfiguration)curveConfiguration;
				newMeterChartConfiguration.addCurveConfiguration((CurveConfiguration) meterCurveConfiguration.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return newMeterChartConfiguration;
	}
	
	private MeterChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}

	public MeterChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
