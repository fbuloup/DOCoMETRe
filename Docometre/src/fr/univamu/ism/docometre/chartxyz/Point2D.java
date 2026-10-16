package fr.univamu.ism.docometre.chartxyz;

/**
 * Représente un point immuable dans le plan.
 */
public class Point2D {

    public final float x, y;

    public Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
