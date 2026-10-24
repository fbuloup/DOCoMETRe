package fr.univamu.ism.docometre.chartxyz;

/**
 * Color of a curve, immutable: 0..255 components (public API, comparison with
 * the palette) and OpenGL components (r / 255), computed once. Replacing the
 * reference is enough to change a color: the rendering thread always sees a
 * complete color.
 */
public final class CurveColor {

    final int r, g, b;
    final float[] gl; // {r, g, b} / 255, never modified

    /** @throws IllegalArgumentException if a component is outside 0..255 */
    public CurveColor(int r, int g, int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("The color components must be between 0 and 255: (" + r + ", " + g + ", " + b + ").");
        }
        this.r = r;
        this.g = g;
        this.b = b;
        this.gl = new float[]{r / 255f, g / 255f, b / 255f};
    }

    boolean sameAs(CurveColor other) {
        return other != null && r == other.r && g == other.g && b == other.b;
    }

    /** Copy of the 0..255 components. */
    public int[] toRGB() {
        return new int[]{r, g, b};
    }
}
