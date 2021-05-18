package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.swtchart.extensions.charts.InteractiveChart;

public interface IMarkersManager {
	InteractiveChart getChart();
	void updateMarkersGroup(String markersGroupLabel);
}
