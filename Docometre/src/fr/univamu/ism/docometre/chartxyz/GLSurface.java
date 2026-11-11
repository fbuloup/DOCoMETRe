package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Display of a 3D surface (see {@link Surface3DRenderer}) embedded in an SWT
 * user interface.
 */
public class GLSurface implements GLComponent {

    protected final Surface3DRenderer renderer;
    protected final GLHost host;

    public GLSurface(Composite container, GLToolkit toolkit, SurfaceData surfaceData) {
        renderer = new Surface3DRenderer(surfaceData);
        host = GLHost.create(toolkit, container, true, renderer, null);
    }

    /** Renderer of the surface, for the display settings. */
    public Surface3DRenderer getRenderer() { return renderer; }

    /**
     * Swap interval: 0 to swap the buffers without waiting (default), 1 to
     * wait for the vertical retrace, -1 for an adaptive synchronization (see
     * {@link GLChartRenderer#setSwapInterval}). Applied at the next frame.
     */
    public void setSwapInterval(int interval) {
        renderer.setSwapInterval(interval);
    }

    /** Requested swap interval. */
    public int getSwapInterval() {
        return renderer.getSwapInterval();
    }

    /** Swap interval of the OpenGL context (see {@link GLChartRenderer#getEffectiveSwapInterval}). */
    public int getEffectiveSwapInterval() {
        return renderer.getEffectiveSwapInterval();
    }

    /**
     * Adds a listener told about each change of a display option (solid,
     * wireframe, lighting, axes, grid, labels; see {@link DisplayOption}),
     * with the keyboard or the API; see {@link DisplayOptionListener} for the
     * calling thread.
     */
    public void addDisplayOptionListener(DisplayOptionListener listener) {
        renderer.addDisplayOptionListener(listener);
    }

    public void removeDisplayOptionListener(DisplayOptionListener listener) {
        renderer.removeDisplayOptionListener(listener);
    }

    @Override
    public void close() {
        host.close();
    }

    @Override
    public boolean isDisposed() {
        return host.isDisposed();
    }

    @Override
    public void moveAbove(Control control) {
        host.getControl().moveAbove(control);
    }

    @Override
    public void setLayoutData(GridData gridData) {
        host.getControl().setLayoutData(gridData);
    }
}
