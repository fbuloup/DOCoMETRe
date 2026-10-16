package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.LineStyle;
import fr.univamu.ism.docometre.chartxyz.windows.Curve3D;

/**
 * Démonstration de l'affichage temps réel de deux courbes 3D : un thread simule une acquisition
 * à SAMPLE_FREQUENCY Hz et envoie les échantillons par blocs de BLOCK_SIZE.
 * Seules les WINDOW_TIME_WIDTH dernières secondes sont affichées.
 */
public class RealTimeCurve3DTest {

    private static final double SAMPLE_FREQUENCY = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH = 5;    // s
    private static final int BLOCK_SIZE = 20;             // échantillons par envoi (=> un envoi toutes les 20 ms)

    public static void main(String[] args) {
        Curve3DData spiral = new Curve3DData(SAMPLE_FREQUENCY, WINDOW_TIME_WIDTH).setName("spirale");
        Curve3DData helix = new Curve3DData(SAMPLE_FREQUENCY, WINDOW_TIME_WIDTH).setName("hélice").setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Courbe 3D temps réel — SWT + JOGL");
        shell.setSize(1000, 700);
        shell.setLayout(new FillLayout());

        new Curve3D(shell, -0.5, 0.5, -0.5, 0.5, -0.5, 0.5, spiral, helix);

        // Simulation d'acquisition : spirale de Lissajous dont la phase dérive lentement
        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        long[] sampleIndex = {0};
        long periodMicros = Math.round(BLOCK_SIZE * 1e6 / SAMPLE_FREQUENCY);
        acquisition.scheduleAtFixedRate(() -> {
            float[] x = new float[BLOCK_SIZE], y = new float[BLOCK_SIZE], z = new float[BLOCK_SIZE];
            float[] hx = new float[BLOCK_SIZE], hy = new float[BLOCK_SIZE], hz = new float[BLOCK_SIZE];
            for (int i = 0; i < BLOCK_SIZE; i++) {
                double t = sampleIndex[0]++ / SAMPLE_FREQUENCY;
                x[i] = (float) Math.sin(2 * Math.PI * 0.7 * t);
                y[i] = (float) Math.sin(2 * Math.PI * 0.3 * t + 0.05 * t);
                z[i] = (float) (Math.cos(2 * Math.PI * 1.1 * t) * (1 + 0.3 * Math.sin(0.2 * t)));
                hx[i] = (float) (0.4 * Math.cos(2 * Math.PI * 0.8 * t));
                hz[i] = (float) (0.4 * Math.sin(2 * Math.PI * 0.8 * t));
                hy[i] = (float) Math.sin(2 * Math.PI * 0.1 * t);
            }
            spiral.addSamples(x, y, z);
            helix.addSamples(hx, hy, hz);
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
