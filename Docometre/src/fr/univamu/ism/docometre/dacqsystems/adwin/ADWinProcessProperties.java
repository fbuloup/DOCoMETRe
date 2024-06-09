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
package fr.univamu.ism.docometre.dacqsystems.adwin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class ADWinProcessProperties extends Property {

//	NAME(ADWinMessages.ProcessName_Label, ADWinMessages.ProcessName_Tooltip, "^[a-z|A-Z]+[0-9|a-z|A-Z]*$"),
	public static final ADWinProcessProperties DURATION = new ADWinProcessProperties("ADWinProcessProperties.DURATION", ADWinMessages.ProcessDuration_Label, ADWinMessages.ProcessDuration_Tooltip, "(^[+]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ADWinProcessProperties DATA_LOSS = new ADWinProcessProperties("ADWinProcessProperties.DATA_LOSS", ADWinMessages.ProcessDataLoss_Label, ADWinMessages.ProcessDataLoss_Tooltip, "^(true|false)$", "true:false");
	public static final ADWinProcessProperties EVENTS_DIARY = new ADWinProcessProperties("ADWinProcessProperties.EVENTS_DIARY", ADWinMessages.ProcessEventsDiary_Label, ADWinMessages.ProcessEventsDiary_Tooltip, ".");
	public static final ADWinProcessProperties EVENTS_DIARY_FILE_NAME = new ADWinProcessProperties("ADWinProcessProperties.EVENTS_DIARY_FILE_NAME", ADWinMessages.ProcessEventsDiary_Label, ADWinMessages.ProcessEventsDiary_Tooltip, ".");
	public static final ADWinProcessProperties PROCESS_NUMBER = new ADWinProcessProperties("ADWinProcessProperties.PROCESS_NUMBER", ADWinMessages.ProcessNumber_Label, ADWinMessages.ProcessNumber_Tooltip, "^([1-9]|10)$");
	
	public static String DURATION_NOT_SPECIFIED = "Duration not specified";

	public static void populateProperties(Process process) {
		process.setProperty(DURATION, DURATION_NOT_SPECIFIED);
		process.setProperty(DATA_LOSS, "false");
		String date = new SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(new Date()) + "\n";
		process.setProperty(EVENTS_DIARY, date);
		process.setProperty(EVENTS_DIARY_FILE_NAME, "diary.txt");
		process.setProperty(PROCESS_NUMBER, "1");
	}

	public static void cloneProcess(Process process, Process newProcess) {
		newProcess.setProperty(DURATION, new String(process.getProperty(DURATION)));
		newProcess.setProperty(DATA_LOSS, new String(process.getProperty(DATA_LOSS)));
		newProcess.setProperty(EVENTS_DIARY, new String(process.getProperty(EVENTS_DIARY)));
		newProcess.setProperty(EVENTS_DIARY_FILE_NAME, new String(process.getProperty(EVENTS_DIARY_FILE_NAME)));
		newProcess.setProperty(PROCESS_NUMBER, new String(process.getProperty(PROCESS_NUMBER)));
	}

	private ADWinProcessProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ADWinProcessProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
