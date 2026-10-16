package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Rendu OpenGL d'une ou plusieurs courbes 2D temps réel ({@link Curve2DData}).
 *
 * L'écran est découpé en une zone de tracé (projection orthographique en
 * unités des données, avec découpage par scissor) entourée de marges en
 * pixels où sont écrites les graduations. Les graduations tombent sur des
 * valeurs "rondes" (1, 2 ou 5 x 10^n).
 *
 * La vue s'ajuste automatiquement aux données de la fenêtre temporelle ;
 * zoom et panoramique s'appliquent par-dessus cet ajustement automatique.
 */
public class Curve2DRenderer implements GLEventListener {

    // Courbes affichées ; la liste peut être modifiée depuis n'importe quel thread
    private final List<Curve2DData> curves = new CopyOnWriteArrayList<>();
    private List<Curve2DData> frameCurves = new ArrayList<>(); // courbes figées pour l'image en cours

    // Boîte englobante de l'ensemble des courbes, recalculée à chaque image
    private float dataMinX = -1f, dataMaxX = 1f, dataMinY = -1f, dataMaxY = 1f;
    private final GLText text = new GLText(); // texte en police du système (graduations, noms d'axes, légende)

    // Polices réglables (voir setScaleFont / setLegendFont)
    private volatile ChartFont scaleFont = new ChartFont(9, SWT.NORMAL);
    private volatile ChartFont legendFont = new ChartFont(9, SWT.NORMAL);
    private volatile boolean showLegend = true; // légende affichée (si au moins une courbe a un nom)
    private final DashedLines dashedLines = new DashedLines(); // tirets ancrés sur les données

    private static final int TARGET_TICK_COUNT = 6;     // nombre approximatif de graduations par axe
    private static final float DEFAULT_LINE_WIDTH = 2f; // épaisseur des courbes sans épaisseur explicite (pixels)
    private static final float DATA_MARGIN = 0.05f;     // espace entre courbe et cadre (fraction de l'étendue)

    // Vue : zoom (1 = ajustement automatique) et panoramique (en fraction de l'étendue non zoomée)
    private float zoom = 1f;
    private float panX = 0f, panY = 0f;

    private boolean showAxes = false;
    private boolean showPoints = false;
    private boolean showGrid = true;
    private boolean showLabel = true;
    private boolean equalAspect = false; // même échelle sur x et y (utile pour les trajectoires)

    // Marges autour de la zone de tracé (pixels), adaptées à la taille des polices
    private int marginLeft = 75, marginRight = 25, marginTop = 25, marginBottom = 35;
    private float yLabelWidth = 0; // largeur de la plus longue graduation verticale (image précédente)
    private float xLabelWidth = 0; // largeur de la plus longue graduation horizontale (image précédente)

    private volatile LegendPosition legendPosition = LegendPosition.INSIDE;

    // Échelles fixes {xMin, xMax, yMin, yMax}, ou null pour des échelles dynamiques (ajustées aux données)
    private volatile double[] fixedScales;
    private float legendBelowHeight = 0; // hauteur de la légende sous l'axe (pixels), 0 si absente

    // Taille de la zone d'affichage et de la zone de tracé (pixels)
    private int width = 1, height = 1;
    private int plotWidth = 1, plotHeight = 1;

    // Fenêtre visible, en unités des données (recalculée à chaque image)
    private float viewMinX, viewMaxX, viewMinY, viewMaxY;

    public Curve2DRenderer(Curve2DData... curves) {
        this.curves.addAll(Arrays.asList(curves));
    }

    /** Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread). */
    public void addCurve(Curve2DData curve) { curves.add(curve); }
    /** Retire une courbe du graphe. */
    public void removeCurve(Curve2DData curve) { curves.remove(curve); }
    /** Courbes actuellement affichées. */
    public List<Curve2DData> getCurves() { return new ArrayList<>(curves); }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glDisable(GL2.GL_DEPTH_TEST);
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
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
        text.setPixelScale(GLText.surfaceScale(drawable));
        // Viewport et projection sont appliqués dans display()
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        // Fige les données reçues depuis l'image précédente (bornes, centre)
        frameCurves = new ArrayList<>(curves);
        for (Curve2DData curve : frameCurves) curve.takeSnapshot();
        computeBounds();

        computeMargins();
        plotWidth = Math.max(1, width - marginLeft - marginRight);
        plotHeight = Math.max(1, height - marginBottom - marginTop);
        computeView();

        double stepX = niceStep((viewMaxX - viewMinX) / TARGET_TICK_COUNT);
        double stepY = niceStep((viewMaxY - viewMinY) / TARGET_TICK_COUNT);

