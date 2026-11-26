package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GLProfile;

import fr.univamu.ism.docometre.Activator;

/**
 * Loading of JOGL at the start of the application, so that the first chart of the
 * "SWT + OpenGL v2" library is shown without delay. The first call to JOGL
 * ({@link GLProfile#get(String)}, see {@link GLHost}) initializes it: loading of
 * its native libraries and of those of the graphics driver, search of the available
 * OpenGL profiles. This takes a few seconds, which are spent here at startup instead.
 * <p>
 * The initialization is done once ({@link GLProfile#initSingleton()} is idempotent and
 * synchronized): a chart created while it is in progress waits for its end.
 * <ul>
 * <li>Windows, Linux (SWT GLCanvas): in a background thread, the user interface
 * is not blocked.</li>
 * <li>macOS (NEWT): in the calling thread, which must be the user interface thread
 * (main thread). JOGL runs some of its macOS operations in the main thread and waits
 * for them: in a background thread, the initialization could wait for the user
 * interface thread while this one waits for the initialization (chart created
 * meanwhile).</li>
 * </ul>
 */
public final class GLPreloader {

    private static boolean started;

    private GLPreloader() {
    }

    /**
     * Starts the initialization of JOGL (only the first time): in a background thread
     * on Windows and Linux, in the calling thread (user interface thread) on macOS.
     */
    public static synchronized void start() {
        if (started) return;
        started = true;
        if (GLToolkit.platformDefault() == GLToolkit.NEWT) {
            initialize();
            return;
        }
        Thread thread = new Thread(GLPreloader::initialize, "OpenGL v2 charts library loading");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /*
     * Initializes JOGL and its GL2 profile, used by the charts. An error is only logged:
     * it will occur again, and be reported, when a chart is created.
     */
    private static void initialize() {
        long start = System.currentTimeMillis();
        try {
            GLProfile.initSingleton();
            GLProfile.get(GLProfile.GL2);
            Activator.logInfoMessage("OpenGL v2 charts library loaded in " + (System.currentTimeMillis() - start) + " ms", GLPreloader.class);
        } catch (Throwable throwable) {
            Activator.logErrorMessage("OpenGL v2 charts library could not be loaded at startup : " + throwable);
        }
    }
}
