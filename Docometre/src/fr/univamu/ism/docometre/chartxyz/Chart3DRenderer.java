package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;

import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenGL rendering of one or several real-time 3D curves ({@link Curve3DData}),
 * seen by a camera orbiting ({@link OrbitCamera}) around the center of the
 * data, with a graduated reference box ({@link ReferenceBox}).
 *
 * Text (tick labels, axis names) is rendered by SWT (see {@link GLText}),
 * without AWT: mixing TextRenderer (AWT) with the native SWT pipeline can
 * produce incorrect rendering on some configurations. The labels are
 * projected on screen while the scene is drawn, then drawn in a final 2D
 * pass.
 */
public class Chart3DRenderer extends CurveChartRenderer<Curve3DData> {

    private static final float DEFAULT_LINE_WIDTH = 2.5f; // width of the curves without an explicit width (pixels)

    /** Label placed in the 3D scene, projected on screen and drawn over the scene. */
    private static final class Label {
        final String text;
        final ChartFont font;
        final float[] color;
        final double x, y; // on-screen position (pixels)

        Label(String text, ChartFont font, float[] color, double x, double y) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }

    private final GLU glu = new GLU();
    private final OrbitCamera camera = new OrbitCamera(25f, -35f);
    private final List<Label> labels = new ArrayList<>(); // labels of the current frame
    private float[] sceneMatrix;                          // data -> clip matrix of the current frame

    // Bounding box of all the curves, recomputed at each frame
    private BoundingBox bounds = BoundingBox.DEFAULT;
    private double[] frameFixedScales; // fixed scales frozen for the current frame
    private float aspect = 1f;         // width / height of the drawing area

    /**
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     */
    public Chart3DRenderer(double historySize, Curve3DData... curves) {
        super(5f, curves);
        setWindowTimeWidth(historySize);
    }

    /** Chart in which each curve keeps its own history duration (static data, for example). */
    public Chart3DRenderer(Curve3DData... curves) {
        super(5f, curves);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        drawable.getGL().getGL2().glEnable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        drawable.getGL().getGL2().glViewport(0, 0, this.width, this.height);
        aspect = (float) this.width / this.height; // the projection is applied in display()
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        beginFrame();
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Freezes the data received since the previous frame, then computes the common bounding box
        labels.clear();
        snapshotCurves();
        computeBounds();

        // The projection depends on the bounding radius, which varies in real time:
        // it is therefore recomputed at each frame (and not only in reshape()).
        float radius = bounds.radius;
        float distance = camera.distance(radius);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, aspect, Math.max(distance - 2f * radius, distance * 0.01f), distance + 2f * radius);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        camera.lookAt(glu, radius);
        camera.orbit(gl, bounds);
        sceneMatrix = sceneMatrix(gl);

