package fr.univamu.ism.docometre;

import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;

public final class ThemeColors {
	
	public static Color getBackgroundColor() {
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		return themeManager.getCurrentTheme().getColorRegistry().get("background");
	}
	
	public static Color getForegroundColor() {
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		return themeManager.getCurrentTheme().getColorRegistry().get("foreground");
	}

}
