package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LineStyle;

/**
 * Demonstration of the real-time display of two 3D curves: a thread
 * simulates an acquisition at SAMPLE_FREQUENCY Hz and sends the samples every
 * 20 ms. Only the last WINDOW_TIME_WIDTH seconds are displayed. The curves
 * are added after the chart is created.
 */
public class RealTimeChart3DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Shell shell = DemoSupport.createShell(new Display(), "Real-time 3D curve — SWT + JOGL (" + toolkit + ")", 1000, 700);

        GLChart3D chart = new GLChart3D(shell, toolkit, WINDOW_TIME_WIDTH);

        Curve3DData spiral = new Curve3DData("spiral", SAMPLE_FREQUENCY);
        Curve3DData helix = new Curve3DData("helix", SAMPLE_FREQUENCY).setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);
        chart.addCurve(helix);
        chart.addCurve(spiral);

        ScheduledExecutorService acquisition = DemoSupport.startSpiral(spiral, helix);
        DemoSupport.run(shell, acquisition);
    }
}
