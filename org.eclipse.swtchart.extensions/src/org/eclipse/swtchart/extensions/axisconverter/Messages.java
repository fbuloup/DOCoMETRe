package org.eclipse.swtchart.extensions.axisconverter;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String MillisecondsToScanNumberConverter_Delay;
	public static String MillisecondsToScanNumberConverter_Interval;
	
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
