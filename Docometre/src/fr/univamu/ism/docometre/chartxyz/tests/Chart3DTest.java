package fr.univamu.ism.docometre.chartxyz.tests;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.GLComponent;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * Courbe 3D statique (équation ou fichier) ; le bouton PUSH ferme le graphe
 * ou le recrée.
 */
public class Chart3DTest {

    public static void main(String[] args) {
        run(GLToolkit.SWT, Curve3DData.loadFromEquation());
    }

    static void run(GLToolkit toolkit, Curve3DData curveData) {
        if (curveData == null) return;

        Shell shell = DemoSupport.createShell(new Display(), "Visualiseur de courbe 3D — SWT + JOGL (" + toolkit + ")", 1000, 700);
        GLComponent[] chart = {new GLChart3D(shell, toolkit, curveData)};
        DemoSupport.addCloseReopenButton(shell, chart, () -> new GLChart3D(shell, toolkit, curveData));
        DemoSupport.run(shell);
    }
}
