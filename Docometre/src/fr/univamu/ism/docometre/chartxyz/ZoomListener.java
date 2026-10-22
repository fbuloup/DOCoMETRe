package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the zoom window changes of a 1D or 2D chart (see
 * {@link PlotRenderer#addZoomListener}).
 *
 * The call takes place in the thread that caused the change: the OpenGL
 * rendering thread for mouse and keyboard commands (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}), the
 * calling thread for the API methods. To act on SWT widgets from the
 * listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface ZoomListener {

    /**
     * @param window new window {xMin, xMax, yMin, yMax} (copy), or null when
     *               going back to the initial view. In 1D scroll mode, the
     *               abscissas are relative to the last sample (negative):
     *               the window follows time.
     */
    void zoomChanged(double[] window);
}
