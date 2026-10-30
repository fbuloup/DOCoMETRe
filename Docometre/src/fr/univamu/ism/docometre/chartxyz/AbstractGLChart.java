package fr.univamu.ism.docometre.chartxyz;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Base of the real-time curve charts embedded in an SWT user interface: an
 * OpenGL surface ({@link GLHost}, SWT or NEWT depending on {@link GLToolkit})
 * and a renderer ({@link CurveChartRenderer}) whose settings are exposed here.
 *
 * The methods of the fr.univamu.ism.rtswtchart interfaces common to all
 * charts are also implemented here (subclasses declare the interface).
 *
 * A right click without drag on the chart (or the menu key) opens a context
 * menu, rebuilt at each opening from the current state of the chart, whose
 * items depend on the type of chart (see {@link #fillContextMenu}).
 *
 * @param <C> curve type
 * @param <R> renderer type
 */
public abstract class AbstractGLChart<C extends CurveData<C>, R extends CurveChartRenderer<C>> implements GLComponent {

    protected final R renderer;
    protected final GLHost host;

    private Menu contextMenu; // created at the first opening, SWT thread only
    private volatile boolean contextMenuEnabled = true;
    private volatile boolean markersMenuItemsVisible = true;

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
        host.setContextMenuHandler(this::showContextMenu);
    }

    /** Renderer of the chart, for the settings that are not exposed here. */
    public R getRenderer() { return renderer; }

    /** SWT control holding the OpenGL surface (drag and drop target, menus, focus...). */
    public Control getControl() { return host.getControl(); }

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

    /**
     * Removes all the curves from the chart, as {@link #removeCurve(CurveData)}
     * one by one: they can be added to another chart, their names are
     * available again, their markers are removed (and notified).
     */
    public void removeAllCurves() {
        renderer.removeAllCurves();
    }

    /** Curve of the chart with this name, or null (usable from any thread). */
    public C getCurve(String name) {
        return renderer.getCurve(name);
    }

    /** Curves of the chart, in the order they were added (copy, usable from any thread). */
    public java.util.List<C> getCurves() {
        return renderer.getCurves();
    }

    /** Names of the curves of the chart, in the order they were added (copy, usable from any thread). */
    public java.util.List<String> getCurvesNames() {
        return renderer.getCurvesNames();
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

    /** True if the frame rate is shown in the chart. */
    public boolean isFpsVisible() {
        return renderer.isFpsVisible();
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

    /**
     * Shows or hides the points of the curves (also with the P key). While
     * they are shown, the reduction of the number of points drawn is
     * disabled, so that every sample appears.
     */
    public void setPointsVisible(boolean visible) {
        renderer.setPointsVisible(visible);
    }

    public boolean isPointsVisible() {
        return renderer.isPointsVisible();
    }

    /** Toggles the display of the points (as the P key). */
    public void togglePoints() {
        renderer.togglePoints();
    }

    /**
     * Adds a listener told about each change of a display option (points,
     * legend, grid... see {@link DisplayOption}), with the keyboard or the
     * API; see {@link DisplayOptionListener} for the calling thread.
     */
    public void addDisplayOptionListener(DisplayOptionListener listener) {
        renderer.addDisplayOptionListener(listener);
    }

    public void removeDisplayOptionListener(DisplayOptionListener listener) {
        renderer.removeDisplayOptionListener(listener);
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

    /**
     * Enables (default) or disables the fading of the 2D and 3D curves
     * towards their oldest samples; disabled, the curves have a uniform
     * color. No effect on 1D charts.
     */
    public void setTrailFading(boolean fading) {
        renderer.setTrailFading(fading);
    }

    public boolean isTrailFading() {
        return renderer.isTrailFading();
    }

    // ------------------------------------------------------------------
    // Context menu

    /**
     * Enables (default) or disables the context menu of the chart (right
     * click without drag, menu key). Disabled, a menu given by the
     * application to the SWT control ({@link #getControl()}) is shown instead.
     */
    public void setContextMenuEnabled(boolean enabled) {
        contextMenuEnabled = enabled;
        host.setContextMenuHandler(enabled ? this::showContextMenu : null);
    }

    public boolean isContextMenuEnabled() {
        return contextMenuEnabled;
    }

    /**
     * Shows (default) or hides the marker items of the context menu, for the
     * charts that never have markers (real-time display, for example).
     */
    public void setMarkersMenuItemsVisible(boolean visible) {
        markersMenuItemsVisible = visible;
    }

    public boolean isMarkersMenuItemsVisible() {
        return markersMenuItemsVisible;
    }

    /**
     * Builds the context menu from the current state of the chart and shows
     * it at location (display coordinates). To be called in the SWT thread.
     */
    public void showContextMenu(Point location) {
        Control control = getControl();
        if (!contextMenuEnabled || control == null || control.isDisposed()) return;
        if (contextMenu == null || contextMenu.isDisposed()) {
            Menu menu = new Menu(control); // owned by the shell: disposed with the control
            control.addListener(SWT.Dispose, e -> disposeContextMenu(menu));
            contextMenu = menu;
        }
        if (contextMenu.isVisible()) return;
        for (MenuItem item : contextMenu.getItems()) item.dispose();
        fillContextMenu(contextMenu);
        if (contextMenu.getItemCount() == 0) return;
        contextMenu.setLocation(location);
        contextMenu.setVisible(true);
    }

    /**
     * Disposes the context menu of a disposed chart. The chart can be disposed
     * while its menu is open (real-time charts recreated at each trial): on
     * Windows and macOS, the system then displays the menu in its own modal
     * loop, in which the disposal takes place. The menu is closed first, and
     * disposed once this loop is over.
     */
    private static void disposeContextMenu(Menu menu) {
        if (menu.isDisposed()) return;
        Display display = menu.getDisplay();
        menu.setVisible(false);
        if (display.isDisposed()) return;
        display.asyncExec(() -> {
            if (!menu.isDisposed()) menu.dispose();
        });
    }

    /**
     * Fills the context menu, section by section (separated by a line):
     * mode, curves, markers (unless hidden, see
     * {@link #setMarkersMenuItemsVisible}), decorations, rendering and view. Subclasses
     * complete the sections that concern them; the items show the change
     * they make ("Show the legend" / "Hide the legend"...) and, when there
     * is one, the equivalent key.
     */
    protected void fillContextMenu(Menu menu) {
        fillModeMenuItems(menu);
        addMenuSeparator(menu);
        fillCurvesMenuItems(menu);
        addMenuSeparator(menu);
        if (markersMenuItemsVisible) fillMarkersMenuItems(menu);
        addMenuSeparator(menu);
        fillDecorationsMenuItems(menu);
        addMenuSeparator(menu);
        fillRenderingMenuItems(menu);
        addMenuSeparator(menu);
        fillViewMenuItems(menu);
    }

    /** Interaction modes (cursor, pause...): none by default. */
    protected void fillModeMenuItems(Menu menu) {
    }

    /** Legend, last values and points of the curves. */
    protected void fillCurvesMenuItems(Menu menu) {
        addToggleMenuItem(menu, isLegendVisible(), DocometreMessages.ChartMenu_ShowLegend,
                DocometreMessages.ChartMenu_HideLegend, "N", this::setLegendVisible);
        addToggleMenuItem(menu, isCurrentValuesVisible(), DocometreMessages.ChartMenu_ShowCurrentValues,
                DocometreMessages.ChartMenu_HideCurrentValues, "V", this::setCurrentValuesVisible);
        addToggleMenuItem(menu, isPointsVisible(), DocometreMessages.ChartMenu_ShowPoints,
                DocometreMessages.ChartMenu_HidePoints, "P", this::setPointsVisible);
    }

    /** Markers: none by default. */
    protected void fillMarkersMenuItems(Menu menu) {
    }

    /** Grid, axes and tick labels. */
    protected void fillDecorationsMenuItems(Menu menu) {
        addToggleMenuItem(menu, renderer.isGridVisible(), DocometreMessages.ChartMenu_ShowGrid,
                DocometreMessages.ChartMenu_HideGrid, "G", renderer::setGridVisible);
        addToggleMenuItem(menu, renderer.isAxesVisible(), DocometreMessages.ChartMenu_ShowAxes,
                DocometreMessages.ChartMenu_HideAxes, "A", renderer::setAxesVisible);
        addToggleMenuItem(menu, renderer.isLabelsVisible(), DocometreMessages.ChartMenu_ShowTickLabels,
                DocometreMessages.ChartMenu_HideTickLabels, "L", renderer::setLabelsVisible);
    }

    /** Frame rate and reduction of the number of points drawn. */
    protected void fillRenderingMenuItems(Menu menu) {
        addToggleMenuItem(menu, isFpsVisible(), DocometreMessages.ChartMenu_ShowFps,
                DocometreMessages.ChartMenu_HideFps, "F", this::setFpsVisible);
        addToggleMenuItem(menu, isDecimationEnabled(), DocometreMessages.ChartMenu_EnableDecimation,
                DocometreMessages.ChartMenu_DisableDecimation, "D", this::setDecimationEnabled);
    }

    /** Initial view. */
    protected void fillViewMenuItems(Menu menu) {
        addMenuItem(menu, DocometreMessages.ChartMenu_ResetView, "R", renderer::resetView);
    }

    /**
     * Adds to menu an item that runs action.
     *
     * @param key equivalent key, shown next to the text (null = none)
     */
    protected static MenuItem addMenuItem(Menu menu, String text, String key, Runnable action) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(key == null ? text : text + '\t' + key);
        item.addListener(SWT.Selection, e -> action.run());
        return item;
    }

    /**
     * Adds to menu an item that inverts a setting: its text is the one of the
     * change it makes, textWhenOff if the setting is off (show, enable...),
     * textWhenOn otherwise (hide, disable...).
     *
     * @param state  current state of the setting
     * @param key    equivalent key, shown next to the text (null = none)
     * @param setter receives the new state
     */
    protected static MenuItem addToggleMenuItem(Menu menu, boolean state, String textWhenOff, String textWhenOn,
                                                String key, Consumer<Boolean> setter) {
        return addMenuItem(menu, state ? textWhenOn : textWhenOff, key, () -> setter.accept(!state));
    }

    /** Adds a separator to menu, unless it is empty or already ends with one. */
    protected static void addMenuSeparator(Menu menu) {
        int count = menu.getItemCount();
        if (count > 0 && (menu.getItem(count - 1).getStyle() & SWT.SEPARATOR) == 0) new MenuItem(menu, SWT.SEPARATOR);
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
