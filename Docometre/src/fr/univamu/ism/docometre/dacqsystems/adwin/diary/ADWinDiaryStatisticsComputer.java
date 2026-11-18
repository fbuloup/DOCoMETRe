/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.adwin.diary;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.editors.DiaryStatisticsComputer;

public class ADWinDiaryStatisticsComputer implements DiaryStatisticsComputer {
	
	private int n_tb2d = 0;
	private double m_tb2d = 0;
	
	private int n_tRecovery = 0;
	private double m_tRecovery = 0;
	
	private int n_tGeneration = 0;
	private double m_tGeneration = 0;
	
	private int n_tDisplay = 0;
	private double m_tDisplay = 0;
	
	private HashMap<String, Integer> buffer_RecoveringChannel = new HashMap<>(0);
	
	private HashMap<String, Integer> buffer_GeneratingChannel = new HashMap<>(0);
	
	// Lines of the real-time loop iterations (three lines per iteration in the diary file, six lines
	// displayed) : mean durations and total samples of each channel since the beginning of the diary
	private static final Pattern DURATION = Pattern.compile("\\d+\\.\\d+");
	private final double[] iterationMeans = new double[3]; // interval, recovery, generation
	private int iterations = 0;
	private final Map<String, Long> recoveredTotals = new LinkedHashMap<>();
	private final Map<String, Long> generatedTotals = new LinkedHashMap<>();

	@Override
	public String computeValue(String line) {
		if(line.startsWith(ADWinMessages.ADWinDiary_TimeBetween_Scanner)) {
			String valueString  = line.split(":")[1].replaceAll("sec.", "").trim();
			double newValue = Double.parseDouble(valueString);
			m_tb2d = newValue/(n_tb2d + 1.0) + 1.0f*n_tb2d/(n_tb2d + 1.0)*m_tb2d;
			n_tb2d++;
			line = line + ADWinMessages.ADWinDiary_Mean + String.valueOf(m_tb2d) + " sec."; 
		}
		
		if(line.startsWith(ADWinMessages.ADWinDiary_RecoveryTime_Scanner)) {
			String valueString  = line.split(":")[1].replaceAll("sec.", "").trim();
			double newValue = Double.parseDouble(valueString);
			m_tRecovery = newValue/(n_tRecovery + 1.0) + 1.0f*n_tRecovery/(n_tRecovery + 1.0)*m_tRecovery;
			n_tRecovery++;
			line = line + ADWinMessages.ADWinDiary_Mean + String.valueOf(m_tRecovery) + " sec."; 
		}
		
		if(line.startsWith(ADWinMessages.ADWinDiary_GenerationTime_Scanner)) {
			String valueString  = line.split(":")[1].replaceAll("sec.", "").trim();
			double newValue = Double.parseDouble(valueString);
			m_tGeneration = newValue/(n_tGeneration + 1.0) + 1.0f*n_tGeneration/(n_tGeneration + 1.0)*m_tGeneration;
			n_tGeneration++;
			line = line + ADWinMessages.ADWinDiary_Mean + String.valueOf(m_tGeneration) + " sec."; 
		}
		
		if(line.startsWith(ADWinMessages.ADWinDiary_DisplayTime_Scanner)) {
			String valueString  = line.split(":")[1].replaceAll("sec.", "").trim();
			double newValue = Double.parseDouble(valueString);
			m_tDisplay = newValue/(n_tDisplay + 1.0) + 1.0f*n_tDisplay/(n_tDisplay + 1.0)*m_tDisplay;
			n_tDisplay++;
			line = line + ADWinMessages.ADWinDiary_Mean + String.valueOf(m_tDisplay) + " sec."; 
		}
		
		if(line.startsWith(ADWinMessages.ADWinDiary_Recovering_Scanner)) {
			String[] valuesString  = line.split("-");
			String key = valuesString[0];
			String valueString = valuesString[1].split(":")[1].trim();
			Integer previousValue = buffer_RecoveringChannel.get(key);
			if(previousValue == null) previousValue = 0;
			int newValue = previousValue + Integer.parseInt(valueString);
			buffer_RecoveringChannel.put(key, newValue);
			line = line + ADWinMessages.ADWinDiary_Total + String.valueOf(newValue); 
		}
		
		if(line.startsWith(ADWinMessages.ADWinDiary_Generating_Scanner)) {
			String[] valuesString  = line.split("-");
			String key = valuesString[0];
			String valueString = valuesString[1].split(":")[1].trim();
			Integer previousValue = buffer_GeneratingChannel.get(key);
			if(previousValue == null) previousValue = 0;
			int newValue = previousValue + Integer.parseInt(valueString);
			buffer_GeneratingChannel.put(key, newValue);
			line = line + ADWinMessages.ADWinDiary_Total + String.valueOf(newValue); 
		}
		
		// Iteration of the real-time loop : durations line followed by the means line, recovered channels
		// line, generated channels line followed by the totals lines (six displayed lines)
		if(line.startsWith(ADWinMessages.ADWinDiary_IterationTimes_Scanner)) line = line + "\n" + computeIterationMeans(line);
		if(line.startsWith(ADWinMessages.ADWinDiary_IterationRecovered_Scanner)) addTotals(line, ADWinMessages.ADWinDiary_IterationRecovered, recoveredTotals);
		if(line.startsWith(ADWinMessages.ADWinDiary_IterationGenerated_Scanner)) {
			addTotals(line, ADWinMessages.ADWinDiary_IterationGenerated, generatedTotals);
			line = line + "\n" + NLS.bind(ADWinMessages.ADWinDiary_IterationRecoveredTotals, totalsToString(recoveredTotals))
					+ "\n" + NLS.bind(ADWinMessages.ADWinDiary_IterationGeneratedTotals, totalsToString(generatedTotals));
		}
		
		return line;
	}
	
