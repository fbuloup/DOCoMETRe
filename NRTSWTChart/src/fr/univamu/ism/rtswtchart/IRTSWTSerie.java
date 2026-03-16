package fr.univamu.ism.rtswtchart;

public interface IRTSWTSerie {
	public String getId();
	public void setDisplayCurrentValue(boolean displayCurrentValues);
	public void setThickness(int thickness);
	public void addPoints(Double[] timeValues, Double[] doubleValues);

}
