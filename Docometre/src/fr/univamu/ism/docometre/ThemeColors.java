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
package fr.univamu.ism.docometre;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;

public final class ThemeColors {
	
	/*
	 * Colors defined in Docometre.theme plugin.xml (light values)
	 * and css/docometre-dark.css (dark values)
	 */
	public static final String CONSOLE_INFO_COLOR = "fr.univamu.ism.docometre.CONSOLE_INFO_COLOR";
	public static final String CONSOLE_WARNING_COLOR = "fr.univamu.ism.docometre.CONSOLE_WARNING_COLOR";
	public static final String CONSOLE_ERROR_COLOR = "fr.univamu.ism.docometre.CONSOLE_ERROR_COLOR";
	public static final String LINE_NUMBER_FOREGROUND_COLOR = "fr.univamu.ism.docometre.LINE_NUMBER_FOREGROUND_COLOR";
	public static final String LINE_NUMBER_BACKGROUND_COLOR = "fr.univamu.ism.docometre.LINE_NUMBER_BACKGROUND_COLOR";
	
	/*
	 * Return RGB of color symbolicName from current theme, or defaultRGB if not defined
	 */
	public static RGB getRGB(String symbolicName, RGB defaultRGB) {
		if(!PlatformUI.isWorkbenchRunning()) return defaultRGB;
		ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		RGB rgb = colorRegistry.getRGB(symbolicName);
		return rgb != null ? rgb : defaultRGB;
	}
	
	/*
	 * Return color symbolicName from current theme, or defaultRGB color if not defined
	 */
	public static Color getColor(String symbolicName, RGB defaultRGB) {
		return JFaceResources.getResources().createColor(getRGB(symbolicName, defaultRGB));
	}
	
	public static Color getBackgroundColor() {
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		return themeManager.getCurrentTheme().getColorRegistry().get("background");
	}
	
	public static Color getForegroundColor() {
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		return themeManager.getCurrentTheme().getColorRegistry().get("foreground");
	}

}
