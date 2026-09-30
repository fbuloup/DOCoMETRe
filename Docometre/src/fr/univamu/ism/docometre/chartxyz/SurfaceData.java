package fr.univamu.ism.docometre.chartxyz;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une surface 3D définie par une grille régulière de points
 * (rows x cols), ainsi que les normales par sommet (pour un ombrage lisse)
 * et les informations géométriques utiles au rendu.
 *
 * La triangulation de chaque quadrilatère (r,c)-(r,c+1)-(r+1,c)-(r+1,c+1)
 * se fait selon la diagonale (r,c)-(r+1,c+1), en deux triangles :
 *   T1 = (r,c), (r+1,c), (r+1,c+1)
 *   T2 = (r,c), (r+1,c+1), (r,c+1)
 */
public class SurfaceData {

    private final Point3D[][] grid;     // grid[row][col]
    private final float[][][] normals;  // normals[row][col] = {nx, ny, nz}
    private final int rows, cols;

    private final float centerX, centerY, centerZ;
    private final float minX, maxX, minY, maxY, minZ, maxZ;
    private final float boundingRadius;

    public SurfaceData(Point3D[][] grid) {
        if (grid == null || grid.length < 2 || grid[0].length < 2) {
            throw new IllegalArgumentException("La grille doit comporter au moins 2 lignes et 2 colonnes.");
        }
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;

        float minXv = Float.MAX_VALUE, minYv = Float.MAX_VALUE, minZv = Float.MAX_VALUE;
        float maxXv = -Float.MAX_VALUE, maxYv = -Float.MAX_VALUE, maxZv = -Float.MAX_VALUE;
        for (Point3D[] row : grid) {
            for (Point3D p : row) {
                minXv = Math.min(minXv, p.x); maxXv = Math.max(maxXv, p.x);
                minYv = Math.min(minYv, p.y); maxYv = Math.max(maxYv, p.y);
                minZv = Math.min(minZv, p.z); maxZv = Math.max(maxZv, p.z);
            }
        }
        this.centerX = (minXv + maxXv) / 2f;
        this.centerY = (minYv + maxYv) / 2f;
        this.centerZ = (minZv + maxZv) / 2f;
        this.minX = minXv; this.maxX = maxXv;
        this.minY = minYv; this.maxY = maxYv;
        this.minZ = minZv; this.maxZ = maxZv;

        float dx = maxXv - minXv, dy = maxYv - minYv, dz = maxZv - minZv;
        float diag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.boundingRadius = Math.max(diag / 2f, 0.001f);

        this.normals = computeSmoothNormals();
    }
    
    public static SurfaceData loadFromEquation() {
//    	int uSteps = 100;
//    	int vSteps = 100;
//    	int uMax = 5;
//    	float a = 2;
//    	float b = 2;
//    	Point3D[][] grid = new Point3D[uSteps][vSteps];
//        for (int i = 0; i < uSteps; i++) {
//            float u = uMax * i / (uSteps - 1);
//            for (int j = 0; j < vSteps; j++) {
//                float v = (float) (2 * Math.PI * j / (vSteps - 1));
//                float x = a * u * (float) Math.cos(v);
//                float z = b * u * (float) Math.sin(v);
//                float y = u * u;
//                grid[i][j] = new Point3D(x, y, z);
//            }
//        }
    	int xSteps = 100;
    	int ySteps = 100;
    	Point3D[][] grid = new Point3D[xSteps][ySteps];
        double dx = 4*Math.PI/xSteps;
        double dy = 4*Math.PI/ySteps;
        for (int i = 0; i < xSteps; i++) {
            double x = -2*Math.PI + dx*i;
            for (int j = 0; j < ySteps; j++) {
            	double z = (-2*Math.PI + dy*j);
            	double y = (Math.cos(2*x)*Math.sin(2*z));
                grid[i][j] = new Point3D((float)x, (float)y, (float)z);
            }
        }
        
        return new SurfaceData(grid);
    }

    /**
     * Charge une grille depuis un fichier texte.
     * Format attendu :
     *   ligne 1            : "rows cols"
     *   lignes suivantes   : rows*cols points "x y z", en ordre ligne par ligne
     *                        (toute la ligne 0 d'abord, puis la ligne 1, etc.)
     * Les lignes vides ou commençant par '#' sont ignorées.
     */
    public static SurfaceData loadFromFile(Path path) throws IOException {
        List<String> dataLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                dataLines.add(trimmed);
            }
        }
        if (dataLines.isEmpty()) {
            throw new IOException("Fichier vide ou sans données valides : " + path);
        }

        String[] dims = dataLines.get(0).split("[\\s,;]+");
        if (dims.length < 2) {
            throw new IOException("La première ligne doit contenir 'rows cols' : " + dataLines.get(0));
        }
        int rows = Integer.parseInt(dims[0]);
        int cols = Integer.parseInt(dims[1]);

        int expected = rows * cols;
        if (dataLines.size() - 1 < expected) {
            throw new IOException("Nombre de points insuffisant. Attendu " + expected
                    + ", trouvé " + (dataLines.size() - 1));
        }

        Point3D[][] grid = new Point3D[rows][cols];
        int idx = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String[] parts = dataLines.get(idx++).split("[\\s,;]+");
                float x = Float.parseFloat(parts[0]);
                float y = Float.parseFloat(parts[1]);
                float z = Float.parseFloat(parts[2]);
                grid[r][c] = new Point3D(x, y, z);
            }
        }
        return new SurfaceData(grid);
    }

    /**
     * Calcule une normale lissée par sommet en moyennant les normales de
     * toutes les faces (triangles) qui touchent ce sommet.
     */
    private float[][][] computeSmoothNormals() {
        float[][] accum = new float[rows * cols][3]; // accumulateur (nx, ny, nz) par sommet
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                Point3D p00 = grid[r][c];
                Point3D p10 = grid[r + 1][c];
                Point3D p11 = grid[r + 1][c + 1];
                Point3D p01 = grid[r][c + 1];

                float[] n1 = triangleNormal(p00, p10, p11);
                accumulate(accum, index(r, c), n1);
                accumulate(accum, index(r + 1, c), n1);
                accumulate(accum, index(r + 1, c + 1), n1);

                float[] n2 = triangleNormal(p00, p11, p01);
                accumulate(accum, index(r, c), n2);
                accumulate(accum, index(r + 1, c + 1), n2);
                accumulate(accum, index(r, c + 1), n2);
            }
        }

        float[][][] result = new float[rows][cols][3];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float[] n = accum[index(r, c)];
                float len = (float) Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
                if (len > 1e-6f) {
                    result[r][c][0] = n[0] / len;
                    result[r][c][1] = n[1] / len;
                    result[r][c][2] = n[2] / len;
                } else {
                    result[r][c][0] = 0; result[r][c][1] = 1; result[r][c][2] = 0;
                }
            }
        }
        return result;
    }

    private int index(int r, int c) { return r * cols + c; }

    private static void accumulate(float[][] accum, int idx, float[] n) {
        accum[idx][0] += n[0];
        accum[idx][1] += n[1];
        accum[idx][2] += n[2];
    }

    private static float[] triangleNormal(Point3D a, Point3D b, Point3D c) {
        float ux = b.x - a.x, uy = b.y - a.y, uz = b.z - a.z;
        float vx = c.x - a.x, vy = c.y - a.y, vz = c.z - a.z;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 1e-8f) { nx /= len; ny /= len; nz /= len; }
        return new float[]{nx, ny, nz};
    }

    public Point3D point(int r, int c) { return grid[r][c]; }
    public float[] normal(int r, int c) { return normals[r][c]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
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
