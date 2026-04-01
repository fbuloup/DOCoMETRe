package fr.univamu.ism.docometre.analyse.datamodel;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class TimeChannel extends Channel {
	
	private double[] values;

	public TimeChannel(Channel associatedChannel) {
		super(null, "time");
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(associatedChannel);
		int nbSamples = MathEngineFactory.getMathEngine().getSamplesNumber(associatedChannel, 0);
		values = new double[nbSamples];
		for (int i = 0; i < values.length; i++) values[i] = i/sf;
	}
	
	public double[] getValues() {
		return values;
	}

}
