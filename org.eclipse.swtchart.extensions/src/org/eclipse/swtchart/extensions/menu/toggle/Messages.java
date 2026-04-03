package org.eclipse.swtchart.extensions.menu.toggle;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String ToggleAxisZeroMarkerHandler_0;
	public static String ToggleLabelTooltipsHandler_0;
	public static String ToggleLegendMarkerHandler_0;
	public static String TogglePlotCenterMarkerHandler_0;
	public static String TogglePositionMarkerHandler_0;
	public static String ToggleRangeSelectorHandler_0;
	public static String ToggleSeriesLabelMarkerHandler_0;
	public static String ToggleSeriesLegendHandler_0;
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
