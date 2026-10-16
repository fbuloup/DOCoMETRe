package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

/**
 * Équivalent 2D de {@link Curve3DData} : ensemble ordonné de points (x, y)
 * stockés dans un tampon circulaire de capacité sampleFrequency * windowTimeWidth.
 *
 * {@link #addSamples} ajoute des échantillons temporellement consécutifs ;
 * lorsque le tampon est plein, les plus anciens sont écrasés par les plus
 * récents. addSamples() peut être appelé depuis n'importe quel thread.
 *
 * Le renderer appelle {@link #takeSnapshot()} une fois au début de chaque
 * image : cette méthode fige une copie des points et recalcule les bornes.
 * Les accesseurs (getMinX(), getCenterY(), ...) renvoient les valeurs de ce
 * dernier instantané, qui restent donc cohérentes pendant toute l'image.
 */
public class Curve2DData implements CurveAppearance {

    private final double sampleFrequency;  // Hz
    private final double windowTimeWidth;  // s
    private final int capacity;            // nombre max d'échantillons conservés

    // Tampon circulaire (x, y entrelacés), protégé par "this"
    private final float[] ring;
    private int head = 0;            // indice (en échantillons) de la prochaine écriture
    private int count = 0;           // nombre d'échantillons valides dans le tampon
    private long totalSamples = 0;   // nombre total d'échantillons reçus depuis le début
    private boolean modified = true;

    // Instantané utilisé par le rendu (dans l'ordre chronologique)
    private final float[] snapshot;
    private int snapshotCount = 0;
    private long snapshotTotalSamples = 0;
    private float centerX, centerY;
    private float minX, maxX, minY, maxY;

    /**
     * Crée une courbe 2D temps réel, initialement vide.
     *
     * @param sampleFrequency fréquence d'échantillonnage des coordonnées, en Hz
     * @param windowTimeWidth durée de l'historique conservé, en secondes
     */
    public Curve2DData(double sampleFrequency, double windowTimeWidth) {
        if (!(sampleFrequency > 0) || !(windowTimeWidth > 0)) {
            throw new IllegalArgumentException("sampleFrequency et windowTimeWidth doivent être strictement positifs.");
        }
        this.sampleFrequency = sampleFrequency;
        this.windowTimeWidth = windowTimeWidth;
        this.capacity = (int) Math.max(1, Math.round(sampleFrequency * windowTimeWidth));
        this.ring = new float[capacity * 2];
        this.snapshot = new float[capacity * 2];
        takeSnapshot();
    }

    /**
     * Crée une courbe 2D statique à partir d'une liste de points : la fenêtre
     * a exactement la taille de la liste (fréquence nominale de 1 Hz).
     * addSamples() reste utilisable et fait alors défiler la courbe.
     */
    public Curve2DData(List<Point2D> points) {
        this(1, checkNotEmpty(points).size());
        addSamples(points);
        takeSnapshot();
    }

