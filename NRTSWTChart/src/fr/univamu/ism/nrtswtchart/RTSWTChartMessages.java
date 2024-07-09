package fr.univamu.ism.nrtswtchart;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public final class RTSWTChartMessages extends NLS {

	private static final String BUNDLE_NAME = "fr.univamu.ism.nrtswtchart.messages";//$NON-NLS-1$

	public static String ShowLegend;
	public static String Top;
	public static String Bottom;
	public static String showGrid;
	public static String AutoScale;
	public static String ShowValues;
	public static String LegendPosition;
	public static String ResetScale;
	public static String ResetXScale;
	public static String ResetYScale;
	public static String showAxis;
	public static String showCursor;

	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(Locale.of("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, RTSWTChartMessages.class);
	}

} 