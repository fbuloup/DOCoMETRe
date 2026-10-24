package fr.univamu.ism.docometre.chartxyz;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test without OpenGL of the view listeners and of the 2D marker settings. */
public class ViewListenerTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK    " : "FAIL  ") + what);
        if (!ok) failures++;
    }

    /** Frame steps up to the view notification (no OpenGL). */
    static void frame(PlotRenderer<?> r) throws Exception {
        CursorLogicTest.frame(r);
        Method notify = PlotRenderer.class.getDeclaredMethod("notifyView");
        notify.setAccessible(true);
        notify.invoke(r);
    }

    public static void main(String[] args) throws Exception {
        Chart2DRenderer r = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        List<double[]> views = new ArrayList<>();
        List<Boolean> automatic = new ArrayList<>();
        r.addViewListener((view, auto) -> { views.add(view); automatic.add(auto); });
        frame(r);
        check("first frame: view notified, automatic", views.size() == 1 && automatic.get(0));
        check("getView() returns the displayed view", Arrays.equals(r.getView(), views.get(0)));
        frame(r);
        check("same view: no new notification", views.size() == 1);

        r.zoom(1);
        frame(r);
        check("zoom without zoom window: notified, not automatic", views.size() == 2 && !automatic.get(1));
        r.resetView();
        frame(r);
        check("reset: automatic view again", views.size() == 3 && automatic.get(2));
        r.pan(0.1f, 0f);
        frame(r);
        check("pan without zoom window: notified, not automatic", views.size() == 4 && !automatic.get(3));
        r.setZoomWindow(1, 2, 3, 4);
        frame(r);
        double[] v = views.get(views.size() - 1);
        check("zoom window: notified with its bounds", v[0] == 1 && v[1] == 2 && v[2] == 3 && v[3] == 4 && !automatic.get(automatic.size() - 1));

        // 1D: horizontal zoom without zoom window is not automatic
        Curve1DData signal = new Curve1DData("signal", 100, new float[]{0, 1, 2, 3, 4});
        Chart1DRenderer r1 = new Chart1DRenderer(1, signal);
        List<Boolean> auto1 = new ArrayList<>();
        r1.addViewListener((view, auto) -> auto1.add(auto));
        frame(r1);
        r1.zoom(1);
        frame(r1);
        check("1D: wheel zoom not automatic", auto1.size() >= 2 && auto1.get(0) && !auto1.get(auto1.size() - 1));

        // Marker settings
        check("marker size by default: 5", r.getMarkerSize() == PlotRenderer.DEFAULT_MARKER_SIZE);
        r.setMarkerSize(8);
        check("marker size: 8", r.getMarkerSize() == 8);
        r.setMarkerSize(0);
        check("marker size limited to 1", r.getMarkerSize() == 1);
        check("markers and labels visible by default", r.isMarkersVisible() && r.isMarkerLabelsVisible());
        r.setMarkersVisible(false);
        r.setMarkerLabelsVisible(false);
        check("markers and labels hidden", !r.isMarkersVisible() && !r.isMarkerLabelsVisible());
        check("2D markers drawn on their point", r.markersOnPoints() && !r1.markersOnPoints());

        System.out.println(failures == 0 ? "\nAll tests pass." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
