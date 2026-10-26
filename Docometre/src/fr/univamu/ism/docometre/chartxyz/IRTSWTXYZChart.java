package fr.univamu.ism.docometre.chartxyz;

import fr.univamu.ism.rtswtchart.IRTSWTXYChart;

public interface IRTSWTXYZChart extends IRTSWTXYChart {
	public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax);
}
