package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;

import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
 *
 * Markers: points identified by a label and placed on a curve, whose color
 * they take; the markers of a curve with the same label form a group
 * (curve, label). They are only displayed (no mouse action): a circle filled
 * in white, outlined in the color of the curve, drawn over the scene, with
 * the label next to it ({@link #addMarkersGroup}, {@link #setMarkersVisible},
 * {@link #setMarkerLabelsVisible}, {@link #setMarkerSize}). Removing a curve
 * removes its markers. With fixed scales, the markers outside the box are
 * not drawn.
 *
 * The camera state ({@link #getCameraState}, {@link #setCameraState}) allows
 * a point of view to be saved and restored; {@link CameraListener}s are told
 * about its changes.
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
    private volatile double[] displayedBounds; // bounds of the last frame {xMin, xMax, yMin, yMax, zMin, zMax}

    // Markers: groups by curve, then by label in creation order ({x, y, z} each); guarded by markersLock
    private final Map<Curve3DData, Map<String, List<double[]>>> markerGroups = new LinkedHashMap<>();
    private final Object markersLock = new Object();
    private volatile boolean markersVisible = true;
    private volatile boolean markerLabelsVisible = true;
    private volatile int markerSize = PlotRenderer.DEFAULT_MARKER_SIZE;

    // Camera: listeners, and state of the previous frame (rendering thread only; null before the first frame)
    private final List<CameraListener> cameraListeners = new CopyOnWriteArrayList<>();
    private float[] previousCameraState;

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
        displayedBounds = new double[]{bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, bounds.minZ, bounds.maxZ};
        notifyCamera();

        // The projection depends on the bounding radius, which varies in real time:
        // it is therefore recomputed at each frame (and not only in reshape()).
        float radius = bounds.radius;
        float distance = camera.distance(radius);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(OrbitCamera.FIELD_OF_VIEW, aspect, Math.max(distance - 2f * radius, distance * 0.01f), distance + 2f * radius);
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
     * names, then legend at the top left (if there is at least one curve),
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
        drawMarkers(gl);
        if (showLegend) CurveStyles.drawLegend(gl, text, legendFont, frameCurves, 16, height - 10);
        drawFps(gl, width - 10, 8);
        drawCurrentValues(gl, width - 10, height - 10);
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    // --- Bounds ---

    /**
     * Bounds of the box displayed by the last frame (data or fixed scales)
     * {xMin, xMax, yMin, yMax, zMin, zMax} (copy), or null before the first
     * frame.
     */
    public double[] getBounds() {
        double[] b = displayedBounds;
        return (b != null) ? b.clone() : null;
    }

    // --- Markers ---

    /**
     * Adds markers to the group (curve, label), which is created if needed
     * (markers already present are skipped).
     *
     * @param coordinates coordinates {x, y, z} of each marker, in data units
     * @throws IllegalArgumentException if the curve is not in the chart, the label is empty or a marker has not 3 coordinates
     */
    public void addMarkersGroup(Curve3DData curve, String label, double[][] coordinates) {
        if (label == null || label.trim().isEmpty()) throw new IllegalArgumentException("The marker label cannot be empty.");
        for (double[] xyz : coordinates) {
            if (xyz == null || xyz.length != 3) throw new IllegalArgumentException("A 3D marker has 3 coordinates.");
        }
        synchronized (markersLock) {
            if (curve == null || !getCurves().contains(curve)) throw new IllegalArgumentException("The curve is not in the chart.");
            List<double[]> group = markerGroups.computeIfAbsent(curve, c -> new LinkedHashMap<>()).computeIfAbsent(label, l -> new ArrayList<>());
            for (double[] xyz : coordinates) {
                boolean present = false;
                for (double[] marker : group) present |= Arrays.equals(marker, xyz);
                if (!present) group.add(xyz.clone());
            }
        }
    }

    /** Removes the group (curve, label); false if it does not exist. */
    public boolean removeMarkersGroup(Curve3DData curve, String label) {
        synchronized (markersLock) {
            Map<String, List<double[]>> groups = markerGroups.get(curve);
            if (groups == null || groups.remove(label) == null) return false;
            if (groups.isEmpty()) markerGroups.remove(curve);
            return true;
        }
    }

    /** Removes all the markers of the curve. */
    public void clearMarkers(Curve3DData curve) {
        synchronized (markersLock) {
            markerGroups.remove(curve);
        }
    }

    /** Curves that have markers. */
    public List<Curve3DData> getMarkersCurves() {
        synchronized (markersLock) {
            return new ArrayList<>(markerGroups.keySet());
        }
    }

    /** Labels of the marker groups of the curve, in creation order. */
    public List<String> getMarkersLabels(Curve3DData curve) {
        synchronized (markersLock) {
            Map<String, List<double[]>> groups = markerGroups.get(curve);
            return (groups != null) ? new ArrayList<>(groups.keySet()) : new ArrayList<>();
        }
    }

    /** Coordinates {x, y, z} of the markers of the group (curve, label) (copy), or null if there is no such group. */
    public double[][] getMarkersGroup(Curve3DData curve, String label) {
        synchronized (markersLock) {
            Map<String, List<double[]>> groups = markerGroups.get(curve);
            List<double[]> group = (groups != null) ? groups.get(label) : null;
            if (group == null) return null;
            double[][] coordinates = new double[group.size()][];
            for (int i = 0; i < coordinates.length; i++) coordinates[i] = group.get(i).clone();
            return coordinates;
        }
    }

    /** The markers of a removed curve are removed. */
    @Override
    protected void curveRemoved(Curve3DData curve, int index) {
        super.curveRemoved(curve, index);
        clearMarkers(curve);
    }

    /** Shows (default) or hides all the markers (they are kept). Usable from any thread. */
    public void setMarkersVisible(boolean visible) { markersVisible = visible; }
    public boolean isMarkersVisible() { return markersVisible; }

    /** Shows (default) or hides the marker labels (the circles remain). Usable from any thread. */
    public void setMarkerLabelsVisible(boolean visible) { markerLabelsVisible = visible; }
    public boolean isMarkerLabelsVisible() { return markerLabelsVisible; }

    /** Radius of the marker circles, in pixels (5 by default, limited to [1, 50]). Usable from any thread. */
    public void setMarkerSize(int size) { markerSize = Math.max(1, Math.min(50, size)); }
    public int getMarkerSize() { return markerSize; }

    /**
     * Markers of the curves of the frame, over the scene (2D pass, in
     * pixels): circle filled in white and outlined in the curve color, label
     * next to the point, below on the right.
     */
    private void drawMarkers(GL2 gl) {
        if (!markersVisible) return;
        List<double[]> points = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<float[]> colors = new ArrayList<>();
        synchronized (markersLock) {
            for (Curve3DData curve : frameCurves) { // curves of the frame: those removed since are not drawn
                Map<String, List<double[]>> groups = markerGroups.get(curve);
                if (groups == null) continue;
                float[] color = CurveStyles.colorOf(curve);
                for (Map.Entry<String, List<double[]>> group : groups.entrySet()) {
                    for (double[] marker : group.getValue()) {
                        points.add(marker);
                        names.add(group.getKey());
                        colors.add(color);
                    }
                }
            }
        }
        double[] f = frameFixedScales;
        float r = markerSize;
        ChartFont font = scaleFont;
        float h = text.lineHeight(font) + 4;
        double[] screen = new double[2];
        for (int m = 0; m < points.size(); m++) {
            double[] p = points.get(m);
            if (f != null && (p[0] < f[0] || p[0] > f[1] || p[1] < f[2] || p[1] > f[3] || p[2] < f[4] || p[2] > f[5])) continue; // outside the box
            if (!project((float) p[0], (float) p[1], (float) p[2], screen)) continue; // behind the camera
            float px = (float) screen[0], py = (float) screen[1];
            float[] c = colors.get(m);
            gl.glColor3f(1f, 1f, 1f);
            CurveStyles.drawCircle(gl, GL2.GL_POLYGON, px, py, r);
            gl.glColor3f(c[0], c[1], c[2]);
            gl.glLineWidth(1.5f);
            CurveStyles.drawCircle(gl, GL2.GL_LINE_LOOP, px, py, r);
            gl.glLineWidth(1f);
            if (markerLabelsVisible) {
                String label = names.get(m);
                drawBadge(gl, label, font, c, px + r + 1, py - r - h, text.width(label, font) + 8, h);
            }
        }
    }

    // --- Camera ---

    /**
     * Camera state, to save and restore a point of view (see
     * {@link OrbitCamera#getState()}).
     */
    public float[] getCameraState() { return camera.getState(); }

    /**
     * Restores a camera state given by {@link #getCameraState()}.
     *
     * @throws IllegalArgumentException if the state is not valid (see {@link OrbitCamera#setState})
     */
    public void setCameraState(float[] state) { camera.setState(state); }

    /** Adds a camera listener (see {@link CameraListener} for the calling thread). */
    public void addCameraListener(CameraListener listener) { if (listener != null) cameraListeners.add(listener); }
    public void removeCameraListener(CameraListener listener) { cameraListeners.remove(listener); }

    /** Notifies the camera listeners if the camera changed since the previous frame (not at the first frame). */
    private void notifyCamera() {
        float[] state = camera.getState();
        float[] previous = previousCameraState;
        previousCameraState = state;
        if (previous == null || Arrays.equals(previous, state)) return;
        for (CameraListener listener : cameraListeners) {
            try {
                listener.cameraChanged(state.clone());
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
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

    /** Moves the view by steps of a tenth of the visible height (also with the arrow keys), see {@link OrbitCamera#panStep}. */
    public void panStep(int stepsX, int stepsY) {
        camera.panStep(stepsX, stepsY, bounds.radius);
    }

    /** Arrow keys: pan, as a drag with the right button. */
    @Override
    public void keyPressed(char key) {
        switch (key) {
            case ARROW_LEFT: panStep(-1, 0); break;
            case ARROW_RIGHT: panStep(1, 0); break;
            case ARROW_UP: panStep(0, 1); break;
            case ARROW_DOWN: panStep(0, -1); break;
            default: super.keyPressed(key); break;
        }
    }

    /** Camera of the chart (for the tests). */
    OrbitCamera camera() { return camera; }
    
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
