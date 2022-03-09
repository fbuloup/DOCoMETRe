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
