package org.eclipse.swtchart.extensions.linecharts;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	
	public static String LineChart_0;
	public static String LineChart_1;
	public static String LineChart_2;
	public static String LineChart_3;
	public static String LineChart_4;
	public static String LineChart_5;
	
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
