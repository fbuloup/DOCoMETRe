package org.eclipse.swtchart.extensions.properties;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String AxisPage_10;
	public static String AxisPage_11;
	public static String AxisPage_12;
	public static String AxisPage_13;
	public static String AxisPage_14;
	public static String AxisPage_2;
	public static String AxisPage_3;
	public static String AxisPage_4;
	public static String AxisPage_5;
	public static String AxisPage_6;
	public static String AxisPage_7;
	public static String AxisPage_8;
	public static String AxisPage_9;
	public static String AxisTickPage_2;
	public static String AxisTickPage_3;
	public static String AxisTickPage_4;
	public static String AxisTickPage_5;
	public static String ChartPage_10;
	public static String ChartPage_11;
	public static String ChartPage_12;
	public static String ChartPage_4;
	public static String ChartPage_5;
	public static String ChartPage_6;
	public static String ChartPage_7;
	public static String ChartPage_8;
	public static String ChartPage_9;
	public static String GridPage_1;
	public static String GridPage_2;
	public static String GridPage_3;
	public static String LegendPage_3;
	public static String LegendPage_4;
	public static String LegendPage_5;
	public static String LegendPage_6;
	public static String SeriesLabelPage_2;
	public static String SeriesLabelPage_3;
	public static String SeriesLabelPage_4;
	public static String SeriesLabelPage_5;
	public static String SeriesPage_10;
	public static String SeriesPage_12;
	public static String SeriesPage_13;
	public static String SeriesPage_14;
	public static String SeriesPage_15;
	public static String SeriesPage_16;
	public static String SeriesPage_17;
	public static String SeriesPage_18;
	public static String SeriesPage_19;
	public static String SeriesPage_20;
	public static String SeriesPage_3;
	public static String SeriesPage_6;
	public static String SeriesPage_7;
	public static String SeriesPage_8;
	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(Locale.of("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, Messages.class);
	}

	private Messages() {
	}
}
