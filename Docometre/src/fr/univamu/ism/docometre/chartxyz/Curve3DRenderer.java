package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;


import java.util.List;
import java.util.Locale;

public class Curve3DRenderer implements GLEventListener {

    private final CurveData curveData;
    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();

    private static final int TICK_COUNT = 5;

    // État de la caméra en orbite autour du centre de la courbe
    private float rotationX = 25f;    // inclinaison haut/bas
    private float rotationY = -35f;   // rotation autour de l'axe vertical
    private float distanceFactor = 3.0f; // distance = rayon englobant * ce facteur
    private float panX = 0f, panY = 0f;

    private boolean showAxes = false;
    private boolean showPoints = false;
    private boolean showGrid = true;
    private boolean showLabel= true;

    public Curve3DRenderer(CurveData curveData) {
        this.curveData = curveData;
    }

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
        // Rien à libérer explicitement ici (GLUT ne détient pas de ressources GL).
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        float distance = curveData.getBoundingRadius() * distanceFactor;
        glu.gluLookAt(0, 0, distance,
                      0, 0, 0,
                      0, 1, 0);

        gl.glTranslatef(panX, panY, 0f);
        gl.glRotatef(rotationX, 1f, 0f, 0f);
        gl.glRotatef(rotationY, 0f, 1f, 0f);
        gl.glTranslatef(-curveData.getCenterX(), -curveData.getCenterY(), -curveData.getCenterZ());

        if (showGrid) drawCubeGrid(gl); 
        if (showGrid && showLabel) drawAxisLabels(gl);
        if (showAxes) drawAxes(gl);
        drawCurve(gl);
        if (showPoints) drawPoints(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height <= 0) height = 1;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) width / height;
        float farPlane = Math.max(curveData.getBoundingRadius() * 20f, 100f);
        glu.gluPerspective(45.0, aspect, 0.1, farPlane);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private void drawCurve(GL2 gl) {
        List<Point3D> points = curveData.getPoints();
        gl.glLineWidth(2.5f);
        gl.glBegin(GL2.GL_LINE_STRIP);
        int n = points.size();
        for (int i = 0; i < n; i++) {
            float t = (n <= 1) ? 0f : (float) i / (n - 1);
            // Dégradé du bleu vers l'orange le long de la courbe
            gl.glColor3f(0.2f + 0.8f * t, 0.55f + 0.25f * (1 - t), 1.0f - 0.8f * t);
            Point3D p = points.get(i);
            gl.glVertex3f(p.x, p.y, p.z);
        }
        gl.glEnd();
    }

    private void drawPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (Point3D p : curveData.getPoints()) {
            gl.glVertex3f(p.x, p.y, p.z);
        }
        gl.glEnd();
    }

    private void drawAxes(GL2 gl) {
        float len = curveData.getBoundingRadius() * 1.3f;
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
        float marginX = Math.max((curveData.getMaxX() - curveData.getMinX()) * 0.08f, curveData.getBoundingRadius() * 0.02f);
        float marginY = Math.max((curveData.getMaxY() - curveData.getMinY()) * 0.08f, curveData.getBoundingRadius() * 0.02f);
        float marginZ = Math.max((curveData.getMaxZ() - curveData.getMinZ()) * 0.08f, curveData.getBoundingRadius() * 0.02f);

        float minX = curveData.getMinX() - marginX, maxX = curveData.getMaxX() + marginX;
        float minY = curveData.getMinY() - marginY, maxY = curveData.getMaxY() + marginY;
        float minZ = curveData.getMinZ() - marginZ, maxZ = curveData.getMaxZ() + marginZ;

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
     * Utilise une police bitmap GLUT (glRasterPos3f + glutBitmapString),
     * qui ne dépend pas d'AWT — contrairement à TextRenderer, dont le mélange
     * avec le pipeline natif SWT peut produire un rendu incorrect (texture de
     * glyphe non initialisée) sur certaines configurations.
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

    /** Positionne le curseur de rendu (raster) à (x, y, z) et y dessine une chaîne en police bitmap HELVETICA_12. */
    private void drawBitmapLabel(GL2 gl, String text, float x, float y, float z) {
        drawBitmapLabel(gl, text, x, y, z, GLUT.BITMAP_HELVETICA_18);
    }

    /** Positionne le curseur de rendu (raster) à (x, y, z) et y dessine une chaîne dans la police bitmap donnée. */
    private void drawBitmapLabel(GL2 gl, String text, float x, float y, float z, int bitmapFont) {
        gl.glRasterPos3f(x, y, z);
        glut.glutBitmapString(bitmapFont, text);
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
        drawBitmapLabel(gl, "x", xFar + xDir * edgeX * 0.10f, box.yFace, box.zFace, GLUT.BITMAP_TIMES_ROMAN_24);
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
        drawBitmapLabel(gl, "y", box.xFace, yFar + yDir * edgeY * 0.10f, box.zFace, GLUT.BITMAP_TIMES_ROMAN_24);
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
        drawBitmapLabel(gl, "z", box.xFace, box.yFace, zFar + zDir * edgeZ * 0.10f, GLUT.BITMAP_TIMES_ROMAN_24);
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
        float distance = curveData.getBoundingRadius() * distanceFactor;

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
                x2 + curveData.getCenterX(),
                y2 + curveData.getCenterY(),
                z2 + curveData.getCenterZ()
        };
    }

    // --- Accesseurs utilisés par CameraController ---

    public void rotate(float deltaX, float deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        rotationX = Math.max(-89f, Math.min(89f, rotationX));
    }

    public void pan(float deltaX, float deltaY) {
        float scale = curveData.getBoundingRadius() * 0.002f;
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
}
