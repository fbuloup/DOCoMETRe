package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;

/**
 * Default curve colors and legend drawing, shared by the 1D, 2D and 3D
 * renderers.
 */
public final class CurveStyles {

    /** Palette readable on a dark background, used in the order the curves are added. */
    private static final float[][] PALETTE = {
            {0.35f, 0.75f, 1.00f}, // blue
            {1.00f, 0.60f, 0.20f}, // orange
            {0.45f, 0.90f, 0.45f}, // green
            {0.95f, 0.40f, 0.50f}, // pinkish red
            {0.75f, 0.55f, 1.00f}, // purple
            {1.00f, 0.90f, 0.30f}, // yellow
            {0.30f, 0.90f, 0.90f}, // cyan
            {0.90f, 0.90f, 0.90f}, // light gray
    };

    private static final int LEGEND_SWATCH_WIDTH = 36;  // pixels (long enough to show the line pattern)

    private CurveStyles() {
    }

    /**
     * Checks scale bounds {min1, max1, min2, max2...}: each pair must be
     * finite with min < max. Returns a copy.
     */
    public static double[] checkScales(double... bounds) {
        for (int i = 0; i < bounds.length; i += 2) {
            if (!(bounds[i] < bounds[i + 1]) || Double.isInfinite(bounds[i]) || Double.isInfinite(bounds[i + 1])) {
                throw new IllegalArgumentException("Invalid scale: the minimum (" + bounds[i]
                        + ") must be strictly less than the maximum (" + bounds[i + 1] + ").");
            }
        }
        return bounds.clone();
    }

    /** Default color of the curve at position index. */
    public static float[] defaultColor(int index) {
        return PALETTE[index % PALETTE.length];
    }

    /** Effective color of a curve: its explicit color, otherwise the palette color for its position. */
    public static float[] colorOf(CurveAppearance curve, int index) {
        float[] color = curve.getColor();
        return (color != null) ? color : defaultColor(index);
    }

    /** Effective line width of a curve: its explicit width, otherwise defaultWidth. */
    public static float lineWidthOf(CurveAppearance curve, float defaultWidth) {
        float width = curve.getLineWidth();
        return (width > 0f) ? width : defaultWidth;
    }

    /** Tells whether a curve is drawn with dashes (line style other than SOLID). */
    public static boolean isDashed(CurveAppearance curve) {
        LineStyle style = curve.getLineStyle();
        return style != null && !style.isSolid();
    }

    /**
     * Color of a vertex according to its position t along the curve (0 =
     * oldest, 1 = most recent): blue -> orange gradient if gradient, otherwise
     * the curve color fading towards the old samples.
     */
    public static void vertexColor(GL2 gl, boolean gradient, float[] color, float t) {
        if (gradient) {
            gl.glColor3f(0.2f + 0.8f * t, 0.55f + 0.25f * (1 - t), 1.0f - 0.8f * t);
        } else {
            gl.glColor4f(color[0], color[1], color[2], 0.25f + 0.75f * t);
        }
    }

    /**
     * Draws the legend (a colored line followed by the name, one row per
     * curve), in the given font, with its top left corner at (x, yTop). The
     * current projection must be in pixels. Nothing is drawn if no curve has
     * a name; unnamed curves then appear as "curve n".
     */
    public static void drawLegend(GL2 gl, GLText text, ChartFont font, List<? extends CurveAppearance> curves, float x, float yTop) {
        if (!hasAnyName(curves)) return;
        float textWidth = 0;
        for (int i = 0; i < curves.size(); i++) {
            textWidth = Math.max(textWidth, text.width(labelOf(curves.get(i), i), font));
        }

        float lineHeight = legendRowHeight(text, font, curves);

        // Semi-transparent background to remain readable over the curves
        float boxWidth = LEGEND_SWATCH_WIDTH + 6 + textWidth + 12;
        float boxHeight = curves.size() * lineHeight + 8;
        gl.glColor4f(0.10f, 0.11f, 0.13f, 0.75f);
        gl.glRectf(x - 6, yTop - boxHeight, x - 6 + boxWidth, yTop);

        for (int i = 0; i < curves.size(); i++) {
            float bottom = yTop - 4 - (i + 1) * lineHeight;  // bottom of row i
            float middle = bottom + lineHeight / 2f;
            drawEntry(gl, text, font, curves.get(i), i, x, middle);
        }
        gl.glLineWidth(1f);
    }

    /** Horizontal space between two entries of an inline legend (pixels). */
    private static final int LEGEND_ENTRY_SPACING = 20;

    /**
     * Height, in pixels, of the inline legend drawn by {@link #drawLegendRows}
     * for an available width maxWidth; 0 if no curve has a name (the legend
     * is then not drawn).
     */
    public static float legendRowsHeight(GLText text, ChartFont font, List<? extends CurveAppearance> curves, float maxWidth) {
        List<float[]> rows = layoutRows(text, font, curves, maxWidth);
        return rows.size() * legendRowHeight(text, font, curves);
    }

