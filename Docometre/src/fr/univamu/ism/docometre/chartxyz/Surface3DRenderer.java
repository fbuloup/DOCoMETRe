package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

import fr.univamu.ism.docometre.chartxyz.windows.Chart3DCameraController;

import java.util.Locale;

/**
 * Gère le rendu OpenGL de la surface 3D : axes, grille de repère au sol,
 * surface triangulée avec ombrage lisse (normales par sommet) et couleur
 * dégradée selon l'altitude (y), bascule solide / filaire / les deux.
 * Expose les méthodes utilisées par {@link Chart3DCameraController} pour piloter
 * la caméra orbitale (rotation, panoramique, zoom).
 */
public class Surface3DRenderer implements GLEventListener {

    private final SurfaceData surfaceData;
    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();

    private static final int TICK_COUNT = 5; // nombre d'intervalles -> TICK_COUNT + 1 étiquettes par axe

    // État de la caméra en orbite autour du centre de la surface
    private float rotationX = 30f;
    private float rotationY = -40f;
    private float distanceFactor = 3.0f;
    private float panX = 0f, panY = 0f;

    private boolean showAxes = true;
    private boolean showGrid = true;
    private boolean showSolid = true;
    private boolean showWireframe = false;
    private boolean showLabel = false;
    private boolean lightingEnabled = true;

