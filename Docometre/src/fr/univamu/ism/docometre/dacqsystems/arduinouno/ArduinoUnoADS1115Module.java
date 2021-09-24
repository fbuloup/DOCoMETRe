package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ArduinoUnoADS1115Module extends Module {
	
	public ArduinoUnoADS1115Module(DACQConfiguration dacqConfiguration) {
		super(dacqConfiguration);
		ArduinoUnoADS1115ModuleProperties.populateProperties(this);
	}

	public static final long serialVersionUID = AbstractElement.serialVersionUID;

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recovery() {
		// TODO Auto-generated method stub

	}

	@Override
	public void generation() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub

	}

	@Override
	public Channel initializeChannelProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
