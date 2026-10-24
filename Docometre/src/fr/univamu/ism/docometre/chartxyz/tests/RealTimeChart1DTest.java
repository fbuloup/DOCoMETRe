package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;

/**
 * Demonstration of the real-time display of several signals y(t): a thread
 * simulates an acquisition every 20 ms. Two channels are sampled at 1000 Hz,
 * the third one at 100 Hz. Only the last WINDOW_TIME_WIDTH seconds are
 * displayed, in oscilloscope-like sweep mode. Space: pause / resume; M:
 * sweep / scroll.
 */
public class RealTimeChart1DTest {

    private static final double FAST_FREQUENCY = 1000;    // Hz
    private static final double SLOW_FREQUENCY = 100;     // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Curve1DData sine = new Curve1DData("1 Hz sine", FAST_FREQUENCY).setLineWidth(3);
        Curve1DData noisy = new Curve1DData("7 Hz sine + noise", FAST_FREQUENCY).setLineWidth(1);
        Curve1DData drift = new Curve1DData("drift (100 Hz)", SLOW_FREQUENCY).setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);

        Shell shell = DemoSupport.createShell(new Display(), "Real-time signals — SWT + JOGL (" + toolkit + ")", 1000, 500);

        GLChart1D graph = new GLChart1D(shell, toolkit, WINDOW_TIME_WIDTH, sine, noisy, drift);
        graph.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph.setLegendFont(null, 11, SWT.BOLD); // system font, bold
        graph.setLegendPosition(LegendPosition.BELOW); // legend below the abscissa axis
        graph.setCurrentValuesVisible(true);              // last values at the top right (V key)

        ScheduledExecutorService acquisition = DemoSupport.startSignals(sine, noisy, drift);
        DemoSupport.run(shell, acquisition);
    }
}
