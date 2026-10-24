package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the markers selected in a 1D chart (see
 * {@link PlotRenderer#addMarkerSelectedListener}): it is called on Ctrl +
 * click on a marker label, which makes this marker the highlighted marker
 * (see {@link PlotRenderer#setSelectedMarker}) and its label the current
 * label. Its parameters are those of {@link PlotRenderer#setSelectedMarker}.
 *
 * The call takes place in the OpenGL rendering thread (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}), outside
 * any lock. To act on SWT widgets from the listener, use Display.asyncExec.
 *
 * @param <C> type of the curves of the chart
 */
@FunctionalInterface
public interface MarkerSelectedListener<C extends CurveData<C>> {

    /**
     * @param curve curve of the marker
     * @param label label of its group
     * @param x     abscissa, in data units (in 1D, time since the first sample)
     * @param y     ordinate, in data units
     */
    void markerSelected(C curve, String label, double x, double y);
}
