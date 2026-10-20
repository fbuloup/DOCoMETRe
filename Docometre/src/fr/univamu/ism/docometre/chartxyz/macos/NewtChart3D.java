package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Graphe 3D dans une fenêtre NEWT, pour macOS.
 * Toutes les fonctionnalités sont dans {@link GLChart3D}.
 */
public class NewtChart3D extends GLChart3D {

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public NewtChart3D(Composite container, double historySize, Curve3DData... curves) {
        super(container, GLToolkit.NEWT, historySize, curves);
    }

    /** @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes */
    public NewtChart3D(Composite container, double historySize, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.NEWT, historySize, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }

    /** Chaque courbe garde sa propre durée d'historique (données statiques, par exemple). */
    public NewtChart3D(Composite container, Curve3DData... curves) {
        super(container, GLToolkit.NEWT, curves);
    }

    /** Échelles fixes ; chaque courbe garde sa propre durée d'historique. */
    public NewtChart3D(Composite container, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.NEWT, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }
}
