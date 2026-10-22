package fr.univamu.ism.docometre.chartxyz;

/**
 * Toolkit hosting the OpenGL surface of a chart in an SWT user interface.
 */
public enum GLToolkit {

    /**
     * SWT GLCanvas, animated in the user interface thread (see {@link SWTAnimator}):
     * to be used on Windows and Linux.
     */
    SWT,

    /**
     * NEWT window embedded by NewtCanvasSWT, animated by its own thread: to be
     * used on macOS (the SWT GLCanvas does not work properly there).
     */
    NEWT;

    /** NEWT on macOS, SWT elsewhere. */
    public static GLToolkit platformDefault() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return (os.contains("mac") || os.contains("darwin")) ? NEWT : SWT;
    }
}
