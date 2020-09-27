/*******************************************************************************
 * Copyright (c) 2008, 2018 SWTChart project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * yoshitaka - initial API and implementation
 *******************************************************************************/
package org.eclipse.swtchart.extensions.charts;

import java.text.NumberFormat;
import java.util.Arrays;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.IAxis.Direction;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.extensions.properties.AxisPage;
import org.eclipse.swtchart.extensions.properties.AxisTickPage;
import org.eclipse.swtchart.extensions.properties.ChartPage;
import org.eclipse.swtchart.extensions.properties.GridPage;
import org.eclipse.swtchart.extensions.properties.LegendPage;
import org.eclipse.swtchart.extensions.properties.PropertiesResources;
import org.eclipse.swtchart.extensions.properties.SeriesLabelPage;
import org.eclipse.swtchart.extensions.properties.SeriesPage;

/**
 * An interactive chart which provides the following abilities.
 * <ul>
 * <li>scroll with arrow keys</li>
 * <li>zoom in and out with ctrl + arrow up/down keys</li>
 * <li>context menus for adjusting axis range and zooming in/out.</li>
 * <li>file selector dialog to save chart to image file.</li>
 * <li>properties dialog to configure the chart settings</li>
 * </ul>
 */
public class InteractiveChart extends Chart implements PaintListener {

	/** the filter extensions */
	private static final String[] EXTENSIONS = new String[]{"*.jpeg", "*.jpg", "*.png"};
	/** the selection rectangle for zoom in/out */
	protected SelectionRectangle selection;
	private Color selectionColor;
	/** the clicked time in milliseconds */
	private long clickedTime;
	/** the resources created with properties dialog */
	private PropertiesResources resources;

	private int currentX_Pixel;
	private int currentY_Pixel;
	private double currentX;
	private double currentY;
	private String cursorCoordinatesString = "";
	private CursorMarkerDeltaPainter cursorMarkerDeltaPainter;
	private boolean showCursor = true;
	private int previousCurrentX_Pixel; 

	private boolean showMarker = true;
	private int currentXMarker_Pixel = -1;
	private int currentYMarker_Pixel = -1;
	private String markerCoordinatesString = "";
	private String deltaCoordinateString = "";
	private boolean doubleClick;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            the style
	 */
	public InteractiveChart(Composite parent, int style) {
		super(parent, style);
		init();
	}

	/**
	 * Initializes.
	 */
	private void init() {
		resources = new PropertiesResources();
		Composite plot = getPlotArea();
		plot.addListener(SWT.Resize, this);
		plot.addListener(SWT.MouseMove, this);
		plot.addListener(SWT.MouseDown, this);
		plot.addListener(SWT.MouseUp, this);
		plot.addListener(SWT.MouseWheel, this);
		plot.addListener(SWT.KeyDown, this);
		plot.addListener(SWT.MouseDoubleClick, this);
		plot.addPaintListener(this);
		cursorMarkerDeltaPainter = new CursorMarkerDeltaPainter(this);
		((IPlotArea)plot).addCustomPaintListener(cursorMarkerDeltaPainter);
		getPlotArea().setCursor(new Cursor(Display.getDefault(), SWT.CURSOR_CROSS));
		createMenuItems();
	}
	
	public void setShowCursor(boolean showCursor) {
		this.showCursor = showCursor;
	}
	
	public boolean isShowCursor() {
		return showCursor && getSeriesSet().getSeries().length > 0;
	}

	public void setShowMarker(boolean showMarker) {
		this.showMarker = showMarker;
	}
	
	public boolean isShowMarker() {
		return showMarker && currentXMarker_Pixel > -1;
	}
	
