package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.CursorShape;
import fr.univamu.ism.docometre.chartxyz.CursorStyle;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLCursorChart2D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;

/**
 * Demonstration of the 2D chart without history: only the last position of
 * two simulated curves is displayed, in fixed scales. The first one is a
 * red star outlined in white; the second one is a green square, or the
 * image given as first argument (PNG, JPEG...), 32 pixels wide.
 */
public class RealTimeCursorChart2DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz

    public static void main(String[] args) {
        run(GLToolkit.SWT, (args.length > 0) ? args[0] : null);
    }

    static void run(GLToolkit toolkit, String imagePath) {
        Curve2DData lissajous = new Curve2DData("Lissajous", SAMPLE_FREQUENCY);
        Curve2DData circle = new Curve2DData("circle", SAMPLE_FREQUENCY);

        Shell shell = DemoSupport.createShell(new Display(), "Last 2D positions — SWT + JOGL (" + toolkit + ")", 1000, 700);

        GLCursorChart2D graph = new GLCursorChart2D(shell, toolkit, -1.6, 1.6, -1, 1, lissajous, circle);
        graph.setLegendPosition(LegendPosition.BELOW);
        graph.setEqualAspect(true);
        graph.setCursorStyle(lissajous, CursorStyle.shape(CursorShape.STAR, 24).withFillColor(230, 40, 40).withOutline(255, 255, 255, 1.5f));
        graph.setCursorStyle(circle, (imagePath != null) ? CursorStyle.image(imagePath, 32)
                : CursorStyle.shape(CursorShape.SQUARE, 16).withFillColor(60, 200, 90));

        ScheduledExecutorService acquisition = DemoSupport.startLissajous(lissajous, circle);
        DemoSupport.run(shell, acquisition);
    }
}
