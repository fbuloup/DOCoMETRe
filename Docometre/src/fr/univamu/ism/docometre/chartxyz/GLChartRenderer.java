package fr.univamu.ism.docometre.chartxyz;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.swt.GLCanvas;

import fr.univamu.ism.docometre.Activator;

/**
 * Base of all renderers: common initial OpenGL state (dark background,
 * smoothed lines, transparency), size of the drawing area, decorations
 * that can be toggled with the keyboard (axes, grid, tick labels) and
 * common keyboard commands:
 * <ul>
 * <li>R: reset the view</li>
 * <li>A: show/hide the axes</li>
 * <li>G: show/hide the grid</li>
 * <li>L: show/hide the tick labels</li>
 * </ul>
 * The {@link DisplayOptionListener}s are told about each change of a display
 * option (see {@link DisplayOption}), made with the keyboard or the API.
 *
 * Swap interval (see {@link #setSwapInterval}): 0 by default, that is
 * without waiting for the vertical retrace. With the SWT host, frames are
 * drawn in the user interface thread: a vertical synchronization would
 * block it at each frame, and for each chart displayed.
 */
public abstract class GLChartRenderer implements GLEventListener, ChartInteraction {

	private static final AtomicBoolean GL_INFO_LOGGED = new AtomicBoolean();
	private static volatile String glInfo = "OpenGL not initialised";
	private static final System.Logger LOGGER = System.getLogger(GLChartRenderer.class.getName());

	/** Swap interval of the charts created without {@link #setSwapInterval}: no vertical synchronization. */
	public static final int DEFAULT_SWAP_INTERVAL = 0;
	private static volatile int defaultSwapInterval = DEFAULT_SWAP_INTERVAL;

	private volatile int swapInterval = defaultSwapInterval; // requested
	private int appliedSwapInterval;                          // last value given to the context (rendering thread)
	private boolean swapIntervalApplied = false;              // false: to be given to the context at the next frame
	private volatile int effectiveSwapInterval = Integer.MIN_VALUE; // value of the context, MIN_VALUE before the first frame
	
	protected volatile boolean showAxes;
    protected volatile boolean showGrid = true;
    protected volatile boolean showLabel;

    /** Size of the drawing area (pixels). */
    protected int width = 1, height = 1;

    protected float scaleX = 1, scaleY = 1;

    private final List<DisplayOptionListener> displayOptionListeners = new CopyOnWriteArrayList<>();
    private final Object optionsLock = new Object(); // option changes and their detection are atomic

    protected GLChartRenderer(boolean showAxes, boolean showLabel) {
        this.showAxes = showAxes;
        this.showLabel = showLabel;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // New context (first initialization, or context recreated): the swap interval is given to it again
        swapIntervalApplied = false;
        applySwapInterval(drawable);

        // OpenGL configuration of the machine, logged only once (support aid)
		if (GL_INFO_LOGGED.compareAndSet(false, true)) {
		    glInfo = gl.glGetString(GL2.GL_RENDERER) + " (" + gl.glGetString(GL2.GL_VENDOR) + ") - "
		            + drawable.getContext().getGLVersion() + " - swap interval " + effectiveSwapInterval;
		    LOGGER.log(System.Logger.Level.INFO, "OpenGL : " + glInfo);
		    if(Activator.getDefault() != null) Activator.logInfoMessage("OpenGL : " + glInfo, getClass());
		}
        
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        
        if(drawable instanceof GLWindow) {
    		float[] scale = new float[2];
    		((GLWindow)drawable).getScreen().getPrimaryMonitor().getPixelScale(scale);
    		this.scaleX = scale[0];
    		this.scaleY = scale[1];
        } else if(drawable instanceof GLCanvas) {
        	float dpiScale = Display.getCurrent().getDPI().x / 96.0f;
        	this.scaleX = dpiScale;
    		this.scaleY = dpiScale;
        }
        
    }
    
    public float getScaleX() {
		return scaleX;
	}
    
