package fr.univamu.ism.docometre.chartxyz;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Common base of the real-time curves ({@link Curve1DData}, {@link Curve2DData},
 * {@link Curve3DData}): a sample is made of {@link #getDimension()}
 * coordinates (y; x, y; x, y, z), stored interleaved in a circular buffer
 * with a capacity of sampleFrequency * windowTimeWidth + 1 samples:
 * windowTimeWidth is the span between the first and the last sample kept
 * (N samples last (N - 1) / sampleFrequency seconds).
 *
 * When the buffer is full, the oldest samples are overwritten by the most
 * recent ones: only the last windowTimeWidth seconds of signal are kept and
 * displayed.
 *
 * Concurrent access: samples can be added from any thread (an acquisition
 * thread, for example). The renderer calls {@link #takeSnapshot()} once at
 * the beginning of each frame: this method freezes a copy of the samples and
 * recomputes the bounds. The accessors (getMin(), getSnapshotData(), ...)
 * return the values of this last snapshot, which therefore remain consistent
 * during the whole frame.
 *
 * A curve can only be displayed in one chart at a time (see {@link #attach}:
 * adding it to a second chart throws an exception). It becomes available
 * again when it is removed from its chart or when that chart is closed.
 *
 * Every curve has a name, given to the constructor and changeable with
 * {@link #setName}: it identifies the curve in its chart, where it must be
 * unique (see {@link CurveChartRenderer#getCurve(String)}). The appearance
 * (name, color, line width, line style) is common to all curves; its setters
 * return the curve itself (type T) to chain calls.
 *
 * @param <T> concrete curve type
 */
public abstract class CurveData<T extends CurveData<T>> implements CurveAppearance, IRTSWTSerie {

    /** Duration kept (s) as long as the curve is not added to a chart that imposes its own. */
    public static final double DEFAULT_WINDOW_TIME_WIDTH = 10;

    private final int dimension;           // coordinates per sample
    private final double sampleFrequency;  // Hz
    private double windowTimeWidth;        // s, guarded by "this"
    private int capacity;                  // max number of samples kept, guarded by "this"

    // Circular buffer (interleaved coordinates), guarded by "this"
    private float[] ring;
    private int head = 0;            // index (in samples) of the next write
    private int count = 0;           // number of valid samples in the buffer
    private long totalSamples = 0;   // total number of samples received since the beginning
    private boolean modified = true;

    // Snapshot used for rendering (in chronological order)
    private float[] snapshot = new float[0];
    private int snapshotCount = 0;
    private int snapshotCapacity = 1;
    private long snapshotTotalSamples = 0;
    private final float[] min, max; // bounds of the snapshot, per coordinate

    // Name (unique in the chart; written under the monitor of the curve, see setName) and appearance
    private volatile String name;
    // Displayed color: given by setColor, or chosen by the chart (automatic color; null outside a chart).
    // Written under the monitor of the curve (and the lock of its chart, see CurveChartRenderer.recolorCurve).
    private volatile CurveColor color;
    private volatile boolean automaticColor = true;
    private volatile float lineWidth = 0f; // 0 = default line width of the chart
    private volatile LineStyle lineStyle = LineStyle.SOLID;

    // Chart (renderer) displaying the curve, or null
    private Object owner;

    /**
     * Creates a real-time curve, initially empty.
     *
     * @param name            name of the curve (not null nor blank), unique in its chart
     * @param dimension       number of coordinates per sample (1, 2 or 3)
     * @param sampleFrequency sampling frequency, in Hz
     * @param windowTimeWidth duration of the history kept and displayed, in seconds
     * @throws IllegalArgumentException if the name is null or blank, or if sampleFrequency is not strictly positive
     */
    protected CurveData(String name, int dimension, double sampleFrequency, double windowTimeWidth) {
        this.name = checkName(name);
        if (!(sampleFrequency > 0)) {
            throw new IllegalArgumentException("sampleFrequency must be strictly positive.");
        }
        checkWindowTimeWidth(windowTimeWidth);
        this.dimension = dimension;
        this.sampleFrequency = sampleFrequency;
        this.windowTimeWidth = windowTimeWidth;
        this.capacity = capacityFor(windowTimeWidth);
        this.ring = new float[capacity * dimension];
        this.min = new float[dimension];
        this.max = new float[dimension];
        takeSnapshot();
    }

    /** Checks a curve name (not null nor blank) and returns it. */
    static String checkName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("The curve name cannot be empty.");
        return name;
    }

    /**
     * Duration (s) of n samples at sampleFrequency Hz, from the first to the
     * last one: (n - 1) / sampleFrequency. Static method: it can be called
     * within the call to the parent constructor, before the object exists.
     *
     * @throws IllegalArgumentException if n is less than 2 or if sampleFrequency is not strictly positive
     */
    protected static double durationOf(double sampleFrequency, int n) {
        if (!(sampleFrequency > 0)) throw new IllegalArgumentException("sampleFrequency must be strictly positive.");
        if (n < 2) throw new IllegalArgumentException("A static curve must have at least two samples.");
        return (n - 1) / sampleFrequency;
    }

    private static void checkWindowTimeWidth(double windowTimeWidth) {
        if (!(windowTimeWidth > 0) || Double.isInfinite(windowTimeWidth)) {
            throw new IllegalArgumentException("windowTimeWidth must be strictly positive.");
        }
    }

    /** N samples span (N - 1) / sampleFrequency seconds: sampleFrequency * duration + 1 are needed. */
    private int capacityFor(double windowTimeWidth) {
        return (int) Math.max(1, Math.round(sampleFrequency * windowTimeWidth) + 1);
    }

    /**
     * Changes the duration of the history kept (and therefore the buffer
     * capacity). The most recent samples are kept, within the limit of the
     * new capacity. Usable from any thread.
     */
    protected synchronized void setWindowTimeWidth(double windowTimeWidth) {
        checkWindowTimeWidth(windowTimeWidth);
        this.windowTimeWidth = windowTimeWidth;
        int newCapacity = capacityFor(windowTimeWidth);
        if (newCapacity == capacity) return;
        int kept = Math.min(count, newCapacity);
        float[] newRing = new float[newCapacity * dimension];
        for (int i = 0; i < kept; i++) { // the "kept" most recent ones, from the oldest to the most recent
            System.arraycopy(ring, ringOffset(count - kept + i), newRing, i * dimension, dimension);
        }
        ring = newRing;
        capacity = newCapacity;
        count = kept;
        head = kept % newCapacity;
        modified = true;
    }

    // ------------------------------------------------------------------
    // Chart ownership

    /**
     * Attaches the curve to the chart owner (called by the renderer when the
     * curve is added to it).
     *
     * @throws IllegalStateException if the curve is already displayed in a chart (this one included)
     */
    synchronized void attach(Object owner) {
        if (this.owner != null) {
            throw new IllegalStateException("The curve \"" + name + "\" is already displayed in a chart: "
                    + "a curve can only belong to one chart.");
        }
        this.owner = owner;
    }

    /** True if the curve is displayed in the chart owner. */
    synchronized boolean isOwnedBy(Object owner) {
        return this.owner == owner;
    }

    /** Changes the name without any check: called by the chart, under its lock and the monitor of the curve. */
    void assignName(String name) {
        this.name = name;
    }

    /** Detaches the curve from owner (no effect if it does not belong to it). */
    synchronized void detach(Object owner) {
        if (this.owner == owner) this.owner = null;
    }

    /** True if the curve is currently displayed in a chart. */
    public synchronized boolean isAttached() {
        return owner != null;
    }

    /** The concrete type, for chainable setters. */
    @SuppressWarnings("unchecked")
    protected final T self() { return (T) this; }

    // ------------------------------------------------------------------
    // Writing (called by subclasses, under the "this" lock)

    /**
     * Index of the first sample to write among n received: there is no point
     * writing those that would be overwritten at once.
     */
    protected final int firstUsefulSample(int n) { // under the lock
        return Math.max(0, n - capacity);
    }

    /**
     * Reserves the slot of the next sample in the buffer and returns the
     * index of its first coordinate in {@link #ring()}.
     */
    protected final int nextSlot() {
        int k = head * dimension;
        head = (head + 1) % capacity;
        if (count < capacity) count++;
        return k;
    }

    /** Circular buffer, for writing the coordinates reserved by {@link #nextSlot()}. */
    protected final float[] ring() { return ring; }

    /** Completes the addition of n samples (whether they have all been written or not). */
    protected final void commit(int n) {
        totalSamples += n;
        modified = true;
    }

    /** Removes all samples and resets time to zero. */
    public synchronized void clear() {
        head = 0;
        count = 0;
        totalSamples = 0;
        modified = true;
    }

    // ------------------------------------------------------------------
    // Snapshot

    /**
     * Freezes the current state of the buffer for rendering: copies the
     * samples in chronological order and recomputes the bounds.
     * Called by the renderer at the beginning of each frame.
     *
     * @return true if the data changed since the previous snapshot
     */
    public synchronized boolean takeSnapshot() {
        if (!modified) return false;
        modified = false;

        // The oldest sample is at "head" if the buffer is full, at 0 otherwise
        if (snapshot.length < count * dimension) snapshot = new float[capacity * dimension];
        int oldest = oldestIndex();
        int firstPart = Math.min(count, capacity - oldest);
        System.arraycopy(ring, oldest * dimension, snapshot, 0, firstPart * dimension);
        System.arraycopy(ring, 0, snapshot, firstPart * dimension, (count - firstPart) * dimension);
        snapshotCount = count;
        snapshotCapacity = capacity;
        snapshotTotalSamples = totalSamples;

        if (count == 0) {
            java.util.Arrays.fill(min, -1f);
            java.util.Arrays.fill(max, 1f);
        } else {
            java.util.Arrays.fill(min, Float.MAX_VALUE);
            java.util.Arrays.fill(max, -Float.MAX_VALUE);
            for (int k = 0; k < count * dimension; k += dimension) {
                for (int d = 0; d < dimension; d++) {
                    float v = snapshot[k + d];
                    if (v < min[d]) min[d] = v;
                    if (v > max[d]) max[d] = v;
                }
            }
        }
        return true;
    }

    /** Index in the buffer of the oldest sample (to be called under the lock). */
    protected final int oldestIndex() {
        return (count == capacity) ? head : 0;
    }

    /** Number of samples currently in the buffer (to be called under the lock). */
    protected final int count() { return count; }

    /** Index in the buffer of the first coordinate of the i-th oldest sample (to be called under the lock). */
    protected final int ringOffset(int i) {
        return ((oldestIndex() + i) % capacity) * dimension;
    }

    /** Interleaved coordinates of the last snapshot, from the oldest to the most recent. */
    public float[] getSnapshotData() { return snapshot; }
    /** Number of samples of the last snapshot. */
    public int getSnapshotCount() { return snapshotCount; }
    /** Buffer capacity at the time of the last snapshot. */
    public int getSnapshotCapacity() { return snapshotCapacity; }
    /**
     * Total number of samples received at the time of the last snapshot:
     * point i of the snapshot is sample number
     * getSnapshotTotalSamples() - getSnapshotCount() + i since the beginning.
     */
    public long getSnapshotTotalSamples() { return snapshotTotalSamples; }
    /**
     * Time (s) of the last sample of the last snapshot, taking t = 0 for the
     * first sample received (negative if there is no sample).
     */
    public double getSnapshotLastSampleTime() { return (snapshotTotalSamples - 1) / sampleFrequency; }

    /** Minimum of the coordinate axis (0 = first) in the last snapshot (-1 if it is empty). */
    public float getMin(int axis) { return min[axis]; }
    /** Maximum of the coordinate axis in the last snapshot (1 if it is empty). */
    public float getMax(int axis) { return max[axis]; }
    /** Middle of the interval [min, max] of the coordinate axis in the last snapshot. */
    public float getCenter(int axis) { return (min[axis] + max[axis]) / 2f; }

    /**
     * Last sample of the last snapshot, formatted for the current values
     * display: "v" in 1D, "(x, y)" or "(x, y, z)" otherwise; null if the
     * snapshot is empty.
     */
    public String formatLastSample() {
        int n = snapshotCount;
        if (n == 0) return null;
        int k = (n - 1) * dimension;
        if (dimension == 1) return CurrentValues.format(snapshot[k]);
        StringBuilder text = new StringBuilder("(");
        for (int d = 0; d < dimension; d++) {
            if (d > 0) text.append(", ");
            text.append(CurrentValues.format(snapshot[k + d]));
        }
        return text.append(')').toString();
    }

    // ------------------------------------------------------------------
    // Characteristics

    /** Number of coordinates per sample. */
    public int getDimension() { return dimension; }
    public double getSampleFrequency() { return sampleFrequency; }
    /** Duration of the history kept, in seconds. */
    public synchronized double getWindowTimeWidth() { return windowTimeWidth; }
    /** Maximum number of samples kept (sampleFrequency * windowTimeWidth + 1). */
    public synchronized int getCapacity() { return capacity; }
    /** Total number of samples received since creation (or the last clear()). */
    public synchronized long getTotalSamples() { return totalSamples; }
    /** Time (s) of the last sample received, taking t = 0 for the first one. */
    public synchronized double getLastSampleTime() { return (totalSamples - 1) / sampleFrequency; }

    // ------------------------------------------------------------------
    // Appearance

    /**
     * Changes the name of the curve (displayed in the legend). In a chart,
     * the name must remain unique: if another curve of the chart already has
     * this name, the name is not changed. Usable from any thread. Returns
     * this to chain calls.
     *
     * @throws IllegalArgumentException if the name is null or blank, or if another curve of its chart has this name
     */
    public T setName(String name) {
        checkName(name);
        while (true) {
            Object chart;
            synchronized (this) {
                if (owner == null) { // not in a chart: nothing to compare with
                    this.name = name;
                    return self();
                }
                chart = owner;
            }
            // In a chart: checked and changed under its lock; false if the curve left it meanwhile (retry)
            if (((CurveChartRenderer<?>) chart).renameCurve(this, name)) return self();
        }
    }
    @Override
    public String getName() { return name; }

    /**
     * Color of the curve, components between 0 and 255. It no longer changes
     * automatically. Usable from any thread. Returns this to chain calls.
     *
     * @throws IllegalArgumentException if a component is outside 0..255
     */
    public T setColor(int r, int g, int b) {
        changeColor(new CurveColor(r, g, b));
        return self();
    }

    /**
     * Goes back to the automatic color: in a chart, the palette color used by
     * the fewest curves (see {@link CurveStyles#getPaletteColor}); outside a
     * chart, the color is chosen when the curve is added to one. Usable from
     * any thread. Returns this to chain calls.
     */
    public T setAutomaticColor() {
        changeColor(null);
        return self();
    }

    /** Changes the color (null = automatic color); in a chart, under its lock, so that its color weights remain right. */
    private void changeColor(CurveColor newColor) {
        while (true) {
            Object chart;
            synchronized (this) {
                if (owner == null) { // not in a chart: no weight to update
                    assignColor(newColor, newColor == null);
                    return;
                }
                chart = owner;
            }
            if (((CurveChartRenderer<?>) chart).recolorCurve(this, newColor)) return;
            // the curve left its chart meanwhile: retry
        }
    }

    /**
     * Color {r, g, b} (0..255) actually displayed (copy): the one given by
     * {@link #setColor} or the automatic one; null for a curve in automatic
     * color that is in no chart (its color is chosen when it is added).
     */
    public int[] getColor() {
        CurveColor c = color;
        return (c != null) ? c.toRGB() : null;
    }

    /** True if the color is chosen automatically in the palette (default), false after {@link #setColor}. */
    public boolean isAutomaticColor() { return automaticColor; }

    /** Displayed color (internal: rendering and color weights of the chart). */
    CurveColor color() { return color; }

    /** Changes the color without any check: called under the monitor of the curve (and the lock of its chart). */
    void assignColor(CurveColor color, boolean automatic) {
        this.color = color;
        this.automaticColor = automatic;
    }

    /**
     * Line width in pixels (1, 2, 3...), or 0 to go back to the default line
     * width of the chart. Returns this to chain calls.
     */
    public T setLineWidth(float lineWidth) {
        if (!(lineWidth >= 0)) throw new IllegalArgumentException("The line width must be positive or zero.");
        this.lineWidth = lineWidth;
        return self();
    }
    @Override
    public float getLineWidth() { return lineWidth; }

    /**
     * Line style: SOLID (default), DASH, DASH_DOT or DASH_DASH_DOT. Combines
     * with the line width. Returns this to chain calls.
     */
    public T setLineStyle(LineStyle lineStyle) {
        this.lineStyle = (lineStyle != null) ? lineStyle : LineStyle.SOLID;
        return self();
    }
    @Override
    public LineStyle getLineStyle() { return lineStyle; }

    // ------------------------------------------------------------------
    // IRTSWTSerie

    /** Identifier of the series: its name. */
    @Override
    public String getId() {
        return getName();
    }

    /** No effect: the current values display is set for the whole chart (V key). */
    @Override
    public void setDisplayCurrentValue(boolean displayCurrentValues) {
    }

    @Override
    public void setThickness(int thickness) {
        setLineWidth(thickness);
    }

    /** Conversion of the arrays received through the IRTSWTSerie interface. */
    protected static float[] toFloats(Double[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) result[i] = values[i].floatValue();
        return result;
    }

    /** Checks that coordinate lists describe the same number of samples. */
    protected static void checkSameLength(int n, int... others) {
        for (int other : others) {
            if (other != n) throw new IllegalArgumentException("The coordinate arrays must have the same length.");
        }
    }
}
