package fr.univamu.ism.docometre.chartxyz;

import java.util.function.Consumer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

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
     * Largest mouse displacement, in window units, between the press and the
     * release of the right button for a click: beyond it, it is a drag (pan)
     * and the context menu is not requested.
     */
    protected static final int CLICK_TOLERANCE = 3;

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
    private volatile Consumer<Point> contextMenuHandler;
    
    /*
     * Listeners of the keys typed in a chart while the NEWT window has the keyboard
     * focus (macOS) : the NEWT window covers the SWT canvas and takes the keyboard
     * focus when the chart is clicked, so the key events are received by NEWT, not
     * by SWT, and the display filters of the application do not see them. The NEWT
     * host forwards these keys to the listeners below, in the SWT thread, as SWT
     * KeyUp events (see NewtGLHost.forwardKeyUpToSWT).
     */
    private static final List<Listener> applicationKeyListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a listener notified, in the SWT thread, of the keys typed in a chart while
     * the NEWT window has the keyboard focus. The listener receives SWT KeyUp events
     * (keyCode, character, stateMask), like a display filter on SWT.KeyUp.
     */
    public static void addApplicationKeyListener(Listener listener) {
        if (listener != null) applicationKeyListeners.add(listener);
    }

    /** Removes a listener added by {@link #addApplicationKeyListener(Listener)}. */
    public static void removeApplicationKeyListener(Listener listener) {
        applicationKeyListeners.remove(listener);
    }

    /** Notifies the application key listeners of a key typed in a NEWT chart, in the SWT thread. */
    static void notifyApplicationKeyListeners(Event event) {
        if (!applicationKeyListeners.isEmpty() && event.display != null && !event.display.isDisposed()) {
            event.display.asyncExec(() -> {
                for (Listener listener : applicationKeyListeners) listener.handleEvent(event);
            });
        }
    }
    
    /**
     * True if a modifier key of the application shortcuts (Ctrl, Command, Alt) is
     * pressed: the key is then a shortcut of the application (Cmd+Shift+F for the
     * full screen mode...), it is not passed to the renderer (F would toggle the
     * frame rate display). Shift alone is not one of them (Shift + Tab).
     */
    protected static boolean isShortcutModifier(int swtStateMask) {
        return (swtStateMask & (SWT.CTRL | SWT.COMMAND | SWT.ALT | SWT.SHIFT)) != 0;
    }

    /** GL2 profile, double buffering, 4x antialiasing and, if requested, depth buffer. */
    private static GLCapabilities capabilities(boolean depthBuffer) {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setDoubleBuffered(true);
        if (depthBuffer) capabilities.setDepthBits(24);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);
        return capabilities;
    }

    /**
     * Action run, in the SWT thread, when a context menu is requested: right
     * click without drag, or menu key of the keyboard. It receives the
     * location of the menu, in display coordinates (null = none).
     */
    public void setContextMenuHandler(Consumer<Point> handler) {
        contextMenuHandler = handler;
    }

    /** True if a context menu handler is set. */
    protected final boolean hasContextMenuHandler() {
        return contextMenuHandler != null;
    }

    /** Requests the context menu at (x, y), in the coordinates of the SWT component; to be called in the SWT thread. */
    protected final void requestContextMenu(int x, int y) {
        Consumer<Point> handler = contextMenuHandler;
        Control control = getControl();
        if (handler == null || closed || control == null || control.isDisposed()) return;
        handler.accept(control.toDisplay(x, y));
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
