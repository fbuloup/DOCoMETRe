package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Base des renderers de courbes temps réel (1D, 2D et 3D) : liste des
 * courbes (modifiable depuis n'importe quel thread), instantané des données
 * à chaque image, texte (polices, légende, valeurs courantes, cadence
 * d'affichage), réduction du nombre de points, échelles fixes ou
 * dynamiques, durée de l'historique imposée aux courbes. Commandes clavier,
 * en plus de celles de {@link GLChartRenderer} :
 * <ul>
 * <li>P : afficher/masquer les points</li>
 * <li>N : afficher/masquer la légende</li>
 * <li>F : afficher/masquer la cadence d'affichage (images/s)</li>
 * <li>V : afficher/masquer les valeurs courantes des courbes</li>
 * <li>D : activer/désactiver la réduction du nombre de points tracés</li>
 * </ul>
 *
 * @param <C> type des courbes affichées
 */
public abstract class CurveChartRenderer<C extends CurveData<C>> extends GLChartRenderer {

    /** Distance minimale à l'écran (pixels) entre deux points tracés, quand la réduction par distance est activée. */
    private static final double MIN_POINT_DISTANCE = 0.5;

    // Courbes affichées ; la liste peut être modifiée depuis n'importe quel thread
    private final List<C> curves = new CopyOnWriteArrayList<>();
    protected List<C> frameCurves = new ArrayList<>(); // courbes figées pour l'image en cours

    protected final GLText text = new GLText(); // texte en police du système (graduations, noms d'axes, légende)
    private final float pointSize;

    // Polices réglables (voir setScaleFont / setLegendFont)
    protected volatile ChartFont scaleFont = new ChartFont(9, SWT.NORMAL);
    protected volatile ChartFont legendFont = new ChartFont(9, SWT.NORMAL);
    protected volatile boolean showLegend = true; // légende affichée (si au moins une courbe a un nom)
    protected volatile boolean showPoints = false;

    private final FpsCounter fpsCounter = new FpsCounter(); // cadence d'affichage réelle
    private volatile boolean showFps = false;
    private int displayedPoints;      // valeur affichée avec la cadence (mise à jour en même temps qu'elle)
    private double displayedFps = -1;
    protected int drawnPoints;        // points envoyés à OpenGL pendant l'image en cours

    protected final Decimation decimation = new Decimation(); // réduction du nombre de points envoyés à OpenGL
    private volatile boolean decimationEnabled = true;
    private final CurrentValues currentValues = new CurrentValues(); // textes des valeurs courantes
    private volatile boolean showCurrentValues = false;
    protected final DashedLines dashedLines = new DashedLines(); // tirets ancrés sur les données

    // Durée de l'historique (s) imposée à toutes les courbes ; 0 = chaque courbe garde la sienne
    private volatile double windowTimeWidth;

    // Échelles fixes {min1, max1, min2, max2...}, ou null pour des échelles dynamiques (ajustées aux données)
    private volatile double[] fixedScales;

    /**
     * @param pointSize taille des points (touche P), en pixels
     */
    @SafeVarargs
    protected CurveChartRenderer(float pointSize, C... curves) {
        super(false, true);
        this.pointSize = pointSize;
        try {
            for (C curve : curves) addCurve(curve);
        } catch (RuntimeException e) {
            removeAllCurves(); // graphe non créé : les courbes déjà rattachées sont libérées
            throw e;
        }
    }

    /**
     * Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread).
     *
     * @throws IllegalStateException si la courbe est déjà affichée dans un graphe (celui-ci compris)
     */
    public void addCurve(C curve) {
        curve.attach(this);
        double window = windowTimeWidth;
        if (window > 0) curve.setWindowTimeWidth(window);
        curves.add(curve);
    }

    /**
     * Durée de l'historique, en secondes, imposée à toutes les courbes du
     * graphe, présentes et futures (leur capacité devient fréquence x durée ;
     * les échantillons les plus récents sont conservés). En 1D, c'est la
     * largeur de l'axe du temps ; en 2D et 3D, la longueur des traînées.
     * Utilisable pendant l'affichage, depuis n'importe quel thread.
     */
    public void setWindowTimeWidth(double windowTimeWidth) {
        if (!(windowTimeWidth > 0) || Double.isInfinite(windowTimeWidth)) {
            throw new IllegalArgumentException("windowTimeWidth doit être strictement positive.");
        }
        this.windowTimeWidth = windowTimeWidth;
        for (C curve : getCurves()) curve.setWindowTimeWidth(windowTimeWidth);
    }

    /** Durée de l'historique imposée aux courbes, en secondes (0 si chaque courbe garde la sienne). */
    public double getWindowTimeWidth() { return windowTimeWidth; }

    /** Retire une courbe du graphe ; elle peut ensuite être ajoutée à un autre graphe. */
    public void removeCurve(C curve) {
        if (curves.remove(curve)) curve.detach(this);
    }

    /** Retire toutes les courbes du graphe (appelé à sa fermeture). */
    public void removeAllCurves() {
        for (C curve : curves) removeCurve(curve);
    }
    /** Courbes actuellement affichées. */
    public List<C> getCurves() { return new ArrayList<>(curves); }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        drawable.getGL().getGL2().glPointSize(pointSize);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        text.dispose(drawable.getGL().getGL2()); // textures des textes
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        text.setPixelScale(GLText.surfaceScale(drawable));
    }

    /** À appeler au début de chaque image : mesure de la cadence et remise à zéro du compte de points. */
    protected void beginFrame() {
        fpsCounter.frame();
        drawnPoints = 0;
    }

    /** Fige les courbes et les données reçues depuis l'image précédente. */
    protected void snapshotCurves() {
        frameCurves = new ArrayList<>(curves);
        for (C curve : frameCurves) curve.takeSnapshot();
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case 'p': togglePoints(); break;
            case 'n': toggleLegend(); break;
            case 'f': toggleFps(); break;
            case 'v': toggleCurrentValues(); break;
            case 'd': toggleDecimation(); break;
            default: super.keyPressed(key); break;
        }
    }

    /** Réduction appliquée si elle est activée, si les points ne sont pas affichés et si la courbe a plus de minCount points. */
    protected boolean useDecimation(int pointCount, int minCount) {
        return decimationEnabled && !showPoints && pointCount > minCount;
    }

    // ------------------------------------------------------------------
    // Habillage commun (coordonnées en pixels)

    /** Cadence d'affichage réelle et nombre de points tracés, alignés à droite sur xRight, dans la police des échelles. */
    protected void drawFps(GL2 gl, float xRight, float y) {
        if (!showFps) return;
        double fps = fpsCounter.getFps();
        if (fps != displayedFps) { // le nombre de points est mis à jour en même temps que la cadence (2 fois par seconde)
            displayedFps = fps;
            displayedPoints = drawnPoints;
        }
        String label = String.format(Locale.US, "%.1f i/s · %d pts", fps, displayedPoints);
        gl.glColor3f(1f, 0.75f, 0.2f);
        text.draw(gl, label, scaleFont, xRight - text.width(label, scaleFont), y);
    }

    /**
     * Valeurs courantes (le dernier échantillon de chaque courbe), alignées à
     * droite sur xRight sous yTop, dans la couleur de chaque courbe et la
     * police des échelles.
     *
     * @return la hauteur occupée en pixels (0 si rien n'est affiché)
     */
    protected float drawCurrentValues(GL2 gl, float xRight, float yTop) {
        if (!showCurrentValues) return 0;
        List<C> curves = frameCurves;
        String[] labels = currentValues.update(text, scaleFont, curves, k -> curves.get(k).formatLastSample());
        return CurveStyles.drawCurrentValues(gl, text, scaleFont, curves, labels, xRight, yTop);
    }

    // ------------------------------------------------------------------
    // Tracé des trajectoires (courbes 2D et 3D)

    /** Projection à l'écran (pixels) d'un point des données ; false si elle est inconnue (point derrière la caméra). */
    protected interface ScreenProjection {
        boolean project(float x, float y, float z, double[] out);
    }

    /**
     * Dessine les courbes 2D ou 3D. Une courbe seule et sans couleur explicite
     * garde le dégradé bleu -> orange ; sinon chaque courbe a sa couleur
     * (explicite ou de la palette) et s'estompe vers ses échantillons les
     * plus anciens. Les courbes en tirets ont un motif ancré sur les données ;
     * les autres sont réduites (si la réduction est activée) en ne gardant
     * que les points distants d'au moins MIN_POINT_DISTANCE pixel à l'écran.
     *
     * @param projection projection des points à l'écran (z = 0 en 2D)
     * @param scaleX     échelle horizontale d'affichage, sert à détecter les changements d'échelle des tirets
     * @param scaleY     échelle verticale d'affichage
     */
    protected void drawTrajectories(GL2 gl, float defaultLineWidth, ScreenProjection projection, double scaleX, double scaleY) {
        for (int k = 0; k < frameCurves.size(); k++) {
            C curve = frameCurves.get(k);
            float width = CurveStyles.lineWidthOf(curve, defaultLineWidth);
            gl.glLineWidth(width);
            float[] c = curve.getSnapshotData();
            int dim = curve.getDimension();
            int n = curve.getSnapshotCount();
            boolean gradient = curve.getColor() == null && frameCurves.size() == 1;
            float[] color = CurveStyles.colorOf(curve, k);
            Decimation.ScreenPosition position = (i, out) ->
                    projection.project(c[dim * i], c[dim * i + 1], (dim > 2) ? c[dim * i + 2] : 0f, out);

            if (CurveStyles.isDashed(curve)) {
                double[] a = new double[2], b = new double[2];
                dashedLines.draw(gl, curve, width, curve.getSnapshotCapacity(), curve.getSnapshotTotalSamples(), n, 0, scaleX, scaleY,
                        i -> (position.project(i - 1, a) && position.project(i, b)) ? Math.hypot(b[0] - a[0], b[1] - a[1]) : 0,
                        null,
                        (i, f) -> {
                            CurveStyles.vertexColor(gl, gradient, color, (n <= 1) ? 1f : (i - 1 + f) / (n - 1));
                            interpolatedVertex(gl, c, dim, i, f);
                        });
                drawnPoints += n;
            } else {
                int[] kept = null;
                int count = n;
                if (useDecimation(n, 2)) {
                    decimation.screenDistance(n, MIN_POINT_DISTANCE, position);
                    kept = decimation.indices();
                    count = decimation.count();
                }
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < count; j++) {
                    int i = (kept == null) ? j : kept[j];
                    CurveStyles.vertexColor(gl, gradient, color, (n <= 1) ? 1f : (float) i / (n - 1));
                    vertex(gl, c, dim, i);
                }
                gl.glEnd();
                drawnPoints += count;
            }
        }
    }

    /** Points de toutes les courbes 2D ou 3D, en blanc. */
    protected void drawTrajectoryPoints(GL2 gl) {
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_POINTS);
        for (C curve : frameCurves) {
            float[] c = curve.getSnapshotData();
            int dim = curve.getDimension();
            for (int i = 0; i < curve.getSnapshotCount(); i++) vertex(gl, c, dim, i);
        }
        gl.glEnd();
    }

    /** Sommet i d'un tableau de coordonnées entrelacées (2 ou 3 par point). */
    private static void vertex(GL2 gl, float[] c, int dim, int i) {
        int k = dim * i;
        if (dim == 2) gl.glVertex2f(c[k], c[k + 1]);
        else gl.glVertex3f(c[k], c[k + 1], c[k + 2]);
    }

    /** Sommet situé entre les points i-1 et i, à la fraction f (0 = i-1, 1 = i). */
    private static void interpolatedVertex(GL2 gl, float[] c, int dim, int i, float f) {
        int a = dim * (i - 1), b = dim * i;
        float x = c[a] + f * (c[b] - c[a]);
        float y = c[a + 1] + f * (c[b + 1] - c[a + 1]);
        if (dim == 2) gl.glVertex2f(x, y);
        else gl.glVertex3f(x, y, c[a + 2] + f * (c[b + 2] - c[a + 2]));
    }

    // ------------------------------------------------------------------
    // Échelles

    /** Mémorise des échelles fixes {min1, max1, min2, max2...}, après vérification. */
    protected void setFixedScaleBounds(double... bounds) {
        fixedScales = CurveStyles.checkScales(bounds);
    }
    /** Échelles fixes courantes, ou null si elles sont dynamiques. */
    protected double[] getFixedScaleBounds() { return fixedScales; }
    /** Échelles dynamiques, ajustées en continu aux valeurs des courbes (par défaut). */
    public void setDynamicScales() { fixedScales = null; }
    /** Vrai si les échelles sont fixes. */
    public boolean hasFixedScales() { return fixedScales != null; }

    // ------------------------------------------------------------------
    // Réglages

    /** Police des graduations des échelles. */
    public void setScaleFont(ChartFont font) { if (font != null) scaleFont = font; }
    public ChartFont getScaleFont() { return scaleFont; }
    /** Police de la légende. */
    public void setLegendFont(ChartFont font) { if (font != null) legendFont = font; }
    public ChartFont getLegendFont() { return legendFont; }

    public void togglePoints() { showPoints = !showPoints; }

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

    /**
     * Active ou désactive la réduction du nombre de points envoyés à OpenGL
     * (activée par défaut, touche D). Le dessin à l'écran est inchangé ; les
     * courbes en tirets et l'affichage des points (touche P) ne sont pas réduits.
     */
    public void setDecimationEnabled(boolean enabled) { decimationEnabled = enabled; }
    public boolean isDecimationEnabled() { return decimationEnabled; }
    public void toggleDecimation() { decimationEnabled = !decimationEnabled; }
    /** Nombre de points envoyés à OpenGL pour tracer les courbes lors de la dernière image. */
    public int getDrawnPointCount() { return drawnPoints; }

    /** Affiche ou masque les valeurs courantes des courbes, en haut à droite (touche V). */
    public void setCurrentValuesVisible(boolean visible) { showCurrentValues = visible; }
    public boolean isCurrentValuesVisible() { return showCurrentValues; }
    public void toggleCurrentValues() { showCurrentValues = !showCurrentValues; }

	public int getCurvesNumber() {
		return curves.size();
	}
}
