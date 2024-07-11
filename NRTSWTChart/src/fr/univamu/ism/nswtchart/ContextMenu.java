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
	
	private MenuItem showLegendMenuItem;
	private MenuItem legendPositionsTopMenuItem;
	private MenuItem legendPositionsBottomMenuItem;
	private MenuItem resetScaleMenu;
	private MenuItem resetXScaleMenu;
	private MenuItem resetYScaleMenu;
	private MenuItem showGridMenu;
	private MenuItem showAxisMenu;
	private MenuItem showCursorMenu;
	
	private XYSWTChart chart;
	private Menu menu;

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
		
		resetScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetScaleMenu.setText(RTSWTChartMessages.ResetScale);
		resetScaleMenu.addSelectionListener(new ResetScaleHandler());
		
		resetXScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetXScaleMenu.setText(RTSWTChartMessages.ResetXScale);
		resetXScaleMenu.addSelectionListener(new ResetXScaleHandler());
		
		resetYScaleMenu = new MenuItem(menu, SWT.NORMAL);
		resetYScaleMenu.setText(RTSWTChartMessages.ResetYScale);
		resetYScaleMenu.addSelectionListener(new ResetYScaleHandler());
		
		showGridMenu = new MenuItem(menu, SWT.CHECK);
		showGridMenu.setText(RTSWTChartMessages.showGrid);
		showGridMenu.addSelectionListener(new ShowGridHandler());
		
		showAxisMenu = new MenuItem(menu, SWT.CHECK);
		showAxisMenu.setText(RTSWTChartMessages.showAxis);
		showAxisMenu.addSelectionListener(new ShowAxisHandler());
		
		showCursorMenu = new MenuItem(menu, SWT.CHECK);
		showCursorMenu.setText(RTSWTChartMessages.showCursor);
		showCursorMenu.addSelectionListener(new ShowCursorHandler());
		
		menu.addMenuListener(new MenuListenerHandler());
		
	}
	
	public void updateShowCursorMenu(boolean showCursor) {
		showCursorMenu.setSelection(showCursor);
	}
	
	public Menu getMenu() {
		return menu;
	}

}
