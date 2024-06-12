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

public class RTSWTOscilloChart extends ControlAdapter implements PaintListener, DisposeListener {
	
	private final class MenuListenerHandler extends MenuAdapter {
		public void menuShown(MenuEvent e) {
			showLegendMenuItem.setSelection(showLegend);
			legendPositionsBottomMenuItem.setSelection(legendPosition == SWT.BOTTOM);
			legendPositionsTopMenuItem.setSelection(legendPosition == SWT.TOP);
			autoScaleMenu.setSelection(autoScale);
			showGridMenu.setSelection(showGrid);
			ShowCurrentValuesMenu.setSelection(showCurrentValues);
		}
	}

	private final class LegendPositionHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			if (((MenuItem) e.widget) == legendPositionsBottomMenuItem &&  legendPosition == SWT.TOP) legendPosition = SWT.BOTTOM;
			if (((MenuItem) e.widget) == legendPositionsTopMenuItem &&  legendPosition == SWT.BOTTOM) legendPosition = SWT.TOP;
			showLegend = true;
			RTSWTOscilloSerie[] series = getSeries();
			for (RTSWTOscilloSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
//			prepareLegend();
//			prepareSeries();
//			prepareCurrentValues();
		}
	}

	private final class ShowLegendHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			showLegend = ((MenuItem) e.widget).getSelection();
			RTSWTOscilloSerie[] series = getSeries();
			for (RTSWTOscilloSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
//			prepareLegend();
//			prepareSeries();
//			prepareCurrentValues();
		}
	}

	private final class AutoScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			autoScale = ((MenuItem) e.widget).getSelection();
			RTSWTOscilloSerie[] series = getSeries();
			for (RTSWTOscilloSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
//			prepareLegend();
			prepareSeries();
//			prepareCurrentValues();
		}
	}

	private final class ShowCurrentValuesHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			showCurrentValues = ((MenuItem) e.widget).getSelection();
		}
	}

	private final class ShowGridHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			showGrid = ((MenuItem) e.widget).getSelection();
			RTSWTOscilloSerie[] series = getSeries();
			for (RTSWTOscilloSerie rtswtSerie : series) {
				rtswtSerie.reset();
			}
			prepareGrids();
