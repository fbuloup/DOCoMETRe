package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.List;

/** Test without OpenGL of the real-time setting of the 1D charts: sweep mode and pause (Chart1DRenderer). */
public class RealTimeModeTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        Chart1DRenderer r = new Chart1DRenderer(5, new Curve1DData("signal", 100));
        List<Chart1DRenderer.DisplayMode> modes = new ArrayList<>();
        List<Boolean> pauses = new ArrayList<>();
        r.addDisplayModeListener(modes::add);
        r.addDisplayOptionListener((option, enabled) -> { if (option == DisplayOption.PAUSE) pauses.add(enabled); });

        // Real time (default): sweep mode and pause available
        check("real time by default, sweep mode", r.isRealTime() && r.getDisplayMode() == Chart1DRenderer.DisplayMode.SWEEP);
        r.keyPressed('m');
        check("real time: M switches to scroll", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL);
        r.keyPressed('m');
        check("real time: M switches back to sweep", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SWEEP);
        r.keyPressed(' ');
        check("real time: space freezes the display", r.isPaused());
        r.setZoomWindow(1, 2, -1, 1);

        // Not real time: scroll mode, pause removed, listeners notified
        modes.clear();
        pauses.clear();
        r.setRealTime(false);
        check("not real time: scroll mode", !r.isRealTime() && r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL);
        check("not real time: pause removed", !r.isPaused());
        check("not real time: mode and pause listeners notified", modes.equals(List.of(Chart1DRenderer.DisplayMode.SCROLL)) && pauses.equals(List.of(false)));
        check("not real time: zoom window of the sweep mode removed", !r.isZoomed());

        // Not real time: sweep mode and pause unavailable
        modes.clear();
        pauses.clear();
        r.keyPressed('m');
        r.keyPressed(' ');
        check("not real time: M and space have no effect", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL && !r.isPaused());
        r.toggleDisplayMode();
        r.togglePause();
        r.setDisplayMode(Chart1DRenderer.DisplayMode.SWEEP);
        r.setPaused(true);
        check("not real time: API calls have no effect", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL && !r.isPaused());
        check("not real time: nothing notified", modes.isEmpty() && pauses.isEmpty());
        r.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL);
        r.setPaused(false);
        check("not real time: scroll mode and no pause still accepted", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL && !r.isPaused());
        r.setRealTime(false);
        check("not real time again: nothing notified", modes.isEmpty() && pauses.isEmpty());

        // Back to real time: the mode is kept, sweep mode and pause available again
        r.setRealTime(true);
        check("real time again: scroll mode kept, nothing notified", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SCROLL && modes.isEmpty() && pauses.isEmpty());
        r.keyPressed('m');
        r.keyPressed(' ');
        check("real time again: M and space available", r.getDisplayMode() == Chart1DRenderer.DisplayMode.SWEEP && r.isPaused());

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
