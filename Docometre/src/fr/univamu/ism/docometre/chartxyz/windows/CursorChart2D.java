package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLCursorChart2D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 2D chart of the last values only (cursor), in an SWT GLCanvas, for Windows and Linux.
 * All the features are in {@link GLCursorChart2D}.
 */
public class CursorChart2D extends GLCursorChart2D {

    /** Dynamic scales. */
    public CursorChart2D(Composite container, Curve2DData... curves) {
        super(container, GLToolkit.SWT, curves);
    }

    /** Fixed scales. */
    public CursorChart2D(Composite container, double xMin, double xMax, double yMin, double yMax, Curve2DData... curves) {
        super(container, GLToolkit.SWT, xMin, xMax, yMin, yMax, curves);
    }
}
