package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test sans OpenGL de la logique du zoom par rectangle (PlotRenderer, Chart1DRenderer, Chart2DRenderer). */
public class ZoomLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK    " : "ÉCHEC ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; }

    /** Composant 800 x 600 ; zone de tracé 700 x 500 à partir de (50, 40) en pixels OpenGL. */
    static void layout(PlotRenderer<?> r) {
        r.reshape(null, 0, 0, 800, 600);
        r.marginLeft = 50; r.marginBottom = 40; r.plotWidth = 700; r.plotHeight = 500;
        r.updateData();
        r.computeView();
    }
    /** Coordonnée y fenêtre (origine en haut) d'un point de la zone de tracé à py pixels au-dessus de son bas. */
    static int wy(int py) { return 600 - (40 + py); }

    public static void main(String[] args) {
        // ---------------- 2D ----------------
        Chart2DRenderer r2 = new Chart2DRenderer(new Curve2DData(Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        List<double[]> events = new ArrayList<>();
        r2.addZoomListener(w -> events.add(w));
        layout(r2);
        double vx0 = r2.viewMinX, vx1 = r2.viewMaxX, vy0 = r2.viewMinY, vy1 = r2.viewMaxY;
        check("2D vue initiale x = [-0.5, 10.5]", near(vx0, -0.5) && near(vx1, 10.5));

        // Rectangle de (70, 100) à (420, 400) pixels dans la zone de tracé, tracé de bas-droite vers haut-gauche
        r2.press(ChartInteraction.LEFT_BUTTON, 50 + 420, wy(400), 800, 600);
        r2.dragTo(ChartInteraction.LEFT_BUTTON, 50 + 200, wy(250), 0, 0, 800, 600, ChartInteraction.InputScale.SWT);
        r2.release(ChartInteraction.LEFT_BUTTON, 50 + 70, wy(100), 800, 600);
        double[] w = r2.getZoomWindow();
        double ex0 = vx0 + 0.1 * (vx1 - vx0), ex1 = vx0 + 0.6 * (vx1 - vx0);
        double ey0 = vy0 + 0.2 * (vy1 - vy0), ey1 = vy0 + 0.8 * (vy1 - vy0);
        check("2D fenêtre = rectangle tracé (coins triés)", w != null && near(w[0], ex0) && near(w[1], ex1) && near(w[2], ey0) && near(w[3], ey1));
        check("2D écouteur appelé une fois avec la fenêtre", events.size() == 1 && Arrays.equals(events.get(0), w));
        r2.computeView();
        check("2D vue = fenêtre de zoom", near(r2.viewMinX, ex0) && near(r2.viewMaxX, ex1) && near(r2.viewMinY, ey0) && near(r2.viewMaxY, ey1));

        // Clic simple (< 5 pixels) : rien ne change
        r2.press(ChartInteraction.LEFT_BUTTON, 300, 300, 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 302, 303, 800, 600);
        check("2D clic simple sans effet", Arrays.equals(r2.getZoomWindow(), w) && events.size() == 1);

        // Rectangle horizontal très plat : zoom sur x seulement
        r2.press(ChartInteraction.LEFT_BUTTON, 50 + 0, wy(250), 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 50 + 350, wy(252), 800, 600);
        double[] w2 = r2.getZoomWindow();
        check("2D rectangle plat : x zoomé, y inchangé", near(w2[0], ex0) && near(w2[1], (ex0 + ex1) / 2) && near(w2[2], ey0) && near(w2[3], ey1));

        // Appui hors de la zone de tracé : pas de sélection
        r2.press(ChartInteraction.LEFT_BUTTON, 10, 10, 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 400, 300, 800, 600);
        check("2D appui hors zone de tracé ignoré", Arrays.equals(r2.getZoomWindow(), w2));

        // Échap pendant le tracé
        r2.computeView();
        r2.press(ChartInteraction.LEFT_BUTTON, 100, 100, 800, 600);
        r2.keyPressed(ChartInteraction.ESCAPE);
        r2.release(ChartInteraction.LEFT_BUTTON, 400, 400, 800, 600);
        check("2D Échap annule le rectangle", Arrays.equals(r2.getZoomWindow(), w2));

        // Molette et panoramique (bouton droit) agissent sur la fenêtre, sans historique
        r2.wheel(1, ChartInteraction.InputScale.SWT);
        double[] w3 = r2.getZoomWindow();
        check("2D molette : fenêtre réduite autour de son centre", near((w3[0] + w3[1]) / 2, (w2[0] + w2[1]) / 2) && near((w3[1] - w3[0]) * 1.15, w2[1] - w2[0]));
        r2.drag(ChartInteraction.RIGHT_BUTTON, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        double[] w4 = r2.getZoomWindow();
        check("2D panoramique : fenêtre décalée vers la gauche", w4[0] < w3[0] && near(w4[1] - w4[0], w3[1] - w3[0]));
        r2.dragTo(ChartInteraction.LEFT_BUTTON, 500, 300, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        check("2D glisser gauche sans sélection : plus de panoramique", Arrays.equals(r2.getZoomWindow(), w4));

        // Historique : U revient à w (le zoom avant w2), Retour arrière à la vue initiale
        r2.keyPressed('u');
        check("2D U : fenêtre précédente", Arrays.equals(r2.getZoomWindow(), w));
        r2.keyPressed(ChartInteraction.BACKSPACE);
        check("2D Retour arrière : vue initiale", r2.getZoomWindow() == null && events.get(events.size() - 1) == null);
        check("2D historique vide ensuite", !r2.undoZoom());

        // R efface fenêtre et historique
        r2.setZoomWindow(1, 2, 1, 2);
        r2.setZoomWindow(1, 1.5, 1, 1.5);
        r2.keyPressed('r');
        check("2D R : vue initiale et historique vide", !r2.isZoomed() && !r2.undoZoom());
        r2.setZoomWindow(1, 2, 1, 2);
        r2.setFixedScales(-1, 1, -1, 1);
        check("2D setFixedScales supprime la fenêtre", !r2.isZoomed());
        boolean refused = false;
        try { r2.setZoomWindow(1e9, 1e9 + 1e-3, 0, 1); } catch (IllegalArgumentException e) { refused = true; }
        check("2D fenêtre trop petite refusée", refused);

        // ---------------- 1D défilement : la loupe suit le temps ----------------
        Curve1DData signal = new Curve1DData(100);
        Chart1DRenderer r1 = new Chart1DRenderer(5, signal);
        List<double[]> events1 = new ArrayList<>();
        r1.addZoomListener(w1 -> events1.add(w1));
        float[] samples = new float[1000];
        for (int i = 0; i < samples.length; i++) samples[i] = (float) Math.sin(i / 50.0);
        signal.addSamples(samples); // 10 s : dernier échantillon à 9,99 s
        r1.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        layout(r1);
        check("1D défilement : vue x = [4.99, 9.99]", near(r1.viewMinX, 4.99) && near(r1.viewMaxX, 9.99));
        // Rectangle de 7,99 s à 8,99 s (pixels 420 à 560 sur 700)
        r1.press(ChartInteraction.LEFT_BUTTON, 50 + 420, wy(100), 800, 600);
        r1.release(ChartInteraction.LEFT_BUTTON, 50 + 560, wy(400), 800, 600);
        double[] z = r1.getZoomWindow();
        check("1D fenêtre x relative au dernier échantillon = [-2, -1]", z != null && near(z[0], -2) && near(z[1], -1));
        signal.addSamples(new float[100]); // une seconde de plus
        r1.updateData();
        r1.computeView();
        check("1D la fenêtre suit le temps : vue x = [8.99, 9.99]", near(r1.viewMinX, 8.99) && near(r1.viewMaxX, 9.99));
        double[] before = r1.getZoomWindow();
        r1.wheel(1, ChartInteraction.InputScale.SWT);
        double[] after = r1.getZoomWindow();
        check("1D molette : y seulement", after[0] == before[0] && after[1] == before[1] && after[3] - after[2] < before[3] - before[2]);
        r1.drag(ChartInteraction.RIGHT_BUTTON, 100, 50, 800, 600, ChartInteraction.InputScale.SWT);
        double[] panned = r1.getZoomWindow();
        check("1D panoramique : vertical seulement", panned[0] == after[0] && panned[2] != after[2]);
        r1.keyPressed('m');
        check("1D touche M : fenêtre supprimée, écouteur prévenu", !r1.isZoomed() && events1.get(events1.size() - 1) == null);
        r1.updateData();
        r1.computeView();
        check("1D balayage après M : vue x = [0, 5]", near(r1.viewMinX, 0) && near(r1.viewMaxX, 5));
        r1.press(ChartInteraction.LEFT_BUTTON, 50 + 140, wy(100), 800, 600);
        r1.release(ChartInteraction.LEFT_BUTTON, 50 + 280, wy(400), 800, 600);
        double[] s = r1.getZoomWindow();
        check("1D balayage : fenêtre x en position dans le balayage = [1, 2]", s != null && near(s[0], 1) && near(s[1], 2));

        System.out.println(failures == 0 ? "\nTous les tests passent." : "\n" + failures + " échec(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
