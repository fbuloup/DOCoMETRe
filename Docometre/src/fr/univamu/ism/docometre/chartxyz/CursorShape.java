package fr.univamu.ism.docometre.chartxyz;

/**
 * Geometric shape of the point displayed by a {@link GLCursorChart2D} (see
 * {@link CursorStyle}). Each shape is filled and can be outlined; its size
 * is the width of its bounding square, in pixels.
 */
public enum CursorShape {
    /** Disk. */
    CIRCLE,
    /** Square, sides parallel to the axes. */
    SQUARE,
    /** Square rotated by 45 degrees. */
    DIAMOND,
    /** Triangle pointing upwards. */
    TRIANGLE,
    /** Triangle pointing downwards. */
    TRIANGLE_DOWN,
    /** Cross with horizontal and vertical arms (+). */
    PLUS,
    /** Cross with diagonal arms (x). */
    CROSS,
    /** Five-pointed star. */
    STAR
}
