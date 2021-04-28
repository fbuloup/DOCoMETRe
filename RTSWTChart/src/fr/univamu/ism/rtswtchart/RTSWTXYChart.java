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

public class RTSWTXYChart extends RTSWTChart {

	/**
	 * 
	 * @param parent
	 * @param style
	 */
	public RTSWTXYChart(Composite parent, int style, RTSWTChartFonts font) {
		super(parent, style, font);
	}
	
	/**
	 * 
	 * @param parent
	 * @param style
	 * @param yMin
	 * @param yMax
	 * @param xMin
	 * @param xMax
	 */
	public RTSWTXYChart(Composite parent, int style, RTSWTChartFonts font, double yMin, double yMax, double xMin, double xMax) {
		this(parent, style, font);
		setyMax(yMax);
		setyMin(yMin);
		setxMax(xMax);
		setxMin(xMin);
	}
	
	/**
	 * 
	 * @param parent
	 * @param style
	 * @param yMin
	 * @param yMax
	 * @param xMin
	 * @param xMax
	 * @param autoscale
	 */
	public RTSWTXYChart(Composite parent, int style, RTSWTChartFonts font, double yMin, double yMax, double xMin, double xMax, boolean autoscale) {
		this(parent, style, font, yMin, yMax, xMin, xMax);
		setAutoScale(autoscale);
	}
	
	/**
	 * 
	 * @param parent
	 * @param style
	 * @param yMin
	 * @param yMax
	 * @param xMin
	 * @param xMax
	 * @param autoscale
	 * @param antialias
	 * @param interpolation
	 */
	public RTSWTXYChart(Composite parent, int style, RTSWTChartFonts font, double yMin, double yMax, double xMin, double xMax, boolean autoscale, int antialias, int interpolation) {
		this(parent, style, font, yMin, yMax, xMin, xMax, autoscale);
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
	 * @return the new created serie {@link RTSWTXYSerie}
	 */
	public RTSWTXYSerie createSerie(String id, Color serieColor) {
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
	 * @return the new created serie {@link RTSWTXYSerie}
	 */
	public RTSWTXYSerie createSerie(String id, Color serieColor, int serieStyle) {
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
	 * @return the new created serie {@link RTSWTXYSerie}
	 */
	public RTSWTXYSerie createSerie(String id, Color serieColor, int serieStyle, int serieWidth) {
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
	 * @return the new created serie {@link RTSWTXYSerie}
	 */
	private RTSWTXYSerie serieFactory(String id, Color serieColor, int serieStyle, int serieWidth) {
		if (series.get(id) != null)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " : A serie with this ID already exists !");
		RTSWTXYSerie serie = new RTSWTXYSerie(this, id, serieColor, serieStyle, serieWidth);
		series.put(id, serie);
		return serie;
	}
	
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
				
				double newXMin = getAbsolutexMinValue();
				double newXMax = getAbsolutexMaxValue();
				if (newXMin != getxMin() || newXMax != getxMax()) {
					if (newXMin == newXMax) {
						newXMin = newXMin - Double.MIN_VALUE;
						newXMax = newXMax + Double.MIN_VALUE;
					}
					setxMax(newXMax);
					setxMin(newXMin);
				}
				
			}
			
			long t = System.nanoTime();
			if (!glInitialized) glInit();
			
			chartArea.setCurrent();
			chartAreaGLContext.makeCurrent();
			chartAreaGLContext.getGL().getGL2().glClearColor(backGroundColor.getRed() / 255f, backGroundColor.getGreen() / 255f, backGroundColor.getBlue() / 255f, 1f);
			chartAreaGLContext.getGL().getGL2().glClear(GL2.GL_COLOR_BUFFER_BIT);
			
			if (showGrid) drawGrids();
			
			drawLeftAxis();
			if (showLegend) drawLegend();
			drawSeries();
			
			drawBottomAxis();
			
			chartArea.swapBuffers();
			chartAreaGLContext.release();
			
			drawTime.add(System.nanoTime() - t);
		}
		
	}

	@Override
	protected void drawBottomAxis() {
		chartAreaGLContext.getGL().getGL2().glColor3f(fontColor.getRed() / 255, fontColor.getGreen() / 255, fontColor.getBlue() / 255);
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int position = verticalGridLinesPositions.get(i).intValue();
			double value = getxMin() + (i + 1) * (getxMax() - getxMin()) / (verticalGridLinesPositions.size() + 1);
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			int valueStringLength = glut.glutBitmapLength(getFontNumber(), valueString);
			float xPosition = position - valueStringLength / 2;
			float yPostion = getHeight() - getBottomAxisHeight() / 2 + getFontHeight() / 2 - ((showLegend && legendPosition == SWT.TOP) ? 0 : showLegend ? getLegendHeight() : 0);
			chartAreaGLContext.getGL().getGL2().glRasterPos3f(xPosition, yPostion, 0);
			glut.glutBitmapString(getFontNumber(), valueString);
		}
		
	}

	@Override
	protected void drawSeries() {
		if (chartArea.isDisposed())
			return;
		RTSWTSerie[] series = getSeries();
		for (int i = 0; i < series.length; i++) {
			RTSWTXYSerie serie = (RTSWTXYSerie) series[i];
			if(serie.getNumberOfPoints() == 0) continue;
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
			chartAreaGLContext.getGL().getGL2().glBegin(GL2.GL_LINE_STRIP);
			int D = showLegend && legendPosition == SWT.TOP ? getLegendHeight() : 0;
			int[] points = serie.getPointsArrayToDraw();
			
			for (int j = 0; j < points.length ; j += 2) {
				chartAreaGLContext.getGL().getGL2().glVertex2i(points[j] + getLeftAxisWidth() + 1, points[j + 1] + D);
			}
			
			chartAreaGLContext.getGL().getGL2().glEnd();
			serie.setModified(false);
		}
		
	}
	
	/**
	 * Return the absolute minimum value of the chart, the min value of the all
	 * series within the window time width.
	 * 
	 * @return The absolute minimum value
	 */
	protected double getAbsolutexMinValue() {
		RTSWTSerie[] series = getSeries();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < series.length; i++) {
			RTSWTXYSerie serie = (RTSWTXYSerie) series[i];
			min = Math.min(serie.getxMinHeight(), min);
		}
		return min;
	}

	/**
	 * Return the absolute maximum value of the chart, the max value of the all
	 * series within the window time width.
	 * 
	 * @return The absolute maximium value
	 */
	protected double getAbsolutexMaxValue() {
		RTSWTSerie[] series = getSeries();
		double max = Double.MIN_VALUE;
		for (int i = 0; i < series.length; i++) {
			RTSWTXYSerie serie = (RTSWTXYSerie) series[i];
			max = Math.max(serie.getxMaxHeight(), max);
		}
		return max;
	}

}
