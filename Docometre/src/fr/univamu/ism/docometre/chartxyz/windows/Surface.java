package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * Surface 3D dans un GLCanvas SWT, pour Windows et Linux.
 * Toutes les fonctionnalités sont dans {@link GLSurface}.
 */
public class Surface extends GLSurface {

    public Surface(Composite container, SurfaceData surfaceData) {
        super(container, GLToolkit.SWT, surfaceData);
    }
}
