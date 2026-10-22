package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test without OpenGL of the rectangle zoom logic (PlotRenderer, Chart1DRenderer, Chart2DRenderer). */
public class ZoomLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; }

    /** 800 x 600 component; 700 x 500 plot area starting at (50, 40) in OpenGL pixels. */
    static void layout(PlotRenderer<?> r) {
        r.reshape(null, 0, 0, 800, 600);
        r.marginLeft = 50; r.marginBottom = 40; r.plotWidth = 700; r.plotHeight = 500;
        r.updateData();
        r.computeView();
    }
    /** Window y coordinate (origin at the top) of a point of the plot area py pixels above its bottom. */
    static int wy(int py) { return 600 - (40 + py); }

    public static void main(String[] args) {
        // ---------------- 2D ----------------
        Chart2DRenderer r2 = new Chart2DRenderer(new Curve2DData(Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        List<double[]> events = new ArrayList<>();
        r2.addZoomListener(w -> events.add(w));
        layout(r2);
        double vx0 = r2.viewMinX, vx1 = r2.viewMaxX, vy0 = r2.viewMinY, vy1 = r2.viewMaxY;
        check("2D initial view x = [-0.5, 10.5]", near(vx0, -0.5) && near(vx1, 10.5));

        // Rectangle from (70, 100) to (420, 400) pixels in the plot area, drawn from bottom right to top left
        r2.press(ChartInteraction.LEFT_BUTTON, 50 + 420, wy(400), 800, 600);
        r2.dragTo(ChartInteraction.LEFT_BUTTON, 50 + 200, wy(250), 0, 0, 800, 600, ChartInteraction.InputScale.SWT);
        r2.release(ChartInteraction.LEFT_BUTTON, 50 + 70, wy(100), 800, 600);
        double[] w = r2.getZoomWindow();
        double ex0 = vx0 + 0.1 * (vx1 - vx0), ex1 = vx0 + 0.6 * (vx1 - vx0);
        double ey0 = vy0 + 0.2 * (vy1 - vy0), ey1 = vy0 + 0.8 * (vy1 - vy0);
        check("2D window = drawn rectangle (sorted corners)", w != null && near(w[0], ex0) && near(w[1], ex1) && near(w[2], ey0) && near(w[3], ey1));
        check("2D listener called once with the window", events.size() == 1 && Arrays.equals(events.get(0), w));
        r2.computeView();
        check("2D view = zoom window", near(r2.viewMinX, ex0) && near(r2.viewMaxX, ex1) && near(r2.viewMinY, ey0) && near(r2.viewMaxY, ey1));

        // Simple click (< 5 pixels): nothing changes
        r2.press(ChartInteraction.LEFT_BUTTON, 300, 300, 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 302, 303, 800, 600);
        check("2D simple click has no effect", Arrays.equals(r2.getZoomWindow(), w) && events.size() == 1);

        // Very flat horizontal rectangle: zoom on x only
        r2.press(ChartInteraction.LEFT_BUTTON, 50 + 0, wy(250), 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 50 + 350, wy(252), 800, 600);
        double[] w2 = r2.getZoomWindow();
        check("2D flat rectangle: x zoomed, y unchanged", near(w2[0], ex0) && near(w2[1], (ex0 + ex1) / 2) && near(w2[2], ey0) && near(w2[3], ey1));

        // Press outside the plot area: no selection
        r2.press(ChartInteraction.LEFT_BUTTON, 10, 10, 800, 600);
        r2.release(ChartInteraction.LEFT_BUTTON, 400, 300, 800, 600);
        check("2D press outside the plot area ignored", Arrays.equals(r2.getZoomWindow(), w2));

        // Escape while drawing
        r2.computeView();
        r2.press(ChartInteraction.LEFT_BUTTON, 100, 100, 800, 600);
        r2.keyPressed(ChartInteraction.ESCAPE);
        r2.release(ChartInteraction.LEFT_BUTTON, 400, 400, 800, 600);
        check("2D Escape cancels the rectangle", Arrays.equals(r2.getZoomWindow(), w2));

        // Wheel and pan (right button) act on the window, without history
        r2.wheel(1, ChartInteraction.InputScale.SWT);
        double[] w3 = r2.getZoomWindow();
        check("2D wheel: window shrunk around its center", near((w3[0] + w3[1]) / 2, (w2[0] + w2[1]) / 2) && near((w3[1] - w3[0]) * 1.15, w2[1] - w2[0]));
        r2.drag(ChartInteraction.RIGHT_BUTTON, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        double[] w4 = r2.getZoomWindow();
        check("2D pan: window shifted to the left", w4[0] < w3[0] && near(w4[1] - w4[0], w3[1] - w3[0]));
        r2.dragTo(ChartInteraction.LEFT_BUTTON, 500, 300, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        check("2D left drag without selection: no more pan", Arrays.equals(r2.getZoomWindow(), w4));

        // History: U goes back to w (the zoom before w2), Backspace to the initial view
        r2.keyPressed('u');
        check("2D U: previous window", Arrays.equals(r2.getZoomWindow(), w));
        r2.keyPressed(ChartInteraction.BACKSPACE);
        check("2D Backspace: initial view", r2.getZoomWindow() == null && events.get(events.size() - 1) == null);
        check("2D history empty afterwards", !r2.undoZoom());

        // R clears window and history
        r2.setZoomWindow(1, 2, 1, 2);
        r2.setZoomWindow(1, 1.5, 1, 1.5);
        r2.keyPressed('r');
        check("2D R: initial view and empty history", !r2.isZoomed() && !r2.undoZoom());
        r2.setZoomWindow(1, 2, 1, 2);
        r2.setFixedScales(-1, 1, -1, 1);
        check("2D setFixedScales removes the window", !r2.isZoomed());
        boolean refused = false;
        try { r2.setZoomWindow(1e9, 1e9 + 1e-3, 0, 1); } catch (IllegalArgumentException e) { refused = true; }
        check("2D window too small rejected", refused);

        // ---------------- 1D scroll: the magnifier follows time ----------------
        Curve1DData signal = new Curve1DData(100);
        Chart1DRenderer r1 = new Chart1DRenderer(5, signal);
        List<double[]> events1 = new ArrayList<>();
        r1.addZoomListener(w1 -> events1.add(w1));
        float[] samples = new float[1000];
        for (int i = 0; i < samples.length; i++) samples[i] = (float) Math.sin(i / 50.0);
        signal.addSamples(samples); // 10 s: last sample at 9.99 s
        r1.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        layout(r1);
        check("1D scroll: view x = [4.99, 9.99]", near(r1.viewMinX, 4.99) && near(r1.viewMaxX, 9.99));
        // Rectangle from 7.99 s to 8.99 s (pixels 420 to 560 out of 700)
        r1.press(ChartInteraction.LEFT_BUTTON, 50 + 420, wy(100), 800, 600);
        r1.release(ChartInteraction.LEFT_BUTTON, 50 + 560, wy(400), 800, 600);
        double[] z = r1.getZoomWindow();
        check("1D window x relative to the last sample = [-2, -1]", z != null && near(z[0], -2) && near(z[1], -1));
        signal.addSamples(new float[100]); // one more second
        r1.updateData();
        r1.computeView();
        check("1D the window follows time: view x = [8.99, 9.99]", near(r1.viewMinX, 8.99) && near(r1.viewMaxX, 9.99));
        double[] before = r1.getZoomWindow();
        r1.wheel(1, ChartInteraction.InputScale.SWT);
        double[] after = r1.getZoomWindow();
        check("1D wheel: y only", after[0] == before[0] && after[1] == before[1] && after[3] - after[2] < before[3] - before[2]);
        r1.drag(ChartInteraction.RIGHT_BUTTON, 100, 50, 800, 600, ChartInteraction.InputScale.SWT);
        double[] panned = r1.getZoomWindow();
        check("1D pan with zoom window: horizontal and vertical", panned[0] < after[0] && panned[2] != after[2]);
        r1.keyPressed('m');
        check("1D M key: window removed, listener notified", !r1.isZoomed() && events1.get(events1.size() - 1) == null);
        r1.updateData();
        r1.computeView();
        check("1D sweep after M: view x = [0, 5]", near(r1.viewMinX, 0) && near(r1.viewMaxX, 5));
        r1.press(ChartInteraction.LEFT_BUTTON, 50 + 140, wy(100), 800, 600);
        r1.release(ChartInteraction.LEFT_BUTTON, 50 + 280, wy(400), 800, 600);
        double[] s = r1.getZoomWindow();
        check("1D sweep: window x as position in the sweep = [1, 2]", s != null && near(s[0], 1) && near(s[1], 2));

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
