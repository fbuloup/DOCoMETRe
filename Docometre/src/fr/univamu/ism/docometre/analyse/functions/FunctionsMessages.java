/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.analyse.functions;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public class FunctionsMessages extends NLS {
	
	private static final String BUNDLE_NAME = "fr.univamu.ism.docometre.analyse.functions.messages";//$NON-NLS-1$
	
	public static String Signals;
	public static String Markers;
	public static String Features;
	public static String Events;
	public static String Filtering;
	public static String Export;

	public static String TrialsList;
	
	// FILTERING functions
	public static String OrderLabel;
	public static String CutoffFrequencyLabel;
	public static String InputSignalLabel;
	public static String OutputSignalSuffixLabel;
	public static String CutOffFrequencyNotValidLabel;
	public static String TrialsListNotValidLabel;
	// Also generic
	public static String FromLabel;
	public static String ToLabel;

	// FRONT_CUT
	public static String FrontCutLabel;
	protected static String FrontCutNotValidLabel;
	
	// END_CUT
	public static String EndCutLabel;
	protected static String EndCutNotValidLabel;

	// TIME_MARKER
	public static String TimeValueLabel;
	public static String MarkersGroupLabel;

	// Motion Distance
	public static String FromMarkerLabel;
	public static String ToMarkerLabel;

	// Find amplitude back and forward
	public static String AmplitudeValueLabel;
	public static String fabFromMarkerLabel;
	public static String fabToMarkerLabel;
	public static String fafFromMarkerLabel;
	public static String fafToMarkerLabel;
	
	// Export markers
	public static String SelectAll;
	public static String UnselectAll;
	public static String FilterLabel;
	public static String SeparatorLabel;
	
	// Interpolate 1D
	public static String tMinLabel;
	public static String tMaxLabel;
	public static String methodLabel;
	public static String tMinLabelTooltip;
	public static String tMaxLabelTooltip;
	
	// Change sample frequency
	public static String InputSignalsLabel;
	public static String NewSampleFrequencyValueLabel;

	// Threshold function
	public static String ThresholdLabel;
	public static String BypassTimeLabel;
	public static String BypassTimeTooltip;

	// Replace samples function
	public static String FromIndexLabel;
	public static String ToIndexLabel;
	public static String ReplacementIndexLabel;
	
	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(new Locale("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, FunctionsMessages.class);
	}

}
