package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the displayed view of a 1D or 2D chart (see
 * {@link PlotRenderer#addViewListener}): called at the first frame, then at
 * each frame whose view differs from the previous one (zoom window, wheel,
 * pan, automatic fit to new data, resize with "same scale"...).
 *
 * The call takes place in the OpenGL rendering thread (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}). To
 * act on SWT widgets from the listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface ViewListener {

    /**
     * @param view      bounds {xMin, xMax, yMin, yMax} of the displayed view
     *                  (copy), in axis units
     * @param automatic true if the view is the initial view (automatic fit to
     *                  the data, or fixed scales), without zoom window, wheel
     *                  zoom or pan applied by the user or by the API
     */
    void viewChanged(double[] view, boolean automatic);
}
