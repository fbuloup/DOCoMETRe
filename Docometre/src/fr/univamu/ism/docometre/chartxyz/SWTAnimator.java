package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Control;

/**
 * Animator for SWT GLCanvas: calls repaint about fps times per second, IN
 * the SWT user interface thread (Display.timerExec).
 *
 * Why not FPSAnimator: with an SWT GLCanvas, each frame is drawn in the SWT
 * thread anyway (JOGL forwards the call there with syncExec). FPSAnimator,
 * which runs in its own thread, then waits for the SWT thread while drawing;
 * if the SWT thread in turn waits for the animator (stop, pause or disposal
 * of the GLCanvas), both block until the one-second JOGL safety timeout
 * ("finishLifecycleAction ... timeout reached"). Here there is no second
 * thread: stopping the animator or disposing the component (directly or
 * through its parent) is immediate.
 *
 * All methods must be called from the SWT thread.
 */
public final class SWTAnimator implements Runnable {

    private final Control control;
    private final Runnable repaint;
    private final long periodNanos;

    private boolean running;
    private long nextFrame; // deadline of the next frame (System.nanoTime)

    /**
     * @param control animated component: the animation stops by itself when it is disposed
     * @param fps     target frame rate, in frames per second
     * @param repaint drawing of one frame (for example glCanvas::display)
     */
    public SWTAnimator(Control control, int fps, Runnable repaint) {
        this.control = control;
        this.repaint = repaint;
        this.periodNanos = 1_000_000_000L / Math.max(1, fps);
        control.addDisposeListener(e -> stop());
    }

    /** Starts the animation (no effect if it is already started). */
    public void start() {
        if (running || control.isDisposed()) return;
        running = true;
        nextFrame = System.nanoTime();
        control.getDisplay().timerExec(0, this);
    }

    /** Stops the animation; never blocks. */
    public void stop() {
        running = false;
        if (!control.isDisposed()) control.getDisplay().timerExec(-1, this); // cancels the scheduled call
    }

    public boolean isRunning() { return running; }

    /** One frame, then scheduling of the next one. */
    @Override
    public void run() {
        if (!running || control.isDisposed()) {
            running = false;
            return;
        }
        repaint.run();

        // Next deadline; if we are late (drawing too long), we restart from now
        long now = System.nanoTime();
        nextFrame += periodNanos;
        if (nextFrame < now) nextFrame = now;
        // Delay of at least 1 ms; on Windows timers have a granularity of about 15.6 ms,
        // so small delays give about 64 frames/s
        int delayMillis = (int) Math.max(1, (nextFrame - now) / 1_000_000L);
        if (running && !control.isDisposed()) control.getDisplay().timerExec(delayMillis, this);
    }
}
