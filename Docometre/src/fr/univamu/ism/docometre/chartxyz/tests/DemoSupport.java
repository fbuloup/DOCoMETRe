package fr.univamu.ism.docometre.chartxyz.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLComponent;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * Outils communs aux programmes de démonstration : fenêtre et boucle
 * d'événements SWT, simulations d'acquisition, chargement des données.
 */
final class DemoSupport {

    /** Un envoi toutes les 20 ms. */
    static final int PERIOD_MS = 20;

    private DemoSupport() {
    }

    /** Fenêtre de la taille donnée, à disposition en FillLayout. */
    static Shell createShell(Display display, String title, int width, int height) {
        Shell shell = new Shell(display);
        shell.setText(title);
        shell.setSize(width, height);
        shell.setLayout(new FillLayout());
        return shell;
    }

    /** Ouvre la fenêtre, traite les événements jusqu'à sa fermeture, puis arrête les acquisitions et libère le Display. */
    static void run(Shell shell, ScheduledExecutorService... acquisitions) {
        Display display = shell.getDisplay();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        for (ScheduledExecutorService acquisition : acquisitions) acquisition.shutdownNow();
        display.dispose();
    }

    /**
     * Ajoute un bouton "PUSH" qui ferme le composant s'il est affiché, ou en
     * crée un nouveau (avec factory) s'il a été fermé.
     */
    static void addCloseReopenButton(Shell shell, GLComponent[] component, Supplier<GLComponent> factory) {
        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!component[0].isDisposed()) component[0].close();
                else {
                    component[0] = factory.get();
                    component[0].moveAbove(button);
                }
                shell.layout();
            }
        });
    }

    /**
     * Simulation d'acquisition 1D : sinus 1 Hz, sinus 7 Hz bruité (même
     * fréquence d'échantillonnage) et dérive lente (fréquence de drift).
     */
    static ScheduledExecutorService startSignals(Curve1DData sine, Curve1DData noisy, Curve1DData drift) {
        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        Random random = new Random();
        long[] fastIndex = {0}, slowIndex = {0};
        double fastFrequency = sine.getSampleFrequency(), slowFrequency = drift.getSampleFrequency();
        int fastBlock = (int) (fastFrequency * PERIOD_MS / 1000);
        int slowBlock = (int) (slowFrequency * PERIOD_MS / 1000);
        acquisition.scheduleAtFixedRate(() -> {
            float[] a = new float[fastBlock], b = new float[fastBlock];
            for (int i = 0; i < fastBlock; i++) {
                double t = fastIndex[0]++ / fastFrequency;
                a[i] = (float) Math.sin(2 * Math.PI * 1.0 * t);
                b[i] = (float) (0.4 * Math.sin(2 * Math.PI * 7.0 * t) - 0.5 + 0.05 * random.nextGaussian());
            }
            float[] c = new float[slowBlock];
            for (int i = 0; i < slowBlock; i++) {
                double t = slowIndex[0]++ / slowFrequency;
                c[i] = (float) (0.8 * Math.sin(2 * Math.PI * 0.05 * t));
            }
            sine.addSamples(a);
            noisy.addSamples(b);
            drift.addSamples(c);
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        return acquisition;
    }

    /** Simulation d'acquisition 2D : figure de Lissajous à amplitude variable et cercle qui dérive. */
    static ScheduledExecutorService startLissajous(Curve2DData lissajous, Curve2DData circle) {
        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        double frequency = lissajous.getSampleFrequency();
        int block = (int) (frequency * PERIOD_MS / 1000);
        long[] sampleIndex = {0};
        acquisition.scheduleAtFixedRate(() -> {
            float[] x1 = new float[block], y1 = new float[block];
            float[] x2 = new float[block], y2 = new float[block];
            for (int i = 0; i < block; i++) {
                double t = sampleIndex[0]++ / frequency;
                x1[i] = (float) (Math.sin(2 * Math.PI * 0.7 * t) * (1 + 0.5 * Math.sin(0.3 * t)));
                y1[i] = (float) (0.5 * Math.sin(2 * Math.PI * 1.1 * t + 0.1 * t));
                x2[i] = (float) (0.3 * Math.cos(2 * Math.PI * 0.5 * t) + 0.5 * Math.sin(0.2 * t));
                y2[i] = (float) (0.3 * Math.sin(2 * Math.PI * 0.5 * t));
            }
            lissajous.addSamples(x1, y1);
            circle.addSamples(x2, y2);
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        return acquisition;
    }

    /** Simulation d'acquisition 3D : spirale de Lissajous dont la phase dérive lentement, et hélice. */
    static ScheduledExecutorService startSpiral(Curve3DData spiral, Curve3DData helix) {
        ScheduledExecutorService acquisition = Executors.newSingleThreadScheduledExecutor();
        double frequency = spiral.getSampleFrequency();
        int block = (int) (frequency * PERIOD_MS / 1000);
        long[] sampleIndex = {0};
        acquisition.scheduleAtFixedRate(() -> {
            float[] x = new float[block], y = new float[block], z = new float[block];
            float[] hx = new float[block], hy = new float[block], hz = new float[block];
            for (int i = 0; i < block; i++) {
                double t = sampleIndex[0]++ / frequency;
                x[i] = (float) Math.sin(2 * Math.PI * 0.7 * t);
                y[i] = (float) Math.sin(2 * Math.PI * 0.3 * t + 0.05 * t);
                z[i] = (float) (Math.cos(2 * Math.PI * 1.1 * t) * (1 + 0.3 * Math.sin(0.2 * t)));
                hx[i] = (float) (0.4 * Math.cos(2 * Math.PI * 0.8 * t));
                hz[i] = (float) (0.4 * Math.sin(2 * Math.PI * 0.8 * t));
                hy[i] = (float) Math.sin(2 * Math.PI * 0.1 * t);
            }
            spiral.addSamples(x, y, z);
            helix.addSamples(hx, hy, hz);
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        return acquisition;
    }

    /** Courbe 3D depuis le fichier args[0] (ou data/sample_curve.txt) ; null si le fichier est illisible. */
    static Curve3DData loadCurve(String[] args) {
        String dataFile = (args.length > 0) ? args[0] : "data/sample_curve.txt";
        try {
            return Curve3DData.loadFromFile(Paths.get(dataFile));
        } catch (IOException e) {
            System.err.println("Impossible de charger le fichier de points : " + dataFile);
            e.printStackTrace();
            return null;
        }
    }

    /** Grille depuis le fichier args[0] (ou data/sample_surface.txt) ; null si le fichier est illisible. */
    static SurfaceData loadSurface(String[] args) {
        String dataFile = (args.length > 0) ? args[0] : "data/sample_surface.txt";
        try {
            return SurfaceData.loadFromFile(Paths.get(dataFile));
        } catch (IOException e) {
            System.err.println("Impossible de charger le fichier de grille : " + dataFile);
            e.printStackTrace();
            return null;
        }
    }
}
