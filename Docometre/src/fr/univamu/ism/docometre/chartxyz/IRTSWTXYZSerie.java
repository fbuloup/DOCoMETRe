package fr.univamu.ism.docometre.chartxyz;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

public interface IRTSWTXYZSerie extends IRTSWTSerie {
	public void addPoints(Double[] xValues, Double[] yValues, Double[] zValues);
}
