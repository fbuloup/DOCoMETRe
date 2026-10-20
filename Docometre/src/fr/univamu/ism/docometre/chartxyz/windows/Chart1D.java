package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Graphe 1D (signaux y(t)) dans un GLCanvas SWT, pour Windows et Linux.
 * Toutes les fonctionnalités sont dans {@link GLChart1D}.
 */
public class Chart1D extends GLChart1D {

    /** @param windowTimeWidth durée de la fenêtre affichée, en secondes */
    public Chart1D(Composite container, double windowTimeWidth, Curve1DData... curves) {
        super(container, GLToolkit.SWT, windowTimeWidth, curves);
    }

    /** @param windowTimeWidth durée de la fenêtre affichée, en secondes */
    public Chart1D(Composite container, double windowTimeWidth, double yMin, double yMax, Curve1DData... curves) {
        super(container, GLToolkit.SWT, windowTimeWidth, yMin, yMax, curves);
    }

}
