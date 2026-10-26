package fr.univamu.ism.docometre.chartxyz;

/**
 * Immutable point in 3D space.
 */
public class Point3D {

    public final float x, y, z;

    public Point3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
