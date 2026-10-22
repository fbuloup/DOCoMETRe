package fr.univamu.ism.docometre.chartxyz;

import java.util.Locale;

import com.jogamp.opengl.GL2;

/**
 * OpenGL rendering of one or several real-time signals y(t) ({@link Curve1DData}),
 * in two modes (see {@link DisplayMode}):
 * <ul>
 * <li>{@link DisplayMode#SWEEP} (default): oscilloscope-like sweep. The
 *     abscissa is fixed, from 0 to windowTimeWidth seconds; the drawing
 *     progresses from left to right, then starts again on the left,
 *     progressively replacing the previous sweep. A vertical cursor shows
 *     the write position and a short gap precedes it.</li>
 * <li>{@link DisplayMode#SCROLL}: scrolling. The abscissa ends at the last
 *     sample received and slides with time.</li>
 * </ul>
 *
 * Several signals: each one can have its own sampling frequency. The window
 * duration (windowTimeWidth) is the one of the chart: it is imposed on all
 * its curves, whose capacity it sets. Times are counted from the first
 * sample of each curve (channels acquired together are therefore aligned);
 * the window ends at the most recent last sample, all curves combined.
 *
 * In scroll mode, the horizontal axis is time, in seconds since the first
 * sample. It always covers windowTimeWidth seconds: as long as the window
 * is not full, it goes from 0 to windowTimeWidth and the curve progresses
 * to the right; then, it ends at the last sample received and scrolls. In
 * sweep mode, it shows the time elapsed since the beginning of the current
 * sweep.
 * The vertical axis automatically fits the values of the window; zoom and
 * pan apply on top of it. The "pause" key freezes the display without
 * interrupting data reception.
 *
 * Cursor (C key, see {@link PlotRenderer}): it designates the sample whose
 * time is closest to the mouse abscissa, whatever the mouse height. Its
 * abscissa is the one of the time axis (time since the first sample in
 * scroll mode, position in the sweep in sweep mode), displayed with the
 * resolution of the sampling frequency; in sweep mode, the samples of the
 * gap cannot be designated.
 *
 * Static cursor: in cursor mode, a left double click freezes a static cursor
 * on the designated sample; the differences Δx (time) and Δy between the
 * two cursors are displayed next to the moving cursor (see {@link PlotRenderer}).
 *
 * Keyboard commands specific to 1D: space, pause / resume; M, toggles sweep
 * / scroll (the zoom window is then removed). Pan (right button) moves the
 * view in x and y; in scroll mode, the shifted view keeps following the
 * last sample. The wheel only zooms on y.
 *
 * Rectangle zoom (see {@link PlotRenderer}): in sweep mode, the window
 * abscissas are positions in the sweep; in scroll mode, they are relative
 * to the last sample, so that the window follows time (magnifier on the
 * last seconds); pause (space) allows examining a precise moment.
 *
 * To keep a good precision even after hours of acquisition, vertex
 * positions are computed in double: time relative to the last sample in
 * scroll mode, time modulo the sweep duration in oscilloscope mode.
 */
public class Chart1DRenderer extends PlotRenderer<Curve1DData> {

    /** Display mode of the time axis. */
    public enum DisplayMode {
        /** Oscilloscope-like sweep: fixed abscissa [0, windowTimeWidth], the drawing starts again on the left. */
        SWEEP,
        /** Scroll: the abscissa ends at the last sample and slides with time. */
        SCROLL
    }

    /** In sweep mode, fraction of the window left empty just before the write cursor. */
    private static final double SWEEP_GAP = 0.01;
    private static final float DEFAULT_LINE_WIDTH = 1.5f; // width of the curves without an explicit width (pixels)

    private volatile DisplayMode displayMode = DisplayMode.SWEEP;
    private DisplayMode frameMode = DisplayMode.SWEEP; // mode frozen for the current frame
    private volatile boolean paused = false;

    // Common bounds, recomputed at each frame
    private float dataMinY = -1f, dataMaxY = 1f;
    private double lastTime = 0;  // time of the most recent last sample (s)
    private double windowTime = 1; // window duration for the current frame (s)

    /**
     * @param windowTimeWidth duration of the displayed window, in seconds, imposed on all the curves
     */
    public Chart1DRenderer(double windowTimeWidth, Curve1DData... curves) {
        super(4f, curves);
        setWindowTimeWidth(windowTimeWidth);
    }

    /**
     * Chart whose window is the longest of the windows of its curves, which
     * each keep their own (original behavior).
     *
     * @deprecated the window belongs to the chart: use {@link #Chart1DRenderer(double, Curve1DData...)}
     */
    @Deprecated
    public Chart1DRenderer(Curve1DData... curves) {
        super(4f, curves);
    }

