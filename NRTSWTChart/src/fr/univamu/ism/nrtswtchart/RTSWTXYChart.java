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
package fr.univamu.ism.nrtswtchart;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class RTSWTXYChart extends ControlAdapter implements PaintListener, DisposeListener {
	
	private final class MenuListenerHandler extends MenuAdapter {
		public void menuShown(MenuEvent e) {
			showLegendMenuItem.setSelection(showLegend);
			legendPositionsBottomMenuItem.setSelection(legendPosition == SWT.BOTTOM);
			legendPositionsTopMenuItem.setSelection(legendPosition == SWT.TOP);
			autoScaleMenu.setSelection(autoScale);
			showGridMenu.setSelection(showGrid);
		}
	}
	
	private final class LegendPositionHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean legendPositionChanged = false;
			if (((MenuItem) e.widget) == legendPositionsBottomMenuItem &&  legendPosition == SWT.TOP) {
				legendPositionChanged = true;
				setLegendPosition(SWT.BOTTOM);
			}
			if (((MenuItem) e.widget) == legendPositionsTopMenuItem &&  legendPosition == SWT.BOTTOM) {
				legendPositionChanged = true;
				setLegendPosition(SWT.TOP);
			}
			if(showLegend && !legendPositionChanged) return;
			showLegend = true;
			RTSWTXYSerie[] series = getSeries();
			for (RTSWTXYSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
		}
	}

	private final class ShowLegendHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			setLegendVisibility(((MenuItem) e.widget).getSelection());
			RTSWTXYSerie[] series = getSeries();
			for (RTSWTXYSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
		}
	}
	
	private final class AutoScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			setAutoScale(((MenuItem) e.widget).getSelection());
			RTSWTXYSerie[] series = getSeries();
			for (RTSWTXYSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
		}
	}
	
	private final class ShowGridHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			setShowGrid(((MenuItem) e.widget).getSelection());
			RTSWTXYSerie[] series = getSeries();
			for (RTSWTXYSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
		}
	}
	
	private MenuItem showLegendMenuItem;
	private MenuItem legendPositionsTopMenuItem;
	private MenuItem legendPositionsBottomMenuItem;
	private MenuItem autoScaleMenu;
	private MenuItem showGridMenu;
	
	private Canvas chart;
	private ArrayList<RTSWTXYSerie> rtswtSeries = new ArrayList<RTSWTXYSerie>();
	
	private PaletteData paletteData;
	private ImageData imageData;
	private Image image;
	private LinkedList<Image> images = new LinkedList<Image>();
	
	private DecimalFormat decimalFormatter = new DecimalFormat("0.##E0");

	private Color fontColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	private Font chartFont;
	private int fontHeight;
	
	private double xMin = -10;
	private double xMax = 10;
	private double yMin = -10;
	private double yMax = 10;
	private boolean waitForAllSeriesToRedraw = false;
	private boolean autoScale = true;
	private boolean showLegend = true;
	private boolean showGrid = true;
	private int legendPosition = SWT.TOP;
	
	private ArrayList<Integer> verticalGridLinesPositions = new ArrayList<Integer>(0);
	private ArrayList<Integer> horizontalGridLinesPositions = new ArrayList<Integer>(0);
	private int leftAxisWidth;
	private int bottomAxisHeight;
	
	private ArrayList<Long> drawTime = new ArrayList<Long>(0);
	
	private int value;
	
	public RTSWTXYChart(Composite parent, int style, String fontName, int chartFontStyle, int chartFontHeight) {
		chart = new Canvas(parent, style);
		chart.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		chart.addPaintListener(this);
		chart.addControlListener(this);
		chart.addDisposeListener(this);
		
		if(fontName == null || "".equals(fontName)) {
			FontDescriptor boldDescriptor = FontDescriptor.createFrom(chart.getFont()).setStyle(chartFontStyle).setHeight(chartFontHeight);
			chartFont = boldDescriptor.createFont(chart.getDisplay());
		} else {
			FontData fontData = new FontData(fontName, chartFontHeight, chartFontStyle);
			chartFont = new Font(chart.getDisplay(), fontData);
		}
		chart.setFont(chartFont);
		
		GC gc = new GC(chart);
		gc.setFont(chart.getFont());
		leftAxisWidth = gc.textExtent("-9." + decimalFormatter.getDecimalFormatSymbols().getDecimalSeparator() + "99E-99").x;
		FontMetrics fontMetrics = gc.getFontMetrics();
		fontHeight = fontMetrics.getHeight();
		bottomAxisHeight = fontHeight + 10;
		gc.dispose();
		
		Menu mainMenu = new Menu(chart);
		mainMenu.addMenuListener(new MenuListenerHandler());
		showLegendMenuItem = new MenuItem(mainMenu, SWT.CHECK);
		showLegendMenuItem.setText(RTSWTChartMessages.ShowLegend);
		showLegendMenuItem.addSelectionListener(new ShowLegendHandler());
		MenuItem legendPositionsMenuItem = new MenuItem(mainMenu, SWT.CASCADE);
		legendPositionsMenuItem.setText(RTSWTChartMessages.LegendPosition);
		Menu legendPositionsMenu = new Menu(legendPositionsMenuItem);
		legendPositionsTopMenuItem = new MenuItem(legendPositionsMenu, SWT.CHECK);
		legendPositionsTopMenuItem.setText(RTSWTChartMessages.Top);
		legendPositionsBottomMenuItem = new MenuItem(legendPositionsMenu, SWT.CHECK);
		legendPositionsBottomMenuItem.setText(RTSWTChartMessages.Bottom);
		LegendPositionHandler legendPositionHandler = new LegendPositionHandler();
		legendPositionsTopMenuItem.addSelectionListener(legendPositionHandler);
		legendPositionsBottomMenuItem.addSelectionListener(legendPositionHandler);
		legendPositionsMenuItem.setMenu(legendPositionsMenu);
		autoScaleMenu = new MenuItem(mainMenu, SWT.CHECK);
		autoScaleMenu.setText(RTSWTChartMessages.AutoScale);
		autoScaleMenu.addSelectionListener(new AutoScaleHandler());
		showGridMenu = new MenuItem(mainMenu, SWT.CHECK);
		showGridMenu.setText(RTSWTChartMessages.showGrid);
		showGridMenu.addSelectionListener(new ShowGridHandler());
		chart.setMenu(mainMenu);
	}
	
	public RTSWTXYSerie createSerie(String title, Color color) {
		RTSWTXYSerie rtswtSerie = new RTSWTXYSerie(this, title, color);
		rtswtSeries.add(rtswtSerie);
		return rtswtSerie;
	}
	
	private RTSWTXYSerie[] getSeries() {
		return rtswtSeries.toArray(new RTSWTXYSerie[rtswtSeries.size()]);
	}
	
	protected double getDx() {
		double D = getWidth() - getLeftAxisWidth() - 1;
		return (getxMax() - getxMin()) / D;
	}
	
	protected double getDy() {
		double D = getHeight() - getBottomAxisHeight() - 1 - (showLegend ? getLegendHeight() : 0);
		return (getyMax() - getyMin()) / D;
	}

	protected int getWidth() {
		chart.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				value = chart.getClientArea().width;
			}
		});
		return value;
	}
	
	protected int getHeight() {
		chart.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				value = chart.getClientArea().height;
			}
		});
		return value;
	}

	protected int getLeftAxisWidth() {
		return showGrid?leftAxisWidth:0;
	}
	
	private boolean isAutoScale() {
		return autoScale;
	}
	
	public void setAutoScale(boolean autoScale) {
		this.autoScale = autoScale;
	}
	
	public void setWaitForAllSeriesToRedraw(boolean waitForAllSeriesToRedraw) {
		this.waitForAllSeriesToRedraw = waitForAllSeriesToRedraw;
	}
	
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}
	
	private double getAbsolutexMinValue() {
		RTSWTXYSerie[] series = getSeries();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < series.length; i++) {
			min = Math.min(series[i].getxMin(), min);
		}
		return min;
	}

	private double getAbsolutexMaxValue() {
		RTSWTXYSerie[] series = getSeries();
		double max = Double.MIN_VALUE;
		for (int i = 0; i < series.length; i++) {
			max = Math.max(series[i].getxMax(), max);
		}
		return max;
	}
	
	private double getAbsoluteyMinValue() {
		RTSWTXYSerie[] series = getSeries();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < series.length; i++) {
			min = Math.min(series[i].getyMin(), min);
		}
		return min;
	}

	private double getAbsoluteyMaxValue() {
		RTSWTXYSerie[] series = getSeries();
		double max = Double.MIN_VALUE;
		for (int i = 0; i < series.length; i++) {
			max = Math.max(series[i].getyMax(), max);
		}
		return max;
	}
	
	protected double getyMin() {
		return yMin;
	}

	private double getyMax() {
		return yMax;
	}
	
	protected double getxMin() {
		return xMin;
	}

	private double getxMax() {
		return xMax;
	}
	
	private void setyMin(double yMin) {
		this.yMin = yMin;
	}

	private void setyMax(double yMax) {
		this.yMax = yMax;
	}
	
	private void setxMin(double xMin) {
		this.xMin = xMin;
	}

	private void setxMax(double xMax) {
		this.xMax = xMax;
	}
	
	protected int getBottomAxisHeight() {
		return showGrid?bottomAxisHeight:0;
	}
	
	protected int getLegendHeight() {
		return showLegend?bottomAxisHeight + 2:0;
	}
	
	protected boolean isLegendVisible() {
		return showLegend;
	}
	
	public void setLegendVisibility(boolean showLegend) {
		this.showLegend = showLegend;
	}
	
	protected Canvas getChart() {
		return chart;
	}
	
	public void setLegendPosition(int legendPosition) {
		this.legendPosition = legendPosition;
	}
	
	private int getFontHeight() {
		return fontHeight;
	}
	
	public void setGridVisibility(boolean showGrid) {
		this.showGrid = showGrid;
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (chart.isDisposed()) return;
		Image image = images.peekLast();
		if(image == null || image.isDisposed()) return;
		e.gc.drawImage(image, 0, 0);
		
		if(showGrid) {
			for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
				int position = horizontalGridLinesPositions.get(i);
				double value = 0;
				if(!showLegend) value = getyMin() + (position - (getHeight() - getBottomAxisHeight() - 1))* (getyMax() - getyMin()) / ( - (getHeight() - getBottomAxisHeight() - 1) );
				if(showLegend && legendPosition == SWT.TOP) value = getyMin() + (position - (getHeight() - getBottomAxisHeight() - 1))* (getyMax() - getyMin()) / ( getLegendHeight() - (getHeight() - getBottomAxisHeight() - 1) );
				if(showLegend && legendPosition == SWT.BOTTOM) value = getyMin() + (position - (getHeight() - getBottomAxisHeight() - getLegendHeight() - 1))* (getyMax() - getyMin()) / (  - (getHeight() - getBottomAxisHeight() - getLegendHeight() - 1) );
				String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
				int valueStringLength = e.gc.textExtent(valueString).x;
				int xPosition = getLeftAxisWidth() / 2 - valueStringLength / 2;
				int yPostion = position - getFontHeight() / 2;
				if(i == 0 && legendPosition == SWT.BOTTOM) yPostion = position;
				e.gc.setForeground(fontColor);
				e.gc.drawString(valueString, xPosition, yPostion);
			}
			for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
				int position = verticalGridLinesPositions.get(i).intValue();
				double value = getxMin() + (position - getLeftAxisWidth())* (getxMax() - getxMin()) / (getWidth() - getLeftAxisWidth() - 1);
				String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
				int valueStringLength =e.gc.textExtent(valueString).x;
				int xPosition = position - valueStringLength / 2;
				if(i == verticalGridLinesPositions.size() - 1) xPosition =  position - valueStringLength;
				int yPostion = getHeight() - getBottomAxisHeight() - ((showLegend && legendPosition == SWT.TOP) ? 0 : showLegend ? getLegendHeight() : 0);
				e.gc.setForeground(fontColor);
				e.gc.drawString(valueString, xPosition, yPostion);
			}
		}
		
		if(showLegend) {
			int totalLength = 0;
			for (int i = 0; i < rtswtSeries.size(); i++) {
				RTSWTXYSerie rtswtSerie = rtswtSeries.get(i);
				String title = rtswtSerie.getTitle(); 
				title = rtswtSerie.getTitle() + " -- ";
				totalLength += e.gc.textExtent(title).x;
			}
			int lastValueStringLength = 0;
			for (int i = 0; i < rtswtSeries.size(); i++) {
				RTSWTXYSerie rtswtSerie = rtswtSeries.get(i);
				String title = rtswtSerie.getTitle();
				title = rtswtSerie.getTitle() + " -- ";
				Color color  = rtswtSerie.getColor();
				int valueStringLength = e.gc.textExtent(title).x;
				int xPosition = getWidth() / 2 - totalLength/2 + lastValueStringLength + ((showGrid==true)?getLeftAxisWidth()/2:0);
				int yPostion = legendPosition == SWT.TOP ? 5 : getHeight() - getLegendHeight() - 3;
				e.gc.setForeground(color);
				e.gc.drawString(title, xPosition, yPostion);
				lastValueStringLength += valueStringLength;
			}
		}
		
	}
	
	public String getMeanDrawTime() {
		Long[] drawTimes = drawTime.toArray(new Long[drawTime.size()]);
		Long sum = Long.valueOf(0);
	    for (int i = 0; i < drawTimes.length; i++) {
	        sum += drawTimes[i];
	    }
	    double finalValue = sum / 1000000f;
	    return Double.toString(finalValue) + "ms for " + drawTimes.length + " graph update. Mean time per graph update : " + (finalValue/(drawTimes.length)) + "ms";
	}

	@Override
	public void controlResized(ControlEvent e) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				RTSWTXYSerie[] series = getSeries();
				for (RTSWTXYSerie rtswtSerie : series) {
					
					rtswtSerie.reset();
				}
				prepareGrids();
			}
		});
	}

	protected void checkUpdate() {
		RTSWTXYSerie[] series = getSeries();
		boolean redraw = true;
		if (waitForAllSeriesToRedraw)
			for (int i = 0; i < series.length; i++)
				redraw = redraw && series[i].getModified();
		
		if (redraw && isAutoScale()) {
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
		if(redraw) {
			long t = System.nanoTime();
			if(images.size() == 3) images.removeFirst().dispose();
			paletteData = new PaletteData(0XFF, 0xFF00, 0xFF0000);
		    imageData = new ImageData(getWidth(), getHeight(), 24, paletteData);
		    if (showGrid) {
		    	try {
					drawGrids();
				} catch (Exception e) {
					e.printStackTrace();
				}
		    }
		    drawSeries();
		    image = new Image(getChart().getDisplay(), imageData);
		    images.add(image);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					getChart().redraw();
					getChart().update();
				}
			});
			drawTime.add(System.nanoTime() - t);
		}
	}
	
	private void drawGrids() {
		// Draw vertical grid
		int color =  fontColor.getRed() + (fontColor.getGreen() << 8) + (fontColor.getBlue() << 16);
		int yPositionMin = (showLegend && legendPosition == SWT.BOTTOM) ? 0 : (showLegend ? getLegendHeight() : 0);
		int yPositionMax = (showLegend && legendPosition == SWT.BOTTOM) ? getHeight() - getBottomAxisHeight() - getLegendHeight() - 1 : getHeight() - getBottomAxisHeight() - 1;
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int x = verticalGridLinesPositions.get(i);
			for (int y = yPositionMin; y < yPositionMax; y++) {
				int index = (y * imageData.bytesPerLine) + (x * 3);
				int pixel = color;
				imageData.data[index] = (byte)((pixel >> 16) & 0xFF);
				imageData.data[index + 1] = (byte)((pixel >> 8) & 0xFF);
				imageData.data[index + 2] = (byte)(pixel & 0xFF);
			}
		}
		// Draw horizontal grid
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			int[] values = new int[getWidth() - getLeftAxisWidth()];
			Arrays.fill(values, color);
			int position = horizontalGridLinesPositions.get(i);
			try {
				imageData.setPixels(getLeftAxisWidth(), position, getWidth() - getLeftAxisWidth() - 1, values, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void drawSeries() {
		RTSWTXYSerie[] series = getSeries();
		int offset = 0;
		if(showLegend && legendPosition == SWT.TOP) offset = getLegendHeight();
		for (RTSWTXYSerie rtswtSerie : series) {
			int[] values = rtswtSerie.getPointsArrayToDraw();
			int color =  rtswtSerie.getColor().getRed() + (rtswtSerie.getColor().getGreen() << 8) + (rtswtSerie.getColor().getBlue() << 16);
			rtswtSerie.setModified(false);
			if(values.length == 0) continue;
			if(values.length <= 2) {
				imageData.setPixel(values[0] + getLeftAxisWidth(), values[1] + offset, color);
			} else {
				for (int i = 0; i < values.length - 2; i += 2) {
					try {
						plotLine(values[i] + getLeftAxisWidth(), values[i + 1] + offset, values[i + 2] + getLeftAxisWidth(), values[i + 3] + offset, color);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void plotLineLow(int x0, int y0, int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int yi = 1;
		if (dy < 0) {
			yi = -1;
			dy = -dy;
		}
		int D = (2 * dy) - dx;
		int y = y0;
		for(int x = x0; x <= x1; x++) {
			imageData.setPixel(x, y, color);
			if (D > 0) {
				y = y + yi;
				D = D + (2 * (dy - dx));
			} else D = D + 2*dy;
		}
	}
	
	private void plotLineHigh(int x0, int y0, int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int xi = 1;
		if (dx < 0) {
			xi = -1;
			dx = -dx;
		}
		int D = (2 * dx) - dy;
		int x = x0;
		for(int y = y0; y <= y1; y++) {
			imageData.setPixel(x, y, color);
			if (D > 0) {
				x = x + xi;
				D = D + (2 * (dx - dy));
			} else D = D + 2*dx;
		}
	}
        
	private void plotLine(int x0, int y0, int x1, int y1, int color) {
		if(x0 != x1 && y0 != y1) {
			if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
				if (x0 > x1) plotLineLow(x1, y1, x0, y0, color);
				else plotLineLow(x0, y0, x1, y1, color);
			} else {
				if (y0 > y1) plotLineHigh(x1, y1, x0, y0, color);
				else plotLineHigh(x0, y0, x1, y1, color);
			}
		} else {
			if(x0 != x1 && y0 == y1) {
				if(x0 < x1) {
					for (int x = x0; x <= x1; x++) {
						imageData.setPixel(x, y0, color);
					}
				} else {
					for (int x = x1; x <= x0; x++) {
						imageData.setPixel(x, y0, color);
					}
				}
			} else {
				if(y0 < y1) {
					for (int y = y0; y <= y1; y++) {
						imageData.setPixel(x0, y, color);
					}
				} else {
					for (int y = y1; y <= y0; y++) {
						imageData.setPixel(x0, y, color);
					}
				}
				
			}
		}
		
	}
	
	private void prepareGrids() {
		// Vertical grids
		verticalGridLinesPositions.clear();
		verticalGridLinesPositions.add(getLeftAxisWidth());
		verticalGridLinesPositions.add(getWidth() - 1);
		createGridLinesPositions(verticalGridLinesPositions, verticalGridLinesPositions.get(0), verticalGridLinesPositions.get(1),100);
		Collections.sort(verticalGridLinesPositions);
		// Horizontal grids
		int yPositionMin = (showLegend && legendPosition == SWT.BOTTOM) ? 0 : (showLegend ? getLegendHeight() : 0);
		int yPositionMax = (showLegend && legendPosition == SWT.BOTTOM) ? getHeight() - getBottomAxisHeight() - getLegendHeight() - 1 : getHeight() - getBottomAxisHeight() - 1;
		horizontalGridLinesPositions.clear();
		horizontalGridLinesPositions.add(yPositionMin);
		horizontalGridLinesPositions.add(yPositionMax);
		createGridLinesPositions(horizontalGridLinesPositions, horizontalGridLinesPositions.get(0), horizontalGridLinesPositions.get(1), 100);
		Collections.sort(horizontalGridLinesPositions);
	}
	
	private void createGridLinesPositions(ArrayList<Integer> gridLines, int from, int to, int delta) {
		if(to - from > 2*delta) {
			int value = (int)Math.rint((double)(from + to) / 2.0);
			gridLines.add(value);
			createGridLinesPositions(gridLines, from, value, delta);
			createGridLinesPositions(gridLines, value, to, delta);
		}
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		while (images.size() > 0) {
			images.removeLast().dispose();
		}
		chartFont.dispose();
	}

}