    /**
     * Draws the legend in row(s): the entries (line + name) follow each other
     * horizontally, each row is centered on xCenter, and a new row starts
     * when the width maxWidth is reached. yTop is the top of the first row.
     * Nothing is drawn if no curve has a name.
     */
    public static void drawLegendRows(GL2 gl, GLText text, ChartFont font, List<? extends CurveAppearance> curves,
                                      float xCenter, float yTop, float maxWidth) {
        List<float[]> rows = layoutRows(text, font, curves, maxWidth);
        float rowHeight = legendRowHeight(text, font, curves);
        for (int r = 0; r < rows.size(); r++) {
            float[] row = rows.get(r); // {first index, end index (excluded), width}
            float x = xCenter - row[2] / 2f;
            float middle = yTop - r * rowHeight - rowHeight / 2f;
            for (int i = (int) row[0]; i < (int) row[1]; i++) {
                drawEntry(gl, text, font, curves.get(i), i, x, middle);
                x += entryWidth(text, font, curves.get(i), i) + LEGEND_ENTRY_SPACING;
            }
        }
        gl.glLineWidth(1f);
    }

    /** Splits the entries into rows at most maxWidth wide: {start, excluded end, width} per row. */
    private static List<float[]> layoutRows(GLText text, ChartFont font, List<? extends CurveAppearance> curves, float maxWidth) {
        List<float[]> rows = new ArrayList<>();
        if (!hasAnyName(curves)) return rows;
        int start = 0;
        float rowWidth = 0;
        for (int i = 0; i < curves.size(); i++) {
            float w = entryWidth(text, font, curves.get(i), i);
            if (i > start && rowWidth + LEGEND_ENTRY_SPACING + w > maxWidth) {
                rows.add(new float[]{start, i, rowWidth});
                start = i;
                rowWidth = 0;
            }
            rowWidth += (i > start ? LEGEND_ENTRY_SPACING : 0) + w;
        }
        rows.add(new float[]{start, curves.size(), rowWidth});
        return rows;
    }

    /** The legend is only drawn if at least one curve has a name. */
    private static boolean hasAnyName(List<? extends CurveAppearance> curves) {
        for (CurveAppearance curve : curves) {
            if (curve.getName() != null) return true;
        }
        return false;
    }

    /** Height of a legend row: the font height, or the line width if it is larger. */
    private static float legendRowHeight(GLText text, ChartFont font, List<? extends CurveAppearance> curves) {
        float height = text.lineHeight(font) + 2;
        for (CurveAppearance curve : curves) height = Math.max(height, legendLineWidth(curve) + 6);
        return height;
    }

    private static float entryWidth(GLText text, ChartFont font, CurveAppearance curve, int index) {
        return LEGEND_SWATCH_WIDTH + 6 + text.width(labelOf(curve, index), font);
    }

    /**
     * Draws the current values: one row per curve that has a value, in the
     * curve color, right-aligned on xRight, the top of the first row being at
     * yTop. The current projection must be in pixels.
     *
     * @param values one formatted value per curve (null = row omitted), see {@link CurrentValues}
     * @return the height used in pixels (0 if nothing is drawn)
     */
    public static float drawCurrentValues(GL2 gl, GLText text, ChartFont font, List<? extends CurveAppearance> curves,
                                          String[] values, float xRight, float yTop) {
        float lineHeight = text.lineHeight(font) + 2;
        int row = 0;
        for (int i = 0; i < values.length && i < curves.size(); i++) {
            if (values[i] == null) continue;
            float[] color = colorOf(curves.get(i), i);
            gl.glColor3f(color[0], color[1], color[2]);
            row++;
            text.draw(gl, values[i], font, xRight - text.width(values[i], font), yTop - row * lineHeight);
        }
        return row * lineHeight;
    }

    /** One legend entry: a line with the style, color and width of the curve, then its name. */
    private static void drawEntry(GL2 gl, GLText text, ChartFont font, CurveAppearance curve, int index, float x, float middle) {
        float[] color = colorOf(curve, index);
        gl.glColor3f(color[0], color[1], color[2]);
        // Width bounded to remain readable
        float width = legendLineWidth(curve);
        gl.glLineWidth(width);
        if (isDashed(curve)) {
            DashedLines.drawSegment(gl, curve.getLineStyle(), width, x, middle, x + LEGEND_SWATCH_WIDTH, middle);
        } else {
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2f(x, middle);
            gl.glVertex2f(x + LEGEND_SWATCH_WIDTH, middle);
            gl.glEnd();
        }
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        text.draw(gl, labelOf(curve, index), font, x + LEGEND_SWATCH_WIDTH + 6, middle - text.lineHeight(font) / 2f);
    }

    private static float legendLineWidth(CurveAppearance curve) {
        return Math.max(2f, Math.min(8f, lineWidthOf(curve, 3f)));
    }

    private static String labelOf(CurveAppearance curve, int index) {
        return (curve.getName() != null) ? curve.getName() : "curve " + (index + 1);
    }
}
