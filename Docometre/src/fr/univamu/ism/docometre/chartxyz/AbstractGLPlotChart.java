package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import fr.univamu.ism.docometre.DocometreMessages;

/**
 * Base of the plane charts (1D and 2D): adds the legend position, and the
 * control of the rectangle zoom and of the cursor (see {@link PlotRenderer}),
 * and the corresponding items of the context menu.
 */
public abstract class AbstractGLPlotChart<C extends CurveData<C>, R extends PlotRenderer<C>> extends AbstractGLChart<C, R> {

    protected AbstractGLPlotChart(Composite container, GLToolkit toolkit, R renderer) {
        super(container, toolkit, renderer, false);
    }

    /**
     * Legend position: LegendPosition.INSIDE (in the chart, at the top left)
     * or LegendPosition.BELOW (at the very bottom, below the abscissa axis, default).
     */
    public void setLegendPosition(LegendPosition position) {
        renderer.setLegendPosition(position);
    }

    public LegendPosition getLegendPosition() {
        return renderer.getLegendPosition();
    }

    /** SWT.BOTTOM: legend below the abscissa axis; any other value: in the chart. */
    public void setLegendPosition(int position) {
        renderer.setLegendPosition(position == SWT.BOTTOM ? LegendPosition.BELOW : LegendPosition.INSIDE);
    }

    // ------------------------------------------------------------------
    // Rectangle zoom

    /**
     * Applies a zoom window, like a rectangle drawn with the mouse (stored in
     * the history). In 1D scroll mode, the abscissas are relative to the last
     * sample (for example -2 to -1 for the second before last): the window
     * follows time.
     *
     * @throws IllegalArgumentException if a minimum is not strictly less than its maximum
     */
    public void setZoomWindow(double xMin, double xMax, double yMin, double yMax) {
        renderer.setZoomWindow(xMin, xMax, yMin, yMax);
    }

    /** Current zoom window {xMin, xMax, yMin, yMax} (copy), or null if the view is the initial view. */
    public double[] getZoomWindow() {
        return renderer.getZoomWindow();
    }

    /** True if a zoom window is applied. */
    public boolean isZoomed() {
        return renderer.isZoomed();
    }

    /** Goes back to the previous window (also with U or Backspace); false if the history is empty. */
    public boolean undoZoom() {
        return renderer.undoZoom();
    }

    /**
     * Moves the view by steps of a tenth of its width / height (also with the
     * arrow keys): stepsX > 0 to the right, stepsY > 0 upwards.
     */
    public void panStep(int stepsX, int stepsY) {
        renderer.panStep(stepsX, stepsY);
    }

    /** Goes back to the initial view and clears the zoom history (also with R). */
    public void resetZoom() {
        renderer.resetView();
    }

    /** Adds a listener of the zoom window changes (see {@link ZoomListener} for the calling thread). */
    public void addZoomListener(ZoomListener listener) {
        renderer.addZoomListener(listener);
    }

    public void removeZoomListener(ZoomListener listener) {
        renderer.removeZoomListener(listener);
    }

    /**
     * Adds a listener of the displayed view (see {@link ViewListener}): also
     * told about the wheel zoom and the pan without zoom window, and about
     * the automatic fit.
     */
    public void addViewListener(ViewListener listener) {
        renderer.addViewListener(listener);
    }

    public void removeViewListener(ViewListener listener) {
        renderer.removeViewListener(listener);
    }

    /** Bounds {xMin, xMax, yMin, yMax} of the view displayed by the last frame (copy), or null before the first frame. */
    public double[] getView() {
        return renderer.getView();
    }

    // ------------------------------------------------------------------
    // Cursor

    /** Enables or disables the cursor (also with the C key); when enabled, it follows the first curve. */
    public void setCursorEnabled(boolean enabled) {
        renderer.setCursorEnabled(enabled);
    }

    public boolean isCursorEnabled() {
        return renderer.isCursorEnabled();
    }

    /** Curve followed by the cursor, by its position (0 = first; also with Tab / Shift + Tab). */
    public void setCursorCurveIndex(int index) {
        renderer.setCursorCurveIndex(index);
    }

    public int getCursorCurveIndex() {
        return renderer.getCursorCurveIndex();
    }

    /** Coordinates {x, y} of the point designated by the cursor (copy), or null if there is none. */
    public float[] getCursorPosition() {
        return renderer.getCursorPosition();
    }

