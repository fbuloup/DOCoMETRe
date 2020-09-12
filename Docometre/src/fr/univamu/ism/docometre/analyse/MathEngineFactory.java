package fr.univamu.ism.docometre.analyse;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public final class MathEngineFactory {
	
	private static MathEngine mathEngine;
	
	public static MathEngine getMathEngine() {
		if(mathEngine != null) return mathEngine;
		String mathEnginePref = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATH_ENGINE);
		if(GeneralPreferenceConstants.MATH_ENGINE_MATLAB.equals(mathEnginePref)) {
			mathEngine = new MatlabEngine();
		}
		if(GeneralPreferenceConstants.MATH_ENGINE_PYTHON.equals(mathEnginePref)) {
			mathEngine = new PythonEngine();
		}
		return mathEngine;
	}

	public static void clear() {
		mathEngine = null;
	}
	
}
