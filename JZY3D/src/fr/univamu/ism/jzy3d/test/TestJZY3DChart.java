package fr.univamu.ism.jzy3d.test;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.Settings;
import org.jzy3d.chart.swt.SWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axes.AxeBox;
import org.jzy3d.plot3d.text.renderers.TextBitmapRenderer;

public class TestJZY3DChart {
	
	private static Shell shell;
	
	private static void createSurface() {
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
		surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(),
				surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
		surface.setFaceDisplayed(true);
		surface.setWireframeDisplayed(false);
		surface.setLegendDisplayed(false);

		// Create a chart
		Settings.getInstance().setHardwareAccelerated(true);

		Chart chart = SWTChartComponentFactory.chart(shell);
		chart.getScene().getGraph().add(surface);

		ChartLauncher.openChart(chart);

		shell.setText("JZY3D Test");
		shell.setSize(800, 600);
	}

	private static void createTrajectory() {
		Settings.getInstance().setHardwareAccelerated(true);
		
		Chart chart = SWTChartComponentFactory.chart(shell);
		AxeBox axeBoxe = (AxeBox) chart.getView().getAxe();
		axeBoxe.setTextRenderer(new TextBitmapRenderer(TextBitmapRenderer.Font.Helvetica_18));
		
		LineStrip trajectory = new LineStrip();
		for (int i = 0; i < 1000; i++) {
			double x = 3f*Math.cos(2f*Math.PI*i/100f);
			double y = 3f*Math.sin(2f*Math.PI*i/100f);
			double z = i;
			Coord3d coord3d = new Coord3d(x, y, z);
			Point point = new Point(coord3d, new Color(1,0,0,0.5f));
			trajectory.add(point);
		}

		trajectory.setDisplayed(true);
		trajectory.setFaceDisplayed(true);
		trajectory.setWireframeDisplayed(false);
		trajectory.setWireframeWidth(1);
        
		chart.getScene().getGraph().add(trajectory);
		
		
		
		ChartLauncher.openChart(chart);
	}
	
    public static void main(String[] args) {
    	
    	boolean displayLoop = true;
    	int choice = 2;
		
		if(args.length > 0) {
			int i = 0;
			while (i < args.length) {
				if(args[i].equalsIgnoreCase("-displayLoop")) {
					displayLoop = Boolean.parseBoolean(args[i+1]);
					i++;
				}
				if(args[i].equalsIgnoreCase("-choice")) {
					choice = Integer.parseInt(args[i+1]);
					i++;
				}
				i++;
			}
		}

        Display display = Display.getDefault();
        shell = new Shell(display);
        shell.setLayout(new FillLayout());

        if(choice == 1) createSurface();
        if(choice == 2) createTrajectory();

        
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