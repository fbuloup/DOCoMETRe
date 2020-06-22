package org.eclipse.swtchart.extensions.charts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;

public class CursorPainter implements ICustomPaintListener {
	
	private InteractiveChart chart;
	private int previousTextWidth;

	public CursorPainter(InteractiveChart chart) {
		this.chart = chart;
	}

	@Override
	public void paintControl(PaintEvent e) {
		String coordinates = chart.getCoordinatesString();
		if(coordinates == null || "".equals(coordinates)) return;
		
		// >>> Cursor coordinates text area 
		Font font = new Font(Display.getCurrent(), "Arial", 14, SWT.BOLD);
        e.gc.setFont(font);
		int textWidth = e.gc.textExtent(coordinates).x;
		int textHeight = e.gc.textExtent(coordinates).y;
		int chartWidth = chart.getPlotArea().getBounds().width;
		int chartHeight = chart.getPlotArea().getBounds().height;
		// Erase previous cursor coordinates text area
		chart.getPlotArea().redraw(chartWidth - previousTextWidth, 0, previousTextWidth, textHeight, true);
		// Draw cursor coordinates text
		Color oldColor = e.gc.getForeground();
		Color newColor = (chart.getCurrentSeries() instanceof ILineSeries) ? ((ILineSeries)chart.getCurrentSeries()).getLineColor() : Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		e.gc.setForeground(newColor);
		e.gc.drawText(coordinates, chartWidth - textWidth, 0);
		
		// >>> Cursor area 
		// Erase previous cursor position area
		int min = Math.min(chart.getPreviousCurrentX_Pixel(), chart.getCurrentX_Pixel());
		int max = Math.max(chart.getPreviousCurrentX_Pixel(), chart.getCurrentX_Pixel());
		int width = max - min + 20;
		chart.getPlotArea().redraw(min - 10, 0, width,chart.getPlotArea().getBounds().height, false);
		// Draw cursor itself
		e.gc.setLineWidth(3);
		e.gc.drawLine(chart.getCurrentX_Pixel(), 0, chart.getCurrentX_Pixel(), chart.getCurrentY_Pixel() - 3);
		e.gc.drawLine(chart.getCurrentX_Pixel(), chart.getCurrentY_Pixel() + 3, chart.getCurrentX_Pixel(), chartHeight);
		e.gc.drawRectangle(chart.getCurrentX_Pixel() - 3, chart.getCurrentY_Pixel() - 3, 6, 6);
		
		e.gc.setForeground(oldColor);
		previousTextWidth = textWidth;
	}

	@Override
	public boolean drawBehindSeries() {
		return false;
	}

}
