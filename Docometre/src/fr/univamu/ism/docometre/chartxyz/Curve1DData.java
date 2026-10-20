package fr.univamu.ism.docometre.chartxyz;

/**
 * Signal temporel y(t) échantillonné à sampleFrequency Hz, dont seules les
 * windowTimeWidth dernières secondes sont conservées (tampon circulaire de
 * capacité sampleFrequency * windowTimeWidth + 1, voir {@link CurveData}).
 *
 * La durée windowTimeWidth est celle du graphe 1D qui affiche la courbe :
 * elle lui est imposée quand on l'ajoute au graphe (voir
 * {@link Chart1DRenderer#setWindowTimeWidth}). Avant cela, la courbe
 * conserve {@link #DEFAULT_WINDOW_TIME_WIDTH} secondes.
 *
 * Le temps n'est pas transmis : il est déduit du rang de l'échantillon,
 * le premier échantillon reçu étant à t = 0 s, le suivant à 1 / sampleFrequency, etc.
 */
public class Curve1DData extends CurveData<Curve1DData> {

    /**
     * Crée un signal temps réel, initialement vide ; la durée conservée sera
     * celle du graphe auquel il sera ajouté.
     *
     * @param sampleFrequency fréquence d'échantillonnage, en Hz
     */
    public Curve1DData(double sampleFrequency) {
        super(1, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Crée un signal statique à partir de ses valeurs (la première à t = 0,
     * la dernière à (values.length - 1) / sampleFrequency) : la durée conservée
     * est exactement cet écart, si bien qu'aucune valeur n'est perdue. addSamples() reste utilisable et fait alors défiler le signal.
     * À afficher dans un graphe de même durée (getWindowTimeWidth()) : une
     * durée plus courte ne garderait que les dernières valeurs.
     *
     * @param sampleFrequency fréquence d'échantillonnage, en Hz
     * @param values          valeurs du signal (au moins deux)
     * @throws IllegalArgumentException si values a moins de deux valeurs ou si sampleFrequency n'est pas strictement positive
     */
    public Curve1DData(double sampleFrequency, float[] values) {
        super(1, sampleFrequency, durationOf(sampleFrequency, values));
        addSamples(values);
        takeSnapshot();
    }

    /**
     * Durée (s) de values.length échantillons à sampleFrequency Hz, du premier
     * au dernier : (values.length - 1) / sampleFrequency. Méthode
     * statique : elle peut être appelée dans l'appel au constructeur parent,
     * avant que l'objet n'existe.
     */
    private static double durationOf(double sampleFrequency, float[] values) {
        if (!(sampleFrequency > 0)) throw new IllegalArgumentException("sampleFrequency doit être strictement positive.");
        if (values == null || values.length < 2) throw new IllegalArgumentException("Le signal doit comporter au moins deux valeurs.");
        return (values.length - 1) / sampleFrequency;
    }

    /**
     * Ajoute des échantillons temporellement consécutifs (le dernier du
     * tableau est le plus récent). Si la fenêtre est pleine, les échantillons
     * les plus anciens sont supprimés.
     */
    public synchronized void addSamples(float[] values) {
        float[] ring = ring();
        for (int i = firstUsefulSample(values.length); i < values.length; i++) {
            ring[nextSlot()] = values[i];
        }
        commit(values.length);
    }

    /** Ajoute un seul échantillon. */
    public void addSample(float value) {
        addSamples(new float[]{value});
    }

    /** Valeurs du dernier instantané, de la plus ancienne à la plus récente. */
    public float[] getSnapshotValues() { return getSnapshotData(); }

    /**
     * Instant (en s) du dernier échantillon du dernier instantané, en prenant
     * t = 0 pour le premier échantillon reçu (négatif si aucun échantillon).
     * L'échantillon i de l'instantané est à getLastSampleTime() - (getSnapshotCount() - 1 - i) / sampleFrequency.
     */
    @Override
    public double getLastSampleTime() { return getSnapshotLastSampleTime(); }

    public float getMinY() { return getMin(0); }
    public float getMaxY() { return getMax(0); }
    public float getCenterY() { return getCenter(0); }

    /** Les temps (xValues) sont ignorés : ils sont déduits de la fréquence d'échantillonnage. */
    @Override
    public void addPoints(Double[] xValues, Double[] yValues) {
        addSamples(toFloats(yValues));
    }
}