    /** Adds a cursor listener (see {@link CursorListener} for the calling thread). */
    public void addCursorListener(CursorListener listener) {
        renderer.addCursorListener(listener);
    }

    public void removeCursorListener(CursorListener listener) {
        renderer.removeCursorListener(listener);
    }

    /** Coordinates {x, y} of the static cursor (copy), or null if there is none (1D charts, double click in cursor mode). */
    public float[] getStaticCursorPosition() {
        return renderer.getStaticCursorPosition();
    }

    /** Differences {Δx, Δy} between the moving cursor and the static cursor, or null if one of them is missing. */
    public float[] getCursorDelta() {
        return renderer.getCursorDelta();
    }

    /** Removes the static cursor (the moving cursor remains). */
    public void clearStaticCursor() {
        renderer.clearStaticCursor();
    }

    /** Adds a static cursor listener (see {@link StaticCursorListener} for the calling thread). */
    public void addStaticCursorListener(StaticCursorListener listener) {
        renderer.addStaticCursorListener(listener);
    }

    public void removeStaticCursorListener(StaticCursorListener listener) {
        renderer.removeStaticCursorListener(listener);
    }

    // ------------------------------------------------------------------
    // Markers (1D and 2D charts)

    /** Shows (default) or hides all the markers (they are kept). */
    public void setMarkersVisible(boolean visible) {
        renderer.setMarkersVisible(visible);
    }

    public boolean isMarkersVisible() {
        return renderer.isMarkersVisible();
    }

    /** Shows (default) or hides the marker labels (the symbols remain). */
    public void setMarkerLabelsVisible(boolean visible) {
        renderer.setMarkerLabelsVisible(visible);
    }

    public boolean isMarkerLabelsVisible() {
        return renderer.isMarkerLabelsVisible();
    }

    /** Size of the marker symbols in pixels: half-diagonal of the diamond in 1D, radius of the circle in 2D (5 by default). */
    public void setMarkerSize(int size) {
        renderer.setMarkerSize(size);
    }

    public int getMarkerSize() {
        return renderer.getMarkerSize();
    }

    /** Label of the markers added with Ctrl + double click in cursor mode ("Default" by default). */
    public void setCurrentMarkerLabel(String label) {
        renderer.setCurrentMarkerLabel(label);
    }

    /**
     * Enables (default) or disables the mouse actions on the markers (add,
     * remove, select): when disabled, the markers are only displayed.
     */
    public void setMarkersInteractive(boolean interactive) {
        renderer.setMarkersInteractive(interactive);
    }

    public boolean isMarkersInteractive() {
        return renderer.isMarkersInteractive();
    }

    public String getCurrentMarkerLabel() {
        return renderer.getCurrentMarkerLabel();
    }

    /** Adds a marker to the group (curve, label), in the color of the curve; false if the group already has it. */
    public boolean addMarker(C curve, String label, double x, double y) {
        return renderer.addMarker(curve, label, x, y);
    }

    /** Adds markers {x, y} to the group (curve, label) (markers already present are skipped). */
    public void addMarkersGroup(C curve, String label, double[][] coordinates) {
        renderer.addMarkersGroup(curve, label, coordinates);
    }

    /** Removes a marker of the group (curve, label); the group disappears with its last marker. */
    public boolean removeMarker(C curve, String label, double x, double y) {
        return renderer.removeMarker(curve, label, x, y);
    }

    /** Removes the group (curve, label). */
    public boolean removeMarkersGroup(C curve, String label) {
        return renderer.removeMarkersGroup(curve, label);
    }

    /** Removes all the markers, of all the curves. */
    public void clearMarkers() {
        renderer.clearMarkers();
    }

    /** Removes all the markers of a curve. */
    public void clearMarkers(C curve) {
        renderer.clearMarkers(curve);
    }

    /** Curves that have markers, in the order of the chart. */
    public java.util.List<C> getMarkersCurves() {
        return renderer.getMarkersCurves();
    }

    /** Labels of the marker groups of a curve, in creation order. */
    public java.util.List<String> getMarkersLabels(C curve) {
        return renderer.getMarkersLabels(curve);
    }

