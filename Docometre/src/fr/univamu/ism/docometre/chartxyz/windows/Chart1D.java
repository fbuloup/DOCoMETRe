package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 1D chart (signals y(t)) in an SWT GLCanvas, for Windows and Linux.
 * All the features are in {@link GLChart1D}.
 */
public class Chart1D extends GLChart1D {

    /** @param windowTimeWidth duration of the displayed window, in seconds */
    public Chart1D(Composite container, double windowTimeWidth, Curve1DData... curves) {
        super(container, GLToolkit.SWT, windowTimeWidth, curves);
    }

    /** @param windowTimeWidth duration of the displayed window, in seconds */
    public Chart1D(Composite container, double windowTimeWidth, double yMin, double yMax, Curve1DData... curves) {
        super(container, GLToolkit.SWT, windowTimeWidth, yMin, yMax, curves);
    }

}
