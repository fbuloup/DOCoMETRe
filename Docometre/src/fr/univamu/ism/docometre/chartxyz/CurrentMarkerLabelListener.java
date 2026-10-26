package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the current marker label of a 1D chart (see
 * {@link PlotRenderer#addCurrentMarkerLabelListener}). It is called when the
 * label actually changes: {@link PlotRenderer#setCurrentMarkerLabel}, or
 * Ctrl + click on a marker label in the chart.
 *
 * The call takes place in the thread that makes the change, outside any
 * lock: the OpenGL rendering thread for mouse actions (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}), the
 * calling thread for the API methods. To act on SWT widgets from the
 * listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface CurrentMarkerLabelListener {

    /** @param label the new current label */
    void currentMarkerLabelChanged(String label);
}
