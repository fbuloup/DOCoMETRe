/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.rtswtchart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import com.jogamp.opengl.GL2;

/**
 * 
 * This Version of SWTChart uses 2.3.2 version of JOGL library
 * 
 * !!!!!! WARNING : use version 4.333 or more of SWT Library !!!!!!
 * 
 * Instances of this class represent a chart that behaves like an oscilloscope
 * or a time chart.
 * <p>
 * It can render data series in real time and is specifically designed for data
 * acquisition systems. It is based on OpenGL and uses JOGL library (see
 * https://jogl.dev.java.net/).
 * </p>
 * <p>
 * You can control the way series are displayed (line width and style), legend
 * visibility and position and the chart has an autoscale capability. There are
 * several areas in the chart. A chart area that displays series and grids. A
 * legend area that displays legend, which is the series names list that are
 * displayed in the chart area, and Left and bottom axis areas that display
 * scales for time (bottom axis) and data values (left axis). Bellow is an
 * example of code that generate random data for 10 seconds.In order to use this
 * class, don't forget to add OpenGL Jars and natives libraries to you classpath
 * (jogl.jar, gluegen-rt.jar and related natives libraries) which are packaged
 * in rtswtchart.jar (on Linux systems you might need to have libjogl_awt.so also).
 * </p>
 * <code>
 * <pre>
 * import java.util.Random;
 * import java.util.Timer;
 * import java.util.TimerTask;
 * import org.eclipse.swt.SWT;
 * import org.eclipse.swt.layout.GridData;
 * import org.eclipse.swt.layout.GridLayout;
 * import org.eclipse.swt.widgets.Display;
 * import org.eclipse.swt.widgets.Shell;
 * 
 * import rtswtchart.RTSWTOscilloChart;
 * import rtswtchart.RTSWTOscilloSerie;
 * 
 * public class TestRTSWTOscilloChart {
 *
 *         private static RTSWTOscilloSerie serie;
 *         private static Random random = new Random();
 *                 
 *         public static class GenerateData extends TimerTask {
 *  
 *                 private double previousTime = 0;
 *  
 *                 public void run() {
 *                         int nbSamples = 1+random.nextInt(10); 
 *                         final Double[] x = new Double[nbSamples]; 
 *                         final Double[] y = new Double[nbSamples]; 
 *                         for (int i = 0; i < y.length; i++) { 
 *                                 x[i] = previousTime + i/1000.0;
 *                                 y[i] = 5*Math.sin(2*Math.PI*1*x[i]) + (random.nextDouble() - 0.5)*2; 
 *                         } 
 *                         previousTime = x[x.length - 1]; 
 *                         serie.addPoints(x, y);
 *                         if(previousTime > 10) cancel();  
 *                 }
 *         }
 *  
 *         public static void main(String[] args) {
 *                 Display display = new Display (); 
 *                 Shell shell = new Shell  (display); 
 *                 shell.setLayout(new GridLayout(1,true));
 *                 RTSWTOscilloChart rtswtChartOscillo = new RTSWTOscilloChart(shell, SWT.NONE); 
 *                 rtswtChartOscillo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
 *                 serie = rtswtChartOscillo.createSerie("serie", display.getSystemColor(SWT.COLOR_GREEN)); 
 *                 shell.open ();
 *                 Timer timer = new Timer(); 
 *                 timer.schedule(new GenerateData(), 100, 40); 
 *                 while (!shell.isDisposed ()) {
 *                         if(!display.readAndDispatch ()) display.sleep ();
 *                 }
 *                 timer.cancel();
 *                 display.dispose (); 
 *                 System.exit(0);
 *         }
 *  }
 * </pre>
 *</code>
 * 
 * @author frank buloup
 */
public final class RTSWTOscilloChart extends RTSWTChart {
	
	/**
	 * Chart displays data series over a second by default
	 */
	private double windowTimeWidth = 1;
	
	/**
	 * Basic chart constructor. Constructs a new instance of this class given
	 * its parent and a style value describing its behaviour and appearance.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 */
	public RTSWTOscilloChart(final Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Chart constructor. Constructs a new instance of this class given its
	 * parent and a style value describing its behaviour and appearance and a
	 * window time width.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @param windowTimeWidth
	 *            window time width
	 */
	public RTSWTOscilloChart(Composite parent, int style, double windowTimeWidth) {
		this(parent, style);
		if (windowTimeWidth <= 0)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "windowTimeWidth can not be null or negative");
		this.windowTimeWidth = windowTimeWidth;
	}

