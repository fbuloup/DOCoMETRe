package fr.univamu.ism.docometre.chartxyz;

/**
 * Time signal y(t) sampled at sampleFrequency Hz, of which only the last
 * windowTimeWidth seconds are kept (circular buffer with a capacity of
 * sampleFrequency * windowTimeWidth + 1, see {@link CurveData}).
 *
 * The duration windowTimeWidth is the one of the 1D chart displaying the
 * curve: it is imposed on the curve when it is added to the chart (see
 * {@link Chart1DRenderer#setWindowTimeWidth}). Until then, the curve keeps
 * {@link #DEFAULT_WINDOW_TIME_WIDTH} seconds.
 *
 * Time is not transmitted: it is derived from the sample index, the first
 * sample received being at t = 0 s, the next one at 1 / sampleFrequency, etc.
 */
public class Curve1DData extends CurveData<Curve1DData> {

    /**
     * Creates a real-time signal, initially empty; the duration kept will be
     * the one of the chart it is added to.
     *
     * @param sampleFrequency sampling frequency, in Hz
     */
    public Curve1DData(double sampleFrequency) {
        super(1, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Creates a static signal from its values (the first one at t = 0, the
     * last one at (values.length - 1) / sampleFrequency): the duration kept
     * is exactly that span, so no value is lost. addSamples() remains usable
     * and then scrolls the signal.
     * To be displayed in a chart with the same duration (getWindowTimeWidth()):
     * a shorter duration would only keep the last values.
     *
     * @param sampleFrequency sampling frequency, in Hz
     * @param values          signal values (at least two)
     * @throws IllegalArgumentException if values has fewer than two values or if sampleFrequency is not strictly positive
     */
    public Curve1DData(double sampleFrequency, float[] values) {
        super(1, sampleFrequency, durationOf(sampleFrequency, values));
        addSamples(values);
        takeSnapshot();
    }

    /**
     * Duration (s) of values.length samples at sampleFrequency Hz, from the
     * first to the last one: (values.length - 1) / sampleFrequency. Static
     * method: it can be called within the call to the parent constructor,
     * before the object exists.
     */
    private static double durationOf(double sampleFrequency, float[] values) {
        if (!(sampleFrequency > 0)) throw new IllegalArgumentException("sampleFrequency must be strictly positive.");
        if (values == null || values.length < 2) throw new IllegalArgumentException("The signal must have at least two values.");
        return (values.length - 1) / sampleFrequency;
    }

    /**
     * Adds consecutive samples (the last one of the array is the most
     * recent). If the window is full, the oldest samples are removed.
     */
    public synchronized void addSamples(float[] values) {
        float[] ring = ring();
        for (int i = firstUsefulSample(values.length); i < values.length; i++) {
            ring[nextSlot()] = values[i];
        }
        commit(values.length);
    }

    /** Adds a single sample. */
    public void addSample(float value) {
        addSamples(new float[]{value});
    }

    /** Values of the last snapshot, from the oldest to the most recent. */
    public float[] getSnapshotValues() { return getSnapshotData(); }

    /**
     * Time (s) of the last sample of the last snapshot, taking t = 0 for the
     * first sample received (negative if there is no sample).
     * Sample i of the snapshot is at getLastSampleTime() - (getSnapshotCount() - 1 - i) / sampleFrequency.
     */
    @Override
    public double getLastSampleTime() { return getSnapshotLastSampleTime(); }

    public float getMinY() { return getMin(0); }
    public float getMaxY() { return getMax(0); }
    public float getCenterY() { return getCenter(0); }

    /** The times (xValues) are ignored: they are derived from the sampling frequency. */
    @Override
    public void addPoints(Double[] xValues, Double[] yValues) {
        addSamples(toFloats(yValues));
    }
}
