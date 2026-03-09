package fr.univamu.ism.rtswtchart;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;

public interface IRTSWTXYChart {
	public void setLayoutData(GridData gridData);
	public GridData getLayoutData();
	public void setAutoScale(boolean autoscale);
	public void setxMax(double xMax);
	public void setxMin(double xMin);
	public void setyMax(double value);
	public void setyMin(double value);
	public void setGridVisibility(boolean value);
	public void setLegendVisibility(boolean value);
	public void setLegendPosition(int position);
	public void setGridLinesColor(Color color);
	public void setFontColor(Color color);
	public void setHistorySize(double historySize);
}
