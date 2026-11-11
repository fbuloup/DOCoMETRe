package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLCursorChart2D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 2D chart of the last values only (cursor), in a NEWT window, for macOS.
 * All the features are in {@link GLCursorChart2D}.
 */
public class NewtCursorChart2D extends GLCursorChart2D {

    /** Dynamic scales. */
    public NewtCursorChart2D(Composite container, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, curves);
    }

    /** Fixed scales. */
    public NewtCursorChart2D(Composite container, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.NEWT, xMin, xMax, yMin, yMax, curves);
    }
}
