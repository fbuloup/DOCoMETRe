package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;

import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class Curve3DRenderer implements GLEventListener {

    // Courbes affichées ; la liste peut être modifiée depuis n'importe quel thread
    private final List<Curve3DData> curves = new CopyOnWriteArrayList<>();
    private List<Curve3DData> frameCurves = new ArrayList<>(); // courbes figées pour l'image en cours

    // Boîte englobante de l'ensemble des courbes, recalculée à chaque image
    private float dataMinX = -1f, dataMaxX = 1f, dataMinY = -1f, dataMaxY = 1f, dataMinZ = -1f, dataMaxZ = 1f;
    private float dataCenterX, dataCenterY, dataCenterZ;
    private float dataRadius = 1f;
    private final GLU glu = new GLU();
    private final GLText text = new GLText(); // texte en police du système (graduations, noms d'axes, légende)

    // Polices réglables (voir setScaleFont / setLegendFont)
    private volatile ChartFont scaleFont = new ChartFont(9, SWT.NORMAL);
    private volatile ChartFont legendFont = new ChartFont(9, SWT.NORMAL);
    private volatile boolean showLegend = true; // légende affichée (si au moins une courbe a un nom)

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
    private final List<Label> labels = new ArrayList<>(); // étiquettes de l'image en cours
    private float[] labelMatrix;                         // matrice données -> écran de l'image en cours
    private final DashedLines dashedLines = new DashedLines(); // tirets ancrés sur les données

    private static final int TICK_COUNT = 5;
    private static final float DEFAULT_LINE_WIDTH = 2.5f; // épaisseur des courbes sans épaisseur explicite (pixels)

    // État de la caméra en orbite autour du centre de la courbe
    private float rotationX = 25f;    // inclinaison haut/bas
    private float rotationY = -35f;   // rotation autour de l'axe vertical
    private float distanceFactor = 3.0f; // distance = rayon englobant * ce facteur
    private float panX = 0f, panY = 0f;

    private boolean showAxes = false;
    private boolean showPoints = false;
    private boolean showGrid = true;
    private boolean showLabel= true;

    // Échelles fixes {xMin, xMax, yMin, yMax, zMin, zMax}, ou null pour des échelles dynamiques
    private volatile double[] fixedScales;
    private double[] frameFixedScales; // échelles fixes figées pour l'image en cours

    private int width = 1, height = 1; // taille de la zone d'affichage (pixels)
    private float aspect = 1f;         // largeur / hauteur de la zone d'affichage

    public Curve3DRenderer(Curve3DData... curves) {
        this.curves.addAll(Arrays.asList(curves));
    }

    /** Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread). */
    public void addCurve(Curve3DData curve) { curves.add(curve); }
    /** Retire une courbe du graphe. */
    public void removeCurve(Curve3DData curve) { curves.remove(curve); }
    /** Courbes actuellement affichées. */
    public List<Curve3DData> getCurves() { return new ArrayList<>(curves); }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glPointSize(5f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        text.dispose(drawable.getGL().getGL2()); // textures des textes
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Fige les données reçues depuis l'image précédente, puis calcule la boîte englobante commune
        labels.clear();
        labelMatrix = null;
        frameCurves = new ArrayList<>(curves);
        for (Curve3DData curve : frameCurves) curve.takeSnapshot();
        computeBounds();

        float distance = dataRadius * distanceFactor;

        // La projection dépend du rayon englobant, qui varie en temps réel :
        // elle est donc recalculée à chaque image (et non plus seulement dans reshape()).
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float radius = dataRadius;
        glu.gluPerspective(45.0, aspect, Math.max(distance - 2f * radius, distance * 0.01f), distance + 2f * radius);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        glu.gluLookAt(0, 0, distance,
                      0, 0, 0,
                      0, 1, 0);

        gl.glTranslatef(panX, panY, 0f);
        gl.glRotatef(rotationX, 1f, 0f, 0f);
        gl.glRotatef(rotationY, 0f, 1f, 0f);
        gl.glTranslatef(-dataCenterX, -dataCenterY, -dataCenterZ);

        if (showGrid) drawCubeGrid(gl); 
        if (showGrid && showLabel) drawAxisLabels(gl);
        if (showAxes) drawAxes(gl);
        enableClipping(gl, true);
        drawCurves(gl);
        if (showPoints) drawPoints(gl);
        enableClipping(gl, false);
        drawOverlay(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height <= 0) height = 1;
        gl.glViewport(0, 0, width, height);
        this.width = Math.max(width, 1);
        this.height = height;
        aspect = (float) width / height; // la projection est appliquée dans display()
        text.setPixelScale(GLText.surfaceScale(drawable));
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

    /** Boîte englobante, centre et rayon de l'ensemble des courbes non vides (de -1 à 1 s'il n'y en a aucune). */
    private void computeBounds() {
        frameFixedScales = fixedScales;
        if (frameFixedScales != null) { // échelles fixes : la boîte est exactement celle fournie
            double[] f = frameFixedScales;
            setBounds((float) f[0], (float) f[1], (float) f[2], (float) f[3], (float) f[4], (float) f[5]);
            return;
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        boolean empty = true;
        for (Curve3DData curve : frameCurves) {
            if (curve.getSnapshotCount() == 0) continue;
            empty = false;
            minX = Math.min(minX, curve.getMinX()); maxX = Math.max(maxX, curve.getMaxX());
            minY = Math.min(minY, curve.getMinY()); maxY = Math.max(maxY, curve.getMaxY());
            minZ = Math.min(minZ, curve.getMinZ()); maxZ = Math.max(maxZ, curve.getMaxZ());
        }
        if (empty) {
            minX = minY = minZ = -1f;
            maxX = maxY = maxZ = 1f;
        }
        setBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private void setBounds(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        dataMinX = minX; dataMaxX = maxX;
        dataMinY = minY; dataMaxY = maxY;
        dataMinZ = minZ; dataMaxZ = maxZ;
        dataCenterX = (minX + maxX) / 2f;
        dataCenterY = (minY + maxY) / 2f;
        dataCenterZ = (minZ + maxZ) / 2f;
        float dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        dataRadius = Math.max((float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2f, 0.001f);
    }

    /**
     * Dessine les courbes. Une courbe seule et sans couleur explicite garde le
     * dégradé bleu -> orange ; sinon chaque courbe a sa couleur (explicite ou
     * de la palette) et s'estompe vers ses échantillons les plus anciens.
     */
    private void drawCurves(GL2 gl) {
        float[] screen = null; // matrice données -> écran, calculée seulement si une courbe est en tirets
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve3DData curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            float[] xyz = curve.getSnapshotCoordinates();
            int n = curve.getSnapshotCount();
            boolean gradient = curve.getColor() == null && frameCurves.size() == 1;
            float[] color = CurveStyles.colorOf(curve, k);
            if (CurveStyles.isDashed(curve)) {
                if (screen == null) screen = screenMatrix(gl);
                float[] m = screen;
                double scale = height / (dataRadius * distanceFactor);
                dashedLines.draw(gl, curve, width, curve.getCapacity(), curve.getSnapshotTotalSamples(), n, 0, scale, scale,
                        i -> screenDistance(m, xyz, i - 1, i), null,
                        (i, f) -> {
                            vertexColor(gl, gradient, color, (n <= 1) ? 1f : (i - 1 + f) / (n - 1));
                            int a = 3 * (i - 1), b = 3 * i;
                            gl.glVertex3f(xyz[a] + f * (xyz[b] - xyz[a]), xyz[a + 1] + f * (xyz[b + 1] - xyz[a + 1]),
                                          xyz[a + 2] + f * (xyz[b + 2] - xyz[a + 2]));
                        });
            } else {
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < n; i++) {
                    vertexColor(gl, gradient, color, (n <= 1) ? 1f : (float) i / (n - 1));
                    gl.glVertex3f(xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2]);
                }
                gl.glEnd();
            }
        }
    }

    /**
     * Couleur d'un sommet selon sa position t le long de la courbe (0 = plus
     * ancien, 1 = plus récent) : dégradé bleu -> orange, ou couleur de la
     * courbe s'estompant vers les échantillons anciens.
     */
    private static void vertexColor(GL2 gl, boolean gradient, float[] color, float t) {
        if (gradient) {
            gl.glColor3f(0.2f + 0.8f * t, 0.55f + 0.25f * (1 - t), 1.0f - 0.8f * t);
        } else {
            gl.glColor4f(color[0], color[1], color[2], 0.25f + 0.75f * t);
        }
    }

    /** Matrice (colonne majeure) qui transforme les coordonnées des données en coordonnées de découpage (projection x vue). */
    private static float[] screenMatrix(GL2 gl) {
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

    /** Distance à l'écran, en pixels, entre les points a et b de l'instantané (0 si l'un est derrière la caméra). */
    private double screenDistance(float[] m, float[] xyz, int a, int b) {
        double[] pa = project(m, xyz[3 * a], xyz[3 * a + 1], xyz[3 * a + 2]);
        double[] pb = project(m, xyz[3 * b], xyz[3 * b + 1], xyz[3 * b + 2]);
        if (pa == null || pb == null) return 0;
        return Math.hypot(pb[0] - pa[0], pb[1] - pa[1]);
    }

    private double[] project(float[] m, float x, float y, float z) {
        double w = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (!(w > 1e-9)) return null;
        double cx = (m[0] * x + m[4] * y + m[8] * z + m[12]) / w;
        double cy = (m[1] * x + m[5] * y + m[9] * z + m[13]) / w;
        return new double[]{(cx + 1) / 2 * width, (cy + 1) / 2 * height};
    }

    private void drawPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (Curve3DData curve : frameCurves) {
            float[] xyz = curve.getSnapshotCoordinates();
            int n = curve.getSnapshotCount();
            for (int i = 0; i < n; i++) {
                gl.glVertex3f(xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2]);
            }
        }
        gl.glEnd();
    }

    /**
     * Passe 2D finale, en pixels, par-dessus la scène 3D : étiquettes des
     * échelles et noms d'axes (positions projetées pendant le dessin de la
     * scène), puis légende en haut à gauche (si au moins une courbe a un nom).
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
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    private void drawAxes(GL2 gl) {
        float len = dataRadius * 1.3f;
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        // Axe X - rouge
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glVertex3f(-len, 0, 0); gl.glVertex3f(len, 0, 0);
        // Axe Y - vert
        gl.glColor3f(0.2f, 0.9f, 0.2f);
        gl.glVertex3f(0, -len, 0); gl.glVertex3f(0, len, 0);
        // Axe Z - bleu
        gl.glColor3f(0.3f, 0.5f, 0.95f);
        gl.glVertex3f(0, 0, -len); gl.glVertex3f(0, 0, len);
        gl.glEnd();
    }

    /**
     * Dessine une grille de repère en forme de boîte : des lignes de grille
     * sont tracées sur les faces d'une boîte qui épouse précisément la boîte
     * englobante réelle de la courbe (et non un cube uniforme). Pour ne pas
     * masquer la courbe, seules les 3 faces les plus ÉLOIGNÉES de la caméra
     * sont dessinées (une par axe) ; les 3 faces les plus proches sont omises.
     */
    /** Regroupe les bornes (avec marge) de la boîte de repère et les 3 faces visibles (les plus éloignées de la caméra). */
    private static final class BoxInfo {
        final float minX, maxX, minY, maxY, minZ, maxZ;
        final float xFace, yFace, zFace;
        final int skipX, skipY, skipZ; // indice de graduation coïncidant avec le coin commun (à omettre)

        BoxInfo(float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                float xFace, float yFace, float zFace, int skipX, int skipY, int skipZ) {
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
            this.xFace = xFace; this.yFace = yFace; this.zFace = zFace;
            this.skipX = skipX; this.skipY = skipY; this.skipZ = skipZ;
        }
    }

    private BoxInfo computeBoxInfo() {
        // Léger espace entre la courbe et la boîte, proportionnel à chaque dimension
        // (aucun avec des échelles fixes : la boîte correspond exactement aux bornes fournies)
        boolean fixed = frameFixedScales != null;
        float marginX = fixed ? 0f : Math.max((dataMaxX - dataMinX) * 0.08f, dataRadius * 0.02f);
        float marginY = fixed ? 0f : Math.max((dataMaxY - dataMinY) * 0.08f, dataRadius * 0.02f);
        float marginZ = fixed ? 0f : Math.max((dataMaxZ - dataMinZ) * 0.08f, dataRadius * 0.02f);

        float minX = dataMinX - marginX, maxX = dataMaxX + marginX;
        float minY = dataMinY - marginY, maxY = dataMaxY + marginY;
        float minZ = dataMinZ - marginZ, maxZ = dataMaxZ + marginZ;

        float[] cam = cameraPositionInDataSpace();
        float xFace = (cam[0] > (minX + maxX) / 2f) ? minX : maxX;
        float yFace = (cam[1] > (minY + maxY) / 2f) ? minY : maxY;
        float zFace = (cam[2] > (minZ + maxZ) / 2f) ? minZ : maxZ;

        // Sur chaque axe, la graduation qui coïncide avec le coin commun aux 3
        // faces visibles (xFace, yFace, zFace) est omise : les 3 séries s'y
        // superposeraient sinon au même point.
        int skipX = (xFace == minX) ? 0 : TICK_COUNT;
        int skipY = (yFace == minY) ? 0 : TICK_COUNT;
        int skipZ = (zFace == minZ) ? 0 : TICK_COUNT;

        return new BoxInfo(minX, maxX, minY, maxY, minZ, maxZ, xFace, yFace, zFace, skipX, skipY, skipZ);
    }

    /**
     * Dessine une grille de repère en forme de boîte : des lignes de grille
     * sont tracées sur les faces d'une boîte qui épouse précisément la boîte
     * englobante réelle de la courbe (et non un cube uniforme). Pour ne pas
     * masquer la courbe, seules les 3 faces les plus ÉLOIGNÉES de la caméra
     * sont dessinées (une par axe) ; les 3 faces les plus proches sont omises.
     * Chaque face dessine aussi son libellé de nom d'axe ('x', 'y' ou 'z').
     */
    private void drawCubeGrid(GL2 gl) {
        BoxInfo box = computeBoxInfo();
        int divisions = 10;

        drawGridFaceX(gl, box, divisions);
        drawGridFaceY(gl, box, divisions);
        drawGridFaceZ(gl, box, divisions);
    }

    /**
     * Affiche les graduations (valeurs numériques) des 3 axes le long de
     * l'arête de la boîte de repère commune aux deux faces visibles — c'est
     * l'endroit où elles restent lisibles quel que soit l'angle de vue,
     * puisque les faces les plus proches de la caméra ne sont pas dessinées.
     *
     * Le texte est rendu par SWT (voir {@link GLText}), sans AWT : le mélange
     * de TextRenderer (AWT) avec le pipeline natif SWT peut produire un rendu
     * incorrect sur certaines configurations.
     */
    private void drawAxisLabels(GL2 gl) {
        BoxInfo box = computeBoxInfo();

        // Le test de profondeur est désactivé le temps de dessiner les étiquettes,
        // pour éviter tout scintillement lié à la coïncidence exacte avec les
        // lignes de la grille (même position que certains sommets de la boîte).
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glColor3f(0.85f, 0.85f, 0.9f);

        // Graduations le long de X, sur l'arête (y = yFace, z = zFace)
        for (int i = 0; i <= TICK_COUNT; i++) {
            if (i == box.skipX) continue;
            float t = (float) i / TICK_COUNT;
            float x = box.minX + t * (box.maxX - box.minX);
            drawBitmapLabel(gl, formatTick(x), x, box.yFace, box.zFace);
        }
        // Graduations le long de Y, sur l'arête (x = xFace, z = zFace)
        for (int i = 0; i <= TICK_COUNT; i++) {
            if (i == box.skipY) continue;
            float t = (float) i / TICK_COUNT;
            float y = box.minY + t * (box.maxY - box.minY);
            drawBitmapLabel(gl, formatTick(y), box.xFace, y, box.zFace);
        }
        // Graduations le long de Z, sur l'arête (x = xFace, y = yFace)
        for (int i = 0; i <= TICK_COUNT; i++) {
            if (i == box.skipZ) continue;
            float t = (float) i / TICK_COUNT;
            float z = box.minZ + t * (box.maxZ - box.minZ);
            drawBitmapLabel(gl, formatTick(z), box.xFace, box.yFace, z);
        }

        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /** Étiquette de graduation au point (x, y, z), dans la police des échelles. */
    private void drawBitmapLabel(GL2 gl, String label, float x, float y, float z) {
        drawBitmapLabel(gl, label, x, y, z, scaleFont);
    }

    /**
     * Étiquette au point (x, y, z) de la scène, dans la couleur courante : la
     * position est projetée à l'écran maintenant (matrices de la scène) et le
     * texte est dessiné dans la passe 2D finale (drawOverlay).
     */
    private void drawBitmapLabel(GL2 gl, String label, float x, float y, float z, ChartFont font) {
        if (labelMatrix == null) labelMatrix = screenMatrix(gl);
        double[] screen = project(labelMatrix, x, y, z);
        if (screen == null) return; // derrière la caméra
        float[] color = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, color, 0);
        labels.add(new Label(label, font, color, screen[0], screen[1]));
    }

    /** Police des noms d'axes : celle des graduations, en gras et 70 % plus grande. */
    private ChartFont axisNameFont() {
        ChartFont font = scaleFont;
        return font.withSize(Math.round(font.getSize() * 1.7f)).withStyle(font.getStyle() | SWT.BOLD);
    }

    /** Formate une valeur de graduation avec 2 décimales, en évitant l'affichage "-0.00". */
    private static String formatTick(float value) {
        if (Math.abs(value) < 0.005f) value = 0f;
        return String.format(Locale.US, "%.2f", value);
    }

    /**
     * Dessine la grille sur la face perpendiculaire à l'axe X (à x = box.xFace),
     * ainsi que le libellé 'x' identifiant l'axe, placé au-delà de l'extrémité
     * de l'arête opposée au coin commun aux 3 faces visibles.
     */
    private void drawGridFaceX(GL2 gl, BoxInfo box, int divisions) {
        float x = box.xFace;
        float stepY = (box.maxY - box.minY) / divisions;
        float stepZ = (box.maxZ - box.minZ) / divisions;

        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i <= divisions; i++) {
            float y = box.minY + i * stepY;
            gl.glVertex3f(x, y, box.minZ);
            gl.glVertex3f(x, y, box.maxZ);
        }
        for (int i = 0; i <= divisions; i++) {
            float z = box.minZ + i * stepZ;
            gl.glVertex3f(x, box.minY, z);
            gl.glVertex3f(x, box.maxY, z);
        }
        gl.glEnd();

        float edgeX = box.maxX - box.minX;
        float xFar = (box.skipX == 0) ? box.maxX : box.minX;
        float xDir = (box.skipX == 0) ? 1f : -1f;
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        drawBitmapLabel(gl, "x", xFar + xDir * edgeX * 0.10f, box.yFace, box.zFace, axisNameFont());
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /**
     * Dessine la grille sur la face perpendiculaire à l'axe Y (à y = box.yFace),
     * ainsi que le libellé 'y' identifiant l'axe, placé au-delà de l'extrémité
     * de l'arête opposée au coin commun aux 3 faces visibles.
     */
    private void drawGridFaceY(GL2 gl, BoxInfo box, int divisions) {
        float y = box.yFace;
        float stepX = (box.maxX - box.minX) / divisions;
        float stepZ = (box.maxZ - box.minZ) / divisions;

        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i <= divisions; i++) {
            float x = box.minX + i * stepX;
            gl.glVertex3f(x, y, box.minZ);
            gl.glVertex3f(x, y, box.maxZ);
        }
        for (int i = 0; i <= divisions; i++) {
            float z = box.minZ + i * stepZ;
            gl.glVertex3f(box.minX, y, z);
            gl.glVertex3f(box.maxX, y, z);
        }
        gl.glEnd();

        float edgeY = box.maxY - box.minY;
        float yFar = (box.skipY == 0) ? box.maxY : box.minY;
        float yDir = (box.skipY == 0) ? 1f : -1f;
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glColor3f(0.2f, 0.9f, 0.2f);
        drawBitmapLabel(gl, "y", box.xFace, yFar + yDir * edgeY * 0.10f, box.zFace, axisNameFont());
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /**
     * Dessine la grille sur la face perpendiculaire à l'axe Z (à z = box.zFace),
     * ainsi que le libellé 'z' identifiant l'axe, placé au-delà de l'extrémité
     * de l'arête opposée au coin commun aux 3 faces visibles.
     */
    private void drawGridFaceZ(GL2 gl, BoxInfo box, int divisions) {
        float z = box.zFace;
        float stepX = (box.maxX - box.minX) / divisions;
        float stepY = (box.maxY - box.minY) / divisions;

        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i <= divisions; i++) {
            float x = box.minX + i * stepX;
            gl.glVertex3f(x, box.minY, z);
            gl.glVertex3f(x, box.maxY, z);
        }
        for (int i = 0; i <= divisions; i++) {
            float y = box.minY + i * stepY;
            gl.glVertex3f(box.minX, y, z);
            gl.glVertex3f(box.maxX, y, z);
        }
        gl.glEnd();

        float edgeZ = box.maxZ - box.minZ;
        float zFar = (box.skipZ == 0) ? box.maxZ : box.minZ;
        float zDir = (box.skipZ == 0) ? 1f : -1f;
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glColor3f(0.3f, 0.5f, 0.95f);
        drawBitmapLabel(gl, "z", box.xFace, box.yFace, zFar + zDir * edgeZ * 0.10f, axisNameFont());
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /**
     * Calcule la position de la caméra dans le même repère que les points de la
     * courbe (repère "données"), en inversant les transformations appliquées
     * dans display() (distance de la caméra, panoramique, rotations). Sert
     * uniquement à déterminer quelles faces de la boîte de repère sont les
     * plus proches de l'utilisateur, pour ne pas les dessiner.
     */
    private float[] cameraPositionInDataSpace() {
        float distance = dataRadius * distanceFactor;

        // Position de la caméra juste avant panoramique/rotations (cf. gluLookAt dans display())
        float dx = -panX;
        float dy = -panY;
        float dz = distance;

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
        float y2 = y1;

        return new float[]{
                x2 + dataCenterX,
                y2 + dataCenterY,
                z2 + dataCenterZ
        };
    }

    // --- Accesseurs utilisés par CameraController ---

    public void rotate(float deltaX, float deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        rotationX = Math.max(-89f, Math.min(89f, rotationX));
    }

    public void pan(float deltaX, float deltaY) {
        float scale = dataRadius * 0.002f;
        panX += deltaX * scale;
        panY -= deltaY * scale;
    }

    public void zoom(float amount) {
        distanceFactor += amount;
        distanceFactor = Math.max(0.5f, Math.min(20f, distanceFactor));
    }

    public void resetView() {
        rotationX = 25f;
        rotationY = -35f;
        distanceFactor = 3.0f;
        panX = 0f;
        panY = 0f;
    }

    public void toggleAxes() { showAxes = !showAxes; }
    public void togglePoints() { showPoints = !showPoints; }
    public void toggleGrid() { showGrid = !showGrid; }
    public void toggleLabel() { showLabel = !showLabel; }

    /** Affiche ou masque la légende (prise en compte à l'image suivante). */
    public void setLegendVisible(boolean visible) { showLegend = visible; }
    public boolean isLegendVisible() { return showLegend; }
    /** Bascule l'affichage de la légende (touche N). */
    public void toggleLegend() { showLegend = !showLegend; }

    /**
     * Échelles fixes : la boîte de repère et les graduations vont exactement
     * de xMin à xMax, yMin à yMax et zMin à zMax, quelles que soient les
     * valeurs des courbes (ce qui sort de la boîte est coupé). La caméra
     * reste alors à distance constante.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        fixedScales = CurveStyles.checkScales(xMin, xMax, yMin, yMax, zMin, zMax);
    }
    /** Échelles dynamiques, ajustées en continu aux valeurs des courbes (par défaut). */
    public void setDynamicScales() { fixedScales = null; }
    /** Vrai si les échelles sont fixes. */
    public boolean hasFixedScales() { return fixedScales != null; }

    /**
     * Police des graduations des échelles (les noms d'axes utilisent la même
     * famille, en gras et 70 % plus grande).
     */
    public void setScaleFont(ChartFont font) { if (font != null) scaleFont = font; }
    public ChartFont getScaleFont() { return scaleFont; }
    /** Police de la légende. */
    public void setLegendFont(ChartFont font) { if (font != null) legendFont = font; }
    public ChartFont getLegendFont() { return legendFont; }
}
