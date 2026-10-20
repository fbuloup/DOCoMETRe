package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Base des renderers plans (1D et 2D). L'écran est découpé en une zone de
 * tracé (projection orthographique en unités des données, avec découpage
 * par scissor) entourée de marges en pixels où sont écrites les
 * graduations. Les graduations tombent sur des valeurs "rondes" (1, 2 ou
 * 5 x 10^n). La vue s'ajuste automatiquement aux données ; zoom et
 * panoramique s'appliquent par-dessus cet ajustement automatique.
 *
 * Chaque image suit le même déroulement ({@link #display}) ; les
 * sous-classes fournissent la mise à jour des données, la fenêtre visible
 * et le tracé des courbes.
 *
 * @param <C> type des courbes affichées
 */
public abstract class PlotRenderer<C extends CurveData<C>> extends CurveChartRenderer<C> {

    private static final int TARGET_TICK_COUNT = 6;          // nombre approximatif de graduations par axe
    protected static final float DATA_MARGIN = 0.05f;        // espace entre courbe et cadre (fraction de l'étendue)

    private volatile LegendPosition legendPosition = LegendPosition.INSIDE;
    private float legendBelowHeight = 0; // hauteur de la légende sous l'axe (pixels), 0 si absente

    // Vue : zoom (1 = ajustement automatique) et panoramique (en fraction de l'étendue non zoomée)
    protected volatile float zoom = 1f;
    protected volatile float panX = 0f, panY = 0f;

    // Marges autour de la zone de tracé (pixels), adaptées à la taille des polices
    protected int marginLeft = 75, marginRight = 25, marginTop = 25, marginBottom = 35;
    private float yLabelWidth = 0; // largeur de la plus longue graduation verticale (image précédente)
    private float xLabelWidth = 0; // largeur de la plus longue graduation horizontale (image précédente)

    // Taille de la zone de tracé (pixels)
    protected int plotWidth = 1, plotHeight = 1;

    // Fenêtre visible, en unités des données (recalculée à chaque image)
    protected double viewMinX, viewMaxX, viewMinY, viewMaxY;

    @SafeVarargs
    protected PlotRenderer(float pointSize, C... curves) {
        super(pointSize, curves);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        drawable.getGL().getGL2().glDisable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        beginFrame();
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        updateData();
        computeMargins();
        plotWidth = Math.max(1, width - marginLeft - marginRight);
        plotHeight = Math.max(1, height - marginBottom - marginTop);
        computeView();

        double stepX = AxisTicks.niceStep((viewMaxX - viewMinX) / TARGET_TICK_COUNT);
        double stepY = AxisTicks.niceStep((viewMaxY - viewMinY) / TARGET_TICK_COUNT);
        double origin = xOrigin();

        // --- Zone de tracé : x relatif à origin, y en unités des données ---
        gl.glViewport(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glEnable(GL2.GL_SCISSOR_TEST);
        gl.glScissor(marginLeft, marginBottom, plotWidth, plotHeight);
        setProjection(gl, viewMinX - origin, viewMaxX - origin, viewMinY, viewMaxY);

        if (showGrid) drawGrid(gl, stepX, stepY, origin);
        if (showAxes) drawAxes(gl, origin);
        drawCurves(gl, origin);

        gl.glDisable(GL2.GL_SCISSOR_TEST);

        // --- Habillage (cadre, graduations), en pixels sur toute la surface ---
        gl.glViewport(0, 0, width, height);
        setProjection(gl, 0, width, 0, height);

        drawFrame(gl);
        if (showLabel) drawLabels(gl, stepX, stepY);
        drawLegend(gl);
        drawFps(gl, marginLeft + plotWidth - 6, marginBottom + 4);
        float valuesHeight = drawCurrentValues(gl, marginLeft + plotWidth - 6, marginBottom + plotHeight - 6);
        drawStatus(gl, valuesHeight);
    }

    private static void setProjection(GL2 gl, double left, double right, double bottom, double top) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(left, right, bottom, top, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    // ------------------------------------------------------------------
    // Étapes fournies par les sous-classes

    /** Fige les données de l'image et calcule leurs bornes. */
    protected abstract void updateData();

    /** Calcule la fenêtre visible (viewMinX... viewMaxY) à partir des bornes, du zoom et du panoramique. */
    protected abstract void computeView();

    /**
     * Abscisse des données placée à x = 0 dans la projection de la zone de
     * tracé (0 par défaut) : permet de garder la précision des float quand
     * les abscisses sont grandes (temps).
     */
    protected double xOrigin() { return 0; }

    /** Dessine les courbes (et leurs points s'ils sont affichés), abscisses relatives à origin. */
    protected abstract void drawCurves(GL2 gl, double origin);

    /** Droite x = 0 (axe vertical) dessinée avec la droite y = 0 (touche A) ? */
    protected boolean hasVerticalAxis() { return false; }

    /**
     * Indication d'état éventuelle, en haut à droite de la zone de tracé
     * (coordonnées en pixels), sous les valeurs courantes.
     *
     * @param valuesHeight hauteur occupée par les valeurs courantes (0 si elles ne sont pas affichées)
     */
    protected void drawStatus(GL2 gl, float valuesHeight) {
    }

    /** Demi-étendue (avec marge) d'un axe ; gère le cas dégénéré où toutes les valeurs sont égales. */
    protected static float halfExtent(float min, float max) {
        float range = max - min;
        if (range > 0f) return range / 2f * (1f + 2f * DATA_MARGIN);
        float center = Math.abs((min + max) / 2f);
        return (center > 0f) ? center * 0.1f : 1f;
    }

    // ------------------------------------------------------------------
    // Habillage

    /**
     * Calcule les marges autour de la zone de tracé à partir des polices : à
     * gauche les graduations verticales (largeur mesurée à l'image précédente),
     * en bas les graduations horizontales et, le cas échéant, la légende, en
     * haut et à droite juste de quoi ne pas couper les graduations placées aux
     * bords. La marge de gauche ne diminue que nettement, pour éviter que la
     * zone de tracé ne tremble quand la longueur des graduations varie.
     */
    private void computeMargins() {
        legendBelowHeight = (showLegend && legendPosition == LegendPosition.BELOW)
                ? CurveStyles.legendRowsHeight(text, legendFont, frameCurves, width - 20) : 0;
        float legendSpace = (legendBelowHeight > 0) ? legendBelowHeight + 6 : 0;
        if (!showLabel) {
            marginLeft = marginRight = marginTop = 15;
            marginBottom = (int) Math.ceil(15 + legendSpace);
            return;
        }
        float tickHeight = text.lineHeight(scaleFont);
        int neededLeft = (int) Math.ceil(Math.max(yLabelWidth, text.width("-0.00", scaleFont)) + 12);
        if (neededLeft > marginLeft || neededLeft < marginLeft * 0.8f) marginLeft = neededLeft;
        marginBottom = (int) Math.ceil(tickHeight + 10 + legendSpace);
        marginTop = (int) Math.max(10, Math.ceil(tickHeight / 2f + 4));
        marginRight = (int) Math.max(10, Math.ceil(xLabelWidth / 2f + 4));
    }

    /** Légende : dans le graphe en haut à gauche, ou en ligne(s) sous l'axe des abscisses. */
    private void drawLegend(GL2 gl) {
        if (!showLegend) return;
        if (legendPosition == LegendPosition.BELOW) {
            // Tout en bas, sous les graduations de l'axe des abscisses, centrée sous la zone de tracé
            CurveStyles.drawLegendRows(gl, text, legendFont, frameCurves, marginLeft + plotWidth / 2f,
                                       legendBelowHeight + 4, width - 20);
        } else {
            CurveStyles.drawLegend(gl, text, legendFont, frameCurves, marginLeft + 12, marginBottom + plotHeight - 6);
        }
    }

    /** Grille alignée sur les graduations (abscisses relatives à origin). */
    private void drawGrid(GL2 gl, double stepX, double stepY, double origin) {
        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (double x : AxisTicks.ticks(viewMinX, viewMaxX, stepX)) {
            gl.glVertex2d(x - origin, viewMinY);
            gl.glVertex2d(x - origin, viewMaxY);
        }
        for (double y : AxisTicks.ticks(viewMinY, viewMaxY, stepY)) {
            gl.glVertex2d(viewMinX - origin, y);
            gl.glVertex2d(viewMaxX - origin, y);
        }
        gl.glEnd();
    }

    /** Droites y = 0 (rouge) et, le cas échéant, x = 0 (verte), si elles sont visibles. */
    private void drawAxes(GL2 gl, double origin) {
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glVertex2d(viewMinX - origin, 0);
        gl.glVertex2d(viewMaxX - origin, 0);
        if (hasVerticalAxis()) {
            gl.glColor3f(0.2f, 0.9f, 0.2f);
            gl.glVertex2d(-origin, viewMinY);
            gl.glVertex2d(-origin, viewMaxY);
        }
        gl.glEnd();
    }

    /** Cadre autour de la zone de tracé (coordonnées en pixels). */
    private void drawFrame(GL2 gl) {
        float left = marginLeft, bottom = marginBottom;
        float right = marginLeft + plotWidth, top = marginBottom + plotHeight;
        gl.glColor4f(0.6f, 0.6f, 0.65f, 0.8f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(left, bottom);
        gl.glVertex2f(right, bottom);
        gl.glVertex2f(right, top);
        gl.glVertex2f(left, top);
        gl.glEnd();
    }

    /**
     * Graduations (sous l'axe horizontal et à gauche de l'axe vertical),
     * dans la police des échelles (coordonnées en pixels).
     */
    private void drawLabels(GL2 gl, double stepX, double stepY) {
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        ChartFont font = scaleFont;
        float lineHeight = text.lineHeight(font);

        float widestX = 0;
        for (double x : AxisTicks.ticks(viewMinX, viewMaxX, stepX)) {
            String label = AxisTicks.format(x, stepX);
            float px = marginLeft + (float) ((x - viewMinX) / (viewMaxX - viewMinX)) * plotWidth;
            float labelWidth = text.width(label, font);
            widestX = Math.max(widestX, labelWidth);
            text.draw(gl, label, font, px - labelWidth / 2f, marginBottom - 4 - lineHeight);
        }
        xLabelWidth = widestX;
        float widestY = 0;
        for (double y : AxisTicks.ticks(viewMinY, viewMaxY, stepY)) {
            String label = AxisTicks.format(y, stepY);
            float labelWidth = text.width(label, font);
            widestY = Math.max(widestY, labelWidth);
            float py = marginBottom + (float) ((y - viewMinY) / (viewMaxY - viewMinY)) * plotHeight;
            text.draw(gl, label, font, marginLeft - 6 - labelWidth, py - lineHeight / 2f);
        }
        yLabelWidth = widestY; // sert au calcul de la marge de gauche à l'image suivante
    }

    // ------------------------------------------------------------------
    // Navigation

    /** Glisser (bouton gauche ou droit) : panoramique ; le contenu suit la souris. */
    @Override
    public void drag(int button, int dx, int dy, int width, int height, InputScale scale) {
        pan((float) dx / Math.max(1, width), (float) dy / Math.max(1, height));
    }

    /** Molette : zoom d'un cran. */
    @Override
    public void wheel(float amount, InputScale scale) {
        zoom(Math.signum(amount));
    }

    /**
     * Panoramique. dx et dy sont des déplacements exprimés en fraction de la
     * taille du composant (ce qui rend le calcul indépendant de la densité de
     * pixels de l'écran). Le contenu suit la souris.
     */
    public void pan(float dx, float dy) {
        panX -= dx * width / plotWidth / zoom;
        panY += dy * height / plotHeight / zoom;
    }

    /** Zoom multiplicatif : steps > 0 rapproche, steps < 0 éloigne. */
    public void zoom(float steps) {
        zoom *= (float) Math.pow(1.15, steps);
        zoom = Math.max(1e-3f, Math.min(1e4f, zoom));
    }

    @Override
    public void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    /** Position de la légende : dans le graphe (INSIDE, par défaut) ou sous l'axe des abscisses (BELOW). */
    public void setLegendPosition(LegendPosition position) { if (position != null) legendPosition = position; }
    public LegendPosition getLegendPosition() { return legendPosition; }
}
