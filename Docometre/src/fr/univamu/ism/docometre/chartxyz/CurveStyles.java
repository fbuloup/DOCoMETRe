package fr.univamu.ism.docometre.chartxyz;

import java.util.List;

import com.jogamp.opengl.GL2;

/**
 * Couleurs par défaut des courbes et dessin de la légende, communs aux
 * renderers 1D, 2D et 3D.
 */
public final class CurveStyles {

    /** Palette lisible sur fond sombre, utilisée dans l'ordre d'ajout des courbes. */
    private static final float[][] PALETTE = {
            {0.35f, 0.75f, 1.00f}, // bleu
            {1.00f, 0.60f, 0.20f}, // orange
            {0.45f, 0.90f, 0.45f}, // vert
            {0.95f, 0.40f, 0.50f}, // rouge rosé
            {0.75f, 0.55f, 1.00f}, // violet
            {1.00f, 0.90f, 0.30f}, // jaune
            {0.30f, 0.90f, 0.90f}, // cyan
            {0.90f, 0.90f, 0.90f}, // gris clair
    };

    private static final int LEGEND_SWATCH_WIDTH = 36;  // pixels (assez long pour montrer le motif du trait)

    private CurveStyles() {
    }

    /**
     * Vérifie des bornes d'échelles {min1, max1, min2, max2...} : chaque
     * couple doit être fini avec min < max. Renvoie une copie.
     */
    public static double[] checkScales(double... bounds) {
        for (int i = 0; i < bounds.length; i += 2) {
            if (!(bounds[i] < bounds[i + 1]) || Double.isInfinite(bounds[i]) || Double.isInfinite(bounds[i + 1])) {
                throw new IllegalArgumentException("Échelle invalide : le minimum (" + bounds[i]
                        + ") doit être strictement inférieur au maximum (" + bounds[i + 1] + ").");
            }
        }
        return bounds.clone();
    }

    /** Couleur par défaut de la courbe de rang index. */
    public static float[] defaultColor(int index) {
        return PALETTE[index % PALETTE.length];
    }

    /** Couleur effective d'une courbe : sa couleur explicite, sinon celle de la palette selon son rang. */
    public static float[] colorOf(CurveAppearance curve, int index) {
        float[] color = curve.getColor();
        return (color != null) ? color : defaultColor(index);
    }

    /** Épaisseur effective d'une courbe : son épaisseur explicite, sinon defaultWidth. */
    public static float lineWidthOf(CurveAppearance curve, float defaultWidth) {
        float width = curve.getLineWidth();
        return (width > 0f) ? width : defaultWidth;
    }

    /** Indique si une courbe se trace en tirets (type de trait autre que SOLID). */
    public static boolean isDashed(CurveAppearance curve) {
        LineStyle style = curve.getLineStyle();
        return style != null && !style.isSolid();
    }

    /**
     * Dessine la légende (un trait de couleur suivi du nom, une ligne par
     * courbe), dans la police donnée, dont le coin supérieur gauche est en (x, yTop). La projection
     * courante doit être en pixels. Rien n'est dessiné si aucune courbe n'a
     * de nom ; les courbes sans nom apparaissent alors comme "courbe n".
     */
    public static void drawLegend(GL2 gl, GLText text, ChartFont font, List<? extends CurveAppearance> curves, float x, float yTop) {
        boolean anyName = false;
        for (CurveAppearance curve : curves) anyName |= curve.getName() != null;
        if (!anyName) return;
        float textWidth = 0;
        for (int i = 0; i < curves.size(); i++) {
            textWidth = Math.max(textWidth, text.width(labelOf(curves.get(i), i), font));
        }

        // Hauteur de ligne : celle de la police, ou l'épaisseur de trait si elle est plus grande
        float lineHeight = text.lineHeight(font) + 2;
        for (CurveAppearance curve : curves) lineHeight = Math.max(lineHeight, legendLineWidth(curve) + 6);

        // Fond semi-transparent pour rester lisible par-dessus les courbes
        float boxWidth = LEGEND_SWATCH_WIDTH + 6 + textWidth + 12;
        float boxHeight = curves.size() * lineHeight + 8;
        gl.glColor4f(0.10f, 0.11f, 0.13f, 0.75f);
        gl.glRectf(x - 6, yTop - boxHeight, x - 6 + boxWidth, yTop);

        for (int i = 0; i < curves.size(); i++) {
            float bottom = yTop - 4 - (i + 1) * lineHeight;  // bas de la ligne i
            float middle = bottom + lineHeight / 2f;
            drawEntry(gl, text, font, curves.get(i), i, x, middle);
        }
        gl.glLineWidth(1f);
    }

