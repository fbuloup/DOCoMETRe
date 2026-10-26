package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

/**
 * Camera orbiting around the center of the data, shared by the 3D charts
 * ({@link Chart3DRenderer}) and the surfaces ({@link Surface3DRenderer}):
 * tilt, rotation around the vertical axis, distance proportional to the
 * bounding radius, and pan.
 *
 * Mouse navigation:
 * <ul>
 * <li>left click + drag: rotation (orbit)</li>
 * <li>right click + drag: pan</li>
 * <li>arrow keys: pan by a tenth of the visible height ({@link #panStep}),
 * the view moves in the direction of the arrow</li>
 * <li>wheel: zoom in/out</li>
 * </ul>
 */
public final class OrbitCamera {

    private static final float DEFAULT_DISTANCE_FACTOR = 3.0f;
    /** Vertical field of view of the 3D charts (gluPerspective), in degrees. */
    static final double FIELD_OF_VIEW = 45.0;
    /** Fraction of the visible height moved by one step of {@link #panStep} (arrow keys). */
    public static final float PAN_STEP = 0.1f;

    private final float defaultRotationX, defaultRotationY;
    private float rotationX;          // up/down tilt (degrees)
    private float rotationY;          // rotation around the vertical axis (degrees)
    private float distanceFactor = DEFAULT_DISTANCE_FACTOR; // distance = bounding radius * this factor
    private float panX = 0f, panY = 0f;

    /** Camera whose initial view (and the one of {@link #reset()}) has the given angles, in degrees. */
    public OrbitCamera(float defaultRotationX, float defaultRotationY) {
        this.defaultRotationX = defaultRotationX;
        this.defaultRotationY = defaultRotationY;
        reset();
    }

    /** Distance from the camera to the center of the data, for a given bounding radius. */
    public float distance(float radius) {
        return radius * distanceFactor;
    }

    /** Places the camera on the z axis, at distance(radius) from the origin, looking at the origin. */
    public void lookAt(GLU glu, float radius) {
        glu.gluLookAt(0, 0, distance(radius),
                      0, 0, 0,
                      0, 1, 0);
    }

    /** Applies pan and rotations, then centers the data of the box on the origin. */
    public void orbit(GL2 gl, BoundingBox box) {
        gl.glTranslatef(panX, panY, 0f);
        gl.glRotatef(rotationX, 1f, 0f, 0f);
        gl.glRotatef(rotationY, 0f, 1f, 0f);
        gl.glTranslatef(-box.centerX, -box.centerY, -box.centerZ);
    }

    /**
     * Position of the camera in the data coordinate system, obtained by
     * inverting the transformations of {@link #lookAt} and {@link #orbit}
     * (distance, pan, rotations). Used to determine which faces of the
     * reference box are closest to the user, so as not to draw them.
     */
    public float[] positionInDataSpace(BoundingBox box) {
        // Camera position just before pan/rotations (see lookAt)
        float dx = -panX;
        float dy = -panY;
        float dz = distance(box.radius);

        // Undoes the rotation around X (inverse of glRotatef(rotationX, 1,0,0))
        double radX = Math.toRadians(-rotationX);
        float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
        float y1 = dy * cosX - dz * sinX;
        float z1 = dy * sinX + dz * cosX;
        float x1 = dx;

        // Undoes the rotation around Y (inverse of glRotatef(rotationY, 0,1,0))
        double radY = Math.toRadians(-rotationY);
        float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);
        float x2 = x1 * cosY + z1 * sinY;
        float z2 = -x1 * sinY + z1 * cosY;

        return new float[]{x2 + box.centerX, y1 + box.centerY, z2 + box.centerZ};
    }

    /** Drag: rotation (left button) or pan (right button). */
    public void drag(int button, int dx, int dy, ChartInteraction.InputScale scale, float radius) {
        if (button == ChartInteraction.LEFT_BUTTON) {
            rotate(dx * scale.degreesPerPixel, dy * scale.degreesPerPixel);
        } else if (button == ChartInteraction.RIGHT_BUTTON) {
            pan(dx, dy, radius);
        }
    }

    /** Wheel: moves the camera closer (amount > 0) or farther. */
    public void wheel(float amount, ChartInteraction.InputScale scale) {
        zoom(-amount * scale.zoomPerWheelUnit);
    }

    public void rotate(float deltaX, float deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        rotationX = Math.max(-89f, Math.min(89f, rotationX));
    }

    /** Pan by (deltaX, deltaY) pixels, proportional to the bounding radius. */
    public void pan(float deltaX, float deltaY, float radius) {
        float scale = radius * 0.002f;
        panX += deltaX * scale;
        panY -= deltaY * scale;
    }

    /**
     * Moves the view by steps of {@link #PAN_STEP} of the height visible at
     * the center of the data (it follows the zoom): stepsX > 0 to the right,
     * stepsY > 0 upwards, in screen directions (the content moves the other
     * way, as with a drag in the opposite direction).
     */
    public void panStep(int stepsX, int stepsY, float radius) {
        float visibleHeight = (float) (2 * distance(radius) * Math.tan(Math.toRadians(FIELD_OF_VIEW / 2)));
        panX -= stepsX * PAN_STEP * visibleHeight;
        panY -= stepsY * PAN_STEP * visibleHeight;
    }

    /** Pan (x, y) applied in front of the rotations, in data units (for the tests). */
    float[] getPan() { return new float[]{panX, panY}; }

    /** Number of values of a camera state (see {@link #getState()}). */
    public static final int STATE_LENGTH = 5;

    /**
     * State of the camera, to save and restore a point of view:
     * {tilt (degrees), rotation around the vertical axis (degrees), distance
     * factor (distance / bounding radius), pan x, pan y (data units)}.
     */
    public float[] getState() {
        return new float[]{rotationX, rotationY, distanceFactor, panX, panY};
    }

    /**
     * Restores a state given by {@link #getState()} (the tilt and the
     * distance are limited as with the mouse).
     *
     * @throws IllegalArgumentException if the state is null, has not {@link #STATE_LENGTH} values or a value is not finite
     */
    public void setState(float[] state) {
        if (state == null || state.length != STATE_LENGTH) throw new IllegalArgumentException("A camera state has " + STATE_LENGTH + " values.");
        for (float value : state) if (!Float.isFinite(value)) throw new IllegalArgumentException("Camera state not finite.");
        rotationX = Math.max(-89f, Math.min(89f, state[0]));
        rotationY = state[1];
        distanceFactor = Math.max(0.5f, Math.min(20f, state[2]));
        panX = state[3];
        panY = state[4];
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
