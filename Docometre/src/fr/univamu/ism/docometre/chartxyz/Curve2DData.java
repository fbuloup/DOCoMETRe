package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

/**
 * Équivalent 2D de {@link Curve3DData} : ensemble ordonné de points (x, y)
 * stockés dans un tampon circulaire de capacité sampleFrequency * windowTimeWidth
 * (voir {@link CurveData}).
 */
public class Curve2DData extends CurveData<Curve2DData> {

    /**
     * Crée une courbe 2D temps réel, initialement vide ; la durée de
     * l'historique sera celle du graphe auquel elle sera ajoutée
     * ({@link #DEFAULT_WINDOW_TIME_WIDTH} secondes d'ici là, ou si le graphe n'en impose pas).
     *
     * @param sampleFrequency fréquence d'échantillonnage des coordonnées, en Hz
     */
    public Curve2DData(double sampleFrequency) {
        super(2, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Crée une courbe 2D statique à partir d'une liste de points : la fenêtre
     * a exactement la taille de la liste (fréquence nominale de 1 Hz).
     * addSamples() reste utilisable et fait alors défiler la courbe.
     * À afficher dans un graphe qui n'impose pas de durée d'historique : sinon
     * la courbe serait réduite à ses historySize derniers points.
     */
    public Curve2DData(List<Point2D> points) {
        super(2, 1, checkNotEmpty(points).size());
        addSamples(points);
        takeSnapshot();
    }

    /**
     * Ajoute des échantillons temporellement consécutifs (le dernier de la
     * liste est le plus récent). Si la fenêtre est pleine, les échantillons
     * les plus anciens sont supprimés.
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
     * Variante de {@link #addSamples(List)} prenant une voie par coordonnée :
     * (x[i], y[i]) forme le i-ème échantillon. Les deux tableaux doivent
     * avoir la même longueur.
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

    /** Renvoie une copie des points actuellement dans la fenêtre, du plus ancien au plus récent. */
    public synchronized List<Point2D> getPoints() {
        List<Point2D> points = new ArrayList<>(count());
        float[] ring = ring();
        for (int i = 0; i < count(); i++) {
            int k = ringOffset(i);
            points.add(new Point2D(ring[k], ring[k + 1]));
        }
        return points;
    }

    /** Coordonnées (x, y entrelacés) du dernier instantané, du plus ancien au plus récent. */
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
