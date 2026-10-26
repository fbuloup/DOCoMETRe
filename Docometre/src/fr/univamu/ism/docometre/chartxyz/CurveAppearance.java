package fr.univamu.ism.docometre.chartxyz;

/**
 * Appearance of a curve used by the rendering helpers (internal): name
 * displayed in the legend, line width and line style. Implemented by
 * {@link CurveData}; the color is read with {@link CurveStyles#colorOf}.
 */
interface CurveAppearance {

    /** Name displayed in the legend. */
    String getName();

    /** Line width in pixels, or 0 for the default line width of the chart. */
    float getLineWidth();

    /** Line style (solid, dashed...). */
    LineStyle getLineStyle();
}
