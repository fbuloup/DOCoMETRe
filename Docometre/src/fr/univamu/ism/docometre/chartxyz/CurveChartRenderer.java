package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Base of the real-time curve renderers (1D, 2D and 3D): list of curves
 * (modifiable from any thread), data snapshot at each frame, text (fonts,
 * legend, current values, frame rate), reduction of the number of points,
 * fixed or dynamic scales, history duration imposed on the curves. Keyboard
 * commands, in addition to those of {@link GLChartRenderer}:
 * <ul>
 * <li>P: show/hide the points</li>
 * <li>N: show/hide the legend</li>
 * <li>F: show/hide the frame rate (frames/s)</li>
 * <li>V: show/hide the current values of the curves</li>
 * <li>D: enable/disable the reduction of the number of points drawn</li>
 * </ul>
 *
 * @param <C> type of the displayed curves
 */
public abstract class CurveChartRenderer<C extends CurveData<C>> extends GLChartRenderer {

    /** Minimum on-screen distance (pixels) between two drawn points, when distance-based reduction is enabled. */
    private static final double MIN_POINT_DISTANCE = 0.5;

    // Displayed curves, in the order they were added (legend, Tab, drawing order), and the same curves by
    // name (unique). Modified together under curvesLock, from any thread; read without lock.
    private final List<C> curves = new CopyOnWriteArrayList<>();
    private final Map<String, C> curvesByName = new ConcurrentHashMap<>();
    // Weight of each palette color: number of curves of the chart displayed in that color, automatic or
    // not (guarded by curvesLock). A curve in automatic color receives a color of minimal weight.
    private final int[] colorWeights = new int[CurveStyles.getPaletteSize()];
    // Lock order: curvesLock, then the monitor of a curve (see CurveData.setName and setColor)
    private final Object curvesLock = new Object();
    protected List<C> frameCurves = new ArrayList<>(); // curves frozen for the current frame

    protected final GLText text = new GLText(); // text in a system font (tick labels, axis names, legend)
    private final float pointSize;

    // Adjustable fonts (see setScaleFont / setLegendFont)
    protected volatile ChartFont scaleFont = new ChartFont(9, SWT.NORMAL);
    protected volatile ChartFont legendFont = new ChartFont(9, SWT.NORMAL);
    protected volatile boolean showLegend = true; // legend displayed (if there is at least one curve)
    protected volatile boolean showPoints = false;

    private final FpsCounter fpsCounter = new FpsCounter(); // actual frame rate
    private volatile boolean showFps = false;
    private int displayedPoints;      // value displayed with the frame rate (updated at the same time)
    private double displayedFps = -1;
    protected int drawnPoints;        // points sent to OpenGL during the current frame

    protected final Decimation decimation = new Decimation(); // reduction of the number of points sent to OpenGL
    private volatile boolean decimationEnabled = true;
    private final CurrentValues currentValues = new CurrentValues(); // texts of the current values
    private volatile boolean showCurrentValues = false;
    protected final DashedLines dashedLines = new DashedLines(); // dashes anchored on the data

    // History duration (s) imposed on all curves; 0 = each curve keeps its own
    private volatile double windowTimeWidth;

    // Fixed scales {min1, max1, min2, max2...}, or null for dynamic scales (fitted to the data)
    private volatile double[] fixedScales;

    /**
     * @param pointSize size of the points (P key), in pixels
     */
    @SafeVarargs
    protected CurveChartRenderer(float pointSize, C... curves) {
        super(false, true);
        this.pointSize = pointSize;
        try {
            for (C curve : curves) addCurve(curve);
        } catch (RuntimeException e) {
            // chart not created: the curves already attached are released (without curveRemoved:
            // the subclasses are not initialized yet)
            for (C curve : this.curves) {
                synchronized (curve) {
                    if (curve.isAutomaticColor()) curve.assignColor(null, true);
                    curve.detach(this);
                }
            }
            this.curves.clear();
            this.curvesByName.clear();
            java.util.Arrays.fill(colorWeights, 0);
            throw e;
        }
    }

