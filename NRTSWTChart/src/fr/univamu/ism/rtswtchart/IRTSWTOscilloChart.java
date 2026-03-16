package fr.univamu.ism.rtswtchart;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;

public interface IRTSWTOscilloChart {
	public void setLayoutData(GridData gridData);
	public GridData getLayoutData();
	public void setShowCurrentValue(boolean value);
	public void setAutoScale(boolean value);
	public void setWindowTimeWidth(double value);
	public void setyMax(double value);
	public void setyMin(double value);
	public void setGridVisibility(boolean value);
	public void setLegendVisibility(boolean value);
	public void setLegendPosition(int position);
	public void setGridLinesColor(Color color);
	public void setFontColor(Color color);
	public IRTSWTSerie createSerie(String serieId, Color color);
	public void dispose();
}
