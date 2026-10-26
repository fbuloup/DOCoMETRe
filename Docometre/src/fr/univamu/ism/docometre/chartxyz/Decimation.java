package fr.univamu.ism.docometre.chartxyz;

/**
 * Reduction of the number of points sent to OpenGL, without changing the
 * on-screen drawing. Computes the list of the (increasing) indices of the
 * points to draw:
 *
 * <ul>
 * <li>{@link #m4} (1D charts): "M4" reduction per pixel column. In each
 *     column, the first sample, the minimum, the maximum and the last one
 *     are kept, in their chronological order. The resulting drawing is
 *     identical to the pixel to the full drawing (the vertical stroke of the
 *     column and the links between columns are kept), with at most 4 points
 *     per column whatever the sampling frequency.</li>
 * <li>{@link #screenDistance} (2D and 3D charts): a point is only kept if it
 *     is at least epsilon pixels away from the last kept point (the first
 *     and last ones are always kept). The simplified drawing never deviates
 *     by more than epsilon pixels from the full drawing.</li>
 * </ul>
 *
 * An instance reuses its index array from one frame to the next: it is
 * specific to a renderer and must be used from its rendering thread.
 */
public final class Decimation {

    /** Pixel column (or any grouping key) of sample i. */
    public interface ColumnKey {
        long key(int i);
    }

    /** On-screen position (pixels) of point i, written into out; false if it is unknown (point behind the camera). */
    public interface ScreenPosition {
        boolean project(int i, double[] out);
    }

    private int[] indices = new int[256];
    private int count;

    /** Indices of the points to draw (the first count() elements are valid). */
    public int[] indices() { return indices; }
    /** Number of points to draw. */
    public int count() { return count; }

    /**
     * M4 reduction of the samples [from, n): consecutive samples with the
     * same key (same pixel column) are grouped and reduced to their first,
     * minimum, maximum and last elements.
     *
     * @param values values (ordinates) of the samples
     */
    public void m4(int from, int n, float[] values, ColumnKey column) {
        count = 0;
        if (n <= from) return;
        ensureCapacity(n - from);
        int start = from, minIndex = from, maxIndex = from;
        long current = column.key(from);
        for (int i = from + 1; i <= n; i++) {
            long key = (i < n) ? column.key(i) : current + 1;
            if (i == n || key != current) {
                appendColumn(start, minIndex, maxIndex, i - 1);
                if (i == n) break;
                start = minIndex = maxIndex = i;
                current = key;
            } else {
                if (values[i] < values[minIndex]) minIndex = i;
                if (values[i] > values[maxIndex]) maxIndex = i;
            }
        }
    }

    /**
     * On-screen distance filter of the points [0, n): a point is kept if it
     * is at least epsilon pixels away from the last kept point.
     */
    public void screenDistance(int n, double epsilon, ScreenPosition position) {
        count = 0;
        if (n <= 0) return;
        ensureCapacity(n);
        double[] p = new double[2];
        double epsilon2 = epsilon * epsilon;
        boolean lastKnown = position.project(0, p);
        double lastX = p[0], lastY = p[1];
        indices[count++] = 0;
        for (int i = 1; i < n - 1; i++) {
            boolean known = position.project(i, p);
            if (!known || !lastKnown) { // unknown position: the point is kept to be safe
                indices[count++] = i;
            } else {
                double dx = p[0] - lastX, dy = p[1] - lastY;
                if (dx * dx + dy * dy < epsilon2) continue;
                indices[count++] = i;
            }
            lastKnown = known;
            lastX = p[0];
            lastY = p[1];
        }
        if (n > 1) indices[count++] = n - 1;
    }

    /** Adds the indices a, b, c, d of a column, sorted and without duplicates. */
    private void appendColumn(int a, int b, int c, int d) {
        // b and c (minimum, maximum) are between a (first) and d (last): only their order must be set
        int lo = Math.min(b, c), hi = Math.max(b, c);
        append(a);
        append(lo);
        append(hi);
        append(d);
    }

    private void append(int index) {
        if (count > 0 && indices[count - 1] >= index) return; // duplicate (or already covered)
        indices[count++] = index;
    }

    private void ensureCapacity(int size) {
        if (indices.length < size) indices = new int[Math.max(size, indices.length * 2)];
    }
}