    /** Freezes the data received since the previous frame (except in pause: the display remains frozen). */
    @Override
    protected void updateData() {
        frameMode = displayMode;
        if (paused) return;
        snapshotCurves();
        computeBounds();
    }

    /** Y bounds of all the non-empty curves, time of the last sample and window duration. */
    private void computeBounds() {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        double imposed = getWindowTimeWidth();
        double last = Double.NEGATIVE_INFINITY, window = imposed;
        for (Curve1DData curve : frameCurves) {
            if (imposed == 0) window = Math.max(window, curve.getWindowTimeWidth());
            if (curve.getSnapshotCount() == 0) continue;
            last = Math.max(last, curve.getLastSampleTime());
            minY = Math.min(minY, curve.getMinY());
            maxY = Math.max(maxY, curve.getMaxY());
        }
        if (minY > maxY) { // no data
            minY = -1f;
            maxY = 1f;
        }
        dataMinY = minY;
        dataMaxY = maxY;
        lastTime = Double.isInfinite(last) ? 0 : last;
        windowTime = (window > 0) ? window : 1;
    }

    /**
     * Computes the visible window: the zoom window if it exists (abscissas
     * relative to {@link #zoomXReference()}); otherwise [0, windowTime] in
     * sweep mode, windowTime seconds ending at the last sample in scroll
     * mode, and y fitted to the data, or equal to the fixed scale if it is set.
     */
    @Override
    protected void computeView() {
        double[] window = zoomWindow();
        if (window != null) {
            double reference = zoomXReference();
            viewMinX = window[0] + reference;
            viewMaxX = window[1] + reference;
            viewMinY = window[2];
            viewMaxY = window[3];
            return;
        }
        if (frameMode == DisplayMode.SWEEP) {
            viewMinX = 0;
            viewMaxX = windowTime;
        } else {
            viewMaxX = Math.max(windowTime, lastTime);
            viewMinX = viewMaxX - windowTime;
        }
        // Horizontal pan, as a fraction of the window (the content follows the mouse)
        double shift = panX * windowTime;
        viewMinX += shift;
        viewMaxX += shift;

        double[] fixed = getFixedScaleBounds();
        float halfY = (fixed != null) ? (float) ((fixed[1] - fixed[0]) / 2) : halfExtent(dataMinY, dataMaxY);
        float baseCenterY = (fixed != null) ? (float) ((fixed[0] + fixed[1]) / 2) : (dataMinY + dataMaxY) / 2f;
        float centerY = baseCenterY + panY * 2f * halfY;
        halfY /= zoom;
        viewMinY = centerY - halfY;
        viewMaxY = centerY + halfY;
    }

    /**
     * Origin of the OpenGL abscissas: the last sample in scroll mode (float
     * precision), the beginning of the sweep in oscilloscope mode (the
     * abscissas then go from 0 to windowTime).
     */
    @Override
    protected double xOrigin() {
        return (frameMode == DisplayMode.SCROLL) ? lastTime : 0;
    }

    /** In scroll mode, the zoom window is relative to the last sample: it follows time. */
    @Override
    protected double zoomXReference() {
        return (frameMode == DisplayMode.SCROLL) ? lastTime : 0;
    }

    /** The wheel only zooms on the vertical axis, with or without a zoom window. */
    @Override
    protected boolean wheelZoomsX() { return false; }

    /** The cursor follows the mouse abscissa: the sample at the same time, whatever its height. */
    @Override
    protected boolean cursorFollowsXOnly() { return true; }

    /** The static cursor (double click in cursor mode) is available in 1D. */
    @Override
    protected boolean supportsStaticCursor() { return true; }

    /** In sweep mode, the samples of the gap are not drawn: the cursor ignores them. */
    @Override
    protected int firstCursorPoint(Curve1DData curve) {
        return (frameMode == DisplayMode.SWEEP) ? firstSweepSample(curve) : 0;
    }

    /** Point i: time (or position in the sweep) and value, as drawn. */
    @Override
    protected boolean cursorPoint(Curve1DData curve, int i, double[] xy) {
        int n = curve.getSnapshotCount();
        double t = curve.getLastSampleTime() - (n - 1 - i) / curve.getSampleFrequency();
        xy[0] = (frameMode == DisplayMode.SWEEP) ? t - Math.floor(t / windowTime) * windowTime : t;
        xy[1] = curve.getSnapshotValues()[i];
        return true;
    }

    /** Cursor time with as many decimals as the sampling period requires (0.002 s at 500 Hz). */
    @Override
    protected String formatCursorX(Curve1DData curve, double x) {
        int decimals = (int) Math.max(0, Math.min(6, Math.ceil(Math.log10(curve.getSampleFrequency()) - 1e-9)));
        return String.format(Locale.US, "%." + decimals + "f", x);
    }

