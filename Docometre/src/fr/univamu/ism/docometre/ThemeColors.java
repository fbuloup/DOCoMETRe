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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;

public final class ThemeColors {
	
	/*
	 * Colors defined in plugin.xml (light values)
	 * and css/docometre-dark.css (dark values)
	 */
	public static final String CONSOLE_WARNING_COLOR = "fr.univamu.ism.docometre.CONSOLE_WARNING_COLOR";
	public static final String CONSOLE_ERROR_COLOR = "fr.univamu.ism.docometre.CONSOLE_ERROR_COLOR";
	public static final String LINE_NUMBER_FOREGROUND_COLOR = "fr.univamu.ism.docometre.LINE_NUMBER_FOREGROUND_COLOR";
	public static final String LINE_NUMBER_BACKGROUND_COLOR = "fr.univamu.ism.docometre.LINE_NUMBER_BACKGROUND_COLOR";
	public static final String COMPLETION_BACKGROUND_COLOR = "fr.univamu.ism.docometre.COMPLETION_BACKGROUND_COLOR";
	public static final String COMPLETION_MARK_COLOR = "fr.univamu.ism.docometre.COMPLETION_MARK_COLOR";
	// Syntax colors : SYNTAX_COLOR_PREFIX + DocometreApplication color name (RED, GREEN...) + "_COLOR"
	public static final String SYNTAX_COLOR_PREFIX = "fr.univamu.ism.docometre.SYNTAX_";
	
	private static boolean darkTheme;
	
	// Eclipse dark theme colors for dialogs (DARK_BACKGROUND and DARK_FOREGROUND)
	private static final RGB DARK_BACKGROUND_RGB = new RGB(0x48, 0x48, 0x4C);
	private static final RGB DARK_FOREGROUND_RGB = new RGB(0xEE, 0xEE, 0xEE);
	
	/*
	 * Dark theme or not, as computed at startup (theme change requires restart)
	 */
	public static boolean isDarkTheme() {
		return darkTheme;
	}
	
	public static void setDarkTheme(boolean darkTheme) {
		ThemeColors.darkTheme = darkTheme;
	}
	
	/*
	 * Same rule as isDarkThemeAtStartup(), before workspace location is set :
	 * theme preference is read from workspace preferences file.
	 */
	public static boolean isDarkThemeBeforeStartup(String workspace) {
		if(workspace != null && !workspace.isEmpty()) {
			File prefsFile = new File(workspace, ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs");
			Properties properties = new Properties();
			try (FileInputStream prefsInputStream = new FileInputStream(prefsFile)) {
				properties.load(prefsInputStream);
				String themeId = properties.getProperty("themeid");
				if(themeId != null) return themeId.contains("dark");
			} catch (IOException e) {
				// No theme saved in this workspace
			}
		}
		return Display.isSystemDarkTheme();
	}
	
	/*
	 * Before workbench is started, CSS theme engine is not available :
	 * apply dark theme dialog colors to control and its children.
	 */
	public static void applyDarkColors(Control control) {
		control.setBackground(JFaceResources.getResources().createColor(DARK_BACKGROUND_RGB));
		control.setForeground(JFaceResources.getResources().createColor(DARK_FOREGROUND_RGB));
		if(control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) applyDarkColors(child);
		}
	}
	
	/*
	 * Same rule as Eclipse ThemeEngine.restore() : theme saved in preferences
	 * if any, otherwise dark theme if system is dark.
	 * Workspace location must be set before calling this method.
	 */
	public static boolean isDarkThemeAtStartup() {
		String themeId = InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme").get("themeid", null);
		if(themeId != null) return themeId.contains("dark");
		return Display.isSystemDarkTheme();
	}
	
	/*
	 * On Windows, native widgets (scroll bars, tree expanders, unfocused selection...)
	 * are dark only if SWT OS.setTheme(true) is called before they are created.
	 * OS is internal to SWT win32 fragment, hence reflection.
	 */
	public static void setWindowsNativeTheme(boolean dark) {
		if(!"win32".equals(SWT.getPlatform())) return;
		try {
			Class<?> osClass = Class.forName("org.eclipse.swt.internal.win32.OS");
			osClass.getMethod("setTheme", boolean.class).invoke(null, dark);
		} catch (ReflectiveOperationException | LinkageError e) {
			Activator.logErrorMessageWithCause(new Exception("Unable to set Windows native theme", e));
		}
	}
	
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
