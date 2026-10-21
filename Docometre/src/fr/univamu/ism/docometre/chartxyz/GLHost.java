package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

/**
 * Surface OpenGL d'un graphe intégrée dans une interface SWT : création du
 * composant, animation (environ {@link #FPS} images par seconde), transmission
 * des événements souris et clavier au renderer ({@link ChartInteraction}),
 * fermeture. Deux implémentations, selon la boîte à outils ({@link GLToolkit}) :
 * {@link SWTGLHost} et {@link NewtGLHost}.
 *
 * La surface est fermée (animation arrêtée, ressources libérées) par
 * {@link #close()} ou quand son composant SWT est détruit (directement ou
 * via son parent).
 */
public abstract class GLHost {

    /** Cadence d'affichage visée, en images par seconde. */
    public static final int FPS = 60;

    /**
     * Crée la surface OpenGL dans parent et démarre l'animation.
     *
     * @param depthBuffer true pour une scène 3D (tampon de profondeur de 24 bits)
     * @param renderer    rendu et commandes souris / clavier
     * @param onClose     action exécutée une fois, à la fermeture (null = aucune)
     */
    public static GLHost create(GLToolkit toolkit, Composite parent, boolean depthBuffer, GLChartRenderer renderer,
                                Runnable onClose) {
        GLCapabilities capabilities = capabilities(depthBuffer);
        GLHost host = (toolkit == GLToolkit.NEWT)
                ? new NewtGLHost(parent, capabilities, renderer)
                : new SWTGLHost(parent, capabilities, renderer);
        host.onClose = onClose;
        return host;
    }

    private Runnable onClose;
    private boolean closed;

    /** Profil GL2, double tampon, anticrénelage 4x et, si demandé, tampon de profondeur. */
    private static GLCapabilities capabilities(boolean depthBuffer) {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setDoubleBuffered(true);
        if (depthBuffer) capabilities.setDepthBits(24);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);
        return capabilities;
    }

    /** Composant SWT qui contient la surface OpenGL (pour la mise en page). */
    public abstract Control getControl();

    /** Arrête l'animation : l'affichage n'est plus rafraîchi. */
    public abstract void stopAnimation();
    
    /** Démarre l'animation : l'affichage est rafraîchi. */
    public abstract void startAnimation();

    /** Arrête l'animation et détruit la surface OpenGL et son composant SWT (une seule fois). */
    public final void close() {
        if (closed) return;
        closed = true;
        destroy();
        if (onClose != null) onClose.run();
    }

    /** Arrête l'animation et détruit la surface OpenGL et son composant SWT. */
    protected abstract void destroy();

    public boolean isDisposed() {
        Control control = getControl();
        return control == null || control.isDisposed();
    }
}
