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
