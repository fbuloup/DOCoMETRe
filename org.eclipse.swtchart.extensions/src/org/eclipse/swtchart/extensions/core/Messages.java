package org.eclipse.swtchart.extensions.core;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	
	public static String AbstractAxisSettings_5;
	public static String AbstractExtendedChart_0;
	public static String AbstractExtendedChart_1;
	public static String AbstractExtendedChart_3;
	public static String BaseChart_0;
	public static String BaseChart_1;
	public static String BaseChart_2;
	public static String BaseChart_3;
	public static String ChartSettings_CantSerVerticalSliderTrue;
	public static String ChartSettings_ChartTitle;
	public static String RangeSelector_11;
	public static String RangeSelector_12;
	public static String RangeSelector_5;
	public static String RangeSelector_6;
	public static String RangeSelector_8;
	public static String RangeSelector_9;
	public static String ScrollableChart_2;

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
