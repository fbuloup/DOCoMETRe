package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.rtswtchart.IRTSWTXYChart;

/**
 * Real-time 2D chart without history: only the last value received by each
 * curve is displayed, as a symbol whose appearance can be configured (filled
 * and outlined geometric shape, or PNG / JPEG image: see {@link CursorStyle}
 * and {@link CursorChart2DRenderer}). Typical use: display of the position
 * of a cursor (hand, effector...) in a workspace given by fixed scales.
 *
 * The other features are those of {@link GLChart2D} (zoom, pan, same scale on
 * x and y, legend, current values...), except the trails, the points and
 * the markers.
 */
public class GLCursorChart2D extends AbstractGLPlotChart<Curve2DData, CursorChart2DRenderer> implements IRTSWTXYChart {

    /** Creates the chart with the default toolkit of the platform and dynamic scales. */
    public static GLCursorChart2D createChart(Composite container, Curve2DData... curves) {
        return new GLCursorChart2D(container, GLToolkit.platformDefault(), curves);
    }

    /** Creates the chart with the default toolkit of the platform and fixed scales. */
    public static GLCursorChart2D createChart(Composite container, double xMin, double xMax, double yMin, double yMax,
                                              Curve2DData... curves) {
        return new GLCursorChart2D(container, GLToolkit.platformDefault(), xMin, xMax, yMin, yMax, curves);
    }

    /**
     * Creates the chart with dynamic scales: the view fits all the positions
     * displayed since the beginning (or the last reset of the view, R key).
     * The curves only keep their last sample.
     *
     * @throws IllegalStateException if a curve is already displayed in another chart
     */
    public GLCursorChart2D(Composite container, GLToolkit toolkit, Curve2DData... curves) {
        super(container, toolkit, new CursorChart2DRenderer(curves));
    }

    /**
     * Creates the chart with fixed scales for the x and y axes: they remain
     * equal to the given bounds whatever the curve values (a symbol outside
     * them is not visible). setDynamicScales() goes back to dynamic scales.
     *
     * @throws IllegalArgumentException if a minimum is not strictly less than its maximum
     */
    public GLCursorChart2D(Composite container, GLToolkit toolkit, double xMin, double xMax, double yMin, double yMax,
                           Curve2DData... curves) {
        super(container, toolkit, withScales(new CursorChart2DRenderer(curves), xMin, xMax, yMin, yMax));
    }

    private static CursorChart2DRenderer withScales(CursorChart2DRenderer renderer, double xMin, double xMax,
                                                    double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
        return renderer;
    }

    /** Fixed scales for the x and y axes (see the corresponding constructor). */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
    }

    /** Same scale on the x and y axes (also with the E key); disabled by default. */
    public void setEqualAspect(boolean equalAspect) {
        renderer.setEqualAspect(equalAspect);
    }

    public boolean isEqualAspect() {
        return renderer.isEqualAspect();
    }

    // --- Symbol of the points ---

    /**
     * Style of the curves that do not have their own (default:
     * {@link CursorStyle#DEFAULT}, disk in the color of the curve); null goes
     * back to the default style. Usable from any thread.
     */
    public void setCursorStyle(CursorStyle style) {
        renderer.setCursorStyle(style);
    }

    /** Style of the curves that do not have their own. */
    public CursorStyle getCursorStyle() {
        return renderer.getCursorStyle();
    }

    /**
     * Own style of a curve of the chart; null makes it use the default style
     * of the chart again. Usable from any thread.
     *
     * @throws IllegalArgumentException if the curve is not displayed in this chart
     */
    public void setCursorStyle(Curve2DData curve, CursorStyle style) {
        renderer.setCursorStyle(curve, style);
    }

    /** Style actually used for a curve: its own style, otherwise the default style of the chart. */
    public CursorStyle getCursorStyle(Curve2DData curve) {
        return renderer.getCursorStyle(curve);
    }

    /** Forgets the positions displayed so far (dynamic scales: the view fits the next positions only). */
    public void resetDataBounds() {
        renderer.resetDataBounds();
    }

    // --- Context menu ---

    /** Legend, its position and current values (no points: a single sample per curve). */
    @Override
    protected void fillCurvesMenuItems(Menu menu) {
        addToggleMenuItem(menu, isLegendVisible(), DocometreMessages.ChartMenu_ShowLegend,
                DocometreMessages.ChartMenu_HideLegend, "N", this::setLegendVisible);
        boolean below = getLegendPosition() == LegendPosition.BELOW;
        MenuItem item = addMenuItem(menu, below ? DocometreMessages.ChartMenu_LegendInside : DocometreMessages.ChartMenu_LegendBelow,
                null, () -> setLegendPosition(below ? LegendPosition.INSIDE : LegendPosition.BELOW));
        item.setEnabled(isLegendVisible());
        addToggleMenuItem(menu, isCurrentValuesVisible(), DocometreMessages.ChartMenu_ShowCurrentValues,
                DocometreMessages.ChartMenu_HideCurrentValues, "V", this::setCurrentValuesVisible);
    }

    /** No markers on this chart. */
    @Override
    protected void fillMarkersMenuItems(Menu menu) {
    }

    /** Frame rate only (nothing to reduce: a single point per curve). */
    @Override
    protected void fillRenderingMenuItems(Menu menu) {
        addToggleMenuItem(menu, isFpsVisible(), DocometreMessages.ChartMenu_ShowFps,
                DocometreMessages.ChartMenu_HideFps, "F", this::setFpsVisible);
    }

    /** Adds the same scale on x and y (E key), before the zoom items. */
    @Override
    protected void fillViewMenuItems(Menu menu) {
        addToggleMenuItem(menu, isEqualAspect(), DocometreMessages.ChartMenu_EqualAspect,
                DocometreMessages.ChartMenu_IndependentAspect, "E", this::setEqualAspect);
        super.fillViewMenuItems(menu);
    }

    // --- IRTSWTXYChart ---

    /** No effect: the curves only keep their last sample. */
    @Override
    public void setHistorySize(double historySize) {
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
