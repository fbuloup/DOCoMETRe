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

public final class OscilloChartConfigurationProperties extends Property {
	
	public static final OscilloChartConfigurationProperties TIME_WIDTH = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.TIME_WIDTH", DocometreMessages.TimeWidth_Title, DocometreMessages.TimeWidth_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final OscilloChartConfigurationProperties Y_MAX = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.Y_MAX", DocometreMessages.MaxAmplitude_Title, DocometreMessages.MaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final OscilloChartConfigurationProperties Y_MIN = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.Y_MIN", DocometreMessages.MinAmplitude_Title, DocometreMessages.MinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final OscilloChartConfigurationProperties AUTO_SCALE = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.AUTO_SCALE", DocometreMessages.AutoScale_Title, DocometreMessages.AutoScale_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties FONT = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.FONT", DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip, FontUtil.getAvailableFontsNames(true), FontUtil.getAvailableFontsNames(false));
	public static final OscilloChartConfigurationProperties DISPLAY_CURRENT_VALUES = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES", DocometreMessages.DisplayValues_Title, DocometreMessages.DisplayValues_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties FONT_BOLD = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.FONT_BOLD", DocometreMessages.Bold_Title, DocometreMessages.Bold_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties FONT_ITALIC = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.FONT_ITALIC", DocometreMessages.Italic_Title, DocometreMessages.Italic_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties FONT_SIZE = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.FONT_SIZE", DocometreMessages.Size_Title,  DocometreMessages.Size_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	// Display options only available with "SWT + OpenGL v2" library
	public static final OscilloChartConfigurationProperties SHOW_LEGEND = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.SHOW_LEGEND", DocometreMessages.ShowLegend_Title, DocometreMessages.ShowLegend_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties LEGEND_INSIDE = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.LEGEND_INSIDE", DocometreMessages.LegendInside_Title, DocometreMessages.LegendInside_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties SHOW_GRID = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.SHOW_GRID", DocometreMessages.ShowGrid_Title, DocometreMessages.ShowGrid_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties SHOW_AXES = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.SHOW_AXES", DocometreMessages.ShowAxes_Title, DocometreMessages.ShowAxes_Tooltip, "^(true|false)$", "true:false");
	public static final OscilloChartConfigurationProperties SHOW_TICK_LABELS = new OscilloChartConfigurationProperties("OscilloChartConfigurationProperties.SHOW_TICK_LABELS", DocometreMessages.ShowTickLabels_Title, DocometreMessages.ShowTickLabels_Tooltip, "^(true|false)$", "true:false");
	
	public static void populateProperties(OscilloChartConfiguration oscilloChartConfiguration){
		ChartConfigurationProperties.populateProperties(oscilloChartConfiguration);
		oscilloChartConfiguration.setProperty(TIME_WIDTH, "10");
		oscilloChartConfiguration.setProperty(Y_MAX, "10");
		oscilloChartConfiguration.setProperty(Y_MIN, "-10");
		oscilloChartConfiguration.setProperty(AUTO_SCALE, "false");
		oscilloChartConfiguration.setProperty(FONT, FontUtil.getDefaultFontName());
		oscilloChartConfiguration.setProperty(DISPLAY_CURRENT_VALUES, "false");
		oscilloChartConfiguration.setProperty(FONT_BOLD, "true");
		oscilloChartConfiguration.setProperty(FONT_ITALIC, "false");
		oscilloChartConfiguration.setProperty(FONT_SIZE, "12");
		oscilloChartConfiguration.setProperty(SHOW_LEGEND, getDefaultValue(SHOW_LEGEND));
		oscilloChartConfiguration.setProperty(LEGEND_INSIDE, getDefaultValue(LEGEND_INSIDE));
		oscilloChartConfiguration.setProperty(SHOW_GRID, getDefaultValue(SHOW_GRID));
		oscilloChartConfiguration.setProperty(SHOW_AXES, getDefaultValue(SHOW_AXES));
		oscilloChartConfiguration.setProperty(SHOW_TICK_LABELS, getDefaultValue(SHOW_TICK_LABELS));
	}
	
	/*
	 * Default values of the display options only available with "SWT + OpenGL v2" library.
	 * They are the same as the default values of this library.
	 */
	private static String getDefaultValue(OscilloChartConfigurationProperties property) {
		if(property == SHOW_LEGEND || property == SHOW_GRID || property == SHOW_TICK_LABELS) return "true";
		if(property == LEGEND_INSIDE || property == SHOW_AXES) return "false";
		return null;
	}
	
	/**
	 * Returns the value of the property, or its default value if this property 
	 * is not yet defined (e.g. configuration created with a previous version).
	 */
	public static String getValue(OscilloChartConfiguration oscilloChartConfiguration, OscilloChartConfigurationProperties property) {
		String value = oscilloChartConfiguration.getProperty(property);
		return value == null ? getDefaultValue(property) : value;
	}

	public static OscilloChartConfiguration clone(OscilloChartConfiguration oscilloChartConfiguration) {
		OscilloChartConfiguration newOscilloChartConfiguration = new OscilloChartConfiguration();
		ChartConfigurationProperties.clone(oscilloChartConfiguration, newOscilloChartConfiguration);
		newOscilloChartConfiguration.setProperty(TIME_WIDTH, new String(oscilloChartConfiguration.getProperty(TIME_WIDTH)));
		newOscilloChartConfiguration.setProperty(Y_MAX, new String(oscilloChartConfiguration.getProperty(Y_MAX)));
		newOscilloChartConfiguration.setProperty(Y_MIN, new String(oscilloChartConfiguration.getProperty(Y_MIN)));
		newOscilloChartConfiguration.setProperty(AUTO_SCALE, new String(oscilloChartConfiguration.getProperty(AUTO_SCALE)));
		newOscilloChartConfiguration.setProperty(FONT, new String(oscilloChartConfiguration.getProperty(FONT)));
		newOscilloChartConfiguration.setProperty(DISPLAY_CURRENT_VALUES, new String(oscilloChartConfiguration.getProperty(DISPLAY_CURRENT_VALUES)));
		if(oscilloChartConfiguration.getProperty(FONT_BOLD) == null) newOscilloChartConfiguration.setProperty(FONT_BOLD, "true");
		else newOscilloChartConfiguration.setProperty(FONT_BOLD, new String(oscilloChartConfiguration.getProperty(FONT_BOLD)));
		if(oscilloChartConfiguration.getProperty(FONT_ITALIC) == null) oscilloChartConfiguration.setProperty(FONT_ITALIC, "false");
		else newOscilloChartConfiguration.setProperty(FONT_ITALIC, new String(oscilloChartConfiguration.getProperty(FONT_ITALIC)));
		if(oscilloChartConfiguration.getProperty(FONT_SIZE) == null) oscilloChartConfiguration.setProperty(FONT_SIZE, "12");
		else newOscilloChartConfiguration.setProperty(FONT_SIZE, new String(oscilloChartConfiguration.getProperty(FONT_SIZE)));
		newOscilloChartConfiguration.setProperty(SHOW_LEGEND, new String(getValue(oscilloChartConfiguration, SHOW_LEGEND)));
		newOscilloChartConfiguration.setProperty(LEGEND_INSIDE, new String(getValue(oscilloChartConfiguration, LEGEND_INSIDE)));
		newOscilloChartConfiguration.setProperty(SHOW_GRID, new String(getValue(oscilloChartConfiguration, SHOW_GRID)));
		newOscilloChartConfiguration.setProperty(SHOW_AXES, new String(getValue(oscilloChartConfiguration, SHOW_AXES)));
		newOscilloChartConfiguration.setProperty(SHOW_TICK_LABELS, new String(getValue(oscilloChartConfiguration, SHOW_TICK_LABELS)));
		CurveConfiguration[] curveConfigurations = oscilloChartConfiguration.getCurvesConfiguration();
		for (CurveConfiguration curveConfiguration : curveConfigurations) {
			try {
				OscilloCurveConfiguration oscilloCurveConfiguration = (OscilloCurveConfiguration)curveConfiguration;
				newOscilloChartConfiguration.addCurveConfiguration((CurveConfiguration) oscilloCurveConfiguration.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return newOscilloChartConfiguration;
	}
	
	private OscilloChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private OscilloChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
