package org.eclipse.swtchart.extensions.menu.export;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String BMPExportHandler_3;
	public static String CSVExportHandler_1;
	public static String CSVExportHandler_3;
	public static String ExportSettingsDialog_0;
	public static String ExportSettingsDialog_1;
	public static String ExportSettingsDialog_2;
	public static String ExportSettingsDialog_3;
	public static String ExportSettingsDialog_4;
	public static String ExportSettingsDialog_5;
	public static String ExportSettingsDialog_6;
	public static String ISeriesExportConverter_0;
	public static String ISeriesExportConverter_1;
	public static String JPGExportHandler_3;
	public static String LaTeXTableExportHandler_1;
	public static String LaTeXTableExportHandler_3;
	public static String PNGExportHandler_3;
	public static String PrinterExportHandler_0;
	public static String PrinterExportHandler_1;
	public static String RScriptExportHandler_0;
	public static String RScriptExportHandler_3;
	public static String TSVExportHandler_1;
	public static String TSVExportHandler_3;
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
