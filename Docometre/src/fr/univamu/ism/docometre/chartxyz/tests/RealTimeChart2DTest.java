package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLChart2D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;

/**
 * Demonstration of the real-time 2D display of two curves: a thread
 * simulates an acquisition at SAMPLE_FREQUENCY Hz and sends the samples every
 * 20 ms. Only the last WINDOW_TIME_WIDTH seconds are displayed.
 */
public class RealTimeChart2DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Curve2DData lissajous = new Curve2DData(SAMPLE_FREQUENCY).setName("Lissajous");
        Curve2DData circle = new Curve2DData(SAMPLE_FREQUENCY).setName("circle").setLineStyle(LineStyle.DASH);

        Shell shell = DemoSupport.createShell(new Display(), "Real-time 2D curves — SWT + JOGL (" + toolkit + ")", 1000, 700);

        GLChart2D graph = new GLChart2D(shell, toolkit, WINDOW_TIME_WIDTH, lissajous, circle);
        graph.setLegendPosition(LegendPosition.BELOW); // legend below the abscissa axis

        ScheduledExecutorService acquisition = DemoSupport.startLissajous(lissajous, circle);
        DemoSupport.run(shell, acquisition);
    }
}
