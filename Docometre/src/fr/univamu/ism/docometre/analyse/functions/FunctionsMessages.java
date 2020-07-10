package fr.univamu.ism.docometre.analyse.functions;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class FunctionsMessages extends NLS {
	
	private static final String BUNDLE_NAME = "fr.univamu.ism.docometre.analyse.functions.messages";//$NON-NLS-1$

	public static String OrderLabel;
	public static String CutoffFrequencyLabel;
	public static String InputSignalLabel;
	public static String OutputSignalSuffixLabel;
	public static String CutOffFrequencyNotValidLabel;
	public static String TrialsListNotValidLabel;
	
	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(new Locale("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, FunctionsMessages.class);
	}

}
