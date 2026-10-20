package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * Surface 3D dans une fenêtre NEWT, pour macOS.
 * Toutes les fonctionnalités sont dans {@link GLSurface}.
 */
public class NewtSurface extends GLSurface {

    public NewtSurface(Composite container, SurfaceData surfaceData) {
        super(container, GLToolkit.NEWT, surfaceData);
    }
}
