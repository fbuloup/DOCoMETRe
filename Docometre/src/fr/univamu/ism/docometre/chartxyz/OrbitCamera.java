package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

/**
 * Caméra en orbite autour du centre des données, commune aux graphes 3D
 * ({@link Chart3DRenderer}) et aux surfaces ({@link Surface3DRenderer}) :
 * inclinaison, rotation autour de l'axe vertical, distance proportionnelle
 * au rayon englobant et panoramique.
 *
 * Navigation à la souris :
 * <ul>
 * <li>clic gauche + glisser : rotation (orbite)</li>
 * <li>clic droit + glisser : panoramique</li>
 * <li>molette : zoom avant/arrière</li>
 * </ul>
 */
public final class OrbitCamera {

    private static final float DEFAULT_DISTANCE_FACTOR = 3.0f;

    private final float defaultRotationX, defaultRotationY;
    private float rotationX;          // inclinaison haut/bas (degrés)
    private float rotationY;          // rotation autour de l'axe vertical (degrés)
    private float distanceFactor = DEFAULT_DISTANCE_FACTOR; // distance = rayon englobant * ce facteur
    private float panX = 0f, panY = 0f;

    /** Caméra dont la vue initiale (et celle de {@link #reset()}) a les angles donnés, en degrés. */
    public OrbitCamera(float defaultRotationX, float defaultRotationY) {
        this.defaultRotationX = defaultRotationX;
        this.defaultRotationY = defaultRotationY;
        reset();
    }

    /** Distance de la caméra au centre des données, pour un rayon englobant donné. */
    public float distance(float radius) {
        return radius * distanceFactor;
    }

    /** Place la caméra sur l'axe z, à distance(radius) de l'origine, regardant l'origine. */
    public void lookAt(GLU glu, float radius) {
        glu.gluLookAt(0, 0, distance(radius),
                      0, 0, 0,
                      0, 1, 0);
    }

    /** Applique panoramique et rotations, puis centre les données de la boîte à l'origine. */
    public void orbit(GL2 gl, BoundingBox box) {
        gl.glTranslatef(panX, panY, 0f);
        gl.glRotatef(rotationX, 1f, 0f, 0f);
        gl.glRotatef(rotationY, 0f, 1f, 0f);
        gl.glTranslatef(-box.centerX, -box.centerY, -box.centerZ);
    }

    /**
     * Position de la caméra dans le repère des données, obtenue en inversant
     * les transformations de {@link #lookAt} et {@link #orbit} (distance,
     * panoramique, rotations). Sert à déterminer quelles faces de la boîte de
     * repère sont les plus proches de l'utilisateur, pour ne pas les dessiner.
     */
    public float[] positionInDataSpace(BoundingBox box) {
        // Position de la caméra juste avant panoramique/rotations (cf. lookAt)
        float dx = -panX;
        float dy = -panY;
        float dz = distance(box.radius);

        // Annule la rotation autour de X (inverse de glRotatef(rotationX, 1,0,0))
        double radX = Math.toRadians(-rotationX);
        float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
        float y1 = dy * cosX - dz * sinX;
        float z1 = dy * sinX + dz * cosX;
        float x1 = dx;

        // Annule la rotation autour de Y (inverse de glRotatef(rotationY, 0,1,0))
        double radY = Math.toRadians(-rotationY);
        float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);
        float x2 = x1 * cosY + z1 * sinY;
        float z2 = -x1 * sinY + z1 * cosY;

        return new float[]{x2 + box.centerX, y1 + box.centerY, z2 + box.centerZ};
    }

    /** Glisser : rotation (bouton gauche) ou panoramique (bouton droit). */
    public void drag(int button, int dx, int dy, ChartInteraction.InputScale scale, float radius) {
        if (button == ChartInteraction.LEFT_BUTTON) {
            rotate(dx * scale.degreesPerPixel, dy * scale.degreesPerPixel);
        } else if (button == ChartInteraction.RIGHT_BUTTON) {
            pan(dx, dy, radius);
        }
    }

    /** Molette : rapproche (amount > 0) ou éloigne la caméra. */
    public void wheel(float amount, ChartInteraction.InputScale scale) {
        zoom(-amount * scale.zoomPerWheelUnit);
    }

    public void rotate(float deltaX, float deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        rotationX = Math.max(-89f, Math.min(89f, rotationX));
    }

    /** Panoramique de (deltaX, deltaY) pixels, proportionnel au rayon englobant. */
    public void pan(float deltaX, float deltaY, float radius) {
        float scale = radius * 0.002f;
        panX += deltaX * scale;
        panY -= deltaY * scale;
    }

    public void zoom(float amount) {
        distanceFactor += amount;
        distanceFactor = Math.max(0.5f, Math.min(20f, distanceFactor));
    }

    public void reset() {
        rotationX = defaultRotationX;
        rotationY = defaultRotationY;
        distanceFactor = DEFAULT_DISTANCE_FACTOR;
        panX = 0f;
        panY = 0f;
    }
}
