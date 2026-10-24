package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the display options of a chart (see {@link DisplayOption} and
 * {@link GLChartRenderer#addDisplayOptionListener}): it is called when an
 * option actually changes, with the keyboard or the API, for example to keep
 * a check box of the application in step with the P key.
 *
 * The call takes place in the thread that makes the change, outside any
 * lock: the OpenGL rendering thread for the keyboard (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}), the
 * calling thread for the API methods. To act on SWT widgets from the
 * listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface DisplayOptionListener {

    /**
     * @param option  the option that changed
     * @param enabled its new state (true: shown / enabled)
     */
    void displayOptionChanged(DisplayOption option, boolean enabled);
}
