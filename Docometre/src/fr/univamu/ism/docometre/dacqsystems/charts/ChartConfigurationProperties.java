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

import java.util.regex.Pattern;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ChartConfigurationProperties extends Property {

	public static final ChartConfigurationProperties ADD_CURVE = new ChartConfigurationProperties("ChartConfigurationProperties.ADD_CURVE", "", "", "");
	public static final ChartConfigurationProperties REMOVE_CURVE = new ChartConfigurationProperties("ChartConfigurationProperties.REMOVE_CURVE", "", "", "");
	public static final ChartConfigurationProperties UNTRANSFERED_CHANNEL = new ChartConfigurationProperties("ChartConfigurationProperties.UNTRANSFERED_CHANNEL", "", "", "");
	public static final ChartConfigurationProperties HORIZONTAL_SPANNING = new ChartConfigurationProperties("ChartConfigurationProperties.HORIZONTAL_SPANNING", DocometreMessages.HSpan_Label, DocometreMessages.HSpan_Tooltip, "^(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final ChartConfigurationProperties VERTICAL_SPANNING = new ChartConfigurationProperties("ChartConfigurationProperties.VERTICAL_SPANNING", DocometreMessages.VSpan_Label, DocometreMessages.VSpan_Tooltip, "^-?(([1-9][0-9]*)|(0))(?:\\.[0-9]+)?$");
	public static final ChartConfigurationProperties COLOR = new ChartConfigurationProperties("CurveConfigurationProperties.COLOR", DocometreMessages.Color_Label, DocometreMessages.Color_Tooltip, "^RGB\\s*\\{\\s*([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\s*,\\s*([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\s*,\\s*([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\}$");
	
	public static void populateProperties(ChartConfiguration chartConfiguration){
		chartConfiguration.setProperty(HORIZONTAL_SPANNING, "1");
		chartConfiguration.setProperty(VERTICAL_SPANNING, "1");
	}
	
	public static Color getColor(AbstractElement chartConfiguration, Property property) {
		String colorKey = chartConfiguration.getProperty(property);
		if(colorKey == null) colorKey = "RGB{255,255,255}";
		colorKey = colorKey.trim();
		boolean matches = Pattern.matches(COLOR.getRegExp(), colorKey);
		if(!matches) colorKey = "RGB{255,255,255}";
		Color color = JFaceResources.getColorRegistry().get(colorKey);
		if(color == null) {
			String value = colorKey.replaceAll("RGB", "");
			value = value.replaceAll("\\{", "");
			value = value.replaceAll("\\}", "");
			value = value.replaceAll("\\s+", "");
			String[] RGB = value.split(",");
			int red = Integer.parseInt(RGB[0]);
			int green = Integer.parseInt(RGB[1]);
			int blue = Integer.parseInt(RGB[2]);
			color = new Color(PlatformUI.getWorkbench().getDisplay(), red, green, blue);
			JFaceResources.getColorRegistry().put(colorKey, new RGB(red, green, blue));
		}
		return color;
	}

	public static void clone(ChartConfiguration chartConfiguration, ChartConfiguration newChartConfiguration) {
		newChartConfiguration.setProperty(HORIZONTAL_SPANNING, new String(chartConfiguration.getProperty(HORIZONTAL_SPANNING)));
		newChartConfiguration.setProperty(VERTICAL_SPANNING, new String(chartConfiguration.getProperty(VERTICAL_SPANNING)));
		newChartConfiguration.setProperty(COLOR, new String(chartConfiguration.getProperty(COLOR)));
	}
	
	private ChartConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ChartConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
