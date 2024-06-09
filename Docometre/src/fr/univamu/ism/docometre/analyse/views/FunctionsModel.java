package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public final class FunctionsModel {
	
	public static Object createModel() {
		String matlabScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION);
		String matlabUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_USER_SCRIPTS_LOCATION);
		
		String pythonScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION);
		String pythonUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_USER_SCRIPTS_LOCATION);
		
		String adwinScriptLocation = Activator.getDefault().getPreferenceStore().getString(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey());
		String adwinUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.ADWIN_USER_LIBRARIES_ABSOLUTE_PATH);
		
		String arduinoUnoScriptLocation = Activator.getDefault().getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey());
		String arduinoUnoUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.ARDUINO_USER_LIBRARIES_ABSOLUTE_PATH);

		BidiMap<String, Path> functionsHashMap = new DualHashBidiMap<>();
		
		if(Path.of(adwinUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.ADwinUserFunctions, Path.of(adwinUserScriptLocation));
		functionsHashMap.put(DocometreMessages.ADwinFunctions, Path.of(adwinScriptLocation).getParent().resolve("ADWinFunctions"));
		
		if(Path.of(arduinoUnoUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.ArduinoUnoUserFunctions, Path.of(arduinoUnoUserScriptLocation));
		functionsHashMap.put(DocometreMessages.ArduinoUnoFunctions, Path.of(arduinoUnoScriptLocation).getParent().resolve("ArduinoUnoFunctions"));
		
		if(Path.of(pythonUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.PythonUserFunctions, Path.of(pythonUserScriptLocation));
		functionsHashMap.put(DocometreMessages.PythonFunctions, Path.of(pythonScriptLocation).getParent().resolve("PythonFunctions"));
		
		if(Path.of(matlabUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.MatlabUserFunctions, Path.of(matlabUserScriptLocation));
		functionsHashMap.put(DocometreMessages.MatlabFunctions, Path.of(matlabScriptLocation).getParent().resolve("MatlabFunctions"));
		
		return functionsHashMap;
	}

}
