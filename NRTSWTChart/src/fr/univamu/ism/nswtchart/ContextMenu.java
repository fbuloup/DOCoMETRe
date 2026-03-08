package fr.univamu.ism.nswtchart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import fr.univamu.ism.nrtswtchart.RTSWTChartMessages;

public class ContextMenu {
	
	private final class MenuListenerHandler extends MenuAdapter {
		public void menuShown(MenuEvent e) {
			showLegendMenuItem.setSelection(chart.isShowLegend());
			legendPositionsBottomMenuItem.setSelection(chart.isLegendPositionBottom());
			legendPositionsTopMenuItem.setSelection(!chart.isLegendPositionBottom());
			showGridMenu.setSelection(chart.isShowGrid());
			showAxisMenu.setSelection(chart.isShowAxis());
			showCursorMenu.setSelection(chart.isShowCursor());
			showSamplesMenu.setSelection(chart.isShowSamples());
			squareSymbolMenuItem.setSelection(chart.isSampleSymbolSquare());
			roundSymbolMenuItem.setSelection(chart.isSampleSymbolRound());
			size1MenuItem.setSelection(chart.getSampleSymbolSize() == 1);
			size2MenuItem.setSelection(chart.getSampleSymbolSize() == 3);
			size3MenuItem.setSelection(chart.getSampleSymbolSize() == 5);
			size4MenuItem.setSelection(chart.getSampleSymbolSize() == 7);
			size5MenuItem.setSelection(chart.getSampleSymbolSize() == 9);
			thickness1MenuItem.setSelection(chart.getSeriesThickness() == 1);
			thickness2MenuItem.setSelection(chart.getSeriesThickness() == 2);
			thickness3MenuItem.setSelection(chart.getSeriesThickness() == 3);
			thickness4MenuItem.setSelection(chart.getSeriesThickness() == 4);
			thickness5MenuItem.setSelection(chart.getSeriesThickness() == 5);
		}
	}
	
