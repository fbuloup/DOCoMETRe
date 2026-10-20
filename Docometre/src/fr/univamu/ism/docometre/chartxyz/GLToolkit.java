package fr.univamu.ism.docometre.chartxyz;

/**
 * Boîte à outils qui héberge la surface OpenGL d'un graphe dans une interface SWT.
 */
public enum GLToolkit {

    /**
     * GLCanvas SWT, animé dans le thread de l'interface (voir {@link SWTAnimator}) :
     * à utiliser sous Windows et Linux.
     */
    SWT,

    /**
     * Fenêtre NEWT intégrée par NewtCanvasSWT, animée par son propre thread :
     * à utiliser sous macOS (le GLCanvas SWT n'y fonctionne pas correctement).
     */
    NEWT;

    /** NEWT sous macOS, SWT ailleurs. */
    public static GLToolkit platformDefault() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return (os.contains("mac") || os.contains("darwin")) ? NEWT : SWT;
    }
}
