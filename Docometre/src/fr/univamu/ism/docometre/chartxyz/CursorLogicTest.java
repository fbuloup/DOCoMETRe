package fr.univamu.ism.docometre.chartxyz;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Test sans OpenGL de la logique du curseur (PlotRenderer, Chart1DRenderer, Chart2DRenderer). */
public class CursorLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK    " : "ÉCHEC ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; }

    /** Composant 800 x 600 ; zone de tracé 700 x 500 à partir de (50, 40) en pixels OpenGL. */
    static void frame(PlotRenderer<?> r) throws Exception {
        r.reshape(null, 0, 0, 800, 600);
        r.marginLeft = 50; r.marginBottom = 40; r.plotWidth = 700; r.plotHeight = 500;
        r.updateData();
        r.computeView();
        Method update = PlotRenderer.class.getDeclaredMethod("updateCursor");
        update.setAccessible(true);
        update.invoke(r);
    }
    /** Position fenêtre (origine en haut à gauche) du point (x, y) des données dans la vue courante. */
    static int[] window(PlotRenderer<?> r, double x, double y) {
        double px = 50 + (x - r.viewMinX) / (r.viewMaxX - r.viewMinX) * 700;
        double py = 40 + (y - r.viewMinY) / (r.viewMaxY - r.viewMinY) * 500;
        return new int[]{(int) Math.round(px), (int) Math.round(600 - py)};
    }
    static void mouse(PlotRenderer<?> r, double x, double y) {
        int[] w = window(r, x, y);
        r.move(w[0], w[1], 800, 600);
    }

    public static void main(String[] args) throws Exception {
        // ---------------- 2D : deux trajectoires ----------------
        List<Point2D> line = new ArrayList<>(), other = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            line.add(new Point2D(i, i / 2f));
            other.add(new Point2D(i, 5 - i / 2f));
        }
        Chart2DRenderer r2 = new Chart2DRenderer(new Curve2DData(line), new Curve2DData(other));
        List<float[]> events = new ArrayList<>();
        r2.addCursorListener(events::add);
        frame(r2);
        mouse(r2, 3.1, 1.6);
        frame(r2);
        check("2D curseur désactivé par défaut : aucun point, aucun appel", r2.getCursorPosition() == null && events.isEmpty());
        check("2D Tabulation non utilisée tant que le curseur est désactivé", !r2.usesTabKey());

        r2.keyPressed('c');
        frame(r2);
        float[] p = r2.getCursorPosition();
        check("2D touche C : point le plus proche de la première courbe = (3, 1.5)", p != null && near(p[0], 3) && near(p[1], 1.5));
        check("2D écouteur appelé avec {3, 1.5}", events.size() == 1 && near(events.get(0)[0], 3) && near(events.get(0)[1], 1.5));
        frame(r2);
        check("2D souris immobile, données inchangées : pas de nouvel appel", events.size() == 1);
        check("2D Tabulation utilisée (curseur actif, 2 courbes)", r2.usesTabKey());

        r2.keyPressed(ChartInteraction.TAB);
        frame(r2);
        p = r2.getCursorPosition();
        // Distance à l'écran depuis la souris en (3.1, 1.6), échelles 63.6 px/unité en x et 90.9 en y :
        // (4, 3) à 140 px, (5, 2.5) à 146 px, (3, 3.5) à 173 px
        check("2D Tabulation : seconde courbe, point le plus proche à l'écran = (4, 3)", r2.getCursorCurveIndex() == 1 && near(p[0], 4) && near(p[1], 3));
        r2.keyPressed(ChartInteraction.TAB);
        check("2D Tabulation en boucle : retour à la première courbe", r2.getCursorCurveIndex() == 0);
        r2.keyPressed(ChartInteraction.BACK_TAB);
        check("2D Maj + Tabulation : courbe précédente (la seconde)", r2.getCursorCurveIndex() == 1);

        mouse(r2, 8.2, 1.0);
        frame(r2);
        p = r2.getCursorPosition();
        check("2D la souris bouge : point le plus proche de la seconde courbe = (8, 1)", near(p[0], 8) && near(p[1], 1));

        r2.setZoomWindow(0, 4, 0, 5);
        frame(r2);
        p = r2.getCursorPosition();
        check("2D zoom : seuls les points visibles sont candidats (x <= 4)", p != null && p[0] <= 4);

        r2.keyPressed('c');
        frame(r2);
        check("2D touche C à nouveau : curseur désactivé, écouteur appelé avec null", r2.getCursorPosition() == null && events.get(events.size() - 1) == null);
        r2.keyPressed('c');
        check("2D réactivation : retour à la première courbe", r2.getCursorCurveIndex() == 0);

        // ---------------- 1D défilement ----------------
        Curve1DData signal = new Curve1DData(100);
        Chart1DRenderer r1 = new Chart1DRenderer(5, signal);
        float[] samples = new float[1001];
        for (int i = 0; i < samples.length; i++) samples[i] = (float) Math.sin(i / 50.0);
        signal.addSamples(samples); // 10 s
        r1.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        r1.setCursorEnabled(true);
        frame(r1);
        double t = 7.5, v = Math.sin(750 / 50.0);
        mouse(r1, t, v);
        frame(r1);
        p = r1.getCursorPosition();
        check("1D défilement : point désigné à t = 7.50 s", p != null && near(p[0], 7.5) && near(p[1], v));
        check("1D abscisse affichée avec la résolution de 100 Hz : \"7.50\"", "7.50".equals(r1.formatCursorX(signal, p[0])));
        check("1D une seule courbe : Tabulation laissée à SWT", !r1.usesTabKey());
        mouse(r1, 6.2, r1.viewMaxY); // tout en haut de la zone de tracé, loin de la courbe
        frame(r1);
        float[] top = r1.getCursorPosition();
        check("1D même abscisse que la souris, quelle que soit sa hauteur : t = 6.20 s", top != null && near(top[0], 6.2) && near(top[1], Math.sin(620 / 50.0)));
        mouse(r1, t, v);
        frame(r1);
        signal.addSamples(new float[100]); // une seconde de zéros : la courbe défile sous la souris
        frame(r1);
        float[] q = r1.getCursorPosition();
        check("1D la courbe défile sous une souris immobile : le point désigné change", q != null && !(near(q[0], p[0]) && near(q[1], p[1])));

        // ---------------- 1D : panoramique horizontal sans fenêtre de zoom ----------------
        r1.setCursorEnabled(false);
        frame(r1);
        double minBefore = r1.viewMinX, maxBefore = r1.viewMaxX, yBefore = r1.viewMinY;
        r1.drag(ChartInteraction.RIGHT_BUTTON, 140, 0, 800, 600, ChartInteraction.InputScale.SWT); // 140 px vers la droite
        frame(r1);
        double expectedShift = 140.0 / 700 * 5; // 140 px de la zone de tracé (700 px pour 5 s)
        check("1D bouton droit vers la droite : vue décalée de 1 s vers les instants anciens", near(r1.viewMinX, minBefore - expectedShift) && near(r1.viewMaxX, maxBefore - expectedShift) && near(r1.viewMinY, yBefore));
        r1.wheel(3, ChartInteraction.InputScale.SWT); // zoom vertical : ne change pas la vitesse du panoramique horizontal
        frame(r1);
        double min2 = r1.viewMinX;
        r1.drag(ChartInteraction.RIGHT_BUTTON, 140, 0, 800, 600, ChartInteraction.InputScale.SWT);
        frame(r1);
        check("1D après zoom molette (y seulement), même décalage horizontal", near(r1.viewMinX, min2 - expectedShift));
        r1.keyPressed('r');
        frame(r1);
        check("1D R : retour à la vue initiale (décalage annulé)", near(r1.viewMaxX, Math.max(5, signal.getLastSampleTime())));

        // ---------------- 1D balayage : l'espace vide n'est pas désignable ----------------
        Curve1DData sweep = new Curve1DData(100);
        Chart1DRenderer rs = new Chart1DRenderer(5, sweep);
        float[] ramp = new float[801];
        for (int i = 0; i < ramp.length; i++) ramp[i] = i / 100f; // 8 s : dernier échantillon à 8 s, curseur d'écriture à 3 s
        sweep.addSamples(ramp);
        rs.setCursorEnabled(true);
        frame(rs);
        int first = rs.firstCursorPoint(sweep);
        mouse(rs, 3.05, 3.05); // juste après le curseur d'écriture : dans l'espace vide
        frame(rs);
        p = rs.getCursorPosition();
        // 501 échantillons de t = 3 à 8 s ; l'espace vide (0,1 s) couvre t = 3,00 à 3,09 : 10 échantillons exclus
        check("1D balayage : les 10 échantillons de l'espace vide sont exclus (1er désignable = 10)", first == 10);
        check("1D balayage : le point désigné n'est pas dans l'espace vide (x hors ]3, 3.1[)", p != null && !(p[0] > 3.0001 && p[0] < 3.0999));

        System.out.println(failures == 0 ? "\nTous les tests passent." : "\n" + failures + " échec(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
