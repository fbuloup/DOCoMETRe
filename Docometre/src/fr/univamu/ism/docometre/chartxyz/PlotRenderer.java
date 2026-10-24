package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Base of the plane renderers (1D and 2D). The screen is split into a plot
 * area (orthographic projection in data units, clipped with a scissor)
 * surrounded by margins in pixels where the tick labels are written. Ticks
 * fall on "round" values (1, 2 or 5 x 10^n). The view automatically fits
 * the data; zoom and pan apply on top of this automatic fit.
 *
 * Rectangle zoom: with the left button held down, the window to display is
 * drawn with the mouse (shown while moving) and applied on release. A very
 * narrow rectangle in one direction only zooms on the other axis. This zoom
 * window is expressed in data units (in 1D scroll mode, abscissas relative
 * to the last sample: the window follows time); as long as it exists, the
 * wheel and pan modify it. Commands, in addition to those of
 * {@link CurveChartRenderer}:
 * <ul>
 * <li>left button + drag: rectangle zoom</li>
 * <li>right button + drag: pan</li>
 * <li>Escape: cancels the rectangle being drawn</li>
 * <li>U or Backspace: goes back to the previous window</li>
 * <li>R: goes back to the initial view (also clears the history)</li>
 * </ul>
 *
 * Cursor (C key, disabled by default): a vertical axis and a horizontal axis,
 * in the color of the followed curve, cross on the point of that curve
 * closest to the mouse (on-screen distance, among the visible samples; in
 * 1D, the point with the same abscissa as the mouse, see
 * {@link #cursorFollowsXOnly()}). Its coordinates are displayed in badges at
 * the foot of each cursor axis, on the abscissa and ordinate axes. The
 * cursor first follows the first curve; Tab moves to the next one, Shift +
 * Tab to the previous one. {@link CursorListener}s receive the coordinates
 * {x, y} of the designated point.
 *
 * Static cursor (charts where {@link #supportsStaticCursor()} is true, 1D):
 * while the cursor is active, a left double click freezes a second cursor
 * on the point designated at that moment (dashed axes and badges in the
 * color of its curve). The differences Δx and Δy between the moving cursor
 * and the static cursor are then displayed next to the moving cursor.
 * Another double click moves the static cursor; disabling the cursor also
 * removes it. {@link StaticCursorListener}s receive its position and the
 * differences.
 *
 * Markers (charts where {@link #supportsMarkers()} is true, 1D): points
 * identified by a label ("Max", "Min"...) and placed on a curve, whose color
 * they take. The markers of a curve with the same label form a group,
 * identified by (curve, label): two curves can have groups with the same
 * label (they are distinct groups). A marker is drawn as a dotted vertical
 * line, a diamond on the point and the label at the foot of the line, on the
 * abscissa axis (no coordinates), moved aside when a cursor label or another
 * marker label occupies its place. In cursor mode, Ctrl + left double click
 * adds a marker with the current label ({@link #setCurrentMarkerLabel},
 * "Default" by default) on the point designated by the cursor, to the group
 * of the curve followed by the cursor; adding a marker that already exists
 * in its group (same coordinates) is ignored. A double click on a marker
 * label removes that marker (the group disappears with its last marker);
 * Ctrl + click on a label makes it the current label. Leaving the cursor
 * mode keeps the markers; removing a curve from the chart removes its
 * markers. Groups can also be managed with {@link #addMarkersGroup} and
 * {@link #removeMarkersGroup}. {@link MarkerAddedListener}s and
 * {@link MarkerRemovedListener}s are told about each marker added or
 * removed, {@link CurrentMarkerLabelListener}s about each change of the
 * current label.
 *
 * Each frame follows the same sequence ({@link #display}); subclasses
 * provide the data update, the visible window and the curve drawing.
 *
 * @param <C> type of the displayed curves
 */
public abstract class PlotRenderer<C extends CurveData<C>> extends CurveChartRenderer<C> {

    private static final int TARGET_TICK_COUNT = 6;          // approximate number of ticks per axis
    protected static final float DATA_MARGIN = 0.05f;        // space between curve and frame (fraction of the range)

    /** Below this size (pixels) in one direction, the rectangle does not zoom on that axis. */
    private static final float MIN_SELECTION_PIXELS = 5f;
    /** Maximum number of windows remembered for U / Backspace. */
    private static final int MAX_ZOOM_HISTORY = 50;
    /** Marker of the initial view in the history (ArrayDeque does not accept null). */
    private static final double[] INITIAL_VIEW = new double[0];

    private volatile LegendPosition legendPosition = LegendPosition.BELOW;
    private float legendBelowHeight = 0; // height of the legend below the axis (pixels), 0 if absent

    // View: zoom (1 = automatic fit) and pan (as a fraction of the unzoomed range)
    protected volatile float zoom = 1f;
    protected volatile float panX = 0f, panY = 0f;

    // Zoom window {xMin, xMax, yMin, yMax}, null = initial view. Never modified
    // in place (replaced as a whole); modified under viewLock, with the history.
    private volatile double[] zoomWindow;
    private final Deque<double[]> zoomHistory = new ArrayDeque<>();
    private final Object viewLock = new Object();
    private final List<ZoomListener> zoomListeners = new CopyOnWriteArrayList<>();

    // Cursor (C key): settings modifiable from any thread
    private volatile boolean cursorEnabled;
    private volatile int cursorCurveIndex;     // position of the followed curve (0 = first)
    private volatile float[] cursorPosition;   // {x, y} of the designated point, null if none (never modified in place)
    private final List<CursorListener> cursorListeners = new CopyOnWriteArrayList<>();
    // Rendering thread only: mouse (OpenGL pixels, < 0 = unknown) and point found in the current frame
    private float mouseX = -1, mouseY = -1;
    private boolean cursorFound;
    private float cursorPixelX, cursorPixelY;
    private float[] cursorColor;
    private String cursorLabelX, cursorLabelY;
    private C cursorCurve; // curve followed in the current frame (formatting of the deltas)

    /** Static cursor: point frozen by a double click, in axis units, with its color and badge texts. */
    private static final class StaticCursor {
        final Object curve; // curve of the point (removed with it)
        final double x, y;
        final float[] color;
        final String labelX, labelY;

        StaticCursor(Object curve, double x, double y, float[] color, String labelX, String labelY) {
            this.curve = curve;
            this.x = x;
            this.y = y;
            this.color = color;
            this.labelX = labelX;
            this.labelY = labelY;
        }
    }

    private volatile StaticCursor staticCursor; // null = none

    /** Label of the new markers until {@link #setCurrentMarkerLabel} is called. */
    public static final String DEFAULT_MARKER_LABEL = "Default";
    /** Marker: point in data units (in 1D, x = absolute time) on a curve, with the label of its group. */
    private final class Marker {
        final C curve;
        final String label;
        final double x, y;

        Marker(C curve, String label, double x, double y) {
            this.curve = curve;
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    /** Label of a marker drawn in the current frame, with its badge {x, y, w, h} (for clicks). */
    private final class MarkerBadge {
        final Marker marker;
        final float[] color;
        final float[] rect;

        MarkerBadge(Marker marker, float[] color, float[] rect) {
            this.marker = marker;
            this.color = color;
            this.rect = rect;
        }
    }

    // Marker groups by curve, then by label in creation order; guarded by markersLock (API usable from any thread)
    private final Map<C, Map<String, List<Marker>>> markerGroups = new HashMap<>();
    private final Object markersLock = new Object();
    private volatile String currentMarkerLabel = DEFAULT_MARKER_LABEL;
    // Rendering thread only: marker requested by Ctrl + double click, badges drawn in the current frame
    private boolean markerRequested;
    private List<MarkerBadge> markerBadges = new ArrayList<>();
    private final List<MarkerAddedListener<C>> markerAddedListeners = new CopyOnWriteArrayList<>();
    private final List<MarkerRemovedListener<C>> markerRemovedListeners = new CopyOnWriteArrayList<>();
    private final List<CurrentMarkerLabelListener> currentMarkerLabelListeners = new CopyOnWriteArrayList<>();
    private final List<StaticCursorListener> staticCursorListeners = new CopyOnWriteArrayList<>();
    // Last state notified to the static cursor listeners (rendering thread only)
    private StaticCursor notifiedStaticCursor;
    private float[] notifiedDelta;
    private boolean staticCursorRequested;     // double click received, placed at the next frame (rendering thread only)

    // Zoom rectangle being drawn, in OpenGL pixels (rendering thread only)
    private boolean selecting;
    private float selectionX0, selectionY0, selectionX1, selectionY1;

    // Margins around the plot area (pixels), adapted to the font size
    protected int marginLeft = 75, marginRight = 25, marginTop = 25, marginBottom = 35;
    private float yLabelWidth = 0; // width of the longest vertical tick label (previous frame)
    private float xLabelWidth = 0; // width of the longest horizontal tick label (previous frame)

    // Size of the plot area (pixels)
    protected int plotWidth = 1, plotHeight = 1;

    // Visible window, in data units (recomputed at each frame)
    protected double viewMinX, viewMaxX, viewMinY, viewMaxY;

    @SafeVarargs
    protected PlotRenderer(float pointSize, C... curves) {
        super(pointSize, curves);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        drawable.getGL().getGL2().glDisable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        selecting = false; // the rectangle being drawn no longer makes sense after a resize
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        beginFrame();
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        updateData();
        computeMargins();
        plotWidth = Math.max(1, width - marginLeft - marginRight);
        plotHeight = Math.max(1, height - marginBottom - marginTop);
        computeView();
        updateCursor();

        double stepX = AxisTicks.niceStep((viewMaxX - viewMinX) / TARGET_TICK_COUNT);
        double stepY = AxisTicks.niceStep((viewMaxY - viewMinY) / TARGET_TICK_COUNT);
        double origin = xOrigin();

        // --- Plot area: x relative to origin, y in data units ---
        gl.glViewport(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glEnable(GL2.GL_SCISSOR_TEST);
        gl.glScissor(marginLeft, marginBottom, plotWidth, plotHeight);
        setProjection(gl, viewMinX - origin, viewMaxX - origin, viewMinY, viewMaxY);

        if (showGrid) drawGrid(gl, stepX, stepY, origin);
        if (showAxes) drawAxes(gl, origin);
        drawCurves(gl, origin);

        gl.glDisable(GL2.GL_SCISSOR_TEST);

        // --- Decorations (frame, tick labels), in pixels over the whole surface ---
        gl.glViewport(0, 0, width, height);
        setProjection(gl, 0, width, 0, height);

        drawFrame(gl);
        if (showLabel) drawLabels(gl, stepX, stepY);
        drawLegend(gl);
        drawFps(gl, marginLeft + plotWidth - 6, marginBottom + 4);
        float valuesHeight = drawCurrentValues(gl, marginLeft + plotWidth - 6, marginBottom + plotHeight - 6);
        drawStatus(gl, valuesHeight);
        drawMarkers(gl);
        drawCursors(gl);
        if (selecting) drawSelection(gl);
    }

    private static void setProjection(GL2 gl, double left, double right, double bottom, double top) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(left, right, bottom, top, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    // ------------------------------------------------------------------
    // Steps provided by the subclasses

    /** Freezes the data of the frame and computes their bounds. */
    protected abstract void updateData();

    /**
     * Computes the visible window (viewMinX... viewMaxY): the zoom window if
     * it exists (see {@link #zoomWindow()}), otherwise from the bounds, the
     * zoom and the pan.
     */
    protected abstract void computeView();

    /**
     * Data abscissa placed at x = 0 in the projection of the plot area (0 by
     * default): keeps float precision when the abscissas are large (time).
     */
    protected double xOrigin() { return 0; }

    /**
     * Reference of the zoom window abscissas: a data abscissa x is stored as
     * x - zoomXReference() (0 by default; the last sample in 1D scroll mode,
     * so that the window follows time). Called in the rendering thread.
     */
    protected double zoomXReference() { return 0; }

    /** Does the wheel also zoom on the x axis when a zoom window exists (yes by default)? */
    protected boolean wheelZoomsX() { return true; }

    /** First point of the curve that the cursor can designate (0 by default; in 1D sweep mode, after the gap). */
    protected int firstCursorPoint(C curve) { return 0; }

    /**
     * Coordinates {x, y}, in axis units (those of viewMinX... viewMaxY), of
     * point i of the last snapshot of the curve, written into xy.
     *
     * @return false if this point is not drawn
     */
    protected abstract boolean cursorPoint(C curve, int i, double[] xy);

    /**
     * True if the cursor designates the point whose abscissa is closest to
     * the mouse abscissa (regardless of its height), false (default) for the
     * point closest to the mouse on screen.
     */
    protected boolean cursorFollowsXOnly() { return false; }

    /** Are the markers (Ctrl + double click in cursor mode) available on this chart (no by default)? */
    protected boolean supportsMarkers() { return false; }

    /**
     * Data abscissa of a marker placed on point i of the curve, whose axis
     * abscissa is axisX (axisX by default; in 1D, the absolute time, so that
     * the marker remains attached to its sample whatever the display mode).
     */
    protected double markerDataX(C curve, int i, double axisX) { return axisX; }

    /** Axis abscissa of a marker of data abscissa dataX, or NaN if it is not displayed (identity by default). */
    protected double markerAxisX(double dataX) { return dataX; }

    /** Is the static cursor (double click in cursor mode) available on this chart (no by default)? */
    protected boolean supportsStaticCursor() { return false; }

    /** Text of the cursor abscissa (4 significant digits by default). */
    protected String formatCursorX(C curve, double x) { return CurrentValues.format(x); }

    /** Draws the curves (and their points if they are displayed), abscissas relative to origin. */
    protected abstract void drawCurves(GL2 gl, double origin);

    /** Line x = 0 (vertical axis) drawn together with the line y = 0 (A key)? */
    protected boolean hasVerticalAxis() { return false; }

    /**
     * Optional status indication, at the top right of the plot area
     * (coordinates in pixels), below the current values.
     *
     * @param valuesHeight height used by the current values (0 if they are not displayed)
     */
    protected void drawStatus(GL2 gl, float valuesHeight) {
    }

    /** Half range (with margin) of an axis; handles the degenerate case where all values are equal. */
    protected static float halfExtent(float min, float max) {
        float range = max - min;
        if (range > 0f) return range / 2f * (1f + 2f * DATA_MARGIN);
        float center = Math.abs((min + max) / 2f);
        return (center > 0f) ? center * 0.1f : 1f;
    }

    // ------------------------------------------------------------------
    // Decorations

    /**
     * Computes the margins around the plot area from the fonts: on the left
     * the vertical tick labels (width measured at the previous frame), at the
     * bottom the horizontal tick labels and, if needed, the legend, at the
     * top and on the right just enough not to cut the tick labels placed at
     * the edges. The left margin only shrinks significantly, to keep the plot
     * area from shaking when the length of the tick labels varies.
     */
    private void computeMargins() {
        legendBelowHeight = (showLegend && legendPosition == LegendPosition.BELOW)
                ? CurveStyles.legendRowsHeight(text, legendFont, frameCurves, width - 20) : 0;
        float legendSpace = (legendBelowHeight > 0) ? legendBelowHeight + 6 : 0;
        if (!showLabel) {
            marginLeft = marginRight = marginTop = 15;
            marginBottom = (int) Math.ceil(15 + legendSpace);
            return;
        }
        float tickHeight = text.lineHeight(scaleFont);
        int neededLeft = (int) Math.ceil(Math.max(yLabelWidth, text.width("-0.00", scaleFont)) + 12);
        if (neededLeft > marginLeft || neededLeft < marginLeft * 0.8f) marginLeft = neededLeft;
        marginBottom = (int) Math.ceil(tickHeight + 10 + legendSpace);
        marginTop = (int) Math.max(10, Math.ceil(tickHeight / 2f + 4));
        marginRight = (int) Math.max(10, Math.ceil(xLabelWidth / 2f + 4));
    }

    /** Legend: in the chart at the top left, or in row(s) below the abscissa axis. */
    private void drawLegend(GL2 gl) {
        if (!showLegend) return;
        if (legendPosition == LegendPosition.BELOW) {
            // At the very bottom, below the abscissa tick labels, centered under the plot area
            CurveStyles.drawLegendRows(gl, text, legendFont, frameCurves, marginLeft + plotWidth / 2f,
                                       legendBelowHeight + 4, width - 20);
        } else {
            CurveStyles.drawLegend(gl, text, legendFont, frameCurves, marginLeft + 12, marginBottom + plotHeight - 6);
        }
    }

    /** Grid aligned on the ticks (abscissas relative to origin). */
    private void drawGrid(GL2 gl, double stepX, double stepY, double origin) {
        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (double x : AxisTicks.ticks(viewMinX, viewMaxX, stepX)) {
            gl.glVertex2d(x - origin, viewMinY);
            gl.glVertex2d(x - origin, viewMaxY);
        }
        for (double y : AxisTicks.ticks(viewMinY, viewMaxY, stepY)) {
            gl.glVertex2d(viewMinX - origin, y);
            gl.glVertex2d(viewMaxX - origin, y);
        }
        gl.glEnd();
    }

    /** Lines y = 0 (red) and, if applicable, x = 0 (green), if they are visible. */
    private void drawAxes(GL2 gl, double origin) {
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glVertex2d(viewMinX - origin, 0);
        gl.glVertex2d(viewMaxX - origin, 0);
        if (hasVerticalAxis()) {
            gl.glColor3f(0.2f, 0.9f, 0.2f);
            gl.glVertex2d(-origin, viewMinY);
            gl.glVertex2d(-origin, viewMaxY);
        }
        gl.glEnd();
    }

    /** Frame around the plot area (coordinates in pixels). */
    private void drawFrame(GL2 gl) {
        float left = marginLeft, bottom = marginBottom;
        float right = marginLeft + plotWidth, top = marginBottom + plotHeight;
        gl.glColor4f(0.6f, 0.6f, 0.65f, 0.8f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(left, bottom);
        gl.glVertex2f(right, bottom);
        gl.glVertex2f(right, top);
        gl.glVertex2f(left, top);
        gl.glEnd();
    }

    /**
     * Tick labels (below the horizontal axis and to the left of the vertical
     * axis), in the scale font (coordinates in pixels).
     */
    private void drawLabels(GL2 gl, double stepX, double stepY) {
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        ChartFont font = scaleFont;
        float lineHeight = text.lineHeight(font);

        float widestX = 0;
        for (double x : AxisTicks.ticks(viewMinX, viewMaxX, stepX)) {
            String label = AxisTicks.format(x, stepX);
            float px = marginLeft + (float) ((x - viewMinX) / (viewMaxX - viewMinX)) * plotWidth;
            float labelWidth = text.width(label, font);
            widestX = Math.max(widestX, labelWidth);
            text.draw(gl, label, font, px - labelWidth / 2f, marginBottom - 4 - lineHeight);
        }
        xLabelWidth = widestX;
        float widestY = 0;
        for (double y : AxisTicks.ticks(viewMinY, viewMaxY, stepY)) {
            String label = AxisTicks.format(y, stepY);
            float labelWidth = text.width(label, font);
            widestY = Math.max(widestY, labelWidth);
            float py = marginBottom + (float) ((y - viewMinY) / (viewMaxY - viewMinY)) * plotHeight;
            text.draw(gl, label, font, marginLeft - 6 - labelWidth, py - lineHeight / 2f);
        }
        yLabelWidth = widestY; // used to compute the left margin at the next frame
    }

    /**
     * Moving cursor and static cursor: axes across the plot area (dashed for
     * the static cursor), marker at the designated point and coordinate
     * badges at the foot of each axis (x below the plot area, y to its left,
     * in the margins when they are large enough, otherwise just inside the
     * plot area). The badges of the moving cursor keep their place; those of
     * the static cursor are shifted when they would overlap them. The static
     * cursor is drawn even when the moving cursor designates no point, but
     * not when its point is outside the view.
     */
    private void drawCursors(GL2 gl) {
        StaticCursor fixed = cursorEnabled ? staticCursor : null;
        float[] fixedPixel = (fixed != null) ? toVisiblePixel(fixed.x, fixed.y) : null;
        if (fixedPixel == null && !cursorFound) return;

        // Axes and markers: the static cursor first (below), then the moving one
        if (fixedPixel != null) drawAxesAndMarker(gl, fixedPixel[0], fixedPixel[1], fixed.color, true);
        if (cursorFound) drawAxesAndMarker(gl, cursorPixelX, cursorPixelY, cursorColor, false);

        ChartFont font = scaleFont;
        float h = text.lineHeight(font) + 4;
        float[] movingX = cursorFound ? xBadge(cursorLabelX, cursorPixelX, font, h) : null;
        float[] movingY = cursorFound ? yBadge(cursorLabelY, cursorPixelY, font, h) : null;
        if (fixedPixel != null) {
            float[] fixedX = fixedXBadge(fixed, fixedPixel, movingX, font, h);
            float[] fixedY = yBadge(fixed.labelY, fixedPixel[1], font, h);
            if (movingY != null) separate(fixedY, movingY, 1, height); // vertical shift on the ordinate axis
            drawBadge(gl, fixed.labelX, font, fixed.color, fixedX[0], fixedX[1], fixedX[2], fixedX[3]);
            drawBadge(gl, fixed.labelY, font, fixed.color, fixedY[0], fixedY[1], fixedY[2], fixedY[3]);
        }
        if (cursorFound) {
            drawBadge(gl, cursorLabelX, font, cursorColor, movingX[0], movingX[1], movingX[2], movingX[3]);
            drawBadge(gl, cursorLabelY, font, cursorColor, movingY[0], movingY[1], movingY[2], movingY[3]);
            if (fixed != null) drawDelta(gl, fixed);
        }
    }

    /** Pixel position of the point (x, y) in axis units, or null if it is outside the view. */
    private float[] toVisiblePixel(double x, double y) {
        if (x < viewMinX || x > viewMaxX || y < viewMinY || y > viewMaxY) return null;
        return new float[]{(float) (marginLeft + (x - viewMinX) * plotWidth / (viewMaxX - viewMinX)),
                           (float) (marginBottom + (y - viewMinY) * plotHeight / (viewMaxY - viewMinY))};
    }

    /** Badge {x, y, w, h} of an abscissa, centered under px, below the plot area. */
    /** Abscissa badge of the static cursor, shifted horizontally if it overlaps the one of the moving cursor (movingX, or null). */
    private float[] fixedXBadge(StaticCursor fixed, float[] fixedPixel, float[] movingX, ChartFont font, float h) {
        float[] fixedX = xBadge(fixed.labelX, fixedPixel[0], font, h);
        if (movingX != null) separate(fixedX, movingX, 0, width);
        return fixedX;
    }

    /**
     * Abscissa badges of the cursors, as drawCursors places them (moving
     * cursor, then static cursor): the marker labels move aside to leave
     * them visible.
     */
    private List<float[]> cursorXBadges(ChartFont font, float h) {
        List<float[]> result = new ArrayList<>(2);
        StaticCursor fixed = cursorEnabled ? staticCursor : null;
        float[] fixedPixel = (fixed != null) ? toVisiblePixel(fixed.x, fixed.y) : null;
        float[] movingX = cursorFound ? xBadge(cursorLabelX, cursorPixelX, font, h) : null;
        if (movingX != null) result.add(movingX);
        if (fixedPixel != null) result.add(fixedXBadge(fixed, fixedPixel, movingX, font, h));
        return result;
    }

    /**
     * Moves the badge rect horizontally to the free position closest to its
     * own (overlapping none of the obstacles): candidates are its position
     * and the positions just before and just after each obstacle. If there
     * is no free position, the badge stays in place.
     */
    private void placeXBadge(float[] rect, List<float[]> obstacles) {
        float origin = rect[0], best = origin, bestDistance = Float.POSITIVE_INFINITY;
        float maxX = Math.max(0, width - rect[2]);
        float[] candidates = new float[1 + 2 * obstacles.size()];
        candidates[0] = origin;
        for (int i = 0; i < obstacles.size(); i++) {
            float[] o = obstacles.get(i);
            candidates[2 * i + 1] = o[0] - rect[2] - 2;
            candidates[2 * i + 2] = o[0] + o[2] + 2;
        }
        for (float candidate : candidates) {
            if (candidate < 0 || candidate > maxX) continue;
            float distance = Math.abs(candidate - origin);
            if (distance >= bestDistance) continue;
            rect[0] = candidate;
            boolean free = true;
            for (float[] o : obstacles) {
                if (overlap(rect, o)) { free = false; break; }
            }
            if (free) {
                best = candidate;
                bestDistance = distance;
            }
        }
        rect[0] = best;
    }

    private float[] xBadge(String label, float px, ChartFont font, float h) {
        float w = text.width(label, font) + 8;
        float x = Math.max(0, Math.min(width - w, px - w / 2));
        float y = (marginBottom >= h + 2) ? marginBottom - h - 1 : marginBottom + 1;
        return new float[]{x, y, w, h};
    }

    /** Badge {x, y, w, h} of an ordinate, centered on py, to the left of the plot area. */
    private float[] yBadge(String label, float py, ChartFont font, float h) {
        float w = text.width(label, font) + 8;
        float x = (marginLeft >= w + 2) ? marginLeft - w - 1 : marginLeft + 1;
        float y = Math.max(0, Math.min(height - h, py - h / 2));
        return new float[]{x, y, w, h};
    }

    /**
     * If badge a overlaps badge b, moves a along the axis (0 = x, 1 = y) just
     * beside b, on the side of its own center, or on the other side if it
     * would leave the surface [0, limit]. Badges are {x, y, w, h}.
     */
    private static void separate(float[] a, float[] b, int axis, float limit) {
        if (!overlap(a, b)) return;
        int size = axis + 2;
        float before = b[axis] - a[size] - 2, after = b[axis] + b[size] + 2;
        boolean towardsAfter = a[axis] + a[size] / 2 >= b[axis] + b[size] / 2;
        float position = towardsAfter ? after : before;
        if (position < 0 || position + a[size] > limit) position = towardsAfter ? before : after;
        a[axis] = Math.max(0, Math.min(limit - a[size], position));
    }

    private static boolean overlap(float[] a, float[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    /** Differences between the moving cursor and the static cursor, in a badge next to the moving cursor. */
    private void drawDelta(GL2 gl, StaticCursor fixed) {
        float[] moving = cursorPosition;
        if (moving == null || cursorCurve == null) return;
        String label = "\u0394x = " + formatCursorX(cursorCurve, moving[0] - fixed.x)
                + "   \u0394y = " + CurrentValues.format(moving[1] - fixed.y);
        ChartFont font = scaleFont;
        float w = text.width(label, font) + 8, h = text.lineHeight(font) + 4;
        // Above and to the right of the marker, kept inside the plot area
        float x = Math.max(marginLeft + 1, Math.min(marginLeft + plotWidth - w - 1, cursorPixelX + 10));
        float y = Math.max(marginBottom + 1, Math.min(marginBottom + plotHeight - h - 1, cursorPixelY + 10));
        drawBadge(gl, label, font, new float[]{0.85f, 0.85f, 0.9f}, x, y, w, h);
    }

    /** Cursor axes across the plot area (dashed for the static cursor) and marker at the point (px, py). */
    private void drawAxesAndMarker(GL2 gl, float px, float py, float[] c, boolean dashed) {
        float left = marginLeft, bottom = marginBottom, right = marginLeft + plotWidth, top = marginBottom + plotHeight;

        gl.glColor4f(c[0], c[1], c[2], 0.8f);
        gl.glLineWidth(1f);
        if (dashed) {
            gl.glEnable(GL2.GL_LINE_STIPPLE);
            gl.glLineStipple(1, (short) 0x0F0F);
        }
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2f(px, bottom);
        gl.glVertex2f(px, top);
        gl.glVertex2f(left, py);
        gl.glVertex2f(right, py);
        gl.glEnd();
        if (dashed) gl.glDisable(GL2.GL_LINE_STIPPLE);

        // Marker at the designated point: filled square outlined with the background color
        gl.glColor3f(0.10f, 0.11f, 0.13f);
        gl.glRectf(px - 4, py - 4, px + 4, py + 4);
        gl.glColor3f(c[0], c[1], c[2]);
        gl.glRectf(px - 3, py - 3, px + 3, py + 3);
    }

    /** Badge: background in the curve color, text in the chart background color. */
    private void drawBadge(GL2 gl, String label, ChartFont font, float[] color, float x, float y, float w, float h) {
        gl.glColor4f(color[0], color[1], color[2], 0.95f);
        gl.glRectf(x, y, x + w, y + h);
        gl.glColor3f(0.10f, 0.11f, 0.13f);
        text.draw(gl, label, font, x + 4, y + 2);
    }

    /** Zoom rectangle being drawn: translucent background and outline (coordinates in pixels). */
    private void drawSelection(GL2 gl) {
        float x0 = Math.min(selectionX0, selectionX1), x1 = Math.max(selectionX0, selectionX1);
        float y0 = Math.min(selectionY0, selectionY1), y1 = Math.max(selectionY0, selectionY1);
        gl.glColor4f(0.35f, 0.75f, 1f, 0.15f);
        gl.glRectf(x0, y0, x1, y1);
        gl.glColor4f(0.35f, 0.75f, 1f, 0.9f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(x0, y0);
        gl.glVertex2f(x1, y0);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x0, y1);
        gl.glEnd();
    }

    // ------------------------------------------------------------------
    // Navigation

    /** Ctrl + left click on a marker label: that label becomes the current marker label. */
    @Override
    public void press(int button, int modifiers, int x, int y, int width, int height) {
        if (button == LEFT_BUTTON && (modifiers & CTRL) != 0) {
            MarkerBadge badge = markerBadgeAt(toPixelX(x, width), toPixelY(y, height));
            if (badge != null) {
                setCurrentMarkerLabel(badge.marker.label);
                return;
            }
        }
        press(button, x, y, width, height);
    }

    /** Left button: start of a zoom rectangle, if it starts in the plot area. */
    @Override
    public void press(int button, int x, int y, int width, int height) {
        float px = toPixelX(x, width), py = toPixelY(y, height);
        mouseX = px;
        mouseY = py;
        if (button != LEFT_BUTTON) return;
        selecting = px >= marginLeft && px <= marginLeft + plotWidth
                 && py >= marginBottom && py <= marginBottom + plotHeight;
        selectionX0 = selectionX1 = px;
        selectionY0 = selectionY1 = py;
    }

    /** Left button: the second corner of the rectangle follows the mouse; right button: pan. */
    @Override
    public void dragTo(int button, int x, int y, int dx, int dy, int width, int height, InputScale scale) {
        mouseX = toPixelX(x, width); // the cursor also follows the mouse during a drag
        mouseY = toPixelY(y, height);
        if (button == LEFT_BUTTON) {
            if (selecting) {
                selectionX1 = clampX(toPixelX(x, width));
                selectionY1 = clampY(toPixelY(y, height));
            }
        } else {
            drag(button, dx, dy, width, height, scale);
        }
    }

    /** Drag with the right button: pan; the content follows the mouse. */
    @Override
    public void drag(int button, int dx, int dy, int width, int height, InputScale scale) {
        if (button == RIGHT_BUTTON) pan((float) dx / Math.max(1, width), (float) dy / Math.max(1, height));
    }

    /**
     * Left button released: the rectangle becomes the zoom window. A
     * rectangle smaller than MIN_SELECTION_PIXELS in one direction keeps the
     * current range of that axis; in both directions, it is a simple click
     * and nothing changes.
     */
    @Override
    public void release(int button, int x, int y, int width, int height) {
        if (button != LEFT_BUTTON || !selecting) return;
        selecting = false;
        selectionX1 = clampX(toPixelX(x, width));
        selectionY1 = clampY(toPixelY(y, height));
        boolean zoomX = Math.abs(selectionX1 - selectionX0) >= MIN_SELECTION_PIXELS;
        boolean zoomY = Math.abs(selectionY1 - selectionY0) >= MIN_SELECTION_PIXELS;
        if (!zoomX && !zoomY) return;

        // Pixels -> data, with the view of the displayed frame
        double reference = zoomXReference();
        double xa = viewMinX, xb = viewMaxX, ya = viewMinY, yb = viewMaxY;
        if (zoomX) {
            xa = pixelToDataX(Math.min(selectionX0, selectionX1));
            xb = pixelToDataX(Math.max(selectionX0, selectionX1));
        }
        if (zoomY) {
            ya = pixelToDataY(Math.min(selectionY0, selectionY1));
            yb = pixelToDataY(Math.max(selectionY0, selectionY1));
        }
        double[] window = {xa - reference, xb - reference, ya, yb};
        if (isUsable(window)) changeZoomWindow(current -> window, true);
    }

    /**
     * Left double click in cursor mode: the static cursor is placed (or moved)
     * on the point designated by the cursor at the double click position, at
     * the next frame.
     */
    @Override
    public void doubleClick(int button, int x, int y, int width, int height) {
        doubleClick(button, 0, x, y, width, height);
    }

    /**
     * Left double click: on a marker label, removes that marker (with or
     * without cursor mode); otherwise, in cursor mode, Ctrl + double click
     * adds a marker and a plain double click places the static cursor, on
     * the point designated by the cursor at the double click position (at
     * the next frame).
     */
    @Override
    public void doubleClick(int button, int modifiers, int x, int y, int width, int height) {
        if (button != LEFT_BUTTON) return;
        float px = toPixelX(x, width), py = toPixelY(y, height);
        MarkerBadge badge = markerBadgeAt(px, py);
        if (badge != null) {
            removeMarker(badge.marker.curve, badge.marker.label, badge.marker.x, badge.marker.y);
            return;
        }
        if (!cursorEnabled) return;
        mouseX = px;
        mouseY = py;
        if ((modifiers & CTRL) != 0) {
            if (supportsMarkers()) markerRequested = true;
        } else if (supportsStaticCursor()) {
            staticCursorRequested = true;
        }
    }

    /** Mouse moved without a button: position stored for the cursor. */
    @Override
    public void move(int x, int y, int width, int height) {
        mouseX = toPixelX(x, width);
        mouseY = toPixelY(y, height);
    }

    /** Tab key used when the cursor is active on a chart with several curves. */
    @Override
    public boolean usesTabKey() {
        return cursorEnabled && getCurvesCount() > 1;
    }

    /** Wheel: zoom by one step (on the zoom window if it exists). */
    @Override
    public void wheel(float amount, InputScale scale) {
        zoom(Math.signum(amount));
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case ESCAPE: selecting = false; break;
            case 'u':
            case BACKSPACE: undoZoom(); break;
            case 'c': setCursorEnabled(!cursorEnabled); break;
            case TAB: if (cursorEnabled) selectNextCursorCurve(1); break;
            case BACK_TAB: if (cursorEnabled) selectNextCursorCurve(-1); break;
            default: super.keyPressed(key); break;
        }
    }

    /**
     * Pan. dx and dy are displacements expressed as a fraction of the
     * component size (which makes the computation independent of the screen
     * pixel density). The content follows the mouse. If a zoom window exists,
     * it is the one that moves.
     */
    public void pan(float dx, float dy) {
        if (zoomWindow != null) {
            double fx = (double) dx * width / plotWidth, fy = (double) dy * height / plotHeight;
            changeZoomWindow(w -> {
                if (w == null) return null; // window removed in the meantime
                double shiftX = -fx * (w[1] - w[0]), shiftY = fy * (w[3] - w[2]);
                return new double[]{w[0] + shiftX, w[1] + shiftX, w[2] + shiftY, w[3] + shiftY};
            }, false);
            return;
        }
        panX -= dx * width / plotWidth / zoom;
        panY += dy * height / plotHeight / zoom;
    }

    /**
     * Multiplicative zoom: steps > 0 zooms in, steps < 0 zooms out. If a zoom
     * window exists, it is shrunk or enlarged around its center.
     */
    public void zoom(float steps) {
        if (zoomWindow != null) {
            double factor = Math.pow(1.15, -steps);
            boolean onX = wheelZoomsX();
            changeZoomWindow(w -> {
                if (w == null) return null; // window removed in the meantime
                double cx = (w[0] + w[1]) / 2, hx = (w[1] - w[0]) / 2 * (onX ? factor : 1);
                double cy = (w[2] + w[3]) / 2, hy = (w[3] - w[2]) / 2 * factor;
                double[] scaled = {cx - hx, cx + hx, cy - hy, cy + hy};
                return isUsable(scaled) ? scaled : w;
            }, false);
            return;
        }
        zoom *= (float) Math.pow(1.15, steps);
        zoom = Math.max(1e-3f, Math.min(1e4f, zoom));
    }

    /** Initial view: automatic fit (or fixed scales), without zoom window or history. */
    @Override
    public void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        selecting = false;
        clearZoomWindow();
    }

    // ------------------------------------------------------------------
    // Cursor

    /**
     * Enables or disables the cursor (also with the C key). When enabled,
     * the cursor follows the first curve. Usable from any thread.
     */
    public void setCursorEnabled(boolean enabled) {
        if (enabled && !cursorEnabled) cursorCurveIndex = 0;
        if (!enabled) staticCursor = null; // disabling the cursor also removes the static cursor
        cursorEnabled = enabled;
    }

    /** Coordinates {x, y} of the static cursor (copy), or null if there is none. */
    public float[] getStaticCursorPosition() {
        StaticCursor fixed = staticCursor;
        return (fixed != null) ? new float[]{(float) fixed.x, (float) fixed.y} : null;
    }

    /**
     * Differences {Δx, Δy} between the moving cursor and the static cursor,
     * or null if one of them is missing.
     */
    public float[] getCursorDelta() {
        StaticCursor fixed = staticCursor;
        float[] moving = cursorPosition;
        if (fixed == null || moving == null) return null;
        return new float[]{(float) (moving[0] - fixed.x), (float) (moving[1] - fixed.y)};
    }

    /** Removes the static cursor (the moving cursor remains). */
    public void clearStaticCursor() { staticCursor = null; }

    public void addStaticCursorListener(StaticCursorListener listener) { if (listener != null) staticCursorListeners.add(listener); }
    public void removeStaticCursorListener(StaticCursorListener listener) { staticCursorListeners.remove(listener); }

    public boolean isCursorEnabled() { return cursorEnabled; }

    /** Curve followed by the cursor, by its position in the chart (0 = first; a missing position falls back to 0). */
    public void setCursorCurveIndex(int index) { cursorCurveIndex = Math.max(0, index); }

    /** Position of the curve followed by the cursor. */
    public int getCursorCurveIndex() { return cursorCurveIndex; }

    /** Copy of the coordinates {x, y} of the point designated by the cursor, or null if there is none. */
    public float[] getCursorPosition() {
        float[] position = cursorPosition;
        return (position != null) ? position.clone() : null;
    }

    public void addCursorListener(CursorListener listener) { if (listener != null) cursorListeners.add(listener); }
    public void removeCursorListener(CursorListener listener) { cursorListeners.remove(listener); }

    /** Moves to the next (step = 1) or previous (step = -1) curve, cyclically. */
    private void selectNextCursorCurve(int step) {
        int count = getCurvesCount();
        if (count > 0) cursorCurveIndex = Math.floorMod(Math.min(cursorCurveIndex, count - 1) + step, count);
    }

    /**
     * Looks, at each frame, for the point of the followed curve closest to
     * the mouse (on-screen distance, among the visible points): the data and
     * the view also change under a motionless mouse. Notifies the listeners
     * if the designated point changed.
     */
    private void updateCursor() {
        cursorFound = false;
        cursorCurve = null;
        float[] found = null;
        List<C> curves = frameCurves;
        if (cursorEnabled && mouseX >= 0 && !curves.isEmpty()) {
            int index = (cursorCurveIndex < curves.size()) ? cursorCurveIndex : 0;
            C curve = curves.get(index);
            double scaleX = plotWidth / (viewMaxX - viewMinX), scaleY = plotHeight / (viewMaxY - viewMinY);
            double[] xy = new double[2];
            double best = Double.POSITIVE_INFINITY, bestX = 0, bestY = 0;
            int bestIndex = -1;
            boolean xOnly = cursorFollowsXOnly();
            for (int i = firstCursorPoint(curve), n = curve.getSnapshotCount(); i < n; i++) {
                if (!cursorPoint(curve, i, xy)) continue;
                if (xy[0] < viewMinX || xy[0] > viewMaxX || xy[1] < viewMinY || xy[1] > viewMaxY) continue; // outside the view
                double dx = marginLeft + (xy[0] - viewMinX) * scaleX - mouseX;
                double dy = marginBottom + (xy[1] - viewMinY) * scaleY - mouseY;
                double d = xOnly ? dx * dx : dx * dx + dy * dy;
                if (d < best) {
                    best = d;
                    bestX = xy[0];
                    bestY = xy[1];
                    bestIndex = i;
                }
            }
            if (best < Double.POSITIVE_INFINITY) {
                cursorFound = true;
                cursorPixelX = (float) (marginLeft + (bestX - viewMinX) * scaleX);
                cursorPixelY = (float) (marginBottom + (bestY - viewMinY) * scaleY);
                cursorColor = CurveStyles.colorOf(curve);
                cursorLabelX = formatCursorX(curve, bestX);
                cursorLabelY = CurrentValues.format(bestY);
                found = new float[]{(float) bestX, (float) bestY};
                cursorCurve = curve;
                if (staticCursorRequested) { // double click: the static cursor is placed on this point
                    staticCursor = new StaticCursor(curve, bestX, bestY, cursorColor, cursorLabelX, cursorLabelY);
                }
                if (markerRequested) { // Ctrl + double click: marker with the current label on this point
                    // not strict: the curve may have left the chart since the beginning of the frame
                    Marker added = addMarkerSilently(curve, currentMarkerLabel, markerDataX(curve, bestIndex, bestX), bestY, false);
                    if (added != null) notifyMarkerAdded(added);
                }
            }
        }
        staticCursorRequested = false;
        markerRequested = false;
        if (!Arrays.equals(found, cursorPosition)) {
            cursorPosition = found;
            for (CursorListener listener : cursorListeners) {
                try {
                    listener.cursorMoved((found != null) ? found.clone() : null);
                } catch (RuntimeException e) {
                    e.printStackTrace(); // an error in a listener must not interrupt rendering
                }
            }
        }
        notifyStaticCursor(found);
    }

    /**
     * Notifies the static cursor listeners when the static cursor is placed,
     * moved or removed, or when the differences with the moving cursor
     * (position found) change.
     */
    private void notifyStaticCursor(float[] moving) {
        StaticCursor fixed = staticCursor;
        float[] delta = (fixed != null && moving != null)
                ? new float[]{(float) (moving[0] - fixed.x), (float) (moving[1] - fixed.y)} : null;
        if (fixed == notifiedStaticCursor && Arrays.equals(delta, notifiedDelta)) return;
        notifiedStaticCursor = fixed;
        notifiedDelta = delta;
        float[] position = (fixed != null) ? new float[]{(float) fixed.x, (float) fixed.y} : null;
        for (StaticCursorListener listener : staticCursorListeners) {
            try {
                listener.staticCursorChanged((position != null) ? position.clone() : null, (delta != null) ? delta.clone() : null);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    // ------------------------------------------------------------------
    // Markers

    /**
     * Label given to the markers added with Ctrl + double click (also set by
     * Ctrl + click on a marker label). Null or blank: {@link #DEFAULT_MARKER_LABEL}.
     * The {@link CurrentMarkerLabelListener}s are notified if the label changes.
     */
    public void setCurrentMarkerLabel(String label) {
        String newLabel = (label == null || label.trim().isEmpty()) ? DEFAULT_MARKER_LABEL : label;
        synchronized (markersLock) {
            if (newLabel.equals(currentMarkerLabel)) return;
            currentMarkerLabel = newLabel;
        }
        for (CurrentMarkerLabelListener listener : currentMarkerLabelListeners) {
            try {
                listener.currentMarkerLabelChanged(newLabel);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    public String getCurrentMarkerLabel() { return currentMarkerLabel; }

    /**
     * Adds a marker to the group (curve, label), drawn in the color of the
     * curve (data units: in 1D, x is the time since the first sample,
     * whatever the display mode).
     *
     * @return false if the group already has a marker with these coordinates (nothing is added)
     * @throws IllegalArgumentException if the curve is not in the chart or the label is empty
     */
    public boolean addMarker(C curve, String label, double x, double y) {
        checkMarkerLabel(label);
        Marker added = addMarkerSilently(curve, label, x, y, true);
        if (added == null) return false;
        notifyMarkerAdded(added);
        return true;
    }

    /**
     * Adds markers to the group (curve, label), which is created if needed
     * (markers already present are skipped). The listeners are notified for
     * each marker added.
     *
     * @param coordinates coordinates {x, y} of each marker, in data units
     * @throws IllegalArgumentException if the curve is not in the chart or the label is empty
     */
    public void addMarkersGroup(C curve, String label, double[][] coordinates) {
        checkMarkerLabel(label);
        List<Marker> added = new ArrayList<>();
        for (double[] xy : coordinates) {
            Marker marker = addMarkerSilently(curve, label, xy[0], xy[1], true);
            if (marker != null) added.add(marker);
        }
        for (Marker marker : added) notifyMarkerAdded(marker);
    }

    private static void checkMarkerLabel(String label) {
        if (label == null || label.trim().isEmpty()) throw new IllegalArgumentException("The marker label cannot be empty.");
    }

    /**
     * Adds a marker without notifying the listeners. The presence of the curve
     * is checked under markersLock: a curve being removed (see
     * {@link #curveRemoved}) cannot keep markers.
     *
     * @param strict throws if the curve is not in the chart (otherwise, returns null)
     * @return the marker added, or null if it already exists
     */
    private Marker addMarkerSilently(C curve, String label, double x, double y, boolean strict) {
        synchronized (markersLock) {
            if (curve == null || !getCurves().contains(curve)) {
                if (strict) throw new IllegalArgumentException("The curve is not in the chart.");
                return null;
            }
            List<Marker> group = markerGroups.computeIfAbsent(curve, k -> new LinkedHashMap<>())
                    .computeIfAbsent(label, k -> new ArrayList<>());
            for (Marker marker : group) {
                if (sameCoordinate(marker.x, x) && sameCoordinate(marker.y, y)) return null;
            }
            Marker marker = new Marker(curve, label, x, y);
            group.add(marker);
            return marker;
        }
    }

    /** Removes a marker of the group (curve, label); the group disappears with its last marker. @return false if there was no such marker */
    public boolean removeMarker(C curve, String label, double x, double y) {
        List<Marker> removed = new ArrayList<>();
        synchronized (markersLock) {
            Map<String, List<Marker>> groups = markerGroups.get(curve);
            List<Marker> group = (groups != null) ? groups.get(label) : null;
            if (group == null) return false;
            group.removeIf(m -> {
                boolean same = sameCoordinate(m.x, x) && sameCoordinate(m.y, y);
                if (same) removed.add(m);
                return same;
            });
            if (group.isEmpty()) {
                groups.remove(label);
                if (groups.isEmpty()) markerGroups.remove(curve);
            }
        }
        for (Marker marker : removed) notifyMarkerRemoved(marker);
        return !removed.isEmpty();
    }

    /** Removes the group of markers (curve, label). @return false if there was no such group */
    public boolean removeMarkersGroup(C curve, String label) {
        List<Marker> removed;
        synchronized (markersLock) {
            Map<String, List<Marker>> groups = markerGroups.get(curve);
            removed = (groups != null) ? groups.remove(label) : null;
            if (removed == null) return false;
            if (groups.isEmpty()) markerGroups.remove(curve);
        }
        for (Marker marker : removed) notifyMarkerRemoved(marker);
        return true;
    }

    /** Removes all the markers, of all the curves. */
    public void clearMarkers() {
        List<Marker> removed = new ArrayList<>();
        synchronized (markersLock) {
            for (Map<String, List<Marker>> groups : markerGroups.values()) {
                for (List<Marker> group : groups.values()) removed.addAll(group);
            }
            markerGroups.clear();
        }
        for (Marker marker : removed) notifyMarkerRemoved(marker);
    }

    /** Removes all the markers of a curve. */
    public void clearMarkers(C curve) {
        List<Marker> removed = new ArrayList<>();
        synchronized (markersLock) {
            Map<String, List<Marker>> groups = markerGroups.remove(curve);
            if (groups == null) return;
            for (List<Marker> group : groups.values()) removed.addAll(group);
        }
        for (Marker marker : removed) notifyMarkerRemoved(marker);
    }

    /**
     * A curve leaves the chart: its markers are removed (and notified), the
     * cursor stays on its curve if it is still there (otherwise it goes back
     * to the first curve), and the static cursor is removed if it was on it.
     */
    @Override
    protected void curveRemoved(C curve, int index) {
        clearMarkers(curve);
        int cursorIndex = cursorCurveIndex;
        if (cursorIndex == index) cursorCurveIndex = 0;
        else if (cursorIndex > index) cursorCurveIndex = cursorIndex - 1;
        StaticCursor fixed = staticCursor;
        if (fixed != null && fixed.curve == curve) staticCursor = null;
    }

    public void addMarkerAddedListener(MarkerAddedListener<C> listener) { if (listener != null) markerAddedListeners.add(listener); }
    public void removeMarkerAddedListener(MarkerAddedListener<C> listener) { markerAddedListeners.remove(listener); }
    public void addMarkerRemovedListener(MarkerRemovedListener<C> listener) { if (listener != null) markerRemovedListeners.add(listener); }
    public void removeMarkerRemovedListener(MarkerRemovedListener<C> listener) { markerRemovedListeners.remove(listener); }
    public void addCurrentMarkerLabelListener(CurrentMarkerLabelListener listener) { if (listener != null) currentMarkerLabelListeners.add(listener); }
    public void removeCurrentMarkerLabelListener(CurrentMarkerLabelListener listener) { currentMarkerLabelListeners.remove(listener); }

    /** Notifies the addition of a marker; called outside markersLock. */
    private void notifyMarkerAdded(Marker marker) {
        for (MarkerAddedListener<C> listener : markerAddedListeners) {
            try {
                listener.markerAdded(marker.curve, marker.label, marker.x, marker.y);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    /** Notifies the removal of a marker; called outside markersLock. */
    private void notifyMarkerRemoved(Marker marker) {
        for (MarkerRemovedListener<C> listener : markerRemovedListeners) {
            try {
                listener.markerRemoved(marker.curve, marker.label, marker.x, marker.y);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    /** Curves that have markers, in the order of the chart. */
    public List<C> getMarkersCurves() {
        List<C> result = new ArrayList<>();
        synchronized (markersLock) {
            for (C curve : getCurves()) {
                if (markerGroups.containsKey(curve)) result.add(curve);
            }
        }
        return result;
    }

    /** Labels of the marker groups of a curve, in creation order (empty list if it has none). */
    public List<String> getMarkersLabels(C curve) {
        synchronized (markersLock) {
            Map<String, List<Marker>> groups = markerGroups.get(curve);
            return (groups != null) ? new ArrayList<>(groups.keySet()) : new ArrayList<>();
        }
    }

    /** Coordinates {x, y} of the markers of the group (curve, label) (copy, in data units), or null if there is no such group. */
    public double[][] getMarkersGroup(C curve, String label) {
        synchronized (markersLock) {
            Map<String, List<Marker>> groups = markerGroups.get(curve);
            List<Marker> group = (groups != null) ? groups.get(label) : null;
            if (group == null) return null;
            double[][] coordinates = new double[group.size()][];
            for (int i = 0; i < coordinates.length; i++) coordinates[i] = new double[]{group.get(i).x, group.get(i).y};
            return coordinates;
        }
    }

    /** Same coordinate, to the precision of the computations (markers placed on the same sample are identical). */
    private static boolean sameCoordinate(double a, double b) {
        return Math.abs(a - b) <= 1e-9 * Math.max(1, Math.max(Math.abs(a), Math.abs(b)));
    }

    /** Badge of the marker label under the pixel (px, py) in the current frame (the last one drawn wins), or null. */
    private MarkerBadge markerBadgeAt(float px, float py) {
        List<MarkerBadge> badges = markerBadges;
        for (int i = badges.size() - 1; i >= 0; i--) {
            float[] r = badges.get(i).rect;
            if (px >= r[0] && px <= r[0] + r[2] && py >= r[1] && py <= r[1] + r[3]) return badges.get(i);
        }
        return null;
    }

    /**
     * Markers visible in the view: dotted vertical line and diamond in the
     * color of their curve, label at the foot of the line (those of the
     * current label are outlined). The cursor abscissa labels stay in place:
     * the marker labels move horizontally to the nearest free position, to
     * avoid them and each other.
     */
    private void drawMarkers(GL2 gl) {
        List<MarkerBadge> badges = new ArrayList<>();
        List<C> curves = frameCurves;
        List<Marker> markers = new ArrayList<>();
        List<float[]> colors = new ArrayList<>();
        synchronized (markersLock) {
            for (int k = 0; k < curves.size(); k++) { // curves of the frame: those removed since are not drawn
                Map<String, List<Marker>> groups = markerGroups.get(curves.get(k));
                if (groups == null) continue;
                float[] color = CurveStyles.colorOf(curves.get(k));
                for (List<Marker> group : groups.values()) {
                    for (Marker marker : group) {
                        markers.add(marker);
                        colors.add(color);
                    }
                }
            }
        }
        ChartFont font = scaleFont;
        float h = text.lineHeight(font) + 4;
        String current = currentMarkerLabel;
        float bottom = marginBottom, top = marginBottom + plotHeight;
        List<float[]> obstacles = cursorXBadges(font, h); // the cursor labels stay in place, the marker labels move aside
        for (int m = 0; m < markers.size(); m++) {
            Marker marker = markers.get(m);
            float[] c = colors.get(m);
            double ax = markerAxisX(marker.x);
            if (Double.isNaN(ax) || ax < viewMinX || ax > viewMaxX) continue;
            float px = (float) (marginLeft + (ax - viewMinX) * plotWidth / (viewMaxX - viewMinX));

            gl.glColor4f(c[0], c[1], c[2], 0.7f);
            gl.glLineWidth(1f);
            gl.glEnable(GL2.GL_LINE_STIPPLE);
            gl.glLineStipple(1, (short) 0x3333);
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2f(px, bottom);
            gl.glVertex2f(px, top);
            gl.glEnd();
            gl.glDisable(GL2.GL_LINE_STIPPLE);

            if (marker.y >= viewMinY && marker.y <= viewMaxY) { // diamond on the point
                float py = (float) (marginBottom + (marker.y - viewMinY) * plotHeight / (viewMaxY - viewMinY));
                gl.glColor3f(c[0], c[1], c[2]);
                gl.glBegin(GL2.GL_POLYGON);
                gl.glVertex2f(px, py - 5);
                gl.glVertex2f(px + 5, py);
                gl.glVertex2f(px, py + 5);
                gl.glVertex2f(px - 5, py);
                gl.glEnd();
            }

            float[] rect = xBadge(marker.label, px, font, h);
            placeXBadge(rect, obstacles);
            obstacles.add(rect);
            badges.add(new MarkerBadge(marker, c, rect));
        }
        for (MarkerBadge badge : badges) {
            float[] r = badge.rect;
            drawBadge(gl, badge.marker.label, font, badge.color, r[0], r[1], r[2], r[3]);
            if (badge.marker.label.equals(current)) { // current label: outlined badge
                gl.glColor3f(1f, 1f, 1f);
                gl.glLineWidth(1f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex2f(r[0], r[1]);
                gl.glVertex2f(r[0] + r[2], r[1]);
                gl.glVertex2f(r[0] + r[2], r[1] + r[3]);
                gl.glVertex2f(r[0], r[1] + r[3]);
                gl.glEnd();
            }
        }
        markerBadges = badges;
    }

    // ------------------------------------------------------------------
    // Zoom window

    /**
     * Current zoom window {xMin, xMax, yMin, yMax}, or null. Not to be
     * modified (the array is never modified in place: it can be read without
     * a lock in computeView()).
     */
    protected final double[] zoomWindow() { return zoomWindow; }

    /** Copy of the zoom window {xMin, xMax, yMin, yMax}, or null if the view is the initial view. */
    public double[] getZoomWindow() {
        double[] window = zoomWindow;
        return (window != null) ? window.clone() : null;
    }

    /** True if a zoom window is applied. */
    public boolean isZoomed() { return zoomWindow != null; }

    /**
     * Applies a zoom window (stored in the history). In 1D scroll mode, the
     * abscissas are relative to the last sample (for example -2 to -1 for the
     * second before last), like those passed to the {@link ZoomListener}s.
     * Usable from any thread.
     *
     * @throws IllegalArgumentException if a minimum is not strictly less than its maximum
     */
    public void setZoomWindow(double xMin, double xMax, double yMin, double yMax) {
        double[] window = CurveStyles.checkScales(xMin, xMax, yMin, yMax);
        if (!isUsable(window)) throw new IllegalArgumentException("Zoom window too small for the computation precision.");
        changeZoomWindow(current -> window, true);
    }

    /**
     * Goes back to the previous window of the history (U or Backspace key).
     *
     * @return false if the history is empty
     */
    public boolean undoZoom() {
        double[] restored;
        synchronized (viewLock) {
            if (zoomHistory.isEmpty()) return false;
            double[] previous = zoomHistory.pop();
            restored = (previous == INITIAL_VIEW) ? null : previous;
            zoomWindow = restored;
        }
        fireZoomChanged(restored);
        return true;
    }

    /** Removes the zoom window and the history; the listeners are notified if a window was applied. */
    protected void clearZoomWindow() {
        boolean wasZoomed;
        synchronized (viewLock) {
            wasZoomed = zoomWindow != null;
            zoomWindow = null;
            zoomHistory.clear();
        }
        if (wasZoomed) fireZoomChanged(null);
    }

    public void addZoomListener(ZoomListener listener) { if (listener != null) zoomListeners.add(listener); }
    public void removeZoomListener(ZoomListener listener) { zoomListeners.remove(listener); }

    /** Changing scales displays the new scales: the zoom window is removed. */
    @Override
    protected void setFixedScaleBounds(double... bounds) {
        super.setFixedScaleBounds(bounds);
        clearZoomWindow();
    }

    @Override
    public void setDynamicScales() {
        super.setDynamicScales();
        clearZoomWindow();
    }

    /**
     * Replaces the zoom window with change(current window), under the lock,
     * then notifies the listeners. If record, the replaced window is stored
     * in the history. change receives null if there is no window; returning
     * the window received changes nothing.
     */
    private void changeZoomWindow(UnaryOperator<double[]> change, boolean record) {
        double[] window;
        synchronized (viewLock) {
            double[] current = zoomWindow;
            window = change.apply(current);
            if (window == current) return;
            if (record) {
                zoomHistory.push((current != null) ? current : INITIAL_VIEW);
                while (zoomHistory.size() > MAX_ZOOM_HISTORY) zoomHistory.removeLast();
            }
            zoomWindow = window;
        }
        fireZoomChanged(window);
    }

    private void fireZoomChanged(double[] window) {
        for (ZoomListener listener : zoomListeners) {
            try {
                listener.zoomChanged((window != null) ? window.clone() : null);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }

    /** Finite window whose range remains measurable with the precision of the data (float). */
    private static boolean isUsable(double[] w) {
        for (int i = 0; i < 4; i += 2) {
            double span = w[i + 1] - w[i];
            if (!(span > 0) || Double.isInfinite(span)
                    || span < 1e-6 * Math.max(Math.abs(w[i]), Math.abs(w[i + 1]))) return false;
        }
        return true;
    }

    // Conversions: window units (origin at the top left) -> OpenGL pixels
    // (origin at the bottom left) -> data units. The ratio this.width / component
    // width takes high-density screens into account.

    private float toPixelX(int x, int componentWidth) {
        return (float) x * width / Math.max(1, componentWidth);
    }

    private float toPixelY(int y, int componentHeight) {
        return height - (float) y * height / Math.max(1, componentHeight);
    }

    private float clampX(float px) {
        return Math.max(marginLeft, Math.min(marginLeft + plotWidth, px));
    }

    private float clampY(float py) {
        return Math.max(marginBottom, Math.min(marginBottom + plotHeight, py));
    }

    private double pixelToDataX(float px) {
        return viewMinX + (px - marginLeft) / plotWidth * (viewMaxX - viewMinX);
    }

    private double pixelToDataY(float py) {
        return viewMinY + (py - marginBottom) / plotHeight * (viewMaxY - viewMinY);
    }

    /** Legend position: in the chart (INSIDE) or below the abscissa axis (BELOW, default). */
    public void setLegendPosition(LegendPosition position) { if (position != null) legendPosition = position; }
    public LegendPosition getLegendPosition() { return legendPosition; }
}
