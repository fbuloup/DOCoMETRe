package org.eclipse.swtchart.extensions.charts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;

public class CursorMarkerDeltaPainter implements ICustomPaintListener {
	
	private InteractiveChart chart;
	private int previousCursorTextWidth;
	private int previousMarkerTextWidth;
	private int previousDeltaTextWidth;

	public CursorMarkerDeltaPainter(InteractiveChart chart) {
		this.chart = chart;
	}

	@Override
	public void paintControl(PaintEvent e) {
		String cursorCoordinates = chart.getCursorCoordinatesString();
		if(cursorCoordinates == null || "".equals(cursorCoordinates)) return;
		
		// >>> Cursor coordinates text area 
		Font font = new Font(Display.getCurrent(), "Arial", 14, SWT.BOLD);
        e.gc.setFont(font);
		int textWidth = e.gc.textExtent(cursorCoordinates).x;
		int textHeight = e.gc.textExtent(cursorCoordinates).y;
		int chartWidth = chart.getPlotArea().getBounds().width;
		int chartHeight = chart.getPlotArea().getBounds().height;
		// Erase previous cursor coordinates text area
		chart.getPlotArea().redraw(chartWidth - previousCursorTextWidth, 0, previousCursorTextWidth, textHeight, true);
		previousCursorTextWidth = textWidth;
		// Draw cursor coordinates text
		Color oldColor = e.gc.getForeground();
		Color newColor = (chart.getCurrentSeries() instanceof ILineSeries) ? ((ILineSeries)chart.getCurrentSeries()).getLineColor() : Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		e.gc.setForeground(newColor);
		e.gc.drawText(cursorCoordinates, chartWidth - textWidth, 0);
		
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
		
		// >>> Marker & delta coordinates text area
		if(chart.isShowMarker()) {
			String markerCoordinates = chart.getMarkerCoordinatesString();
			String deltaCoordinates = chart.getDeltaCoordinateString();
			
			textWidth = e.gc.textExtent(markerCoordinates).x;
			// Erase previous marker coordinates text area
			chart.getPlotArea().redraw(chartWidth - previousMarkerTextWidth, textHeight, previousMarkerTextWidth, textHeight, true);
			previousMarkerTextWidth = textWidth;
			// Draw marker & delta coordinates text
			e.gc.drawText(markerCoordinates, chartWidth - textWidth, textHeight);
			
			textWidth = e.gc.textExtent(deltaCoordinates).x;
			// Erase previous delta coordinates text area
			chart.getPlotArea().redraw(chartWidth - previousDeltaTextWidth, 2*textHeight, previousDeltaTextWidth, textHeight, true);
			previousDeltaTextWidth = textWidth;
			// Draw marker & delta coordinates text
			e.gc.drawText(deltaCoordinates, chartWidth - textWidth, 2*textHeight);
			
			// >>> Marker area
			// Erase previous ?
			// Draw marker istelf
			if(chart.isShowMarker()) {
				e.gc.drawLine(chart.getCurrentXMarker_Pixel(), 0, chart.getCurrentXMarker_Pixel(), chart.getCurrentYMarker_Pixel() - 3);
				e.gc.drawLine(chart.getCurrentXMarker_Pixel(), chart.getCurrentYMarker_Pixel() + 3, chart.getCurrentXMarker_Pixel(), chart.getPlotArea().getBounds().height);
				e.gc.drawRectangle(chart.getCurrentXMarker_Pixel() - 3, chart.getCurrentYMarker_Pixel() - 3, 6, 6);
			}
		}
		e.gc.setForeground(oldColor);
	}

	@Override
	public boolean drawBehindSeries() {
		return false;
	}

}