    public float getScaleY() {
		return scaleY;
	}

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
    }

    /** Goes back to the initial view (R key). */
    public abstract void resetView();

    // ------------------------------------------------------------------
    // Swap interval

    /**
     * Swap interval of the charts created from now on: 0 (default) to swap
     * the buffers without waiting, 1 to wait for the vertical retrace of the
     * screen, -1 for an adaptive synchronization (if the driver supports it,
     * otherwise 1). Usable from any thread; the existing charts keep theirs.
     */
    public static void setDefaultSwapInterval(int interval) {
        defaultSwapInterval = interval;
    }

    /** Swap interval of the charts created from now on (see {@link #setDefaultSwapInterval}). */
    public static int getDefaultSwapInterval() {
        return defaultSwapInterval;
    }

    /**
     * Swap interval of this chart: 0 to swap the buffers without waiting
     * (default), 1 to wait for the vertical retrace, -1 for an adaptive
     * synchronization (if the driver supports it, otherwise 1). Applied at
     * the next frame; usable from any thread. The graphics driver settings
     * can impose another behavior (vertical synchronization forced on or off).
     */
    public void setSwapInterval(int interval) {
        swapInterval = interval;
    }

    /** Requested swap interval (see {@link #setSwapInterval}). */
    public int getSwapInterval() {
        return swapInterval;
    }

    /**
     * Swap interval of the OpenGL context, as known by JOGL after the last
     * change (Integer.MIN_VALUE before the first frame). It can differ from
     * the requested one if the driver refused it.
     */
    public int getEffectiveSwapInterval() {
        return effectiveSwapInterval;
    }

    /**
     * Gives the requested swap interval to the context if it changed. To be
     * called with the context current: in init() and at the beginning of
     * each display() of the subclasses.
     */
    protected final void applySwapInterval(GLAutoDrawable drawable) {
        int interval = swapInterval;
        if (swapIntervalApplied && interval == appliedSwapInterval) return;
        GLContext context = drawable.getContext();
        boolean accepted = context.setSwapInterval(interval);
        if (!accepted && interval < 0) context.setSwapInterval(1); // adaptive synchronization not supported
        appliedSwapInterval = interval;
        swapIntervalApplied = true;
        effectiveSwapInterval = context.getSwapInterval();
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case 'r': resetView(); break;
            case 'a': toggleAxes(); break;
            case 'g': toggleGrid(); break;
            case 'l': toggleLabel(); break;
            default: break;
        }
    }

    /** Shows or hides the axes (also with the A key). */
    public void setAxesVisible(boolean visible) { setOption(DisplayOption.AXES, visible, () -> showAxes, v -> showAxes = v); }
    public boolean isAxesVisible() { return showAxes; }
    public void toggleAxes() { toggleOption(DisplayOption.AXES, () -> showAxes, v -> showAxes = v); }

    /** Shows or hides the grid (also with the G key). */
    public void setGridVisible(boolean visible) { setOption(DisplayOption.GRID, visible, () -> showGrid, v -> showGrid = v); }
    public boolean isGridVisible() { return showGrid; }
    public void toggleGrid() { toggleOption(DisplayOption.GRID, () -> showGrid, v -> showGrid = v); }

    /** Shows or hides the tick labels (also with the L key). */
    public void setLabelsVisible(boolean visible) { setOption(DisplayOption.LABELS, visible, () -> showLabel, v -> showLabel = v); }
    public boolean isLabelsVisible() { return showLabel; }
    public void toggleLabel() { toggleOption(DisplayOption.LABELS, () -> showLabel, v -> showLabel = v); }

    public void setGridVisibility(boolean value) {
        setGridVisible(value);
    }

    // ------------------------------------------------------------------
    // Display options

    /** Adds a display option listener (see {@link DisplayOptionListener} for the calling thread). */
    public void addDisplayOptionListener(DisplayOptionListener listener) { if (listener != null) displayOptionListeners.add(listener); }
    public void removeDisplayOptionListener(DisplayOptionListener listener) { displayOptionListeners.remove(listener); }

    /**
     * Gives the value to an option and notifies the listeners if it changed.
     *
     * @param current reads the option
     * @param apply   writes the option (with its side effects), under the options lock
     */
    protected final void setOption(DisplayOption option, boolean value, BooleanSupplier current, Consumer<Boolean> apply) {
        synchronized (optionsLock) {
            if (current.getAsBoolean() == value) return;
            apply.accept(value);
        }
        notifyDisplayOption(option, value);
    }

    /** Inverts an option and notifies the listeners (see {@link #setOption}). */
    protected final void toggleOption(DisplayOption option, BooleanSupplier current, Consumer<Boolean> apply) {
        boolean value;
        synchronized (optionsLock) {
            value = !current.getAsBoolean();
            apply.accept(value);
        }
        notifyDisplayOption(option, value);
    }

    /** Called outside the options lock. */
    private void notifyDisplayOption(DisplayOption option, boolean value) {
        for (DisplayOptionListener listener : displayOptionListeners) {
            try {
                listener.displayOptionChanged(option, value);
            } catch (RuntimeException e) {
                e.printStackTrace(); // an error in a listener must not interrupt rendering
            }
        }
    }
}
