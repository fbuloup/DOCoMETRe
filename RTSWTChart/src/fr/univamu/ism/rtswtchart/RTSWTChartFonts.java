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
