package fr.univamu.ism.docometre.chartxyz;

/**
 * Line style of a curve. The patterns are drawn by {@link DashedLines},
 * anchored on the data (they do not slide along a real-time curve), and
 * scaled with the line width: dashes and dots keep the same proportions
 * whatever the width.
 */
public enum LineStyle {

    /** Solid line. */
    SOLID(null),
    /** Dashes:             ________  ________  ________ */
    DASH("1111111100000000"),
    /** Dash-dot:           ________ .  ________ .  ____ */
    DASH_DOT("1111111100011000"),
    /** Dash-dash-dot:      ____ ____ .  ____ ____ .  __ */
    DASH_DASH_DOT("1111001111001000");

    /** Number of pixels represented by one character of the pattern, for a 1-pixel line. */
    static final int BASE_FACTOR = 2;

    private final short pattern;

    /**
     * @param pattern 16-character pattern ('1' = on, '0' = off), read from
     *                left to right in the drawing direction; null = solid line
     */
    LineStyle(String pattern) {
        short bits = 0;
        if (pattern != null) {
            // The pattern is read from the least significant bit
            for (int i = 0; i < 16; i++) {
                if (pattern.charAt(i) == '1') bits |= (short) (1 << i);
            }
        }
        this.pattern = bits;
    }

    /** 16-bit pattern, bit 0 being the first one drawn (not applicable to SOLID). */
    public short getPattern() { return pattern; }

    public boolean isSolid() { return this == SOLID; }
}
