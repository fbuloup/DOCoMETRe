package fr.univamu.ism.docometre.chartxyz.tests;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.GLComponent;
import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * 3D surface (equation, or file with DemoSupport.loadSurface(args)); the PUSH
 * button closes the view or creates it again.
 */
public class SurfaceTest {

    public static void main(String[] args) {
        run(GLToolkit.SWT, SurfaceData.loadFromEquation());
    }

    static void run(GLToolkit toolkit, SurfaceData surfaceData) {
        if (surfaceData == null) return;

        Shell shell = DemoSupport.createShell(new Display(), "3D surface viewer — SWT + JOGL (" + toolkit + ")", 1000, 700);
        GLComponent[] surface = {new GLSurface(shell, toolkit, surfaceData)};
        DemoSupport.addCloseReopenButton(shell, surface, () -> new GLSurface(shell, toolkit, surfaceData));
        DemoSupport.run(shell);
    }
}
