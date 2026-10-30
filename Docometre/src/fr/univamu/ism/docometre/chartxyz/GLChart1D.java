package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.rtswtchart.IRTSWTOscilloChart;

/**
 * Chart of real-time signals y(t) (see {@link Chart1DRenderer}). The
 * duration of the time window belongs to the chart and is imposed on all its
 * curves.
 */
public class GLChart1D extends AbstractGLPlotChart<Curve1DData, Chart1DRenderer> implements IRTSWTOscilloChart {

	public static GLChart1D createChart(Composite container, double windowTimeWidth, Curve1DData... curves) {
		return new GLChart1D(container, GLToolkit.platformDefault(), windowTimeWidth, curves);
	}
	

    /**
     * Creates the chart with one or several curves (others can be added later
     * with addCurve), with a dynamic vertical scale: it is continuously fitted
     * to the curve values.
     *
     * @param windowTimeWidth duration of the displayed window, in seconds
     * @throws IllegalStateException if a curve is already displayed in another chart
     */
    public GLChart1D(Composite container, GLToolkit toolkit, double windowTimeWidth, Curve1DData... curves) {
        super(container, toolkit, new Chart1DRenderer(windowTimeWidth, curves));
    }

    /**
     * Creates the chart with a fixed scale for the vertical axis: it remains
     * equal to the given bounds whatever the curve values (what exceeds them
     * is clipped). setDynamicScales() goes back to the dynamic scale.
     *
     * @param windowTimeWidth duration of the displayed window, in seconds
     * @throws IllegalArgumentException if yMin is not strictly less than yMax
     */
    public GLChart1D(Composite container, GLToolkit toolkit, double windowTimeWidth, double yMin, double yMax,
                     Curve1DData... curves) {
        super(container, toolkit, withScales(new Chart1DRenderer(windowTimeWidth, curves), yMin, yMax));
    }

    private static Chart1DRenderer withScales(Chart1DRenderer renderer, double yMin, double yMax) {
        renderer.setFixedScales(yMin, yMax);
        return renderer;
    }

    /** Chooses the display mode: oscilloscope-like sweep (default) or scroll. */
    public void setDisplayMode(Chart1DRenderer.DisplayMode displayMode) {
        renderer.setDisplayMode(displayMode);
    }

    public Chart1DRenderer.DisplayMode getDisplayMode() {
        return renderer.getDisplayMode();
    }

    /** Listener of the display mode, sweep or scroll (M key or {@link #setDisplayMode}). */
    public void addDisplayModeListener(DisplayModeListener listener) {
        renderer.addDisplayModeListener(listener);
    }

    public void removeDisplayModeListener(DisplayModeListener listener) {
        renderer.removeDisplayModeListener(listener);
    }

    /** Freezes / resumes the display (also with the space key); data keep being received. */
    public void setPaused(boolean paused) {
        renderer.setPaused(paused);
    }

    public boolean isPaused() {
        return renderer.isPaused();
    }

    /**
     * Label of the abscissa axis, at the right end of the axis, at the height
     * of the legend ("t(s)" by default). Null or blank: no label; if the
     * legend is not displayed below the axis either, the space is given back
     * to the plot area.
     */
    public void setXAxisLabel(String label) {
        renderer.setXAxisLabel(label);
    }

    /** Label of the abscissa axis, or null if there is none. */
    public String getXAxisLabel() {
        return renderer.getXAxisLabel();
    }

    /** Fixed scale for the vertical axis (see the corresponding constructor). */
    public void setFixedScales(double yMin, double yMax) {
        renderer.setFixedScales(yMin, yMax);
    }

    /** Duration of the displayed window, in seconds, imposed on all the curves of the chart. */
    @Override
    public void setWindowTimeWidth(double value) {
        renderer.setWindowTimeWidth(value);
    }

    public double getWindowTimeWidth() {
        return renderer.getWindowTimeWidth();
    }

    // --- Context menu ---

    /** Adds the pause (space key) and the display mode (M key) to the cursor. */
    @Override
    protected void fillModeMenuItems(Menu menu) {
        super.fillModeMenuItems(menu);
        addToggleMenuItem(menu, isPaused(), DocometreMessages.ChartMenu_Pause, DocometreMessages.ChartMenu_Resume,
                DocometreMessages.ChartMenu_SpaceKey, this::setPaused);
        boolean sweep = getDisplayMode() == Chart1DRenderer.DisplayMode.SWEEP;
        addMenuItem(menu, sweep ? DocometreMessages.ChartMenu_ScrollMode : DocometreMessages.ChartMenu_SweepMode, "M",
                () -> setDisplayMode(sweep ? Chart1DRenderer.DisplayMode.SCROLL : Chart1DRenderer.DisplayMode.SWEEP));
    }

    // --- IRTSWTOscilloChart: settings not supported ---

    @Override
    public void setShowCurrentValue(boolean value) {
    	renderer.setCurrentValuesVisible(value);
    }

    @Override
    public void dispose() {
    	close();
    }
}
