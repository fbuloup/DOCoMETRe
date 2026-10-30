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
        Chart2DRenderer r2 = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
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

        // Wheel and pan (right button) act on the window; the wheel is stored in the history, not the pan
        r2.move(5, 5, 800, 600); // mouse outside the plot area: zoom around the center
        r2.wheel(1, ChartInteraction.InputScale.SWT);
        double[] w3 = r2.getZoomWindow();
        check("2D wheel, mouse outside the plot area: window shrunk around its center", near((w3[0] + w3[1]) / 2, (w2[0] + w2[1]) / 2) && near((w3[1] - w3[0]) * 1.15, w2[1] - w2[0]));
        r2.drag(ChartInteraction.RIGHT_BUTTON, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        double[] w4 = r2.getZoomWindow();
        check("2D pan: window shifted to the left", w4[0] < w3[0] && near(w4[1] - w4[0], w3[1] - w3[0]));
        r2.dragTo(ChartInteraction.LEFT_BUTTON, 500, 300, 80, 0, 800, 600, ChartInteraction.InputScale.SWT);
        check("2D left drag without selection: no more pan", Arrays.equals(r2.getZoomWindow(), w4));

        // History: U goes back to w2 (before the wheel, the pan is also undone), then w (the zoom
        // before w2), Backspace to the initial view
        r2.keyPressed('u');
        check("2D U: window before the wheel", Arrays.equals(r2.getZoomWindow(), w2));
        r2.keyPressed('u');
        check("2D U: previous window", Arrays.equals(r2.getZoomWindow(), w));
        r2.keyPressed(ChartInteraction.BACKSPACE);
        check("2D Backspace: initial view", r2.getZoomWindow() == null && events.get(events.size() - 1) == null);
        check("2D history empty afterwards", !r2.canUndoZoom() && !r2.undoZoom());

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
        Curve1DData signal = new Curve1DData("signal", 100);
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
        r1.move(50 + 175, wy(100), 800, 600); // mouse at 1/4 of the width and 1/5 of the height of the window
        double[] anchor = r1.mouseDataPosition();
        double ax = anchor[0] - (r1.viewMinX - before[0]); // in window coordinates (relative to the last sample)
        r1.wheel(1, ChartInteraction.InputScale.SWT);
        double[] after = r1.getZoomWindow();
        check("1D wheel with zoom window: both axes zoomed in by 1.15",
                near((before[1] - before[0]) / (after[1] - after[0]), 1.15) && near((before[3] - before[2]) / (after[3] - after[2]), 1.15));
        check("1D wheel with zoom window: the point under the mouse stays in place",
                near((ax - after[0]) / (after[1] - after[0]), 0.25) && near((anchor[1] - after[2]) / (after[3] - after[2]), 0.2));
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

        // ---------------- 2D: wheel on both axes around the mouse ----------------
        for (boolean equal : new boolean[]{false, true}) {
            Chart2DRenderer rz = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
            if (equal) rz.keyPressed('e'); // same scale on x and y: the tightest axis is widened
            layout(rz);
            String mode = equal ? " (same scale)" : "";
            double zw0 = rz.viewMaxX - rz.viewMinX, zh0 = rz.viewMaxY - rz.viewMinY;
            rz.move(50 + 210, wy(400), 800, 600); // 30 % of the width, 80 % of the height
            double[] zm = rz.mouseDataPosition();
            rz.wheel(1, ChartInteraction.InputScale.SWT);
            rz.computeView();
            check("2D wheel" + mode + ": x and y zoomed in by 1.15",
                    near(zw0 / (rz.viewMaxX - rz.viewMinX), 1.15) && near(zh0 / (rz.viewMaxY - rz.viewMinY), 1.15));
            check("2D wheel" + mode + ": the point under the mouse stays in place",
                    near((zm[0] - rz.viewMinX) / (rz.viewMaxX - rz.viewMinX), 0.3) && near((zm[1] - rz.viewMinY) / (rz.viewMaxY - rz.viewMinY), 0.8));
            for (int i = 0; i < 3; i++) { rz.wheel(-1, ChartInteraction.InputScale.SWT); rz.computeView(); }
            check("2D wheel back" + mode + ": anchor still in place",
                    near((zm[0] - rz.viewMinX) / (rz.viewMaxX - rz.viewMinX), 0.3) && near((zm[1] - rz.viewMinY) / (rz.viewMaxY - rz.viewMinY), 0.8));
        }
        Chart2DRenderer rzw = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        layout(rzw);
        rzw.setZoomWindow(2, 6, 1, 3);
        rzw.computeView();
        rzw.move(50 + 175, wy(100), 800, 600); // 1/4 of the width, 1/5 of the height
        double[] zm = rzw.mouseDataPosition();
        rzw.wheel(1, ChartInteraction.InputScale.SWT);
        double[] zw = rzw.getZoomWindow();
        check("2D wheel with zoom window: the point under the mouse stays in place",
                near((zw[1] - zw[0]) * 1.15, 4) && near((zm[0] - zw[0]) / (zw[1] - zw[0]), 0.25) && near((zm[1] - zw[2]) / (zw[3] - zw[2]), 0.2));

        // ---------------- 1D without zoom window: wheel on both axes around the mouse ----------------
        Curve1DData ramp = new Curve1DData("ramp", 100);
        float[] values = new float[501];
        for (int i = 0; i < values.length; i++) values[i] = (float) Math.sin(i / 40.0);
        ramp.addSamples(values); // 5 s
        Chart1DRenderer rw = new Chart1DRenderer(5, ramp);
        rw.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        layout(rw);
        double w0 = rw.viewMaxX - rw.viewMinX, h0 = rw.viewMaxY - rw.viewMinY;
        rw.move(50 + 210, wy(400), 800, 600); // 30 % of the width, 80 % of the height
        double[] m = rw.mouseDataPosition();
        rw.wheel(1, ChartInteraction.InputScale.SWT);
        rw.computeView();
        check("1D wheel: x and y zoomed in by 1.15",
                near(w0 / (rw.viewMaxX - rw.viewMinX), 1.15) && near(h0 / (rw.viewMaxY - rw.viewMinY), 1.15));
        check("1D wheel: the point under the mouse stays in place",
                near((m[0] - rw.viewMinX) / (rw.viewMaxX - rw.viewMinX), 0.3) && near((m[1] - rw.viewMinY) / (rw.viewMaxY - rw.viewMinY), 0.8));
        check("1D wheel: no zoom window created", !rw.isZoomed());
        for (int i = 0; i < 3; i++) { rw.wheel(-1, ChartInteraction.InputScale.SWT); rw.computeView(); }
        check("1D wheel back: anchor still in place", near((m[0] - rw.viewMinX) / (rw.viewMaxX - rw.viewMinX), 0.3)
                && near(w0 / (rw.viewMaxX - rw.viewMinX), Math.pow(1.15, -2)));
        ramp.addSamples(new float[100]); // one more second
        rw.updateData();
        rw.computeView();
        check("1D scroll: the zoomed view follows time (+1 s)", near((m[0] + 1 - rw.viewMinX) / (rw.viewMaxX - rw.viewMinX), 0.3));
        rw.move(5, 5, 800, 600); // outside the plot area: zoom around the center
        double c = (rw.viewMinX + rw.viewMaxX) / 2;
        rw.wheel(1, ChartInteraction.InputScale.SWT);
        rw.computeView();
        check("1D wheel outside the plot area: around the center", near((rw.viewMinX + rw.viewMaxX) / 2, c));
        rw.keyPressed('r');
        rw.computeView();
        check("1D R: horizontal zoom cancelled", near(rw.viewMaxX - rw.viewMinX, 5));
        check("1D R: empty history", !rw.canUndoZoom());

        // ---------------- Wheel in the history (U key) ----------------
        rw.computeView();
        double[] initial = {rw.viewMinX, rw.viewMaxX, rw.viewMinY, rw.viewMaxY};
        rw.move(50 + 210, wy(400), 800, 600);
        for (int i = 0; i < 4; i++) { rw.wheel(1, ChartInteraction.InputScale.SWT); rw.computeView(); } // one gesture
        double[] gesture1 = {rw.viewMinX, rw.viewMaxX, rw.viewMinY, rw.viewMaxY};
        check("1D wheel without window: undo available", rw.canUndoZoom() && !rw.isZoomed());
        try { Thread.sleep(600); } catch (InterruptedException e) { }
        rw.wheel(1, ChartInteraction.InputScale.SWT); // second gesture, after a pause
        rw.wheel(1, ChartInteraction.InputScale.SWT);
        rw.keyPressed('u');
        rw.computeView();
        check("1D U: back to the end of the first gesture",
                near(rw.viewMinX, gesture1[0]) && near(rw.viewMaxX, gesture1[1]) && near(rw.viewMinY, gesture1[2]) && near(rw.viewMaxY, gesture1[3]));
        rw.keyPressed('u');
        rw.computeView();
        check("1D U: the steps of the first gesture are undone together (horizontal zoom included)",
                near(rw.viewMinX, initial[0]) && near(rw.viewMaxX, initial[1]) && near(rw.viewMinY, initial[2]) && near(rw.viewMaxY, initial[3]));
        check("1D history empty afterwards", !rw.canUndoZoom());

        // A rectangle between two wheel steps ends the gesture: each is undone separately
        Chart2DRenderer rg = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        layout(rg);
        rg.zoom(1);
        rg.computeView();
        double[] afterZoom = {rg.viewMinX, rg.viewMaxX};
        rg.setZoomWindow(1, 2, 1, 2);
        rg.wheel(1, ChartInteraction.InputScale.SWT);
        double[] scaled = rg.getZoomWindow();
        check("2D wheel after a rectangle: window scaled", scaled != null && near((scaled[1] - scaled[0]) * 1.15, 1));
        rg.undoZoom();
        check("2D U: window before the wheel", Arrays.equals(rg.getZoomWindow(), new double[]{1, 2, 1, 2}));
        rg.undoZoom();
        rg.computeView();
        check("2D U: view zoomed by zoom(), without window", !rg.isZoomed() && near(rg.viewMinX, afterZoom[0]) && near(rg.viewMaxX, afterZoom[1]));
        rg.undoZoom();
        rg.computeView();
        check("2D U: initial view", near(rg.viewMinX, -0.5) && near(rg.viewMaxX, 10.5) && !rg.canUndoZoom());

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
