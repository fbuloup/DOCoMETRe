package fr.univamu.ism.jzy3d.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.Settings;
import org.jzy3d.chart.swt.SWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;

public class TestJZY3DChart {
	
	private static Shell shell;

    public static void main(String[] args) {
    	
    	boolean displayLoop = true;
		
		if(args.length > 0) {
			int i = 0;
			while (i < args.length) {
				if(args[i].equalsIgnoreCase("-displayLoop")) {
					displayLoop = Boolean.parseBoolean(args[i+1]);
					i++;
				}
				i++;
			}
		}
    	
    	
        Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                return x * Math.sin(x * y);
            }
        };

        // Define range and precision for the function to plot
        Range range = new Range(-3, 3);
        int steps = 100;

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(false);
        surface.setLegendDisplayed(false);

        // Create a chart
        Settings.getInstance().setHardwareAccelerated(true);

        Display display = Display.getDefault();
        shell = new Shell(display);
        shell.setLayout(new FillLayout());
        Composite container = new Composite(shell, SWT.NONE);
        container.setLayout(new FillLayout());

        Chart chart = SWTChartComponentFactory.chart(container);
        chart.getScene().getGraph().add(surface);

        ChartLauncher.openChart(chart);

        shell.setText("JZY3D Test");
        shell.setSize(800, 600);
        shell.open();

        
        if(displayLoop) {
			/* SWT display loop */
	        while (!shell.isDisposed()) {
	            if (!display.readAndDispatch())  display.sleep();
	        }
	        display.dispose();
        }
    }
    
    public static Shell getShell() {
		return shell;
	}
}