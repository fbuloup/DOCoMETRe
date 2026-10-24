package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Base of the real-time curve charts embedded in an SWT user interface: an
 * OpenGL surface ({@link GLHost}, SWT or NEWT depending on {@link GLToolkit})
 * and a renderer ({@link CurveChartRenderer}) whose settings are exposed here.
 *
 * The methods of the fr.univamu.ism.rtswtchart interfaces common to all
 * charts are also implemented here (subclasses declare the interface).
 *
 * @param <C> curve type
 * @param <R> renderer type
 */
public abstract class AbstractGLChart<C extends CurveData<C>, R extends CurveChartRenderer<C>> implements GLComponent {

    protected final R renderer;
    protected final GLHost host;

    /**
     * Creates the OpenGL surface in container and starts rendering. When the
     * chart is closed, its curves are removed from it: they can then be
     * displayed in another chart.
     *
     * @param renderer    renderer already configured (curves, scales)
     * @param depthBuffer true for a 3D scene
     */
    protected AbstractGLChart(Composite container, GLToolkit toolkit, R renderer, boolean depthBuffer) {
        this.renderer = renderer;
        try {
			this.host = GLHost.create(toolkit, container, depthBuffer, renderer, renderer::removeAllCurves);
		} catch (RuntimeException | Error e) {
			renderer.removeAllCurves();
			throw e;
		}
    }

    /** Renderer of the chart, for the settings that are not exposed here. */
    public R getRenderer() { return renderer; }

    @Override
    public void close() {
        host.close();
    }

    /** Stops the animation: the display is no longer refreshed. */
    public void stopAnimator() {
        host.stopAnimation();
    }
    
    /** Starts the animation: the display is refreshed. */
    public void startAnimator() {
        host.startAnimation();
    }

    @Override
    public boolean isDisposed() {
        return host.isDisposed();
    }

    @Override
    public void moveAbove(Control control) {
        host.getControl().moveAbove(control);
    }

    @Override
    public void setLayoutData(GridData gridData) {
        host.getControl().setLayoutData(gridData);
    }

    public GridData getLayoutData() {
        Object data = host.getControl().getLayoutData();
        return (data instanceof GridData) ? (GridData) data : null;
    }

    /**
     * Adds a curve to the chart (usable while rendering, from any thread).
     * Its name must be unique in the chart: otherwise nothing is changed.
     *
     * @throws IllegalStateException    if the curve is already displayed in a chart
     * @throws IllegalArgumentException if another curve of the chart has the same name
     */
    public void addCurve(C curve) {
        renderer.addCurve(curve);
    }

    /** Removes a curve from the chart. @return false if the curve was not in the chart */
    public boolean removeCurve(C curve) {
        return renderer.removeCurve(curve);
    }

    /** Removes the curve with this name from the chart. @return false if the chart has no curve with this name */
    public boolean removeCurve(String name) {
        return renderer.removeCurve(name);
    }

    /** Curve of the chart with this name, or null (usable from any thread). */
    public C getCurve(String name) {
        return renderer.getCurve(name);
    }

    /** Number of curves currently displayed in the chart (usable from any thread). */
    public int getCurvesCount() {
        return renderer.getCurvesCount();
    }

    /**
     * Font of the scale tick labels; axis names use the same family, bold
     * and larger.
     *
     * @param family family ("Arial", "Helvetica", "Courier New"...) or null for the system font
     * @param size   size in points
     * @param style  SWT.NORMAL, SWT.BOLD, SWT.ITALIC or SWT.BOLD | SWT.ITALIC
     */
    public void setScaleFont(String family, int size, int style) {
        renderer.setScaleFont(new ChartFont(family, size, style));
    }

    /** Font of the legend (same parameters as {@link #setScaleFont}). */
    public void setLegendFont(String family, int size, int style) {
        renderer.setLegendFont(new ChartFont(family, size, style));
    }

    /** Dynamic scales, continuously fitted to the curve values (default). */
    public void setDynamicScales() {
        renderer.setDynamicScales();
    }

    /** True if the scales are fixed. */
    public boolean hasFixedScales() {
        return renderer.hasFixedScales();
    }

    /** Shows or hides the legend (also with the N key when the chart has the focus). */
    public void setLegendVisible(boolean visible) {
        renderer.setLegendVisible(visible);
    }

    public boolean isLegendVisible() {
        return renderer.isLegendVisible();
    }

    /** Actual measured frame rate, in frames per second (target: {@link GLHost#FPS}). */
    public double getFps() {
        return renderer.getFps();
    }

    /** Shows or hides the frame rate in the chart (also with the F key). */
    public void setFpsVisible(boolean visible) {
        renderer.setFpsVisible(visible);
    }

    /** Shows or hides, at the top right of the chart, the last value of each curve (also with the V key). */
    public void setCurrentValuesVisible(boolean visible) {
        renderer.setCurrentValuesVisible(visible);
    }

    public boolean isCurrentValuesVisible() {
        return renderer.isCurrentValuesVisible();
    }

    /**
     * Enables or disables the reduction of the number of points sent to
     * OpenGL (enabled by default, also with the D key). The drawing is unchanged.
     */
    public void setDecimationEnabled(boolean enabled) {
        renderer.setDecimationEnabled(enabled);
    }

    public boolean isDecimationEnabled() {
        return renderer.isDecimationEnabled();
    }

    // ------------------------------------------------------------------
    // Methods common to the fr.univamu.ism.rtswtchart interfaces

    public void setAutoScale(boolean autoscale) {
        if (autoscale) setDynamicScales();
    }

    public void setGridVisibility(boolean value) {
        renderer.setGridVisibility(value);
    }

    public void setLegendVisibility(boolean value) {
        setLegendVisible(value);
    }

    /** Not supported: bounds are set together with setFixedScales(). */
    public void setyMax(double value) {
    }

    /** Not supported: bounds are set together with setFixedScales(). */
    public void setyMin(double value) {
    }

    /** Not supported: fixed color. */
    public void setGridLinesColor(Color color) {
    }

    /** Not supported: fixed color. */
    public void setFontColor(Color color) {
    }

    /** Not supported: curves are created by the caller, then added with addCurve(). */
    public IRTSWTSerie createSerie(String serieId, Color color) {
    	throw new UnsupportedOperationException("use addCurve(...)");
    }
}
