package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * 3D surface in an SWT GLCanvas, for Windows and Linux.
 * All the features are in {@link GLSurface}.
 */
public class Surface extends GLSurface {

    public Surface(Composite container, SurfaceData surfaceData) {
        super(container, GLToolkit.SWT, surfaceData);
    }
}