	/**
	 * Creates menu items.
	 */
	private void createMenuItems() {

		Menu menu = new Menu(getPlotArea());
		getPlotArea().setMenu(menu);
		// adjust axis range menu group
		MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText(Messages.ADJUST_AXIS_RANGE_GROUP);
		Menu adjustAxisRangeMenu = new Menu(menuItem);
		menuItem.setMenu(adjustAxisRangeMenu);
		// adjust axis range
		menuItem = new MenuItem(adjustAxisRangeMenu, SWT.PUSH);
		menuItem.setText(Messages.ADJUST_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		// adjust X axis range
		menuItem = new MenuItem(adjustAxisRangeMenu, SWT.PUSH);
		menuItem.setText(Messages.ADJUST_X_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		// adjust Y axis range
		menuItem = new MenuItem(adjustAxisRangeMenu, SWT.PUSH);
		menuItem.setText(Messages.ADJUST_Y_AXIS_RANGE);
		menuItem.addListener(SWT.Selection, this);
		menuItem = new MenuItem(menu, SWT.SEPARATOR);
		// zoom in menu group
		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText(Messages.ZOOMIN_GROUP);
		Menu zoomInMenu = new Menu(menuItem);
		menuItem.setMenu(zoomInMenu);
		// zoom in both axes
		menuItem = new MenuItem(zoomInMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMIN);
		menuItem.addListener(SWT.Selection, this);
		// zoom in X axis
		menuItem = new MenuItem(zoomInMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMIN_X);
		menuItem.addListener(SWT.Selection, this);
		// zoom in Y axis
		menuItem = new MenuItem(zoomInMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMIN_Y);
		menuItem.addListener(SWT.Selection, this);
		// zoom out menu group
		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText(Messages.ZOOMOUT_GROUP);
		Menu zoomOutMenu = new Menu(menuItem);
		menuItem.setMenu(zoomOutMenu);
		// zoom out both axes
		menuItem = new MenuItem(zoomOutMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMOUT);
		menuItem.addListener(SWT.Selection, this);
		// zoom out X axis
		menuItem = new MenuItem(zoomOutMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMOUT_X);
		menuItem.addListener(SWT.Selection, this);
		// zoom out Y axis
		menuItem = new MenuItem(zoomOutMenu, SWT.PUSH);
		menuItem.setText(Messages.ZOOMOUT_Y);
		menuItem.addListener(SWT.Selection, this);
		menuItem = new MenuItem(menu, SWT.SEPARATOR);
		// save as
		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(Messages.SAVE_AS);
		menuItem.addListener(SWT.Selection, this);
		menuItem = new MenuItem(menu, SWT.SEPARATOR);
		// properties
		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(Messages.PROPERTIES);
		menuItem.addListener(SWT.Selection, this);
	}
	
	public MenuItem getMenuItem(Menu menu, String ID) {
		if(menu == null) {
			Control plotArea = (Control)getPlotArea();
			menu = plotArea.getMenu();
		}
		MenuItem[] items = menu.getItems();
		MenuItem foundMenuItem = null;
		for (MenuItem menuItem : items) {
			if(menuItem.getText().equals(ID)) {
				foundMenuItem = menuItem;
				break;
			}
		}
		if(foundMenuItem == null) {
			for (MenuItem menuItem : items) {
				if(menuItem.getMenu() != null) {
					foundMenuItem = getMenuItem(menuItem.getMenu(), ID);
					if(foundMenuItem != null) break;
				}
			}
		}
		return foundMenuItem;
	}

	/*
	 * @see PaintListener#paintControl(PaintEvent)
	 */
	public void paintControl(PaintEvent e) {
		if(selection != null && !selection.isDisposed()) selection.draw(e.gc);
	}

	/*
	 * @see Listener#handleEvent(Event)
	 */
	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		switch(event.type) {
			case SWT.MouseMove:
				handleMouseMoveEvent(event);
				break;
			case SWT.MouseDown:
				handleMouseDownEvent(event);
				break;
			case SWT.MouseUp:
				handleMouseUpEvent(event);
				break;
			case SWT.MouseWheel:
				handleMouseWheel(event);
				resetMarker(event);
				break;
			case SWT.MouseDoubleClick:
				handleMouseDoubleClick(event);
				break;
			case SWT.KeyDown:
				handleKeyDownEvent(event);
				break;
			case SWT.Selection:
				handleSelectionEvent(event);
				resetMarker(event);
				break;
			default:
				resetMarker(event);
				break;
		}
	}

	/*
	 * @see Chart#dispose()
	 */
	@Override
	public void dispose() {

		super.dispose();
		resources.dispose();
	}

	/**
	 * Handles mouse move event.
	 * 
	 * @param event
	 *            the mouse move event
	 */
	private void handleMouseMoveEvent(Event event) {
		getPlotArea().setFocus();
		if(selection != null && !selection.isDisposed()) {
			selection.setEndPoint(event.x, event.y);
			if(!doubleClick) 
				resetMarker(event);
			doubleClick = false;
			redraw();
		} else {
			if (isShowCursor()) {
				// Compute current coordinates
				computeCurrentCordinates(event);
				// Convert to String
				cursorCoordinatesString = convertToString(1, currentX, currentY);
			} else cursorCoordinatesString = "";
			if(isShowMarker()) {
				double mx = getAxisSet().getXAxes()[0].getDataCoordinate(currentXMarker_Pixel);
				double my = getAxisSet().getYAxes()[0].getDataCoordinate(currentYMarker_Pixel);
				markerCoordinatesString = convertToString(2, mx, my);
				double x = getCurrentX() - mx;
				double y = getCurrentY() - my;
				deltaCoordinateString = convertToString(3, x, y);
			}
			if(isShowCursor() || isShowMarker()) {
				boolean disposeGC = false;
				if(event.gc == null) {
					event.gc = new GC(getPlotArea());
					disposeGC = true;
				}
				PaintEvent paintEvent = new PaintEvent(event);
				cursorMarkerDeltaPainter.paintControl(paintEvent);
				if(disposeGC) event.gc.dispose();
			}
		}
		
	}
	
	public String getCursorCoordinatesString() {
		return cursorCoordinatesString;
	}
	
	public String getMarkerCoordinatesString() {
		return markerCoordinatesString;
	}
	
	public String getDeltaCoordinateString() {
		return deltaCoordinateString;
	}
	
	private String convertToString(int type, double x, double y) {
		if(getCurrentSeries() == null) return "";
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(6);
		
		StringBuilder text = new StringBuilder();
		if(type == 2) text.append("Marker (");
		else if(type == 1)  text.append("Cursor (");
		else if(type == 3) text.append("\u0394 (");
		text.append(nf.format(x));
		text.append(" ; ");
		text.append(nf.format(y));
		text.append(")");
		
		
		return text.toString();
	}
	
	public int getCurrentX_Pixel() {
		return currentX_Pixel;
	}

	public int getCurrentY_Pixel() {
		return currentY_Pixel;
	}
	
	public int getPreviousCurrentX_Pixel() {
		return previousCurrentX_Pixel;
	}
	
	public double getCurrentX() {
		return currentX;
	}
	
	public double getCurrentY() {
		return currentY;
	}
	
	public int getCurrentXMarker_Pixel() {
		return currentXMarker_Pixel;
	}
	
	public int getCurrentYMarker_Pixel() {
		return currentYMarker_Pixel;
	}
	
	private void computeCurrentCordinates(Event event) {
		currentX = Double.NaN;
		currentY = Double.NaN;
		if(getCurrentSeries() == null) return;
		previousCurrentX_Pixel = currentX_Pixel;
		currentX_Pixel = event.x;
		double x = getAxisSet().getXAxes()[0].getDataCoordinate(event.x);
		int index = Arrays.binarySearch(getCurrentSeries().getXSeries(), x);
		double y = Double.NaN;
		if(index < 0) {
			index = - index - 1;
			if(index > 0 && index < getCurrentSeries().getXSeries().length) {
				double y1  = getCurrentSeries().getYSeries()[index - 1];
				double y2  = getCurrentSeries().getYSeries()[index];
				double x1  = getCurrentSeries().getXSeries()[index - 1];
				double x2  = getCurrentSeries().getXSeries()[index];
				y = (y2 -y1)/(x2 - x1)*(x - x1) + y1; 
			}
		} else {
			if(index >= 0 && index < getCurrentSeries().getXSeries().length) {
				y = getCurrentSeries().getYSeries()[index];
			}
		}
		currentY_Pixel = getAxisSet().getYAxes()[0].getPixelCoordinate(y);
		currentX = x;
		currentY = y;
	}
	
	private void handleMouseDoubleClick(Event event) {
		doubleClick = true;
		currentXMarker_Pixel = getCurrentX_Pixel();
		currentYMarker_Pixel = getCurrentY_Pixel();

		double mx = getAxisSet().getXAxes()[0].getDataCoordinate(currentXMarker_Pixel);
		
		double[] xValues = getCurrentSeries().getXSeries();
		double[] yValues = getCurrentSeries().getYSeries();
		
		int index = -1;
		for (int i = 1; i < xValues.length; i++) {
			
			double signum_i = Math.signum(mx - xValues[i]);
			double signum_i_1 = Math.signum(mx - xValues[i-1]);
			
			if(signum_i == 0) index = i;
			else if(signum_i_1 == 0) index = i - 1;
			else if(signum_i != signum_i_1) {
				index = i;
				double d1 = mx - xValues[i];
				double d2 = mx - xValues[i - 1];
				if(Math.abs(d2) < Math.abs(d1)) index = index - 1;
			}
			
			if(index > -1) break;
							
		}
		
		currentXMarker_Pixel = getAxisSet().getXAxes()[0].getPixelCoordinate(xValues[index]);
		currentYMarker_Pixel = getAxisSet().getYAxes()[0].getPixelCoordinate(yValues[index]);
		
		handleMouseMoveEvent(event);
	}
	
	private void resetMarker(Event event) {
		currentXMarker_Pixel = -1;
		event.x = getCurrentX_Pixel();
//		handleMouseMoveEvent(event);
	}

	/**
	 * Handles the mouse down event.
	 * 
	 * @param event
	 *            the mouse down event
	 */
	private void handleMouseDownEvent(Event event) {

		if(event.button == 1) {
			selection = new SelectionRectangle();
			selection.setColor(selectionColor);
			selection.setStartPoint(event.x, event.y);
			clickedTime = System.currentTimeMillis();
		}
	}

	/**
	 * Handles the mouse up event.
	 * 
	 * @param event
	 *            the mouse up event
	 */
	private void handleMouseUpEvent(Event event) {

		if(event.button == 1 && System.currentTimeMillis() - clickedTime > 100) {
			for(IAxis axis : getAxisSet().getAxes()) {
				Point range = null;
				if((getOrientation() == SWT.HORIZONTAL && axis.getDirection() == Direction.X) || (getOrientation() == SWT.VERTICAL && axis.getDirection() == Direction.Y)) {
					range = selection.getHorizontalRange();
				} else {
					range = selection.getVerticalRange();
				}
				if(range != null && range.x != range.y) {
					setRange(range, axis);
				}
			}
		}
		selection.dispose();
		redraw();
		if (isShowCursor()) handleMouseMoveEvent(event);
	}

	/**
	 * Handles mouse wheel event.
	 * 
	 * @param event
	 *            the mouse wheel event
	 */
	private void handleMouseWheel(Event event) {

		for(IAxis axis : getAxes(SWT.HORIZONTAL)) {
			double coordinate = axis.getDataCoordinate(event.x);
			if(event.count > 0) {
				axis.zoomIn(coordinate);
			} else {
				axis.zoomOut(coordinate);
			}
		}
		for(IAxis axis : getAxes(SWT.VERTICAL)) {
			double coordinate = axis.getDataCoordinate(event.y);
			if(event.count > 0) {
				axis.zoomIn(coordinate);
			} else {
				axis.zoomOut(coordinate);
			}
		}
		redraw();
		if (isShowCursor()) handleMouseMoveEvent(event);

	}

	/**
	 * Handles the key down event.
	 * 
	 * @param event
	 *            the key down event
	 */
	private void handleKeyDownEvent(Event event) {

		if(event.keyCode == SWT.ARROW_DOWN) {
			if(event.stateMask == SWT.CTRL) {
				getAxisSet().zoomOut();
			} else {
				for(IAxis axis : getAxes(SWT.VERTICAL)) {
					axis.scrollDown();
				}
			}
			redraw();
		} else if(event.keyCode == SWT.ARROW_UP) {
			if(event.stateMask == SWT.CTRL) {
				getAxisSet().zoomIn();
			} else {
				for(IAxis axis : getAxes(SWT.VERTICAL)) {
					axis.scrollUp();
				}
			}
			redraw();
		} else if(event.keyCode == SWT.ARROW_LEFT) {
			for(IAxis axis : getAxes(SWT.HORIZONTAL)) {
				axis.scrollDown();
			}
			redraw();
		} else if(event.keyCode == SWT.ARROW_RIGHT) {
			for(IAxis axis : getAxes(SWT.HORIZONTAL)) {
				axis.scrollUp();
			}
			redraw();
		} else if(event.keyCode == SWT.TAB) {
			if (isShowCursor()) {
				resetMarker(event);
				if (getSeriesSet().getSeries().length > 1) {
					ISeries[] series = getSeriesSet().getSeries();
					for (int i = 0; i < series.length; i++) {
						if (getCurrentSeries() == series[i]) {
							int index = 0;
							if ((event.stateMask & SWT.SHIFT) > 0)
								index = i - 1;
							else
								index = (i + 1) % series.length;
							if (index < 0)
								index = series.length - 1;
							setCurrentSeries(series[index]);
							break;
						}
					}
				} 
			} 
		} 
		if (isShowCursor()) {
			event.x = currentX_Pixel;
			handleMouseMoveEvent(event);
		}
	}

	/**
	 * Gets the axes for given orientation.
	 * 
	 * @param orientation
	 *            the orientation
	 * @return the axes
	 */
	private IAxis[] getAxes(int orientation) {

		IAxis[] axes;
		if(getOrientation() == orientation) {
			axes = getAxisSet().getXAxes();
		} else {
			axes = getAxisSet().getYAxes();
		}
		return axes;
	}

	/**
	 * Handles the selection event.
	 * 
	 * @param event
	 *            the event
	 */
	private void handleSelectionEvent(Event event) {

		if(!(event.widget instanceof MenuItem)) {
			return;
		}
		MenuItem menuItem = (MenuItem)event.widget;
		if(menuItem.getText().equals(Messages.ADJUST_AXIS_RANGE)) {
			getAxisSet().adjustRange();
		} else if(menuItem.getText().equals(Messages.ADJUST_X_AXIS_RANGE)) {
			for(IAxis axis : getAxisSet().getXAxes()) {
				axis.adjustRange();
			}
		} else if(menuItem.getText().equals(Messages.ADJUST_Y_AXIS_RANGE)) {
			for(IAxis axis : getAxisSet().getYAxes()) {
				axis.adjustRange();
			}
		} else if(menuItem.getText().equals(Messages.ZOOMIN)) {
			getAxisSet().zoomIn();
		} else if(menuItem.getText().equals(Messages.ZOOMIN_X)) {
			for(IAxis axis : getAxisSet().getXAxes()) {
				axis.zoomIn();
			}
		} else if(menuItem.getText().equals(Messages.ZOOMIN_Y)) {
			for(IAxis axis : getAxisSet().getYAxes()) {
				axis.zoomIn();
			}
		} else if(menuItem.getText().equals(Messages.ZOOMOUT)) {
			getAxisSet().zoomOut();
		} else if(menuItem.getText().equals(Messages.ZOOMOUT_X)) {
			for(IAxis axis : getAxisSet().getXAxes()) {
				axis.zoomOut();
			}
		} else if(menuItem.getText().equals(Messages.ZOOMOUT_Y)) {
			for(IAxis axis : getAxisSet().getYAxes()) {
				axis.zoomOut();
			}
		} else if(menuItem.getText().equals(Messages.SAVE_AS)) {
			openSaveAsDialog();
		} else if(menuItem.getText().equals(Messages.PROPERTIES)) {
			openPropertiesDialog();
		}
		redraw();
		if (isShowCursor()) {
			event.x = currentX_Pixel;
			handleMouseMoveEvent(event);
		}
	}

	/**
	 * Opens the Save As dialog.
	 */
	private void openSaveAsDialog() {

		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(Messages.SAVE_AS_DIALOG_TITLE);
		dialog.setFilterExtensions(EXTENSIONS);
		String filename = dialog.open();
		if(filename == null) {
			return;
		}
		int format;
		if(filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			format = SWT.IMAGE_JPEG;
		} else if(filename.endsWith(".png")) {
			format = SWT.IMAGE_PNG;
		} else {
			format = SWT.IMAGE_UNDEFINED;
		}
		if(format != SWT.IMAGE_UNDEFINED) {
			save(filename, format);
		}
	}

	/**
	 * Opens the properties dialog.
	 */
	private void openPropertiesDialog() {

		PreferenceManager manager = new PreferenceManager();
		final String chartTitle = "Chart";
		PreferenceNode chartNode = new PreferenceNode(chartTitle);
		chartNode.setPage(new ChartPage(this, resources, chartTitle));
		manager.addToRoot(chartNode);
		final String legendTitle = "Legend";
		PreferenceNode legendNode = new PreferenceNode(legendTitle);
		legendNode.setPage(new LegendPage(this, resources, legendTitle));
		manager.addTo(chartTitle, legendNode);
		final String xAxisTitle = "X Axis";
		PreferenceNode xAxisNode = new PreferenceNode(xAxisTitle);
		xAxisNode.setPage(new AxisPage(this, resources, Direction.X, xAxisTitle));
		manager.addTo(chartTitle, xAxisNode);
		final String gridTitle = "Grid";
		PreferenceNode xGridNode = new PreferenceNode(gridTitle);
		xGridNode.setPage(new GridPage(this, resources, Direction.X, gridTitle));
		manager.addTo(chartTitle + "." + xAxisTitle, xGridNode);
		final String tickTitle = "Tick";
		PreferenceNode xTickNode = new PreferenceNode(tickTitle);
		xTickNode.setPage(new AxisTickPage(this, resources, Direction.X, tickTitle));
		manager.addTo(chartTitle + "." + xAxisTitle, xTickNode);
		final String yAxisTitle = "Y Axis";
		PreferenceNode yAxisNode = new PreferenceNode(yAxisTitle);
		yAxisNode.setPage(new AxisPage(this, resources, Direction.Y, yAxisTitle));
		manager.addTo(chartTitle, yAxisNode);
		PreferenceNode yGridNode = new PreferenceNode(gridTitle);
		yGridNode.setPage(new GridPage(this, resources, Direction.Y, gridTitle));
		manager.addTo(chartTitle + "." + yAxisTitle, yGridNode);
		PreferenceNode yTickNode = new PreferenceNode(tickTitle);
		yTickNode.setPage(new AxisTickPage(this, resources, Direction.Y, tickTitle));
		manager.addTo(chartTitle + "." + yAxisTitle, yTickNode);
		final String seriesTitle = "Series";
		PreferenceNode plotNode = new PreferenceNode(seriesTitle);
		plotNode.setPage(new SeriesPage(this, resources, seriesTitle));
		manager.addTo(chartTitle, plotNode);
		final String labelTitle = "Label";
		PreferenceNode labelNode = new PreferenceNode(labelTitle);
		labelNode.setPage(new SeriesLabelPage(this, resources, labelTitle));
		manager.addTo(chartTitle + "." + seriesTitle, labelNode);
		PreferenceDialog dialog = new PreferenceDialog(getShell(), manager);
		dialog.create();
		dialog.getShell().setText("Properties");
		dialog.getTreeViewer().expandAll();
		dialog.open();
	}

	/**
	 * Sets the axis range.
	 * 
	 * @param range
	 *            the axis range in pixels
	 * @param axis
	 *            the axis to set range
	 */
	private void setRange(Point range, IAxis axis) {

		if(range == null) {
			return;
		}
		double min = axis.getDataCoordinate(range.x);
		double max = axis.getDataCoordinate(range.y);
		axis.setRange(new Range(min, max));
	}

	public void setSelectionRectangleColor(Color color) {
		selectionColor = color;
	}
	
	public void removeSeries(String seriesID) {
		if(getSeriesSet().getSeries(seriesID) != null) {
			getSeriesSet().deleteSeries(seriesID);
		}
		if(getCurrentSeries() != null && getCurrentSeries().getId().equals(seriesID)) {
			if(getSeriesSet().getSeries().length > 0) setCurrentSeries(getSeriesSet().getSeries()[0]);
			else {
				setCurrentSeries(null);
			}
		}
		Event event = new Event();
		event.widget = getPlotArea();
		event.x = getCurrentX_Pixel();
		handleMouseMoveEvent(event);
	}
}
