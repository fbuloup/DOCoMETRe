package fr.univamu.ism.docometre.chartxyz;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test without OpenGL of the marker logic (PlotRenderer, Chart1DRenderer). */
public class MarkerLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static String name(CurveData<?> curve) { return curve.getName(); }
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
    static void ctrlDoubleClick(PlotRenderer<?> r, double x) throws Exception {
        int[] at = window(r, x, 0);
        r.doubleClick(ChartInteraction.LEFT_BUTTON, ChartInteraction.CTRL, at[0], at[1], 800, 600);
        frame(r);
    }

    public static void main(String[] args) throws Exception {
        Curve1DData signal = new Curve1DData(100);
        Chart1DRenderer r = new Chart1DRenderer(5, signal);
        float[] samples = new float[1001];
        for (int i = 0; i < samples.length; i++) samples[i] = (float) Math.sin(i / 50.0);
        signal.addSamples(samples); // 10 s
        r.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        frame(r);

        check("Default label is \"Default\"", "Default".equals(r.getCurrentMarkerLabel()));
        ctrlDoubleClick(r, 7.0);
        check("Ctrl + double click without cursor mode: no marker", r.getMarkersLabels(signal).isEmpty());

        r.setCursorEnabled(true);
        frame(r);
        ctrlDoubleClick(r, 7.0);
        double[][] g = r.getMarkersGroup(signal, "Default");
        check("Ctrl + double click in cursor mode: marker \"Default\" at t = 7.00 with the sample value",
                g != null && g.length == 1 && near(g[0][0], 7.0) && near(g[0][1], Math.sin(700 / 50.0)));
        ctrlDoubleClick(r, 7.0);
        check("Same marker again (same label, same coordinates): ignored", r.getMarkersGroup(signal, "Default").length == 1);

        r.setCurrentMarkerLabel("Max");
        ctrlDoubleClick(r, 7.0);
        ctrlDoubleClick(r, 8.0);
        check("Current label \"Max\": same point, other label -> new group with 2 markers",
                r.getMarkersLabels(signal).equals(Arrays.asList("Default", "Max")) && r.getMarkersGroup(signal, "Max").length == 2);
        r.setCurrentMarkerLabel("  ");
        check("Blank label -> \"Default\"", "Default".equals(r.getCurrentMarkerLabel()));

        int[] at = window(r, 6.0, 0);
        r.doubleClick(ChartInteraction.LEFT_BUTTON, 0, at[0], at[1], 800, 600);
        frame(r);
        check("Plain double click in cursor mode: static cursor, no marker added",
                r.getStaticCursorPosition() != null && r.getMarkersGroup(signal, "Max").length == 2 && r.getMarkersGroup(signal, "Default").length == 1);

        r.keyPressed('c');
        check("Leaving cursor mode keeps the markers", r.getMarkersLabels(signal).size() == 2 && r.getStaticCursorPosition() == null);

        r.addMarkersGroup(signal, "Min", new double[][]{{1, -0.5}, {2, -0.6}, {1, -0.5}});
        check("addMarkersGroup: 2 markers (duplicate skipped)", r.getMarkersGroup(signal, "Min").length == 2);
        check("removeMarker: existing marker removed", r.removeMarker(signal, "Min", 1, -0.5) && r.getMarkersGroup(signal, "Min").length == 1);
        r.removeMarker(signal, "Min", 2, -0.6);
        check("Removing the last marker of a group removes the group", r.getMarkersGroup(signal, "Min") == null
                && !r.getMarkersLabels(signal).contains("Min"));
        check("removeMarkersGroup", r.removeMarkersGroup(signal, "Max") && r.getMarkersLabels(signal).equals(Arrays.asList("Default")));

        // Marker abscissa = absolute time: shown in sweep mode only if its time is in the displayed sweep
        r.keyPressed('m'); // sweep
        frame(r);
        double last = signal.getLastSampleTime(); // 10 s, window 5 s
        check("Sweep: marker at t = 7 s shown at position 7 mod 5 = 2", near(r.markerAxisX(7.0), 2.0));
        check("Sweep: marker older than the displayed sweep hidden (NaN)", Double.isNaN(r.markerAxisX(last - 5.0)));
        r.keyPressed('m'); // scroll
        frame(r);
        check("Scroll: marker shown at its time", near(r.markerAxisX(7.0), 7.0));

        Chart2DRenderer r2 = new Chart2DRenderer(new Curve2DData(Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        r2.setCursorEnabled(true);
        frame(r2);
        int[] at2 = window(r2, 5, 2.5);
        r2.doubleClick(ChartInteraction.LEFT_BUTTON, ChartInteraction.CTRL, at2[0], at2[1], 800, 600);
        frame(r2);
        check("2D: no markers (1D only)", r2.getMarkersCurves().isEmpty());

        // Listeners: one notification per marker, with the parameters of addMarker / removeMarker
        List<String> events = new ArrayList<>();
        MarkerAddedListener<Curve1DData> added = (curve, label, x, y) -> events.add("+" + name(curve) + "/" + label + "(" + x + "," + y + ")");
        MarkerRemovedListener<Curve1DData> removed = (curve, label, x, y) -> events.add("-" + name(curve) + "/" + label + "(" + x + "," + y + ")");
        CurrentMarkerLabelListener current = label -> events.add("current:" + label);
        r.clearMarkers();
        r.addMarkerAddedListener(added);
        r.addMarkerRemovedListener(removed);
        r.addCurrentMarkerLabelListener(current);
        signal.setName("S");
        r.addMarker(signal, "A", 1, 1);
        check("Listener: marker added", events.equals(Arrays.asList("+S/A(1.0,1.0)")));
        events.clear();
        r.addMarker(signal, "A", 1, 1);
        check("Listener: duplicate not notified", events.isEmpty());
        r.addMarkersGroup(signal, "B", new double[][]{{1, 1}, {2, 2}, {1, 1}});
        check("Listener: addMarkersGroup, one notification per marker added",
                events.equals(Arrays.asList("+S/B(1.0,1.0)", "+S/B(2.0,2.0)")));
        events.clear();
        r.removeMarker(signal, "B", 9, 9);
        check("Listener: removing a missing marker not notified", events.isEmpty());
        r.removeMarker(signal, "B", 2, 2);
        check("Listener: marker removed", events.equals(Arrays.asList("-S/B(2.0,2.0)")));
        events.clear();
        r.addMarker(signal, "B", 3, 3);
        events.clear();
        r.removeMarkersGroup(signal, "B");
        check("Listener: removeMarkersGroup, one notification per marker",
                events.equals(Arrays.asList("-S/B(1.0,1.0)", "-S/B(3.0,3.0)")));
        events.clear();
        r.clearMarkers();
        check("Listener: clearMarkers", events.equals(Arrays.asList("-S/A(1.0,1.0)")));
        events.clear();
        r.setCurrentMarkerLabel("E");
        r.setCurrentMarkerLabel("E");
        r.setCurrentMarkerLabel(" ");
        check("Current label listener: actual changes only", events.equals(Arrays.asList("current:E", "current:Default")));
        events.clear();
        MarkerAddedListener<Curve1DData> failing = (curve, label, x, y) -> { throw new IllegalStateException("test: listener failure (expected stack trace)"); };
        r.addMarkerAddedListener(failing);
        r.addMarker(signal, "F", 1, 1);
        check("Listener: a failing listener does not prevent the others", events.equals(Arrays.asList("+S/F(1.0,1.0)")));
        r.removeMarkerAddedListener(failing);
        events.clear();
        r.removeMarkerAddedListener(added);
        r.addMarker(signal, "F", 2, 2);
        check("Listener removed: no more notifications", events.isEmpty());
        r.addMarkerAddedListener(added);
        r.clearMarkers();

        // Mouse actions go through the same notifications
        r.setCursorEnabled(true);
        frame(r);
        events.clear();
        ctrlDoubleClick(r, 7.0);
        check("Ctrl + double click notified", events.size() == 1 && events.get(0).startsWith("+S/Default(7.0"));
        r.setCursorEnabled(false);
        r.clearMarkers();

        boolean thrown = false;
        try { r.addMarker(signal, " ", 0, 0); } catch (IllegalArgumentException e) { thrown = true; }
        check("Empty label refused", thrown);
        thrown = false;
        try { r.addMarker(new Curve1DData(100), "A", 0, 0); } catch (IllegalArgumentException e) { thrown = true; }
        check("Curve not in the chart refused", thrown && r.getMarkersCurves().isEmpty());

        // Groups belong to a curve
        Curve1DData a = new Curve1DData(100).setName("a"), b = new Curve1DData(100).setName("b"), c = new Curve1DData(100).setName("c");
        float[] sa = new float[501], sb = new float[501], sc = new float[501];
        for (int i = 0; i < sa.length; i++) {
            sa[i] = (float) Math.sin(i / 50.0);
            sb[i] = 3 + (float) Math.cos(i / 50.0);
            sc[i] = -3;
        }
        a.addSamples(sa);
        b.addSamples(sb);
        c.addSamples(sc);
        Chart1DRenderer r3 = new Chart1DRenderer(5, a, b, c);
        r3.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        List<String> events3 = new ArrayList<>();
        r3.addMarkerAddedListener((curve, label, x, y) -> events3.add("+" + name(curve) + "/" + label));
        r3.addMarkerRemovedListener((curve, label, x, y) -> events3.add("-" + name(curve) + "/" + label));
        r3.setCursorEnabled(true);
        frame(r3);
        r3.keyPressed(ChartInteraction.TAB); // cursor on b
        frame(r3);
        ctrlDoubleClick(r3, 2.0);
        double[][] m = r3.getMarkersGroup(b, "Default");
        check("Ctrl + double click on curve b: group (b, Default) with {2, 3 + cos(200 / 50)}",
                m != null && m.length == 1 && near(m[0][0], 2.0) && near(m[0][1], 3 + Math.cos(200 / 50.0))
                && r3.getMarkersGroup(a, "Default") == null);
        r3.keyPressed(ChartInteraction.BACK_TAB); // back to a
        frame(r3);
        ctrlDoubleClick(r3, 2.0);
        check("Same label on curve a: distinct group (a, Default)",
                r3.getMarkersGroup(a, "Default").length == 1 && r3.getMarkersGroup(b, "Default").length == 1);
        check("Listener: curve of each marker", events3.equals(Arrays.asList("+b/Default", "+a/Default")));
        check("Same coordinates in two groups with the same label: distinct markers",
                r3.addMarker(b, "Default", 2.0, 0.5) && r3.addMarker(a, "Default", 2.0, 0.5));
        check("Same coordinates in the same group: duplicate", !r3.addMarker(b, "Default", 2.0, 0.5));
        r3.addMarkersGroup(c, "Y", new double[][]{{1, -3}, {2, -3}});
        check("getMarkersCurves: in the order of the chart", r3.getMarkersCurves().equals(Arrays.asList(a, b, c)));
        check("getMarkersLabels per curve", r3.getMarkersLabels(c).equals(Arrays.asList("Y"))
                && r3.getMarkersLabels(a).equals(Arrays.asList("Default")));
        check("removeMarker in the group of another curve: nothing removed", !r3.removeMarker(a, "Y", 1, -3));
        check("removeMarkersGroup on one curve only", r3.removeMarkersGroup(b, "Default")
                && r3.getMarkersGroup(a, "Default").length == 2 && r3.getMarkersGroup(b, "Default") == null);
        r3.addMarker(b, "Z", 4, 4);

        // Removal of a curve in the middle of the list
        r3.keyPressed(ChartInteraction.TAB);
        r3.keyPressed(ChartInteraction.TAB); // cursor on c
        frame(r3);
        events3.clear();
        r3.removeCurve(b);
        frame(r3);
        check("Removing curve b: its markers removed and notified", events3.equals(Arrays.asList("-b/Z"))
                && r3.getMarkersCurves().equals(Arrays.asList(a, c)) && r3.getMarkersLabels(b).isEmpty());
        check("Removing curve b: the groups of a and c are unchanged",
                r3.getMarkersGroup(a, "Default").length == 2 && r3.getMarkersGroup(c, "Y").length == 2);
        check("Removing curve b: the cursor stays on c (index 2 -> 1)", r3.getCursorCurveIndex() == 1);
        thrown = false;
        try { r3.addMarker(b, "Z", 4, 4); } catch (IllegalArgumentException e) { thrown = true; }
        check("Removed curve: markers refused", thrown);
        r3.addCurve(b);
        check("Curve b added again: no markers", r3.getMarkersLabels(b).isEmpty() && r3.addMarker(b, "Z", 4, 4));

        // Static cursor on a removed curve
        int[] at3 = window(r3, 3.0, 0);
        r3.doubleClick(ChartInteraction.LEFT_BUTTON, 0, at3[0], at3[1], 800, 600); // static cursor on c
        frame(r3);
        boolean placed = r3.getStaticCursorPosition() != null;
        r3.removeCurve(a);
        frame(r3);
        check("Removing another curve keeps the static cursor; cursor stays on c (1 -> 0)",
                placed && r3.getStaticCursorPosition() != null && r3.getCursorCurveIndex() == 0);
        r3.removeCurve(c);
        frame(r3);
        check("Removing the curve of the static cursor removes it; cursor back to the first curve",
                r3.getStaticCursorPosition() == null && r3.getCursorCurveIndex() == 0);
        events3.clear();
        r3.removeAllCurves();
        check("removeAllCurves: remaining markers notified", events3.equals(Arrays.asList("-b/Z"))
                && r3.getMarkersCurves().isEmpty());

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
