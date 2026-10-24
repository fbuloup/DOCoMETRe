package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Base of the plane charts (1D and 2D): adds the legend position, and the
 * control of the rectangle zoom and of the cursor (see {@link PlotRenderer}).
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
    // Markers (1D charts)

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
}