    /**
     * Adds a curve to the chart (usable while rendering, from any thread).
     * Its name must be unique in the chart: otherwise nothing is changed.
     * A curve in automatic color receives the palette color used by the
     * fewest curves of the chart (the first one of the palette in case of a
     * tie); its color then no longer changes when other curves are added or
     * removed.
     *
     * @throws IllegalStateException    if the curve is already displayed in a chart (this one included)
     * @throws IllegalArgumentException if another curve of the chart has the same name
     */
    public void addCurve(C curve) {
        synchronized (curvesLock) {
            synchronized (curve) { // its name cannot change during the check (see CurveData.setName)
                curve.attach(this);
                String name = curve.getName();
                if (curvesByName.putIfAbsent(name, curve) != null) {
                    curve.detach(this);
                    throw new IllegalArgumentException("The chart already has a curve named \"" + name + "\".");
                }
                if (curve.isAutomaticColor()) curve.assignColor(takeAutomaticColor(), true);
                else countColor(curve.color(), 1);
            }
            double window = windowTimeWidth;
            if (window > 0) curve.setWindowTimeWidth(window);
            curves.add(curve);
        }
    }

    /**
     * Renames a curve of this chart (called by {@link CurveData#setName}),
     * checking that the new name is not used by another curve.
     *
     * @return false if the curve is no longer in this chart (nothing done)
     * @throws IllegalArgumentException if another curve of the chart has this name
     */
    @SuppressWarnings("unchecked")
    boolean renameCurve(CurveData<?> curve, String name) {
        synchronized (curvesLock) {
            synchronized (curve) {
                if (!curve.isOwnedBy(this)) return false;
                String old = curve.getName();
                if (old.equals(name)) return true;
                if (curvesByName.putIfAbsent(name, (C) curve) != null) {
                    throw new IllegalArgumentException("The chart already has a curve named \"" + name + "\".");
                }
                curvesByName.remove(old);
                curve.assignName(name);
                return true;
            }
        }
    }

    /**
     * Changes the color of a curve of this chart (called by
     * {@link CurveData#setColor} and {@link CurveData#setAutomaticColor}):
     * the weight of its old color decreases, the one of its new color
     * increases (palette colors only).
     *
     * @param color new color, or null for an automatic color (a color of minimal weight)
     * @return false if the curve is no longer in this chart (nothing done)
     */
    boolean recolorCurve(CurveData<?> curve, CurveColor color) {
        synchronized (curvesLock) {
            synchronized (curve) {
                if (!curve.isOwnedBy(this)) return false;
                countColor(curve.color(), -1);
                if (color == null) {
                    curve.assignColor(takeAutomaticColor(), true);
                } else {
                    countColor(color, 1);
                    curve.assignColor(color, false);
                }
                return true;
            }
        }
    }

    /** Palette color of minimal weight (the first one in case of a tie), whose weight is increased (curvesLock held). */
    private CurveColor takeAutomaticColor() {
        int best = 0;
        for (int i = 1; i < colorWeights.length; i++) {
            if (colorWeights[i] < colorWeights[best]) best = i;
        }
        colorWeights[best]++;
        return CurveStyles.paletteColor(best);
    }

    /** Adds delta to the weight of the color if it is a palette color (curvesLock held). */
    private void countColor(CurveColor color, int delta) {
        int index = CurveStyles.paletteIndex(color);
        if (index >= 0) colorWeights[index] += delta;
    }

    /** Copy of the weights of the palette colors (tests). */
    int[] getColorWeights() {
        synchronized (curvesLock) {
            return colorWeights.clone();
        }
    }

    /** Curve of the chart with this name, or null. Usable from any thread. */
    public C getCurve(String name) {
        return (name != null) ? curvesByName.get(name) : null;
    }

