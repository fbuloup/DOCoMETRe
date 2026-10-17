package fr.univamu.ism.docometre;

import java.util.TreeSet;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.rtswtchart.RTSWTChartFonts;

public final class FontUtil {
	
	private static FontData[] getFontList() {
		return Display.getDefault().getFontList(null, true);
	}
	
	public static String[] getAvailableFontsNames() {
		// For backward compatibility
		String defaultValue = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		if("".equals(defaultValue)) {
			boolean useOpenGL = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_OPENGL_FOR_RTSWT_CHART);
			if(useOpenGL) Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1);
			else Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT);
		}
		String rtSWTChartLibrary = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		boolean useOpenGL_V1 = GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1.equals(rtSWTChartLibrary);
		if(useOpenGL_V1) {
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
		// For backward compatibility
		String defaultValue = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		if("".equals(defaultValue)) {
			boolean useOpenGL = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_OPENGL_FOR_RTSWT_CHART);
			if(useOpenGL) Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1);
			else Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT);
		}
		String rtSWTChartLibrary = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		boolean useOpenGL_V1 = GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1.equals(rtSWTChartLibrary);
		if(useOpenGL_V1) {
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
		// For backward compatibility
		String defaultValue = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		if("".equals(defaultValue)) {
			boolean useOpenGL = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_OPENGL_FOR_RTSWT_CHART);
			if(useOpenGL) Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1);
			else Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY, GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT);
		}
		String rtSWTChartLibrary = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.RTSWT_CHART_LIBRARY);
		boolean useOpenGL_V1 = GeneralPreferenceConstants.RTSWT_CHART_LIBRARY_SWT_OPENGL_V1.equals(rtSWTChartLibrary);
		if(useOpenGL_V1) {
			return RTSWTChartFonts.getAvailableFontsNames()[0];
		} else {
			FontData[] fontDatas = getFontList();
			return fontDatas[0].getName();
		}
	}

}
