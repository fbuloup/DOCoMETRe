package fr.univamu.ism.docometre.chartxyz;

import java.util.Arrays;

/** Test without OpenGL of the pan with the arrow keys (1D and 2D). */
public class ArrowPanTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK    " : "FAIL  ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b)); }

    /** View after the key: shifted by (fx, fy) times its width / height, same size. */
    static void checkShift(String what, PlotRenderer<?> r, char key, double fx, double fy) throws Exception {
        ViewListenerTest.frame(r);
        double[] before = r.getView();
        r.keyPressed(key);
        ViewListenerTest.frame(r);
        double[] after = r.getView();
        double w = before[1] - before[0], h = before[3] - before[2];
        check(what + " " + Arrays.toString(after), near(after[0], before[0] + fx * w) && near(after[1], before[1] + fx * w)
                && near(after[2], before[2] + fy * h) && near(after[3], before[3] + fy * h));
    }

    public static void main(String[] args) throws Exception {
        Chart2DRenderer r = new Chart2DRenderer(new Curve2DData("line", 1, Arrays.asList(new Point2D(0, 0), new Point2D(10, 5))));
        checkShift("2D right arrow: view moved right by 1/10", r, ChartInteraction.ARROW_RIGHT, 0.1, 0);
        checkShift("2D left arrow: view moved left by 1/10", r, ChartInteraction.ARROW_LEFT, -0.1, 0);
        checkShift("2D up arrow: view moved up by 1/10", r, ChartInteraction.ARROW_UP, 0, 0.1);
        checkShift("2D down arrow: view moved down by 1/10", r, ChartInteraction.ARROW_DOWN, 0, -0.1);
        r.setZoomWindow(1, 3, 2, 4);
        checkShift("2D zoom window: right arrow moves the window", r, ChartInteraction.ARROW_RIGHT, 0.1, 0);
        check("2D zoom window: still a window, moved", r.isZoomed() && near(r.getZoomWindow()[0], 1.2));
        checkShift("2D zoom window: up arrow moves the window", r, ChartInteraction.ARROW_UP, 0, 0.1);
        r.keyPressed('r');
        r.setCursorEnabled(true);
        checkShift("2D arrows with the cursor enabled", r, ChartInteraction.ARROW_RIGHT, 0.1, 0);

        Curve1DData signal = new Curve1DData("signal", 100, new float[]{0, 1, 2, 3, 4, 3, 2, 1});
        Chart1DRenderer r1 = new Chart1DRenderer(signal.getWindowTimeWidth(), signal);
        checkShift("1D right arrow: later times", r1, ChartInteraction.ARROW_RIGHT, 0.1, 0);
        checkShift("1D down arrow: lower values", r1, ChartInteraction.ARROW_DOWN, 0, -0.1);
        r1.zoom(2);
        checkShift("1D zoomed with the wheel: left arrow by 1/10 of the zoomed view", r1, ChartInteraction.ARROW_LEFT, -0.1, 0);

        // 3D: same pan as a drag with the right button, by a tenth of the visible height
        Chart3DRenderer r3 = new Chart3DRenderer();
        OrbitCamera camera = r3.camera();
        float radius = BoundingBox.DEFAULT.radius;
        float step = (float) (0.1 * 2 * camera.distance(radius) * Math.tan(Math.toRadians(OrbitCamera.FIELD_OF_VIEW / 2)));
        r3.keyPressed(ChartInteraction.ARROW_RIGHT);
        check("3D right arrow: scene moved left by 1/10 of the visible height", near(camera.getPan()[0], -step) && camera.getPan()[1] == 0);
        r3.keyPressed(ChartInteraction.ARROW_UP);
        check("3D up arrow: scene moved down", near(camera.getPan()[1], -step));
        r3.keyPressed(ChartInteraction.ARROW_LEFT);
        r3.keyPressed(ChartInteraction.ARROW_DOWN);
        check("3D opposite arrows: back to the initial pan", near(camera.getPan()[0], 0) && near(camera.getPan()[1], 0));
        r3.pan(10, 0);
        check("3D drag to the right (content to the right) = opposite of the right arrow", camera.getPan()[0] > 0);
        r3.keyPressed('r');
        r3.zoom(3);
        r3.keyPressed(ChartInteraction.ARROW_RIGHT);
        check("3D step follows the zoom (camera farther: larger step)", -camera.getPan()[0] > step);

        // Surface
        Point3D[][] grid = new Point3D[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) grid[i][j] = new Point3D(i, j, i * j);
        Surface3DRenderer rs = new Surface3DRenderer(new SurfaceData(grid));
        java.lang.reflect.Field field = Surface3DRenderer.class.getDeclaredField("camera");
        field.setAccessible(true);
        OrbitCamera surfaceCamera = (OrbitCamera) field.get(rs);
        rs.keyPressed(ChartInteraction.ARROW_LEFT);
        check("surface left arrow: scene moved right", surfaceCamera.getPan()[0] > 0);
        rs.keyPressed('s');
        check("surface other keys still handled (S: solid hidden)", !rs.isSolidVisible());

        System.out.println(failures == 0 ? "\nAll tests pass." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
