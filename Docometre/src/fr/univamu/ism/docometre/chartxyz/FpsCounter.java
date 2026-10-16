package fr.univamu.ism.docometre.chartxyz;

/**
 * Mesure de la cadence d'affichage réelle (images par seconde) : frame() est
 * appelé au début de chaque image dessinée, et la cadence est recalculée
 * toutes les MEASURE_PERIOD_NS à partir du nombre d'images et du temps
 * réellement écoulé.
 */
public final class FpsCounter {

    private static final long MEASURE_PERIOD_NS = 500_000_000L; // 0,5 s

    private long periodStart = -1;
    private int frames;
    private volatile double fps;

    /** À appeler au début de chaque image (thread d'affichage). */
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

    /** Dernière cadence mesurée, en images par seconde (0 avant la première mesure). */
    public double getFps() { return fps; }
}
