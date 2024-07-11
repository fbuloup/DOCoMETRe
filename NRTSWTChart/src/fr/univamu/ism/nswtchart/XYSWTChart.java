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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [01/06/2024]
 ******************************************************************************/
package fr.univamu.ism.nswtchart;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class XYSWTChart extends Canvas implements PaintListener, MouseListener, MouseMoveListener, KeyListener, MouseWheelListener {
	
	private ArrayList<XYSWTSerie> xyswtSeries = new ArrayList<XYSWTSerie>();
	private Window window;
	private int yAxisWidth = 50;
	private int xAxisHeight = 50;
	private Color axisColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
	private int axisLineWidth = 1;
	private boolean showLegend = true;
	private boolean showCursor = false;
	private int legendPosition = SWT.TOP;
	private int legendHeight = 50;
	private boolean showGrid = false;
	private boolean showAxis = true;
	private Font chartFont;
	private ArrayList<Integer> verticalGridLinesPositions = new ArrayList<Integer>(0);
	private ArrayList<Integer> horizontalGridLinesPositions = new ArrayList<Integer>(0);
	private ArrayList<String> verticalGridLinesLabels = new ArrayList<String>(0);
	private ArrayList<String> horizontalGridLinesLabels = new ArrayList<String>(0);
	private DecimalFormat decimalFormatter = new DecimalFormat("0.##E0");
	private int marginAxis = 4;
	private boolean zooming;
	private Rectangle zoomWindow;
	private int selectedSerie = -1;
	private Cursor cursor;
	private Cursor cursorBusy;
	private Point cursorPosition = new Point(0, 0);
	private Point markerPosition;
	private Point previousCursorPosition;
	private Point crossPosition = new Point(0, 0);
	private Image backupImage;
	private Rectangle previousZoomWindow;
	private ArrayList<CursorMarkerListener> cursorMarkerListeners = new ArrayList<CursorMarkerListener>();
	private ContextMenu contextMenu;
	
	
	public XYSWTChart(Composite parent, int style, String fontName, int chartFontStyle, int chartFontHeight) {
		super(parent, style);
		addPaintListener(this);
		addMouseListener(this);
		addMouseMoveListener(this);
		addKeyListener(this);
		addMouseWheelListener(this);
		if(fontName == null || "".equals(fontName)) {	
			FontDescriptor boldDescriptor = FontDescriptor.createFrom(getFont()).setStyle(chartFontStyle).setHeight(chartFontHeight);
			chartFont = boldDescriptor.createFont(getDisplay());
		} else {
			FontData fontData = new FontData(fontName, chartFontHeight, chartFontStyle);
			chartFont = new Font(getDisplay(), fontData);
		}
		setFont(chartFont);
		
		contextMenu = new ContextMenu(this);
		setMenu(contextMenu.getMenu());
		
		cursor = new Cursor(getDisplay(), SWT.CURSOR_CROSS);
		setCursor(cursor);
		
		cursorBusy = new Cursor(getDisplay(), SWT.CURSOR_WAIT);
		
		GC gc = new GC(this);
		xAxisHeight = gc.textExtent("1E10").y;
		gc.dispose();
		
	}
	
	public void addCursorMarkerListener(CursorMarkerListener cursorMarkerListener) {
		if(!cursorMarkerListeners.contains(cursorMarkerListener)) cursorMarkerListeners.add(cursorMarkerListener);
	}
	
	public boolean removeCursorMarkerListener(CursorMarkerListener cursorMarkerListener) {
		return cursorMarkerListeners.remove(cursorMarkerListener);
	}
	
	public void createSerie(double[] xValues, double[] yValues, String id, Color color, int thickness) {
		XYSWTSerie xyswtSerie = new XYSWTSerie(xValues, yValues, this, id, color, thickness);
		xyswtSeries.add(xyswtSerie);
		reset();
	}
	
	public void removeSerie(String id) {
		XYSWTSerie xyswtSerieToRemove = null;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			if(xyswtSerie.getTitle().equals(id)) {
				xyswtSerieToRemove = xyswtSerie;
				break;
			}
		}
		if(xyswtSerieToRemove != null) {
			xyswtSeries.remove(xyswtSerieToRemove);
			redraw();
			update();
		}
	}
	
	public int getSeriesNumber() {
		return xyswtSeries.size();
	}
	
	public String getSeriesTooltip() {
		String toolTip = "";
		for (int i = 0; i < xyswtSeries.size(); i++) {
			if(i == xyswtSeries.size() - 1) toolTip += xyswtSeries.get(i).getTitle();
			else toolTip += xyswtSeries.get(i).getTitle() + "\n";
		}
		return toolTip;
	}

	public void reset() {
		double xMax = Double.NEGATIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
		double xMin = Double.POSITIVE_INFINITY, yMin = Double.POSITIVE_INFINITY;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			xMax = Math.max(xyswtSerie.getxMax(), xMax);
			yMax = Math.max(xyswtSerie.getyMax(), yMax);
			xMin = Math.min(xyswtSerie.getxMin(), xMin);
			yMin = Math.min(xyswtSerie.getyMin(), yMin);
		}
		window = new Window(xMin, xMax, yMin, yMax);
	}
	
	public void resetX() {
		double xMax = Double.NEGATIVE_INFINITY;
		double xMin = Double.POSITIVE_INFINITY;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			xMax = Math.max(xyswtSerie.getxMax(), xMax);
			xMin = Math.min(xyswtSerie.getxMin(), xMin);
		}
		window = new Window(xMin, xMax, window.getYMin(), window.getYMax());
	}
	
	public void resetY() {
		double yMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			yMax = Math.max(xyswtSerie.getyMax(), yMax);
			yMin = Math.min(xyswtSerie.getyMin(), yMin);
		}
		window = new Window(window.getXMin(), window.getXMax(), yMin, yMax);
	}
	
	protected Window getWindow() {
		return window;
	}
	
	protected int xValueToPixel(double x) {
		int xPixel = (int) ((getWidth() - 1 - getYAxisWidth())*(x - getWindow().getXMin())/(getWindow().getXMax() - getWindow().getXMin()));
		return xPixel + getYAxisWidth();
	}
	
	protected int yValueToPixel(double y) {
		int yPixel = (int) ((getHeight() - 1 - getXAxisHeight() - getLegendHeight())*(y - getWindow().getYMin())/(getWindow().getYMax() - getWindow().getYMin()));
		return getHeight() - 1 - yPixel - getXAxisHeight() - (isLegendPositionBottom() ? getLegendHeight() : 0);
	}
	
	protected double xPixelToValue(int xPixel) {
		double x = xPixel * (getWindow().getXMax() - getWindow().getXMin())/(getWidth() - 1 - getYAxisWidth()) + getWindow().getXMin();
		return x;
	}
	
	protected double yPixelToValue(int yPixel) {
		yPixel = -yPixel + (getHeight() - 1 - getXAxisHeight() - getLegendHeight());
		double y = yPixel * (getWindow().getYMax() - getWindow().getYMin()) / (getHeight() - 1 - getXAxisHeight() - getLegendHeight()) + getWindow().getYMin();
		return y;
	}

	@Override
	public void paintControl(PaintEvent e) {

		markerPosition = null;
		cursorPosition = null;
		setCursor(cursorBusy);
		
		drawLegend(e.gc);
		if(showAxis || showGrid) computeGrid(e.gc);
		drawXAxis(e.gc);
		drawYAxis(e.gc);
		drawGrid(e.gc);
		drawSeries(e.gc);
		
		if(backupImage != null && !backupImage.isDisposed()) backupImage.dispose();
		backupImage = new Image(e.display, getBounds());
		try {
			e.gc.copyArea(backupImage, 0, 0);
		} catch (Exception e1) {
		}
		
		if(zooming) drawZoom(e.gc);
		if(showCursor && selectedSerie >= 0) drawCursor(e.gc);

		setCursor(cursor);
	}
	
	private void drawCursor(GC gc) {
		if(cursorPosition == null) return;
		XYSWTSerie xyswtSerie = xyswtSeries.get(selectedSerie);
		gc.setForeground(xyswtSerie.getColor());
		gc.drawLine(cursorPosition.x, 0, cursorPosition.x, getHeight() - 1);
		gc.drawRectangle(new Rectangle(cursorPosition.x - 4, cursorPosition.y - 4, 8, 8));
		if(SWT.getPlatform().equals("cocoa")) redraw(cursorPosition.x - 4, 0, 8, getHeight() - 1, true);
		if(markerPosition != null) {
			gc.drawLine(markerPosition.x, 0, markerPosition.x, getHeight() - 1);
			gc.drawRectangle(new Rectangle(markerPosition.x - 4, markerPosition.y - 4, 8, 8));
			if(SWT.getPlatform().equals("cocoa")) redraw(cursorPosition.x - 4, markerPosition.y - 4, 8, 8, true);
		}
	}

	private void drawZoom(GC gc) {
		gc.setClipping(0, 0, getWidth(), getHeight());
		clearZoom(gc);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		gc.drawRectangle(zoomWindow.x, zoomWindow.y, zoomWindow.width, zoomWindow.height);
		if(SWT.getPlatform().equals("cocoa")) redraw(zoomWindow.x, zoomWindow.y, zoomWindow.width, zoomWindow.height, true);
		previousZoomWindow = zoomWindow;
	}
	
	private void clearZoom(GC gc) {
		if(previousZoomWindow != null) {
			Rectangle orderedWindow = orderWindow(previousZoomWindow);
			int x0 = orderedWindow.x - 200;
			int y0 = orderedWindow.y - 200;
			if(x0 < 0) x0 = 0;
			if(y0 < 0) y0 = 0;
			int w = orderedWindow.width + 400;
			int h = orderedWindow.height + 400;
			if(x0 + w > backupImage.getImageData().width) w = backupImage.getImageData().width - x0;
			if(y0 + h > backupImage.getImageData().height) h = backupImage.getImageData().height - y0;
			gc.drawImage(backupImage, x0, y0, w, h, x0, y0, w, h);
			if(SWT.getPlatform().equals("cocoa")) redraw(x0, y0, w, h, true);
		}
	}
	
	private Rectangle orderWindow(Rectangle window) {
		if(window.width >= 0 && window.height >= 0) return window;
		int x = window.x, y = window.y, w = window.width, h = window.height;
		if(window.width < 0) {
			x = window.x + window.width;
			w = -window.width;
			if(x < 0) {
				w = w + x;
				x = 0;
			}
		}
		if(window.height < 0) {
			y = window.y + window.height;
			h = -window.height;
			if(y < 0) {
				h = h + y;
				y = 0;
			}
		}
		return new Rectangle(x, y, w, h);
	}

	private void computeGrid(GC gc) {
		// Horizontal grids
		yAxisWidth = 0;
		horizontalGridLinesPositions.clear();
		horizontalGridLinesLabels.clear();
		horizontalGridLinesPositions.add(0);
		horizontalGridLinesPositions.add(getHeight() - getLegendHeight() - getXAxisHeight() - 1);
		createGridLinesPositions(horizontalGridLinesPositions, horizontalGridLinesPositions.get(0), horizontalGridLinesPositions.get(1), 100);
		Collections.sort(horizontalGridLinesPositions);
		for (Integer horizontalGridLinesPosition : horizontalGridLinesPositions) {
			int position = horizontalGridLinesPosition.intValue();
			double value = yPixelToValue(position);
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			yAxisWidth = Math.max(yAxisWidth, gc.textExtent(valueString).x);
			horizontalGridLinesLabels.add(valueString);
		}
		// Vertical grids
		xAxisHeight = 0;
		verticalGridLinesPositions.clear();
		verticalGridLinesLabels.clear();
		verticalGridLinesPositions.add(0);
		verticalGridLinesPositions.add(getWidth() - getYAxisWidth() - 1);
		createGridLinesPositions(verticalGridLinesPositions, verticalGridLinesPositions.get(0), verticalGridLinesPositions.get(1), 100);
		Collections.sort(verticalGridLinesPositions);
		for (Integer verticalGridLinesPosition : verticalGridLinesPositions) {
			int position = verticalGridLinesPosition.intValue();
			double value = xPixelToValue(position);
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			xAxisHeight = Math.max(xAxisHeight, gc.textExtent(valueString).y);
			verticalGridLinesLabels.add(valueString);
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

	private void drawGrid(GC gc) {
		if(!showGrid) return;
		gc.setForeground(axisColor);
		gc.setLineStyle(SWT.LINE_DASH);
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int xPosition = verticalGridLinesPositions.get(i).intValue() + getYAxisWidth();
			int yPositionMin = isLegendPositionBottom() ? 0 : getLegendHeight();
			int yPositionMax = getHeight() - getXAxisHeight() - (isLegendPositionBottom() ? getLegendHeight() : 0);
			gc.drawLine(xPosition, yPositionMin, xPosition, yPositionMax);
		}
		
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			int yPosition = horizontalGridLinesPositions.get(i).intValue() + (isLegendPositionBottom() ? 0 : getLegendHeight());
			int xPositionMin = getYAxisWidth();
			int xPositionMax = getWidth() - 1;
			gc.drawLine(xPositionMin, yPosition, xPositionMax, yPosition);
		}

		gc.setLineStyle(SWT.LINE_SOLID);
	}

	private void drawLegend(GC gc) {
		if(!showLegend) return;
		int totalLength = 0;
		legendHeight = 0;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			String title = xyswtSerie.getTitle() + "___";
			totalLength += gc.textExtent(title).x;
			legendHeight =  Math.max(gc.textExtent(title).y, legendHeight);
		}
		int Y0 = (isLegendPositionBottom() ? getHeight() - getLegendHeight() : 0);
		int lastValueStringLength = 0;
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			String title = xyswtSerie.getTitle() + "___";
			Color color  = xyswtSerie.getColor();
			int valueStringLength = gc.textExtent(title).x;
			int xPosition = getWidth() / 2 - totalLength / 2 + lastValueStringLength;
			gc.setForeground(color);
			gc.drawString(title, xPosition, Y0);
			lastValueStringLength += valueStringLength;
		}
	}

	private void drawYAxis(GC gc) {
		if(!showAxis) return;
		gc.setLineWidth(axisLineWidth);
		gc.setForeground(axisColor);
		int Y0 = (isLegendPositionBottom() ? 0 : getLegendHeight());
		int Y1 = (isLegendPositionBottom() ? getLegendHeight() : 0);
		gc.drawLine(getYAxisWidth(), Y0, getYAxisWidth(), getHeight() - 1 - getXAxisHeight() - Y1);
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			int position = horizontalGridLinesPositions.get(i).intValue() + (isLegendPositionBottom() ? 0 : getLegendHeight());
			int valueStringLength = gc.textExtent(horizontalGridLinesLabels.get(i)).x;
			int xPosition = getYAxisWidth() / 2 - valueStringLength / 2;
			int yPostion = position - xAxisHeight / 2;
			if(i == 0 && (!showLegend || (showLegend && isLegendPositionBottom()))) yPostion = position;
			gc.drawString(horizontalGridLinesLabels.get(i), xPosition, yPostion);
		}
	}

	private void drawXAxis(GC gc) {
		if(!showAxis) return;
		gc.setLineWidth(axisLineWidth);
		gc.setForeground(axisColor);
		int Y0 = (isLegendPositionBottom() ? getLegendHeight() : 0);
		gc.drawLine(getYAxisWidth(), getHeight() - 1 - getXAxisHeight() - Y0, getWidth() - 1,  getHeight() - 1 - getXAxisHeight() - Y0);
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int position = verticalGridLinesPositions.get(i).intValue() + getYAxisWidth();
			int valueStringLength = gc.textExtent(verticalGridLinesLabels.get(i)).x;
			int xPosition = position - valueStringLength / 2;
			if(i == verticalGridLinesPositions.size() - 1) xPosition =  position - valueStringLength;
			int yPostion = getHeight() - getXAxisHeight() + marginAxis/2 - (isLegendPositionBottom() ? getLegendHeight() : 0) ;
			gc.drawString(verticalGridLinesLabels.get(i), xPosition, yPostion);
		}
	}

	private void drawSeries(GC gc) {
		for (XYSWTSerie xyswtSerie : xyswtSeries) {
			int[] values = xyswtSerie.getPointsArrayToDraw();
			int x0 = getYAxisWidth();
			int y0 = isLegendPositionBottom()?0:getLegendHeight();
			int w = getWidth() - x0;
			int h = getHeight() - y0 - getXAxisHeight() - (isLegendPositionBottom()?getLegendHeight():0);
			gc.setClipping(x0, y0,w, h);
			gc.setLineWidth(xyswtSerie.getThickness());
			gc.setForeground(xyswtSerie.getColor());
			gc.drawPolyline(values);
		}
	}
	
	public boolean isLegendPositionBottom() {
		return legendPosition == SWT.BOTTOM;
	}
	
	protected int getLegendHeight() {
		if(!showLegend) return 0;
		return legendHeight;
	}
	
	protected int getYAxisWidth() {
		if(!showAxis) return 0;
		return yAxisWidth + marginAxis;
	}
	
	protected int getXAxisHeight() {
		if(!showAxis) return 0;
		return xAxisHeight + marginAxis;
	}

	protected int getWidth() {
		return getClientArea().width;
	}

	protected int getHeight() {
		return getClientArea().height;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		chartFont.dispose();
		cursor.dispose();
		cursorBusy.dispose();
		if(backupImage != null && !backupImage.isDisposed()) backupImage.dispose();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if(showCursor) {
			double xValue = xPixelToValue(e.x - getYAxisWidth());
			double yValue = xyswtSeries.get(selectedSerie).getYValue(xValue);
			if(!Double.isNaN(yValue)) {
				if(markerPosition == null) markerPosition = new Point(0, 0);
				markerPosition.y = yValueToPixel(yValue);
				markerPosition.x = e.x;
			}
		}
		
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if(e.button != 1) return;
		zooming = true;
		zoomWindow = new Rectangle(e.x, e.y, 0, 0);
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if(zooming == false) return;
		if(e.button != 1) return;
		zooming = false;
		zoomWindow.width = e.x - zoomWindow.x;
		zoomWindow.height = e.y - zoomWindow.y;
		if(zoomWindow.width == 0 || zoomWindow.height ==0) return;
		zoomWindow = orderWindow(zoomWindow);
		double xMin = xPixelToValue(zoomWindow.x - getYAxisWidth());
		double xMax = xPixelToValue(zoomWindow.x + zoomWindow.width - getYAxisWidth());
		double yMax = yPixelToValue(zoomWindow.y - (isLegendPositionBottom()?0:getLegendHeight()));
		double yMin = yPixelToValue(zoomWindow.y + zoomWindow.height - (isLegendPositionBottom()?0:getLegendHeight()));
		window = new Window(xMin, xMax, yMin, yMax);
		updateCursor();
		redraw();
		update();
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if(zooming) {
			zoomWindow.width = e.x - zoomWindow.x;
			zoomWindow.height = e.y - zoomWindow.y;
			GC gc = new GC(this);
			drawZoom(gc);
			gc.dispose();
		} else {
			if(cursorPosition == null) cursorPosition = new Point(0, 0);
			cursorPosition.x = e.x;
			cursorPosition.y = e.y;
			crossPosition.x = e.x;
			crossPosition.y = e.y;
			if(showCursor) {
				updateCursor();
				redrawCursor();
			}
			if(showCursor) notifyCursorMarkerListeners();
		}
	}

	private void notifyCursorMarkerListeners() {
		int y0 = isLegendPositionBottom()?0:getLegendHeight();
		Point2D.Double cursor = new Point2D.Double(xPixelToValue(cursorPosition.x - getYAxisWidth()), yPixelToValue(cursorPosition.y - y0));
		Point2D.Double marker = null;
		if(markerPosition != null) marker = new Point2D.Double(xPixelToValue(markerPosition.x - getYAxisWidth()), yPixelToValue(markerPosition.y - y0));
		for (CursorMarkerListener cursorMarkerListener : cursorMarkerListeners) cursorMarkerListener.update(cursor, marker);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int D = 10;
		if((e.stateMask & SWT.CTRL) != 0) D = 100;
		if (e.keyCode == SWT.ARROW_UP) {
			double dy = (window.getYMax() - window.getYMin())/D;
			window = new Window(window.getXMin(), window.getXMax(), window.getYMin() + dy, window.getYMax() + dy);
			updateCursor();
			redraw();
			update();
		} else if (e.keyCode == SWT.ARROW_DOWN) {
			double dy = (window.getYMax() - window.getYMin())/D;
			window = new Window(window.getXMin(), window.getXMax(), window.getYMin() - dy, window.getYMax() - dy);
			updateCursor();
			redraw();
			update();
		} else if (e.keyCode == SWT.ARROW_LEFT) {
			double dx = (window.getXMax() - window.getXMin())/D;
			window = new Window(window.getXMin() - dx, window.getXMax() - dx, window.getYMin(), window.getYMax());
			updateCursor();
			redraw();
			update();
		} else if (e.keyCode == SWT.ARROW_RIGHT) {
			double dx = (window.getXMax() - window.getXMin())/D;
			window = new Window(window.getXMin() + dx, window.getXMax() + dx, window.getYMin(), window.getYMax());
			updateCursor();
			redraw();
			update();
		} else if (e.keyCode == SWT.TAB) {
			if(!showCursor) return;
			selectedSerie++;
			if(selectedSerie >= xyswtSeries.size()) selectedSerie = 0;
			if(xyswtSeries.size() == 0) selectedSerie = -1;
			updateCursor();
			redrawCursor();
		} else if (e.keyCode == SWT.ESC) {
			selectedSerie = -1;
			showCursor = false;
			contextMenu.updateShowCursorMenu(false);
			markerPosition = null;
			redrawCursor();
			previousCursorPosition = null;
			zooming = false;
			GC gc = new GC(this);
			clearZoom(gc);
			gc.dispose();
		}
	}
	
	private void redrawCursor() {
		GC gc = new GC(this);
		if(previousCursorPosition != null) {
			int x0 = previousCursorPosition.x - 200;
			if(x0 < 0) x0 = 0;
			int y0 = 0;
			int w = 400;
			int h = getHeight();
			if(x0 + w > getWidth()) w = getWidth() - x0;
			if(getBounds().equals(backupImage.getBounds())) {
				gc.drawImage(backupImage, x0, y0, w, h, x0, y0, w, h);
				if(SWT.getPlatform().equals("cocoa")) redraw(x0, y0, w, h, true);
			}
		}
		if(!showCursor) {
			gc.dispose();
			return;
		}
		if(selectedSerie == -1) {
			gc.dispose();
			return;
		}
		drawCursor(gc);
		gc.dispose();
		previousCursorPosition = cursorPosition;
	}

	private void updateCursor() {
		if(!showCursor) return;
		if(selectedSerie == -1) return;
		double xValue = xPixelToValue(cursorPosition.x - getYAxisWidth());
		double yValue = xyswtSeries.get(selectedSerie).getYValue(xValue);
		if(!Double.isNaN(yValue)) cursorPosition.y = yValueToPixel(yValue);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Nothing !
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		double xValue = xPixelToValue(crossPosition.x - getYAxisWidth());
		int y0 = isLegendPositionBottom()?0:getLegendHeight();
		double yValue = yPixelToValue(crossPosition.y - y0);
		window.zoom(e.count, xValue, yValue);
		redraw();
		update();
	}
	
	public void setShowAxis(boolean showAxis) {
		this.showAxis = showAxis;
	}
	
	public boolean isShowAxis() {
		return showAxis;
	}
	
	public void setShowCursor(boolean showCursor) {
		this.showCursor = showCursor;
		selectedSerie = showCursor ? 0 : -1;
		markerPosition = null;
		cursorPosition = null;
	}
	
	public boolean isShowCursor() {
		return showCursor;
	}
	
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}
	
	public boolean isShowGrid() {
		return showGrid;
	}
	
	public void setAxisColor(Color axisColor) {
		this.axisColor = axisColor;
	}
	
	public void setAxisLineWidth(int axisLineWidth) {
		this.axisLineWidth = axisLineWidth;
	}
	
	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}
	
	public boolean isShowLegend() {
		return showLegend;
	}
	
	public void setLegendPosition(int legendPosition) {
		this.legendPosition = legendPosition;
	}
}