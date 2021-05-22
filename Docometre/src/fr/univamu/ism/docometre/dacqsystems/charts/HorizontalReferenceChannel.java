package fr.univamu.ism.docometre.dacqsystems.charts;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.rtswtchart.RTSWTSerie;

public class HorizontalReferenceChannel extends Channel {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	
	public HorizontalReferenceChannel() {
		super(null);
		ChannelProperties.populateProperties(this);
		setProperty(ChannelProperties.NAME, RTSWTSerie.HORIZONTAL_REFERENCE);
	}
	
	public void setValue(String value) {
		setProperty(ChannelProperties.NAME, value);
	}
	
	public double getValue() {
		return Double.parseDouble(getProperty(ChannelProperties.NAME));
	}

	@Override
	public void notifyChannelObservers() {

	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {

	}

	@Override
	public void addSamples(float[] buffer) {

	}

	@Override
	public float[] getSamples(int nbData) {
		return null;
	}

	@Override
	public void open(Process process, String prefix, String suffix) {

	}

	@Override
	public void close(Process process) {

	}

	@Override
	public String getID() {
		return RTSWTSerie.HORIZONTAL_REFERENCE + "[" + getProperty(ChannelProperties.NAME) + "]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return null;
	}

	@Override
	public void initializeObservers() {
	}

}
