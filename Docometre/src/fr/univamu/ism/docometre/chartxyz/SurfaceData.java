package fr.univamu.ism.docometre.chartxyz;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 3D surface defined by a regular grid of points (rows x cols), together
 * with the per-vertex normals (for smooth shading) and the geometric
 * information useful for rendering.
 *
 * Each quadrilateral (r,c)-(r,c+1)-(r+1,c)-(r+1,c+1) is triangulated along
 * the diagonal (r,c)-(r+1,c+1), into two triangles:
 *   T1 = (r,c), (r+1,c), (r+1,c+1)
 *   T2 = (r,c), (r+1,c+1), (r,c+1)
 */
public class SurfaceData {

    private final Point3D[][] grid;     // grid[row][col]
    private final float[][][] normals;  // normals[row][col] = {nx, ny, nz}
    private final int rows, cols;

    private final BoundingBox bounds;

    public SurfaceData(Point3D[][] grid) {
        if (grid == null || grid.length < 2 || grid[0].length < 2) {
            throw new IllegalArgumentException("The grid must have at least 2 rows and 2 columns.");
        }
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Point3D[] row : grid) {
            for (Point3D p : row) {
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
                minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
            }
        }
        this.bounds = new BoundingBox(minX, maxX, minY, maxY, minZ, maxZ);
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
     * Loads a grid from a text file.
     * Expected format:
     *   line 1             : "rows cols"
     *   following lines    : rows*cols points "x y z", row by row
     *                        (all of row 0 first, then row 1, etc.)
     * Empty lines and lines starting with '#' are ignored.
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
            throw new IOException("Empty file or file without valid data: " + path);
        }

        String[] dims = dataLines.get(0).split("[\\s,;]+");
        if (dims.length < 2) {
            throw new IOException("The first line must contain 'rows cols': " + dataLines.get(0));
        }
        int rows = Integer.parseInt(dims[0]);
        int cols = Integer.parseInt(dims[1]);

        int expected = rows * cols;
        if (dataLines.size() - 1 < expected) {
            throw new IOException("Not enough points. Expected " + expected
                    + ", found " + (dataLines.size() - 1));
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
     * Computes a smoothed normal per vertex by averaging the normals of all
     * the faces (triangles) touching that vertex.
     */
    private float[][][] computeSmoothNormals() {
        float[][] accum = new float[rows * cols][3]; // accumulator (nx, ny, nz) per vertex
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
    /** Bounding box of the grid. */
    public BoundingBox getBoundingBox() { return bounds; }
    public float getCenterX() { return bounds.centerX; }
    public float getCenterY() { return bounds.centerY; }
    public float getCenterZ() { return bounds.centerZ; }
    public float getMinX() { return bounds.minX; }
    public float getMaxX() { return bounds.maxX; }
    public float getMinY() { return bounds.minY; }
    public float getMaxY() { return bounds.maxY; }
    public float getMinZ() { return bounds.minZ; }
    public float getMaxZ() { return bounds.maxZ; }
    public float getBoundingRadius() { return bounds.radius; }
}
