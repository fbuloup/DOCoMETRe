package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.concurrent.ScheduledExecutorService;

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
import fr.univamu.ism.docometre.chartxyz.windows.Chart1D;
import fr.univamu.ism.docometre.chartxyz.windows.Chart2D;
import fr.univamu.ism.docometre.chartxyz.windows.Chart3D;

/**
 * Démonstration de l'affichage de plusieurs signaux y(t) en temps réel : un
 * thread simule une acquisition toutes les 20 ms. Deux voies sont
 * échantillonnées à 1000 Hz, la troisième à 100 Hz. Seules les
 * WINDOW_TIME_WIDTH dernières secondes sont affichées, en balayage de type
 * oscilloscope. Espace : pause / reprise ; M : balayage / défilement.
 */
public class RealTimeChartsTest {

    private static final double FAST_FREQUENCY = 1000;    // Hz
    private static final double SLOW_FREQUENCY = 100;     // Hz
    private static final double WINDOW_TIME_WIDTH1 = 15;    // s
    
    private static final double SAMPLE_FREQUENCY2 = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH2 = 5;    // s
    
    private static final double SAMPLE_FREQUENCY3 = 1000;  // Hz
    private static final double WINDOW_TIME_WIDTH3 = 5;    // s

    public static void main(String[] args) {
        Curve1DData sine = new Curve1DData(FAST_FREQUENCY).setName("sinus 1 Hz").setLineWidth(3);
        Curve1DData noisy = new Curve1DData(FAST_FREQUENCY).setName("sinus 7 Hz + bruit").setLineWidth(1);
        Curve1DData drift = new Curve1DData(SLOW_FREQUENCY).setName("dérive (100 Hz)").setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);

        Curve2DData lissajous = new Curve2DData(SAMPLE_FREQUENCY2).setName("Lissajous");
        Curve2DData circle = new Curve2DData(SAMPLE_FREQUENCY2).setName("cercle").setLineStyle(LineStyle.DASH);

        Curve3DData spiral = new Curve3DData(SAMPLE_FREQUENCY3).setName("spirale");
        Curve3DData helix = new Curve3DData(SAMPLE_FREQUENCY3).setName("hélice").setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);
        
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Signaux temps réel — SWT + JOGL");
        shell.setSize(1000, 500);
        shell.setLayout(new GridLayout(2, true));

        Chart1D graph1D = new Chart1D(shell, WINDOW_TIME_WIDTH1, -10, 10, sine, noisy, drift);
        graph1D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph1D.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph1D.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph1D.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        ScheduledExecutorService acquisition1D = DemoSupport.startSignals(sine, noisy, drift);

        Chart2D graph2D = new Chart2D(shell, WINDOW_TIME_WIDTH2, -10, 10, -10, 10, lissajous, circle);
        graph2D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph2D.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses

        ScheduledExecutorService acquisition2D = DemoSupport.startLissajous(lissajous, circle);

        Chart3D graph3D = new Chart3D(shell, WINDOW_TIME_WIDTH3, spiral, helix);
        graph3D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph3D.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras

        ScheduledExecutorService acquisition3D = DemoSupport.startSpiral(spiral, helix);

        Curve1DData last = new Curve1DData(FAST_FREQUENCY).setName("sinus 1 Hz").setLineWidth(3);
        float[] a = new float[5000];
        for (int i = 0; i < 5000; i++) {
            double t = i / SAMPLE_FREQUENCY2;
            a[i] = (float) (0.8 * Math.sin(2 * Math.PI * 0.05 * t));
        }
        last.addSamples(a);
        Chart1D graph1DBIS = new Chart1D(shell, WINDOW_TIME_WIDTH1, -10, 10, last);
        graph1DBIS.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph1DBIS.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph1DBIS.setLegendFont(null, 11, SWT.BOLD); // police du système, en gras
        graph1DBIS.setLegendPosition(LegendPosition.BELOW); // légende sous l'axe des abscisses
        graph1DBIS.setLegendVisible(false);
        
        DemoSupport.run(shell, acquisition1D, acquisition2D, acquisition3D);
    }
}