    /** In sweep mode, first drawn sample of the curve: the oldest ones are in the gap before the write cursor. */
    private int firstSweepSample(Curve1DData curve) {
        double oldestShown = lastTime - windowTime * (1 - SWEEP_GAP);
        int n = curve.getSnapshotCount();
        return (int) Math.max(0, Math.ceil((oldestShown - curve.getLastSampleTime()) * curve.getSampleFrequency() + (n - 1) - 1e-9));
    }

    @Override
    protected void drawCurves(GL2 gl, double origin) {
        if (frameMode == DisplayMode.SWEEP) {
            drawSweepCursor(gl);
            drawCurvesSweep(gl, showPoints);
        } else {
            drawCurvesScroll(gl);
            if (showPoints) drawPointsScroll(gl);
        }
    }

    /** "PAUSE", below the current values if they are displayed. */
    @Override
    protected void drawStatus(GL2 gl, float valuesHeight) {
        if (!paused) return;
        gl.glColor3f(1f, 0.75f, 0.2f);
        String label = "PAUSE";
        text.draw(gl, label, scaleFont, marginLeft + plotWidth - text.width(label, scaleFont) - 6,
                  marginBottom + plotHeight - text.lineHeight(scaleFont) - 4 - (valuesHeight > 0 ? valuesHeight + 4 : 0));
    }

