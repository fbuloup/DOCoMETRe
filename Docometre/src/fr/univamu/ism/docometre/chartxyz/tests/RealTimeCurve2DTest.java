package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;
import fr.univamu.ism.docometre.chartxyz.windows.Curve2D;

/**
 * Démonstration de l'affichage 2D temps réel de deux courbes : un thread
 * simule une acquisition à SAMPLE_FREQUENCY Hz et envoie les échantillons
 * par blocs de BLOCK_SIZE. Seules les WINDOW_TIME_WIDTH dernières secondes
 * sont affichées.
 */
public class RealTimeCurve2DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s
    private static final int BLOCK_SIZE = 20;             // échantillons par envoi (=> un envoi toutes les 20 ms)

    public static void main(String[] args) {
        Curve2DData lissajous = new Curve2DData(SAMPLE_FREQUENCY, WINDOW_TIME_WIDTH).setName("Lissajous");
        Curve2DData circle = new Curve2DData(SAMPLE_FREQUENCY, WINDOW_TIME_WIDTH).setName("cercle").setLineStyle(LineStyle.DASH);

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Courbes 2D temps réel — SWT + JOGL");
        shell.setSize(1000, 700);
        shell.setLayout(new FillLayout());

        Curve2D graph = new Curve2D(shell, -10, 10, -10, 10, lissajous, circle);
        graph.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        // Simulation d'acquisition : figure de Lissajous à amplitude variable et cercle qui dérive
        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        long[] sampleIndex = {0};
        long periodMicros = Math.round(BLOCK_SIZE * 1e6 / SAMPLE_FREQUENCY);
        acquisition.scheduleAtFixedRate(() -> {
            float[] x1 = new float[BLOCK_SIZE], y1 = new float[BLOCK_SIZE];
            float[] x2 = new float[BLOCK_SIZE], y2 = new float[BLOCK_SIZE];
            for (int i = 0; i < BLOCK_SIZE; i++) {
                double t = sampleIndex[0]++ / SAMPLE_FREQUENCY;
                x1[i] = (float) (Math.sin(2 * Math.PI * 0.7 * t) * (1 + 0.5 * Math.sin(0.3 * t)));
                y1[i] = (float) (0.5 * Math.sin(2 * Math.PI * 1.1 * t + 0.1 * t));
                x2[i] = (float) (0.3 * Math.cos(2 * Math.PI * 0.5 * t) + 0.5 * Math.sin(0.2 * t));
                y2[i] = (float) (0.3 * Math.sin(2 * Math.PI * 0.5 * t));
            }
            lissajous.addSamples(x1, y1);
            circle.addSamples(x2, y2);
        }, 0, periodMicros, TimeUnit.MICROSECONDS);

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
