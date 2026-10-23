package fr.univamu.ism.docometre.chartxyz.tests;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Chart1DRenderer;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.chartxyz.GLChart2D;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.GLToolkit;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.LineStyle;

/**
 * Static chart test. Platform independent.
 */
public class StaticChartsTest {
	
    public static void main(String[] args) {
    	
    	Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Static signals — SWT + JOGL (Linux, Mac or Windows)");
        shell.setSize(1000, 500);
        shell.setLayout(new GridLayout(2, true));
    	
        // 2D chart
        // Curves
        double sf3 = 1000;
        double wtw2 = 5;
        float[] dataLissajouX = new float[(int) (sf3*wtw2)], dataLissajouY = new float[(int) (sf3*wtw2)];
        float[] dataCircleX = new float[(int) (sf3*wtw2)], dataCircleY = new float[(int) (sf3*wtw2)];
        for (int i = 0; i < dataLissajouX.length; i++) {
            double t = i / sf3;
            dataLissajouX[i] = (float) (Math.sin(2 * Math.PI * 0.7 * t) * (1 + 0.5 * Math.sin(0.3 * t)));
            dataLissajouY[i] = (float) (0.5 * Math.sin(2 * Math.PI * 1.1 * t + 0.1 * t));
            dataCircleX[i] = (float) (0.3 * Math.cos(2 * Math.PI * 0.5 * t) + 0.5 * Math.sin(0.2 * t));
            dataCircleY[i] = (float) (0.3 * Math.sin(2 * Math.PI * 0.5 * t));
        }
        Curve2DData lissajous = new Curve2DData(sf3).setName("Lissajous");
        lissajous.addSamples(dataLissajouX, dataLissajouY);
        Curve2DData circle = new Curve2DData(sf3).setName("circle").setLineStyle(LineStyle.DASH);
        circle.addSamples(dataCircleX, dataCircleY);
        // Chart
        GLChart2D graph2D = new GLChart2D(shell, GLToolkit.platformDefault(), wtw2, lissajous, circle);
        graph2D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph2D.setLegendPosition(LegendPosition.BELOW); // legend below the abscissa axis

        // 3D chart
        // Curves
        double sf4 = 1000;
        double wtw3 = 15;
        Curve3DData spiral = new Curve3DData(sf4).setName("spiral");
        Curve3DData helix = new Curve3DData(sf4).setName("helix").setLineStyle(LineStyle.DASH_DASH_DOT).setLineWidth(2);
        float[] spiralX = new float[(int)(wtw3*sf4)], spiralY = new float[(int)(wtw3*sf4)], spiralZ = new float[(int)(wtw3*sf4)];
        float[] helixX = new float[(int)(wtw3*sf4)], helixY = new float[(int)(wtw3*sf4)], helixZ = new float[(int)(wtw3*sf4)];
        for (int i = 0; i < spiralX.length; i++) {
            double t = i / sf4;
            spiralX[i] = (float) Math.sin(2 * Math.PI * 0.7 * t);
            spiralY[i] = (float) Math.sin(2 * Math.PI * 0.3 * t + 0.05 * t);
            spiralZ[i] = (float) (Math.cos(2 * Math.PI * 1.1 * t) * (1 + 0.3 * Math.sin(0.2 * t)));
            helixX[i] = (float) (0.4 * Math.cos(2 * Math.PI * 0.8 * t));
            helixY[i] = (float) (0.4 * Math.sin(2 * Math.PI * 0.8 * t));
            helixZ[i] = (float) Math.sin(2 * Math.PI * 0.1 * t);
        }
        spiral.addSamples(spiralX,  spiralY, spiralZ);
        helix.addSamples(helixX, helixY, helixZ);
        // Chart
        GLChart3D graph3D = new GLChart3D(shell, GLToolkit.platformDefault(), wtw3, spiral, helix);
        graph3D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graph3D.setLegendFont(null, 11, SWT.BOLD); // system font, bold
        
        // 1D chart
        // Curves
        Random random = new Random();
        double sf1 = 1000;
        double wtw1 = 10;
        float[] dataSine = new float[(int) (wtw1*sf1)];
        float[] dataNoisy = new float[(int) (wtw1*sf1)];
        for (int i = 0; i < dataSine.length; i++) {
        	double t = i / sf1;
        	dataSine[i] = (float) Math.sin(2 * Math.PI * 1.0 * t);
        	dataNoisy[i] = (float) (0.4 * Math.sin(2 * Math.PI * 7.0 * t) - 0.5 + 0.05 * random.nextGaussian());
		}
        double sf2 = 100;
        float[] dataDrift = new float[(int) (wtw1*sf2)];
        for (int i = 0; i < dataDrift.length; i++) {
        	double t = i / sf2;
        	dataDrift[i] = (float) (0.8 * Math.sin(2 * Math.PI * 0.05 * t));
		}
        Curve1DData sine = new Curve1DData(sf1, dataSine).setName("1 Hz sine").setLineWidth(3);
        Curve1DData noisy = new Curve1DData(sf1, dataNoisy).setName("7 Hz sine + noise").setLineWidth(1);
        Curve1DData drift = new Curve1DData(sf2, dataDrift).setName("drift (100 Hz)").setLineWidth(2).setLineStyle(LineStyle.DASH_DOT);
        // Chart
        GLChart1D graph1D = new GLChart1D(shell, GLToolkit.platformDefault(), wtw1, sine, noisy, drift);
        // Very important because the sweep mode removes some points in front of the write cursor
        graph1D.setDisplayMode(Chart1DRenderer.DisplayMode.SCROLL); 
        graph1D.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        graph1D.setScaleFont("Courier New", 10, SWT.NORMAL);
        graph1D.setLegendFont(null, 11, SWT.BOLD); // system font, bold
        graph1D.setLegendPosition(LegendPosition.BELOW); // legend below the abscissa axis
        
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

}
