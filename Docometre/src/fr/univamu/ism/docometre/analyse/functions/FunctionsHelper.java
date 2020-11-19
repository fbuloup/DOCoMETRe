package fr.univamu.ism.docometre.analyse.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;

public final class FunctionsHelper {
	public static String createTrialsListHelper(String trialsList) {
		String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
		if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) {
			Pattern pattern = Pattern.compile("^\\d+:\\d+$");
			Matcher matcher = pattern.matcher(trialsList);
	        if(matcher.matches()) {
	        	String[] numbersString = trialsList.split(":");
	        	int startNumber = Integer.valueOf(numbersString[0]) - 1;
	        	trialsList = "" + startNumber + "," + numbersString[1];
	        	return trialsList;
	        }
	        pattern = Pattern.compile("^\\d+$");
			matcher = pattern.matcher(trialsList);
			if(matcher.matches()) {
				return String.valueOf(Integer.valueOf(trialsList) - 1);
			}
		}
		return trialsList;
	}
}
