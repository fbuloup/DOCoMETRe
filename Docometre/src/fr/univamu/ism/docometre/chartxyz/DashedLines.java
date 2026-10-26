package fr.univamu.ism.docometre.chartxyz;

import java.util.Map;
import java.util.WeakHashMap;

import com.jogamp.opengl.GL2;

/**
 * Drawing of dashed curves (DASH, DASH_DOT, DASH_DASH_DOT) with a pattern
 * anchored on the data.
 *
 * OpenGL native stippling (glLineStipple) restarts the pattern at the first
 * vertex of each glBegin. In real time, this first vertex is the oldest
 * sample of the window, which changes at each frame: the dashes then seem
 * to slide along the curve. Here, each sample receives once and for all, on
 * arrival, its position in the pattern (cumulative on-screen length of the
 * drawing, in pixels): the dashes remain attached to the data.
 *
 * Lengths are measured at the display scale of the moment the samples
 * arrive. If the scale then changes significantly (zoom, automatic fit,
 * factor greater than 2), all the positions are recomputed, which causes a
 * single jump of the pattern.
 *
 * An instance is specific to a renderer and must be used from its rendering
 * thread.
 */
public final class DashedLines {

    /** Emits the vertex located between vertices i-1 and i of the snapshot, at fraction f (0 = i-1, 1 = i). */
    public interface Emitter {
        void vertex(int i, float f);
    }

    /** On-screen length, in pixels, of the segment between vertices i-1 and i of the snapshot. */
    public interface SegmentLength {
        double pixels(int i);
    }

    /** Tells whether the segment between vertices i-1 and i must not be drawn. */
    public interface SegmentFilter {
        boolean skip(int i);
    }

    /** Scale factor beyond which the positions in the pattern are recomputed. */
    private static final double RESCALE_RATIO = 2.0;

    /** Positions in the pattern of the samples of a curve. */
    private static final class Phases {
        final double[] cumulative; // cumulative length (pixels), indexed by absolute sample number % capacity
        long last = -1;            // absolute number of the last processed sample
        double referenceScaleX, referenceScaleY;

        Phases(int capacity) {
            cumulative = new double[capacity];
        }
    }

    private final Map<Object, Phases> phasesByCurve = new WeakHashMap<>();

    /**
     * Draws a curve of the current snapshot with dashes.
     *
     * @param curve        the curve (its line style must be other than SOLID)
     * @param width        line width in pixels (the pattern is proportional to it)
     * @param capacity     buffer capacity of the curve
     * @param totalSamples total number of samples received at the time of the snapshot
     * @param n            number of points of the snapshot
     * @param from         first point to draw (the previous points keep their position in the pattern)
     * @param scaleX       horizontal display scale (pixels per unit), used to detect scale changes
     * @param scaleY       vertical display scale (pixels per unit)
     * @param length       on-screen length of each segment, at the current scale
     * @param filter       segments not to draw (null = all)
     * @param emitter      emission of the vertices (color included)
     */
    void draw(GL2 gl, CurveAppearance curve, float width, int capacity, long totalSamples, int n, int from,
                     double scaleX, double scaleY, SegmentLength length, SegmentFilter filter, Emitter emitter) {
        Phases phases = update(curve, capacity, totalSamples, n, scaleX, scaleY, length);
        if (n < 2) return;

        short pattern = curve.getLineStyle().getPattern();
        double unit = unitLength(width);
        long first = totalSamples - n;
        boolean[] open = {false};
        for (int i = Math.max(from, 0) + 1; i < n; i++) {
            if (filter != null && filter.skip(i)) {
                if (open[0]) {
                    gl.glEnd();
                    open[0] = false;
                }
                continue;
            }
            double p0 = phases.cumulative[index(first + i - 1, capacity)];
            double p1 = phases.cumulative[index(first + i, capacity)];
            walkSegment(gl, pattern, unit, p0, p1 - p0, i, emitter, open);
        }
        if (open[0]) gl.glEnd();
    }

