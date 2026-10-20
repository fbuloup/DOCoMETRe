package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Graphe 1D (signaux y(t)) dans une fenêtre NEWT, pour macOS.
 * Toutes les fonctionnalités sont dans {@link GLChart1D}.
 */
public class NewtChart1D extends GLChart1D {

    /** @param windowTimeWidth durée de la fenêtre affichée, en secondes */
    public NewtChart1D(Composite container, double windowTimeWidth, Curve1DData... curves) {
        super(container, GLToolkit.NEWT, windowTimeWidth, curves);
    }

    /** @param windowTimeWidth durée de la fenêtre affichée, en secondes */
    public NewtChart1D(Composite container, double windowTimeWidth, double yMin, double yMax, Curve1DData... curves) {
        super(container, GLToolkit.NEWT, windowTimeWidth, yMin, yMax, curves);
    }

}
