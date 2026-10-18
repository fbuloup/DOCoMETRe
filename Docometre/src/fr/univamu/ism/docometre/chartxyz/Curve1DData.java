package fr.univamu.ism.docometre.chartxyz;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Signal temporel y(t) échantillonné à sampleFrequency Hz, dont seules les
 * windowTimeWidth dernières secondes sont conservées (tampon circulaire de
 * capacité sampleFrequency * windowTimeWidth).
 *
 * Le temps n'est pas transmis : il est déduit du rang de l'échantillon,
 * le premier échantillon reçu étant à t = 0 s, le suivant à 1 / sampleFrequency, etc.
 *
 * {@link #addSamples} ajoute des échantillons temporellement consécutifs ;
 * lorsque le tampon est plein, les plus anciens sont écrasés par les plus
 * récents. addSamples() peut être appelé depuis n'importe quel thread.
 *
 * Le renderer appelle {@link #takeSnapshot()} une fois au début de chaque
 * image : cette méthode fige une copie des valeurs et recalcule les bornes.
 * Les accesseurs (getMinY(), getLastSampleTime(), ...) renvoient les valeurs
 * de ce dernier instantané, qui restent donc cohérentes pendant toute l'image.
 */
public class Curve1DData implements CurveAppearance, IRTSWTSerie {

    private final double sampleFrequency;  // Hz
    private final double windowTimeWidth;  // s
    private final int capacity;            // nombre max d'échantillons conservés

    // Tampon circulaire des valeurs, protégé par "this"
    private final float[] ring;
    private int head = 0;            // indice de la prochaine écriture
    private int count = 0;           // nombre d'échantillons valides dans le tampon
    private long totalSamples = 0;   // nombre total d'échantillons reçus depuis le début
    private boolean modified = true;

    // Instantané utilisé par le rendu (dans l'ordre chronologique)
    private final float[] snapshot;
    private int snapshotCount = 0;
    private long snapshotTotalSamples = 0;
    private float minY, maxY, centerY;

    /**
     * Crée un signal temps réel, initialement vide.
     *
     * @param sampleFrequency fréquence d'échantillonnage, en Hz
     * @param windowTimeWidth durée de l'historique conservé et affiché, en secondes
     */
    public Curve1DData(double sampleFrequency, double windowTimeWidth) {
        if (!(sampleFrequency > 0) || !(windowTimeWidth > 0)) {
            throw new IllegalArgumentException("sampleFrequency et windowTimeWidth doivent être strictement positifs.");
        }
        this.sampleFrequency = sampleFrequency;
        this.windowTimeWidth = windowTimeWidth;
        this.capacity = (int) Math.max(1, Math.round(sampleFrequency * windowTimeWidth));
        this.ring = new float[capacity];
        this.snapshot = new float[capacity];
        takeSnapshot();
    }

    /**
     * Ajoute des échantillons temporellement consécutifs (le dernier du
     * tableau est le plus récent). Si la fenêtre est pleine, les échantillons
     * les plus anciens sont supprimés.
     */
    public synchronized void addSamples(float[] values) {
        int n = values.length;
        for (int i = Math.max(0, n - capacity); i < n; i++) { // inutile d'écrire ce qui serait aussitôt écrasé
            ring[head] = values[i];
            head = (head + 1) % capacity;
            if (count < capacity) count++;
        }
        totalSamples += n;
        modified = true;
    }

    /** Ajoute un seul échantillon. */
    public void addSample(float value) {
        addSamples(new float[]{value});
    }

    /** Supprime tous les échantillons et remet le temps à zéro. */
    public synchronized void clear() {
        head = 0;
        count = 0;
        totalSamples = 0;
        modified = true;
    }

    /**
     * Fige l'état courant du tampon pour le rendu : copie les valeurs dans
     * l'ordre chronologique et recalcule les bornes en y.
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
        System.arraycopy(ring, oldest, snapshot, 0, firstPart);
        System.arraycopy(ring, 0, snapshot, firstPart, count - firstPart);
        snapshotCount = count;
        snapshotTotalSamples = totalSamples;

        if (count == 0) {
            minY = -1f;
            maxY = 1f;
        } else {
            float minYv = Float.MAX_VALUE, maxYv = -Float.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                minYv = Math.min(minYv, snapshot[i]);
                maxYv = Math.max(maxYv, snapshot[i]);
            }
            minY = minYv;
            maxY = maxYv;
        }
        centerY = (minY + maxY) / 2f;
        return true;
    }

    // Apparence dans un graphe à plusieurs courbes (facultatif)
    private volatile String name;
    private volatile float[] color; // null = couleur automatique selon le rang de la courbe
    private volatile float lineWidth = 0f; // 0 = épaisseur par défaut du graphe
    private volatile LineStyle lineStyle = LineStyle.SOLID;

    /** Nom affiché dans la légende (null = pas de nom). Renvoie this pour chaîner les appels. */
    public Curve1DData setName(String name) { this.name = name; return this; }
    @Override
    public String getName() { return name; }

    /** Couleur RGB (composantes entre 0 et 1). Renvoie this pour chaîner les appels. */
    public Curve1DData setColor(float r, float g, float b) { this.color = new float[]{r, g, b}; return this; }
    /** Revient à la couleur automatique (palette, selon le rang de la courbe dans le graphe). */
    public Curve1DData setAutomaticColor() { this.color = null; return this; }
    @Override
    public float[] getColor() { return color; }

    /**
     * Épaisseur du trait en pixels (1, 2, 3...), ou 0 pour revenir à
     * l'épaisseur par défaut du graphe. Renvoie this pour chaîner les appels.
     */
    public Curve1DData setLineWidth(float lineWidth) {
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
    public Curve1DData setLineStyle(LineStyle lineStyle) {
        this.lineStyle = (lineStyle != null) ? lineStyle : LineStyle.SOLID;
        return this;
    }
    @Override
    public LineStyle getLineStyle() { return lineStyle; }

    /** Valeurs du dernier instantané, de la plus ancienne à la plus récente. */
    public float[] getSnapshotValues() { return snapshot; }
    /** Nombre de valeurs du dernier instantané. */
    public int getSnapshotCount() { return snapshotCount; }
    /**
     * Nombre total d'échantillons reçus au moment du dernier instantané : le
     * point i de l'instantané est l'échantillon numéro
     * getSnapshotTotalSamples() - getSnapshotCount() + i depuis le début.
     */
    public long getSnapshotTotalSamples() { return snapshotTotalSamples; }
    /**
     * Instant (en s) du dernier échantillon du dernier instantané, en prenant
     * t = 0 pour le premier échantillon reçu (négatif si aucun échantillon).
     * L'échantillon i de l'instantané est à getLastSampleTime() - (getSnapshotCount() - 1 - i) / sampleFrequency.
     */
    public double getLastSampleTime() { return (snapshotTotalSamples - 1) / sampleFrequency; }

    public double getSampleFrequency() { return sampleFrequency; }
    public double getWindowTimeWidth() { return windowTimeWidth; }
    /** Nombre maximal d'échantillons conservés (sampleFrequency * windowTimeWidth). */
    public int getCapacity() { return capacity; }
    /** Nombre total d'échantillons reçus depuis la création (ou le dernier clear()). */
    public synchronized long getTotalSamples() { return totalSamples; }

    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
    public float getCenterY() { return centerY; }

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDisplayCurrentValue(boolean displayCurrentValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setThickness(int thickness) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addPoints(Double[] xValues, Double[] yValues) {
		float[] yf = new float[yValues.length];
		for (int i = 0; i < yValues.length; i++) yf[i] = yValues[i].floatValue();   
		addSamples(yf);
	}
}
