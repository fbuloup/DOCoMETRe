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
package fr.univamu.ism.docometre.preferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ChooseWorkspaceData;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;

public class PreferencesInitializer extends AbstractPreferenceInitializer {

	public PreferencesInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		defaults.put(GeneralPreferenceConstants.PREF_UNDO_LIMIT, "10");
		defaults.putBoolean(GeneralPreferenceConstants.PREF_CONFIRM_UNDO, true);
		defaults.putBoolean(GeneralPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false);
		defaults.putBoolean(GeneralPreferenceConstants.SHOW_WORKSPACE_SELECTION_DIALOG, ChooseWorkspaceData.getInstance().getShowDialog());
		defaults.put(GeneralPreferenceConstants.WINE_FULL_PATH, "");//"/Applications/Wine Staging.app/Contents/Resources/wine/bin/wine");
		defaults.putBoolean(GeneralPreferenceConstants.USE_DOCKER, false);
		
		defaults.putBoolean(GeneralPreferenceConstants.STOP_TRIAL_NOW, true);
		defaults.putBoolean(GeneralPreferenceConstants.USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW, false);
		defaults.putBoolean(GeneralPreferenceConstants.AUTO_VALIDATE_TRIALS, false);
		defaults.putBoolean(GeneralPreferenceConstants.AUTO_START_TRIALS, false);
		
		defaults.putBoolean(GeneralPreferenceConstants.SHOW_MATLAB_WINDOW, false);
		defaults.put(GeneralPreferenceConstants.MATLAB_LOCATION, "");
		defaults.put(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, "");
		defaults.putInt(GeneralPreferenceConstants.MATLAB_TIME_OUT, 180);
		
		defaults.put(GeneralPreferenceConstants.PYTHON_LOCATION, "");
		defaults.put(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, "");
		defaults.putInt(GeneralPreferenceConstants.PYTHON_TIME_OUT, 15);
		
		defaults.putBoolean(GeneralPreferenceConstants.SHOW_CURSOR, false);
		defaults.putBoolean(GeneralPreferenceConstants.SHOW_MARKER, false);
		defaults.putBoolean(GeneralPreferenceConstants.SYNCHRONIZE_CHARTS_WHEN_TRIAL_CHANGE, true);

		defaults.putBoolean(GeneralPreferenceConstants.XML_SERIALIZATION, true);
		defaults.putBoolean(GeneralPreferenceConstants.BUILD_AUTOMATICALLY, true);
		
		defaults.put(MathEnginePreferencesConstants.MATH_ENGINE, MathEnginePreferencesConstants.MATH_ENGINE_MATLAB);
		defaults.putBoolean(MathEnginePreferencesConstants.ALWAYS_LOAD_FROM_SAVED_DATA, false);
		
		defaults.putBoolean(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE, false);
		defaults.put(GeneralPreferenceConstants.STD_ERR_OUT_FILE, "");
		
		
		Bundle bundle = Platform.getBundle("Libraries");
		String librariesPath = "Libraries_" + bundle.getVersion();
		if(Boolean.getBoolean("DEV")) librariesPath = "Libraries";
		String runtimeFolder = System.getProperty("user.dir");
		IPath path = Path.fromOSString(runtimeFolder);
		if(Boolean.getBoolean("DEV")) path = path.append(librariesPath);
		else {
			if(Platform.OS_MACOSX.equals(Platform.getOS())) {
				path = path.removeLastSegments(1).append("Eclipse").append("plugins").append(librariesPath);
			}
			if(Platform.OS_WIN32.equals(Platform.getOS())) {
				path = path.append("plugins").append(librariesPath);;
			}
			if(Platform.OS_LINUX.equals(Platform.getOS())) {
				path = path.append("plugins").append(librariesPath);;
			}
		}
		path = path.append("includes");
		defaults.put(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), path.append("ADWinIncludeFiles").toOSString());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), path.append("ADWinIncludeFiles").toOSString());
		defaults.put(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(),  path.append("ArduinoUnoFunctions").toOSString());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(),  path.append("ArduinoUnoFunctions").toOSString());
		defaults.put(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, path.append("MatlabScripts").toOSString());
		preferenceStore.putValue(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, path.append("MatlabScripts").toOSString());
		defaults.put(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, path.append("PythonScripts").toOSString());
		preferenceStore.putValue(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, path.append("PythonScripts").toOSString());
		
	}

}