	/**
	 * Chart constructor. Constructs a new instance of this class given its
	 * parent, a style value describing its behaviour and appearance, a window
	 * time width and amplitude extrema values.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @param windowTimeWidth
	 *            window time width
	 * @param yMin
	 *            minimum amplitude value
	 * @param yMax
	 *            maximum amplitude value
	 */
	public RTSWTOscilloChart(Composite parent, int style, double windowTimeWidth, double yMin, double yMax) {
		this(parent, style, windowTimeWidth);
		if (yMin > yMax)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "yMin can not be greater than yMax");
		if (yMin == yMax)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "yMin and yMax cannot be equals");
		setyMin(yMin);
		setyMax(yMax);
	}

	/**
	 * Chart constructor. See also
	 * {@link #RTSWTOscilloChart(Composite, int, double, double, double)}
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @param windowTimeWidth
	 *            window time width
	 * @param yMin
	 *            minimum amplitude value
	 * @param yMax
	 *            maximum amplitude value
	 * @param autoscale
	 *            enable or disable autoscale capability
	 */
	public RTSWTOscilloChart(Composite parent, int style, double windowTimeWidth, double yMin, double yMax, boolean autoScale) {
		this(parent, style, windowTimeWidth, yMin, yMax);
		setAutoScale(autoScale);
	}

	/**
	 * Chart constructor. See also
	 * {@link #RTSWTOscilloChart(Composite, int, double, double, double, boolean, int, int)}
	 * . Values for {@link #antialias} can be <b>SWT.ON</b> or <b>SWT.OFF</b>.
	 * Values for {@link #interpolation} can be <b>SWT.DEFAULT</b>,
	 * <b>SWT.NONE</b>, <b>SWT.LOW</b> or <b>SWT.HIGH</b>.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @param windowTimeWidth
	 *            window time width
	 * @param yMin
	 *            minimum amplitude value
	 * @param yMax
	 *            maximum amplitude value
	 * @param autoscale
	 *            enable or disable autoscale capability
	 * @param antialias
	 *            enable or disable antialiasis
	 * @param interpolation
	 *            antialiasis quality
	 */
	public RTSWTOscilloChart(Composite parent, int style, double windowTimeWidth, double yMin, double yMax, boolean autoScale, int antialias, int interpolation) {
		this(parent, style, windowTimeWidth, yMin, yMax, autoScale);
		setAntialias(antialias);
		setInterpolation(interpolation);
	}

	/**
	 * Add a new serie to the chart specifying its ID and colour.
	 * 
	 * @param id
	 *            a unique serie identifier
	 * @param serieColor
	 *            the colour of the serie
	 * @return the new created serie {@link RTSWTOscilloSerie}
	 */
	public RTSWTOscilloSerie createSerie(String id, Color serieColor) {
		return serieFactory(id, serieColor, SWT.LINE_SOLID, 1);
	}

	/**
	 * Add a new serie to the chart specifying its ID, line colour and style.
	 * 
	 * @param id
	 *            a unique serie identifier
	 * @param serieColor
	 *            the colour of the serie
	 * @param serieStyle
	 *            the style of the serie (dot, dash dot etc.)
	 * @return the new created serie {@link RTSWTOscilloSerie}
	 */
	public RTSWTOscilloSerie createSerie(String id, Color serieColor, int serieStyle) {
		return serieFactory(id, serieColor, serieStyle, 1);
	}

	/**
	 * Add a new serie to the chart specifying its ID, line colour, style and
	 * width.
	 * 
	 * @param id
	 *            a unique serie identifier
	 * @param serieColor
	 *            the colour of the serie
	 * @param serieStyle
	 *            the style of the serie (dot, dash dot etc.)
	 * @param serieWidth
	 *            the line width of the serie
	 * @return the new created serie {@link RTSWTOscilloSerie}
	 */
	public RTSWTOscilloSerie createSerie(String id, Color serieColor, int serieStyle, int serieWidth) {
		return serieFactory(id, serieColor, serieStyle, serieWidth);
	}

	/**
	 * Factory method to add a serie to the chart specifying its ID, line
	 * colour, style and width.
	 * 
	 * @param id
	 *            a unique serie identifier
	 * @param serieColor
	 *            the colour of the serie
	 * @param serieStyle
	 *            the style of the serie (dot, dash dot etc.)
	 * @param serieWidth
	 *            the line width of the serie
	 * @return the new created serie {@link RTSWTOscilloSerie}
	 */
	private RTSWTOscilloSerie serieFactory(String id, Color serieColor, int serieStyle, int serieWidth) {
		if (series.get(id) != null)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " : A serie with this ID already exists !");
		RTSWTOscilloSerie serie = new RTSWTOscilloSerie(this, id, serieColor, serieStyle, serieWidth);
		series.put(id, serie);
		return serie;
	}

	/**
	 * Return the time step (time elapsed between two pixels)
	 * 
	 * @return the time step
	 */
	@Override
	protected double getDx() {
		return windowTimeWidth / (chartArea.getClientArea().width - getLeftAxisWidth());
	}

	/**
	 * Called from series to ask an update of the chart image. If
	 * {@link #waitForAllSeriesToRedraw} is true, drawing will wait all series
	 * to have new values to update chart image. When drawing is allowed, this
	 * method perform an autoscale if necessary, and then draw series, grids and
	 * update axis and legend.
	 */
	@Override
	protected void render() {
		if (chartArea.isDisposed()) return;
		RTSWTSerie[] series = getSeries();
		boolean redraw = true;
		if (waitForAllSeriesToRedraw)
			for (int i = 0; i < series.length; i++)
				redraw = redraw && series[i].getModified();
		if (redraw) {
			if (isAutoScale()) {
				double newYMin = getAbsoluteyMinValue();
				double newYMax = getAbsoluteyMaxValue();
				if (newYMin != getyMin() || newYMax != getyMax()) {
					if (newYMin == newYMax) {
						newYMin = newYMin - Double.MIN_VALUE;
						newYMax = newYMax + Double.MIN_VALUE;
					}
					setyMax(newYMax);
					setyMin(newYMin);
				}
			}

			long t = System.nanoTime();
			if (!glInitialized) glInit();
			
			chartArea.setCurrent();
			chartAreaGLContext.makeCurrent();
			chartAreaGLContext.getGL().getGL2().glClearColor(backGroundColor.getRed() / 255f, backGroundColor.getGreen() / 255f, backGroundColor.getBlue() / 255f, 1f);
			chartAreaGLContext.getGL().getGL2().glClear(GL2.GL_COLOR_BUFFER_BIT);
			
			drawLeftAxis();
			if (showLegend) drawLegend();
			drawSeries();
			if (showGrid) drawGrids();
			drawBottomAxis();
			
			chartArea.swapBuffers();
			chartAreaGLContext.release();
			
			drawTime.add(System.nanoTime() - t);
		}
	}

	/**
	 * Draw bottom axis
	 */
	@Override
	protected void drawBottomAxis() {
		chartAreaGLContext.getGL().getGL2().glColor3f(fontColor.getRed() / 255, fontColor.getGreen() / 255, fontColor.getBlue() / 255);
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int position = verticalGridLinesPositions.get(i).intValue();
			double value = (i + 1) * (windowTimeWidth) / (verticalGridLinesPositions.size() + 1);
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			int valueStringLength = glut.glutBitmapLength(getFontNumber(), valueString);
			float xPosition = position - valueStringLength / 2;
			float yPostion = getHeight() - getBottomAxisHeight() / 2 + getFontHeight() / 2 - ((showLegend && legendPosition == SWT.TOP) ? 0 : showLegend ? getLegendHeight() : 0);
			chartAreaGLContext.getGL().getGL2().glRasterPos3f(xPosition, yPostion, 0);
			glut.glutBitmapString(getFontNumber(), valueString);
		}
	}

	/**
	 * Draw series using line strip mode.
	 */
	@Override
	protected void drawSeries() {
		if (chartArea.isDisposed())
			return;
		RTSWTSerie[] series = getSeries();
		for (int i = 0; i < series.length; i++) {
			RTSWTSerie serie = series[i];
			chartAreaGLContext.getGL().getGL2().glColor3f(serie.getLineColor().getRed() / 255.0f, serie.getLineColor().getGreen() / 255.0f, serie.getLineColor().getBlue() / 255.0f);
			chartAreaGLContext.getGL().getGL2().glLineWidth(serie.getLineWidth());
			if (serie.getLineStyle() == SWT.LINE_SOLID)
				chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0xFFFF);
			if (serie.getLineStyle() == SWT.LINE_DOT)
				chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x0101);
			if (serie.getLineStyle() == SWT.LINE_DASH)
				chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x00FF);
			if (serie.getLineStyle() == SWT.LINE_DASHDOT)
				chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x1C47);
			if (serie.getLineStyle() == SWT.LINE_DASHDOTDOT)
				chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x333F);
			int[] points = ((RTSWTOscilloSerie)serie).getPointsArrayToDraw();
			int currentPostion = ((RTSWTOscilloSerie)serie).getCurrentIndex();
			if (currentPostion == -1)
				continue;
			chartAreaGLContext.getGL().getGL2().glBegin(GL2.GL_LINE_STRIP);
			int D = showLegend && legendPosition == SWT.TOP ? getLegendHeight() : 0;
			for (int j = 0; j < 2 * (currentPostion + 1); j += 2)
				chartAreaGLContext.getGL().getGL2().glVertex2i(points[j] + getLeftAxisWidth() + 1, D + points[j + 1]);
			chartAreaGLContext.getGL().getGL2().glEnd();
			chartAreaGLContext.getGL().getGL2().glBegin(GL2.GL_LINE_STRIP);
			for (int j = 2 * (currentPostion + 1); j < points.length; j += 2)
				chartAreaGLContext.getGL().getGL2().glVertex2i(points[j] + getLeftAxisWidth() + 1, D + points[j + 1]);
			chartAreaGLContext.getGL().getGL2().glEnd();
			serie.setModified(false);
		}
	}

}
