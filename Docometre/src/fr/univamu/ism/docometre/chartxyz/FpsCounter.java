package fr.univamu.ism.docometre.chartxyz;

/**
 * Measurement of the actual frame rate (frames per second): frame() is
 * called at the beginning of each frame drawn, and the rate is recomputed
 * every MEASURE_PERIOD_NS from the number of frames and the time actually
 * elapsed.
 */
public final class FpsCounter {

    private static final long MEASURE_PERIOD_NS = 500_000_000L; // 0.5 s

    private long periodStart = -1;
    private int frames;
    private volatile double fps;

    /** To be called at the beginning of each frame (rendering thread). */
    public void frame() {
        long now = System.nanoTime();
        if (periodStart < 0) {
            periodStart = now;
            return;
        }
        frames++;
        long elapsed = now - periodStart;
        if (elapsed >= MEASURE_PERIOD_NS) {
            fps = frames * 1e9 / elapsed;
            frames = 0;
            periodStart = now;
        }
    }

    /** Last measured frame rate, in frames per second (0 before the first measurement). */
    public double getFps() { return fps; }
}