    /**
     * History duration, in seconds, imposed on all the curves of the chart,
     * present and future (their capacity becomes frequency x duration + 1;
     * the most recent samples are kept). In 1D, it is the width of the time
     * axis; in 2D and 3D, the length of the trails.
     * Usable while rendering, from any thread.
     */
    public void setWindowTimeWidth(double windowTimeWidth) {
        if (!(windowTimeWidth > 0) || Double.isInfinite(windowTimeWidth)) {
            throw new IllegalArgumentException("windowTimeWidth must be strictly positive.");
        }
        this.windowTimeWidth = windowTimeWidth;
        for (C curve : getCurves()) curve.setWindowTimeWidth(windowTimeWidth);
    }

    /** History duration imposed on the curves, in seconds (0 if each curve keeps its own). */
    public double getWindowTimeWidth() { return windowTimeWidth; }

    /**
     * Removes a curve from the chart; it can then be added to another chart,
     * and its name is available again in this one.
     *
     * @return false if the curve was not in the chart
     */
    public boolean removeCurve(C curve) {
        int index;
        synchronized (curvesLock) {
            index = curves.indexOf(curve);
            if (index < 0) return false;
            curves.remove(index);
            curvesByName.remove(curve.getName(), curve);
            synchronized (curve) {
                countColor(curve.color(), -1); // palette color: one curve fewer, automatic or not
                if (curve.isAutomaticColor()) curve.assignColor(null, true); // chosen again in its next chart
                curve.detach(this);
            }
        }
        curveRemoved(curve, index); // outside the lock: listeners may be called
        return true;
    }

    /**
     * Removes the curve with this name from the chart (see {@link #removeCurve(CurveData)}).
     *
     * @return false if the chart has no curve with this name
     */
    public boolean removeCurve(String name) {
        while (true) {
            C curve = getCurve(name);
            if (curve == null) return false;
            if (removeCurve(curve)) return true;
            // removed or renamed meanwhile by another thread: look again
        }
    }

    /**
     * Called after the removal of a curve (in the calling thread), to release
     * what belongs to it. Does nothing by default.
     *
     * @param index position the curve had in the chart
     */
    protected void curveRemoved(C curve, int index) { }

