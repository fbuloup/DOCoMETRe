package org.eclipse.swtchart.extensions.menu;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String IChartMenuCategories_1;
	public static String IChartMenuCategories_2;
	public static String IChartMenuCategories_3;
	public static String RedoSelectionHandler_0;
	public static String ResetChartHandler_0;
	public static String ResetSelectedSeriesHandler_0;
	public static String UndoSelectionHandler_0;
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
