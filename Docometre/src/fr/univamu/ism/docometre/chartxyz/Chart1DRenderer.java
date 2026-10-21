package fr.univamu.ism.docometre.chartxyz;

import java.util.Locale;

import com.jogamp.opengl.GL2;

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
 * Plusieurs signaux : chacun peut avoir sa propre fréquence d'échantillonnage.
 * La durée de la fenêtre (windowTimeWidth) est celle du graphe : elle est
 * imposée à toutes ses courbes, dont elle fixe la capacité. Les temps sont
 * comptés à partir du premier échantillon de chaque courbe (des voies
 * acquises ensemble sont donc alignées) ; la fenêtre se termine au dernier
 * échantillon le plus récent, toutes courbes confondues.
 *
 * En mode défilement, l'axe horizontal est le temps, en secondes depuis le
 * premier échantillon. Il couvre toujours windowTimeWidth secondes : tant
 * que la fenêtre n'est pas remplie, il va de 0 à windowTimeWidth et la
 * courbe progresse vers la droite ; ensuite, il se termine au dernier
 * échantillon reçu et défile. En mode balayage, il indique le temps écoulé
 * depuis le début du balayage en cours.
 * L'axe vertical s'ajuste automatiquement aux valeurs de la fenêtre ;
 * zoom et panoramique s'appliquent par-dessus. La touche "pause"
 * fige l'affichage sans interrompre la réception des données.
 *
 * Curseur (touche C, voir {@link PlotRenderer}) : il désigne l'échantillon
 * dont l'instant est le plus proche de l'abscisse de la souris, quelle que
 * soit la hauteur de celle-ci. Son abscisse est celle de
 * l'axe du temps (temps depuis le premier échantillon en défilement, position
 * dans le balayage en balayage), affichée avec la résolution de la fréquence
 * d'échantillonnage ; en balayage, les échantillons de l'espace vide ne
 * peuvent pas être désignés.
 *
 * Commandes clavier propres au 1D : espace, pause / reprise ; M, bascule
 * balayage / défilement (la fenêtre de zoom est alors supprimée). Le
 * panoramique (bouton droit) déplace la vue en x et en y ; en défilement,
 * la vue décalée continue de suivre le dernier échantillon. La molette ne
 * zoome que sur y.
 *
 * Zoom par rectangle (voir {@link PlotRenderer}) : en balayage, les
 * abscisses de la fenêtre sont des positions dans le balayage ; en
 * défilement, elles sont relatives au dernier échantillon, si bien que la
 * fenêtre suit le temps (loupe sur les dernières secondes) ; la pause
 * (espace) permet d'examiner un instant précis.
 *
 * Pour garder une bonne précision même après des heures d'acquisition,
 * les positions des sommets sont calculées en double : temps relatif au
 * dernier échantillon en défilement, temps modulo la durée de balayage en
 * mode oscilloscope.
 */
public class Chart1DRenderer extends PlotRenderer<Curve1DData> {

    /** Mode d'affichage de l'axe du temps. */
    public enum DisplayMode {
        /** Balayage de type oscilloscope : abscisse fixe [0, windowTimeWidth], le tracé repart à gauche. */
        SWEEP,
        /** Défilement : l'abscisse se termine au dernier échantillon et glisse avec le temps. */
        SCROLL
    }

    /** En balayage, fraction de la fenêtre laissée vide juste devant le curseur d'écriture. */
    private static final double SWEEP_GAP = 0.02;
    private static final float DEFAULT_LINE_WIDTH = 1.5f; // épaisseur des courbes sans épaisseur explicite (pixels)

    private volatile DisplayMode displayMode = DisplayMode.SWEEP;
    private DisplayMode frameMode = DisplayMode.SWEEP; // mode figé pour l'image en cours
    private volatile boolean paused = false;

    // Bornes communes, recalculées à chaque image
    private float dataMinY = -1f, dataMaxY = 1f;
    private double lastTime = 0;  // instant du dernier échantillon le plus récent (s)
    private double windowTime = 1; // durée de la fenêtre pour l'image en cours (s)

