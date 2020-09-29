package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public final class MarkersManager extends MouseAdapter implements ICustomPaintListener {

	private InteractiveChart chart;
	private String markerLabel;

	public MarkersManager(InteractiveChart chart) {
		this.chart = chart;
		chart.getPlotArea().addMouseListener(this);
		((IPlotArea)chart.getPlotArea()).addCustomPaintListener(this);
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent mouseEvent) {
		if (markerLabel != null && !markerLabel.equals("") && !(chart.getCurrentSeries() == null)) {
			ISeries series = chart.getCurrentSeries();
			String fullSignalName = series.getId().replaceAll("\\.\\d+$", "");
			int trialNumber = Integer.parseInt(series.getId().replaceAll("^\\w+\\.\\w+\\.\\w+\\.", ""));
			Event event = new Event();
			event.x = mouseEvent.x;
			event.y = mouseEvent.y;
			double[] coordinates = chart.getMarkerCoordinates(event);
			if(coordinates.length == 2) {
				Activator.logInfoMessage("Add marker " + markerLabel + " at index " + mouseEvent.x + " for : " + fullSignalName + " trial number " + trialNumber, getClass());
				Activator.logInfoMessage("With coordinates x " + coordinates[0] + " and y " + coordinates[1], getClass());
				MathEngineFactory.getMathEngine().addMarker(markerLabel, trialNumber, coordinates[0], coordinates[1], fullSignalName);
			}
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
	}

}
