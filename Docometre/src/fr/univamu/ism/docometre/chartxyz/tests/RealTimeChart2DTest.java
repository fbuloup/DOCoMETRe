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
 * Démonstration de l'affichage 2D temps réel de deux courbes : un thread
 * simule une acquisition à SAMPLE_FREQUENCY Hz et envoie les échantillons
 * toutes les 20 ms. Seules les WINDOW_TIME_WIDTH dernières secondes sont
 * affichées.
 */
public class RealTimeChart2DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Curve2DData lissajous = new Curve2DData(SAMPLE_FREQUENCY).setName("Lissajous");
        Curve2DData circle = new Curve2DData(SAMPLE_FREQUENCY).setName("cercle").setLineStyle(LineStyle.DASH);

        Shell shell = DemoSupport.createShell(new Display(), "Courbes 2D temps réel — SWT + JOGL (" + toolkit + ")", 1000, 700);

        GLChart2D graph = new GLChart2D(shell, toolkit, WINDOW_TIME_WIDTH, lissajous, circle);
        graph.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        ScheduledExecutorService acquisition = DemoSupport.startLissajous(lissajous, circle);
        DemoSupport.run(shell, acquisition);
    }
}
