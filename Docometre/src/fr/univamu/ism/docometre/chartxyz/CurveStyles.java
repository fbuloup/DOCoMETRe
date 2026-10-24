package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;

/**
 * Default curve colors and legend drawing, shared by the 1D, 2D and 3D
 * renderers.
 */
public final class CurveStyles {

    /**
     * Palette readable on a dark background (0..255 components). In a chart,
     * a curve in automatic color receives the palette color used by the
     * fewest curves (the first one in this order in case of a tie), see
     * {@link CurveChartRenderer#addCurve}.
     */
	public static final CurveColor[] PALETTE = {
	        new CurveColor(89, 191, 255),  // blue
	        new CurveColor(255, 153, 51),  // orange
	        new CurveColor(115, 230, 115), // green
	        new CurveColor(242, 102, 128), // pinkish red
	        new CurveColor(191, 140, 255), // purple
	        new CurveColor(255, 230, 76),  // yellow
	        new CurveColor(76, 230, 230),  // cyan
	        new CurveColor(230, 230, 230), // light gray
	        new CurveColor(156, 152, 94),  // olive
	        new CurveColor(39, 169, 132),  // teal green
	        new CurveColor(253, 184, 221), // light pink
	        new CurveColor(255, 181, 161), // peach
	        new CurveColor(43, 164, 179),  // petrol blue
	        new CurveColor(110, 145, 251), // cornflower blue
	        new CurveColor(234, 115, 78),  // terracotta
	        new CurveColor(235, 199, 145), // sand
	        new CurveColor(221, 233, 171), // pale lime
	        new CurveColor(186, 195, 255), // lavender
	        new CurveColor(98, 166, 65),   // leaf green
	        new CurveColor(200, 126, 179), // mauve
	        new CurveColor(187, 141, 98),  // caramel
	        new CurveColor(62, 254, 201),  // mint
	        new CurveColor(173, 191, 73),  // pistachio
	        new CurveColor(144, 194, 147), // sage
	};

    /** Color of a curve in automatic color that is in no chart (normally never drawn). */
    private static final CurveColor NO_COLOR = new CurveColor(128, 128, 128);

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

    /** Number of colors of the palette (8). */
    public static int getPaletteSize() {
        return PALETTE.length;
    }

    /** Color {r, g, b} (0..255) of the palette at position index (copy). */
    public static int[] getPaletteColor(int index) {
        return PALETTE[index].toRGB();
    }

    static CurveColor paletteColor(int index) {
        return PALETTE[index];
    }

    /** Position of the color in the palette, or -1 if it is not a palette color (or null). */
    static int paletteIndex(CurveColor color) {
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i].sameAs(color)) return i;
        }
        return -1;
    }

    /** OpenGL color {r, g, b} (0..1) of a curve, computed when its color changed. */
    static float[] colorOf(CurveData<?> curve) {
        CurveColor color = curve.color();
        return (color != null) ? color.gl : NO_COLOR.gl;
    }

    /** Effective line width of a curve: its explicit width, otherwise defaultWidth. */
    static float lineWidthOf(CurveAppearance curve, float defaultWidth) {
        float width = curve.getLineWidth();
        return (width > 0f) ? width : defaultWidth;
    }

    /** Tells whether a curve is drawn with dashes (line style other than SOLID). */
    static boolean isDashed(CurveAppearance curve) {
        LineStyle style = curve.getLineStyle();
        return style != null && !style.isSolid();
    }

    /**
     * Color of a vertex according to its position t along the curve (0 =
     * oldest, 1 = most recent): blue -> orange gradient if gradient, otherwise
     * the curve color, fading towards the old samples if fading.
     */
    /** Circle of radius r centered on (x, y), in pixels: filled (GL_POLYGON) or outline (GL_LINE_LOOP). */
    static void drawCircle(GL2 gl, int mode, float x, float y, float r) {
        int segments = 24;
        gl.glBegin(mode);
        for (int i = 0; i < segments; i++) {
            double a = 2 * Math.PI * i / segments;
            gl.glVertex2f(x + (float) (r * Math.cos(a)), y + (float) (r * Math.sin(a)));
        }
        gl.glEnd();
    }

    static void vertexColor(GL2 gl, boolean gradient, boolean fading, float[] color, float t) {
        if (gradient) {
            gl.glColor3f(0.2f + 0.8f * t, 0.55f + 0.25f * (1 - t), 1.0f - 0.8f * t);
        } else {
            gl.glColor4f(color[0], color[1], color[2], fading ? 0.25f + 0.75f * t : 1f);
        }
    }

    /**
     * Draws the legend (a colored line followed by the name, one row per
     * curve), in the given font, with its top left corner at (x, yTop). The
     * current projection must be in pixels. Nothing is drawn if there is no
     * curve.
     */
    static void drawLegend(GL2 gl, GLText text, ChartFont font, List<? extends CurveData<?>> curves, float x, float yTop) {
        if (curves.isEmpty()) return;
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
     * for an available width maxWidth; 0 if there is no curve (the legend is
     * then not drawn).
     */
    static float legendRowsHeight(GLText text, ChartFont font, List<? extends CurveData<?>> curves, float maxWidth) {
        List<float[]> rows = layoutRows(text, font, curves, maxWidth);
        return rows.size() * legendRowHeight(text, font, curves);
    }

    /**
     * Draws the legend in row(s): the entries (line + name) follow each other
     * horizontally, each row is centered on xCenter, and a new row starts
     * when the width maxWidth is reached. yTop is the top of the first row.
     * Nothing is drawn if there is no curve.
     */
    static void drawLegendRows(GL2 gl, GLText text, ChartFont font, List<? extends CurveData<?>> curves,
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
    private static List<float[]> layoutRows(GLText text, ChartFont font, List<? extends CurveData<?>> curves, float maxWidth) {
        List<float[]> rows = new ArrayList<>();
        if (curves.isEmpty()) return rows;
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

    /** Height of a legend row: the font height, or the line width if it is larger. */
    static float legendRowHeight(GLText text, ChartFont font, List<? extends CurveData<?>> curves) {
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
    static float drawCurrentValues(GL2 gl, GLText text, ChartFont font, List<? extends CurveData<?>> curves,
                                          String[] values, float xRight, float yTop) {
        float lineHeight = text.lineHeight(font) + 2;
        int row = 0;
        for (int i = 0; i < values.length && i < curves.size(); i++) {
            if (values[i] == null) continue;
            float[] color = colorOf(curves.get(i));
            gl.glColor3f(color[0], color[1], color[2]);
            row++;
            text.draw(gl, values[i], font, xRight - text.width(values[i], font), yTop - row * lineHeight);
        }
        return row * lineHeight;
    }

    /** One legend entry: a line with the style, color and width of the curve, then its name. */
    private static void drawEntry(GL2 gl, GLText text, ChartFont font, CurveData<?> curve, int index, float x, float middle) {
        float[] color = colorOf(curve);
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
        return curve.getName();
    }
}