    /**
     * @param windowTimeWidth durée de la fenêtre affichée, en secondes, imposée à toutes les courbes
     */
    public Chart1DRenderer(double windowTimeWidth, Curve1DData... curves) {
        super(4f, curves);
        setWindowTimeWidth(windowTimeWidth);
    }

    /**
     * Graphe dont la fenêtre est la plus longue des fenêtres de ses courbes,
     * qui gardent chacune la leur (comportement d'origine).
     *
     * @deprecated la fenêtre appartient au graphe : utiliser {@link #Chart1DRenderer(double, Curve1DData...)}
     */
    @Deprecated
    public Chart1DRenderer(Curve1DData... curves) {
        super(4f, curves);
    }

    /** Fige les données reçues depuis l'image précédente (sauf en pause : l'affichage reste figé). */
    @Override
    protected void updateData() {
        frameMode = displayMode;
        if (paused) return;
        snapshotCurves();
        computeBounds();
    }

    /** Bornes en y de l'ensemble des courbes non vides, instant du dernier échantillon et durée de la fenêtre. */
    private void computeBounds() {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        double imposed = getWindowTimeWidth();
        double last = Double.NEGATIVE_INFINITY, window = imposed;
        for (Curve1DData curve : frameCurves) {
            if (imposed == 0) window = Math.max(window, curve.getWindowTimeWidth());
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
     * Calcule la fenêtre visible : la fenêtre de zoom si elle existe (abscisses
     * relatives à {@link #zoomXReference()}) ; sinon [0, windowTime] en
     * balayage, windowTime secondes se terminant au dernier échantillon en
     * défilement, et y ajusté aux données, ou égal à l'échelle fixe si elle est définie.
     */
    @Override
    protected void computeView() {
        double[] window = zoomWindow();
        if (window != null) {
            double reference = zoomXReference();
            viewMinX = window[0] + reference;
            viewMaxX = window[1] + reference;
            viewMinY = window[2];
            viewMaxY = window[3];
            return;
        }
        if (frameMode == DisplayMode.SWEEP) {
            viewMinX = 0;
            viewMaxX = windowTime;
        } else {
            viewMaxX = Math.max(windowTime, lastTime);
            viewMinX = viewMaxX - windowTime;
        }
        // Panoramique horizontal, en fraction de la fenêtre (le contenu suit la souris)
        double shift = panX * windowTime;
        viewMinX += shift;
        viewMaxX += shift;

        double[] fixed = getFixedScaleBounds();
        float halfY = (fixed != null) ? (float) ((fixed[1] - fixed[0]) / 2) : halfExtent(dataMinY, dataMaxY);
        float baseCenterY = (fixed != null) ? (float) ((fixed[0] + fixed[1]) / 2) : (dataMinY + dataMaxY) / 2f;
        float centerY = baseCenterY + panY * 2f * halfY;
        halfY /= zoom;
        viewMinY = centerY - halfY;
        viewMaxY = centerY + halfY;
    }

    /**
     * Origine des abscisses OpenGL : le dernier échantillon en défilement
     * (précision des float), le début du balayage en mode oscilloscope (les
     * abscisses vont alors de 0 à windowTime).
     */
    @Override
    protected double xOrigin() {
        return (frameMode == DisplayMode.SCROLL) ? lastTime : 0;
    }

    /** En défilement, la fenêtre de zoom est relative au dernier échantillon : elle suit le temps. */
    @Override
    protected double zoomXReference() {
        return (frameMode == DisplayMode.SCROLL) ? lastTime : 0;
    }

    /** La molette ne zoome que sur l'axe vertical, avec ou sans fenêtre de zoom. */
    @Override
    protected boolean wheelZoomsX() { return false; }

    /** Le curseur suit l'abscisse de la souris : l'échantillon de même instant, quelle que soit sa hauteur. */
    @Override
    protected boolean cursorFollowsXOnly() { return true; }

    /** En balayage, les échantillons de l'espace vide ne sont pas dessinés : le curseur les ignore. */
    @Override
    protected int firstCursorPoint(Curve1DData curve) {
        return (frameMode == DisplayMode.SWEEP) ? firstSweepSample(curve) : 0;
    }

    /** Point i : temps (ou position dans le balayage) et valeur, comme au tracé. */
    @Override
    protected boolean cursorPoint(Curve1DData curve, int i, double[] xy) {
        int n = curve.getSnapshotCount();
        double t = curve.getLastSampleTime() - (n - 1 - i) / curve.getSampleFrequency();
        xy[0] = (frameMode == DisplayMode.SWEEP) ? t - Math.floor(t / windowTime) * windowTime : t;
        xy[1] = curve.getSnapshotValues()[i];
        return true;
    }

    /** Temps du curseur avec autant de décimales que la période d'échantillonnage en demande (0,002 s à 500 Hz). */
    @Override
    protected String formatCursorX(Curve1DData curve, double x) {
        int decimals = (int) Math.max(0, Math.min(6, Math.ceil(Math.log10(curve.getSampleFrequency()) - 1e-9)));
        return String.format(Locale.US, "%." + decimals + "f", x);
    }

    /** En balayage, premier échantillon dessiné de la courbe : les plus anciens sont dans l'espace vide devant le curseur d'écriture. */
    private int firstSweepSample(Curve1DData curve) {
        double oldestShown = lastTime - windowTime * (1 - SWEEP_GAP);
        int n = curve.getSnapshotCount();
        return (int) Math.max(0, Math.ceil((oldestShown - curve.getLastSampleTime()) * curve.getSampleFrequency() + (n - 1) - 1e-9));
    }

    @Override
    protected void drawCurves(GL2 gl, double origin) {
        if (frameMode == DisplayMode.SWEEP) {
            drawSweepCursor(gl);
            drawCurvesSweep(gl, showPoints);
        } else {
            drawCurvesScroll(gl);
            if (showPoints) drawPointsScroll(gl);
        }
    }

    /** "PAUSE", sous les valeurs courantes si elles sont affichées. */
    @Override
    protected void drawStatus(GL2 gl, float valuesHeight) {
        if (!paused) return;
        gl.glColor3f(1f, 0.75f, 0.2f);
        String label = "PAUSE";
        text.draw(gl, label, scaleFont, marginLeft + plotWidth - text.width(label, scaleFont) - 6,
                  marginBottom + plotHeight - text.lineHeight(scaleFont) - 4 - (valuesHeight > 0 ? valuesHeight + 4 : 0));
    }

    /**
     * Dessine les courbes, chacune de sa couleur (explicite ou de la palette).
     * Les abscisses sont en temps relatif à lastTime (le dernier échantillon
     * le plus récent), ce qui préserve la précision des float.
     */
    private void drawCurvesScroll(GL2 gl) {
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
                drawnPoints += n;
            } else if (useDecimation(n, 2 * plotWidth)) {
                // Réduction M4 : colonne de pixels de chaque échantillon, à partir de son abscisse tracée
                // (étendue visible et non windowTime : l'axe du temps peut être zoomé)
                double left = viewMinX - lastTime, pixelsPerSecond = plotWidth / (viewMaxX - viewMinX);
                decimation.m4(0, n, values, i -> (long) Math.floor(((offset - (n - 1 - i) * dt) - left) * pixelsPerSecond));
                int[] kept = decimation.indices();
                int count = decimation.count();
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < count; j++) {
                    int i = kept[j];
                    gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
                }
                gl.glEnd();
                drawnPoints += count;
            } else {
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int i = 0; i < n; i++) {
                    gl.glVertex2f(offset - (n - 1 - i) * dt, values[i]);
                }
                gl.glEnd();
                drawnPoints += n;
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
        double scaleX = plotWidth / (viewMaxX - viewMinX);              // pixels par seconde (zoom compris)
        double scaleY = plotHeight / (viewMaxY - viewMinY);          // pixels par unité
        double dx = scaleX / curve.getSampleFrequency();                // pixels entre deux échantillons
        dashedLines.draw(gl, curve, width, curve.getSnapshotCapacity(), curve.getSnapshotTotalSamples(), n, from, scaleX, scaleY,
                i -> Math.hypot(dx, (values[i] - values[i - 1]) * scaleY), filter, emitter);
    }

    /**
     * Mode balayage : chaque échantillon d'instant t est placé en x = t modulo
     * windowTime ; le tracé est interrompu à chaque retour à gauche. Les
     * échantillons dont l'instant précède lastTime - windowTime * (1 - SWEEP_GAP)
     * ne sont pas dessinés, ce qui laisse un espace vide devant le curseur.
     */
    private void drawCurvesSweep(GL2 gl, boolean withPoints) {
        for (int k = 0; k < frameCurves.size(); k++) {
            Curve1DData curve = frameCurves.get(k);
            float[] values = curve.getSnapshotValues();
            int n = curve.getSnapshotCount();
            double frequency = curve.getSampleFrequency();
            double curveLast = curve.getLastSampleTime();
            int first = firstSweepSample(curve); // les plus anciens sont dans l'espace vide

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
                drawnPoints += n - first;
            } else {
                // Réduction M4 éventuelle : clé = (balayage, colonne de pixels dans le balayage)
                int[] kept = null;
                int count = n - first;
                if (useDecimation(count, 2 * plotWidth)) {
                    double pixelsPerSecond = plotWidth / (viewMaxX - viewMinX), left = viewMinX;
                    decimation.m4(first, n, values, i -> {
                        double t = curveLast - (n - 1 - i) / frequency;
                        double sweep = Math.floor(t / windowTime);
                        return ((long) sweep << 32) + (long) Math.floor((t - sweep * windowTime - left) * pixelsPerSecond);
                    });
                    kept = decimation.indices();
                    count = decimation.count();
                }
                drawnPoints += count;
                long currentSweep = Long.MIN_VALUE;
                boolean open = false;
                for (int j = 0; j < count; j++) {
                    int i = (kept == null) ? first + j : kept[j];
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
                drawnPoints += n - first;
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

    private void drawPointsScroll(GL2 gl) {
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

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case ' ': togglePause(); break;
            case 'm': toggleDisplayMode(); break;
            default: super.keyPressed(key); break;
        }
    }

    /**
     * Panoramique horizontal et vertical, comme en 2D ; le contenu suit la
     * souris. Sans fenêtre de zoom, le zoom de la molette ne porte que sur y :
     * le déplacement horizontal n'en dépend donc pas.
     */
    @Override
    public void pan(float dx, float dy) {
        if (zoomWindow() != null) {
            super.pan(dx, dy); // la fenêtre de zoom est déplacée sur les deux axes
            return;
        }
        panX -= dx * width / plotWidth;
        super.pan(0f, dy);
    }

    /**
     * Échelle verticale fixe : l'axe vertical va exactement de yMin à yMax,
     * quelles que soient les valeurs des courbes (ce qui dépasse est coupé
     * par le cadre). Zoom et panoramique restent possibles ; R revient à
     * cette échelle.
     */
    public void setFixedScales(double yMin, double yMax) { setFixedScaleBounds(yMin, yMax); }

    /** Choisit le mode d'affichage ; un changement de mode supprime la fenêtre de zoom (abscisses incompatibles). */
    public void setDisplayMode(DisplayMode displayMode) {
        if (displayMode == null || displayMode == this.displayMode) return;
        this.displayMode = displayMode;
        panX = 0f; // le décalage horizontal n'a pas le même sens dans les deux modes
        clearZoomWindow();
    }
    public DisplayMode getDisplayMode() { return displayMode; }
    /** Bascule entre balayage (oscilloscope) et défilement. */
    public void toggleDisplayMode() {
        setDisplayMode((displayMode == DisplayMode.SWEEP) ? DisplayMode.SCROLL : DisplayMode.SWEEP);
    }

    /** Fige / relance l'affichage ; les données continuent d'être reçues pendant la pause. */
    public void togglePause() { paused = !paused; }
}
