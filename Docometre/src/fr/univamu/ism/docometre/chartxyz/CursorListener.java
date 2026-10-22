package fr.univamu.ism.docometre.chartxyz;

/**
 * Cursor listener of a 1D or 2D chart (C key, see
 * {@link PlotRenderer#addCursorListener}). It is called each time the point
 * designated by the cursor changes: mouse move, curve change (Tab), but also
 * arrival of new data or view change under a motionless mouse.
 *
 * The call takes place in the OpenGL rendering thread (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}): to act
 * on SWT widgets from the listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface CursorListener {

    /**
     * @param xy coordinates {x, y} of the designated point, in axis units
     *           (in 1D: x = time displayed on the axis, position in the sweep
     *           in sweep mode), or null when the cursor is disabled or no
     *           longer designates any point (empty curve or outside the view)
     */
    void cursorMoved(float[] xy);
}
