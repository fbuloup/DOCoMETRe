package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LineStyle;

/**
 * Démonstration de l'affichage temps réel de deux courbes 3D : un thread
 * simule une acquisition à SAMPLE_FREQUENCY Hz et envoie les échantillons
 * toutes les 20 ms. Seules les WINDOW_TIME_WIDTH dernières secondes sont
 * affichées. Les courbes sont ajoutées après la création du graphe.
 */
public class RealTimeChart3DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Shell shell = DemoSupport.createShell(new Display(), "Courbe 3D temps réel — SWT + JOGL (" + toolkit + ")", 1000, 700);

        GLChart3D chart = new GLChart3D(shell, toolkit, WINDOW_TIME_WIDTH);

        Curve3DData spiral = new Curve3DData(SAMPLE_FREQUENCY).setName("spirale");
        Curve3DData helix = new Curve3DData(SAMPLE_FREQUENCY).setName("hélice").setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);
        chart.addCurve(helix);
        chart.addCurve(spiral);

        ScheduledExecutorService acquisition = DemoSupport.startSpiral(spiral, helix);
        DemoSupport.run(shell, acquisition);
    }
}
