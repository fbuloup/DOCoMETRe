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
 * circulaire dont la capacité vaut sampleFrequency * windowTimeWidth
 * échantillons. {@link #addSamples} ajoute des échantillons temporellement
 * consécutifs ; lorsque le tampon est plein, les plus anciens sont écrasés
 * par les plus récents. Seuls les windowTimeWidth dernières secondes de
 * signal sont donc conservées et affichées.
 *
 * Accès concurrents : addSamples() peut être appelé depuis n'importe quel
 * thread (thread d'acquisition par exemple). Le renderer appelle
 * {@link #takeSnapshot()} une fois au début de chaque image : cette méthode
 * fige une copie des points et recalcule les bornes. Les accesseurs
 * (getMinX(), getBoundingRadius(), ...) renvoient les valeurs de ce dernier
 * instantané, qui restent donc cohérentes pendant toute l'image.
 */
public class Curve3DData implements CurveAppearance, IRTSWTXYZSerie {

    private final double sampleFrequency;  // Hz
    private final double windowTimeWidth;  // s
    private final int capacity;            // nombre max d'échantillons conservés

    // Tampon circulaire (x, y, z entrelacés), protégé par "this"
    private final float[] ring;
    private int head = 0;            // indice (en échantillons) de la prochaine écriture
    private int count = 0;           // nombre d'échantillons valides dans le tampon
    private long totalSamples = 0;   // nombre total d'échantillons reçus depuis le début
    private boolean modified = true;

    // Instantané utilisé par le rendu (dans l'ordre chronologique)
    private final float[] snapshot;
    private int snapshotCount = 0;
    private long snapshotTotalSamples = 0;
    private float centerX, centerY, centerZ;
    private float minX, maxX, minY, maxY, minZ, maxZ;
    private float boundingRadius = 1f;

    /**
     * Crée une courbe temps réel, initialement vide.
     *
     * @param sampleFrequency fréquence d'échantillonnage des coordonnées, en Hz
     * @param windowTimeWidth durée de l'historique conservé, en secondes
     */
    public Curve3DData(double sampleFrequency, double windowTimeWidth) {
        if (!(sampleFrequency > 0) || !(windowTimeWidth > 0)) {
            throw new IllegalArgumentException("sampleFrequency et windowTimeWidth doivent être strictement positifs.");
        }
        this.sampleFrequency = sampleFrequency;
        this.windowTimeWidth = windowTimeWidth;
        this.capacity = (int) Math.max(1, Math.round(sampleFrequency * windowTimeWidth));
        this.ring = new float[capacity * 3];
        this.snapshot = new float[capacity * 3];
        takeSnapshot();
    }

    /**
     * Crée une courbe statique à partir d'une liste de points (comportement
     * d'origine) : la fenêtre a exactement la taille de la liste (fréquence
     * nominale de 1 Hz). addSamples() reste utilisable et fait alors défiler
     * la courbe.
     */
    public Curve3DData(List<Point3D> points) {
        this(1, checkNotEmpty(points).size());
        addSamples(points);
        takeSnapshot();
    }

    private static List<Point3D> checkNotEmpty(List<Point3D> points) {
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
    public synchronized void addSamples(List<Point3D> samples) {
        int n = samples.size();
        int start = Math.max(0, n - capacity); // inutile d'écrire ce qui serait aussitôt écrasé
        for (int i = start; i < n; i++) {
            Point3D p = samples.get(i);
            put(p.x, p.y, p.z);
        }
        totalSamples += n;
        modified = true;
    }

    /**
     * Variante de {@link #addSamples(List)} prenant une voie par coordonnée
     * (x[i], y[i], z[i]) forme le i-ème échantillon. Les trois tableaux
     * doivent avoir la même longueur.
     */
    public synchronized void addSamples(float[] x, float[] y, float[] z) {
        int n = x.length;
        if (y.length != n || z.length != n) {
            throw new IllegalArgumentException("Les tableaux x, y et z doivent avoir la même longueur.");
        }
        for (int i = Math.max(0, n - capacity); i < n; i++) {
            put(x[i], y[i], z[i]);
        }
        totalSamples += n;
        modified = true;
    }

    private void put(float x, float y, float z) {
        int k = head * 3;
        ring[k] = x;
        ring[k + 1] = y;
        ring[k + 2] = z;
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
     * l'ordre chronologique et recalcule boîte englobante, centre et rayon.
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
        System.arraycopy(ring, oldest * 3, snapshot, 0, firstPart * 3);
        System.arraycopy(ring, 0, snapshot, firstPart * 3, (count - firstPart) * 3);
        snapshotCount = count;
        snapshotTotalSamples = totalSamples;

        if (count == 0) {
            minX = minY = minZ = -1f;
            maxX = maxY = maxZ = 1f;
        } else {
            float minXv = Float.MAX_VALUE, minYv = Float.MAX_VALUE, minZv = Float.MAX_VALUE;
            float maxXv = -Float.MAX_VALUE, maxYv = -Float.MAX_VALUE, maxZv = -Float.MAX_VALUE;
            for (int k = 0; k < count * 3; k += 3) {
                minXv = Math.min(minXv, snapshot[k]);     maxXv = Math.max(maxXv, snapshot[k]);
                minYv = Math.min(minYv, snapshot[k + 1]); maxYv = Math.max(maxYv, snapshot[k + 1]);
                minZv = Math.min(minZv, snapshot[k + 2]); maxZv = Math.max(maxZv, snapshot[k + 2]);
            }
            minX = minXv; maxX = maxXv;
            minY = minYv; maxY = maxYv;
            minZ = minZv; maxZ = maxZv;
        }

        centerX = (minX + maxX) / 2f;
        centerY = (minY + maxY) / 2f;
        centerZ = (minZ + maxZ) / 2f;

        float dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        float diag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boundingRadius = Math.max(diag / 2f, 0.001f);
        return true;
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
        List<Point3D> points = new ArrayList<>(count);
        int oldest = (count == capacity) ? head : 0;
        for (int i = 0; i < count; i++) {
            int k = ((oldest + i) % capacity) * 3;
            points.add(new Point3D(ring[k], ring[k + 1], ring[k + 2]));
        }
        return points;
    }

    // Apparence dans un graphe à plusieurs courbes (facultatif)
    private volatile String name;
    private volatile float[] color; // null = couleur automatique selon le rang de la courbe
    private volatile float lineWidth = 0f; // 0 = épaisseur par défaut du graphe
    private volatile LineStyle lineStyle = LineStyle.SOLID;

    /** Nom affiché dans la légende (null = pas de nom). Renvoie this pour chaîner les appels. */
    public Curve3DData setName(String name) { this.name = name; return this; }
    @Override
    public String getName() { return name; }

    /** Couleur RGB (composantes entre 0 et 1). Renvoie this pour chaîner les appels. */
    public Curve3DData setColor(float r, float g, float b) { this.color = new float[]{r, g, b}; return this; }
    /** Revient à la couleur automatique (palette, selon le rang de la courbe dans le graphe). */
    public Curve3DData setAutomaticColor() { this.color = null; return this; }
    @Override
    public float[] getColor() { return color; }

    /**
     * Épaisseur du trait en pixels (1, 2, 3...), ou 0 pour revenir à
     * l'épaisseur par défaut du graphe. Renvoie this pour chaîner les appels.
     */
    public Curve3DData setLineWidth(float lineWidth) {
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
    public Curve3DData setLineStyle(LineStyle lineStyle) {
        this.lineStyle = (lineStyle != null) ? lineStyle : LineStyle.SOLID;
        return this;
    }
    @Override
    public LineStyle getLineStyle() { return lineStyle; }

    /** Coordonnées (x, y, z entrelacés) du dernier instantané, du plus ancien au plus récent. */
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
    public float getCenterZ() { return centerZ; }
    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
    public float getMinZ() { return minZ; }
    public float getMaxZ() { return maxZ; }
    public float getBoundingRadius() { return boundingRadius; }

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setDisplayCurrentValue(boolean displayCurrentValues) {
	}

	@Override
	public void setThickness(int thickness) {
		setLineWidth(thickness);
	}

	@Override
	public void addPoints(Double[] timeValues, Double[] doubleValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addPoints(Double[] xValues, Double[] yValues, Double[] zValues) {
		float[] xf = new float[xValues.length];
		float[] yf = new float[yValues.length];
		float[] zf = new float[zValues.length];
		for (int i = 0; i < xValues.length; i++) xf[i] = xValues[i].floatValue();   
		for (int i = 0; i < yValues.length; i++) yf[i] = yValues[i].floatValue();   
		for (int i = 0; i < zValues.length; i++) zf[i] = zValues[i].floatValue();   
		addSamples(xf, yf, zf);
	}
}
