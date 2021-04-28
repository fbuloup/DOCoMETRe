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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.gl2.GLUT;

public abstract class RTSWTChart extends Composite implements ControlListener {
	
	/**
	 * Handler for legend position menu : top or bottom
	 */
	private final class LegendPositionHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			if (((MenuItem) e.widget) == legendPositionsBottomMenuItem) {
				boolean bottomChecked = ((MenuItem) e.widget).getSelection();
				if (!bottomChecked) {
					legendPositionsTopMenuItem.setSelection(true);
					RTSWTChart.this.setLegendPosition(SWT.TOP);
				} else
					RTSWTChart.this.setLegendPosition(SWT.BOTTOM);
			}
			if (((MenuItem) e.widget) == legendPositionsTopMenuItem) {
				boolean topChecked = ((MenuItem) e.widget).getSelection();
				if (!topChecked) {
					legendPositionsBottomMenuItem.setSelection(true);
					RTSWTChart.this.setLegendPosition(SWT.BOTTOM);
				} else
					RTSWTChart.this.setLegendPosition(SWT.TOP);
			}
		}
	}

	/**
	 * Handler to update menu items when legend menu is shown
	 */
	private final class MenuListenerHandler implements MenuListener {
		public void menuHidden(MenuEvent e) {
		}

		public void menuShown(MenuEvent e) {
			showLegendMenuItem.setSelection(showLegend);
			legendPositionsBottomMenuItem.setSelection(legendPosition == SWT.BOTTOM);
			legendPositionsTopMenuItem.setSelection(legendPosition == SWT.TOP);
			autoScaleMenu.setSelection(autoScale);
		}
	}

	/**
	 * Handler to update chart when legend visibility is toggled
	 */
	private final class ShowLegendHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			RTSWTChart.this.setLegendVisibility(((MenuItem) e.widget).getSelection());
		}
	}
	
	/**
	 * Handler to update chart when legend visibility is toggled
	 */
	private final class AutoScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			RTSWTChart.this.setAutoScale(((MenuItem) e.widget).getSelection());
		}
	}
	
	/**
	 * Hashmap that contains series
	 */
	protected HashMap<String, RTSWTSerie> series = new HashMap<String, RTSWTSerie>(0);
	/**
	 * Draw times array to compute mean draw time
	 */
	protected ArrayList<Long> drawTime = new ArrayList<Long>(0);
	/**
	 * Glut instance to render text
	 */
	protected GLUT glut;
	/**
	 * Decimal formatter to display axis scales
	 */
	protected DecimalFormat decimalFormatter = new DecimalFormat("0.###E0");
	/**
	 * Decimal formatter to display current values
	 */
	protected DecimalFormat decimalFormatterCurrentValues = new DecimalFormat("#0.000000");
	/***
	 * Width in pixels of left axis as the length of the biggest displayed
	 * number
	 */
	protected int leftAxisWidth = 40;
	/**
	 * Background colour
	 */
	protected Color backGroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	/**
	 * OpenGL profile
	 */
	protected GLProfile glProfile;
	/**
	 * This is the chart area composite that support OpenGL context
	 */
	protected GLCanvas chartArea;
	/**
	 * Default is autoscale off
	 */
	protected boolean autoScale = false;
	/**
	 * Default is antialiasis off
	 * SWT.DEFAULT, SWT.ON, SWT.OFF
	 */
	protected int antialias = SWT.OFF;
	/**
	 * Default is interpolation default
	 * SWT.DEFAULT, SWT.NONE, SWT.LOW or SWT.HIGH.
	 */
	protected int interpolation = SWT.DEFAULT;
	/**
	 * Chart waits until <b>all</b> series have fresh values to update display.
	 * This is default behaviour.
	 */
	protected boolean waitForAllSeriesToRedraw = true;
	/**
	 * Default is grid lines visible
	 */
	protected boolean showGrid = true;
	/**
	 * Grid Lines style
	 */
	protected int gridLinesStyle = SWT.LINE_DASH;
	/**
	 * Grid Lines width
	 */
	protected int gridLinesWidth = 1;
	/**
	 * Grid Lines colour
	 */
	protected Color gridLinesColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	/**
	 * An array that contains vertical grids lines positions in pixels
	 */
	protected ArrayList<Integer> verticalGridLinesPositions = new ArrayList<Integer>(0);
	/**
	 * An array that contains horizontal grids lines positions in pixels
	 */
	protected ArrayList<Integer> horizontalGridLinesPositions = new ArrayList<Integer>(0);
	/**
	 * Default is legend visible
	 */
	protected boolean showLegend = true;
	/**
	 * Default is legend visible on top
	 */
	protected int legendPosition = SWT.TOP;
	/**
	 * A menu item in chart area contextual menu to toggle legend visibility
	 */
	protected MenuItem showLegendMenuItem;
	/**
	 * A menu item in chart area contextual menu to move legend top
	 */
	protected MenuItem legendPositionsTopMenuItem;
	/**
	 * A menu item in chart area contextual menu move legend bottom
	 */
	protected MenuItem legendPositionsBottomMenuItem;
	/**
	 * Auto scale menu item
	 */
	protected MenuItem autoScaleMenu;
	/**
	 * Tells if GL has been initialized
	 */
	protected boolean glInitialized;
	/**
	 * Chart area Graphical Context
	 */
	protected GLContext chartAreaGLContext;
	/**
	 * The font 
	 */
	protected RTSWTChartFonts font;
	/**
	 * The colour of the font in bottom and left axis
	 */
	protected Color fontColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	/**
	 * Minus ten is default min amplitude value
	 */
	private double yMin = -10;
	/**
	 * Ten is default max amplitude value
	 */
	private double yMax = 10;
	/**
	 * Minus ten is default min amplitude value
	 */
	private double xMin = -10;
	/**
	 * Ten is default max amplitude value
	 */
	private double xMax = 10;

	public RTSWTChart(Composite parent, int style, RTSWTChartFonts font) {
		super(parent, style);
		this.font = font;
		setLayout(new FillLayout());
		GLProfile.initSingleton();
		glProfile = GLProfile.get(GLProfile.GL2ES2);// GLProfile.getMinimum(true);
		glut = new GLUT();
		leftAxisWidth = glut.glutBitmapLength(getFontNumber(), "-9." + decimalFormatter.getDecimalFormatSymbols().getDecimalSeparator() + "999E-999");
		GLData glDataChartArea = new GLData();
		glDataChartArea.doubleBuffer = true;
		chartArea = new GLCanvas(this, SWT.NONE | SWT.NO_BACKGROUND, glDataChartArea);
		chartArea.addControlListener(this);
		Menu mainMenu = new Menu(this);
		mainMenu.addMenuListener(new MenuListenerHandler());
		showLegendMenuItem = new MenuItem(mainMenu, SWT.CHECK);
		showLegendMenuItem.setText("Show legend");
		showLegendMenuItem.addSelectionListener(new ShowLegendHandler());
		MenuItem legendPositionsMenuItem = new MenuItem(mainMenu, SWT.CASCADE);
		legendPositionsMenuItem.setText("Set legend position");
		Menu legendPositionsMenu = new Menu(legendPositionsMenuItem);
		legendPositionsTopMenuItem = new MenuItem(legendPositionsMenu, SWT.CHECK);
		legendPositionsTopMenuItem.setText("Top");
		legendPositionsBottomMenuItem = new MenuItem(legendPositionsMenu, SWT.CHECK);
		legendPositionsBottomMenuItem.setText("Bottom");
		LegendPositionHandler legendPositionHandler = new LegendPositionHandler();
		legendPositionsTopMenuItem.addSelectionListener(legendPositionHandler);
		legendPositionsBottomMenuItem.addSelectionListener(legendPositionHandler);
		legendPositionsMenuItem.setMenu(legendPositionsMenu);
		autoScaleMenu = new MenuItem(mainMenu, SWT.CHECK);
		autoScaleMenu.setText("Autoscale");
		autoScaleMenu.addSelectionListener(new AutoScaleHandler());
		chartArea.setMenu(mainMenu);
	}
	
	/**
	 * Initialize Graphic library
	 */
	protected void glInit() {
		chartArea.setCurrent();
		if (chartAreaGLContext == null) chartAreaGLContext = GLDrawableFactory.getFactory(glProfile).createExternalGLContext();
		chartAreaGLContext.makeCurrent();
		chartAreaGLContext.getGL().getGL2().glViewport(0, 0, getWidth(), getHeight());
		chartAreaGLContext.getGL().getGL2().glMatrixMode(GL2.GL_PROJECTION);
		chartAreaGLContext.getGL().getGL2().glLoadIdentity();
		chartAreaGLContext.getGL().getGL2().glOrtho(0, getWidth(), getHeight(), 0, 0, -1);
		chartAreaGLContext.getGL().getGL2().glDisable(GL2.GL_DEPTH_TEST);
		chartAreaGLContext.getGL().getGL2().glEnable(GL2.GL_LINE_STIPPLE);
		if (getAntialias() == SWT.ON) {
			chartAreaGLContext.getGL().getGL2().glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			chartAreaGLContext.getGL().getGL2().glEnable(GL2.GL_BLEND);
			chartAreaGLContext.getGL().getGL2().glEnable(GL2.GL_LINE_SMOOTH);
		}
		chartAreaGLContext.getGL().getGL2().glMatrixMode(GL2.GL_MODELVIEW);
		chartAreaGLContext.getGL().getGL2().glLoadIdentity();
		chartAreaGLContext.release();
		glInitialized = true;
	}

	@Override
	public void controlMoved(ControlEvent e) {

	}

	/**
	 * If chart is resized, then reset chart image and series histories.
	 */
	@Override
	public void controlResized(ControlEvent e) {
		resetSeries();
		if(glInitialized) {
			chartArea.setCurrent();
			chartAreaGLContext.makeCurrent();
			chartAreaGLContext.getGL().getGL2().glViewport(0, 0, getWidth(), getHeight());
			chartAreaGLContext.getGL().getGL2().glMatrixMode(GL2.GL_PROJECTION);
			chartAreaGLContext.getGL().getGL2().glLoadIdentity();
			chartAreaGLContext.getGL().getGL2().glOrtho(0, getWidth(), getHeight(), 0, 0, -1);
			chartAreaGLContext.getGL().getGL2().glDisable(GL2.GL_DEPTH_TEST);
			chartAreaGLContext.getGL().getGL2().glEnable(GL2.GL_LINE_STIPPLE);
			chartAreaGLContext.getGL().getGL2().glMatrixMode(GL2.GL_MODELVIEW);
			chartAreaGLContext.getGL().getGL2().glLoadIdentity();
			chartAreaGLContext.getGL().getGL2().glClearColor(backGroundColor.getRed() / 255f, backGroundColor.getGreen() / 255f, backGroundColor.getBlue() / 255f, 1f);
			chartAreaGLContext.getGL().getGL2().glClear(GL2.GL_COLOR_BUFFER_BIT);
			chartArea.swapBuffers();
			chartAreaGLContext.release();
		}
	}
	
	/**
	 * Return the all series contained in this chart in an array.
	 * 
	 * @return an array of #RTSWTSerie
	 */
	protected RTSWTSerie[] getSeries() {
		return series.values().toArray(new RTSWTSerie[series.size()]);
	}

	
	/**
	 * Toggle legend visibility on or off.
	 * 
	 * @param showLegend
	 *            true of false
	 */
	public void setLegendVisibility(boolean showLegend) {
		this.showLegend = showLegend;
	}
	
	/**
	 * Return legend visibility 
	 * 
	 *  @return legend visibility (true, false)
	 */
	public boolean isLegendVisible() {
		return showLegend;
	}

	/**
	 * Set legend position
	 * 
	 * @param legendPosition
	 *            <b>SWT.TOP</b> or <b>SWT.BOTTOM</b>
	 */
	public void setLegendPosition(int legendPosition) {
		if (legendPosition != SWT.TOP && legendPosition != SWT.BOTTOM) return;
		this.legendPosition = legendPosition;
	}
	
	/**
	 * Get legend position
	 * 
	 * @return legendPosition
	 *            <b>SWT.TOP</b> or <b>SWT.BOTTOM</b>
	 */
	public int getLegendPosition() {
		return legendPosition;
	}
	
	/**
	 * Return antialiasis value
	 * 
	 * @return the antialiasis value
	 */
	public int getAntialias() {
		return antialias;
	}

	/**
	 * Return interpolation value
	 * 
	 * @return the interpolation value
	 */

	public int getInterpolation() {
		return interpolation;
	}

	/**
	 * Toggle antialiasis On / Off
	 * 
	 * @param antialias
	 *            SWT.ON or SWT.OFF
	 */
	public void setAntialias(int antialias) {
		this.antialias = antialias;
	}

	/**
	 * Set interpolation level when antialiasis is on
	 * 
	 * @param interpolation
	 *            <b>SWT.DEFAULT</b>, <b>SWT.NONE</b>, <b>SWT.LOW</b> or
	 *            <b>SWT.HIGH</b>.
	 */
	public void setInterpolation(int interpolation) {
		this.interpolation = interpolation;
	}

	/**
	 * Return autoscale value
	 * 
	 * @return the autoscale value
	 */
	public boolean isAutoScale() {
		return autoScale;
	}

	/**
	 * Set the autoscale value
	 * 
	 * @param autoScale
	 *            true or false
	 */
	public void setAutoScale(boolean autoScale) {
		this.autoScale = autoScale;
	}
	
	/**
	 * If this field is set to true, the chart will wait for all series to have
	 * new values before starting a drawing. If set to false, chart will be
	 * redrawn each time a serie receive new values.
	 * 
	 * @param waitForAllSeriesToRedraw
	 *            true or false
	 */
	public void setWaitForAllSeriesToRedraw(boolean waitForAllSeriesToRedraw) {
		this.waitForAllSeriesToRedraw = waitForAllSeriesToRedraw;
	}

	/**
	 * Set grid lines style
	 * 
	 * @param gridLinesStyle
	 *            The style of the grid lines
	 */
	public void setGridLinesStyle(int gridLinesStyle) {
		this.gridLinesStyle = gridLinesStyle;
	}

	/**
	 * Set grid lines width
	 * 
	 * @param gridLinesWidth
	 *            The width of the grid lines
	 */
	public void setGridLinesWidth(int gridLinesWidth) {
		this.gridLinesWidth = gridLinesWidth;
	}

	/**
	 * Set grid lines colour
	 * 
	 * @param gridLinesColor
	 *            The colour of the grid lines
	 */
	public void setGridLinesColor(Color gridLinesColor) {
		this.gridLinesColor = gridLinesColor;
	}
	
	/**
	 * Toggle grid lines visibility on or off.
	 * 
	 * @param showGrid
	 *            true of false
	 */
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}
	
	/**
	 * Return the minimum amplitude value.
	 * 
	 * @return minimum amplitude value
	 */
	protected double getyMin() {
		return yMin;
	}

	/**
	 * Return the maximum amplitude value.
	 * 
	 * @return maximum amplitude value
	 */
	protected double getyMax() {
		return yMax;
	}
	
	/**
	 * Set the minimum amplitude value.
	 */
	protected void setyMin(double yMin) {
		this.yMin = yMin;
	}

	/**
	 * Set the maximum amplitude value.
	 */
	protected void setyMax(double yMax) {
		this.yMax = yMax;
	}
	
	/**
	 * Return the minimum amplitude value.
	 * 
	 * @return minimum amplitude value
	 */
	protected double getxMin() {
		return xMin;
	}

	/**
	 * Return the maximum amplitude value.
	 * 
	 * @return maximum amplitude value
	 */
	protected double getxMax() {
		return xMax;
	}
	
	/**
	 * Set the minimum amplitude value.
	 */
	protected void setxMin(double xMin) {
		this.xMin = xMin;
	}

	/**
	 * Set the maximum amplitude value.
	 */
	protected void setxMax(double xMax) {
		this.xMax = xMax;
	}
	
	/**
	 * Return the font number used
	 * 
	 * @return
	 */
	protected int getFontNumber() {
		return font.getValue();
	}
	
	/**
	 * Return the font height
	 * 
	 * @return
	 */
	protected int getFontHeight() {
		int fontHeight;
		switch (getFontNumber()) {
		case GLUT.BITMAP_8_BY_13:
			fontHeight = 13;
			break;
		case GLUT.BITMAP_9_BY_15:
			fontHeight = 15;
			break;
		case GLUT.BITMAP_HELVETICA_10:
			fontHeight = 10;
			break;
		case GLUT.BITMAP_HELVETICA_12:
			fontHeight = 12;
			break;
		case GLUT.BITMAP_HELVETICA_18:
			fontHeight = 18;
			break;
		case GLUT.BITMAP_TIMES_ROMAN_10:
			fontHeight = 10;
			break;
		case GLUT.BITMAP_TIMES_ROMAN_24:
			fontHeight = 24;
			break;
		default:
			fontHeight = 13;
			break;
		}
		return fontHeight;
	}
	
	/**
	 * Set the background colour of the chart
	 * 
	 * @param color
	 */
	public void setBackGroundColor(Color color) {
		this.backGroundColor = color;
		setBackground(color);
	}

	/**
	 * Set the font color used in the bottom and left axis of the chart
	 * 
	 * @param color
	 */
	public void setFontColor(Color color) {
		this.fontColor = color;
	}

	/**
	 * Return the legend height
	 * 
	 * @return
	 */
	protected int getLegendHeight() {
		return getFontHeight() * 3 / 2;
	}

	/**
	 * Return the bottom axis height
	 * 
	 * @return
	 */
	protected int getBottomAxisHeight() {
		return getFontHeight() * 3 / 2;
	}
	
	protected int getLeftAxisWidth() {
		return leftAxisWidth;
	}
	
	/**
	 * Return the total chart area height including bottom axis and legend height. 
	 * Notice that it is not the height of the chart {@link RTSWTChart} 
	 * itself (which is return in height field of {@link RTSWTChart#getClientArea()}).
	 * 
	 * @return height in pixels
	 */
	protected int getHeight() {
		return chartArea.getClientArea().height;
	}

	/**
	 * Return the chart area width including left axis width. Notice that it is not the width of the chart
	 * {@link RTSWTChart} itself (which is return in width field of
	 * {@link RTSWTChart#getClientArea()})
	 * 
	 * @return height in pixels
	 */
	protected int getWidth() {
		return chartArea.getClientArea().width;
	}
	
	/**
	 * Reset all series chart. Called when chart is resized.
	 */
	protected void resetSeries() {
		RTSWTSerie[] series = getSeries();
		for (int i = 0; i < series.length; i++)
			series[i].reset();
	}
	
	/**
	 * Draw left axis
	 */
	protected void drawLeftAxis() {
		chartAreaGLContext.getGL().getGL2().glColor3f(fontColor.getRed() / 255, fontColor.getGreen() / 255, fontColor.getBlue() / 255);
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			int position = horizontalGridLinesPositions.get(i).intValue();
			double value = getyMax() - (i + 1) * (getyMax() - getyMin()) / (horizontalGridLinesPositions.size() + 1);
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			int valueStringLength = glut.glutBitmapLength(getFontNumber(), valueString);
			float xPosition = getLeftAxisWidth() / 2 - valueStringLength / 2;
			float yPostion = position + getFontHeight() / 2;
			chartAreaGLContext.getGL().getGL2().glRasterPos3f(xPosition, yPostion, 0);
			glut.glutBitmapString(getFontNumber(), valueString);
		}
	}

	

	/**
	 * Draw the chart legend
	 */
	protected void drawLegend() {
		if (getSeries().length == 0) return;
		RTSWTSerie[] seriesList = getSeries();
		int totalWidth = 0;
		int legendLineWidth = 40;
		for (int i = 0; i < seriesList.length; i++) {
			totalWidth += legendLineWidth + 5;
			totalWidth += glut.glutBitmapLength(getFontNumber(), seriesList[i].getId());
			totalWidth += glut.glutBitmapLength(getFontNumber(), "  ");
		}
		totalWidth -= glut.glutBitmapLength(getFontNumber(), "  ");
		int position = 0;
		for (int i = 0; i < seriesList.length; i++) {
			RTSWTSerie serie = seriesList[i];
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
			int xPosition = (getWidth() - getLeftAxisWidth()) / 2 - totalWidth / 2 + position + getLeftAxisWidth();
			int yPosition = legendPosition == SWT.BOTTOM ? getHeight() - getLegendHeight() / 2 + 3: getLegendHeight() / 2 + 3;
			chartAreaGLContext.getGL().getGL2().glVertex2i(xPosition, yPosition);
			chartAreaGLContext.getGL().getGL2().glVertex2i(xPosition + legendLineWidth, yPosition);
			chartAreaGLContext.getGL().getGL2().glEnd();
			chartAreaGLContext.getGL().getGL2().glRasterPos3f(xPosition + legendLineWidth + 5, yPosition, 0);
			glut.glutBitmapString(getFontNumber(), serie.getId());
			position += glut.glutBitmapLength(getFontNumber(), serie.getId());
			position += glut.glutBitmapLength(getFontNumber(), "  ");
			position += legendLineWidth + 5;
		}
	}
	
	/**
	 * Draw grids on the chart image
	 */
	protected void drawGrids() {
		if (chartArea.isDisposed())
			return;
		chartAreaGLContext.getGL().getGL2().glColor3f(gridLinesColor.getRed() / 255.0f, gridLinesColor.getGreen() / 255.0f, gridLinesColor.getBlue() / 255.0f);
		chartAreaGLContext.getGL().getGL2().glLineWidth(gridLinesWidth);
		if (gridLinesStyle == SWT.LINE_SOLID)
			chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0xFFFF);
		if (gridLinesStyle == SWT.LINE_DOT)
			chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x0101);
		if (gridLinesStyle == SWT.LINE_DASH)
			chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x00FF);
		if (gridLinesStyle == SWT.LINE_DASHDOT)
			chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x1C47);
		if (gridLinesStyle == SWT.LINE_DASHDOTDOT)
			chartAreaGLContext.getGL().getGL2().glLineStipple(1, (short) 0x333F);
		// Draw vertical grid
		verticalGridLinesPositions.clear();
		verticalGridLinesPositions = new ArrayList<Integer>(0);
		verticalGridLinesPositions.add(getLeftAxisWidth());
		verticalGridLinesPositions.add(getWidth() - 1);
		createGridLinesPositions(verticalGridLinesPositions, verticalGridLinesPositions.get(0), verticalGridLinesPositions.get(1), getLeftAxisWidth());
		Collections.sort(verticalGridLinesPositions);
		verticalGridLinesPositions.remove(0);
		verticalGridLinesPositions.remove(verticalGridLinesPositions.size() - 1);
		int yPositionMin = (showLegend && legendPosition == SWT.BOTTOM) ? 0 : (showLegend ? getLegendHeight() : 0);
		int yPositionMax = (showLegend && legendPosition == SWT.BOTTOM) ? getHeight() - getBottomAxisHeight() - getLegendHeight() : getHeight() - getBottomAxisHeight();
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			chartAreaGLContext.getGL().getGL2().glBegin(GL2.GL_LINE_STRIP);
			chartAreaGLContext.getGL().getGL2().glVertex2i(verticalGridLinesPositions.get(i), yPositionMin);
			chartAreaGLContext.getGL().getGL2().glVertex2i(verticalGridLinesPositions.get(i), yPositionMax);
			chartAreaGLContext.getGL().getGL2().glEnd();
		}
		// Draw horizontal grid
		horizontalGridLinesPositions.clear();
		horizontalGridLinesPositions.add(yPositionMin);
		horizontalGridLinesPositions.add(yPositionMax - 1);
		createGridLinesPositions(horizontalGridLinesPositions, horizontalGridLinesPositions.get(0), horizontalGridLinesPositions.get(1), 2*getFontHeight());
		Collections.sort(horizontalGridLinesPositions);
		horizontalGridLinesPositions.remove(0);
		horizontalGridLinesPositions.remove(horizontalGridLinesPositions.size() - 1);
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			chartAreaGLContext.getGL().getGL2().glBegin(GL2.GL_LINE_STRIP);
			chartAreaGLContext.getGL().getGL2().glVertex2i(getLeftAxisWidth(), horizontalGridLinesPositions.get(i));
			chartAreaGLContext.getGL().getGL2().glVertex2i(getWidth(), horizontalGridLinesPositions.get(i));
			chartAreaGLContext.getGL().getGL2().glEnd();
		}
	}
	
	private void createGridLinesPositions(ArrayList<Integer> gridLines, int from, int to, int delta) {
		if(to - from > 2*delta) {
			int value = (int)Math.rint((double)(from + to) / 2.0);
			gridLines.add(value);
			createGridLinesPositions(gridLines, from, value, delta);
			createGridLinesPositions(gridLines, value, to, delta);
		}
		
	}
	
	/**
	 * Mean draw time computation
	 * @return mean draw time
	 */
	public String getMeanDrawTime() {
		Long[] drawTimes = drawTime.toArray(new Long[drawTime.size()]);
		Long sum = Long.valueOf(0);
	    for (int i = 0; i < drawTimes.length; i++) {
	        sum += drawTimes[i];
	    }
	    double finalValue = sum / 1000000f;
	    return Double.toString(finalValue) + "ms for " + drawTimes.length + " graph update. Mean time per graph update : " + (finalValue/(drawTimes.length)) + "ms";
	}
	
	/**
	 * Return the time step (time elapsed between two pixels)
	 * 
	 * @return the time step
	 */
	protected double getDx() {
		double D = chartArea.getClientArea().width - getLeftAxisWidth();
		return (getxMax() - getxMin()) / D;
	}

	/**
	 * Return the amplitude step (amplitude step between two pixels)
	 * 
	 * @return the amplitude step
	 */
	protected double getDy() {
		double D = chartArea.getClientArea().height - getBottomAxisHeight() - (showLegend ? getLegendHeight() : 0);
		return (getyMax() - getyMin()) / D;
	}
	
	/**
	 * Return the absolute minimum value of the chart, the min value of the all
	 * series within the window time width.
	 * 
	 * @return The absolute minimum value
	 */
	protected double getAbsoluteyMinValue() {
		RTSWTSerie[] series = getSeries();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < series.length; i++) {
			min = Math.min(series[i].getyMinHeight(), min);
		}
		return min;
	}

	/**
	 * Return the absolute maximum value of the chart, the max value of the all
	 * series within the window time width.
	 * 
	 * @return The absolute maximium value
	 */
	protected double getAbsoluteyMaxValue() {
		RTSWTSerie[] series = getSeries();
		double max = Double.MIN_VALUE;
		for (int i = 0; i < series.length; i++) {
			max = Math.max(series[i].getyMaxHeight(), max);
		}
		return max;
	}
	
	protected abstract void render();
	protected abstract void drawBottomAxis();
	protected abstract void drawSeries();
	
}
