package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.function.IntSupplier;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.GLChartRenderer;
import fr.univamu.ism.docometre.chartxyz.GLSurface;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.Point3D;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

/**
 * Swap interval of real SWT charts (1D, 3D, surface): default, change, adaptive,
 * default for the new charts. To be run with a display: the effective values are
 * checked only if the OpenGL context supports the swap interval control (always
 * the case on Windows, WGL_EXT_swap_control; not with Xvfb).
 */
public class SwapIntervalTest {
	static Display display;
	static int failures = 0;
	static void check(String what, boolean ok) { System.out.println((ok ? "OK     " : "FAILED ") + what); if (!ok) failures++; }
	static void pump(long ms) throws InterruptedException {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) { while (display.readAndDispatch()) { } Thread.sleep(5); }
	}
	static boolean supported = true; // the context accepts the swap interval control

	static void checkChart(String name, java.util.function.IntConsumer set, IntSupplier requested, IntSupplier effective) throws InterruptedException {
		pump(300);
		check(name + ": first frame drawn, default requested 0", requested.getAsInt() == 0 && effective.getAsInt() != Integer.MIN_VALUE);
		int initial = effective.getAsInt();
		set.accept(1); pump(200);
		if (requested.getAsInt() == 1 && effective.getAsInt() != 1 && effective.getAsInt() == initial) {
			supported = false;
			System.out.println("SKIPPED " + name + ": the OpenGL context refuses the swap interval control (context stays at " + initial + ")");
		}
		check(name + ": 1 requested" + (supported ? ", context " + effective.getAsInt() : ""), requested.getAsInt() == 1 && (!supported || effective.getAsInt() == 1));
		set.accept(-1); pump(200);
		check(name + ": -1 requested (adaptive, or 1 if not supported)" + (supported ? ", context " + effective.getAsInt() : ""),
				requested.getAsInt() == -1 && (!supported || effective.getAsInt() == -1 || effective.getAsInt() == 1));
		set.accept(0); pump(200);
		check(name + ": back to 0" + (supported ? ", context " + effective.getAsInt() : ""), requested.getAsInt() == 0 && (!supported || effective.getAsInt() == 0));
	}
	public static void main(String[] args) throws Exception {
		display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(900, 400);
		float[] values = new float[2000];
		for (int i = 0; i < values.length; i++) values[i] = (float) Math.sin(i * 0.01);
		GLChart1D chart1D = GLChart1D.createChart(shell, 2, new Curve1DData("a", 1000, values));
		Curve3DData curve3D = new Curve3DData("b", 100);
		GLChart3D chart3D = GLChart3D.createChart(shell, curve3D);
		Point3D[][] grid = new Point3D[10][10];
		for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) grid[i][j] = new Point3D(i, j, (float) Math.sin(i + j));
		GLSurface surface = new GLSurface(shell, GLToolkit.SWT, new SurfaceData(grid));
		shell.open();
		checkChart("1D", chart1D::setSwapInterval, chart1D::getSwapInterval, chart1D::getEffectiveSwapInterval);
		checkChart("3D", chart3D::setSwapInterval, chart3D::getSwapInterval, chart3D::getEffectiveSwapInterval);
		checkChart("surface", surface::setSwapInterval, surface::getSwapInterval, surface::getEffectiveSwapInterval);
		GLChartRenderer.setDefaultSwapInterval(1);
		GLChart1D later = GLChart1D.createChart(shell, 2, new Curve1DData("c", 1000, values));
		shell.layout(true, true);
		pump(300);
		check("chart created after setDefaultSwapInterval(1): 1 requested" + (supported ? ", context " + later.getEffectiveSwapInterval() : ""),
				later.getSwapInterval() == 1 && (!supported || later.getEffectiveSwapInterval() == 1));
		check("existing chart keeps 0", chart1D.getSwapInterval() == 0 && (!supported || chart1D.getEffectiveSwapInterval() == 0));
		GLChartRenderer.setDefaultSwapInterval(GLChartRenderer.DEFAULT_SWAP_INTERVAL);
		shell.dispose();
		display.dispose();
		System.out.println(failures == 0 ? "All tests passed." : failures + " failure(s)");
		System.exit(failures == 0 ? 0 : 1);
	}
}