    /**
     * Draws the curves, each one in its color (explicit or from the palette).
     * Abscissas are in time relative to lastTime (the most recent last
     * sample), which preserves float precision.
     */
    private void drawCurvesScroll(GL2 gl) {
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve1DData curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            float dt = (float) (1.0 / curve.getSampleFrequency());
            float offset = (float) (curve.getLastSampleTime() - lastTime); // <= 0
            float[] color = CurveStyles.colorOf(curve, k);
            gl.glColor3f(color[0], color[1], color[2]);
            if (CurveStyles.isDashed(curve)) {
                drawDashed(gl, curve, width, values, n, 0, null,
                        (i, f) -> gl.glVertex2f(offset - (n - i - f) * dt, values[i - 1] + f * (values[i] - values[i - 1])));
                drawnPoints += n;
            } else if (useDecimation(n, 2 * plotWidth)) {
                // M4 reduction: pixel column of each sample, from its drawn abscissa
                // (visible range and not windowTime: the time axis can be zoomed)
                double left = viewMinX - lastTime, pixelsPerSecond = plotWidth / (viewMaxX - viewMinX);
                decimation.m4(0, n, values, i -> (long) Math.floor(((offset - (n - 1 - i) * dt) - left) * pixelsPerSecond));
                int[] kept = decimation.indices();
                int count = decimation.count();
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < count; j++) {
                    int i = kept[j];
                    gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
                }
                gl.glEnd();
                drawnPoints += count;
            } else {
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < n; i++) {
                    gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
                }
                gl.glEnd();
                drawnPoints += n;
            }
        }
    }

    /**
     * Dashed drawing of a signal (pattern anchored on the data, see
     * {@link DashedLines}). The on-screen length of a segment only depends on
     * the scales: it is the same in sweep and scroll modes.
     */
    private void drawDashed(GL2 gl, Curve1DData curve, float width, float[] values, int n, int from,
                            DashedLines.SegmentFilter filter, DashedLines.Emitter emitter) {
        double scaleX = plotWidth / (viewMaxX - viewMinX);              // pixels per second (zoom included)
        double scaleY = plotHeight / (viewMaxY - viewMinY);          // pixels per unit
        double dx = scaleX / curve.getSampleFrequency();                // pixels between two samples
        dashedLines.draw(gl, curve, width, curve.getSnapshotCapacity(), curve.getSnapshotTotalSamples(), n, from, scaleX, scaleY,
                i -> Math.hypot(dx, (values[i] - values[i - 1]) * scaleY), filter, emitter);
    }

    /**
     * Sweep mode: each sample at time t is placed at x = t modulo windowTime;
     * the drawing is interrupted at each return to the left. The samples
     * whose time precedes lastTime - windowTime * (1 - SWEEP_GAP) are not
     * drawn, which leaves a gap before the cursor.
     */
    private void drawCurvesSweep(GL2 gl, boolean withPoints) {
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve1DData curve = frameCurves.get(k);
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            double frequency = curve.getSampleFrequency();
            double curveLast = curve.getLastSampleTime();
            int first = firstSweepSample(curve); // the oldest ones are in the gap

            float[] color = CurveStyles.colorOf(curve, k);
            gl.glColor3f(color[0], color[1], color[2]);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            if (CurveStyles.isDashed(curve)) {
                // Segments straddling a return to the left are not drawn
                drawDashed(gl, curve, width, values, n, first,
                        i -> Math.floor((curveLast - (n - i) / frequency) / windowTime)
                             != Math.floor((curveLast - (n - 1 - i) / frequency) / windowTime),
                        (i, f) -> {
                            double t = curveLast - (n - i - f) / frequency;
                            double sweepStart = Math.floor((curveLast - (n - i) / frequency) / windowTime) * windowTime;
                            gl.glVertex2d(t - sweepStart, values[i - 1] + f * (values[i] - values[i - 1]));
                        });
                drawnPoints += n - first;
            } else {
                // Optional M4 reduction: key = (sweep, pixel column in the sweep)
                int[] kept = null;
                int count = n - first;
                if (useDecimation(count, 2 * plotWidth)) {
                    double pixelsPerSecond = plotWidth / (viewMaxX - viewMinX), left = viewMinX;
                    decimation.m4(first, n, values, i -> {
                        double t = curveLast - (n - 1 - i) / frequency;
                        double sweep = Math.floor(t / windowTime);
                        return ((long) sweep << 32) + (long) Math.floor((t - sweep * windowTime - left) * pixelsPerSecond);
                    });
                    kept = decimation.indices();
                    count = decimation.count();
                }
                drawnPoints += count;
                long currentSweep = Long.MIN_VALUE;
                boolean open = false;
                for (int j = 0; j < count; j++) {
                    int i = (kept == null) ? first + j : kept[j];
                    double t = curveLast - (n - 1 - i) / frequency;
                    long sweep = (long) Math.floor(t / windowTime);
                    if (sweep != currentSweep) { // return to the left: new strip
                        if (open) gl.glEnd();
                        gl.glBegin(GL2.GL_LINE_STRIP);
                        open = true;
                        currentSweep = sweep;
                    }
                    gl.glVertex2d(t - sweep * windowTime, values[i]);
                }
                if (open) gl.glEnd();
            }

            if (withPoints) {
                drawnPoints += n - first;
                gl.glColor3f(1f, 1f, 1f);
                gl.glBegin(GL2.GL_POINTS);
                for (int i = first; i < n; i++) {
                    double t = curveLast - (n - 1 - i) / frequency;
                    gl.glVertex2d(t - Math.floor(t / windowTime) * windowTime, values[i]);
                }
                gl.glEnd();
            }
        }
    }

    /** Sweep mode: vertical line at the current write position. */
    private void drawSweepCursor(GL2 gl) {
        boolean any = false;
        for (Curve1DData curve : frameCurves) any |= curve.getSnapshotCount() > 0;
        if (!any) return;
        double x = lastTime - Math.floor(lastTime / windowTime) * windowTime;
        gl.glColor4f(0.85f, 0.85f, 0.9f, 0.5f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2d(x, viewMinY);
        gl.glVertex2d(x, viewMaxY);
        gl.glEnd();
    }

    private void drawPointsScroll(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (Curve1DData curve : frameCurves) {
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            float dt = (float) (1.0 / curve.getSampleFrequency());
            float offset = (float) (curve.getLastSampleTime() - lastTime);
            for (int i = 0; i < n; i++) {
                gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
            }
        }
        gl.glEnd();
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case ' ': togglePause(); break;
            case 'm': toggleDisplayMode(); break;
            default: super.keyPressed(key); break;
        }
    }

    /**
     * Horizontal and vertical pan, as in 2D; the content follows the mouse.
     * Without a zoom window, the wheel zoom only applies to y: the horizontal
     * displacement therefore does not depend on it.
     */
    @Override
    public void pan(float dx, float dy) {
        if (zoomWindow() != null) {
            super.pan(dx, dy); // the zoom window is moved on both axes
            return;
        }
        panX -= dx * width / plotWidth;
        super.pan(0f, dy);
    }

    /**
     * Fixed vertical scale: the vertical axis goes exactly from yMin to yMax,
     * whatever the curve values (what exceeds them is clipped by the frame).
     * Zoom and pan remain possible; R goes back to this scale.
     */
    public void setFixedScales(double yMin, double yMax) { setFixedScaleBounds(yMin, yMax); }

    /** Chooses the display mode; a mode change removes the zoom window (incompatible abscissas). */
    public void setDisplayMode(DisplayMode displayMode) {
        if (displayMode == null || displayMode == this.displayMode) return;
        this.displayMode = displayMode;
        panX = 0f; // the horizontal shift does not have the same meaning in both modes
        clearZoomWindow();
        clearStaticCursor(); // its abscissa does not have the same meaning in both modes either
    }
    public DisplayMode getDisplayMode() { return displayMode; }
    /** Toggles between sweep (oscilloscope) and scroll. */
    public void toggleDisplayMode() {
        setDisplayMode((displayMode == DisplayMode.SWEEP) ? DisplayMode.SCROLL : DisplayMode.SWEEP);
    }

    /** Freezes / resumes the display; data keep being received during the pause. */
    public void togglePause() { paused = !paused; }
}
