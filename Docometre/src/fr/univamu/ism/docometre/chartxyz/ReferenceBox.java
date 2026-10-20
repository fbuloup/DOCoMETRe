package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;

/**
 * Boîte de repère des scènes 3D ({@link Chart3DRenderer}, {@link Surface3DRenderer}) :
 * des lignes de grille sont tracées sur les faces d'une boîte qui épouse
 * précisément la boîte englobante réelle des données (et non un cube
 * uniforme). Pour ne pas masquer les données, seules les 3 faces les plus
 * ÉLOIGNÉES de la caméra sont dessinées (une par axe) ; les 3 faces les plus
 * proches sont omises.
 *
 * Les graduations (valeurs numériques) des 3 axes sont placées le long des
 * arêtes communes aux faces visibles : c'est là qu'elles restent lisibles
 * quel que soit l'angle de vue. Le dessin du texte est délégué à un
 * {@link LabelSink}, propre à chaque renderer.
 */
final class ReferenceBox {

    /** Nombre d'intervalles entre graduations : TICK_COUNT + 1 étiquettes par axe. */
    static final int TICK_COUNT = 5;
    /** Nombre d'intervalles de la grille sur chaque face. */
    private static final int DIVISIONS = 10;

    private static final String[] AXIS_NAMES = {"x", "y", "z"};
    private static final float[][] AXIS_COLORS = {
            {0.9f, 0.2f, 0.2f},  // x : rouge
            {0.2f, 0.9f, 0.2f},  // y : vert
            {0.3f, 0.5f, 0.95f}, // z : bleu
    };

    /** Dessin d'une étiquette au point (x, y, z) de la scène, dans la couleur OpenGL courante. */
    interface LabelSink {
        /** @param axisName true pour un nom d'axe ('x', 'y', 'z'), false pour une graduation */
        void label(GL2 gl, String text, float x, float y, float z, boolean axisName);
    }

    // Par axe (0 = x, 1 = y, 2 = z)
    private final float[] min = new float[3], max = new float[3];
    private final float[] face = new float[3]; // coordonnée de la face visible perpendiculaire à l'axe
    private final int[] skip = new int[3];     // graduation coïncidant avec le coin commun (à omettre)

    /**
     * @param data       boîte englobante des données
     * @param withMargin léger espace entre les données et la boîte (sans marge,
     *                   la boîte correspond exactement aux bornes : échelles fixes)
     * @param camera     position de la caméra dans le repère des données
     */
    ReferenceBox(BoundingBox data, boolean withMargin, float[] camera) {
        float[] dataMin = {data.minX, data.minY, data.minZ};
        float[] dataMax = {data.maxX, data.maxY, data.maxZ};
        for (int a = 0; a < 3; a++) {
            // Espace proportionnel à chaque dimension
            float margin = withMargin ? Math.max((dataMax[a] - dataMin[a]) * 0.08f, data.radius * 0.02f) : 0f;
            min[a] = dataMin[a] - margin;
            max[a] = dataMax[a] + margin;
            face[a] = (camera[a] > (min[a] + max[a]) / 2f) ? min[a] : max[a];
            // Sur chaque axe, la graduation qui coïncide avec le coin commun aux 3
            // faces visibles est omise : les 3 séries s'y superposeraient sinon.
            skip[a] = (face[a] == min[a]) ? 0 : TICK_COUNT;
        }
    }

    /**
     * Dessine la grille des 3 faces visibles et, pour chacune, le nom de son
     * axe, placé au-delà de l'extrémité de l'arête opposée au coin commun aux
     * 3 faces visibles.
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

    /** Grille sur la face perpendiculaire à l'axe a (à la coordonnée face[a]). */
    private void drawFace(GL2 gl, int a) {
        int u = (a == 0) ? 1 : 0, v = (a == 2) ? 1 : 2; // les deux autres axes, dans l'ordre
        float[] p0 = new float[3], p1 = new float[3];
        p0[a] = p1[a] = face[a];

        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (int pass = 0; pass < 2; pass++) {
            int step = (pass == 0) ? u : v;  // axe le long duquel les lignes sont réparties
            int along = (pass == 0) ? v : u; // axe le long duquel elles s'étendent
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
     * Graduations des 3 axes, le long des arêtes communes aux faces visibles.
     * Le test de profondeur est désactivé le temps de les dessiner, pour
     * éviter tout scintillement lié à la coïncidence exacte avec les lignes
     * de la grille.
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

    /** Axes x (rouge), y (vert) et z (bleu) passant par l'origine, de longueur 2 * 1,3 * radius. */
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
