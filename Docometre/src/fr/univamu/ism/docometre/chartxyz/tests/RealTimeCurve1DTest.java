package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;
import fr.univamu.ism.docometre.chartxyz.windows.Curve1D;

/**
 * Démonstration de l'affichage de plusieurs signaux y(t) en temps réel : un
 * thread simule une acquisition toutes les 20 ms. Deux voies sont
 * échantillonnées à 1000 Hz, la troisième à 100 Hz. Seules les
 * WINDOW_TIME_WIDTH dernières secondes sont affichées, en balayage de type
 * oscilloscope. Espace : pause / reprise ; M : balayage / défilement.
 */
public class RealTimeCurve1DTest {

    private static final double FAST_FREQUENCY = 1000;    // Hz
    private static final double SLOW_FREQUENCY = 100;     // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s
    private static final int PERIOD_MS = 20;              // un envoi toutes les 20 ms

    public static void main(String[] args) {
        Curve1DData sine = new Curve1DData(FAST_FREQUENCY, WINDOW_TIME_WIDTH).setName("sinus 1 Hz").setLineWidth(3);
        Curve1DData noisy = new Curve1DData(FAST_FREQUENCY, WINDOW_TIME_WIDTH).setName("sinus 7 Hz + bruit").setLineWidth(1);
        Curve1DData drift = new Curve1DData(SLOW_FREQUENCY, WINDOW_TIME_WIDTH).setName("dérive (100 Hz)").setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Signaux temps réel — SWT + JOGL");
        shell.setSize(1000, 500);
        shell.setLayout(new FillLayout());

        Curve1D graph = new Curve1D(shell, -10, 10, sine, noisy, drift);
        graph.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        Random random = new Random();
        long[] fastIndex = {0}, slowIndex = {0};
        int fastBlock = (int) (FAST_FREQUENCY * PERIOD_MS / 1000);
        int slowBlock = (int) (SLOW_FREQUENCY * PERIOD_MS / 1000);
        acquisition.scheduleAtFixedRate(() -> {
            float[] a = new float[fastBlock], b = new float[fastBlock];
            for (int i = 0; i < fastBlock; i++) {
                double t = fastIndex[0]++ / FAST_FREQUENCY;
                a[i] = (float) Math.sin(2 * Math.PI * 1.0 * t);
                b[i] = (float) (0.4 * Math.sin(2 * Math.PI * 7.0 * t) - 0.5 + 0.05 * random.nextGaussian());
            }
            float[] c = new float[slowBlock];
            for (int i = 0; i < slowBlock; i++) {
                double t = slowIndex[0]++ / SLOW_FREQUENCY;
                c[i] = (float) (0.8 * Math.sin(2 * Math.PI * 0.05 * t));
            }
            sine.addSamples(a);
            noisy.addSamples(b);
            drift.addSamples(c);
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        acquisition.shutdownNow();
        display.dispose();
    }
}