	/*
	 * Durations line of an iteration of the real-time loop : returns the line of the mean durations
	 * since the beginning of the diary (interval, recovery and generation).
	 */
	private String computeIterationMeans(String line) {
		// The first three decimal numbers are the durations (interval, recovery, generation)
		Matcher matcher = DURATION.matcher(line);
		double[] durations = new double[3];
		for (int i = 0; i < durations.length; i++) {
			if(!matcher.find()) return NLS.bind(ADWinMessages.ADWinDiary_IterationMeans, new Object[] {"?", "?", "?"});
			durations[i] = Double.parseDouble(matcher.group());
		}
		for (int i = 0; i < durations.length; i++) iterationMeans[i] = durations[i]/(iterations + 1.0) + iterations/(iterations + 1.0)*iterationMeans[i];
		iterations++;
		return NLS.bind(ADWinMessages.ADWinDiary_IterationMeans, new Object[] {format(iterationMeans[0]), format(iterationMeans[1]), format(iterationMeans[2])});
	}
	
	/*
	 * Adds the samples of the channels listed in the line after the label of the message
	 * ("Recovered channels : {0}" or "Generated channels : {0}") : "name samples, name samples..."
	 */
	private static void addTotals(String line, String labelMessage, Map<String, Long> totals) {
		String list = line.substring(Math.min(line.length(), labelMessage.indexOf("{0}")));
		for (String item : list.split(",")) {
			item = item.trim();
			int space = item.lastIndexOf(' ');
			if(space <= 0) continue;
			try {
				long samples = Long.parseLong(item.substring(space + 1).trim());
				totals.merge(item.substring(0, space), samples, Long::sum);
			} catch (NumberFormatException e) {
				// Item not in the expected format : ignored
			}
		}
	}
	
	private static String totalsToString(Map<String, Long> totals) {
		StringBuilder string = new StringBuilder();
		for (Map.Entry<String, Long> entry : totals.entrySet()) {
			if(string.length() > 0) string.append(", ");
			string.append(entry.getKey()).append(' ').append(entry.getValue());
		}
		return string.toString();
	}
	
	private static String format(double seconds) {
		return String.format(Locale.ROOT, "%.4f", seconds);
	}

}
