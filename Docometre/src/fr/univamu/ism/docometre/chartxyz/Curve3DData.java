package fr.univamu.ism.docometre.chartxyz;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordered set of 3D points forming the curve to display, together with the
 * geometric information useful for rendering (center, bounding radius).
 *
 * "Real-time" operation: the points are stored in a circular buffer whose
 * capacity is sampleFrequency * windowTimeWidth + 1 samples (see
 * {@link CurveData}). {@link #addSamples} adds consecutive samples; when the
 * buffer is full, the oldest ones are overwritten by the most recent ones.
 */
public class Curve3DData extends CurveData<Curve3DData> implements IRTSWTXYZSerie {

    /**
     * Creates a real-time curve, initially empty; the history duration will
     * be the one of the chart it is added to ({@link #DEFAULT_WINDOW_TIME_WIDTH}
     * seconds until then, or if the chart does not impose one).
     *
     * @param sampleFrequency sampling frequency of the coordinates, in Hz
     */
    public Curve3DData(double sampleFrequency) {
        super(3, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Creates a static curve from a list of points (original behavior): the
     * window has exactly the size of the list (nominal frequency of 1 Hz).
     * addSamples() remains usable and then scrolls the curve. To be displayed
     * in a chart that does not impose a history duration: otherwise the curve
     * would be reduced to its last historySize points.
     */
    public Curve3DData(List<Point3D> points) {
        super(3, 1, Math.max(1, checkNotEmpty(points).size() - 1)); // N points span N - 1 seconds at 1 Hz
        addSamples(points);
        takeSnapshot();
    }

    /**
     * Adds consecutive samples (the last one of the list is the most
     * recent). If the window is full, the oldest samples are removed.
     */
    public synchronized void addSamples(List<Point3D> samples) {
        int n = samples.size();
        for (int i = firstUsefulSample(n); i < n; i++) {
            Point3D p = samples.get(i);
            put(p.x, p.y, p.z);
        }
        commit(n);
    }

    /**
     * Variant of {@link #addSamples(List)} taking one channel per coordinate:
     * (x[i], y[i], z[i]) forms the i-th sample. The three arrays must have
     * the same length.
     */
    public synchronized void addSamples(float[] x, float[] y, float[] z) {
        int n = x.length;
        checkSameLength(n, y.length, z.length);
        for (int i = firstUsefulSample(n); i < n; i++) {
            put(x[i], y[i], z[i]);
        }
        commit(n);
    }

    private void put(float x, float y, float z) {
        float[] ring = ring();
        int k = nextSlot();
        ring[k] = x;
        ring[k + 1] = y;
        ring[k + 2] = z;
    }

    /**
     * Loads a curve from a text file.
     * Expected format: one line per point "x y z" (separators: space, comma or semicolon).
     * Empty lines and lines starting with '#' are ignored.
     */
    public static Curve3DData loadFromFile(Path path) throws IOException {
        List<Point3D> points = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("[\\s,;]+");
                if (parts.length < 3) {
                    System.err.println("Line " + lineNumber + " ignored (invalid format): " + line);
                    continue;
                }
                try {
                    float x = Float.parseFloat(parts[0]);
                    float y = Float.parseFloat(parts[1]);
                    float z = Float.parseFloat(parts[2]);
                    points.add(new Point3D(x, y, z));
                } catch (NumberFormatException e) {
                    System.err.println("Line " + lineNumber + " ignored (invalid number): " + line);
                }
            }
        }
        return new Curve3DData(points);
    }

    /**
     * Builds a curve from an equation.
     */
    public static Curve3DData loadFromEquation() {
        List<Point3D> points = new ArrayList<>();
        double t = -Math.PI*2;
        double dt = Math.PI*4/100000;
        while (t < Math.PI*2) {
            float x = (float) (1f*Math.cos(2*t));
            float z = (float) (1f*Math.sin(2*t));
            float y = (float) (0.1*t);
            points.add(new Point3D(x, y, z));
            t = t + dt;
        }
        return new Curve3DData(points);
    }

    /** Returns a copy of the points currently in the window, from the oldest to the most recent. */
    public synchronized List<Point3D> getPoints() {
        List<Point3D> points = new ArrayList<>(count());
        float[] ring = ring();
        for (int i = 0; i < count(); i++) {
            int k = ringOffset(i);
            points.add(new Point3D(ring[k], ring[k + 1], ring[k + 2]));
        }
        return points;
    }

    /** Coordinates (interleaved x, y, z) of the last snapshot, from the oldest to the most recent. */
    public float[] getSnapshotCoordinates() { return getSnapshotData(); }

    /** Bounding box of the last snapshot (from -1 to 1 if it is empty). */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(getMinX(), getMaxX(), getMinY(), getMaxY(), getMinZ(), getMaxZ());
    }

    public float getCenterX() { return getCenter(0); }
    public float getCenterY() { return getCenter(1); }
    public float getCenterZ() { return getCenter(2); }
    public float getMinX() { return getMin(0); }
    public float getMaxX() { return getMax(0); }
    public float getMinY() { return getMin(1); }
    public float getMaxY() { return getMax(1); }
    public float getMinZ() { return getMin(2); }
    public float getMaxZ() { return getMax(2); }
    /** Half diagonal of the bounding box of the last snapshot. */
    public float getBoundingRadius() { return getBoundingBox().radius; }

    /** No effect: a 3D curve needs the three coordinates (see {@link #addPoints(Double[], Double[], Double[])}). */
    @Override
    public void addPoints(Double[] timeValues, Double[] doubleValues) {
    }

    @Override
    public void addPoints(Double[] xValues, Double[] yValues, Double[] zValues) {
        addSamples(toFloats(xValues), toFloats(yValues), toFloats(zValues));
    }
}
