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
import fr.univamu.ism.docometre.FontUtil;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class XYChartConfigurationProperties extends Property {
	
	public static final XYChartConfigurationProperties X_MAX = new XYChartConfigurationProperties("XYChartConfigurationProperties.X_MAX", DocometreMessages.xMaxAmplitude_Title, DocometreMessages.xMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYChartConfigurationProperties X_MIN = new XYChartConfigurationProperties("XYChartConfigurationProperties.X_MIN", DocometreMessages.xMinAmplitude_Title, DocometreMessages.xMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYChartConfigurationProperties Y_MAX = new XYChartConfigurationProperties("XYChartConfigurationProperties.Y_MAX", DocometreMessages.yMaxAmplitude_Title, DocometreMessages.yMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYChartConfigurationProperties Y_MIN = new XYChartConfigurationProperties("XYChartConfigurationProperties.Y_MIN", DocometreMessages.yMinAmplitude_Title, DocometreMessages.yMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYChartConfigurationProperties AUTO_SCALE = new XYChartConfigurationProperties("XYChartConfigurationProperties.AUTO_SCALE", DocometreMessages.AutoScale_Title, DocometreMessages.AutoScale_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties FONT = new XYChartConfigurationProperties("XYChartConfigurationProperties.FONT", DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip, FontUtil.getAvailableFontsNames(true), FontUtil.getAvailableFontsNames(false));
	public static final XYChartConfigurationProperties FONT_BOLD = new XYChartConfigurationProperties("XYChartConfigurationProperties.FONT_BOLD", DocometreMessages.Bold_Title, DocometreMessages.Bold_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties FONT_ITALIC = new XYChartConfigurationProperties("XYChartConfigurationProperties.FONT_ITALIC", DocometreMessages.Italic_Title, DocometreMessages.Italic_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties FONT_SIZE = new XYChartConfigurationProperties("XYChartConfigurationProperties.FONT_SIZE", DocometreMessages.Size_Title, DocometreMessages.Size_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYChartConfigurationProperties HISTORY_SIZE = new XYChartConfigurationProperties("XYChartConfigurationProperties.HISTORY_SIZE", DocometreMessages.HistorySize_Title, DocometreMessages.HistorySize_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	// Display options only available with "SWT + OpenGL v2" library
	public static final XYChartConfigurationProperties SHOW_LEGEND = new XYChartConfigurationProperties("XYChartConfigurationProperties.SHOW_LEGEND", DocometreMessages.ShowLegend_Title, DocometreMessages.ShowLegend_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties LEGEND_INSIDE = new XYChartConfigurationProperties("XYChartConfigurationProperties.LEGEND_INSIDE", DocometreMessages.LegendInside_Title, DocometreMessages.LegendInside_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties DISPLAY_CURRENT_VALUES = new XYChartConfigurationProperties("XYChartConfigurationProperties.DISPLAY_CURRENT_VALUES", DocometreMessages.DisplayValuesCurves_Title, DocometreMessages.DisplayValuesCurves_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties SHOW_GRID = new XYChartConfigurationProperties("XYChartConfigurationProperties.SHOW_GRID", DocometreMessages.ShowGrid_Title, DocometreMessages.ShowGrid_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties SHOW_AXES = new XYChartConfigurationProperties("XYChartConfigurationProperties.SHOW_AXES", DocometreMessages.ShowAxes_Title, DocometreMessages.ShowAxes_Tooltip, "^(true|false)$", "true:false");
	public static final XYChartConfigurationProperties SHOW_TICK_LABELS = new XYChartConfigurationProperties("XYChartConfigurationProperties.SHOW_TICK_LABELS", DocometreMessages.ShowTickLabels_Title, DocometreMessages.ShowTickLabels_Tooltip, "^(true|false)$", "true:false");
	
	public static void populateProperties(XYChartConfiguration xyChartConfiguration){
		ChartConfigurationProperties.populateProperties(xyChartConfiguration);
		xyChartConfiguration.setProperty(X_MAX, "10");
		xyChartConfiguration.setProperty(X_MIN, "-10");
		xyChartConfiguration.setProperty(Y_MAX, "10");
		xyChartConfiguration.setProperty(Y_MIN, "-10");
		xyChartConfiguration.setProperty(AUTO_SCALE, "false");
		xyChartConfiguration.setProperty(FONT, FontUtil.getDefaultFontName());
		xyChartConfiguration.setProperty(FONT_BOLD, "true");
		xyChartConfiguration.setProperty(FONT_ITALIC, "false");
		xyChartConfiguration.setProperty(FONT_SIZE, "12");
		xyChartConfiguration.setProperty(HISTORY_SIZE, "1");
		xyChartConfiguration.setProperty(SHOW_LEGEND, getDefaultValue(SHOW_LEGEND));
		xyChartConfiguration.setProperty(LEGEND_INSIDE, getDefaultValue(LEGEND_INSIDE));
		xyChartConfiguration.setProperty(DISPLAY_CURRENT_VALUES, getDefaultValue(DISPLAY_CURRENT_VALUES));
		xyChartConfiguration.setProperty(SHOW_GRID, getDefaultValue(SHOW_GRID));
		xyChartConfiguration.setProperty(SHOW_AXES, getDefaultValue(SHOW_AXES));
		xyChartConfiguration.setProperty(SHOW_TICK_LABELS, getDefaultValue(SHOW_TICK_LABELS));
	}
	
	/*
	 * Default values of the display options only available with "SWT + OpenGL v2" library.
	 * They are the same as the default values of this library.
	 */
	private static String getDefaultValue(XYChartConfigurationProperties property) {
		if(property == SHOW_LEGEND || property == SHOW_GRID || property == SHOW_TICK_LABELS) return "true";
		if(property == LEGEND_INSIDE || property == DISPLAY_CURRENT_VALUES || property == SHOW_AXES) return "false";
		return null;
	}
	
	/**
	 * Returns the value of the property, or its default value if this property 
	 * is not yet defined (e.g. configuration created with a previous version).
	 */
	public static String getValue(XYChartConfiguration xyChartConfiguration, XYChartConfigurationProperties property) {
		String value = xyChartConfiguration.getProperty(property);
		return value == null ? getDefaultValue(property) : value;
	}

	public static XYChartConfiguration clone(XYChartConfiguration xyChartConfiguration) {
		return copy(xyChartConfiguration, new XYChartConfiguration());
	}
	
	/*
	 * Copies the properties and the curves of xyChartConfiguration into newXYChartConfiguration
	 */
	public static XYChartConfiguration copy(XYChartConfiguration xyChartConfiguration, XYChartConfiguration newXYChartConfiguration) {
		ChartConfigurationProperties.clone(xyChartConfiguration, newXYChartConfiguration);
		newXYChartConfiguration.setProperty(X_MAX, new String(xyChartConfiguration.getProperty(X_MAX)));
		newXYChartConfiguration.setProperty(X_MIN, new String(xyChartConfiguration.getProperty(X_MIN)));
		newXYChartConfiguration.setProperty(Y_MAX, new String(xyChartConfiguration.getProperty(Y_MAX)));
		newXYChartConfiguration.setProperty(Y_MIN, new String(xyChartConfiguration.getProperty(Y_MIN)));
		newXYChartConfiguration.setProperty(AUTO_SCALE, new String(xyChartConfiguration.getProperty(AUTO_SCALE)));
		newXYChartConfiguration.setProperty(FONT, new String(xyChartConfiguration.getProperty(FONT)));
		newXYChartConfiguration.setProperty(FONT_BOLD, new String(xyChartConfiguration.getProperty(FONT_BOLD)));
		newXYChartConfiguration.setProperty(FONT_ITALIC, new String(xyChartConfiguration.getProperty(FONT_ITALIC)));
		newXYChartConfiguration.setProperty(FONT_SIZE, new String(xyChartConfiguration.getProperty(FONT_SIZE)));
		newXYChartConfiguration.setProperty(HISTORY_SIZE, new String(xyChartConfiguration.getProperty(HISTORY_SIZE)));
		newXYChartConfiguration.setProperty(SHOW_LEGEND, new String(getValue(xyChartConfiguration, SHOW_LEGEND)));
		newXYChartConfiguration.setProperty(LEGEND_INSIDE, new String(getValue(xyChartConfiguration, LEGEND_INSIDE)));
		newXYChartConfiguration.setProperty(DISPLAY_CURRENT_VALUES, new String(getValue(xyChartConfiguration, DISPLAY_CURRENT_VALUES)));
		newXYChartConfiguration.setProperty(SHOW_GRID, new String(getValue(xyChartConfiguration, SHOW_GRID)));
		newXYChartConfiguration.setProperty(SHOW_AXES, new String(getValue(xyChartConfiguration, SHOW_AXES)));
		newXYChartConfiguration.setProperty(SHOW_TICK_LABELS, new String(getValue(xyChartConfiguration, SHOW_TICK_LABELS)));
		CurveConfiguration[] curveConfigurations = xyChartConfiguration.getCurvesConfiguration();
		for (CurveConfiguration curveConfiguration : curveConfigurations) {
			try {
				XYCurveConfiguration xyCurveConfiguration = (XYCurveConfiguration)curveConfiguration;
				newXYChartConfiguration.addCurveConfiguration((CurveConfiguration) xyCurveConfiguration.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return newXYChartConfiguration;
	}
	
	private XYChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private XYChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
