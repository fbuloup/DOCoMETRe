package fr.univamu.ism.docometre.chartxyz.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.TimeChannel;

/**
 * Check of the sampling frequencies of the signals of an XY or XYZ curve:
 * their samples are paired by sample number, which gives the same instant
 * only if all the signals have the same sampling frequency. The time
 * (TimeChannel) is not checked: it is computed from the other signal.
 */
public final class SampleFrequencies {

	private SampleFrequencies() {
	}

	/**
	 * Error message if the signals (time excluded) do not all have the same
	 * sampling frequency, null otherwise. The message lists each signal with
	 * its frequency (DocometreMessages.DifferentSampleFrequencies).
	 */
	public static String check(Channel... channels) {
		List<Channel> signals = new ArrayList<>();
		for (Channel channel : channels) {
			if(channel == null || channel instanceof TimeChannel || channel == Channel.timeChannel) continue;
			signals.add(channel);
		}
		if(signals.size() < 2) return null;
		double[] frequencies = new double[signals.size()];
		boolean same = true;
		for (int i = 0; i < frequencies.length; i++) {
			frequencies[i] = MathEngineFactory.getMathEngine().getSampleFrequency(signals.get(i));
			same &= Math.abs(frequencies[i] - frequencies[0]) <= 1e-9 * Math.max(1, Math.abs(frequencies[0]));
		}
		if(same) return null;
		StringBuilder list = new StringBuilder();
		for (int i = 0; i < frequencies.length; i++) {
			if(i > 0) list.append(", ");
			list.append(signals.get(i).getFullName()).append(" (").append(frequencies[i]).append(" Hz)");
		}
		return NLS.bind(DocometreMessages.DifferentSampleFrequencies, list.toString());
	}
}
