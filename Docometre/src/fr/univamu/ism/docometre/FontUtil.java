package fr.univamu.ism.docometre;

import java.util.TreeSet;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.rtswtchart.RTSWTChartFonts;

public final class FontUtil {
	
	private static FontData[] getFontList() {
		return Display.getDefault().getFontList(null, true);
	}
	
	public static String[] getAvailableFontsNames() {
		if(Activator.useOpenGLV1()) {
			return RTSWTChartFonts.getAvailableFontsNames();
		} else {
			FontData[] fontDatas = getFontList();
			TreeSet<String> fontsNames = new TreeSet<String>();
			for (FontData currentFontData : fontDatas) {
				fontsNames.add(currentFontData.getName());
			}
			return fontsNames.toArray(new String[fontsNames.size()]);
		}
	}
	
	public static String getAvailableFontsNames(boolean forRegExp) {
		if(Activator.useOpenGLV1()) {
			if(forRegExp) return RTSWTChartFonts.getRegExp();
			return RTSWTChartFonts.getAvailableValues();
		} else {
			FontData[] fontDatas = getFontList();
			TreeSet<String> fontsNames = new TreeSet<String>();
			for (FontData currentFontData : fontDatas) {
				fontsNames.add(currentFontData.getName());
			}
			if(forRegExp) return "^(" + String.join("|", fontsNames.toArray(new String[fontsNames.size()])) + ")$";
			return String.join(":", fontsNames.toArray(new String[fontsNames.size()]));
		}
		
	}
	
	public static String getDefaultFontName() {
		if(Activator.useOpenGLV1()) {
			return RTSWTChartFonts.getAvailableFontsNames()[0];
		} else {
			FontData[] fontDatas = getFontList();
			return fontDatas[0].getName();
		}
	}

}
