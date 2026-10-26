package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * 3D surface in a NEWT window, for macOS.
 * All the features are in {@link GLSurface}.
 */
public class NewtSurface extends GLSurface {

    public NewtSurface(Composite container, SurfaceData surfaceData) {
        super(container, GLToolkit.NEWT, surfaceData);
    }
}
