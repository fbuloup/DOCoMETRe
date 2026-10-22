package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;

/**
 * Chart of real-time 3D curves (see {@link Chart3DRenderer}).
 */
public class GLChart3D extends AbstractGLChart<Curve3DData, Chart3DRenderer> implements IRTSWTXYZChart {

    /**
     * Creates the chart with dynamic scales, continuously fitted to the curve values.
     *
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     * @throws IllegalStateException if a curve is already displayed in another chart
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double historySize, Curve3DData... curves) {
        super(container, toolkit, new Chart3DRenderer(historySize, curves), true);
    }

    /**
     * Creates the chart with fixed scales for the x, y and z axes (see
     * {@link #GLChart3D(Composite, GLToolkit, double, double, double, double, double, double, Curve3DData...)}).
     *
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double historySize, double xMin, double xMax,
                     double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, toolkit, withScales(new Chart3DRenderer(historySize, curves), xMin, xMax, yMin, yMax, zMin, zMax), true);
    }

    /**
     * Creates the chart with dynamic scales; each curve keeps its own history
     * duration (static data, for example).
     */
    public GLChart3D(Composite container, GLToolkit toolkit, Curve3DData... curves) {
        super(container, toolkit, new Chart3DRenderer(curves), true);
    }

    /**
     * Creates the chart with fixed scales for the x, y and z axes (each curve
     * keeps its own history duration): they remain equal to the given bounds
     * whatever the curve values (what leaves the box is clipped).
     * setDynamicScales() goes back to dynamic scales.
     *
     * @throws IllegalArgumentException if a minimum is not strictly less than its maximum
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double xMin, double xMax, double yMin, double yMax,
                     double zMin, double zMax, Curve3DData... curves) {
        super(container, toolkit, withScales(new Chart3DRenderer(curves), xMin, xMax, yMin, yMax, zMin, zMax), true);
    }

    private static Chart3DRenderer withScales(Chart3DRenderer renderer, double xMin, double xMax, double yMin, double yMax,
                                              double zMin, double zMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
        return renderer;
    }

    /** Fixed scales for the x, y and z axes (see the corresponding constructor). */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    // --- IRTSWTXYZChart ---

    /** Not applicable in 3D: the legend is always at the top left. */
    @Override
    public void setLegendPosition(int position) {
    }

    /** History duration (length of the trails), in seconds, imposed on all the curves of the chart. */
    @Override
    public void setHistorySize(double historySize) {
        renderer.setWindowTimeWidth(historySize);
    }

    /** History duration imposed on the curves, in seconds (0 if each curve keeps its own). */
    public double getHistorySize() {
        return renderer.getWindowTimeWidth();
    }

    /** Not supported: the sampling frequency is specific to each curve. */
    @Override
    public void setSampleFrequency(double sfx) {
    }

    @Override
    public void setWaitForAllSeriesToRedraw(boolean wait) {
    }

    @Override
    public String getMeanDrawTime() {
        return null;
    }

    /** This method must do nothing if the scale is dynamic:
     *  in that case f is null
     *  **/
	@Override
	public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
		double[] f = renderer.getFixedScaleBounds();
		if (f != null) renderer.setFixedScales(xMin, xMax, yMin, yMax, f[4], f[5]);
	}
}
