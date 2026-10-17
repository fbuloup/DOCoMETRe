package fr.univamu.ism.docometre.chartxyz;

/**
 * Réduction du nombre de points envoyés à OpenGL, sans changer le dessin à
 * l'écran. Calcule la liste des indices (croissants) des points à tracer :
 *
 * <ul>
 * <li>{@link #m4} (graphes 1D) : réduction "M4" par colonne de pixels. Dans
 *     chaque colonne, on garde le premier échantillon, le minimum, le
 *     maximum et le dernier, dans leur ordre chronologique. Le tracé obtenu
 *     est identique au pixel près au tracé complet (le trait vertical de la
 *     colonne et les liaisons entre colonnes sont conservés), avec au plus
 *     4 points par colonne quelle que soit la fréquence d'échantillonnage.</li>
 * <li>{@link #screenDistance} (graphes 2D et 3D) : on ne garde un point que
 *     s'il est à au moins epsilon pixels du dernier point gardé (le premier
 *     et le dernier sont toujours gardés). Le tracé simplifié ne s'écarte
 *     jamais de plus d'epsilon pixels du tracé complet.</li>
 * </ul>
 *
 * Une instance réutilise son tableau d'indices d'une image à l'autre : elle
 * est propre à un renderer et doit être utilisée depuis son thread d'affichage.
 */
public final class Decimation {

    /** Colonne de pixels (ou toute clé de regroupement) de l'échantillon i. */
    public interface ColumnKey {
        long key(int i);
    }

    /** Position à l'écran (pixels) du point i, écrite dans out ; false si elle est inconnue (point derrière la caméra). */
    public interface ScreenPosition {
        boolean project(int i, double[] out);
    }

    private int[] indices = new int[256];
    private int count;

    /** Indices des points à tracer (les count() premiers éléments sont valides). */
    public int[] indices() { return indices; }
    /** Nombre de points à tracer. */
    public int count() { return count; }

    /**
     * Réduction M4 des échantillons [from, n) : les échantillons consécutifs
     * de même clé (même colonne de pixels) sont regroupés et réduits à leurs
     * premier, minimum, maximum et dernier éléments.
     *
     * @param values valeurs (ordonnées) des échantillons
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
     * Filtre par distance à l'écran des points [0, n) : un point est gardé
     * s'il est à au moins epsilon pixels du dernier point gardé.
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
            if (!known || !lastKnown) { // position inconnue : on garde le point par prudence
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

    /** Ajoute les indices a, b, c, d d'une colonne, triés et sans doublon. */
    private void appendColumn(int a, int b, int c, int d) {
        // b et c (minimum, maximum) sont entre a (premier) et d (dernier) : seul leur ordre est à fixer
        int lo = Math.min(b, c), hi = Math.max(b, c);
        append(a);
        append(lo);
        append(hi);
        append(d);
    }

    private void append(int index) {
        if (count > 0 && indices[count - 1] >= index) return; // doublon (ou déjà couvert)
        indices[count++] = index;
    }

    private void ensureCapacity(int size) {
        if (indices.length < size) indices = new int[Math.max(size, indices.length * 2)];
    }
}
