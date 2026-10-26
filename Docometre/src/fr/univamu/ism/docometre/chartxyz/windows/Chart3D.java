package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 3D chart in an SWT GLCanvas, for Windows and Linux.
 * All the features are in {@link GLChart3D}.
 */
public class Chart3D extends GLChart3D {

    /** @param historySize history duration (length of the trails), in seconds, imposed on all the curves */
    public Chart3D(Composite container, double historySize, Curve3DData... curves) {
        super(container, GLToolkit.SWT, historySize, curves);
    }

    /** @param historySize history duration (length of the trails), in seconds, imposed on all the curves */
    public Chart3D(Composite container, double historySize, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.SWT, historySize, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }

    /** Each curve keeps its own history duration (static data, for example). */
    public Chart3D(Composite container, Curve3DData... curves) {
        super(container, GLToolkit.SWT, curves);
    }

    /** Fixed scales; each curve keeps its own history duration. */
    public Chart3D(Composite container, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, GLToolkit.SWT, xMin, xMax, yMin, yMax, zMin, zMax, curves);
    }
}