    public Surface3DRenderer(SurfaceData surfaceData) {
        this.surfaceData = surfaceData;
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
        gl.glShadeModel(GL2.GL_SMOOTH);

        // Éclairage simple : une lumière directionnelle + un peu d'ambiant global
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[]{0.9f, 0.9f, 0.9f, 1f}, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.3f, 0.3f, 0.3f, 1f}, 0);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[]{0.25f, 0.25f, 0.27f, 1f}, 0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[]{0.2f, 0.2f, 0.2f, 1f}, 0);
        gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 16f);

        if (lightingEnabled) gl.glEnable(GL2.GL_LIGHTING);
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

        float distance = surfaceData.getBoundingRadius() * distanceFactor;
        glu.gluLookAt(0, 0, distance,
                      0, 0, 0,
                      0, 1, 0);

        // Lumière fixée dans l'espace caméra : elle "suit" la vue, toujours bien éclairée
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{0.4f, 0.6f, 1f, 0f}, 0);

        gl.glTranslatef(panX, panY, 0f);
        gl.glRotatef(rotationX, 1f, 0f, 0f);
        gl.glRotatef(rotationY, 0f, 1f, 0f);
        gl.glTranslatef(-surfaceData.getCenterX(), -surfaceData.getCenterY(), -surfaceData.getCenterZ());

        if (showGrid) drawCubeGrid(gl); 
        if (showGrid && showLabel) drawAxisLabels(gl);
        if (showAxes) drawAxes(gl);

        if (lightingEnabled) gl.glEnable(GL2.GL_LIGHTING); else gl.glDisable(GL2.GL_LIGHTING);

        if (showSolid) {
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            drawSurface(gl);
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
        }

        if (showWireframe) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor4f(0f, 0f, 0f, 0.35f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawSurfaceFlatColor(gl);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height <= 0) height = 1;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) width / height;
        float farPlane = Math.max(surfaceData.getBoundingRadius() * 20f, 100f);
        glu.gluPerspective(45.0, aspect, 0.1, farPlane);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    /** Dessine la surface pleine, ombrée, avec couleur dégradée selon l'altitude (y). */
    private void drawSurface(GL2 gl) {
        int rows = surfaceData.getRows();
        int cols = surfaceData.getCols();
        float minY = surfaceData.getMinY();
        float maxY = surfaceData.getMaxY();
        float range = Math.max(maxY - minY, 1e-6f);

        gl.glBegin(GL2.GL_TRIANGLES);
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                emitVertex(gl, r, c, minY, range);
                emitVertex(gl, r + 1, c, minY, range);
                emitVertex(gl, r + 1, c + 1, minY, range);

                emitVertex(gl, r, c, minY, range);
                emitVertex(gl, r + 1, c + 1, minY, range);
                emitVertex(gl, r, c + 1, minY, range);
            }
        }
        gl.glEnd();
    }

    private void emitVertex(GL2 gl, int r, int c, float minY, float range) {
        Point3D p = surfaceData.point(r, c);
        float[] n = surfaceData.normal(r, c);
        float t = (p.y - minY) / range;
        // Dégradé du bleu (bas) au orange/rouge (haut)
        gl.glColor3f(0.15f + 0.75f * t, 0.4f + 0.25f * (1 - t), 0.95f - 0.75f * t);
        gl.glNormal3f(n[0], n[1], n[2]);
        gl.glVertex3f(p.x, p.y, p.z);
    }

    /** Même géométrie que drawSurface, mais sans dégradé (utilisé pour l'overlay filaire). */
    private void drawSurfaceFlatColor(GL2 gl) {
        int rows = surfaceData.getRows();
        int cols = surfaceData.getCols();
        gl.glBegin(GL2.GL_TRIANGLES);
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                Point3D p00 = surfaceData.point(r, c);
                Point3D p10 = surfaceData.point(r + 1, c);
                Point3D p11 = surfaceData.point(r + 1, c + 1);
                Point3D p01 = surfaceData.point(r, c + 1);

                gl.glVertex3f(p00.x, p00.y, p00.z);
                gl.glVertex3f(p10.x, p10.y, p10.z);
                gl.glVertex3f(p11.x, p11.y, p11.z);

                gl.glVertex3f(p00.x, p00.y, p00.z);
                gl.glVertex3f(p11.x, p11.y, p11.z);
                gl.glVertex3f(p01.x, p01.y, p01.z);
            }
        }
        gl.glEnd();
    }

    private void drawAxes(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        float len = surfaceData.getBoundingRadius() * 1.3f;
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glVertex3f(-len, 0, 0); gl.glVertex3f(len, 0, 0);
        gl.glColor3f(0.2f, 0.9f, 0.2f);
        gl.glVertex3f(0, -len, 0); gl.glVertex3f(0, len, 0);
        gl.glColor3f(0.3f, 0.5f, 0.95f);
        gl.glVertex3f(0, 0, -len); gl.glVertex3f(0, 0, len);
        gl.glEnd();
    }

    /**
     * Dessine une grille de repère en forme de boîte : des lignes de grille
     * sont tracées sur les faces d'une boîte qui épouse précisément la boîte
     * englobante réelle de la surface (et non un cube uniforme). Pour ne pas
     * masquer la surface, seules les 3 faces les plus ÉLOIGNÉES de la caméra
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
        // Léger espace entre la surface et la boîte, proportionnel à chaque dimension
        float marginX = Math.max((surfaceData.getMaxX() - surfaceData.getMinX()) * 0.08f, surfaceData.getBoundingRadius() * 0.02f);
        float marginY = Math.max((surfaceData.getMaxY() - surfaceData.getMinY()) * 0.08f, surfaceData.getBoundingRadius() * 0.02f);
        float marginZ = Math.max((surfaceData.getMaxZ() - surfaceData.getMinZ()) * 0.08f, surfaceData.getBoundingRadius() * 0.02f);

        float minX = surfaceData.getMinX() - marginX, maxX = surfaceData.getMaxX() + marginX;
        float minY = surfaceData.getMinY() - marginY, maxY = surfaceData.getMaxY() + marginY;
        float minZ = surfaceData.getMinZ() - marginZ, maxZ = surfaceData.getMaxZ() + marginZ;

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
     * englobante réelle de la surface (et non un cube uniforme). Pour ne pas
     * masquer la surface, seules les 3 faces les plus ÉLOIGNÉES de la caméra
     * sont dessinées (une par axe) ; les 3 faces les plus proches sont omises.
     * Chaque face dessine aussi son libellé de nom d'axe ('x', 'y' ou 'z').
     */
    private void drawCubeGrid(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);

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
        gl.glDisable(GL2.GL_LIGHTING);
        // Le test de profondeur est désactivé le temps de dessiner les étiquettes,
        // pour éviter tout scintillement lié à la coïncidence exacte avec les
        // lignes de la grille (même position que certains sommets de la boîte).
        gl.glDisable(GL2.GL_DEPTH_TEST);

        BoxInfo box = computeBoxInfo();
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
        drawBitmapLabel(gl, text, x, y, z, GLUT.BITMAP_HELVETICA_12);
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
        drawBitmapLabel(gl, "x", xFar + xDir * edgeX * 0.10f, box.yFace, box.zFace, GLUT.BITMAP_HELVETICA_18);
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
        drawBitmapLabel(gl, "y", box.xFace, yFar + yDir * edgeY * 0.10f, box.zFace, GLUT.BITMAP_HELVETICA_18);
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
        drawBitmapLabel(gl, "z", box.xFace, box.yFace, zFar + zDir * edgeZ * 0.10f, GLUT.BITMAP_HELVETICA_18);
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    /**
     * Calcule la position de la caméra dans le même repère que les points de
     * la surface (repère "données"), en inversant les transformations
     * appliquées dans display() (distance de la caméra, panoramique,
     * rotations). Sert uniquement à déterminer quelles faces de la boîte de
     * repère sont les plus proches de l'utilisateur, pour ne pas les dessiner.
     */
    private float[] cameraPositionInDataSpace() {
        float distance = surfaceData.getBoundingRadius() * distanceFactor;

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
                x2 + surfaceData.getCenterX(),
                y2 + surfaceData.getCenterY(),
                z2 + surfaceData.getCenterZ()
        };
    }

    // --- Accesseurs utilisés par CameraController ---

    public void rotate(float deltaX, float deltaY) {
        rotationY += deltaX;
        rotationX += deltaY;
        rotationX = Math.max(-89f, Math.min(89f, rotationX));
    }

    public void pan(float deltaX, float deltaY) {
        float scale = surfaceData.getBoundingRadius() * 0.002f;
        panX += deltaX * scale;
        panY -= deltaY * scale;
    }

    public void zoom(float amount) {
        distanceFactor += amount;
        distanceFactor = Math.max(0.5f, Math.min(20f, distanceFactor));
    }

    public void resetView() {
        rotationX = 30f;
        rotationY = -40f;
        distanceFactor = 3.0f;
        panX = 0f;
        panY = 0f;
    }

    public void toggleAxes() { showAxes = !showAxes; }
    public void toggleGrid() { showGrid = !showGrid; }
    public void toggleLabel() { showLabel = !showLabel; }
    public void toggleSolid() { showSolid = !showSolid; }
    public void toggleWireframe() { showWireframe = !showWireframe; }
    public void toggleLighting() { lightingEnabled = !lightingEnabled; }
}
