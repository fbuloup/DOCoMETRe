package fr.univamu.ism.docometre.chartxyz;

/**
 * Appearance of a curve in a chart with several curves: name displayed in
 * the legend, color, line width and line style. Implemented by
 * {@link Curve1DData}, {@link Curve2DData} and {@link Curve3DData}.
 */
public interface CurveAppearance {

    /** Name displayed in the legend, or null. */
    String getName();

    /** RGB color (components between 0 and 1), or null for an automatic color. */
    float[] getColor();

    /** Line width in pixels, or 0 for the default line width of the chart. */
    float getLineWidth();

    /** Line style (solid, dashed...). */
    LineStyle getLineStyle();
}
