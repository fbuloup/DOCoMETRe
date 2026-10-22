package fr.univamu.ism.docometre.chartxyz.tests;

import fr.univamu.ism.docometre.chartxyz.GLToolkit;

/**
 * {@link Chart3DTest} with NEWT, to be used on macOS; the curve is read from
 * the file args[0] (data/sample_curve.txt by default).
 */
public class NewtChart3DTest {

    public static void main(String[] args) {
        Chart3DTest.run(GLToolkit.NEWT, DemoSupport.loadCurve(args));
    }
}
