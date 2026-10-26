package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;

/**
 * Reference box of the 3D scenes ({@link Chart3DRenderer}, {@link Surface3DRenderer}):
 * grid lines are drawn on the faces of a box that exactly matches the
 * actual bounding box of the data (and not a uniform cube). So as not to
 * hide the data, only the 3 faces FARTHEST from the camera are drawn (one
 * per axis); the 3 closest faces are omitted.
 *
 * The tick labels (numeric values) of the 3 axes are placed along the edges
 * shared by the visible faces: that is where they remain readable whatever
 * the view angle. Text drawing is delegated to a {@link LabelSink}, specific
 * to each renderer.
 */
final class ReferenceBox {

    /** Number of intervals between ticks: TICK_COUNT + 1 labels per axis. */
    static final int TICK_COUNT = 5;
    /** Number of grid intervals on each face. */
    private static final int DIVISIONS = 10;

    private static final String[] AXIS_NAMES = {"x", "y", "z"};
    private static final float[][] AXIS_COLORS = {
            {0.9f, 0.2f, 0.2f},  // x: red
            {0.2f, 0.9f, 0.2f},  // y: green
            {0.3f, 0.5f, 0.95f}, // z: blue
    };

    /** Drawing of a label at the scene point (x, y, z), in the current OpenGL color. */
    interface LabelSink {
        /** @param axisName true for an axis name ('x', 'y', 'z'), false for a tick label */
        void label(GL2 gl, String text, float x, float y, float z, boolean axisName);
    }

    // Per axis (0 = x, 1 = y, 2 = z)
    private final float[] min = new float[3], max = new float[3];
    private final float[] face = new float[3]; // coordinate of the visible face perpendicular to the axis
    private final int[] skip = new int[3];     // tick coinciding with the common corner (to be omitted)

    /**
     * @param data       bounding box of the data
     * @param withMargin small space between the data and the box (without a
     *                   margin, the box matches the bounds exactly: fixed scales)
     * @param camera     position of the camera in the data coordinate system
     */
    ReferenceBox(BoundingBox data, boolean withMargin, float[] camera) {
        float[] dataMin = {data.minX, data.minY, data.minZ};
        float[] dataMax = {data.maxX, data.maxY, data.maxZ};
        for (int a = 0; a < 3; a++) {
            // Space proportional to each dimension
            float margin = withMargin ? Math.max((dataMax[a] - dataMin[a]) * 0.08f, data.radius * 0.02f) : 0f;
            min[a] = dataMin[a] - margin;
            max[a] = dataMax[a] + margin;
            face[a] = (camera[a] > (min[a] + max[a]) / 2f) ? min[a] : max[a];
            // On each axis, the tick that coincides with the corner shared by the 3
            // visible faces is omitted: the 3 series would overlap there otherwise.
            skip[a] = (face[a] == min[a]) ? 0 : TICK_COUNT;
        }
    }

    /**
     * Draws the grid of the 3 visible faces and, for each of them, the name
     * of its axis, placed beyond the end of the edge opposite to the corner
     * shared by the 3 visible faces.
     */
    void drawGrid(GL2 gl, LabelSink labels) {
        for (int a = 0; a < 3; a++) {
            drawFace(gl, a);

            float far = (skip[a] == 0) ? max[a] : min[a];
            float direction = (skip[a] == 0) ? 1f : -1f;
            float[] p = face.clone();
            p[a] = far + direction * (max[a] - min[a]) * 0.10f;
            gl.glDisable(GL2.GL_DEPTH_TEST);
            float[] color = AXIS_COLORS[a];
            gl.glColor3f(color[0], color[1], color[2]);
            labels.label(gl, AXIS_NAMES[a], p[0], p[1], p[2], true);
            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }

    /** Grid on the face perpendicular to axis a (at coordinate face[a]). */
    private void drawFace(GL2 gl, int a) {
        int u = (a == 0) ? 1 : 0, v = (a == 2) ? 1 : 2; // the two other axes, in order
        float[] p0 = new float[3], p1 = new float[3];
        p0[a] = p1[a] = face[a];

        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (int pass = 0; pass < 2; pass++) {
            int step = (pass == 0) ? u : v;  // axis along which the lines are distributed
            int along = (pass == 0) ? v : u; // axis along which they extend
            float delta = (max[step] - min[step]) / DIVISIONS;
            for (int i = 0; i <= DIVISIONS; i++) {
                p0[step] = p1[step] = min[step] + i * delta;
                p0[along] = min[along];
                p1[along] = max[along];
                gl.glVertex3f(p0[0], p0[1], p0[2]);
                gl.glVertex3f(p1[0], p1[1], p1[2]);
            }
        }
        gl.glEnd();
    }

    /**
     * Tick labels of the 3 axes, along the edges shared by the visible faces.
     * The depth test is disabled while they are drawn, to avoid any
     * flickering due to the exact coincidence with the grid lines.
     */
    void drawTickLabels(GL2 gl, LabelSink labels) {
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        for (int a = 0; a < 3; a++) {
            float[] p = face.clone();
            for (int i = 0; i <= TICK_COUNT; i++) {
                if (i == skip[a]) continue;
                float t = (float) i / TICK_COUNT;
                p[a] = min[a] + t * (max[a] - min[a]);
                labels.label(gl, AxisTicks.format2(p[a]), p[0], p[1], p[2], false);
            }
        }
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /** x (red), y (green) and z (blue) axes through the origin, of length 2 * 1.3 * radius. */
    static void drawOriginAxes(GL2 gl, float radius) {
        float len = radius * 1.3f;
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        for (int a = 0; a < 3; a++) {
            float[] color = AXIS_COLORS[a];
            float[] p = new float[3];
            gl.glColor3f(color[0], color[1], color[2]);
            p[a] = -len;
            gl.glVertex3f(p[0], p[1], p[2]);
            p[a] = len;
            gl.glVertex3f(p[0], p[1], p[2]);
        }
        gl.glEnd();
    }
}
