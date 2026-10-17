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
 * Rendu OpenGL d'un ou plusieurs signaux temps réel y(t) ({@link Curve1DData}),
 * selon deux modes (voir {@link DisplayMode}) :
 * <ul>
 * <li>{@link DisplayMode#SWEEP} (par défaut) : balayage de type oscilloscope.
 *     L'abscisse est fixe, de 0 à windowTimeWidth secondes ; le tracé
 *     progresse de gauche à droite puis repart à gauche en remplaçant
 *     progressivement le balayage précédent. Un curseur vertical indique la
 *     position d'écriture et un court espace vide le précède.</li>
 * <li>{@link DisplayMode#SCROLL} : défilement. L'abscisse se termine au
 *     dernier échantillon reçu et glisse avec le temps.</li>
 * </ul>
 *
 * Plusieurs signaux : chacun peut avoir sa propre fréquence d'échantillonnage
 * et sa propre durée de fenêtre. Leurs temps sont comptés à partir de leur
 * premier échantillon respectif (des voies acquises ensemble sont donc
 * alignées). La fenêtre affichée a la durée de la plus longue fenêtre et se
 * termine au dernier échantillon le plus récent, toutes courbes confondues.
 *
 * En mode défilement, l'axe horizontal est le temps, en secondes depuis le
 * premier échantillon. Il couvre toujours windowTimeWidth secondes : tant
 * que la fenêtre n'est pas remplie, il va de 0 à windowTimeWidth et la
 * courbe progresse vers la droite ; ensuite, il se termine au dernier
 * échantillon reçu et défile. En mode balayage, il indique le temps écoulé
 * depuis le début du balayage en cours.
 * L'axe vertical s'ajuste automatiquement aux valeurs de la fenêtre ;
 * zoom et panoramique vertical s'appliquent par-dessus. La touche "pause"
 * fige l'affichage sans interrompre la réception des données.
 *
 * Pour garder une bonne précision même après des heures d'acquisition,
 * les positions des sommets sont calculées en double : temps relatif au
 * dernier échantillon en défilement, temps modulo la durée de balayage en
 * mode oscilloscope.
 */
public class Curve1DRenderer implements GLEventListener {

    /** Mode d'affichage de l'axe du temps. */
    public enum DisplayMode {
        /** Balayage de type oscilloscope : abscisse fixe [0, windowTimeWidth], le tracé repart à gauche. */
        SWEEP,
        /** Défilement : l'abscisse se termine au dernier échantillon et glisse avec le temps. */
        SCROLL
    }

    private volatile LegendPosition legendPosition = LegendPosition.INSIDE;

    // Échelle verticale fixe {yMin, yMax}, ou null pour une échelle dynamique (ajustée aux données)
    private volatile double[] fixedScales;
    private float legendBelowHeight = 0; // hauteur de la légende sous l'axe (pixels), 0 si absente

    /** En balayage, fraction de la fenêtre laissée vide juste devant le curseur d'écriture. */
    private static final double SWEEP_GAP = 0.02;

    private volatile DisplayMode displayMode = DisplayMode.SWEEP;

    // Courbes affichées ; la liste peut être modifiée depuis n'importe quel thread
    private final List<Curve1DData> curves = new CopyOnWriteArrayList<>();
    private List<Curve1DData> frameCurves = new ArrayList<>(); // courbes figées pour l'image en cours

    // Bornes communes, recalculées à chaque image
    private float dataMinY = -1f, dataMaxY = 1f;
    private double lastTime = 0;  // instant du dernier échantillon le plus récent (s)
    private double windowTime = 1; // durée de la plus longue fenêtre (s)
    private final GLText text = new GLText(); // texte en police du système (graduations, noms d'axes, légende)

    // Polices réglables (voir setScaleFont / setLegendFont)
    private volatile ChartFont scaleFont = new ChartFont(9, SWT.NORMAL);
    private volatile ChartFont legendFont = new ChartFont(9, SWT.NORMAL);
    private volatile boolean showLegend = true; // légende affichée (si au moins une courbe a un nom)
    private final FpsCounter fpsCounter = new FpsCounter(); // cadence d'affichage réelle
    private volatile boolean showFps = false;
    private final CurrentValues currentValues = new CurrentValues(); // textes des valeurs courantes
    private volatile boolean showCurrentValues = false;
    private final DashedLines dashedLines = new DashedLines(); // tirets ancrés sur les données

    private static final int TARGET_TICK_COUNT = 6;     // nombre approximatif de graduations par axe
    private static final float DEFAULT_LINE_WIDTH = 1.5f; // épaisseur des courbes sans épaisseur explicite (pixels)
    private static final float DATA_MARGIN = 0.05f;     // espace entre courbe et cadre (fraction de l'étendue)

    // Vue verticale : zoom (1 = ajustement automatique) et panoramique (en fraction de l'étendue non zoomée)
    private float zoomY = 1f;
    private float panY = 0f;

    private boolean showAxes = false;
    private boolean showPoints = false;
    private boolean showGrid = true;
    private boolean showLabel = true;
    private boolean paused = false;

    // Marges autour de la zone de tracé (pixels), adaptées à la taille des polices
    private int marginLeft = 75, marginRight = 25, marginTop = 25, marginBottom = 35;
    private float yLabelWidth = 0; // largeur de la plus longue graduation verticale (image précédente)
    private float xLabelWidth = 0; // largeur de la plus longue graduation horizontale (image précédente)

    // Taille de la zone d'affichage et de la zone de tracé (pixels)
    private int width = 1, height = 1;
    private int plotWidth = 1, plotHeight = 1;

    // Fenêtre visible : temps absolus (s) et valeurs, recalculés à chaque image
    private double viewStartTime, viewEndTime;
    private float viewMinY, viewMaxY;

    public Curve1DRenderer(Curve1DData... curves) {
        this.curves.addAll(Arrays.asList(curves));
    }

    /** Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread). */
    public void addCurve(Curve1DData curve) { curves.add(curve); }
    /** Retire une courbe du graphe. */
    public void removeCurve(Curve1DData curve) { curves.remove(curve); }
    /** Courbes actuellement affichées. */
    public List<Curve1DData> getCurves() { return new ArrayList<>(curves); }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glPointSize(4f);
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
        fpsCounter.frame();
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        // Fige les données reçues depuis l'image précédente (sauf en pause : l'affichage reste figé)
        if (!paused) {
            frameCurves = new ArrayList<>(curves);
            for (Curve1DData curve : frameCurves) curve.takeSnapshot();
            computeBounds();
        }

        computeMargins();
        plotWidth = Math.max(1, width - marginLeft - marginRight);
        plotHeight = Math.max(1, height - marginBottom - marginTop);
        computeView();

        double stepT = niceStep((viewEndTime - viewStartTime) / TARGET_TICK_COUNT);
        double stepY = niceStep((viewMaxY - viewMinY) / TARGET_TICK_COUNT);

        // Origine des abscisses OpenGL : le dernier échantillon en défilement (précision des float),
        // le début du balayage en mode oscilloscope (les abscisses vont alors de 0 à windowTime)
        DisplayMode mode = displayMode;
        double origin = (mode == DisplayMode.SCROLL) ? lastTime : 0;

        // --- Zone de tracé : x en temps relatif à origin, y en unités des données ---
        gl.glViewport(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glEnable(GL2.GL_SCISSOR_TEST);
        gl.glScissor(marginLeft, marginBottom, plotWidth, plotHeight);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(viewStartTime - origin, viewEndTime - origin, viewMinY, viewMaxY, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        if (showGrid) drawGrid(gl, stepT, stepY, origin);
        if (showAxes) drawZeroLine(gl, origin);
        if (mode == DisplayMode.SWEEP) {
            drawSweepCursor(gl);
            drawCurvesSweep(gl, showPoints);
        } else {
            drawCurves(gl);
            if (showPoints) drawPoints(gl);
        }

        gl.glDisable(GL2.GL_SCISSOR_TEST);

        // --- Habillage (cadre, graduations), en pixels sur toute la surface ---
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, width, 0, height, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawFrame(gl);
        if (showLabel) drawLabels(gl, stepT, stepY);
        drawLegend(gl);
        drawFps(gl);
        float valuesHeight = drawCurrentValues(gl);
        if (paused) {
            // Sous les valeurs courantes si elles sont affichées
            gl.glColor3f(1f, 0.75f, 0.2f);
            String label = "PAUSE";
            text.draw(gl, label, scaleFont, marginLeft + plotWidth - text.width(label, scaleFont) - 6,
                      marginBottom + plotHeight - text.lineHeight(scaleFont) - 4 - (valuesHeight > 0 ? valuesHeight + 4 : 0));
        }
    }

    /**
     * Calcule la fenêtre visible : [0, windowTime] en balayage, windowTime
     * secondes se terminant au dernier échantillon en défilement ; y ajusté
     * aux données, ou égal à l'échelle fixe si elle est définie.
     */
    private void computeView() {
        if (displayMode == DisplayMode.SWEEP) {
            viewStartTime = 0;
            viewEndTime = windowTime;
        } else {
            viewEndTime = Math.max(windowTime, lastTime);
            viewStartTime = viewEndTime - windowTime;
        }

        double[] fixed = fixedScales;
        float halfY = (fixed != null) ? (float) ((fixed[1] - fixed[0]) / 2) : halfExtent(dataMinY, dataMaxY);
        float baseCenterY = (fixed != null) ? (float) ((fixed[0] + fixed[1]) / 2) : (dataMinY + dataMaxY) / 2f;
        float centerY = baseCenterY + panY * 2f * halfY;
        halfY /= zoomY;
        viewMinY = centerY - halfY;
        viewMaxY = centerY + halfY;
    }

    /** Demi-étendue (avec marge) d'un axe ; gère le cas dégénéré où toutes les valeurs sont égales. */
    private static float halfExtent(float min, float max) {
        float range = max - min;
        if (range > 0f) return range / 2f * (1f + 2f * DATA_MARGIN);
        float center = Math.abs((min + max) / 2f);
        return (center > 0f) ? center * 0.1f : 1f;
    }

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

    /**
     * Valeurs courantes (la dernière valeur de chaque courbe), en haut à droite de la zone de tracé, dans la couleur de chaque courbe et la police
     * des échelles du graphe (coordonnées en pixels).
     *
     * @return la hauteur occupée en pixels (0 si rien n'est affiché)
     */
    private float drawCurrentValues(GL2 gl) {
        if (!showCurrentValues) return 0;
        List<Curve1DData> curves = frameCurves;
        String[] labels = currentValues.update(text, scaleFont, curves, k -> {
            Curve1DData c = curves.get(k);
            int n = c.getSnapshotCount();
            return (n == 0) ? null : CurrentValues.format(c.getSnapshotValues()[n - 1]);
        });
        return CurveStyles.drawCurrentValues(gl, text, scaleFont, curves, labels, marginLeft + plotWidth - 6, marginBottom + plotHeight - 6);
    }

    /** Cadence d'affichage réelle, en bas à droite, dans la police des échelles (coordonnées en pixels). */
    private void drawFps(GL2 gl) {
        if (!showFps) return;
        String label = String.format(java.util.Locale.US, "%.1f i/s", fpsCounter.getFps());
        gl.glColor3f(1f, 0.75f, 0.2f);
        text.draw(gl, label, scaleFont, marginLeft + plotWidth - text.width(label, scaleFont) - 6, marginBottom + 4);
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

    private void drawGrid(GL2 gl, double stepT, double stepY, double origin) {
        double startRel = viewStartTime - origin, endRel = viewEndTime - origin;
        gl.glColor4f(0.4f, 0.4f, 0.45f, 0.3f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        for (double t : ticks(viewStartTime, viewEndTime, stepT)) {
            gl.glVertex2d(t - origin, viewMinY);
            gl.glVertex2d(t - origin, viewMaxY);
        }
        for (double y : ticks(viewMinY, viewMaxY, stepY)) {
            gl.glVertex2d(startRel, y);
            gl.glVertex2d(endRel, y);
        }
        gl.glEnd();
    }

    /** Droite y = 0, si elle est visible (abscisses relatives à origin). */
    private void drawZeroLine(GL2 gl, double origin) {
        gl.glColor3f(0.9f, 0.2f, 0.2f);
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2d(viewStartTime - origin, 0);
        gl.glVertex2d(viewEndTime - origin, 0);
        gl.glEnd();
    }

    /** Bornes en y de l'ensemble des courbes non vides, instant du dernier échantillon et durée de fenêtre communs. */
    private void computeBounds() {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        double last = Double.NEGATIVE_INFINITY, window = 0;
        for (Curve1DData curve : frameCurves) {
            window = Math.max(window, curve.getWindowTimeWidth());
            if (curve.getSnapshotCount() == 0) continue;
            last = Math.max(last, curve.getLastSampleTime());
            minY = Math.min(minY, curve.getMinY());
            maxY = Math.max(maxY, curve.getMaxY());
        }
        if (minY > maxY) { // aucune donnée
            minY = -1f;
            maxY = 1f;
        }
        dataMinY = minY;
        dataMaxY = maxY;
        lastTime = Double.isInfinite(last) ? 0 : last;
        windowTime = (window > 0) ? window : 1;
    }

    /**
     * Dessine les courbes, chacune de sa couleur (explicite ou de la palette).
     * Les abscisses sont en temps relatif à lastTime (le dernier échantillon
     * le plus récent), ce qui préserve la précision des float.
     */
    private void drawCurves(GL2 gl) {
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve1DData curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            float dt = (float) (1.0 / curve.getSampleFrequency());
            float offset = (float) (curve.getLastSampleTime() - lastTime); // <= 0
            float[] color = CurveStyles.colorOf(curve, k);
            gl.glColor3f(color[0], color[1], color[2]);
            if (CurveStyles.isDashed(curve)) {
                drawDashed(gl, curve, width, values, n, 0, null,
                        (i, f) -> gl.glVertex2f(offset - (n - i - f) * dt, values[i - 1] + f * (values[i] - values[i - 1])));
            } else {
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < n; i++) {
                    gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
                }
                gl.glEnd();
            }
        }
    }

    /**
     * Tracé en tirets d'un signal (motif ancré sur les données, voir
     * {@link DashedLines}). La longueur à l'écran d'un segment ne dépend que
     * des échelles : elle est la même en balayage et en défilement.
     */
    private void drawDashed(GL2 gl, Curve1DData curve, float width, float[] values, int n, int from,
                            DashedLines.SegmentFilter filter, DashedLines.Emitter emitter) {
        double scaleX = plotWidth / windowTime;                         // pixels par seconde
        double scaleY = plotHeight / (double) (viewMaxY - viewMinY);   // pixels par unité
        double dx = scaleX / curve.getSampleFrequency();                // pixels entre deux échantillons
        dashedLines.draw(gl, curve, width, curve.getCapacity(), curve.getSnapshotTotalSamples(), n, from, scaleX, scaleY,
                i -> Math.hypot(dx, (values[i] - values[i - 1]) * scaleY), filter, emitter);
    }

    /**
     * Mode balayage : chaque échantillon d'instant t est placé en x = t modulo
     * windowTime ; le tracé est interrompu à chaque retour à gauche. Les
     * échantillons dont l'instant précède lastTime - windowTime * (1 - SWEEP_GAP)
     * ne sont pas dessinés, ce qui laisse un espace vide devant le curseur.
     */
    private void drawCurvesSweep(GL2 gl, boolean withPoints) {
        double oldestShown = lastTime - windowTime * (1 - SWEEP_GAP);
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve1DData curve = frameCurves.get(k);
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            double frequency = curve.getSampleFrequency();
            double curveLast = curve.getLastSampleTime();
            // Premier échantillon à dessiner (les plus anciens sont dans l'espace vide)
            int first = (int) Math.max(0, Math.ceil((oldestShown - curveLast) * frequency + (n - 1) - 1e-9));

            float[] color = CurveStyles.colorOf(curve, k);
            gl.glColor3f(color[0], color[1], color[2]);
            float width = CurveStyles.lineWidthOf(curve, DEFAULT_LINE_WIDTH);
            gl.glLineWidth(width);
            if (CurveStyles.isDashed(curve)) {
                // Segments à cheval sur un retour à gauche non dessinés
                drawDashed(gl, curve, width, values, n, first,
                        i -> Math.floor((curveLast - (n - i) / frequency) / windowTime)
                             != Math.floor((curveLast - (n - 1 - i) / frequency) / windowTime),
                        (i, f) -> {
                            double t = curveLast - (n - i - f) / frequency;
                            double sweepStart = Math.floor((curveLast - (n - i) / frequency) / windowTime) * windowTime;
                            gl.glVertex2d(t - sweepStart, values[i - 1] + f * (values[i] - values[i - 1]));
                        });
            } else {
                long currentSweep = Long.MIN_VALUE;
                boolean open = false;
                for (int i = first; i < n; i++) {
                    double t = curveLast - (n - 1 - i) / frequency;
                    long sweep = (long) Math.floor(t / windowTime);
                    if (sweep != currentSweep) { // retour à gauche : nouveau tronçon
                        if (open) gl.glEnd();
                        gl.glBegin(GL2.GL_LINE_STRIP);
                        open = true;
                        currentSweep = sweep;
                    }
                    gl.glVertex2d(t - sweep * windowTime, values[i]);
                }
                if (open) gl.glEnd();
            }

            if (withPoints) {
                gl.glColor3f(1f, 1f, 1f);
                gl.glBegin(GL2.GL_POINTS);
                for (int i = first; i < n; i++) {
                    double t = curveLast - (n - 1 - i) / frequency;
                    gl.glVertex2d(t - Math.floor(t / windowTime) * windowTime, values[i]);
                }
                gl.glEnd();
            }
        }
    }

    /** Mode balayage : trait vertical à la position d'écriture courante. */
    private void drawSweepCursor(GL2 gl) {
        boolean any = false;
        for (Curve1DData curve : frameCurves) any |= curve.getSnapshotCount() > 0;
        if (!any) return;
        double x = lastTime - Math.floor(lastTime / windowTime) * windowTime;
        gl.glColor4f(0.85f, 0.85f, 0.9f, 0.5f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2d(x, viewMinY);
        gl.glVertex2d(x, viewMaxY);
        gl.glEnd();
    }

    private void drawPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (Curve1DData curve : frameCurves) {
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            float dt = (float) (1.0 / curve.getSampleFrequency());
            float offset = (float) (curve.getLastSampleTime() - lastTime);
            for (int i = 0; i < n; i++) {
                gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
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
     * Graduations (sous l'axe du temps et à gauche de l'axe vertical) et
     * dans la police des échelles (coordonnées en pixels).
     */
    private void drawLabels(GL2 gl, double stepT, double stepY) {
        gl.glColor3f(0.85f, 0.85f, 0.9f);
        ChartFont font = scaleFont;
        float lineHeight = text.lineHeight(font);

        float widestX = 0;
        for (double t : ticks(viewStartTime, viewEndTime, stepT)) {
            String label = formatTick(t, stepT);
            float px = marginLeft + (float) ((t - viewStartTime) / (viewEndTime - viewStartTime)) * plotWidth;
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
     * Panoramique vertical (l'axe du temps suit toujours le dernier
     * échantillon). dy est un déplacement exprimé en fraction de la hauteur
     * du composant ; dx est ignoré. Le contenu suit la souris.
     */
    public void pan(float dx, float dy) {
        panY += dy * height / plotHeight / zoomY;
    }

    /** Zoom vertical multiplicatif : steps > 0 rapproche, steps < 0 éloigne. */
    public void zoom(float steps) {
        zoomY *= (float) Math.pow(1.15, steps);
        zoomY = Math.max(1e-3f, Math.min(1e4f, zoomY));
    }

    public void resetView() {
        zoomY = 1f;
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
     * Échelle verticale fixe : l'axe vertical va exactement de yMin à yMax,
     * quelles que soient les valeurs des courbes (ce qui dépasse est coupé
     * par le cadre). Zoom et panoramique restent possibles ; R revient à
     * cette échelle.
     */
    public void setFixedScales(double yMin, double yMax) { fixedScales = CurveStyles.checkScales(yMin, yMax); }
    /** Échelle verticale dynamique, ajustée en continu aux valeurs des courbes (par défaut). */
    public void setDynamicScales() { fixedScales = null; }
    /** Vrai si l'échelle verticale est fixe. */
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

    /** Cadence d'affichage réelle mesurée, en images par seconde. */
    public double getFps() { return fpsCounter.getFps(); }
    /** Affiche ou masque la cadence d'affichage dans le graphe (touche F). */
    public void setFpsVisible(boolean visible) { showFps = visible; }
    public boolean isFpsVisible() { return showFps; }
    public void toggleFps() { showFps = !showFps; }

    /** Affiche ou masque les valeurs courantes des courbes, en haut à droite (touche V). */
    public void setCurrentValuesVisible(boolean visible) { showCurrentValues = visible; }
    public boolean isCurrentValuesVisible() { return showCurrentValues; }
    public void toggleCurrentValues() { showCurrentValues = !showCurrentValues; }
    public void setDisplayMode(DisplayMode displayMode) { this.displayMode = displayMode; }
    public DisplayMode getDisplayMode() { return displayMode; }
    /** Bascule entre balayage (oscilloscope) et défilement. */
    public void toggleDisplayMode() {
        displayMode = (displayMode == DisplayMode.SWEEP) ? DisplayMode.SCROLL : DisplayMode.SWEEP;
    }

    /** Fige / relance l'affichage ; les données continuent d'être reçues pendant la pause. */
    public void togglePause() { paused = !paused; }
}
