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
package fr.univamu.ism.rtswtchart;

public enum RTSWTChartFonts {
	
	STROKE_ROMAN("Stroke Roman", 0),
	STROKE_MONO_ROMAN("Stroke Mono Roman", 1),
	BITMAP_8_BY_13("Bitmap 8x13", 3),
	BITMAP_9_BY_15("Bitmap 9x15", 2),
	BITMAP_TIMES_ROMAN_10("Times Roman 10", 4),
	BITMAP_TIMES_ROMAN_24("Times Roman 24", 5),
	BITMAP_HELVETICA_10("Helvetica 10", 6),
	BITMAP_HELVETICA_12("Helvetica 12", 7),
	BITMAP_HELVETICA_18("Helvetica 18", 8);
	
	private String label;
	private int value;
	
	private RTSWTChartFonts(String label, int value) {
		this.label = label;
		this.value = value;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getValue() {
		return value;
	}
	
	public static String getRegExp() {
		String expression = "^(";
		expression = expression + STROKE_ROMAN.label + "|";
		expression = expression + STROKE_MONO_ROMAN.label + "|";
		expression = expression + BITMAP_8_BY_13.label + "|";
		expression = expression + BITMAP_9_BY_15.label + "|";
		expression = expression + BITMAP_TIMES_ROMAN_10.label + "|";
		expression = expression + BITMAP_TIMES_ROMAN_24.label + "|";
		expression = expression + BITMAP_HELVETICA_10.label + "|";
		expression = expression + BITMAP_HELVETICA_12.label + "|";
		expression = expression + BITMAP_HELVETICA_18.label;
		expression = expression +  ")$";
		return expression;
	}
	
	public static String getAvailableValues() {
		String expression = STROKE_ROMAN.label + ":";
		expression = expression + STROKE_MONO_ROMAN.label + ":";
		expression = expression + BITMAP_8_BY_13.label + ":";
		expression = expression + BITMAP_9_BY_15.label + ":";
		expression = expression + BITMAP_TIMES_ROMAN_10.label + ":";
		expression = expression + BITMAP_TIMES_ROMAN_24.label + ":";
		expression = expression + BITMAP_HELVETICA_10.label + ":";
		expression = expression + BITMAP_HELVETICA_12.label + ":";
		expression = expression + BITMAP_HELVETICA_18.label;
		return expression;
	}
	
	public static RTSWTChartFonts getFont(String fontKey) {
		if(STROKE_ROMAN.label.equals(fontKey)) return STROKE_ROMAN;
		if(STROKE_MONO_ROMAN.label.equals(fontKey)) return STROKE_MONO_ROMAN;
		if(BITMAP_8_BY_13.label.equals(fontKey)) return BITMAP_8_BY_13;
		if(BITMAP_9_BY_15.label.equals(fontKey)) return BITMAP_9_BY_15;
		if(BITMAP_TIMES_ROMAN_10.label.equals(fontKey)) return BITMAP_TIMES_ROMAN_10;
		if(BITMAP_TIMES_ROMAN_24.label.equals(fontKey)) return BITMAP_TIMES_ROMAN_24;
		if(BITMAP_HELVETICA_10.label.equals(fontKey)) return BITMAP_HELVETICA_10;
		if(BITMAP_HELVETICA_12.label.equals(fontKey)) return BITMAP_HELVETICA_12;
		return BITMAP_HELVETICA_18;
	}

}
