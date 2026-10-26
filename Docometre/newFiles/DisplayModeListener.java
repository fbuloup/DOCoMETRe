package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the display mode of a 1D chart, sweep or scroll (M key or
 * {@link Chart1DRenderer#setDisplayMode}): it is called when the mode
 * actually changes.
 *
 * The call takes place in the thread that makes the change, outside any
 * lock: the OpenGL rendering thread for the keyboard, the calling thread for
 * the API methods. To act on SWT widgets from the listener, use
 * Display.asyncExec.
 */
@FunctionalInterface
public interface DisplayModeListener {

    /** @param mode the new display mode */
    void displayModeChanged(Chart1DRenderer.DisplayMode mode);
}
