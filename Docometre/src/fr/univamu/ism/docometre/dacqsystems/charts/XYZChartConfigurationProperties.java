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

public final class XYZChartConfigurationProperties extends Property {
	
	public static final XYZChartConfigurationProperties X_MAX = new XYZChartConfigurationProperties("XYChartConfigurationProperties.X_MAX", DocometreMessages.xMaxAmplitude_Title, DocometreMessages.xMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties X_MIN = new XYZChartConfigurationProperties("XYChartConfigurationProperties.X_MIN", DocometreMessages.xMinAmplitude_Title, DocometreMessages.xMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties Y_MAX = new XYZChartConfigurationProperties("XYChartConfigurationProperties.Y_MAX", DocometreMessages.yMaxAmplitude_Title, DocometreMessages.yMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties Y_MIN = new XYZChartConfigurationProperties("XYChartConfigurationProperties.Y_MIN", DocometreMessages.yMinAmplitude_Title, DocometreMessages.yMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties Z_MAX = new XYZChartConfigurationProperties("XYChartConfigurationProperties.Z_MAX", DocometreMessages.zMaxAmplitude_Title, DocometreMessages.zMaxAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties Z_MIN = new XYZChartConfigurationProperties("XYChartConfigurationProperties.Z_MIN", DocometreMessages.zMinAmplitude_Title, DocometreMessages.zMinAmplitude_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties AUTO_SCALE = new XYZChartConfigurationProperties("XYChartConfigurationProperties.AUTO_SCALE", DocometreMessages.AutoScale_Title, DocometreMessages.AutoScale_Tooltip, "^(true|false)$", "true:false");
	public static final XYZChartConfigurationProperties FONT = new XYZChartConfigurationProperties("XYChartConfigurationProperties.FONT", DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip, FontUtil.getAvailableFontsNames(true), FontUtil.getAvailableFontsNames(false));
	public static final XYZChartConfigurationProperties FONT_BOLD = new XYZChartConfigurationProperties("XYChartConfigurationProperties.FONT_BOLD", DocometreMessages.Bold_Title, DocometreMessages.Bold_Tooltip, "^(true|false)$", "true:false");
	public static final XYZChartConfigurationProperties FONT_ITALIC = new XYZChartConfigurationProperties("XYChartConfigurationProperties.FONT_ITALIC", DocometreMessages.Italic_Title, DocometreMessages.Italic_Tooltip, "^(true|false)$", "true:false");
	public static final XYZChartConfigurationProperties FONT_SIZE = new XYZChartConfigurationProperties("XYChartConfigurationProperties.FONT_SIZE", DocometreMessages.Size_Title, DocometreMessages.Size_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final XYZChartConfigurationProperties HISTORY_SIZE = new XYZChartConfigurationProperties("XYChartConfigurationProperties.HISTORY_SIZE", DocometreMessages.HistorySize_Title, DocometreMessages.HistorySize_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	
	public static void populateProperties(XYZChartConfiguration xyzChartConfiguration){
		ChartConfigurationProperties.populateProperties(xyzChartConfiguration);
		xyzChartConfiguration.setProperty(X_MAX, "10");
		xyzChartConfiguration.setProperty(X_MIN, "-10");
		xyzChartConfiguration.setProperty(Y_MAX, "10");
		xyzChartConfiguration.setProperty(Y_MIN, "-10");
		xyzChartConfiguration.setProperty(Z_MAX, "10");
		xyzChartConfiguration.setProperty(Z_MIN, "-10");
		xyzChartConfiguration.setProperty(AUTO_SCALE, "false");
		xyzChartConfiguration.setProperty(FONT, FontUtil.getDefaultFontName());
		xyzChartConfiguration.setProperty(FONT_BOLD, "true");
		xyzChartConfiguration.setProperty(FONT_ITALIC, "false");
		xyzChartConfiguration.setProperty(FONT_SIZE, "12");
		xyzChartConfiguration.setProperty(HISTORY_SIZE, "1");
	}

	public static XYZChartConfiguration clone(XYZChartConfiguration xyzChartConfiguration) {
		XYZChartConfiguration newXYZChartConfiguration = new XYZChartConfiguration();
		ChartConfigurationProperties.clone(xyzChartConfiguration, newXYZChartConfiguration);
		newXYZChartConfiguration.setProperty(X_MAX, new String(xyzChartConfiguration.getProperty(Y_MAX)));
		newXYZChartConfiguration.setProperty(X_MIN, new String(xyzChartConfiguration.getProperty(Y_MIN)));
		newXYZChartConfiguration.setProperty(Y_MAX, new String(xyzChartConfiguration.getProperty(Y_MAX)));
		newXYZChartConfiguration.setProperty(Y_MIN, new String(xyzChartConfiguration.getProperty(Y_MIN)));
		newXYZChartConfiguration.setProperty(Z_MAX, new String(xyzChartConfiguration.getProperty(Z_MAX)));
		newXYZChartConfiguration.setProperty(Z_MIN, new String(xyzChartConfiguration.getProperty(Z_MIN)));
		newXYZChartConfiguration.setProperty(AUTO_SCALE, new String(xyzChartConfiguration.getProperty(AUTO_SCALE)));
		newXYZChartConfiguration.setProperty(FONT, new String(xyzChartConfiguration.getProperty(FONT)));
		newXYZChartConfiguration.setProperty(FONT_BOLD, new String(xyzChartConfiguration.getProperty(FONT_BOLD)));
		newXYZChartConfiguration.setProperty(FONT_ITALIC, new String(xyzChartConfiguration.getProperty(FONT_ITALIC)));
		newXYZChartConfiguration.setProperty(FONT_SIZE, new String(xyzChartConfiguration.getProperty(FONT_SIZE)));
		newXYZChartConfiguration.setProperty(HISTORY_SIZE, new String(xyzChartConfiguration.getProperty(HISTORY_SIZE)));
		CurveConfiguration[] curveConfigurations = xyzChartConfiguration.getCurvesConfiguration();
		for (CurveConfiguration curveConfiguration : curveConfigurations) {
			try {
				XYZCurveConfiguration xyzCurveConfiguration = (XYZCurveConfiguration)curveConfiguration;
				newXYZChartConfiguration.addCurveConfiguration((CurveConfiguration) xyzCurveConfiguration.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return newXYZChartConfiguration;
	}
	
	private XYZChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private XYZChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
