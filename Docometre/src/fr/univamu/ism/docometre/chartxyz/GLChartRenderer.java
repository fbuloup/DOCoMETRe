package fr.univamu.ism.docometre.chartxyz;

import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

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
 */
public abstract class GLChartRenderer implements GLEventListener, ChartInteraction {

	private static final AtomicBoolean GL_INFO_LOGGED = new AtomicBoolean();
	private static volatile String glInfo = "OpenGL not initialised";
	private static final System.Logger LOGGER = System.getLogger(GLChartRenderer.class.getName());
	
	protected volatile boolean showAxes;
    protected volatile boolean showGrid = true;
    protected volatile boolean showLabel;

    /** Size of the drawing area (pixels). */
    protected int width = 1, height = 1;

    protected GLChartRenderer(boolean showAxes, boolean showLabel) {
        this.showAxes = showAxes;
        this.showLabel = showLabel;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // OpenGL configuration of the machine, logged only once (support aid)
		if (GL_INFO_LOGGED.compareAndSet(false, true)) {
		    glInfo = gl.glGetString(GL2.GL_RENDERER) + " (" + gl.glGetString(GL2.GL_VENDOR) + ") - "
		            + drawable.getContext().getGLVersion();
		    LOGGER.log(System.Logger.Level.INFO, "OpenGL : " + glInfo);
		    if(Activator.getDefault() != null) Activator.logInfoMessage("OpenGL : " + glInfo, getClass());
		}
        
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
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

    public void toggleAxes() { showAxes = !showAxes; }
    public void toggleGrid() { showGrid = !showGrid; }
    public void toggleLabel() { showLabel = !showLabel; }

    public void setGridVisibility(boolean value) {
        showGrid = value;
    }
}
