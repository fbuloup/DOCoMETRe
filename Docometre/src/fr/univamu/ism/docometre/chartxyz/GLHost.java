package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

/**
 * OpenGL surface of a chart embedded in an SWT user interface: creation of
 * the component, animation (about {@link #FPS} frames per second),
 * forwarding of the mouse and keyboard events to the renderer
 * ({@link ChartInteraction}), closing. Two implementations, depending on the
 * toolkit ({@link GLToolkit}): {@link SWTGLHost} and {@link NewtGLHost}.
 *
 * The surface is closed (animation stopped, resources released) by
 * {@link #close()} or when its SWT component is disposed (directly or
 * through its parent).
 */
public abstract class GLHost {

    /** Target frame rate, in frames per second. */
    public static final int FPS = 60;

    /**
     * Creates the OpenGL surface in parent and starts the animation.
     *
     * @param depthBuffer true for a 3D scene (24-bit depth buffer)
     * @param renderer    rendering and mouse / keyboard commands
     * @param onClose     action run once, on closing (null = none)
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

    /** GL2 profile, double buffering, 4x antialiasing and, if requested, depth buffer. */
    private static GLCapabilities capabilities(boolean depthBuffer) {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setDoubleBuffered(true);
        if (depthBuffer) capabilities.setDepthBits(24);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);
        return capabilities;
    }

    /** SWT component containing the OpenGL surface (for layout). */
    public abstract Control getControl();

    /** Stops the animation: the display is no longer refreshed. */
    public abstract void stopAnimation();
    
    /** Starts the animation: the display is refreshed. */
    public abstract void startAnimation();

    /** Stops the animation and destroys the OpenGL surface and its SWT component (only once). */
    public final void close() {
        if (closed) return;
        closed = true;
        destroy();
        if (onClose != null) onClose.run();
    }

    /** Stops the animation and destroys the OpenGL surface and its SWT component. */
    protected abstract void destroy();

    public boolean isDisposed() {
        Control control = getControl();
        return control == null || control.isDisposed();
    }
}