	private final class LegendPositionHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			if (((MenuItem) e.widget) == legendPositionsBottomMenuItem &&  !chart.isLegendPositionBottom()) chart.setLegendPosition(SWT.BOTTOM);
			if (((MenuItem) e.widget) == legendPositionsTopMenuItem &&  chart.isLegendPositionBottom()) chart.setLegendPosition(SWT.TOP);
			chart.redraw();
			chart.update();
		}
	}

	private final class ShowLegendHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean showLegend = ((MenuItem) e.widget).getSelection();
			chart.setShowLegend(showLegend);
			chart.redraw();
			chart.update();
		}
	}

	private final class ResetScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			chart.reset();
			chart.redraw();
			chart.update();
		}
	}
	
	private final class ResetXScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			chart.resetX();
			chart.redraw();
			chart.update();
		}
	}
	
	private final class ResetYScaleHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			chart.resetY();
			chart.redraw();
			chart.update();
		}
	}

	private final class ShowGridHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean showGrid = ((MenuItem) e.widget).getSelection();
			chart.setShowGrid(showGrid);
			chart.redraw();
			chart.update();
		}
	}
	
	private final class ShowAxisHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean showAxis = ((MenuItem) e.widget).getSelection();
			chart.setShowAxis(showAxis);
			chart.redraw();
			chart.update();
		}
	}
	
	private final class ShowCursorHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean showCursor = ((MenuItem) e.widget).getSelection();
			chart.setShowCursor(showCursor);
			chart.redraw();
			chart.update();
		}
	}
	
	private final class ShowSamplesHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			boolean showSamples = ((MenuItem) e.widget).getSelection();
			chart.setShowSamples(showSamples);
			chart.redraw();
			chart.update();
		}
	}
	
	private final class SymbolSamplesHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			if (((MenuItem) e.widget) == squareSymbolMenuItem &&  !chart.isSampleSymbolSquare()) chart.setSampleSymbol(XYSWTChart.SQUARE);
			if (((MenuItem) e.widget) == roundSymbolMenuItem &&  !chart.isSampleSymbolRound()) chart.setSampleSymbol(XYSWTChart.ROUND);
			chart.setShowSamples(true);
			chart.redraw();
			chart.update();
		}
	}
	
	private final class SymbolSizeHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			chart.setSampleSymbolSize(((MenuItem) e.widget).getID());
			chart.redraw();
			chart.update();
		}
	}
	
	private final class SeriesThicknessHandler extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			chart.setSeriesThickness(((MenuItem) e.widget).getID());
			chart.redraw();
			chart.update();
		}
	}
	
	private MenuItem showLegendMenuItem;
	private MenuItem legendPositionsTopMenuItem;
	private MenuItem legendPositionsBottomMenuItem;
	private MenuItem resetScaleMenu;
	private MenuItem resetXScaleMenu;
	private MenuItem resetYScaleMenu;
	private MenuItem showGridMenu;
	private MenuItem showAxisMenu;
	private MenuItem showCursorMenu;
	private MenuItem showSamplesMenu;
	private MenuItem squareSymbolMenuItem;
	private MenuItem roundSymbolMenuItem;
	private Menu menu;
	
	private XYSWTChart chart;
	private MenuItem size1MenuItem;
	private MenuItem size2MenuItem;
	private MenuItem size3MenuItem;
	private MenuItem size4MenuItem;
	private MenuItem size5MenuItem;
	private MenuItem thickness1MenuItem;
	private MenuItem thickness2MenuItem;
	private MenuItem thickness3MenuItem;
	private MenuItem thickness4MenuItem;
	private MenuItem thickness5MenuItem;

	public ContextMenu(Control parent) {
		
		menu = new Menu(parent);

		this.chart = (XYSWTChart) parent;
		
		showLegendMenuItem = new MenuItem(menu, SWT.CHECK);
		showLegendMenuItem.setText(RTSWTChartMessages.ShowLegend);
		showLegendMenuItem.addSelectionListener(new ShowLegendHandler());
		
		MenuItem legendPositionsMenuItem = new MenuItem(menu, SWT.CASCADE);
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
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		resetScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetScaleMenu.setText(RTSWTChartMessages.ResetScale);
		resetScaleMenu.addSelectionListener(new ResetScaleHandler());
		
		resetXScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetXScaleMenu.setText(RTSWTChartMessages.ResetXScale);
		resetXScaleMenu.addSelectionListener(new ResetXScaleHandler());
		
		resetYScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetYScaleMenu.setText(RTSWTChartMessages.ResetYScale);
		resetYScaleMenu.addSelectionListener(new ResetYScaleHandler());
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		showGridMenu = new MenuItem(menu, SWT.CHECK);
		showGridMenu.setText(RTSWTChartMessages.showGrid);
		showGridMenu.addSelectionListener(new ShowGridHandler());
		
		showAxisMenu = new MenuItem(menu, SWT.CHECK);
		showAxisMenu.setText(RTSWTChartMessages.showAxis);
		showAxisMenu.addSelectionListener(new ShowAxisHandler());
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		showCursorMenu = new MenuItem(menu, SWT.CHECK);
		showCursorMenu.setText(RTSWTChartMessages.showCursor);
		showCursorMenu.addSelectionListener(new ShowCursorHandler());
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		
		MenuItem seriesThicknessMenuItem = new MenuItem(menu, SWT.CASCADE);
		seriesThicknessMenuItem.setText(RTSWTChartMessages.SeriesThickness);
		Menu seriesThicknessMenu = new Menu(seriesThicknessMenuItem);
		thickness1MenuItem = new MenuItem(seriesThicknessMenu, SWT.CHECK);
		thickness1MenuItem.setID(1);
		thickness1MenuItem.setText(RTSWTChartMessages.Size1);
		thickness2MenuItem = new MenuItem(seriesThicknessMenu, SWT.CHECK);
		thickness2MenuItem.setID(2);
		thickness2MenuItem.setText(RTSWTChartMessages.Size2);
		thickness3MenuItem = new MenuItem(seriesThicknessMenu, SWT.CHECK);
		thickness3MenuItem.setID(3);
		thickness3MenuItem.setText(RTSWTChartMessages.Size3);
		thickness4MenuItem = new MenuItem(seriesThicknessMenu, SWT.CHECK);
		thickness4MenuItem.setID(4);
		thickness4MenuItem.setText(RTSWTChartMessages.Size4);
		thickness5MenuItem = new MenuItem(seriesThicknessMenu, SWT.CHECK);
		thickness5MenuItem.setID(5);
		thickness5MenuItem.setText(RTSWTChartMessages.Size5);
		SeriesThicknessHandler seriesThicknessHandler = new SeriesThicknessHandler();
		thickness1MenuItem.addSelectionListener(seriesThicknessHandler);
		thickness2MenuItem.addSelectionListener(seriesThicknessHandler);
		thickness3MenuItem.addSelectionListener(seriesThicknessHandler);
		thickness4MenuItem.addSelectionListener(seriesThicknessHandler);
		thickness5MenuItem.addSelectionListener(seriesThicknessHandler);
		seriesThicknessMenuItem.setMenu(seriesThicknessMenu);
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		showSamplesMenu = new MenuItem(menu, SWT.CHECK);
		showSamplesMenu.setText(RTSWTChartMessages.showSamples);
		showSamplesMenu.addSelectionListener(new ShowSamplesHandler());
		
		MenuItem symbolSamplesMenuItem = new MenuItem(menu, SWT.CASCADE);
		symbolSamplesMenuItem.setText(RTSWTChartMessages.SymbolSample);
		Menu symbolSampleMenu = new Menu(symbolSamplesMenuItem);
		squareSymbolMenuItem = new MenuItem(symbolSampleMenu, SWT.CHECK);
		squareSymbolMenuItem.setText(RTSWTChartMessages.SquareSymbol);
		roundSymbolMenuItem = new MenuItem(symbolSampleMenu, SWT.CHECK);
		roundSymbolMenuItem.setText(RTSWTChartMessages.RoundSymbol);
		SymbolSamplesHandler symbolSamplesHandler = new SymbolSamplesHandler();
		squareSymbolMenuItem.addSelectionListener(symbolSamplesHandler);
		roundSymbolMenuItem.addSelectionListener(symbolSamplesHandler);
		symbolSamplesMenuItem.setMenu(symbolSampleMenu);
		
		MenuItem symbolSamplesSizeMenuItem = new MenuItem(menu, SWT.CASCADE);
		symbolSamplesSizeMenuItem.setText(RTSWTChartMessages.SymbolSampleSize);
		Menu symbolSizeMenu = new Menu(symbolSamplesSizeMenuItem);
		size1MenuItem = new MenuItem(symbolSizeMenu, SWT.CHECK);
		size1MenuItem.setID(1);
		size1MenuItem.setText(RTSWTChartMessages.Size1);
		size2MenuItem = new MenuItem(symbolSizeMenu, SWT.CHECK);
		size2MenuItem.setID(2);
		size2MenuItem.setText(RTSWTChartMessages.Size2);
		size3MenuItem = new MenuItem(symbolSizeMenu, SWT.CHECK);
		size3MenuItem.setID(3);
		size3MenuItem.setText(RTSWTChartMessages.Size3);
		size4MenuItem = new MenuItem(symbolSizeMenu, SWT.CHECK);
		size4MenuItem.setID(4);
		size4MenuItem.setText(RTSWTChartMessages.Size4);
		size5MenuItem = new MenuItem(symbolSizeMenu, SWT.CHECK);
		size5MenuItem.setID(5);
		size5MenuItem.setText(RTSWTChartMessages.Size5);
		SymbolSizeHandler symbolSizeHandler = new SymbolSizeHandler();
		size1MenuItem.addSelectionListener(symbolSizeHandler);
		size2MenuItem.addSelectionListener(symbolSizeHandler);
		size3MenuItem.addSelectionListener(symbolSizeHandler);
		size4MenuItem.addSelectionListener(symbolSizeHandler);
		size5MenuItem.addSelectionListener(symbolSizeHandler);
		symbolSamplesSizeMenuItem.setMenu(symbolSizeMenu);
		
		menu.addMenuListener(new MenuListenerHandler());
		
	}
	
	public void updateShowCursorMenu(boolean showCursor) {
		showCursorMenu.setSelection(showCursor);
	}
	
	public Menu getMenu() {
		return menu;
	}

}
