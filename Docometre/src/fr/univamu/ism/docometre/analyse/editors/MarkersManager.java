package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

public final class MarkersManager extends MouseAdapter implements ICustomPaintListener {

	private InteractiveChart chart;
	private String markerLabel;

	public MarkersManager(InteractiveChart chart) {
		this.chart = chart;
		chart.getPlotArea().addMouseListener(this);
		((IPlotArea)chart.getPlotArea()).addCustomPaintListener(this);
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if(markerLabel != null && !markerLabel.equals("") && !(chart.getCurrentSeries() == null)) {
			ISeries series = chart.getCurrentSeries();
			System.out.println("Add marker " + markerLabel + " at : " + e.x + " for : " + series.getId());
		}
	}

	@Override
	public boolean drawBehindSeries() {
		return true;
	}

	@Override
	public void paintControl(PaintEvent e) {
		
	}
	
	public void setMarkerLabel(String label) {
		this.markerLabel = label;
		System.out.println("Marker label : " + markerLabel);
	}

}
