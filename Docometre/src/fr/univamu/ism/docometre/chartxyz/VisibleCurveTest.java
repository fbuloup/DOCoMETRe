package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

/** Test without OpenGL: the cursor always follows a curve with visible points. */
public class VisibleCurveTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK    " : "FAIL  ") + what);
        if (!ok) failures++;
    }
    static int names = 0;
    static Curve2DData band(float y) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i <= 10; i++) points.add(new Point2D(i, y + i / 10f));
        return new Curve2DData("c" + names++, 10, points);
    }

    public static void main(String[] args) throws Exception {
        // Three horizontal bands: y in [0, 1], [4, 5], [8, 9]
        Chart2DRenderer r = new Chart2DRenderer(band(0), band(4), band(8));
        List<float[]> events = new ArrayList<>();
        r.addCursorListener(events::add);
        CursorLogicTest.frame(r);
        r.setZoomWindow(0, 10, 3.5, 6); // only the second band is visible
        CursorLogicTest.frame(r);
        CursorLogicTest.mouse(r, 5, 4.5);
        r.keyPressed('c');
        CursorLogicTest.frame(r);
        float[] p = r.getCursorPosition();
        check("C with curve 0 outside the view: the second curve is followed", r.getCursorCurveIndex() == 1 && p != null && p[1] >= 4 && p[1] <= 5);
        check("listener notified with the point of the second curve", !events.isEmpty() && events.get(events.size() - 1) != null);
        r.keyPressed(ChartInteraction.TAB);
        check("Tab with a single visible curve: unchanged", r.getCursorCurveIndex() == 1);
        r.keyPressed(ChartInteraction.BACK_TAB);
        check("Shift + Tab with a single visible curve: unchanged", r.getCursorCurveIndex() == 1);

        // Two visible curves out of three: a 2D view cannot hide only the middle band by y,
        // so the third band is moved into the view by a zoom excluding the middle band in x
        Chart2DRenderer r3 = new Chart2DRenderer(band(0), shifted(), band(8));
        CursorLogicTest.frame(r3);
        r3.setZoomWindow(0, 10, -1, 10); // the second curve lies at x in [20, 30]: outside
        CursorLogicTest.frame(r3);
        r3.setCursorEnabled(true);
        CursorLogicTest.mouse(r3, 5, 0.5);
        CursorLogicTest.frame(r3);
        check("three curves, the second outside the view: curve 0 followed", r3.getCursorCurveIndex() == 0);
        r3.keyPressed(ChartInteraction.TAB);
        check("Tab skips the invisible curve: 0 -> 2", r3.getCursorCurveIndex() == 2);
        r3.keyPressed(ChartInteraction.TAB);
        check("Tab cycles: 2 -> 0", r3.getCursorCurveIndex() == 0);
        r3.keyPressed(ChartInteraction.BACK_TAB);
        check("Shift + Tab skips the invisible curve: 0 -> 2", r3.getCursorCurveIndex() == 2);
        r3.keyPressed(ChartInteraction.BACK_TAB);
        check("Shift + Tab: 2 -> 0", r3.getCursorCurveIndex() == 0);

        // Zoom leaving the followed curve: the first visible curve is followed
        r3.keyPressed(ChartInteraction.TAB); // 2
        CursorLogicTest.frame(r3);
        r3.setZoomWindow(0, 10, -1, 2); // only band 0 visible
        CursorLogicTest.frame(r3);
        CursorLogicTest.mouse(r3, 5, 0.5);
        CursorLogicTest.frame(r3);
        p = r3.getCursorPosition();
        check("zoom leaving curve 2: curve 0 followed", r3.getCursorCurveIndex() == 0 && p != null && p[1] <= 1.0001);
        r3.setZoomWindow(0, 10, -1, 10); // curve 2 visible again: the cursor stays on curve 0
        CursorLogicTest.frame(r3);
        check("curve 2 visible again: the cursor stays on curve 0", r3.getCursorCurveIndex() == 0);

        // No visible curve
        List<float[]> events3 = new ArrayList<>();
        r3.addCursorListener(events3::add);
        r3.setZoomWindow(0, 10, 50, 60);
        CursorLogicTest.frame(r3);
        CursorLogicTest.mouse(r3, 5, 55);
        CursorLogicTest.frame(r3);
        check("no visible curve: no point, listener notified with null", r3.getCursorPosition() == null && !events3.isEmpty() && events3.get(events3.size() - 1) == null);
        int before = r3.getCursorCurveIndex();
        r3.keyPressed(ChartInteraction.TAB);
        check("no visible curve: Tab still moves to the next curve", r3.getCursorCurveIndex() == (before + 1) % 3);

        // 1D: zoom on a time range where the first signal is absent (shorter trial)
        Curve1DData shortSignal = new Curve1DData("short", 100), longSignal = new Curve1DData("long", 100);
        Chart1DRenderer r1 = new Chart1DRenderer(20, shortSignal, longSignal);
        float[] a = new float[201], b = new float[1001];
        for (int i = 0; i < a.length; i++) a[i] = 1;
        for (int i = 0; i < b.length; i++) b[i] = -1;
        shortSignal.addSamples(a); // 2 s
        longSignal.addSamples(b);  // 10 s
        CursorLogicTest.frame(r1);
        r1.setZoomWindow(5, 8, -2, 2);
        CursorLogicTest.frame(r1);
        CursorLogicTest.mouse(r1, 6, 0);
        r1.setCursorEnabled(true);
        CursorLogicTest.frame(r1);
        p = r1.getCursorPosition();
        check("1D zoom where the first signal is absent: the second one is followed", r1.getCursorCurveIndex() == 1 && p != null && Math.abs(p[1] + 1) < 1e-3);

        System.out.println(failures == 0 ? "\nAll tests pass." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }

    static Curve2DData shifted() {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i <= 10; i++) points.add(new Point2D(20 + i, 4));
        return new Curve2DData("c" + names++, 10, points);
    }
}
