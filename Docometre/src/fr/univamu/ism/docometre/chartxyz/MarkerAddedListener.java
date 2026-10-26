package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the markers added to a 1D chart (see
 * {@link PlotRenderer#addMarkerAddedListener}). It is called once for each
 * marker actually added (a marker that already exists is not added again):
 * {@link PlotRenderer#addMarker}, each new marker of
 * {@link PlotRenderer#addMarkersGroup}, Ctrl + double click in cursor mode.
 * Its parameters are those of {@link PlotRenderer#addMarker}: the call can
 * be replayed as is.
 *
 * The call takes place in the thread that makes the change, outside any
 * lock: the OpenGL rendering thread for mouse actions (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}), the
 * calling thread for the API methods. To act on SWT widgets from the
 * listener, use Display.asyncExec.
 *
 * @param <C> type of the curves of the chart
 */
@FunctionalInterface
public interface MarkerAddedListener<C extends CurveData<C>> {

    /**
     * @param curve curve of the marker
     * @param label label of its group
     * @param x     abscissa, in data units (in 1D, time since the first sample)
     * @param y     ordinate, in data units
     */
    void markerAdded(C curve, String label, double x, double y);
}
