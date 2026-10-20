package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;

import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.List;

/**
 * Rendu OpenGL d'une ou plusieurs courbes 3D temps réel ({@link Curve3DData}),
 * vues par une caméra en orbite ({@link OrbitCamera}) autour du centre des
 * données, avec une boîte de repère graduée ({@link ReferenceBox}).
 *
 * Le texte (graduations, noms d'axes) est rendu par SWT (voir {@link GLText}),
 * sans AWT : le mélange de TextRenderer (AWT) avec le pipeline natif SWT
 * peut produire un rendu incorrect sur certaines configurations. Les
 * étiquettes sont projetées à l'écran pendant le dessin de la scène, puis
 * dessinées dans une passe 2D finale.
 */
public class Chart3DRenderer extends CurveChartRenderer<Curve3DData> {

    private static final float DEFAULT_LINE_WIDTH = 2.5f; // épaisseur des courbes sans épaisseur explicite (pixels)

    /** Étiquette placée dans la scène 3D, projetée à l'écran et dessinée par-dessus la scène. */
    private static final class Label {
        final String text;
        final ChartFont font;
        final float[] color;
        final double x, y; // position à l'écran (pixels)

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
    private final List<Label> labels = new ArrayList<>(); // étiquettes de l'image en cours
    private float[] sceneMatrix;                          // matrice données -> découpage de l'image en cours

    // Boîte englobante de l'ensemble des courbes, recalculée à chaque image
    private BoundingBox bounds = BoundingBox.DEFAULT;
    private double[] frameFixedScales; // échelles fixes figées pour l'image en cours
    private float aspect = 1f;         // largeur / hauteur de la zone d'affichage

    /**
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     */
    public Chart3DRenderer(double historySize, Curve3DData... curves) {
        super(5f, curves);
        setWindowTimeWidth(historySize);
    }

    /** Graphe dont chaque courbe garde sa propre durée d'historique (données statiques, par exemple). */
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
        aspect = (float) this.width / this.height; // la projection est appliquée dans display()
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        beginFrame();
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Fige les données reçues depuis l'image précédente, puis calcule la boîte englobante commune
        labels.clear();
        snapshotCurves();
        computeBounds();

        // La projection dépend du rayon englobant, qui varie en temps réel :
        // elle est donc recalculée à chaque image (et non seulement dans reshape()).
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
        double scale = height / distance; // échelle d'affichage, sert à détecter les changements d'échelle des tirets
        drawTrajectories(gl, DEFAULT_LINE_WIDTH, (x, y, z, pixel) -> project(x, y, z, pixel), scale, scale);
        if (showPoints) drawTrajectoryPoints(gl);
        enableClipping(gl, false);
        drawOverlay(gl);
    }

    /** Boîte englobante de l'ensemble des courbes non vides (de -1 à 1 s'il n'y en a aucune), ou échelles fixes. */
    private void computeBounds() {
        frameFixedScales = getFixedScaleBounds();
        if (frameFixedScales != null) { // échelles fixes : la boîte est exactement celle fournie
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
     * Avec des échelles fixes, coupe ce qui sort de la boîte [xMin, xMax] x
     * [yMin, yMax] x [zMin, zMax] (6 plans de découpage, exprimés dans le
     * repère des données : la matrice courante doit être celle de la scène).
     */
    private void enableClipping(GL2 gl, boolean enable) {
        double[] f = frameFixedScales;
        if (f == null) return;
        for (int i = 0; i < 6; i++) {
            int plane = GL2.GL_CLIP_PLANE0 + i;
            if (enable) {
                int axis = i / 2;
                double[] equation = new double[4];
                // Plan i pair : coordonnée >= min ; plan i impair : coordonnée <= max
                equation[axis] = (i % 2 == 0) ? 1 : -1;
                equation[3] = (i % 2 == 0) ? -f[2 * axis] : f[2 * axis + 1];
                gl.glClipPlane(plane, equation, 0);
                gl.glEnable(plane);
            } else {
                gl.glDisable(plane);
            }
        }
    }

    /** Matrice (colonne majeure) qui transforme les coordonnées des données en coordonnées de découpage (projection x vue). */
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

    /** Position à l'écran (pixels) du point (x, y, z) de la scène ; false s'il est derrière la caméra. */
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
     * Étiquette au point (x, y, z) de la scène, dans la couleur courante : la
     * position est projetée à l'écran maintenant et le texte est dessiné dans
     * la passe 2D finale (drawOverlay).
     */
    private void addLabel(GL2 gl, String label, float x, float y, float z, boolean axisName) {
        double[] screen = new double[2];
        if (!project(x, y, z, screen)) return; // derrière la caméra
        float[] color = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, color, 0);
        labels.add(new Label(label, axisName ? axisNameFont() : scaleFont, color, screen[0], screen[1]));
    }

    /** Police des noms d'axes : celle des graduations, en gras et 70 % plus grande. */
    private ChartFont axisNameFont() {
        ChartFont font = scaleFont;
        return font.withSize(Math.round(font.getSize() * 1.7f)).withStyle(font.getStyle() | SWT.BOLD);
    }

    /**
     * Passe 2D finale, en pixels, par-dessus la scène 3D : étiquettes des
     * échelles et noms d'axes, puis légende en haut à gauche (si au moins une
     * courbe a un nom), cadence d'affichage et valeurs courantes.
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

    public void rotate(float deltaX, float deltaY) { camera.rotate(deltaX, deltaY); }
    public void pan(float deltaX, float deltaY) { camera.pan(deltaX, deltaY, bounds.radius); }
    public void zoom(float amount) { camera.zoom(amount); }

    @Override
    public void resetView() { camera.reset(); }

    /**
     * Échelles fixes : la boîte de repère et les graduations vont exactement
     * de xMin à xMax, yMin à yMax et zMin à zMax, quelles que soient les
     * valeurs des courbes (ce qui sort de la boîte est coupé). La caméra
     * reste alors à distance constante.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        setFixedScaleBounds(xMin, xMax, yMin, yMax, zMin, zMax);
    }
}
