package fr.univamu.ism.docometre.chartxyz.tests;

import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/** {@link SurfaceTest} with NEWT (macOS). */
public class NewtSurfaceTest {

    public static void main(String[] args) {
        SurfaceTest.run(GLToolkit.NEWT, SurfaceData.loadFromEquation());
    }
}