    private static List<Point2D> checkNotEmpty(List<Point2D> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("La liste de points ne peut pas être vide.");
        }
        return points;
    }

    /**
     * Ajoute des échantillons temporellement consécutifs (le dernier de la
     * liste est le plus récent). Si la fenêtre est pleine, les échantillons
     * les plus anciens sont supprimés.
     */
    public synchronized void addSamples(List<Point2D> samples) {
        int n = samples.size();
        for (int i = Math.max(0, n - capacity); i < n; i++) { // inutile d'écrire ce qui serait aussitôt écrasé
            Point2D p = samples.get(i);
            put(p.x, p.y);
        }
        totalSamples += n;
        modified = true;
    }

    /**
     * Variante de {@link #addSamples(List)} prenant une voie par coordonnée :
     * (x[i], y[i]) forme le i-ème échantillon. Les deux tableaux doivent
     * avoir la même longueur.
     */
    public synchronized void addSamples(float[] x, float[] y) {
        int n = x.length;
        if (y.length != n) {
            throw new IllegalArgumentException("Les tableaux x et y doivent avoir la même longueur.");
        }
        for (int i = Math.max(0, n - capacity); i < n; i++) {
            put(x[i], y[i]);
        }
        totalSamples += n;
        modified = true;
    }

    private void put(float x, float y) {
        int k = head * 2;
        ring[k] = x;
        ring[k + 1] = y;
        head = (head + 1) % capacity;
        if (count < capacity) count++;
    }

    /** Supprime tous les échantillons. */
    public synchronized void clear() {
        head = 0;
        count = 0;
        totalSamples = 0;
        modified = true;
    }

    /**
     * Fige l'état courant du tampon pour le rendu : copie les points dans
     * l'ordre chronologique et recalcule boîte englobante et centre.
     * Appelé par le renderer au début de chaque image.
     *
     * @return true si les données ont changé depuis le précédent instantané
     */
    public synchronized boolean takeSnapshot() {
        if (!modified) return false;
        modified = false;

        // Le plus ancien échantillon est à "head" si le tampon est plein, à 0 sinon
        int oldest = (count == capacity) ? head : 0;
        int firstPart = Math.min(count, capacity - oldest);
        System.arraycopy(ring, oldest * 2, snapshot, 0, firstPart * 2);
        System.arraycopy(ring, 0, snapshot, firstPart * 2, (count - firstPart) * 2);
        snapshotCount = count;
        snapshotTotalSamples = totalSamples;

        if (count == 0) {
            minX = minY = -1f;
            maxX = maxY = 1f;
        } else {
            float minXv = Float.MAX_VALUE, minYv = Float.MAX_VALUE;
            float maxXv = -Float.MAX_VALUE, maxYv = -Float.MAX_VALUE;
            for (int k = 0; k < count * 2; k += 2) {
                minXv = Math.min(minXv, snapshot[k]);     maxXv = Math.max(maxXv, snapshot[k]);
                minYv = Math.min(minYv, snapshot[k + 1]); maxYv = Math.max(maxYv, snapshot[k + 1]);
            }
            minX = minXv; maxX = maxXv;
            minY = minYv; maxY = maxYv;
        }

        centerX = (minX + maxX) / 2f;
        centerY = (minY + maxY) / 2f;
        return true;
    }

    /** Renvoie une copie des points actuellement dans la fenêtre, du plus ancien au plus récent. */
    public synchronized List<Point2D> getPoints() {
        List<Point2D> points = new ArrayList<>(count);
        int oldest = (count == capacity) ? head : 0;
        for (int i = 0; i < count; i++) {
            int k = ((oldest + i) % capacity) * 2;
            points.add(new Point2D(ring[k], ring[k + 1]));
        }
        return points;
    }

    // Apparence dans un graphe à plusieurs courbes (facultatif)
    private volatile String name;
    private volatile float[] color; // null = couleur automatique selon le rang de la courbe
    private volatile float lineWidth = 0f; // 0 = épaisseur par défaut du graphe
    private volatile LineStyle lineStyle = LineStyle.SOLID;

    /** Nom affiché dans la légende (null = pas de nom). Renvoie this pour chaîner les appels. */
    public Curve2DData setName(String name) { this.name = name; return this; }
    @Override
    public String getName() { return name; }

    /** Couleur RGB (composantes entre 0 et 1). Renvoie this pour chaîner les appels. */
    public Curve2DData setColor(float r, float g, float b) { this.color = new float[]{r, g, b}; return this; }
    /** Revient à la couleur automatique (palette, selon le rang de la courbe dans le graphe). */
    public Curve2DData setAutomaticColor() { this.color = null; return this; }
    @Override
    public float[] getColor() { return color; }

    /**
     * Épaisseur du trait en pixels (1, 2, 3...), ou 0 pour revenir à
     * l'épaisseur par défaut du graphe. Renvoie this pour chaîner les appels.
     */
    public Curve2DData setLineWidth(float lineWidth) {
        if (!(lineWidth >= 0)) throw new IllegalArgumentException("L'épaisseur doit être positive ou nulle.");
        this.lineWidth = lineWidth;
        return this;
    }
    @Override
    public float getLineWidth() { return lineWidth; }

    /**
     * Type de tracé : SOLID (par défaut), DASH, DASH_DOT ou DASH_DASH_DOT.
     * Se combine avec l'épaisseur. Renvoie this pour chaîner les appels.
     */
    public Curve2DData setLineStyle(LineStyle lineStyle) {
        this.lineStyle = (lineStyle != null) ? lineStyle : LineStyle.SOLID;
        return this;
    }
    @Override
    public LineStyle getLineStyle() { return lineStyle; }

    /** Coordonnées (x, y entrelacés) du dernier instantané, du plus ancien au plus récent. */
    public float[] getSnapshotCoordinates() { return snapshot; }
    /** Nombre de points du dernier instantané. */
    public int getSnapshotCount() { return snapshotCount; }
    /**
     * Nombre total d'échantillons reçus au moment du dernier instantané : le
     * point i de l'instantané est l'échantillon numéro
     * getSnapshotTotalSamples() - getSnapshotCount() + i depuis le début.
     */
    public long getSnapshotTotalSamples() { return snapshotTotalSamples; }

    public double getSampleFrequency() { return sampleFrequency; }
    public double getWindowTimeWidth() { return windowTimeWidth; }
    /** Nombre maximal d'échantillons conservés (sampleFrequency * windowTimeWidth). */
    public int getCapacity() { return capacity; }
    /** Nombre total d'échantillons reçus depuis la création (ou le dernier clear()). */
    public synchronized long getTotalSamples() { return totalSamples; }
    /** Instant (en s) du dernier échantillon reçu, en prenant t = 0 pour le premier. */
    public synchronized double getLastSampleTime() { return (totalSamples - 1) / sampleFrequency; }

    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
}
