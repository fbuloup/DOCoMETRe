package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.rtswtchart.IRTSWTXYChart;

/**
 * Chart of real-time 2D curves (see {@link Chart2DRenderer}).
 */
public class GLChart2D extends AbstractGLPlotChart<Curve2DData, Chart2DRenderer> implements IRTSWTXYChart {

    /**
     * Creates the chart with the default toolkit of the platform and dynamic
     * scales (see {@link #GLChart2D(Composite, GLToolkit, double, Curve2DData...)}).
     *
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     */
    public static GLChart2D createChart(Composite container, double historySize, Curve2DData... curves) {
        return new GLChart2D(container, GLToolkit.platformDefault(), historySize, curves);
    }

    /**
     * Creates the chart with the default toolkit of the platform and dynamic
     * scales; each curve keeps its own history duration (static data, for
     * example: see {@link #GLChart2D(Composite, GLToolkit, Curve2DData...)}).
     */
    public static GLChart2D createChart(Composite container, Curve2DData... curves) {
        return new GLChart2D(container, GLToolkit.platformDefault(), curves);
    }

    /**
     * Creates the chart with dynamic scales, continuously fitted to the curve values.
     *
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     * @throws IllegalStateException if a curve is already displayed in another chart
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double historySize, Curve2DData... curves) {
        super(container, toolkit, new Chart2DRenderer(historySize, curves));
    }

    /**
     * Creates the chart with fixed scales for the x and y axes (see
     * {@link #GLChart2D(Composite, GLToolkit, double, double, double, double, Curve2DData...)}).
     *
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double historySize, double xMin, double xMax,
                     double yMin, double yMax, Curve2DData... curves) {
        super(container, toolkit, withScales(new Chart2DRenderer(historySize, curves), xMin, xMax, yMin, yMax));
    }

    /**
     * Creates the chart with dynamic scales; each curve keeps its own history
     * duration (static data, for example).
     */
    public GLChart2D(Composite container, GLToolkit toolkit, Curve2DData... curves) {
        super(container, toolkit, new Chart2DRenderer(curves));
    }

    /**
     * Creates the chart with fixed scales for the x and y axes (each curve
     * keeps its own history duration): they remain equal to the given bounds
     * whatever the curve values (what exceeds them is clipped).
     * setDynamicScales() goes back to dynamic scales.
     *
     * @throws IllegalArgumentException if a minimum is not strictly less than its maximum
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double xMin, double xMax, double yMin, double yMax,
                     Curve2DData... curves) {
        super(container, toolkit, withScales(new Chart2DRenderer(curves), xMin, xMax, yMin, yMax));
    }

    private static Chart2DRenderer withScales(Chart2DRenderer renderer, double xMin, double xMax, double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
        return renderer;
    }

    /** Fixed scales for the x and y axes (see the corresponding constructor). */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
    }

    /** Same scale on the x and y axes (also with the E key), useful for trajectories; disabled by default. */
    public void setEqualAspect(boolean equalAspect) {
        renderer.setEqualAspect(equalAspect);
    }

    public boolean isEqualAspect() {
        return renderer.isEqualAspect();
    }

    // --- Context menu ---

    /** Adds the fading of the trails. */
    @Override
    protected void fillRenderingMenuItems(Menu menu) {
        super.fillRenderingMenuItems(menu);
        addToggleMenuItem(menu, isTrailFading(), DocometreMessages.ChartMenu_EnableTrailFading,
                DocometreMessages.ChartMenu_DisableTrailFading, null, this::setTrailFading);
    }

    /** Adds the same scale on x and y (E key), before the zoom items. */
    @Override
    protected void fillViewMenuItems(Menu menu) {
        addToggleMenuItem(menu, isEqualAspect(), DocometreMessages.ChartMenu_EqualAspect,
                DocometreMessages.ChartMenu_IndependentAspect, "E", this::setEqualAspect);
        super.fillViewMenuItems(menu);
    }

    // --- IRTSWTXYChart ---

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
}
