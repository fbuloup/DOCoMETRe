package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

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
 * Zoom par rectangle : bouton gauche enfoncé, on trace à la souris la
 * fenêtre à afficher (dessinée pendant le déplacement), qui est appliquée
 * au relâchement. Un rectangle très étroit dans une direction ne zoome que
 * sur l'autre axe. Cette fenêtre de zoom est exprimée en unités des données
 * (en 1D, mode défilement, abscisses relatives au dernier échantillon :
 * la fenêtre suit le temps) ; tant qu'elle existe, molette et panoramique la
 * modifient. Commandes, en plus de celles de {@link CurveChartRenderer} :
 * <ul>
 * <li>bouton gauche + glisser : zoom par rectangle</li>
 * <li>bouton droit + glisser : panoramique</li>
 * <li>Échap : annule le rectangle en cours</li>
 * <li>U ou Retour arrière : revient à la fenêtre précédente</li>
 * <li>R : revient à la vue initiale (efface aussi l'historique)</li>
 * </ul>
 *
 * Curseur (touche C, désactivé par défaut) : un axe vertical et un axe
 * horizontal, dans la couleur de la courbe suivie, se croisent sur le point
 * de cette courbe le plus proche de la souris (distance à l'écran, parmi les
 * échantillons visibles ; en 1D, le point de même abscisse que la souris,
 * voir {@link #cursorFollowsXOnly()}). Ses coordonnées sont affichées dans des étiquettes
 * placées au pied de chaque axe du curseur, sur les axes des abscisses et des
 * ordonnées. Le curseur suit d'abord la première courbe ; Tabulation passe à
 * la suivante, Maj + Tabulation à la précédente. Les {@link CursorListener}
 * reçoivent les coordonnées {x, y} du point désigné.
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

    /** En dessous de cette taille (pixels) dans une direction, le rectangle ne zoome pas sur cet axe. */
    private static final float MIN_SELECTION_PIXELS = 5f;
    /** Nombre maximal de fenêtres mémorisées pour U / Retour arrière. */
    private static final int MAX_ZOOM_HISTORY = 50;
    /** Marque de la vue initiale dans l'historique (ArrayDeque n'accepte pas null). */
    private static final double[] INITIAL_VIEW = new double[0];

    private volatile LegendPosition legendPosition = LegendPosition.BELOW;
    private float legendBelowHeight = 0; // hauteur de la légende sous l'axe (pixels), 0 si absente

    // Vue : zoom (1 = ajustement automatique) et panoramique (en fraction de l'étendue non zoomée)
    protected volatile float zoom = 1f;
    protected volatile float panX = 0f, panY = 0f;

    // Fenêtre de zoom {xMin, xMax, yMin, yMax}, null = vue initiale. Jamais modifiée
    // en place (remplacée en entier) ; modifiée sous viewLock, avec l'historique.
    private volatile double[] zoomWindow;
    private final Deque<double[]> zoomHistory = new ArrayDeque<>();
    private final Object viewLock = new Object();
    private final List<ZoomListener> zoomListeners = new CopyOnWriteArrayList<>();

    // Curseur (touche C) : réglages modifiables depuis n'importe quel thread
    private volatile boolean cursorEnabled;
    private volatile int cursorCurveIndex;     // rang de la courbe suivie (0 = première)
    private volatile float[] cursorPosition;   // {x, y} du point désigné, null si aucun (jamais modifié en place)
    private final List<CursorListener> cursorListeners = new CopyOnWriteArrayList<>();
    // Thread d'affichage uniquement : souris (pixels OpenGL, < 0 = inconnue) et point trouvé à l'image courante
    private float mouseX = -1, mouseY = -1;
    private boolean cursorFound;
    private float cursorPixelX, cursorPixelY;
    private float[] cursorColor;
    private String cursorLabelX, cursorLabelY;

    // Rectangle de zoom en cours de tracé, en pixels OpenGL (thread d'affichage uniquement)
    private boolean selecting;
    private float selectionX0, selectionY0, selectionX1, selectionY1;

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
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        selecting = false; // le rectangle en cours n'a plus de sens après un redimensionnement
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
        updateCursor();

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
        if (cursorFound) drawCursor(gl);
        if (selecting) drawSelection(gl);
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

    /**
     * Calcule la fenêtre visible (viewMinX... viewMaxY) : la fenêtre de zoom
     * si elle existe (voir {@link #zoomWindow()}), sinon à partir des bornes,
     * du zoom et du panoramique.
     */
    protected abstract void computeView();

    /**
     * Abscisse des données placée à x = 0 dans la projection de la zone de
     * tracé (0 par défaut) : permet de garder la précision des float quand
     * les abscisses sont grandes (temps).
     */
    protected double xOrigin() { return 0; }

    /**
     * Référence des abscisses de la fenêtre de zoom : une abscisse x des
     * données y est mémorisée sous la forme x - zoomXReference() (0 par
     * défaut ; le dernier échantillon en 1D défilement, pour que la fenêtre
     * suive le temps). Appelé dans le thread d'affichage.
     */
    protected double zoomXReference() { return 0; }

    /** La molette zoome-t-elle aussi sur l'axe x quand une fenêtre de zoom existe (oui par défaut) ? */
    protected boolean wheelZoomsX() { return true; }

    /** Premier point de la courbe que le curseur peut désigner (0 par défaut ; en 1D balayage, après l'espace vide). */
    protected int firstCursorPoint(C curve) { return 0; }

    /**
     * Coordonnées {x, y}, dans les unités des axes (celles de viewMinX...
     * viewMaxY), du point i du dernier instantané de la courbe, écrites dans xy.
     *
     * @return false si ce point n'est pas dessiné
     */
    protected abstract boolean cursorPoint(C curve, int i, double[] xy);

    /**
     * Vrai si le curseur désigne le point dont l'abscisse est la plus proche
     * de celle de la souris (sans tenir compte de sa hauteur), faux (par
     * défaut) pour le point le plus proche de la souris à l'écran.
     */
    protected boolean cursorFollowsXOnly() { return false; }

    /** Texte de l'abscisse du curseur (4 chiffres significatifs par défaut). */
    protected String formatCursorX(C curve, double x) { return CurrentValues.format(x); }

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

    /**
     * Axes du curseur (vertical et horizontal, sur toute la zone de tracé),
     * marque au point désigné et étiquettes des coordonnées au pied de chaque
     * axe : x sous la zone de tracé, y à sa gauche (dans les marges quand elles
     * sont assez grandes, sinon juste à l'intérieur de la zone de tracé).
     */
    private void drawCursor(GL2 gl) {
        float left = marginLeft, bottom = marginBottom, right = marginLeft + plotWidth, top = marginBottom + plotHeight;
        float px = cursorPixelX, py = cursorPixelY;
        float[] c = cursorColor;

        gl.glColor4f(c[0], c[1], c[2], 0.8f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2f(px, bottom);
        gl.glVertex2f(px, top);
        gl.glVertex2f(left, py);
        gl.glVertex2f(right, py);
        gl.glEnd();

        // Marque au point désigné : carré plein cerclé de la couleur du fond
        gl.glColor3f(0.10f, 0.11f, 0.13f);
        gl.glRectf(px - 4, py - 4, px + 4, py + 4);
        gl.glColor3f(c[0], c[1], c[2]);
        gl.glRectf(px - 3, py - 3, px + 3, py + 3);

        ChartFont font = scaleFont;
        float h = text.lineHeight(font) + 4;
        float wx = text.width(cursorLabelX, font) + 8;
        float xBadge = Math.max(0, Math.min(width - wx, px - wx / 2));
        float yBadge = (marginBottom >= h + 2) ? marginBottom - h - 1 : marginBottom + 1;
        drawBadge(gl, cursorLabelX, font, c, xBadge, yBadge, wx, h);

        float wy = text.width(cursorLabelY, font) + 8;
        xBadge = (marginLeft >= wy + 2) ? marginLeft - wy - 1 : marginLeft + 1;
        yBadge = Math.max(0, Math.min(height - h, py - h / 2));
        drawBadge(gl, cursorLabelY, font, c, xBadge, yBadge, wy, h);
    }

    /** Étiquette : fond de la couleur de la courbe, texte de la couleur du fond du graphe. */
    private void drawBadge(GL2 gl, String label, ChartFont font, float[] color, float x, float y, float w, float h) {
        gl.glColor4f(color[0], color[1], color[2], 0.95f);
        gl.glRectf(x, y, x + w, y + h);
        gl.glColor3f(0.10f, 0.11f, 0.13f);
        text.draw(gl, label, font, x + 4, y + 2);
    }

    /** Rectangle de zoom en cours de tracé : fond translucide et contour (coordonnées en pixels). */
    private void drawSelection(GL2 gl) {
        float x0 = Math.min(selectionX0, selectionX1), x1 = Math.max(selectionX0, selectionX1);
        float y0 = Math.min(selectionY0, selectionY1), y1 = Math.max(selectionY0, selectionY1);
        gl.glColor4f(0.35f, 0.75f, 1f, 0.15f);
        gl.glRectf(x0, y0, x1, y1);
        gl.glColor4f(0.35f, 0.75f, 1f, 0.9f);
        gl.glLineWidth(1f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(x0, y0);
        gl.glVertex2f(x1, y0);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x0, y1);
        gl.glEnd();
    }

    // ------------------------------------------------------------------
    // Navigation

    /** Bouton gauche : début d'un rectangle de zoom, s'il commence dans la zone de tracé. */
    @Override
    public void press(int button, int x, int y, int width, int height) {
        float px = toPixelX(x, width), py = toPixelY(y, height);
        mouseX = px;
        mouseY = py;
        if (button != LEFT_BUTTON) return;
        selecting = px >= marginLeft && px <= marginLeft + plotWidth
                 && py >= marginBottom && py <= marginBottom + plotHeight;
        selectionX0 = selectionX1 = px;
        selectionY0 = selectionY1 = py;
    }

    /** Bouton gauche : le second coin du rectangle suit la souris ; bouton droit : panoramique. */
    @Override
    public void dragTo(int button, int x, int y, int dx, int dy, int width, int height, InputScale scale) {
        mouseX = toPixelX(x, width); // le curseur suit aussi la souris pendant un glisser
        mouseY = toPixelY(y, height);
        if (button == LEFT_BUTTON) {
            if (selecting) {
                selectionX1 = clampX(toPixelX(x, width));
                selectionY1 = clampY(toPixelY(y, height));
            }
        } else {
            drag(button, dx, dy, width, height, scale);
        }
    }

    /** Glisser avec le bouton droit : panoramique ; le contenu suit la souris. */
    @Override
    public void drag(int button, int dx, int dy, int width, int height, InputScale scale) {
        if (button == RIGHT_BUTTON) pan((float) dx / Math.max(1, width), (float) dy / Math.max(1, height));
    }

    /**
     * Bouton gauche relâché : le rectangle devient la fenêtre de zoom. Un
     * rectangle de moins de MIN_SELECTION_PIXELS dans une direction garde
     * l'étendue actuelle de cet axe ; dans les deux directions, c'est un
     * simple clic et rien ne change.
     */
    @Override
    public void release(int button, int x, int y, int width, int height) {
        if (button != LEFT_BUTTON || !selecting) return;
        selecting = false;
        selectionX1 = clampX(toPixelX(x, width));
        selectionY1 = clampY(toPixelY(y, height));
        boolean zoomX = Math.abs(selectionX1 - selectionX0) >= MIN_SELECTION_PIXELS;
        boolean zoomY = Math.abs(selectionY1 - selectionY0) >= MIN_SELECTION_PIXELS;
        if (!zoomX && !zoomY) return;

        // Pixels -> données, avec la vue de l'image affichée
        double reference = zoomXReference();
        double xa = viewMinX, xb = viewMaxX, ya = viewMinY, yb = viewMaxY;
        if (zoomX) {
            xa = pixelToDataX(Math.min(selectionX0, selectionX1));
            xb = pixelToDataX(Math.max(selectionX0, selectionX1));
        }
        if (zoomY) {
            ya = pixelToDataY(Math.min(selectionY0, selectionY1));
            yb = pixelToDataY(Math.max(selectionY0, selectionY1));
        }
        double[] window = {xa - reference, xb - reference, ya, yb};
        if (isUsable(window)) changeZoomWindow(current -> window, true);
    }

    /** Souris déplacée sans bouton : position mémorisée pour le curseur. */
    @Override
    public void move(int x, int y, int width, int height) {
        mouseX = toPixelX(x, width);
        mouseY = toPixelY(y, height);
    }

    /** Tabulation utilisée quand le curseur est actif sur un graphe de plusieurs courbes. */
    @Override
    public boolean usesTabKey() {
        return cursorEnabled && getCurvesCount() > 1;
    }

    /** Molette : zoom d'un cran (sur la fenêtre de zoom si elle existe). */
    @Override
    public void wheel(float amount, InputScale scale) {
        zoom(Math.signum(amount));
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case ESCAPE: selecting = false; break;
            case 'u':
            case BACKSPACE: undoZoom(); break;
            case 'c': setCursorEnabled(!cursorEnabled); break;
            case TAB: if (cursorEnabled) selectNextCursorCurve(1); break;
            case BACK_TAB: if (cursorEnabled) selectNextCursorCurve(-1); break;
            default: super.keyPressed(key); break;
        }
    }

    /**
     * Panoramique. dx et dy sont des déplacements exprimés en fraction de la
     * taille du composant (ce qui rend le calcul indépendant de la densité de
     * pixels de l'écran). Le contenu suit la souris. Si une fenêtre de zoom
     * existe, c'est elle qui est déplacée.
     */
    public void pan(float dx, float dy) {
        if (zoomWindow != null) {
            double fx = (double) dx * width / plotWidth, fy = (double) dy * height / plotHeight;
            changeZoomWindow(w -> {
                if (w == null) return null; // fenêtre supprimée entre-temps
                double shiftX = -fx * (w[1] - w[0]), shiftY = fy * (w[3] - w[2]);
                return new double[]{w[0] + shiftX, w[1] + shiftX, w[2] + shiftY, w[3] + shiftY};
            }, false);
            return;
        }
        panX -= dx * width / plotWidth / zoom;
        panY += dy * height / plotHeight / zoom;
    }

    /**
     * Zoom multiplicatif : steps > 0 rapproche, steps < 0 éloigne. Si une
     * fenêtre de zoom existe, elle est réduite ou agrandie autour de son centre.
     */
    public void zoom(float steps) {
        if (zoomWindow != null) {
            double factor = Math.pow(1.15, -steps);
            boolean onX = wheelZoomsX();
            changeZoomWindow(w -> {
                if (w == null) return null; // fenêtre supprimée entre-temps
                double cx = (w[0] + w[1]) / 2, hx = (w[1] - w[0]) / 2 * (onX ? factor : 1);
                double cy = (w[2] + w[3]) / 2, hy = (w[3] - w[2]) / 2 * factor;
                double[] scaled = {cx - hx, cx + hx, cy - hy, cy + hy};
                return isUsable(scaled) ? scaled : w;
            }, false);
            return;
        }
        zoom *= (float) Math.pow(1.15, steps);
        zoom = Math.max(1e-3f, Math.min(1e4f, zoom));
    }

    /** Vue initiale : ajustement automatique (ou échelles fixes), sans fenêtre de zoom ni historique. */
    @Override
    public void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        selecting = false;
        clearZoomWindow();
    }

    // ------------------------------------------------------------------
    // Curseur

    /**
     * Active ou désactive le curseur (également avec la touche C). À
     * l'activation, le curseur suit la première courbe. Utilisable depuis
     * n'importe quel thread.
     */
    public void setCursorEnabled(boolean enabled) {
        if (enabled && !cursorEnabled) cursorCurveIndex = 0;
        cursorEnabled = enabled;
    }

    public boolean isCursorEnabled() { return cursorEnabled; }

    /** Courbe suivie par le curseur, par son rang dans le graphe (0 = première ; un rang absent revient à 0). */
    public void setCursorCurveIndex(int index) { cursorCurveIndex = Math.max(0, index); }

    /** Rang de la courbe suivie par le curseur. */
    public int getCursorCurveIndex() { return cursorCurveIndex; }

    /** Copie des coordonnées {x, y} du point désigné par le curseur, ou null s'il n'y en a pas. */
    public float[] getCursorPosition() {
        float[] position = cursorPosition;
        return (position != null) ? position.clone() : null;
    }

    public void addCursorListener(CursorListener listener) { if (listener != null) cursorListeners.add(listener); }
    public void removeCursorListener(CursorListener listener) { cursorListeners.remove(listener); }

    /** Passe à la courbe suivante (step = 1) ou précédente (step = -1), en boucle. */
    private void selectNextCursorCurve(int step) {
        int count = getCurvesCount();
        if (count > 0) cursorCurveIndex = Math.floorMod(Math.min(cursorCurveIndex, count - 1) + step, count);
    }

    /**
     * Cherche, à chaque image, le point de la courbe suivie le plus proche de
     * la souris (distance à l'écran, parmi les points visibles) : les données
     * et la vue changent aussi sous une souris immobile. Prévient les
     * écouteurs si le point désigné a changé.
     */
    private void updateCursor() {
        cursorFound = false;
        float[] found = null;
        List<C> curves = frameCurves;
        if (cursorEnabled && mouseX >= 0 && !curves.isEmpty()) {
            int index = (cursorCurveIndex < curves.size()) ? cursorCurveIndex : 0;
            C curve = curves.get(index);
            double scaleX = plotWidth / (viewMaxX - viewMinX), scaleY = plotHeight / (viewMaxY - viewMinY);
            double[] xy = new double[2];
            double best = Double.POSITIVE_INFINITY, bestX = 0, bestY = 0;
            boolean xOnly = cursorFollowsXOnly();
            for (int i = firstCursorPoint(curve), n = curve.getSnapshotCount(); i < n; i++) {
                if (!cursorPoint(curve, i, xy)) continue;
                if (xy[0] < viewMinX || xy[0] > viewMaxX || xy[1] < viewMinY || xy[1] > viewMaxY) continue; // hors de la vue
                double dx = marginLeft + (xy[0] - viewMinX) * scaleX - mouseX;
                double dy = marginBottom + (xy[1] - viewMinY) * scaleY - mouseY;
                double d = xOnly ? dx * dx : dx * dx + dy * dy;
                if (d < best) {
                    best = d;
                    bestX = xy[0];
                    bestY = xy[1];
                }
            }
            if (best < Double.POSITIVE_INFINITY) {
                cursorFound = true;
                cursorPixelX = (float) (marginLeft + (bestX - viewMinX) * scaleX);
                cursorPixelY = (float) (marginBottom + (bestY - viewMinY) * scaleY);
                cursorColor = CurveStyles.colorOf(curve, index);
                cursorLabelX = formatCursorX(curve, bestX);
                cursorLabelY = CurrentValues.format(bestY);
                found = new float[]{(float) bestX, (float) bestY};
            }
        }
        if (!Arrays.equals(found, cursorPosition)) {
            cursorPosition = found;
            for (CursorListener listener : cursorListeners) {
                try {
                    listener.cursorMoved((found != null) ? found.clone() : null);
                } catch (RuntimeException e) {
                    e.printStackTrace(); // une erreur dans un écouteur ne doit pas interrompre l'affichage
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Fenêtre de zoom

    /**
     * Fenêtre de zoom courante {xMin, xMax, yMin, yMax}, ou null. À ne pas
     * modifier (le tableau n'est jamais modifié en place : il peut être lu
     * sans verrou dans computeView()).
     */
    protected final double[] zoomWindow() { return zoomWindow; }

    /** Copie de la fenêtre de zoom {xMin, xMax, yMin, yMax}, ou null si la vue est la vue initiale. */
    public double[] getZoomWindow() {
        double[] window = zoomWindow;
        return (window != null) ? window.clone() : null;
    }

    /** Vrai si une fenêtre de zoom est appliquée. */
    public boolean isZoomed() { return zoomWindow != null; }

    /**
     * Applique une fenêtre de zoom (mémorisée dans l'historique). En 1D, mode
     * défilement, les abscisses sont relatives au dernier échantillon (par
     * exemple -2 à -1 pour la seconde avant-dernière), comme celles transmises
     * aux {@link ZoomListener}. Utilisable depuis n'importe quel thread.
     *
     * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
     */
    public void setZoomWindow(double xMin, double xMax, double yMin, double yMax) {
        double[] window = CurveStyles.checkScales(xMin, xMax, yMin, yMax);
        if (!isUsable(window)) throw new IllegalArgumentException("Fenêtre de zoom trop petite pour la précision de calcul.");
        changeZoomWindow(current -> window, true);
    }

    /**
     * Revient à la fenêtre précédente de l'historique (touche U ou Retour arrière).
     *
     * @return false si l'historique est vide
     */
    public boolean undoZoom() {
        double[] restored;
        synchronized (viewLock) {
            if (zoomHistory.isEmpty()) return false;
            double[] previous = zoomHistory.pop();
            restored = (previous == INITIAL_VIEW) ? null : previous;
            zoomWindow = restored;
        }
        fireZoomChanged(restored);
        return true;
    }

    /** Supprime la fenêtre de zoom et l'historique ; les écouteurs sont prévenus si une fenêtre était appliquée. */
    protected void clearZoomWindow() {
        boolean wasZoomed;
        synchronized (viewLock) {
            wasZoomed = zoomWindow != null;
            zoomWindow = null;
            zoomHistory.clear();
        }
        if (wasZoomed) fireZoomChanged(null);
    }

    public void addZoomListener(ZoomListener listener) { if (listener != null) zoomListeners.add(listener); }
    public void removeZoomListener(ZoomListener listener) { zoomListeners.remove(listener); }

    /** Changer d'échelles affiche les nouvelles échelles : la fenêtre de zoom est supprimée. */
    @Override
    protected void setFixedScaleBounds(double... bounds) {
        super.setFixedScaleBounds(bounds);
        clearZoomWindow();
    }

    @Override
    public void setDynamicScales() {
        super.setDynamicScales();
        clearZoomWindow();
    }

    /**
     * Remplace la fenêtre de zoom par change(fenêtre courante), sous le
     * verrou, puis prévient les écouteurs. Si record, la fenêtre remplacée est
     * mémorisée dans l'historique. change reçoit null s'il n'y a pas de
     * fenêtre ; renvoyer la fenêtre reçue ne change rien.
     */
    private void changeZoomWindow(UnaryOperator<double[]> change, boolean record) {
        double[] window;
        synchronized (viewLock) {
            double[] current = zoomWindow;
            window = change.apply(current);
            if (window == current) return;
            if (record) {
                zoomHistory.push((current != null) ? current : INITIAL_VIEW);
                while (zoomHistory.size() > MAX_ZOOM_HISTORY) zoomHistory.removeLast();
            }
            zoomWindow = window;
        }
        fireZoomChanged(window);
    }

    private void fireZoomChanged(double[] window) {
        for (ZoomListener listener : zoomListeners) {
            try {
                listener.zoomChanged((window != null) ? window.clone() : null);
            } catch (RuntimeException e) {
                e.printStackTrace(); // une erreur dans un écouteur ne doit pas interrompre l'affichage
            }
        }
    }

    /** Fenêtre finie dont l'étendue reste mesurable avec la précision des données (float). */
    private static boolean isUsable(double[] w) {
        for (int i = 0; i < 4; i += 2) {
            double span = w[i + 1] - w[i];
            if (!(span > 0) || Double.isInfinite(span)
                    || span < 1e-6 * Math.max(Math.abs(w[i]), Math.abs(w[i + 1]))) return false;
        }
        return true;
    }

    // Conversions : unités de la fenêtre (origine en haut à gauche) -> pixels OpenGL
    // (origine en bas à gauche) -> unités des données. Le rapport this.width / largeur
    // du composant prend en compte les écrans haute densité.

    private float toPixelX(int x, int componentWidth) {
        return (float) x * width / Math.max(1, componentWidth);
    }

    private float toPixelY(int y, int componentHeight) {
        return height - (float) y * height / Math.max(1, componentHeight);
    }

    private float clampX(float px) {
        return Math.max(marginLeft, Math.min(marginLeft + plotWidth, px));
    }

    private float clampY(float py) {
        return Math.max(marginBottom, Math.min(marginBottom + plotHeight, py));
    }

    private double pixelToDataX(float px) {
        return viewMinX + (px - marginLeft) / plotWidth * (viewMaxX - viewMinX);
    }

    private double pixelToDataY(float py) {
        return viewMinY + (py - marginBottom) / plotHeight * (viewMaxY - viewMinY);
    }

    /** Position de la légende : dans le graphe (INSIDE) ou sous l'axe des abscisses (BELOW, par défaut). */
    public void setLegendPosition(LegendPosition position) { if (position != null) legendPosition = position; }
    public LegendPosition getLegendPosition() { return legendPosition; }
}