    /** Markers {x, y} of the group (curve, label) (copy), or null if there is no such group. */
    public double[][] getMarkersGroup(C curve, String label) {
        return renderer.getMarkersGroup(curve, label);
    }

    /**
     * Highlights a marker of the group (curve, label), designated by its
     * coordinates (data units), for example the marker selected in a list.
     */
    public void setSelectedMarker(C curve, String label, double x, double y) {
        renderer.setSelectedMarker(curve, label, x, y);
    }

    /** No more highlighted marker. */
    public void clearSelectedMarker() {
        renderer.clearSelectedMarker();
    }

    /** Listener of the markers selected with Ctrl + click on their label (see {@link MarkerSelectedListener}). */
    public void addMarkerSelectedListener(MarkerSelectedListener<C> listener) {
        renderer.addMarkerSelectedListener(listener);
    }

    public void removeMarkerSelectedListener(MarkerSelectedListener<C> listener) {
        renderer.removeMarkerSelectedListener(listener);
    }

    /** Listener of the markers added (see {@link MarkerAddedListener} for the calling thread). */
    public void addMarkerAddedListener(MarkerAddedListener<C> listener) {
        renderer.addMarkerAddedListener(listener);
    }

    public void removeMarkerAddedListener(MarkerAddedListener<C> listener) {
        renderer.removeMarkerAddedListener(listener);
    }

    /** Listener of the markers removed (see {@link MarkerRemovedListener} for the calling thread). */
    public void addMarkerRemovedListener(MarkerRemovedListener<C> listener) {
        renderer.addMarkerRemovedListener(listener);
    }

    public void removeMarkerRemovedListener(MarkerRemovedListener<C> listener) {
        renderer.removeMarkerRemovedListener(listener);
    }

    /** Listener of the current marker label (see {@link CurrentMarkerLabelListener} for the calling thread). */
    public void addCurrentMarkerLabelListener(CurrentMarkerLabelListener listener) {
        renderer.addCurrentMarkerLabelListener(listener);
    }

    public void removeCurrentMarkerLabelListener(CurrentMarkerLabelListener listener) {
        renderer.removeCurrentMarkerLabelListener(listener);
    }

    // ------------------------------------------------------------------
    // Context menu

    /** Cursor (C key) and removal of the static cursor if there is one. */
    @Override
    protected void fillModeMenuItems(Menu menu) {
        super.fillModeMenuItems(menu);
        addToggleMenuItem(menu, isCursorEnabled(), DocometreMessages.ChartMenu_ShowCursor,
                DocometreMessages.ChartMenu_HideCursor, "C", this::setCursorEnabled);
        if (getStaticCursorPosition() != null) {
            addMenuItem(menu, DocometreMessages.ChartMenu_ClearStaticCursor, null, this::clearStaticCursor);
        }
    }

    /** Adds the legend position, after the legend. */
    @Override
    protected void fillCurvesMenuItems(Menu menu) {
        super.fillCurvesMenuItems(menu);
        boolean below = getLegendPosition() == LegendPosition.BELOW;
        MenuItem item = addMenuItem(menu, below ? DocometreMessages.ChartMenu_LegendInside : DocometreMessages.ChartMenu_LegendBelow,
                null, () -> setLegendPosition(below ? LegendPosition.INSIDE : LegendPosition.BELOW));
        item.setEnabled(isLegendVisible());
    }

    /** Markers and their labels. */
    @Override
    protected void fillMarkersMenuItems(Menu menu) {
        super.fillMarkersMenuItems(menu);
        addToggleMenuItem(menu, isMarkersVisible(), DocometreMessages.ChartMenu_ShowMarkers,
                DocometreMessages.ChartMenu_HideMarkers, null, this::setMarkersVisible);
        MenuItem labels = addToggleMenuItem(menu, isMarkerLabelsVisible(), DocometreMessages.ChartMenu_ShowMarkerLabels,
                DocometreMessages.ChartMenu_HideMarkerLabels, null, this::setMarkerLabelsVisible);
        labels.setEnabled(isMarkersVisible());
    }

    /** Previous zoom window (U key), then initial view. */
    @Override
    protected void fillViewMenuItems(Menu menu) {
        MenuItem undo = addMenuItem(menu, DocometreMessages.ChartMenu_UndoZoom, "U", this::undoZoom);
        undo.setEnabled(isZoomed());
        super.fillViewMenuItems(menu);
    }
}
