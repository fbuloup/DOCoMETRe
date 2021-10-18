package fr.univamu.ism.docometre.analyse.editors;

import fr.univamu.ism.docometre.analyse.datamodel.XYChart;

public interface Chart2D3DBehaviour {
	
	void setDirty(boolean dirty);
	void refreshTrialsListFrontEndCuts();
	XYChart getChartData();
	String[] getSeriesIDs();
	void redraw();
	void updateFrontEndCutsChartHandler();
	void updateXAxisRange(double min, double max);
	void updateYAxisRange(double min, double max);
	void updateZAxisRange(double min, double max);
	void removeSeries(String seriesID);

}
