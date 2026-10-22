package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the markers removed from a 1D chart (see
 * {@link PlotRenderer#addMarkerRemovedListener}). It is called once for each
 * marker removed: {@link PlotRenderer#removeMarker}, double click on a
 * marker label, and each marker of the groups removed by
 * {@link PlotRenderer#removeMarkersGroup}, {@link PlotRenderer#clearMarkers()},
 * {@link PlotRenderer#clearMarkers(CurveData)} or the removal of its curve
 * from the chart ({@link CurveChartRenderer#removeCurve}). Its parameters are
 * those of {@link PlotRenderer#removeMarker}.
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
public interface MarkerRemovedListener<C extends CurveData<C>> {

    /**
     * @param curve curve of the marker (possibly already removed from the chart)
     * @param label label of its group
     * @param x     abscissa, in data units (in 1D, time since the first sample)
     * @param y     ordinate, in data units
     */
    void markerRemoved(C curve, String label, double x, double y);
}
