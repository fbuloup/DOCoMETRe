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
		
		return line;
	}

}
