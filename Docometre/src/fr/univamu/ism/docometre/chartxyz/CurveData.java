package fr.univamu.ism.docometre.chartxyz;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Base commune des courbes temps réel ({@link Curve1DData}, {@link Curve2DData},
 * {@link Curve3DData}) : un échantillon est formé de {@link #getDimension()}
 * coordonnées (y ; x, y ; x, y, z), stockées entrelacées dans un tampon
 * circulaire de capacité sampleFrequency * windowTimeWidth + 1 échantillons :
 * windowTimeWidth est l'écart entre le premier et le dernier échantillon
 * conservés (N échantillons durent (N - 1) / sampleFrequency secondes).
 *
 * Lorsque le tampon est plein, les échantillons les plus anciens sont
 * écrasés par les plus récents : seules les windowTimeWidth dernières
 * secondes de signal sont conservées et affichées.
 *
 * Accès concurrents : les ajouts peuvent être faits depuis n'importe quel
 * thread (thread d'acquisition par exemple). Le renderer appelle
 * {@link #takeSnapshot()} une fois au début de chaque image : cette méthode
 * fige une copie des échantillons et recalcule les bornes. Les accesseurs
 * (getMin(), getSnapshotData(), ...) renvoient les valeurs de ce dernier
 * instantané, qui restent donc cohérentes pendant toute l'image.
 *
 * Une courbe ne peut être affichée que dans un seul graphe à la fois (voir
 * {@link #attach} : l'ajouter à un second graphe lève une exception). Elle
 * redevient disponible quand elle est retirée de son graphe ou quand celui-ci
 * est fermé.
 *
 * L'apparence (nom, couleur, épaisseur, type de trait) est commune à toutes
 * les courbes ; ses setters renvoient la courbe elle-même (type T) pour
 * chaîner les appels.
 *
 * @param <T> type concret de la courbe
 */
public abstract class CurveData<T extends CurveData<T>> implements CurveAppearance, IRTSWTSerie {

    /** Durée conservée (s) tant que la courbe n'est pas ajoutée à un graphe qui impose la sienne. */
    public static final double DEFAULT_WINDOW_TIME_WIDTH = 10;

    private final int dimension;           // coordonnées par échantillon
    private final double sampleFrequency;  // Hz
    private double windowTimeWidth;        // s, protégé par "this"
    private int capacity;                  // nombre max d'échantillons conservés, protégé par "this"

    // Tampon circulaire (coordonnées entrelacées), protégé par "this"
    private float[] ring;
    private int head = 0;            // indice (en échantillons) de la prochaine écriture
    private int count = 0;           // nombre d'échantillons valides dans le tampon
    private long totalSamples = 0;   // nombre total d'échantillons reçus depuis le début
    private boolean modified = true;

    // Instantané utilisé par le rendu (dans l'ordre chronologique)
    private float[] snapshot = new float[0];
    private int snapshotCount = 0;
    private int snapshotCapacity = 1;
    private long snapshotTotalSamples = 0;
    private final float[] min, max; // bornes de l'instantané, par coordonnée

    // Apparence dans un graphe à plusieurs courbes (facultatif)
    private volatile String name;
    private volatile float[] color; // null = couleur automatique selon le rang de la courbe
    private volatile float lineWidth = 0f; // 0 = épaisseur par défaut du graphe
    private volatile LineStyle lineStyle = LineStyle.SOLID;

    // Graphe (renderer) qui affiche la courbe, ou null
    private Object owner;

    /**
     * Crée une courbe temps réel, initialement vide.
     *
     * @param dimension       nombre de coordonnées par échantillon (1, 2 ou 3)
     * @param sampleFrequency fréquence d'échantillonnage, en Hz
     * @param windowTimeWidth durée de l'historique conservé et affiché, en secondes
     */
    protected CurveData(int dimension, double sampleFrequency, double windowTimeWidth) {
        if (!(sampleFrequency > 0)) {
            throw new IllegalArgumentException("sampleFrequency doit être strictement positive.");
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

    private static void checkWindowTimeWidth(double windowTimeWidth) {
        if (!(windowTimeWidth > 0) || Double.isInfinite(windowTimeWidth)) {
            throw new IllegalArgumentException("windowTimeWidth doit être strictement positive.");
        }
    }

    /** N échantillons couvrent (N - 1) / sampleFrequency secondes : il en faut sampleFrequency * durée + 1. */
    private int capacityFor(double windowTimeWidth) {
        return (int) Math.max(1, Math.round(sampleFrequency * windowTimeWidth) + 1);
    }

    /**
     * Change la durée de l'historique conservé (et donc la capacité du
     * tampon). Les échantillons les plus récents sont conservés, dans la
     * limite de la nouvelle capacité. Utilisable depuis n'importe quel thread.
     */
    protected synchronized void setWindowTimeWidth(double windowTimeWidth) {
        checkWindowTimeWidth(windowTimeWidth);
        this.windowTimeWidth = windowTimeWidth;
        int newCapacity = capacityFor(windowTimeWidth);
        if (newCapacity == capacity) return;
        int kept = Math.min(count, newCapacity);
        float[] newRing = new float[newCapacity * dimension];
        for (int i = 0; i < kept; i++) { // les "kept" plus récents, du plus ancien au plus récent
            System.arraycopy(ring, ringOffset(count - kept + i), newRing, i * dimension, dimension);
        }
        ring = newRing;
        capacity = newCapacity;
        count = kept;
        head = kept % newCapacity;
        modified = true;
    }

    // ------------------------------------------------------------------
    // Appartenance à un graphe

    /**
     * Rattache la courbe au graphe owner (appelé par le renderer quand on lui
     * ajoute la courbe).
     *
     * @throws IllegalStateException si la courbe est déjà affichée dans un graphe (celui-ci compris)
     */
    synchronized void attach(Object owner) {
        if (this.owner != null) {
            throw new IllegalStateException("La courbe \"" + name + "\" est déjà affichée dans un graphe : "
                    + "une courbe ne peut appartenir qu'à un seul graphe.");
        }
        this.owner = owner;
    }

    /** Détache la courbe de owner (sans effet si elle ne lui appartient pas). */
    synchronized void detach(Object owner) {
        if (this.owner == owner) this.owner = null;
    }

    /** Vrai si la courbe est actuellement affichée dans un graphe. */
    public synchronized boolean isAttached() {
        return owner != null;
    }

    /** Le type concret, pour les setters chaînables. */
    @SuppressWarnings("unchecked")
    protected final T self() { return (T) this; }

    // ------------------------------------------------------------------
    // Écriture (appelée par les sous-classes, sous le verrou "this")

    /**
     * Indice du premier échantillon à écrire parmi n reçus : il est inutile
     * d'écrire ceux qui seraient aussitôt écrasés.
     */
    protected final int firstUsefulSample(int n) { // sous le verrou
        return Math.max(0, n - capacity);
    }

    /**
     * Réserve la place du prochain échantillon dans le tampon et renvoie
     * l'indice de sa première coordonnée dans {@link #ring()}.
     */
    protected final int nextSlot() {
        int k = head * dimension;
        head = (head + 1) % capacity;
        if (count < capacity) count++;
        return k;
    }

    /** Tampon circulaire, pour l'écriture des coordonnées réservées par {@link #nextSlot()}. */
    protected final float[] ring() { return ring; }

    /** Termine un ajout de n échantillons (qu'ils aient tous été écrits ou non). */
    protected final void commit(int n) {
        totalSamples += n;
        modified = true;
    }

    /** Supprime tous les échantillons et remet le temps à zéro. */
    public synchronized void clear() {
        head = 0;
        count = 0;
        totalSamples = 0;
        modified = true;
    }

    // ------------------------------------------------------------------
    // Instantané

    /**
     * Fige l'état courant du tampon pour le rendu : copie les échantillons
     * dans l'ordre chronologique et recalcule les bornes.
     * Appelé par le renderer au début de chaque image.
     *
     * @return true si les données ont changé depuis le précédent instantané
     */
    public synchronized boolean takeSnapshot() {
        if (!modified) return false;
        modified = false;

        // Le plus ancien échantillon est à "head" si le tampon est plein, à 0 sinon
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

    /** Indice dans le tampon du plus ancien échantillon (à appeler sous le verrou). */
    protected final int oldestIndex() {
        return (count == capacity) ? head : 0;
    }

    /** Nombre d'échantillons actuellement dans le tampon (à appeler sous le verrou). */
    protected final int count() { return count; }

    /** Indice dans le tampon de la première coordonnée du i-ème échantillon le plus ancien (à appeler sous le verrou). */
    protected final int ringOffset(int i) {
        return ((oldestIndex() + i) % capacity) * dimension;
    }

    /** Coordonnées entrelacées du dernier instantané, du plus ancien au plus récent. */
    public float[] getSnapshotData() { return snapshot; }
    /** Nombre d'échantillons du dernier instantané. */
    public int getSnapshotCount() { return snapshotCount; }
    /** Capacité du tampon au moment du dernier instantané. */
    public int getSnapshotCapacity() { return snapshotCapacity; }
    /**
     * Nombre total d'échantillons reçus au moment du dernier instantané : le
     * point i de l'instantané est l'échantillon numéro
     * getSnapshotTotalSamples() - getSnapshotCount() + i depuis le début.
     */
    public long getSnapshotTotalSamples() { return snapshotTotalSamples; }
    /**
     * Instant (en s) du dernier échantillon du dernier instantané, en prenant
     * t = 0 pour le premier échantillon reçu (négatif si aucun échantillon).
     */
    public double getSnapshotLastSampleTime() { return (snapshotTotalSamples - 1) / sampleFrequency; }

    /** Minimum de la coordonnée axis (0 = première) dans le dernier instantané (-1 s'il est vide). */
    public float getMin(int axis) { return min[axis]; }
    /** Maximum de la coordonnée axis dans le dernier instantané (1 s'il est vide). */
    public float getMax(int axis) { return max[axis]; }
    /** Milieu de l'intervalle [min, max] de la coordonnée axis dans le dernier instantané. */
    public float getCenter(int axis) { return (min[axis] + max[axis]) / 2f; }

    /**
     * Dernier échantillon du dernier instantané, formaté pour l'affichage des
     * valeurs courantes : "v" en 1D, "(x, y)" ou "(x, y, z)" sinon ; null si
     * l'instantané est vide.
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
    // Caractéristiques

    /** Nombre de coordonnées par échantillon. */
    public int getDimension() { return dimension; }
    public double getSampleFrequency() { return sampleFrequency; }
    /** Durée de l'historique conservé, en secondes. */
    public synchronized double getWindowTimeWidth() { return windowTimeWidth; }
    /** Nombre maximal d'échantillons conservés (sampleFrequency * windowTimeWidth + 1). */
    public synchronized int getCapacity() { return capacity; }
    /** Nombre total d'échantillons reçus depuis la création (ou le dernier clear()). */
    public synchronized long getTotalSamples() { return totalSamples; }
    /** Instant (en s) du dernier échantillon reçu, en prenant t = 0 pour le premier. */
    public synchronized double getLastSampleTime() { return (totalSamples - 1) / sampleFrequency; }

    // ------------------------------------------------------------------
    // Apparence

    /** Nom affiché dans la légende (null = pas de nom). Renvoie this pour chaîner les appels. */
    public T setName(String name) { this.name = name; return self(); }
    @Override
    public String getName() { return name; }

    /** Couleur RGB (composantes entre 0 et 1). Renvoie this pour chaîner les appels. */
    public T setColor(float r, float g, float b) { this.color = new float[]{r, g, b}; return self(); }
    /** Revient à la couleur automatique (palette, selon le rang de la courbe dans le graphe). */
    public T setAutomaticColor() { this.color = null; return self(); }
    @Override
    public float[] getColor() { return color; }

    /**
     * Épaisseur du trait en pixels (1, 2, 3...), ou 0 pour revenir à
     * l'épaisseur par défaut du graphe. Renvoie this pour chaîner les appels.
     */
    public T setLineWidth(float lineWidth) {
        if (!(lineWidth >= 0)) throw new IllegalArgumentException("L'épaisseur doit être positive ou nulle.");
        this.lineWidth = lineWidth;
        return self();
    }
    @Override
    public float getLineWidth() { return lineWidth; }

    /**
     * Type de tracé : SOLID (par défaut), DASH, DASH_DOT ou DASH_DASH_DOT.
     * Se combine avec l'épaisseur. Renvoie this pour chaîner les appels.
     */
    public T setLineStyle(LineStyle lineStyle) {
        this.lineStyle = (lineStyle != null) ? lineStyle : LineStyle.SOLID;
        return self();
    }
    @Override
    public LineStyle getLineStyle() { return lineStyle; }

    // ------------------------------------------------------------------
    // IRTSWTSerie

    /** Identifiant de la série : son nom. */
    @Override
    public String getId() {
        return getName();
    }

    /** Sans effet : l'affichage des valeurs courantes se règle pour tout le graphe (touche V). */
    @Override
    public void setDisplayCurrentValue(boolean displayCurrentValues) {
    }

    @Override
    public void setThickness(int thickness) {
        setLineWidth(thickness);
    }

    /** Conversion des tableaux reçus par l'interface IRTSWTSerie. */
    protected static float[] toFloats(Double[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) result[i] = values[i].floatValue();
        return result;
    }

    /** Vérifie que des listes de coordonnées décrivent le même nombre d'échantillons. */
    protected static void checkSameLength(int n, int... others) {
        for (int other : others) {
            if (other != n) throw new IllegalArgumentException("Les tableaux de coordonnées doivent avoir la même longueur.");
        }
    }

    /** Vérifie qu'une liste de points n'est pas vide. */
    protected static <L extends java.util.List<?>> L checkNotEmpty(L points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("La liste de points ne peut pas être vide.");
        }
        return points;
    }
}
