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
 * Démonstration de l'affichage de plusieurs signaux y(t) en temps réel : un
 * thread simule une acquisition toutes les 20 ms. Deux voies sont
 * échantillonnées à 1000 Hz, la troisième à 100 Hz. Seules les
 * WINDOW_TIME_WIDTH dernières secondes sont affichées, en balayage de type
 * oscilloscope. Espace : pause / reprise ; M : balayage / défilement.
 */
public class RealTimeChart1DTest {

    private static final double FAST_FREQUENCY = 1000;    // Hz
    private static final double SLOW_FREQUENCY = 100;     // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s

    public static void main(String[] args) {
        run(GLToolkit.SWT);
    }

    static void run(GLToolkit toolkit) {
        Curve1DData sine = new Curve1DData(FAST_FREQUENCY).setName("sinus 1 Hz").setLineWidth(3);
        Curve1DData noisy = new Curve1DData(FAST_FREQUENCY).setName("sinus 7 Hz + bruit").setLineWidth(1);
        Curve1DData drift = new Curve1DData(SLOW_FREQUENCY).setName("dérive (100 Hz)").setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);

        Shell shell = DemoSupport.createShell(new Display(), "Signaux temps réel — SWT + JOGL (" + toolkit + ")", 1000, 500);

        GLChart1D graph = new GLChart1D(shell, toolkit, WINDOW_TIME_WIDTH, sine, noisy, drift);
        graph.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses
        graph.setCurrentValuesVisible(true);              // dernières valeurs en haut à droite (touche V)

        ScheduledExecutorService acquisition = DemoSupport.startSignals(sine, noisy, drift);
        DemoSupport.run(shell, acquisition);
    }
}
