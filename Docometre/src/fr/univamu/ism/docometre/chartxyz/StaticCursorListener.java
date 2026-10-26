package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the static cursor of a 1D chart (double click in cursor mode,
 * see {@link PlotRenderer#addStaticCursorListener}). It is called when the
 * static cursor is placed, moved or removed, and each time the differences
 * between the moving cursor and the static cursor change (mouse move, curve
 * change with Tab, new data or view change under a motionless mouse).
 *
 * The call takes place in the OpenGL rendering thread (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}): to act
 * on SWT widgets from the listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface StaticCursorListener {

    /**
     * @param position coordinates {x, y} of the static cursor, in axis units,
     *                 or null when there is no static cursor (removed, or
     *                 cursor mode disabled)
     * @param delta    differences {Δx, Δy} = moving cursor - static cursor, or
     *                 null when one of the two cursors designates no point
     */
    void staticCursorChanged(float[] position, float[] delta);
}