//			prepareLegend();
//			prepareSeries();
//			prepareCurrentValues();
		}
	}

	private MenuItem showLegendMenuItem;
	private MenuItem legendPositionsTopMenuItem;
	private MenuItem legendPositionsBottomMenuItem;
	private MenuItem autoScaleMenu;
	private MenuItem ShowCurrentValuesMenu;
	private MenuItem showGridMenu;

	private Canvas chart;
	private ArrayList<RTSWTOscilloSerie> rtswtSeries = new ArrayList<RTSWTOscilloSerie>();

	private PaletteData paletteData;
	private ImageData legendImageData;
	private Image legendImage;
	private ImageData gridImageData;
	private Image gridImage;
	private ImageData seriesImageData;
	private Image seriesImage;
	private ImageData currentValuesImageData;
	private Image currentValuesImage;

	private DecimalFormat decimalFormatter = new DecimalFormat("0.##E0");
	private DecimalFormat decimalFormatterCurrentValues = new DecimalFormat("#0.000000");

	private Color fontColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	private Color gridLinesColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	private Color backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	private Font chartFont;
	private int fontHeight;

	private double yMin = -10;
	private double yMax = 10;
	private double windowTimeWidth = 1;
	private boolean waitForAllSeriesToRedraw = false;
	private boolean autoScale = true;
	private boolean showLegend = true;
	private boolean showGrid = true;
	private boolean redrawGrid;
	private boolean showCurrentValues = false;
	private int legendPosition = SWT.TOP;

	private ArrayList<Integer> verticalGridLinesPositions = new ArrayList<Integer>(0);
	private ArrayList<Integer> horizontalGridLinesPositions = new ArrayList<Integer>(0);
	
	private int leftAxisWidth;
	private int bottomAxisHeight;
	private int legendHeight;
	private int showCurrentValuesFontHeight;

	private ArrayList<Long> drawTime = new ArrayList<Long>(0);

	public RTSWTOscilloChart(Composite parent, int style, String fontName, int chartFontStyle, int chartFontHeight) {
		chart = new Canvas(parent, style);
		chart.setBackground(backgroundColor);
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
		leftAxisWidth = gc.textExtent("-9." + decimalFormatter.getDecimalFormatSymbols().getDecimalSeparator() + "9E-99").x + 10;
		FontMetrics fontMetrics = gc.getFontMetrics();
		showCurrentValuesFontHeight = fontMetrics.getAscent();
		bottomAxisHeight = fontMetrics.getAscent() + fontMetrics.getDescent();
		legendHeight = bottomAxisHeight;
		fontHeight = bottomAxisHeight;
		
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
		ShowCurrentValuesMenu =  new MenuItem(mainMenu, SWT.CHECK);
		ShowCurrentValuesMenu.setText(RTSWTChartMessages.ShowValues);
		ShowCurrentValuesMenu.addSelectionListener(new ShowCurrentValuesHandler());
		chart.setMenu(mainMenu);

		paletteData = new PaletteData(0XFF, 0xFF00, 0xFF0000);
	}
	
	public RTSWTOscilloSerie createSerie(String title, Color color) {
		RTSWTOscilloSerie rtswtSerie = new RTSWTOscilloSerie(this, title, color);
		rtswtSeries.add(rtswtSerie);
		return rtswtSerie;
	}

	private RTSWTOscilloSerie[] getSeries() {
		return rtswtSeries.toArray(new RTSWTOscilloSerie[rtswtSeries.size()]);
	}

	protected double getDx() {
		return windowTimeWidth / (getWidth() - getLeftAxisWidth() - 1);
	}

	protected double getDy() {
		double D = getHeight() - getBottomAxisHeight() - getLegendHeight() - 1;
		return (getyMax() - getyMin()) / D;
	}

	protected int getWidth() {
		return chart.getClientArea().width;
	}

	protected int getHeight() {
		return chart.getClientArea().height;
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

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}

	public void setShowCurrentValue(boolean showCurrentValues) {
		this.showCurrentValues = showCurrentValues;
	}

	private double getAbsoluteyMinValue() {
		RTSWTOscilloSerie[] series = getSeries();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < series.length; i++) {
			min = Math.min(series[i].getyMin(), min);
		}
		return min;
	}

	private double getAbsoluteyMaxValue() {
		RTSWTOscilloSerie[] series = getSeries();
		double max = Double.MIN_VALUE;
		for (int i = 0; i < series.length; i++) {
			max = Math.max(series[i].getyMax(), max);
		}
		return max;
	}
	
	public void setGridLinesColor(Color color) {
		gridLinesColor = color;
	}
	
	public void setFontColor(Color fontColor) {
		this.fontColor = fontColor;
	}

	protected double getyMin() {
		return yMin;
	}

	private double getyMax() {
		return yMax;
	}

	public void setyMin(double yMin) {
		this.yMin = yMin;
	}

	public void setyMax(double yMax) {
		this.yMax = yMax;
	}

	protected int getBottomAxisHeight() {
		return showGrid?bottomAxisHeight:0;
	}

	protected int getLegendHeight() {
		return showLegend?legendHeight:0;
	}

	protected boolean isLegendVisible() {
		return showLegend;
	}

	public void setLegendVisibility(boolean showLegend) {
		this.showLegend = showLegend;
	}

	public Canvas getChart() {
		return chart;
	}

	public void setWindowTimeWidth(double windowTimeWidth) {
		this.windowTimeWidth = windowTimeWidth;
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

	public void setWaitForAllSeriesToRedraw(boolean waitForAllSeriesToRedraw) {
		this.waitForAllSeriesToRedraw = waitForAllSeriesToRedraw;
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
		RTSWTOscilloSerie[] series = getSeries();
		for (RTSWTOscilloSerie rtswtSerie : series) {
			rtswtSerie.reset();
		}
		prepareGrids();
		prepareLegend();
		prepareSeries();
		prepareCurrentValues();
	}
	
	@Override
	public void widgetDisposed(DisposeEvent e) {
		if(legendImage != null && !legendImage.isDisposed()) legendImage.dispose();
		if(gridImage != null && !gridImage.isDisposed()) gridImage.dispose();
		if(seriesImage != null && !seriesImage.isDisposed()) seriesImage.dispose();
		if(currentValuesImage != null && !currentValuesImage.isDisposed()) currentValuesImage.dispose();
		chartFont.dispose();
		chart.getMenu().setVisible(false);
	}
	
	private void computeGrids() {
		// Vertical grids
		verticalGridLinesPositions.clear();
		verticalGridLinesPositions.add(0);
		verticalGridLinesPositions.add(getWidth() - getLeftAxisWidth() - 1);
		createGridLinesPositions(verticalGridLinesPositions, verticalGridLinesPositions.get(0), verticalGridLinesPositions.get(1), 100);
		Collections.sort(verticalGridLinesPositions);
		// Horizontal grids
		horizontalGridLinesPositions.clear();
		horizontalGridLinesPositions.add(0);
		horizontalGridLinesPositions.add(getHeight() - getLegendHeight() - getBottomAxisHeight() - 1);
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
	
	private void prepareGrids() {
		if(gridImage != null && !gridImage.isDisposed()) gridImage.dispose();
		gridImageData = new ImageData(getWidth(), getHeight() - getLegendHeight(), 24, paletteData);
		computeGrids();
		gridImage = new Image(chart.getDisplay(), gridImageData);
		// Draw labels
		GC gc = new GC(gridImage);
		for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
			int position = horizontalGridLinesPositions.get(i);
			double value = 0;
			value = getyMin() + (position - (gridImageData.height - getBottomAxisHeight() - 1))* (getyMax() - getyMin()) / ( - (gridImageData.height - getBottomAxisHeight() - 1) );
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			int valueStringLength = gc.textExtent(valueString).x;
			int xPosition = getLeftAxisWidth() / 2 - valueStringLength / 2;
			int yPostion = position - getFontHeight() / 2;
			if(i == 0) yPostion = position;
			gc.setForeground(fontColor);
			gc.setBackground(backgroundColor);
			gc.drawString(valueString, xPosition, yPostion);
		}
		for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
			int position = verticalGridLinesPositions.get(i).intValue();
			double value = position * windowTimeWidth / (getWidth() - getLeftAxisWidth() - 1);
			position = verticalGridLinesPositions.get(i).intValue() + getLeftAxisWidth();
			String valueString = decimalFormatter.format(value).replaceAll("E0$", "");
			int valueStringLength = gc.textExtent(valueString).x;
			int xPosition = position - valueStringLength / 2;
			if(i == verticalGridLinesPositions.size() - 1) xPosition =  position - valueStringLength;
			int yPostion = gridImageData.height - getBottomAxisHeight() ;//- ((showLegend && legendPosition == SWT.TOP) ? 0 : showLegend ? getLegendHeight() : 0);
			gc.setForeground(fontColor);
			gc.setBackground(backgroundColor);
			gc.drawString(valueString, xPosition, yPostion);
		}
		gc.dispose();
	}
	
	private void prepareLegend() {
		if(legendImage != null && !legendImage.isDisposed()) legendImage.dispose();
		legendImageData = new ImageData(getWidth() - getLeftAxisWidth(), fontHeight, 24, paletteData);
		legendImage = new Image(chart.getDisplay(), legendImageData);
		GC gc = new GC(legendImage);
		int totalLength = 0;
		for (int i = 0; i < rtswtSeries.size(); i++) {
			RTSWTOscilloSerie rtswtSerie = rtswtSeries.get(i);
			if(rtswtSerie.isHorizontalReference()) continue;
			String title = rtswtSerie.getTitle(); 
			title = rtswtSerie.getTitle() + " -- ";
			totalLength += gc.textExtent(title).x;
		}
		int lastValueStringLength = 0;
		for (int i = 0; i < rtswtSeries.size(); i++) {
			RTSWTOscilloSerie rtswtSerie = rtswtSeries.get(i);
			if(rtswtSerie.isHorizontalReference()) continue;
			String title = rtswtSerie.getTitle();
			title = rtswtSerie.getTitle() + " -- ";
			Color color  = rtswtSerie.getColor();
			int valueStringLength = gc.textExtent(title).x;
			int xPosition = legendImageData.width / 2 - totalLength / 2 + lastValueStringLength;
			gc.setForeground(color);
			gc.setBackground(backgroundColor);
			gc.drawString(title, xPosition, 0);
			lastValueStringLength += valueStringLength;
		}
		gc.dispose();
	}

	private void prepareSeries() {
		if(seriesImage != null && !seriesImage.isDisposed()) seriesImage.dispose();
		seriesImageData = new ImageData(getWidth() - getLeftAxisWidth(), getHeight() - getBottomAxisHeight() - getLegendHeight(), 24, paletteData);
		
		if(showGrid) {
			int color =  gridLinesColor.getRed() + (gridLinesColor.getGreen() << 8) + (gridLinesColor.getBlue() << 16);
			// Draw vertical grid
			for (int i = 0; i < verticalGridLinesPositions.size(); i++) {
				int position = verticalGridLinesPositions.get(i);
				RTSWTChartUtils.plotLine(seriesImageData, position, 0, position, seriesImageData.height - 1, color);
			}
			// Draw horizontal grid
			int[] values = new int[getWidth() - getLeftAxisWidth()];
			Arrays.fill(values, color);
			for (int i = 0; i < horizontalGridLinesPositions.size(); i++) {
				int position = horizontalGridLinesPositions.get(i);
				RTSWTChartUtils.plotLine(seriesImageData, 0, position, seriesImageData.width - 1, position, color);
			}
		}
		
		RTSWTOscilloSerie[] series = getSeries();
		for (RTSWTOscilloSerie rtswtSerie : series) {
			int[] values = rtswtSerie.getPointsArrayToDraw();
			int color =  rtswtSerie.getColor().getRed() + (rtswtSerie.getColor().getGreen() << 8) + (rtswtSerie.getColor().getBlue() << 16);
			rtswtSerie.setModified(false);
			if(values.length == 0) continue;
			if(rtswtSerie.isHorizontalReference()) {
				try {
					RTSWTChartUtils.plotLine(seriesImageData, 0, values[1], seriesImageData.width - 1, values[1], color);
				} catch (Exception e) {
					e.printStackTrace();
				}
				continue;
			}
			if(values.length <= 2) {
				seriesImageData.setPixel(values[0], values[1], color);
			} else {
				for (int i = 0; i < values.length - 2; i += 2) {
					try {
						RTSWTChartUtils.plotLine(seriesImageData, values[i], values[i + 1], values[i + 2], values[i + 3], color);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		seriesImage = new Image(chart.getDisplay(), seriesImageData);
	}
	
	private void prepareCurrentValues() {
		if(currentValuesImage != null && !currentValuesImage.isDisposed()) currentValuesImage.dispose();
		currentValuesImageData = new ImageData(10, 10, 24, paletteData);
		currentValuesImage = new Image(chart.getDisplay(), currentValuesImageData);
		GC gc = new GC(currentValuesImage);
		
		int imageWidth = 0;
		int imageHeight = 0;
		ArrayList<String> valuesString = new ArrayList<>();
		for (RTSWTOscilloSerie rtswtSerie : rtswtSeries) {
			if(rtswtSerie.isHorizontalReference()) continue;
			double currentValue = rtswtSerie.getCurrentValue();
			String valueString = decimalFormatterCurrentValues.format(currentValue);
			valuesString.add(valueString);
			int valueStringWidth = gc.textExtent(valueString).x;
			int valueStringHeight = gc.textExtent(valueString).y;
			imageWidth = Math.max(valueStringWidth, imageWidth);
			imageHeight += valueStringHeight;
		}
		
		gc.dispose();
		currentValuesImage.dispose();
		
		currentValuesImageData = new ImageData(imageWidth, imageHeight, 24, paletteData);
		currentValuesImage = new Image(chart.getDisplay(), currentValuesImageData);
		gc = new GC(currentValuesImage);

		int numSerie = 0;
		for (RTSWTOscilloSerie rtswtSerie : rtswtSeries) {
			if(rtswtSerie.isHorizontalReference()) continue;
			String valueString = valuesString.get(numSerie);
			int yPosition = showCurrentValuesFontHeight*numSerie;
			Color color  = rtswtSerie.getColor();
			gc.setForeground(color);
			gc.setBackground(backgroundColor);
			gc.drawString(valueString, 0, yPosition);
			numSerie++;
		}
		
		gc.dispose();
	}

	protected void checkUpdate() {
		RTSWTOscilloSerie[] series = getSeries();
		boolean redraw = true;
		redrawGrid = false;
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
				redrawGrid = true;
			}
		}
		if(redraw) {
			long t = System.nanoTime();
			if (showGrid && redrawGrid) prepareGrids();
			if(showCurrentValues) prepareCurrentValues();
			prepareSeries();
			getChart().redraw();
			getChart().update();
			drawTime.add(System.nanoTime() - t);
		}
	}
	
	@Override
	public void paintControl(PaintEvent e) {
		int yTop = 0;
		int yHeight = 0;
		int xWidth = 0;
		int xLeft = 0;
		if(showLegend) {
			yTop = (legendPosition == SWT.TOP) ? 0 : getHeight() - getLegendHeight();
			xLeft = getLeftAxisWidth();
			xWidth = getWidth() - getLeftAxisWidth();
			e.gc.drawImage(legendImage, 0, 0, legendImageData.width, legendImageData.height, xLeft, yTop, xWidth, getLegendHeight());
		}
		
		if(showGrid) {
			yTop = (legendPosition == SWT.TOP) ? getLegendHeight() : 0;
			yHeight = getHeight() - getLegendHeight( );
			xWidth = getWidth();
			e.gc.drawImage(gridImage, 0, 0, gridImageData.width, gridImageData.height, 0, yTop, xWidth, yHeight);
		}
		
		yTop = (legendPosition == SWT.TOP) ? getLegendHeight() : 0;
		xLeft = showGrid ? getLeftAxisWidth() : 0;
		xWidth = seriesImageData.width;
		yHeight = seriesImageData.height;
		e.gc.drawImage(seriesImage, 0, 0, seriesImageData.width, seriesImageData.height, xLeft, yTop, xWidth, yHeight);

		if(showCurrentValues) {
			xLeft = getWidth() - currentValuesImageData.width;
			yTop = (showLegend && legendPosition == SWT.TOP) ? getLegendHeight() : 0;
			xWidth = currentValuesImageData.width;
			yHeight = currentValuesImageData.height;
			e.gc.drawImage(currentValuesImage, 0, 0, currentValuesImageData.width, currentValuesImageData.height, xLeft - 5, yTop + 5, xWidth, yHeight);
		}
	}

}