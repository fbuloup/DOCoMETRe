package fr.univamu.ism.jzy3d.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.Settings;
import org.jzy3d.chart.factories.CanvasNewtSWT;
import org.jzy3d.chart.factories.SWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Shape;

import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.JoglVersion;

public class TestJZY3DChart {
	
	private static Shell shell;
	private static boolean visible;
	private static CanvasNewtSWT canvas;
	private static Chart chart;
	
	public static Chart createSurface(Composite container) {
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
		final Shape surface = new SurfaceBuilder().orthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
		surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(),
				surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
		surface.setFaceDisplayed(true);
		surface.setWireframeDisplayed(false);
		surface.setLegendDisplayed(false);

		// Create a chart
		Settings.getInstance().setHardwareAccelerated(true);
		chart = new SWTChartFactory(container).newChart();
		canvas = (CanvasNewtSWT) chart.getCanvas();
		chart.add(surface);
		ChartLauncher.openChart(chart);
		visible = true;
		
		Button button = new Button(container, SWT.NONE);
		button.setText("Toggle chart visibility");
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				visible = !visible;
				if(!visible) {
					canvas.dispose();
					container.layout();
				} else {
					chart = new SWTChartFactory(container).newChart();
					canvas = (CanvasNewtSWT) chart.getCanvas();
					chart.add(surface);
					ChartLauncher.openChart(chart);
					canvas.moveAbove(button);
					container.layout();
				}
			}
		});
		((CanvasNewtSWT) chart.getCanvas()).addMouseListener(new MouseListener() {
			@Override
			public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseWheelMoved");
			}
			@Override
			public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseReleased");
			}
			@Override
			public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mousePressed");
			}
			@Override
			public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseMoved");
			}
			@Override
			public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseExited");
			}
			@Override
			public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseEntered");
			}
			@Override
			public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseDragged");
			}
			@Override
			public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseClicked");
			}
		});
		return chart;

	}

	public static Chart createTrajectory(Composite container) {
		
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
		
		Settings.getInstance().setHardwareAccelerated(true);
		chart = new SWTChartFactory(container).newChart();
		canvas = (CanvasNewtSWT) chart.getCanvas();
//		AxeBox axeBoxe = (AxeBox) chart.getView().getAxe();
//		axeBoxe.setTextRenderer(new TextBitmapRenderer(TextBitmapRenderer.Font.Helvetica_18));
		chart.getScene().getGraph().add(trajectory);
		ChartLauncher.openChart(chart);

		visible = true;
		
		Button button = new Button(container, SWT.NONE);
		button.setText("Toggle chart visibility");
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				visible = !visible;
				if(!visible) {
					canvas.dispose();
					container.layout();
				} else {
					chart = new SWTChartFactory(container).newChart();
					canvas = (CanvasNewtSWT) chart.getCanvas();
					chart.getScene().getGraph().add(trajectory);
					ChartLauncher.openChart(chart);
					canvas.moveAbove(button);
					container.layout();
				}
			}
		});
		((CanvasNewtSWT) chart.getCanvas()).addMouseListener(new MouseListener() {
			@Override
			public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseWheelMoved");
			}
			@Override
			public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseReleased");
			}
			@Override
			public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mousePressed");
			}
			@Override
			public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseMoved");
			}
			@Override
			public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseExited");
			}
			@Override
			public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseEntered");
			}
			@Override
			public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseDragged");
			}
			@Override
			public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
				System.out.println("mouseClicked");
			}
		});
		return chart;
	}
	
    public static void main(String[] args) {
    	
    	System.out.println(JoglVersion.getInstance().toString());
    	
    	boolean displayLoop = true;
    	int choice = 1;
		
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

        if(choice == 1) createSurface(shell);
        if(choice == 2) createTrajectory(shell);

        
        shell.open();

        
        if(displayLoop) {
			/* SWT display loop */
	        while (!shell.isDisposed()) {
	            if (!display.readAndDispatch())  display.sleep();
	        }
	        display.dispose();
	        System.exit(0);
        }
        
    }
    
    public static Shell getShell() {
		return shell;
	}
    
}