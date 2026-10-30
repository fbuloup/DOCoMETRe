package fr.univamu.ism.docometre.chartxyz;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * Abscissa axis label ({@link #setXAxisLabel}, "t(s)" by default): at the
 * right end of the time axis, at the height of the legend below the axis.
 *
 * Markers (see {@link PlotRenderer}): in cursor mode, Ctrl + left double
 * click adds a marker with the current label; its abscissa is the absolute
 * time of the sample (time since the first sample), in both display modes.
 *
 * Static cursor: in cursor mode, a left double click freezes a static cursor
 * on the designated sample; the differences Δx (time) and Δy between the
 * two cursors are displayed next to the moving cursor (see {@link PlotRenderer}).
 *
 * Real time ({@link #setRealTime}, default): the sweep mode and the pause
 * are only available for real-time signals. Otherwise (signals loaded from
 * files, for example), the display is in scroll mode, without pause.
 *
 * Keyboard commands specific to 1D: space, pause / resume; M, toggles sweep
 * / scroll (the zoom window is then removed), in real time only. Pan (right button) moves the
 * view in x and y; in scroll mode, the shifted view keeps following the
 * last sample. The wheel zooms on both axes around the point under the
 * mouse, which stays in place (on the zoom window if it exists); in scroll
 * mode, the zoomed view keeps following the last sample.
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
    /** Abscissa axis label until {@link #setXAxisLabel} is called. */
    public static final String DEFAULT_X_AXIS_LABEL = "t(s)";

    private volatile String xAxisLabel = DEFAULT_X_AXIS_LABEL; // null = no label

    private volatile DisplayMode displayMode = DisplayMode.SWEEP;
    private DisplayMode frameMode = DisplayMode.SWEEP; // mode frozen for the current frame
    private volatile boolean paused = false;
    private volatile boolean realTime = true; // sweep mode and pause available
    private volatile float zoomX = 1f; // horizontal zoom without zoom window (1 = windowTime seconds displayed)
    private final List<DisplayModeListener> displayModeListeners = new CopyOnWriteArrayList<>();
    private final Object displayModeLock = new Object(); // mode changes and their detection are atomic

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
        double baseMaxX = (frameMode == DisplayMode.SWEEP) ? windowTime : Math.max(windowTime, lastTime);
        // Horizontal pan, as a fraction of the window, and horizontal zoom around the center
        double centerX = baseMaxX - windowTime / 2 + panX * windowTime;
        double halfX = windowTime / 2 / zoomX;
        viewMinX = centerX - halfX;
        viewMaxX = centerX + halfX;

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

    /** The cursor follows the mouse abscissa: the sample at the same time, whatever its height. */
    @Override
    protected boolean cursorFollowsXOnly() { return true; }

    /** The static cursor (double click in cursor mode) is available in 1D. */
    @Override
    protected boolean supportsStaticCursor() { return true; }

    /** The markers (Ctrl + double click in cursor mode) are available in 1D. */
    @Override
    protected boolean supportsMarkers() { return true; }

    /** The horizontal zoom without zoom window is part of the zoom state. */
    @Override
    protected boolean isAutomaticView() { return super.isAutomaticView() && zoomX == 1f; }

    /** The horizontal zoom is stored in the zoom history. */
    @Override
    protected float[] saveExtraZoomState() { return new float[]{zoomX}; }

    @Override
    protected void restoreExtraZoomState(float[] state) { if (state != null) zoomX = state[0]; }

    /** A marker stores the absolute time of its sample: it remains attached to it in both display modes. */
    @Override
    protected double markerDataX(Curve1DData curve, int i, double axisX) {
        int n = curve.getSnapshotCount();
        return curve.getLastSampleTime() - (n - 1 - i) / curve.getSampleFrequency();
    }

    /**
     * Axis abscissa of a marker: its time in scroll mode; in sweep mode, its
     * position in the sweep if its time is in the displayed sweep, otherwise NaN.
     */
    @Override
    protected double markerAxisX(double time) {
        if (frameMode == DisplayMode.SCROLL) return time;
        if (time > lastTime || time < lastTime - windowTime * (1 - SWEEP_GAP)) return Double.NaN;
        return time - Math.floor(time / windowTime) * windowTime;
    }

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
            float[] color = CurveStyles.colorOf(curve);
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

            float[] color = CurveStyles.colorOf(curve);
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

    /** Horizontal and vertical pan, as in 2D; the content follows the mouse. */
    @Override
    public void pan(float dx, float dy) {
        if (zoomWindow() != null) {
            super.pan(dx, dy); // the zoom window is moved on both axes
            return;
        }
        panX -= dx * width / plotWidth / zoomX;
        super.pan(0f, dy);
    }

    /**
     * Zoom on x (zoomX) and y (zoom); the pan is adjusted so that the anchor
     * keeps its position on the screen.
     */
    @Override
    protected void zoomView(double factor, double ax, double ay) {
        double centerX = (viewMinX + viewMaxX) / 2, centerY = (viewMinY + viewMaxY) / 2;
        float newZoomX = clampZoom(zoomX * factor), newZoomY = clampZoom(zoom * factor);
        double fx = newZoomX / zoomX, fy = newZoomY / zoom; // actual factors (limited zoom)
        double unzoomedHalfY = (viewMaxY - viewMinY) / 2 * zoom; // unit of panY: twice this half range
        // New centers so that the anchor keeps its position on the screen
        panX += (float) ((ax + (centerX - ax) / fx - centerX) / windowTime);
        if (unzoomedHalfY > 0) panY += (float) ((ay + (centerY - ay) / fy - centerY) / (2 * unzoomedHalfY));
        zoomX = newZoomX;
        zoom = newZoomY;
    }

    /** Initial view: also cancels the horizontal zoom. */
    @Override
    public void resetView() {
        super.resetView();
        zoomX = 1f;
    }

    /**
     * Fixed vertical scale: the vertical axis goes exactly from yMin to yMax,
     * whatever the curve values (what exceeds them is clipped by the frame).
     * Zoom and pan remain possible; R goes back to this scale.
     */
    public void setFixedScales(double yMin, double yMax) { setFixedScaleBounds(yMin, yMax); }

    /**
     * Label of the abscissa axis, drawn at the right end of the axis, at the
     * height of the legend below the axis, in the legend font ("t(s)" by
     * default). Null or blank: no label; if the legend is not displayed below
     * the axis either, the space of the row is given back to the plot area.
     * Usable from any thread.
     */
    public void setXAxisLabel(String label) {
        xAxisLabel = (label == null || label.trim().isEmpty()) ? null : label;
    }

    /** Label of the abscissa axis, or null if there is none. */
    public String getXAxisLabel() { return xAxisLabel; }

    @Override
    protected String xAxisLabel() { return xAxisLabel; }

    /**
     * Real-time signals (true, default): the sweep mode (M key) and the pause
     * (space key) are available. Otherwise (signals loaded from files, for
     * example), the display goes to scroll mode, the pause is removed, and
     * both are no longer available: {@link #setDisplayMode}(SWEEP),
     * {@link #setPaused}(true) and their keys have no effect. The changes are
     * notified to the listeners. Usable from any thread.
     */
    public void setRealTime(boolean realTime) {
        boolean scroll;
        synchronized (displayModeLock) {
            this.realTime = realTime;
            scroll = !realTime && displayMode != DisplayMode.SCROLL;
            if (scroll) applyDisplayMode(DisplayMode.SCROLL);
        }
        if (scroll) notifyDisplayMode(DisplayMode.SCROLL);
        if (!realTime) setOption(DisplayOption.PAUSE, false, () -> paused, v -> paused = v);
    }

    /** True if the signals are real-time ones: sweep mode and pause available. */
    public boolean isRealTime() { return realTime; }

    /**
     * Chooses the display mode; a mode change removes the zoom window
     * (incompatible abscissas) and is notified to the {@link DisplayModeListener}s.
     * The sweep mode is ignored if the signals are not real-time ones (see {@link #setRealTime}).
     */
    public void setDisplayMode(DisplayMode displayMode) {
        if (displayMode == null) return;
        synchronized (displayModeLock) {
            if (displayMode == this.displayMode) return;
            if (displayMode == DisplayMode.SWEEP && !realTime) return;
            applyDisplayMode(displayMode);
        }
        notifyDisplayMode(displayMode);
    }

    public DisplayMode getDisplayMode() { return displayMode; }

    /** Toggles between sweep (oscilloscope) and scroll (M key); no effect if the signals are not real-time ones. */
    public void toggleDisplayMode() {
        DisplayMode mode;
        synchronized (displayModeLock) {
            if (!realTime) return;
            mode = (displayMode == DisplayMode.SWEEP) ? DisplayMode.SCROLL : DisplayMode.SWEEP;
            applyDisplayMode(mode);
        }
        notifyDisplayMode(mode);
    }

    private void applyDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
        panX = 0f; // the horizontal shift does not have the same meaning in both modes
        clearZoomWindow();
        clearStaticCursor(); // its abscissa does not have the same meaning in both modes either
    }

    /** Adds a display mode listener (see {@link DisplayModeListener} for the calling thread). */
    public void addDisplayModeListener(DisplayModeListener listener) { if (listener != null) displayModeListeners.add(listener); }
    public void removeDisplayModeListener(DisplayModeListener listener) { displayModeListeners.remove(listener); }

    /** Called outside displayModeLock. */
    private void notifyDisplayMode(DisplayMode mode) {
        for (DisplayModeListener listener : displayModeListeners) {
            try {
                listener.displayModeChanged(mode);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    /**
     * Freezes / resumes the display (also with the space key); data keep being
     * received during the pause. The pause is ignored if the signals are not
     * real-time ones (see {@link #setRealTime}).
     */
    public void setPaused(boolean paused) {
        if (paused && !realTime) return;
        setOption(DisplayOption.PAUSE, paused, () -> this.paused, v -> this.paused = v);
    }
    public boolean isPaused() { return paused; }
    public void togglePause() {
        if (!realTime) return;
        toggleOption(DisplayOption.PAUSE, () -> paused, v -> paused = v);
    }
}
