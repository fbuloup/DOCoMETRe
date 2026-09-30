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
 */
public class CurveData {

    private final List<Point3D> points;
    private final float centerX, centerY, centerZ;
    private final float minX, maxX, minY, maxY, minZ, maxZ;
    private final float boundingRadius;

    public CurveData(List<Point3D> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("La liste de points ne peut pas être vide.");
        }
        this.points = points;

        float minXv = Float.MAX_VALUE, minYv = Float.MAX_VALUE, minZv = Float.MAX_VALUE;
        float maxXv = -Float.MAX_VALUE, maxYv = -Float.MAX_VALUE, maxZv = -Float.MAX_VALUE;

        for (Point3D p : points) {
            minXv = Math.min(minXv, p.x); maxXv = Math.max(maxXv, p.x);
            minYv = Math.min(minYv, p.y); maxYv = Math.max(maxYv, p.y);
            minZv = Math.min(minZv, p.z); maxZv = Math.max(maxZv, p.z);
        }

        this.minX = minXv; this.maxX = maxXv;
        this.minY = minYv; this.maxY = maxYv;
        this.minZ = minZv; this.maxZ = maxZv;

        this.centerX = (minXv + maxXv) / 2f;
        this.centerY = (minYv + maxYv) / 2f;
        this.centerZ = (minZv + maxZv) / 2f;

        float dx = maxXv - minXv, dy = maxYv - minYv, dz = maxZv - minZv;
        float diag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.boundingRadius = Math.max(diag / 2f, 0.001f);
    }

    /**
     * Charge une courbe depuis un fichier texte.
     * Format attendu : une ligne par point "x y z" (séparateurs : espace, virgule ou point-virgule).
     * Les lignes vides ou commençant par '#' sont ignorées.
     */
    public static CurveData loadFromFile(Path path) throws IOException {
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
        return new CurveData(points);
    }
    
    /**
     * Charge une courbe depuis une équation
     */
    public static CurveData loadFromEquation() {
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
        return new CurveData(points);
    }

    public List<Point3D> getPoints() { return points; }
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
}