    /**
     * Draws the straight segment (x0, y0) - (x1, y1) with dashes, the pattern
     * starting at (x0, y0). Used by the legend (coordinates in pixels).
     */
    public static void drawSegment(GL2 gl, LineStyle style, float width, float x0, float y0, float x1, float y1) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        boolean[] open = {false};
        walkSegment(gl, style.getPattern(), unitLength(width), 0, length, 1,
                    (i, f) -> gl.glVertex2f(x0 + f * (x1 - x0), y0 + f * (y1 - y0)), open);
        if (open[0]) gl.glEnd();
    }

    /** Length in pixels of one character of the pattern: proportional to the line width. */
    private static double unitLength(float width) {
        return LineStyle.BASE_FACTOR * Math.max(1, Math.round(width));
    }

    /** Computes the position in the pattern of the samples received since the previous frame. */
    private Phases update(Object curve, int capacity, long totalSamples, int n,
                          double scaleX, double scaleY, SegmentLength length) {
        Phases phases = phasesByCurve.get(curve);
        if (phases == null || phases.cumulative.length != capacity) {
            phases = new Phases(capacity);
            phasesByCurve.put(curve, phases);
        }
        if (n == 0) {
            phases.last = -1;
            return phases;
        }
        long first = totalSamples - n;
        long lastAbs = totalSamples - 1;
        boolean rescaled = ratio(scaleX, phases.referenceScaleX) > RESCALE_RATIO
                        || ratio(scaleY, phases.referenceScaleY) > RESCALE_RATIO;

        long start;
        if (phases.last < 0 || phases.last < first - 1 || phases.last > lastAbs || rescaled) {
            // First display, samples lost between two frames, clear() or scale change: start again from zero
            phases.cumulative[index(first, capacity)] = 0;
            phases.referenceScaleX = scaleX;
            phases.referenceScaleY = scaleY;
            start = first + 1;
        } else {
            start = phases.last + 1;
        }
        for (long abs = start; abs <= lastAbs; abs++) {
            phases.cumulative[index(abs, capacity)] = phases.cumulative[index(abs - 1, capacity)]
                    + length.pixels((int) (abs - first));
        }
        phases.last = lastAbs;
        return phases;
    }

    private static double ratio(double a, double b) {
        if (!(a > 0) || !(b > 0)) return 1; // unknown scale: no recomputation
        return Math.max(a / b, b / a);
    }

    private static int index(long absoluteSample, int capacity) {
        return (int) (absoluteSample % capacity);
    }

    /**
     * Walks a segment of length len (pixels) starting at position p0 in the
     * pattern, and emits the "on" portions as GL_LINE_STRIP. open[0] tells
     * whether a strip is open: it remains open from one segment to the next
     * as long as the pattern is on, so that thick dashes have no break at
     * the vertices.
     */
    private static void walkSegment(GL2 gl, short pattern, double unit, double p0, double len,
                                    int i, Emitter emitter, boolean[] open) {
        if (!(len > 0)) return;
        double period = 16 * unit;
        double pos = p0 % period;
        if (pos < 0) pos += period;
        double done = 0;
        while (true) {
            int u = (int) (pos / unit);
            if (u >= 16) { // numerical safeguard
                u = 0;
                pos -= period;
            }
            boolean on = ((pattern >> u) & 1) != 0;
            if (on && !open[0]) {
                gl.glBegin(GL2.GL_LINE_STRIP);
                open[0] = true;
                emitter.vertex(i, (float) (done / len));
            } else if (!on && open[0]) {
                gl.glEnd();
                open[0] = false;
            }
            double step = Math.min((u + 1) * unit - pos, len - done);
            done += step;
            pos += step;
            if (done >= len) break;
            // Character boundary reached inside the segment
            boolean nextOn = ((pattern >> ((u + 1) % 16)) & 1) != 0;
            if (open[0] && !nextOn) {
                emitter.vertex(i, (float) (done / len));
                gl.glEnd();
                open[0] = false;
            }
            if (pos >= period) pos -= period;
        }
        if (open[0]) emitter.vertex(i, 1f);
    }
}
