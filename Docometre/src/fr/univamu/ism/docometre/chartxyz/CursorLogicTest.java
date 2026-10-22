package fr.univamu.ism.docometre.chartxyz;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Test without OpenGL of the cursor logic (PlotRenderer, Chart1DRenderer, Chart2DRenderer). */
public class CursorLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; }

    /** 800 x 600 component; 700 x 500 plot area starting at (50, 40) in OpenGL pixels. */
    static void frame(PlotRenderer<?> r) throws Exception {
        r.reshape(null, 0, 0, 800, 600);
        r.marginLeft = 50; r.marginBottom = 40; r.plotWidth = 700; r.plotHeight = 500;
        r.updateData();
        r.computeView();
        Method update = PlotRenderer.class.getDeclaredMethod("updateCursor");
        update.setAccessible(true);
        update.invoke(r);
    }
    /** Window position (origin at the top left) of the data point (x, y) in the current view. */
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
        // ---------------- 2D: two trajectories ----------------
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
        check("2D cursor disabled by default: no point, no call", r2.getCursorPosition() == null && events.isEmpty());
        check("2D Tab not used while the cursor is disabled", !r2.usesTabKey());

        r2.keyPressed('c');
        frame(r2);
        float[] p = r2.getCursorPosition();
        check("2D C key: closest point of the first curve = (3, 1.5)", p != null && near(p[0], 3) && near(p[1], 1.5));
        check("2D listener called with {3, 1.5}", events.size() == 1 && near(events.get(0)[0], 3) && near(events.get(0)[1], 1.5));
        frame(r2);
        check("2D motionless mouse, unchanged data: no new call", events.size() == 1);
        check("2D Tab used (active cursor, 2 curves)", r2.usesTabKey());

        r2.keyPressed(ChartInteraction.TAB);
        frame(r2);
        p = r2.getCursorPosition();
        // On-screen distance from the mouse at (3.1, 1.6), scales 63.6 px/unit in x and 90.9 in y:
        // (4, 3) at 140 px, (5, 2.5) at 146 px, (3, 3.5) at 173 px
        check("2D Tab: second curve, closest point on screen = (4, 3)", r2.getCursorCurveIndex() == 1 && near(p[0], 4) && near(p[1], 3));
        r2.keyPressed(ChartInteraction.TAB);
        check("2D Tab cycles: back to the first curve", r2.getCursorCurveIndex() == 0);
        r2.keyPressed(ChartInteraction.BACK_TAB);
        check("2D Shift + Tab: previous curve (the second one)", r2.getCursorCurveIndex() == 1);

        mouse(r2, 8.2, 1.0);
        frame(r2);
        p = r2.getCursorPosition();
        check("2D the mouse moves: closest point of the second curve = (8, 1)", near(p[0], 8) && near(p[1], 1));

        r2.setZoomWindow(0, 4, 0, 5);
        frame(r2);
        p = r2.getCursorPosition();
        check("2D zoom: only the visible points are candidates (x <= 4)", p != null && p[0] <= 4);

        r2.keyPressed('c');
        frame(r2);
        check("2D C key again: cursor disabled, listener called with null", r2.getCursorPosition() == null && events.get(events.size() - 1) == null);
        r2.keyPressed('c');
        check("2D re-enabling: back to the first curve", r2.getCursorCurveIndex() == 0);

        // ---------------- 1D scroll ----------------
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
        check("1D scroll: designated point at t = 7.50 s", p != null && near(p[0], 7.5) && near(p[1], v));
        check("1D abscissa displayed with the 100 Hz resolution: \"7.50\"", "7.50".equals(r1.formatCursorX(signal, p[0])));
        check("1D single curve: Tab left to SWT", !r1.usesTabKey());
        mouse(r1, 6.2, r1.viewMaxY); // at the very top of the plot area, far from the curve
        frame(r1);
        float[] top = r1.getCursorPosition();
        check("1D same abscissa as the mouse, whatever its height: t = 6.20 s", top != null && near(top[0], 6.2) && near(top[1], Math.sin(620 / 50.0)));
        mouse(r1, t, v);
        frame(r1);
        signal.addSamples(new float[100]); // one second of zeros: the curve scrolls under the mouse
        frame(r1);
        float[] q = r1.getCursorPosition();
        check("1D the curve scrolls under a motionless mouse: the designated point changes", q != null && !(near(q[0], p[0]) && near(q[1], p[1])));

        // ---------------- 1D: static cursor (double click in cursor mode) ----------------
        r1.setCursorEnabled(false);
        frame(r1);
        int[] at = window(r1, 7.0, 0);
        r1.doubleClick(ChartInteraction.LEFT_BUTTON, at[0], at[1], 800, 600);
        frame(r1);
        check("1D double click without cursor mode: no static cursor", r1.getStaticCursorPosition() == null);
        List<float[][]> staticEvents = new ArrayList<>();
        r1.addStaticCursorListener((position, d) -> staticEvents.add(new float[][]{position, d}));
        r1.setCursorEnabled(true);
        double t1 = r1.viewMinX + 2.5, t2 = r1.viewMinX + 3.0; // two times in the visible window
        at = window(r1, t1, 0);
        r1.doubleClick(ChartInteraction.LEFT_BUTTON, at[0], at[1], 800, 600);
        frame(r1);
        float[] fixed = r1.getStaticCursorPosition();
        check("1D double click: static cursor placed on the sample at the mouse abscissa", fixed != null && near(fixed[0], t1));
        float[][] last = staticEvents.get(staticEvents.size() - 1);
        check("1D static listener: position on placement, delta = (0, 0)", last[0] != null && near(last[0][0], t1)
                && last[1] != null && near(last[1][0], 0) && near(last[1][1], 0));
        int eventsBefore = staticEvents.size();
        frame(r1);
        check("1D static listener: no call when nothing changes", staticEvents.size() == eventsBefore);
        mouse(r1, t2, 0);
        frame(r1);
        float[] delta = r1.getCursorDelta(), moving = r1.getCursorPosition();
        check("1D mouse moved: delta = moving - static (dx = 0.5 s)", delta != null && near(delta[0], 0.5)
                && near(delta[1], moving[1] - fixed[1]));
        last = staticEvents.get(staticEvents.size() - 1);
        check("1D static listener: called with the new delta", near(last[1][0], 0.5) && near(last[1][1], delta[1]));
        at = window(r1, t2, 0);
        r1.doubleClick(ChartInteraction.LEFT_BUTTON, at[0], at[1], 800, 600);
        frame(r1);
        check("1D second double click: static cursor moved", near(r1.getStaticCursorPosition()[0], t2) && near(r1.getCursorDelta()[0], 0));
        r1.keyPressed('c');
        check("1D cursor mode disabled: static cursor removed", r1.getStaticCursorPosition() == null && r1.getCursorDelta() == null);
        frame(r1);
        last = staticEvents.get(staticEvents.size() - 1);
        check("1D static listener: called with (null, null) on removal", last[0] == null && last[1] == null);
        r1.keyPressed('c');
        frame(r1);
        r1.doubleClick(ChartInteraction.LEFT_BUTTON, at[0], at[1], 800, 600);
        frame(r1);
        r1.keyPressed('m');
        check("1D M key (mode change): static cursor removed", r1.getStaticCursorPosition() == null);
        r1.keyPressed('m');
        r2.setCursorEnabled(true);
        frame(r2);
        at = window(r2, 3, 1.5);
        r2.doubleClick(ChartInteraction.LEFT_BUTTON, at[0], at[1], 800, 600);
        frame(r2);
        check("2D: no static cursor (1D only)", r2.getStaticCursorPosition() == null);

        // ---------------- Badge overlap: the static badge is moved beside the moving one ----------------
        Method separate = PlotRenderer.class.getDeclaredMethod("separate", float[].class, float[].class, int.class, float.class);
        separate.setAccessible(true);
        float[] a = {10, 100, 40, 16}, b = {12, 104, 40, 16};
        separate.invoke(null, a, b, 1, 600f);
        check("Y badges overlapping: static badge moved just below the moving one (y = 86)", near(a[1], 86) && near(a[0], 10));
        a = new float[]{10, 590, 40, 16};
        b = new float[]{10, 582, 40, 16};
        separate.invoke(null, a, b, 1, 600f);
        check("Y badges at the top edge: static badge moved to the other side (y = 564)", near(a[1], 564));
        a = new float[]{300, 5, 50, 16};
        b = new float[]{280, 5, 50, 16};
        separate.invoke(null, a, b, 0, 800f);
        check("X badges overlapping: static badge moved just to the right (x = 332)", near(a[0], 332));
        a = new float[]{10, 100, 40, 16};
        b = new float[]{10, 300, 40, 16};
        separate.invoke(null, a, b, 1, 600f);
        check("Badges not overlapping: unchanged", near(a[1], 100));

        // ---------------- 1D: horizontal pan without zoom window ----------------
        r1.setCursorEnabled(false);
        frame(r1);
        double minBefore = r1.viewMinX, maxBefore = r1.viewMaxX, yBefore = r1.viewMinY;
        r1.drag(ChartInteraction.RIGHT_BUTTON, 140, 0, 800, 600, ChartInteraction.InputScale.SWT); // 140 px to the right
        frame(r1);
        double expectedShift = 140.0 / 700 * 5; // 140 px of the plot area (700 px for 5 s)
        check("1D right button to the right: view shifted by 1 s towards older times", near(r1.viewMinX, minBefore - expectedShift) && near(r1.viewMaxX, maxBefore - expectedShift) && near(r1.viewMinY, yBefore));
        r1.wheel(3, ChartInteraction.InputScale.SWT); // vertical zoom: does not change the horizontal pan speed
        frame(r1);
        double min2 = r1.viewMinX;
        r1.drag(ChartInteraction.RIGHT_BUTTON, 140, 0, 800, 600, ChartInteraction.InputScale.SWT);
        frame(r1);
        check("1D after wheel zoom (y only), same horizontal shift", near(r1.viewMinX, min2 - expectedShift));
        r1.keyPressed('r');
        frame(r1);
        check("1D R: back to the initial view (shift cancelled)", near(r1.viewMaxX, Math.max(5, signal.getLastSampleTime())));

        // ---------------- 1D sweep: the gap cannot be designated ----------------
        Curve1DData sweep = new Curve1DData(100);
        Chart1DRenderer rs = new Chart1DRenderer(5, sweep);
        float[] ramp = new float[801];
        for (int i = 0; i < ramp.length; i++) ramp[i] = i / 100f; // 8 s: last sample at 8 s, write cursor at 3 s
        sweep.addSamples(ramp);
        rs.setCursorEnabled(true);
        frame(rs);
        int first = rs.firstCursorPoint(sweep);
        mouse(rs, 3.025, 3.025); // just after the write cursor: in the gap
        frame(rs);
        p = rs.getCursorPosition();
        // 501 samples from t = 3 to 8 s; the gap (1% of 5 s = 0.05 s) covers t = 3.00 to 3.04: 5 samples excluded
        check("1D sweep: the 5 samples of the gap are excluded (1st selectable = 5)", first == 5);
        check("1D sweep: the designated point is not in the gap (x outside ]3, 3.05[)", p != null && !(p[0] > 3.0001 && p[0] < 3.0499));

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