    /** Espace horizontal entre deux entrées d'une légende en ligne (pixels). */
    private static final int LEGEND_ENTRY_SPACING = 20;

    /**
     * Hauteur, en pixels, de la légende en ligne(s) dessinée par
     * {@link #drawLegendRows} pour une largeur disponible maxWidth ; 0 si
     * aucune courbe n'a de nom (la légende n'est alors pas dessinée).
     */
    public static float legendRowsHeight(GLText text, ChartFont font, List<? extends CurveAppearance> curves, float maxWidth) {
        List<float[]> rows = layoutRows(text, font, curves, maxWidth);
        return rows.size() * legendRowHeight(text, font, curves);
    }

    /**
     * Dessine la légende en ligne(s) : les entrées (trait + nom) se suivent
     * horizontalement, chaque ligne est centrée sur xCenter, et l'on passe à
     * la ligne suivante quand la largeur maxWidth est atteinte. yTop est le
     * haut de la première ligne. Rien n'est dessiné si aucune courbe n'a de nom.
     */
    public static void drawLegendRows(GL2 gl, GLText text, ChartFont font, List<? extends CurveAppearance> curves,
                                      float xCenter, float yTop, float maxWidth) {
        List<float[]> rows = layoutRows(text, font, curves, maxWidth);
        float rowHeight = legendRowHeight(text, font, curves);
        for (int r = 0; r < rows.size(); r++) {
            float[] row = rows.get(r); // {premier indice, indice de fin (exclu), largeur}
            float x = xCenter - row[2] / 2f;
            float middle = yTop - r * rowHeight - rowHeight / 2f;
            for (int i = (int) row[0]; i < (int) row[1]; i++) {
                drawEntry(gl, text, font, curves.get(i), i, x, middle);
                x += entryWidth(text, font, curves.get(i), i) + LEGEND_ENTRY_SPACING;
            }
        }
        gl.glLineWidth(1f);
    }

    /** Répartit les entrées en lignes de largeur au plus maxWidth : {début, fin exclue, largeur} par ligne. */
    private static List<float[]> layoutRows(GLText text, ChartFont font, List<? extends CurveAppearance> curves, float maxWidth) {
        List<float[]> rows = new java.util.ArrayList<>();
        boolean anyName = false;
        for (CurveAppearance curve : curves) anyName |= curve.getName() != null;
        if (!anyName) return rows;
        int start = 0;
        float rowWidth = 0;
        for (int i = 0; i < curves.size(); i++) {
            float w = entryWidth(text, font, curves.get(i), i);
            if (i > start && rowWidth + LEGEND_ENTRY_SPACING + w > maxWidth) {
                rows.add(new float[]{start, i, rowWidth});
                start = i;
                rowWidth = 0;
            }
            rowWidth += (i > start ? LEGEND_ENTRY_SPACING : 0) + w;
        }
        rows.add(new float[]{start, curves.size(), rowWidth});
        return rows;
    }

    private static float legendRowHeight(GLText text, ChartFont font, List<? extends CurveAppearance> curves) {
        float height = text.lineHeight(font) + 2;
        for (CurveAppearance curve : curves) height = Math.max(height, legendLineWidth(curve) + 6);
        return height;
    }

    private static float entryWidth(GLText text, ChartFont font, CurveAppearance curve, int index) {
        return LEGEND_SWATCH_WIDTH + 6 + text.width(labelOf(curve, index), font);
    }

    /** Une entrée de légende : trait au type, à la couleur et à l'épaisseur de la courbe, puis son nom. */
    private static void drawEntry(GL2 gl, GLText text, ChartFont font, CurveAppearance curve, int index, float x, float middle) {
        float[] color = colorOf(curve, index);
        gl.glColor3f(color[0], color[1], color[2]);
        // Épaisseur bornée pour rester lisible
        float width = legendLineWidth(curve);
        gl.glLineWidth(width);
        if (isDashed(curve)) {
            DashedLines.drawSegment(gl, curve.getLineStyle(), width, x, middle, x + LEGEND_SWATCH_WIDTH, middle);
        } else {
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2f(x, middle);
            gl.glVertex2f(x + LEGEND_SWATCH_WIDTH, middle);
            gl.glEnd();
        }
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        text.draw(gl, labelOf(curve, index), font, x + LEGEND_SWATCH_WIDTH + 6, middle - text.lineHeight(font) / 2f);
    }

    private static float legendLineWidth(CurveAppearance curve) {
        return Math.max(2f, Math.min(8f, lineWidthOf(curve, 3f)));
    }

    private static String labelOf(CurveAppearance curve, int index) {
        return (curve.getName() != null) ? curve.getName() : "courbe " + (index + 1);
    }
}
