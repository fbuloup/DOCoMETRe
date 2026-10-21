package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;

/**
 * Rendu OpenGL d'une ou plusieurs courbes 2D temps réel ({@link Curve2DData}).
 *
 * La vue s'ajuste automatiquement aux données de la fenêtre temporelle (ou
 * aux échelles fixes) ; zoom et panoramique s'appliquent par-dessus, ainsi
 * que le zoom par rectangle (voir {@link PlotRenderer}).
 * Curseur (touche C, voir {@link PlotRenderer}) : il désigne le point de la
 * trajectoire le plus proche de la souris.
 * Commande clavier propre au 2D : E, même échelle sur x et y.
 */
public class Chart2DRenderer extends PlotRenderer<Curve2DData> {

    private static final float DEFAULT_LINE_WIDTH = 2f; // épaisseur des courbes sans épaisseur explicite (pixels)

    // Boîte englobante de l'ensemble des courbes, recalculée à chaque image
    private float dataMinX = -1f, dataMaxX = 1f, dataMinY = -1f, dataMaxY = 1f;

    private volatile boolean equalAspect = false; // même échelle sur x et y (utile pour les trajectoires)

    /**
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     */
    public Chart2DRenderer(double historySize, Curve2DData... curves) {
        super(5f, curves);
        setWindowTimeWidth(historySize);
    }

    /** Graphe dont chaque courbe garde sa propre durée d'historique (données statiques, par exemple). */
    public Chart2DRenderer(Curve2DData... curves) {
        super(5f, curves);
    }

    @Override
    protected void updateData() {
        snapshotCurves();
        computeBounds();
    }

    /** Boîte englobante de l'ensemble des courbes non vides (de -1 à 1 s'il n'y en a aucune). */
    private void computeBounds() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean empty = true;
        for (Curve2DData curve : frameCurves) {
            if (curve.getSnapshotCount() == 0) continue;
            empty = false;
            minX = Math.min(minX, curve.getMinX()); maxX = Math.max(maxX, curve.getMaxX());
            minY = Math.min(minY, curve.getMinY()); maxY = Math.max(maxY, curve.getMaxY());
        }
        if (empty) {
            minX = minY = -1f;
            maxX = maxY = 1f;
        }
        dataMinX = minX; dataMaxX = maxX;
        dataMinY = minY; dataMaxY = maxY;
    }

    /**
     * Fenêtre visible : la fenêtre de zoom si elle existe, sinon les échelles
     * fixes ou les données (avec marge), zoom et panoramique compris ; puis,
     * si "même échelle" est active, élargissement de l'axe le plus serré.
     */
    @Override
    protected void computeView() {
        double halfX, halfY, centerX, centerY;
        double[] window = zoomWindow();
        double[] fixed = getFixedScaleBounds();
        if (window != null) { // fenêtre de zoom : exactement le rectangle choisi
            halfX = (window[1] - window[0]) / 2;
            halfY = (window[3] - window[2]) / 2;
            centerX = (window[0] + window[1]) / 2;
            centerY = (window[2] + window[3]) / 2;
        } else {
            double baseCenterX, baseCenterY;
            if (fixed != null) { // échelles fixes : exactement les bornes fournies, sans marge
                halfX = (fixed[1] - fixed[0]) / 2;
                halfY = (fixed[3] - fixed[2]) / 2;
                baseCenterX = (fixed[0] + fixed[1]) / 2;
                baseCenterY = (fixed[2] + fixed[3]) / 2;
            } else {
                halfX = halfExtent(dataMinX, dataMaxX);
                halfY = halfExtent(dataMinY, dataMaxY);
                baseCenterX = (dataMinX + dataMaxX) / 2f;
                baseCenterY = (dataMinY + dataMaxY) / 2f;
            }
            centerX = baseCenterX + panX * 2 * halfX;
            centerY = baseCenterY + panY * 2 * halfY;
            halfX /= zoom;
            halfY /= zoom;
        }

        if (equalAspect) {
            // Même nombre d'unités par pixel sur les deux axes : on élargit l'axe le plus "serré"
            double unitsPerPixel = Math.max(2 * halfX / plotWidth, 2 * halfY / plotHeight);
            halfX = unitsPerPixel * plotWidth / 2;
            halfY = unitsPerPixel * plotHeight / 2;
        }

        viewMinX = centerX - halfX; viewMaxX = centerX + halfX;
        viewMinY = centerY - halfY; viewMaxY = centerY + halfY;
    }

    @Override
    protected boolean hasVerticalAxis() { return true; }

    /** Point i de la trajectoire : ses coordonnées (x, y). */
    @Override
    protected boolean cursorPoint(Curve2DData curve, int i, double[] xy) {
        float[] c = curve.getSnapshotCoordinates();
        xy[0] = c[2 * i];
        xy[1] = c[2 * i + 1];
        return true;
    }

    @Override
    protected void drawCurves(GL2 gl, double origin) {
        double scaleX = plotWidth / (viewMaxX - viewMinX);   // pixels par unité
        double scaleY = plotHeight / (viewMaxY - viewMinY);
        double left = viewMinX, bottom = viewMinY;
        drawTrajectories(gl, DEFAULT_LINE_WIDTH, (x, y, z, pixel) -> {
            pixel[0] = (x - left) * scaleX;
            pixel[1] = (y - bottom) * scaleY;
            return true;
        }, scaleX, scaleY);
        if (showPoints) drawTrajectoryPoints(gl);
    }

    @Override
    public void keyPressed(char key) {
        if (key == 'e') toggleEqualAspect();
        else super.keyPressed(key);
    }

    /**
     * Échelles fixes : les axes vont exactement de xMin à xMax et de yMin à
     * yMax, quelles que soient les valeurs des courbes (ce qui dépasse est
     * coupé par le cadre). Zoom, panoramique et "même échelle" (touche E)
     * restent possibles ; R revient à ces échelles.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        setFixedScaleBounds(xMin, xMax, yMin, yMax);
    }

    public void toggleEqualAspect() { equalAspect = !equalAspect; }
}
