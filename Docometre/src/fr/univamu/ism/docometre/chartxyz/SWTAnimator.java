package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Control;

/**
 * Animateur pour les GLCanvas SWT : appelle repaint environ fps fois par
 * seconde, DANS le thread de l'interface SWT (Display.timerExec).
 *
 * Pourquoi pas FPSAnimator : avec un GLCanvas SWT, chaque image est de toute
 * façon dessinée dans le thread SWT (JOGL y renvoie l'appel par syncExec).
 * FPSAnimator, qui tourne dans son propre thread, attend alors le thread SWT
 * pendant le dessin ; si le thread SWT attend à son tour l'animateur (arrêt,
 * pause ou destruction du GLCanvas), les deux se bloquent jusqu'au délai de
 * sécurité d'une seconde de JOGL ("finishLifecycleAction ... timeout
 * reached"). Ici il n'y a pas de second thread : arrêter l'animateur ou
 * détruire le composant (directement ou via son parent) est immédiat.
 *
 * Toutes les méthodes doivent être appelées depuis le thread SWT.
 */
public final class SWTAnimator implements Runnable {

    private final Control control;
    private final Runnable repaint;
    private final long periodNanos;

    private boolean running;
    private long nextFrame; // échéance de la prochaine image (System.nanoTime)

    /**
     * @param control composant animé : l'animation s'arrête d'elle-même quand il est détruit
     * @param fps     cadence visée, en images par seconde
     * @param repaint dessin d'une image (par exemple glCanvas::display)
     */
    public SWTAnimator(Control control, int fps, Runnable repaint) {
        this.control = control;
        this.repaint = repaint;
        this.periodNanos = 1_000_000_000L / Math.max(1, fps);
        control.addDisposeListener(e -> stop());
    }

    /** Démarre l'animation (sans effet si elle est déjà démarrée). */
    public void start() {
        if (running || control.isDisposed()) return;
        running = true;
        nextFrame = System.nanoTime();
        control.getDisplay().timerExec(0, this);
    }

    /** Arrête l'animation ; ne bloque jamais. */
    public void stop() {
        running = false;
        if (!control.isDisposed()) control.getDisplay().timerExec(-1, this); // annule l'appel programmé
    }

    public boolean isRunning() { return running; }

    /** Une image, puis programmation de la suivante. */
    @Override
    public void run() {
        if (!running || control.isDisposed()) {
            running = false;
            return;
        }
        repaint.run();

        // Échéance suivante ; si l'on a pris du retard (dessin trop long), on repart de maintenant
        long now = System.nanoTime();
        nextFrame += periodNanos;
        if (nextFrame < now) nextFrame = now;
        // Délai d'au moins 1 ms ; sous Windows les minuteries ont une granularité d'environ 15,6 ms,
        // les petits délais donnent donc environ 64 images/s
        int delayMillis = (int) Math.max(1, (nextFrame - now) / 1_000_000L);
        if (running && !control.isDisposed()) control.getDisplay().timerExec(delayMillis, this);
    }
}
