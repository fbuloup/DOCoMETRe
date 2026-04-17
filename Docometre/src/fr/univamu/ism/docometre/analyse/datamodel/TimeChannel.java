package fr.univamu.ism.docometre.analyse.datamodel;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class TimeChannel extends Channel {
	
	private double[] values;
	double sf;
	private Channel associatedChannel;

	public TimeChannel(Channel associatedChannel) {
		super(null, "time");
		this.associatedChannel = associatedChannel;
		sf = MathEngineFactory.getMathEngine().getSampleFrequency(associatedChannel);
	}
	
	private void computeValues(int trialNumber) {
		int nbSamples = MathEngineFactory.getMathEngine().getSamplesNumber(associatedChannel, trialNumber);
		values = new double[nbSamples];
		for (int i = 0; i < values.length; i++) values[i] = i/sf;
	}
	
	public double[] getValues(int trialNumber) {
		computeValues(trialNumber);
		return values;
	}

}
