package fr.univamu.ism.docometre.chartxyz.tests;

import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * {@link Chart3DTest} avec NEWT, à utiliser sous macOS ; la courbe est lue
 * dans le fichier args[0] (data/sample_curve.txt par défaut).
 */
public class NewtChart3DTest {

    public static void main(String[] args) {
        Chart3DTest.run(GLToolkit.NEWT, DemoSupport.loadCurve(args));
    }
}