        if (showGrid) {
            ReferenceBox box = new ReferenceBox(bounds, frameFixedScales == null, camera.positionInDataSpace(bounds));
            box.drawGrid(gl, this::addLabel);
            if (showLabel) box.drawTickLabels(gl, this::addLabel);
        }
        if (showAxes) ReferenceBox.drawOriginAxes(gl, radius);
        enableClipping(gl, true);
        double scale = height / distance; // display scale, used to detect scale changes for the dashes
        drawTrajectories(gl, DEFAULT_LINE_WIDTH, (x, y, z, pixel) -> project(x, y, z, pixel), scale, scale);
        if (showPoints) drawTrajectoryPoints(gl);
        enableClipping(gl, false);
        drawOverlay(gl);
    }

    /** Bounding box of all the non-empty curves (from -1 to 1 if there is none), or fixed scales. */
    private void computeBounds() {
        frameFixedScales = getFixedScaleBounds();
        if (frameFixedScales != null) { // fixed scales: the box is exactly the one given
            double[] f = frameFixedScales;
            bounds = new BoundingBox((float) f[0], (float) f[1], (float) f[2], (float) f[3], (float) f[4], (float) f[5]);
            return;
        }
        BoundingBox union = null;
        for (Curve3DData curve : frameCurves) {
            if (curve.getSnapshotCount() == 0) continue;
            union = curve.getBoundingBox().union(union);
        }
        bounds = (union != null) ? union : BoundingBox.DEFAULT;
    }

    /**
     * With fixed scales, clips what leaves the box [xMin, xMax] x
     * [yMin, yMax] x [zMin, zMax] (6 clipping planes, expressed in the data
     * coordinate system: the current matrix must be the scene matrix).
     */
    private void enableClipping(GL2 gl, boolean enable) {
        double[] f = frameFixedScales;
        if (f == null) return;
        for (int i = 0; i < 6; i++) {
            int plane = GL2.GL_CLIP_PLANE0 + i;
            if (enable) {
                int axis = i / 2;
                double[] equation = new double[4];
                // Even plane i: coordinate >= min; odd plane i: coordinate <= max
                equation[axis] = (i % 2 == 0) ? 1 : -1;
                equation[3] = (i % 2 == 0) ? -f[2 * axis] : f[2 * axis + 1];
                gl.glClipPlane(plane, equation, 0);
                gl.glEnable(plane);
            } else {
                gl.glDisable(plane);
            }
        }
    }

    /** Matrix (column-major) transforming data coordinates into clip coordinates (projection x view). */
    private static float[] sceneMatrix(GL2 gl) {
        float[] modelView = new float[16], projection = new float[16], result = new float[16];
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelView, 0);
        gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection, 0);
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) sum += projection[k * 4 + row] * modelView[col * 4 + k];
                result[col * 4 + row] = sum;
            }
        }
        return result;
    }

    /** On-screen position (pixels) of the scene point (x, y, z); false if it is behind the camera. */
    private boolean project(float x, float y, float z, double[] out) {
        float[] m = sceneMatrix;
        double w = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (!(w > 1e-9)) return false;
        double cx = (m[0] * x + m[4] * y + m[8] * z + m[12]) / w;
        double cy = (m[1] * x + m[5] * y + m[9] * z + m[13]) / w;
        out[0] = (cx + 1) / 2 * width;
        out[1] = (cy + 1) / 2 * height;
        return true;
    }

    /**
     * Label at the scene point (x, y, z), in the current color: the position
     * is projected on screen now and the text is drawn in the final 2D pass
     * (drawOverlay).
     */
    private void addLabel(GL2 gl, String label, float x, float y, float z, boolean axisName) {
        double[] screen = new double[2];
        if (!project(x, y, z, screen)) return; // behind the camera
        float[] color = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, color, 0);
        labels.add(new Label(label, axisName ? axisNameFont() : scaleFont, color, screen[0], screen[1]));
    }

    /** Font of the axis names: the one of the tick labels, bold and 70% larger. */
    private ChartFont axisNameFont() {
        ChartFont font = scaleFont;
        return font.withSize(Math.round(font.getSize() * 1.7f)).withStyle(font.getStyle() | SWT.BOLD);
    }

    /**
     * Final 2D pass, in pixels, over the 3D scene: tick labels and axis
     * names, then legend at the top left (if at least one curve has a name),
     * frame rate and current values.
     */
    private void drawOverlay(GL2 gl) {
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, width, 0, height, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        for (Label label : labels) {
            gl.glColor4f(label.color[0], label.color[1], label.color[2], label.color[3]);
            text.draw(gl, label.text, label.font, (float) label.x, (float) label.y);
        }
        if (showLegend) CurveStyles.drawLegend(gl, text, legendFont, frameCurves, 16, height - 10);
        drawFps(gl, width - 10, 8);
        drawCurrentValues(gl, width - 10, height - 10);
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    // --- Navigation ---

    @Override
    public void drag(int button, int dx, int dy, int width, int height, InputScale scale) {
        camera.drag(button, dx, dy, scale, bounds.radius);
    }

    @Override
    public void wheel(float amount, InputScale scale) {
        camera.wheel(amount, scale);
    }

    public void rotate(float deltaX, float deltaY) { 
    	camera.rotate(deltaX, deltaY); 
    }
    
    public void pan(float deltaX, float deltaY) { 
    	camera.pan(deltaX, deltaY, bounds.radius); 
    }
    
    public void zoom(float amount) { 
    	camera.zoom(amount); 
    }

    @Override
    public void resetView() { 
    	camera.reset(); 
    }

    /**
     * Fixed scales: the reference box and the tick labels go exactly from
     * xMin to xMax, yMin to yMax and zMin to zMax, whatever the curve values
     * (what leaves the box is clipped). The camera then stays at a constant
     * distance.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        setFixedScaleBounds(xMin, xMax, yMin, yMax, zMin, zMax);
    }
}
