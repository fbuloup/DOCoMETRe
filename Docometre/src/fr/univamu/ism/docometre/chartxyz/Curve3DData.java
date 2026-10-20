package fr.univamu.ism.docometre.chartxyz;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'ensemble ordonné de points 3D constituant la courbe à afficher,
 * ainsi que les informations géométriques utiles au rendu (centre, rayon englobant).
 *
 * Fonctionnement "temps réel" : les points sont stockés dans un tampon
 * circulaire dont la capacité vaut sampleFrequency * windowTimeWidth + 1
 * échantillons (voir {@link CurveData}). {@link #addSamples} ajoute des
 * échantillons temporellement consécutifs ; lorsque le tampon est plein,
 * les plus anciens sont écrasés par les plus récents.
 */
public class Curve3DData extends CurveData<Curve3DData> implements IRTSWTXYZSerie {

    /**
     * Crée une courbe temps réel, initialement vide ; la durée de
     * l'historique sera celle du graphe auquel elle sera ajoutée
     * ({@link #DEFAULT_WINDOW_TIME_WIDTH} secondes d'ici là, ou si le graphe n'en impose pas).
     *
     * @param sampleFrequency fréquence d'échantillonnage des coordonnées, en Hz
     */
    public Curve3DData(double sampleFrequency) {
        super(3, sampleFrequency, DEFAULT_WINDOW_TIME_WIDTH);
    }

    /**
     * Crée une courbe statique à partir d'une liste de points (comportement
     * d'origine) : la fenêtre a exactement la taille de la liste (fréquence
     * nominale de 1 Hz). addSamples() reste utilisable et fait alors défiler
     * la courbe. À afficher dans un graphe qui n'impose pas de durée
     * d'historique : sinon la courbe serait réduite à ses historySize derniers points.
     */
    public Curve3DData(List<Point3D> points) {
        super(3, 1, Math.max(1, checkNotEmpty(points).size() - 1)); // N points couvrent N - 1 secondes à 1 Hz
        addSamples(points);
        takeSnapshot();
    }

    /**
     * Ajoute des échantillons temporellement consécutifs (le dernier de la
     * liste est le plus récent). Si la fenêtre est pleine, les échantillons
     * les plus anciens sont supprimés.
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
     * Variante de {@link #addSamples(List)} prenant une voie par coordonnée
     * (x[i], y[i], z[i]) forme le i-ème échantillon. Les trois tableaux
     * doivent avoir la même longueur.
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
     * Charge une courbe depuis un fichier texte.
     * Format attendu : une ligne par point "x y z" (séparateurs : espace, virgule ou point-virgule).
     * Les lignes vides ou commençant par '#' sont ignorées.
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
                    System.err.println("Ligne " + lineNumber + " ignorée (format invalide) : " + line);
                    continue;
                }
                try {
                    float x = Float.parseFloat(parts[0]);
                    float y = Float.parseFloat(parts[1]);
                    float z = Float.parseFloat(parts[2]);
                    points.add(new Point3D(x, y, z));
                } catch (NumberFormatException e) {
                    System.err.println("Ligne " + lineNumber + " ignorée (nombre invalide) : " + line);
                }
            }
        }
        return new Curve3DData(points);
    }

    /**
     * Charge une courbe depuis une équation
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

    /** Renvoie une copie des points actuellement dans la fenêtre, du plus ancien au plus récent. */
    public synchronized List<Point3D> getPoints() {
        List<Point3D> points = new ArrayList<>(count());
        float[] ring = ring();
        for (int i = 0; i < count(); i++) {
            int k = ringOffset(i);
            points.add(new Point3D(ring[k], ring[k + 1], ring[k + 2]));
        }
        return points;
    }

    /** Coordonnées (x, y, z entrelacés) du dernier instantané, du plus ancien au plus récent. */
    public float[] getSnapshotCoordinates() { return getSnapshotData(); }

    /** Boîte englobante du dernier instantané (de -1 à 1 s'il est vide). */
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
    /** Demi-diagonale de la boîte englobante du dernier instantané. */
    public float getBoundingRadius() { return getBoundingBox().radius; }

    /** Sans effet : une courbe 3D a besoin des trois coordonnées (voir {@link #addPoints(Double[], Double[], Double[])}). */
    @Override
    public void addPoints(Double[] timeValues, Double[] doubleValues) {
    }

    @Override
    public void addPoints(Double[] xValues, Double[] yValues, Double[] zValues) {
        addSamples(toFloats(xValues), toFloats(yValues), toFloats(zValues));
    }
}
