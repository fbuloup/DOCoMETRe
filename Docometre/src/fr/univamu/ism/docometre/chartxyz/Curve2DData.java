package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D equivalent of {@link Curve3DData}: ordered set of points (x, y) stored
 * in a circular buffer with a capacity of sampleFrequency * windowTimeWidth + 1
 * (see {@link CurveData}).
 */
public class Curve2DData extends CurveData<Curve2DData> {

    /**
     * Creates a real-time 2D curve, initially empty; the history duration
     * will be the one of the chart it is added to ({@link #DEFAULT_WINDOW_TIME_WIDTH}
     * seconds until then, or if the chart does not impose one).
     *
     * @param name            name of the curve (not null nor blank), unique in its chart
     * @param sampleFrequency sampling frequency of the coordinates, in Hz
     */
    public Curve2DData(String name, double sampleFrequency) {
        super(name, 2, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Creates a static curve from its points (the first one at t = 0, the
     * last one at (points.size() - 1) / sampleFrequency): the duration kept
     * is exactly that span, so no point is lost. addSamples() remains usable
     * and then scrolls the curve.
     * To be displayed in a chart with the same duration (getWindowTimeWidth()),
     * or that does not impose one: a shorter duration would only keep the
     * last points.
     *
     * @param name            name of the curve (not null nor blank), unique in its chart
     * @param sampleFrequency sampling frequency of the points, in Hz
     * @param points          points of the curve (at least two)
     * @throws IllegalArgumentException if the name is blank, if there are fewer than two points or if sampleFrequency is not strictly positive
     */
    public Curve2DData(String name, double sampleFrequency, List<Point2D> points) {
        super(name, 2, sampleFrequency, durationOf(sampleFrequency, (points != null) ? points.size() : 0));
        addSamples(points);
        takeSnapshot();
    }

    /**
     * Adds consecutive samples (the last one of the list is the most
     * recent). If the window is full, the oldest samples are removed.
     */
    public synchronized void addSamples(List<Point2D> samples) {
        int n = samples.size();
        for (int i = firstUsefulSample(n); i < n; i++) {
            Point2D p = samples.get(i);
            put(p.x, p.y);
        }
        commit(n);
    }

    /**
     * Variant of {@link #addSamples(List)} taking one channel per coordinate:
     * (x[i], y[i]) forms the i-th sample. The two arrays must have the same
     * length.
     */
    public synchronized void addSamples(float[] x, float[] y) {
        int n = x.length;
        checkSameLength(n, y.length);
        for (int i = firstUsefulSample(n); i < n; i++) {
            put(x[i], y[i]);
        }
        commit(n);
    }

    private void put(float x, float y) {
        float[] ring = ring();
        int k = nextSlot();
        ring[k] = x;
        ring[k + 1] = y;
    }

    /** Returns a copy of the points currently in the window, from the oldest to the most recent. */
    public synchronized List<Point2D> getPoints() {
        List<Point2D> points = new ArrayList<>(count());
        float[] ring = ring();
        for (int i = 0; i < count(); i++) {
            int k = ringOffset(i);
            points.add(new Point2D(ring[k], ring[k + 1]));
        }
        return points;
    }

    /** Coordinates (interleaved x, y) of the last snapshot, from the oldest to the most recent. */
    public float[] getSnapshotCoordinates() { return getSnapshotData(); }

    public float getCenterX() { return getCenter(0); }
    public float getCenterY() { return getCenter(1); }
    public float getMinX() { return getMin(0); }
    public float getMaxX() { return getMax(0); }
    public float getMinY() { return getMin(1); }
    public float getMaxY() { return getMax(1); }

    @Override
    public void addPoints(Double[] xValues, Double[] yValues) {
        addSamples(toFloats(xValues), toFloats(yValues));
    }
}
