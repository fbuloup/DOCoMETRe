package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;
import fr.univamu.ism.docometre.chartxyz.windows.Curve1D;
import fr.univamu.ism.docometre.chartxyz.windows.Curve2D;
import fr.univamu.ism.docometre.chartxyz.windows.Curve3D;

/**
 * Démonstration de l'affichage de plusieurs signaux y(t) en temps réel : un
 * thread simule une acquisition toutes les 20 ms. Deux voies sont
 * échantillonnées à 1000 Hz, la troisième à 100 Hz. Seules les
 * WINDOW_TIME_WIDTH dernières secondes sont affichées, en balayage de type
 * oscilloscope. Espace : pause / reprise ; M : balayage / défilement.
 */
public class RealTimeCurvesTest {

    private static final double FAST_FREQUENCY = 1000;    // Hz
    private static final double SLOW_FREQUENCY = 100;     // Hz
    private static final double WINDOW_TIME_WIDTH1 = 15;    // s
    private static final int PERIOD_MS = 20;              // un envoi toutes les 20 ms
    
    private static final double SAMPLE_FREQUENCY2 = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH2 = 5;    // s
    private static final int BLOCK_SIZE = 20;             // échantillons par envoi (=> un envoi toutes les 20 ms)
    
    private static final double SAMPLE_FREQUENCY3 = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH3 = 5;    // s
    private static final int BLOCK_SIZE3 = 20; 

    public static void main(String[] args) {
        Curve1DData sine = new Curve1DData(FAST_FREQUENCY, WINDOW_TIME_WIDTH1).setName("sinus 1 Hz").setLineWidth(3);
        Curve1DData noisy = new Curve1DData(FAST_FREQUENCY, WINDOW_TIME_WIDTH1).setName("sinus 7 Hz + bruit").setLineWidth(1);
        Curve1DData drift = new Curve1DData(SLOW_FREQUENCY, WINDOW_TIME_WIDTH1).setName("dérive (100 Hz)").setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);

        Curve2DData lissajous = new Curve2DData(SAMPLE_FREQUENCY2, WINDOW_TIME_WIDTH2).setName("Lissajous");
        Curve2DData circle = new Curve2DData(SAMPLE_FREQUENCY2, WINDOW_TIME_WIDTH2).setName("cercle").setLineStyle(LineStyle.DASH);

        Curve3DData spiral = new Curve3DData(SAMPLE_FREQUENCY3, WINDOW_TIME_WIDTH3).setName("spirale");
        Curve3DData helix = new Curve3DData(SAMPLE_FREQUENCY3, WINDOW_TIME_WIDTH3).setName("hélice").setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);
        
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Signaux temps réel — SWT + JOGL");
        shell.setSize(1000, 500);
        shell.setLayout(new GridLayout(2, true));

        Curve1D graph1D = new Curve1D(shell, -10, 10, sine, noisy, drift);
        graph1D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph1D.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph1D.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph1D.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        ScheduledExecutorService acquisition1D = Executors.newSingleThreadScheduledExecutor();
        Random random = new Random();
        long[] fastIndex = {0}, slowIndex = {0};
        int fastBlock = (int) (FAST_FREQUENCY * PERIOD_MS / 1000);
        int slowBlock = (int) (SLOW_FREQUENCY * PERIOD_MS / 1000);
        acquisition1D.scheduleAtFixedRate(() -> {
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
        
        Curve2D graph2D = new Curve2D(shell, -10, 10, -10, 10, lissajous, circle);
        graph2D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph2D.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        // Simulation d'acquisition : figure de Lissajous à amplitude variable et cercle qui dérive
        ScheduledExecutorService acquisition2D = Executors.newSingleThreadScheduledExecutor();
        long[] sampleIndex = {0};
        long periodMicros = Math.round(BLOCK_SIZE * 1e6 / SAMPLE_FREQUENCY2);
        acquisition2D.scheduleAtFixedRate(() -> {
            float[] x1 = new float[BLOCK_SIZE], y1 = new float[BLOCK_SIZE];
            float[] x2 = new float[BLOCK_SIZE], y2 = new float[BLOCK_SIZE];
            for (int i = 0; i < BLOCK_SIZE; i++) {
                double t = sampleIndex[0]++ / SAMPLE_FREQUENCY2;
                x1[i] = (float) (Math.sin(2 * Math.PI * 0.7 * t) * (1 + 0.5 * Math.sin(0.3 * t)));
                y1[i] = (float) (0.5 * Math.sin(2 * Math.PI * 1.1 * t + 0.1 * t));
                x2[i] = (float) (0.3 * Math.cos(2 * Math.PI * 0.5 * t) + 0.5 * Math.sin(0.2 * t));
                y2[i] = (float) (0.3 * Math.sin(2 * Math.PI * 0.5 * t));
            }
            lissajous.addSamples(x1, y1);
            circle.addSamples(x2, y2);
        }, 0, periodMicros, TimeUnit.MICROSECONDS);
        
        
        Curve3D graph3D = new Curve3D(shell, spiral, helix);
        graph3D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph3D.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras

        // Simulation d'acquisition : spirale de Lissajous dont la phase dérive lentement
        ScheduledExecutorService acquisition3D = Executors.newSingleThreadScheduledExecutor();
        long[] sampleIndex3 = {0};
        long periodMicros3 = Math.round(BLOCK_SIZE3 * 1e6 / SAMPLE_FREQUENCY3);
        acquisition3D.scheduleAtFixedRate(() -> {
            float[] x = new float[BLOCK_SIZE3], y = new float[BLOCK_SIZE3], z = new float[BLOCK_SIZE3];
            float[] hx = new float[BLOCK_SIZE3], hy = new float[BLOCK_SIZE3], hz = new float[BLOCK_SIZE3];
            for (int i = 0; i < BLOCK_SIZE3; i++) {
                double t = sampleIndex3[0]++ / SAMPLE_FREQUENCY3;
                x[i] = (float) Math.sin(2 * Math.PI * 0.7 * t);
                y[i] = (float) Math.sin(2 * Math.PI * 0.3 * t + 0.05 * t);
                z[i] = (float) (Math.cos(2 * Math.PI * 1.1 * t) * (1 + 0.3 * Math.sin(0.2 * t)));
                hx[i] = (float) (0.4 * Math.cos(2 * Math.PI * 0.8 * t));
                hz[i] = (float) (0.4 * Math.sin(2 * Math.PI * 0.8 * t));
                hy[i] = (float) Math.sin(2 * Math.PI * 0.1 * t);
            }
            spiral.addSamples(x, y, z);
            helix.addSamples(hx, hy, hz);
        }, 0, periodMicros3, TimeUnit.MICROSECONDS);
        

        Curve1DData last = new Curve1DData(FAST_FREQUENCY, WINDOW_TIME_WIDTH1).setName("sinus 1 Hz").setLineWidth(3);
        float[] a = new float[5000];
        for (int i = 0; i < 5000; i++) {
            double t = i / SAMPLE_FREQUENCY2;
            a[i] = (float) (0.8 * Math.sin(2 * Math.PI * 0.05 * t));
        }
        last.addSamples(a);
        Curve1D graph1DBIS = new Curve1D(shell, -10, 10, last);
        graph1DBIS.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph1DBIS.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph1DBIS.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph1DBIS.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses
        graph1DBIS.setLegendVisible(false);
        
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        acquisition1D.shutdownNow();
        acquisition2D.shutdownNow();
        acquisition3D.shutdownNow();
        display.dispose();
    }
}