    /** Removes all the curves from the chart (called when it is closed). */
    public void removeAllCurves() {
        for (C curve : curves) removeCurve(curve);
    }
    /** Curves currently displayed. */
    public List<C> getCurves() { return new ArrayList<>(curves); }
    /** Number of curves currently displayed (without copying the list). */
    public int getCurvesCount() { return curves.size(); }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        drawable.getGL().getGL2().glPointSize(pointSize);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        text.dispose(drawable.getGL().getGL2()); // text textures
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        text.setPixelScale(GLText.surfaceScale(drawable));
    }

    /** To be called at the beginning of each frame: frame rate measurement and reset of the point count. */
    protected void beginFrame() {
        fpsCounter.frame();
        drawnPoints = 0;
    }

    /** Freezes the curves and the data received since the previous frame. */
    protected void snapshotCurves() {
        frameCurves = new ArrayList<>(curves);
        for (C curve : frameCurves) curve.takeSnapshot();
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case 'p': togglePoints(); break;
            case 'n': toggleLegend(); break;
            case 'f': toggleFps(); break;
            case 'v': toggleCurrentValues(); break;
            case 'd': toggleDecimation(); break;
            default: super.keyPressed(key); break;
        }
    }

    /** Reduction applied if it is enabled, if the points are not displayed and if the curve has more than minCount points. */
    protected boolean useDecimation(int pointCount, int minCount) {
        return decimationEnabled && !showPoints && pointCount > minCount;
    }

    // ------------------------------------------------------------------
    // Common decorations (coordinates in pixels)

    /** Actual frame rate and number of points drawn, right-aligned on xRight, in the scale font. */
    protected void drawFps(GL2 gl, float xRight, float y) {
        if (!showFps) return;
        double fps = fpsCounter.getFps();
        if (fps != displayedFps) { // the number of points is updated together with the frame rate (twice per second)
            displayedFps = fps;
            displayedPoints = drawnPoints;
        }
        String label = String.format(Locale.US, "%.1f fps · %d pts", fps, displayedPoints);
        gl.glColor3f(1f, 0.75f, 0.2f);
        text.draw(gl, label, scaleFont, xRight - text.width(label, scaleFont), y);
    }

    /**
     * Current values (the last sample of each curve), right-aligned on xRight
     * below yTop, in the color of each curve and the scale font.
     *
     * @return the height used in pixels (0 if nothing is displayed)
     */
    protected float drawCurrentValues(GL2 gl, float xRight, float yTop) {
        if (!showCurrentValues) return 0;
        List<C> curves = frameCurves;
        String[] labels = currentValues.update(text, scaleFont, curves, k -> curves.get(k).formatLastSample());
        return CurveStyles.drawCurrentValues(gl, text, scaleFont, curves, labels, xRight, yTop);
    }

    // ------------------------------------------------------------------
    // Drawing of trajectories (2D and 3D curves)

    /** On-screen projection (pixels) of a data point; false if it is unknown (point behind the camera). */
    protected interface ScreenProjection {
        boolean project(float x, float y, float z, double[] out);
    }

    /**
     * Draws the 2D or 3D curves. A single curve without an explicit color
     * keeps the blue -> orange gradient; otherwise each curve has its color
     * (explicit or from the palette) and fades towards its oldest samples.
     * Dashed curves have a pattern anchored on the data; the others are
     * reduced (if reduction is enabled) by keeping only the points at least
     * MIN_POINT_DISTANCE pixel apart on screen.
     *
     * @param projection on-screen projection of the points (z = 0 in 2D)
     * @param scaleX     horizontal display scale, used to detect scale changes for the dashes
     * @param scaleY     vertical display scale
     */
    protected void drawTrajectories(GL2 gl, float defaultLineWidth, ScreenProjection projection, double scaleX, double scaleY) {
        for (int k = 0; k < frameCurves.size(); k++) {
            C curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, defaultLineWidth);
            gl.glLineWidth(width);
            float[] c = curve.getSnapshotData();
            int dim = curve.getDimension();
            int n = curve.getSnapshotCount();
            boolean gradient = curve.isAutomaticColor() && frameCurves.size() == 1;
            float[] color = CurveStyles.colorOf(curve);
            Decimation.ScreenPosition position = (i, out) ->
                    projection.project(c[dim * i], c[dim * i + 1], (dim > 2) ? c[dim * i + 2] : 0f, out);

            if (CurveStyles.isDashed(curve)) {
                double[] a = new double[2], b = new double[2];
                dashedLines.draw(gl, curve, width, curve.getSnapshotCapacity(), curve.getSnapshotTotalSamples(), n, 0, scaleX, scaleY,
                        i -> (position.project(i - 1, a) && position.project(i, b)) ? Math.hypot(b[0] - a[0], b[1] - a[1]) : 0,
                        null,
                        (i, f) -> {
                            CurveStyles.vertexColor(gl, gradient, color, (n <= 1) ? 1f : (i - 1 + f) / (n - 1));
                            interpolatedVertex(gl, c, dim, i, f);
                        });
                drawnPoints += n;
            } else {
                int[] kept = null;
                int count = n;
                if (useDecimation(n, 2)) {
                    decimation.screenDistance(n, MIN_POINT_DISTANCE, position);
                    kept = decimation.indices();
                    count = decimation.count();
                }
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < count; j++) {
                    int i = (kept == null) ? j : kept[j];
                    CurveStyles.vertexColor(gl, gradient, color, (n <= 1) ? 1f : (float) i / (n - 1));
                    vertex(gl, c, dim, i);
                }
                gl.glEnd();
                drawnPoints += count;
            }
        }
    }

    /** Points of all the 2D or 3D curves, in white. */
    protected void drawTrajectoryPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (C curve : frameCurves) {
            float[] c = curve.getSnapshotData();
            int dim = curve.getDimension();
            for (int i = 0; i < curve.getSnapshotCount(); i++) vertex(gl, c, dim, i);
        }
        gl.glEnd();
    }

    /** Vertex i of an array of interleaved coordinates (2 or 3 per point). */
    private static void vertex(GL2 gl, float[] c, int dim, int i) {
        int k = dim * i;
        if (dim == 2) gl.glVertex2f(c[k], c[k + 1]);
        else gl.glVertex3f(c[k], c[k + 1], c[k + 2]);
    }

    /** Vertex located between points i-1 and i, at fraction f (0 = i-1, 1 = i). */
    private static void interpolatedVertex(GL2 gl, float[] c, int dim, int i, float f) {
        int a = dim * (i - 1), b = dim * i;
        float x = c[a] + f * (c[b] - c[a]);
        float y = c[a + 1] + f * (c[b + 1] - c[a + 1]);
        if (dim == 2) gl.glVertex2f(x, y);
        else gl.glVertex3f(x, y, c[a + 2] + f * (c[b + 2] - c[a + 2]));
    }

    // ------------------------------------------------------------------
    // Scales

    /** Stores fixed scales {min1, max1, min2, max2...}, after checking them. */
    protected void setFixedScaleBounds(double... bounds) {
        fixedScales = CurveStyles.checkScales(bounds);
    }
    /** Current fixed scales, or null if they are dynamic. */
    protected double[] getFixedScaleBounds() { return fixedScales; }
    /** Dynamic scales, continuously fitted to the curve values (default). */
    public void setDynamicScales() { fixedScales = null; }
    /** True if the scales are fixed. */
    public boolean hasFixedScales() { return fixedScales != null; }

    // ------------------------------------------------------------------
    // Settings

    /** Font of the scale tick labels. */
    public void setScaleFont(ChartFont font) { if (font != null) scaleFont = font; }
    public ChartFont getScaleFont() { return scaleFont; }
    /** Font of the legend. */
    public void setLegendFont(ChartFont font) { if (font != null) legendFont = font; }
    public ChartFont getLegendFont() { return legendFont; }

    public void togglePoints() { showPoints = !showPoints; }

    /** Shows or hides the legend (applied at the next frame). */
    public void setLegendVisible(boolean visible) { showLegend = visible; }
    public boolean isLegendVisible() { return showLegend; }
    /** Toggles the legend display (N key). */
    public void toggleLegend() { showLegend = !showLegend; }

    /** Actual measured frame rate, in frames per second. */
    public double getFps() { return fpsCounter.getFps(); }
    /** Shows or hides the frame rate in the chart (F key). */
    public void setFpsVisible(boolean visible) { showFps = visible; }
    public boolean isFpsVisible() { return showFps; }
    public void toggleFps() { showFps = !showFps; }

    /**
     * Enables or disables the reduction of the number of points sent to
     * OpenGL (enabled by default, D key). The on-screen drawing is unchanged;
     * dashed curves and the points display (P key) are not reduced.
     */
    public void setDecimationEnabled(boolean enabled) { decimationEnabled = enabled; }
    public boolean isDecimationEnabled() { return decimationEnabled; }
    public void toggleDecimation() { decimationEnabled = !decimationEnabled; }
    /** Number of points sent to OpenGL to draw the curves during the last frame. */
    public int getDrawnPointCount() { return drawnPoints; }

    /** Shows or hides the current values of the curves, at the top right (V key). */
    public void setCurrentValuesVisible(boolean visible) { showCurrentValues = visible; }
    public boolean isCurrentValuesVisible() { return showCurrentValues; }
    public void toggleCurrentValues() { showCurrentValues = !showCurrentValues; }
}
