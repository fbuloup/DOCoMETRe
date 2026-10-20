package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLChart2D;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Graphe 2D dans un GLCanvas SWT, pour Windows et Linux.
 * Toutes les fonctionnalités sont dans {@link GLChart2D}.
 */
public class Chart2D extends GLChart2D {

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public Chart2D(Composite container, double historySize, Curve2DData... curves) {
        super(container, GLToolkit.SWT, historySize, curves);
    }

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public Chart2D(Composite container, double historySize, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.SWT, historySize, xMin, xMax, yMin, yMax, curves);
    }

    /** Chaque courbe garde sa propre durée d'historique (données statiques, par exemple). */
    public Chart2D(Composite container, Curve2DData... curves) {
        super(container, GLToolkit.SWT, curves);
    }

    /** Échelles fixes ; chaque courbe garde sa propre durée d'historique. */
    public Chart2D(Composite container, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.SWT, xMin, xMax, yMin, yMax, curves);
    }
}
