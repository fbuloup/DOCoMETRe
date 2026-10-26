package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLChart2D;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 2D chart in a NEWT window, for macOS.
 * All the features are in {@link GLChart2D}.
 */
public class NewtChart2D extends GLChart2D {

    /** @param historySize history duration (length of the trails), in seconds, imposed on all the curves */
    public NewtChart2D(Composite container, double historySize, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, historySize, curves);
    }

    /** @param historySize history duration (length of the trails), in seconds, imposed on all the curves */
    public NewtChart2D(Composite container, double historySize, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, historySize, xMin, xMax, yMin, yMax, curves);
    }

    /** Each curve keeps its own history duration (static data, for example). */
    public NewtChart2D(Composite container, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, curves);
    }

    /** Fixed scales; each curve keeps its own history duration. */
    public NewtChart2D(Composite container, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, xMin, xMax, yMin, yMax, curves);
    }
}
