package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * 1D chart (signals y(t)) in a NEWT window, for macOS.
 * All the features are in {@link GLChart1D}.
 */
public class NewtChart1D extends GLChart1D {

    /** @param windowTimeWidth duration of the displayed window, in seconds */
    public NewtChart1D(Composite container, double windowTimeWidth, Curve1DData... curves) {
        super(container, GLToolkit.NEWT, windowTimeWidth, curves);
    }

    /** @param windowTimeWidth duration of the displayed window, in seconds */
    public NewtChart1D(Composite container, double windowTimeWidth, double yMin, double yMax, Curve1DData... curves) {
        super(container, GLToolkit.NEWT, windowTimeWidth, yMin, yMax, curves);
    }

}