        // --- Zone de tracé, en unités des données ---
        gl.glViewport(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glEnable(GL2.GL_SCISSOR_TEST);
        gl.glScissor(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(viewMinX, viewMaxX, viewMinY, viewMaxY, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        if (showGrid) drawGrid(gl, stepX, stepY);
        if (showAxes) drawAxes(gl);
        drawCurves(gl);
        if (showPoints) drawPoints(gl);

        gl.glDisable(GL2.GL_SCISSOR_TEST);

        // --- Habillage (cadre, graduations), en pixels sur toute la surface ---
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, width, 0, height, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawFrame(gl);
        if (showLabel) drawLabels(gl, stepX, stepY);
        drawLegend(gl);
    }

    /** Calcule la fenêtre visible à partir des bornes des données, du zoom et du panoramique. */
    private void computeView() {
        double[] fixed = fixedScales;
        float halfX, halfY, baseCenterX, baseCenterY;
        if (fixed != null) { // échelles fixes : exactement les bornes fournies, sans marge
            halfX = (float) ((fixed[1] - fixed[0]) / 2);
            halfY = (float) ((fixed[3] - fixed[2]) / 2);
            baseCenterX = (float) ((fixed[0] + fixed[1]) / 2);
            baseCenterY = (float) ((fixed[2] + fixed[3]) / 2);
        } else {
            halfX = halfExtent(dataMinX, dataMaxX);
            halfY = halfExtent(dataMinY, dataMaxY);
            baseCenterX = (dataMinX + dataMaxX) / 2f;
            baseCenterY = (dataMinY + dataMaxY) / 2f;
        }

        float centerX = baseCenterX + panX * 2f * halfX;
        float centerY = baseCenterY + panY * 2f * halfY;

        halfX /= zoom;
        halfY /= zoom;

        if (equalAspect) {
            // Même nombre d'unités par pixel sur les deux axes : on élargit l'axe le plus "serré"
            float unitsPerPixel = Math.max(2f * halfX / plotWidth, 2f * halfY / plotHeight);
            halfX = unitsPerPixel * plotWidth / 2f;
            halfY = unitsPerPixel * plotHeight / 2f;
        }

        viewMinX = centerX - halfX; viewMaxX = centerX + halfX;
        viewMinY = centerY - halfY; viewMaxY = centerY + halfY;
    }

    /** Demi-étendue (avec marge) d'un axe ; gère le cas dégénéré où toutes les valeurs sont égales. */
    private static float halfExtent(float min, float max) {
        float range = max - min;
        if (range > 0f) return range / 2f * (1f + 2f * DATA_MARGIN);
        float center = Math.abs((min + max) / 2f);
        return (center > 0f) ? center * 0.1f : 1f;
    }

    /**
     * Calcule les marges autour de la zone de tracé à partir de la police des
     * graduations : à gauche les graduations verticales (largeur mesurée à
     * l'image précédente), en bas les graduations horizontales et, le cas
     * échéant, la légende, en haut et à droite juste de quoi ne pas couper
     * les graduations placées aux bords.
     * La marge de gauche ne diminue que nettement, pour éviter que la zone de
     * tracé ne tremble quand la longueur des graduations varie.
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

    /** Pas de graduation "rond" (1, 2 ou 5 x 10^n) le plus proche de rawStep. */
    private static double niceStep(double rawStep) {
        if (!(rawStep > 0)) return 1;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;
        double nice = (normalized < 1.5) ? 1 : (normalized < 3.5) ? 2 : (normalized < 7.5) ? 5 : 10;
        return nice * magnitude;
    }

    /** Valeurs des graduations multiples de step comprises dans [min, max] (au plus 100). */
    private static double[] ticks(double min, double max, double step) {
        long first = (long) Math.ceil(min / step);
        int n = (int) Math.max(0, Math.min(100, Math.floor(max / step) - first + 1));
        double[] values = new double[n];
        for (int i = 0; i < n; i++) values[i] = (first + i) * step;
        return values;
    }

    private void drawGrid(GL2 gl, double stepX, double stepY) {
        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (double x : ticks(viewMinX, viewMaxX, stepX)) {
            gl.glVertex2d(x, viewMinY);
            gl.glVertex2d(x, viewMaxY);
        }
        for (double y : ticks(viewMinY, viewMaxY, stepY)) {
            gl.glVertex2d(viewMinX, y);
            gl.glVertex2d(viewMaxX, y);
        }
        gl.glEnd();
    }

    /** Droites x = 0 (verte, axe y) et y = 0 (rouge, axe x), si elles sont visibles. */
    private void drawAxes(GL2 gl) {
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glVertex2f(viewMinX, 0); gl.glVertex2f(viewMaxX, 0);
        gl.glColor3f(0.2f, 0.9f, 0.2f);
        gl.glVertex2f(0, viewMinY); gl.glVertex2f(0, viewMaxY);
        gl.glEnd();
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
     * Dessine les courbes. Une courbe seule et sans couleur explicite garde le
     * dégradé bleu -> orange ; sinon chaque courbe a sa couleur (explicite ou
     * de la palette) et s'estompe vers ses échantillons les plus anciens.
     */
    private void drawCurves(GL2 gl) {
        double scaleX = plotWidth / (double) (viewMaxX - viewMinX);   // pixels par unité
        double scaleY = plotHeight / (double) (viewMaxY - viewMinY);
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve2DData curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            float[] xy = curve.getSnapshotCoordinates();
            int n = curve.getSnapshotCount();
            boolean gradient = curve.getColor() == null && frameCurves.size() == 1;
            float[] color = CurveStyles.colorOf(curve, k);
            if (CurveStyles.isDashed(curve)) {
                dashedLines.draw(gl, curve, width, curve.getCapacity(), curve.getSnapshotTotalSamples(), n, 0, scaleX, scaleY,
                        i -> Math.hypot((xy[2 * i] - xy[2 * i - 2]) * scaleX, (xy[2 * i + 1] - xy[2 * i - 1]) * scaleY), null,
                        (i, f) -> {
                            vertexColor(gl, gradient, color, (n <= 1) ? 1f : (i - 1 + f) / (n - 1));
                            int a = 2 * (i - 1), b = 2 * i;
                            gl.glVertex2f(xy[a] + f * (xy[b] - xy[a]), xy[a + 1] + f * (xy[b + 1] - xy[a + 1]));
                        });
            } else {
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < n; i++) {
                    vertexColor(gl, gradient, color, (n <= 1) ? 1f : (float) i / (n - 1));
                    gl.glVertex2f(xy[2 * i], xy[2 * i + 1]);
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

    private void drawPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (Curve2DData curve : frameCurves) {
            float[] xy = curve.getSnapshotCoordinates();
            int n = curve.getSnapshotCount();
            for (int i = 0; i < n; i++) {
                gl.glVertex2f(xy[2 * i], xy[2 * i + 1]);
            }
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
     * Graduations (sous l'axe horizontal et à gauche de l'axe vertical) et
     * dans la police des échelles (coordonnées en pixels).
     */
    private void drawLabels(GL2 gl, double stepX, double stepY) {
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        ChartFont font = scaleFont;
        float lineHeight = text.lineHeight(font);

        float widestX = 0;
        for (double x : ticks(viewMinX, viewMaxX, stepX)) {
            String label = formatTick(x, stepX);
            float px = marginLeft + (float) ((x - viewMinX) / (viewMaxX - viewMinX)) * plotWidth;
            float labelWidth = text.width(label, font);
            widestX = Math.max(widestX, labelWidth);
            text.draw(gl, label, font, px - labelWidth / 2f, marginBottom - 4 - lineHeight);
        }
        xLabelWidth = widestX;
        float widest = 0;
        for (double y : ticks(viewMinY, viewMaxY, stepY)) {
            String label = formatTick(y, stepY);
            float labelWidth = text.width(label, font);
            widest = Math.max(widest, labelWidth);
            float py = marginBottom + (float) ((y - viewMinY) / (viewMaxY - viewMinY)) * plotHeight;
            text.draw(gl, label, font, marginLeft - 6 - labelWidth, py - lineHeight / 2f);
        }
        yLabelWidth = widest; // sert au calcul de la marge de gauche à l'image suivante
    }

    /** Formate une graduation avec juste assez de décimales pour le pas utilisé, en évitant "-0". */
    private static String formatTick(double value, double step) {
        if (Math.abs(value) < step * 1e-6) value = 0;
        if (step >= 1e6 || step < 1e-4) return String.format(Locale.US, "%.3g", value);
        int decimals = (int) Math.max(0, -Math.floor(Math.log10(step)));
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    // --- Accesseurs utilisés par CameraController ---

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

    public void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    /**
     * Police des graduations des échelles.
     */
    public void setScaleFont(ChartFont font) { if (font != null) scaleFont = font; }
    public ChartFont getScaleFont() { return scaleFont; }
    /** Police de la légende. */
    public void setLegendFont(ChartFont font) { if (font != null) legendFont = font; }
    /**
     * Échelles fixes : les axes vont exactement de xMin à xMax et de yMin à
     * yMax, quelles que soient les valeurs des courbes (ce qui dépasse est
     * coupé par le cadre). Zoom, panoramique et "même échelle" (touche E)
     * restent possibles ; R revient à ces échelles.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        fixedScales = CurveStyles.checkScales(xMin, xMax, yMin, yMax);
    }
    /** Échelles dynamiques, ajustées en continu aux valeurs des courbes (par défaut). */
    public void setDynamicScales() { fixedScales = null; }
    /** Vrai si les échelles sont fixes. */
    public boolean hasFixedScales() { return fixedScales != null; }

    /** Position de la légende : dans le graphe (INSIDE, par défaut) ou sous l'axe des abscisses (BELOW). */
    public void setLegendPosition(LegendPosition position) { if (position != null) legendPosition = position; }
    public LegendPosition getLegendPosition() { return legendPosition; }
    public ChartFont getLegendFont() { return legendFont; }

    public void toggleAxes() { showAxes = !showAxes; }
    public void togglePoints() { showPoints = !showPoints; }
    public void toggleGrid() { showGrid = !showGrid; }
    public void toggleLabel() { showLabel = !showLabel; }

    /** Affiche ou masque la légende (prise en compte à l'image suivante). */
    public void setLegendVisible(boolean visible) { showLegend = visible; }
    public boolean isLegendVisible() { return showLegend; }
    /** Bascule l'affichage de la légende (touche N). */
    public void toggleLegend() { showLegend = !showLegend; }
    public void toggleEqualAspect() { equalAspect = !equalAspect; }
}
