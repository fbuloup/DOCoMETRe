package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Graphe 3D dans un GLCanvas SWT, pour Windows et Linux.
 * Toutes les fonctionnalités sont dans {@link GLChart3D}.
 */
public class Chart3D extends GLChart3D {

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public Chart3D(Composite container, double historySize, Curve3DData... curves) {
        super(container, GLToolkit.SWT, historySize, curves);
    }

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public Chart3D(Composite container, double historySize, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.SWT, historySize, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }

    /** Chaque courbe garde sa propre durée d'historique (données statiques, par exemple). */
    public Chart3D(Composite container, Curve3DData... curves) {
        super(container, GLToolkit.SWT, curves);
    }

    /** Échelles fixes ; chaque courbe garde sa propre durée d'historique. */
    public Chart3D(Composite container, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.SWT, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }
}
