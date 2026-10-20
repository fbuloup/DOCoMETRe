package fr.univamu.ism.docometre.chartxyz.tests;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.GLComponent;
import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * Surface 3D (équation, ou fichier avec DemoSupport.loadSurface(args)) ; le
 * bouton PUSH ferme la vue ou la recrée.
 */
public class SurfaceTest {

    public static void main(String[] args) {
        run(GLToolkit.SWT, SurfaceData.loadFromEquation());
    }

    static void run(GLToolkit toolkit, SurfaceData surfaceData) {
        if (surfaceData == null) return;

        Shell shell = DemoSupport.createShell(new Display(), "Visualiseur de surface 3D — SWT + JOGL (" + toolkit + ")", 1000, 700);
        GLComponent[] surface = {new GLSurface(shell, toolkit, surfaceData)};
        DemoSupport.addCloseReopenButton(shell, surface, () -> new GLSurface(shell, toolkit, surfaceData));
        DemoSupport.run(shell);
    }
}
