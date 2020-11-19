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
package fr.univamu.ism.docometre.scripteditor.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuManager;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.functions.MatlabEngineFunctionsMenuFactory;
import fr.univamu.ism.docometre.analyse.functions.PythonEngineFunctionsMenuFactory;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.functions.ADWinFunctionsMenuFactory;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.dacqsystems.functions.ArduinoUnoFunctionsMenuFactory;
import fr.univamu.ism.docometre.dacqsystems.functions.CustomerFunction;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Script;

public final class FunctionFactory {
	
	public final static String MENU_TITLE = "MENU_TITLE";
	public final static String DESCRIPTION = "DESCRIPTION";
	
	private static final String countrySuffix = "_" + Locale.getDefault().getCountry();
	private static final String USER_FUNCTION_KEY = "USER_FUNCTION";
	
	public static void populateMenu(AbstractScriptSegmentEditor scriptSegmentEditor, BlockEditPart blockEditPart, IMenuManager functionsMenuManager) {
		Object object = ((ResourceEditorInput)scriptSegmentEditor.getEditorInput()).getObject();
		if(object instanceof ADWinProcess) ADWinFunctionsMenuFactory.populateMenu(scriptSegmentEditor, blockEditPart, functionsMenuManager);
		if(object instanceof ArduinoUnoProcess) ArduinoUnoFunctionsMenuFactory.populateMenu(scriptSegmentEditor, blockEditPart, functionsMenuManager);
		if(object instanceof Script) {
			String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
			if(MathEnginePreferencesConstants.MATH_ENGINE_MATLAB.equals(mathEngine)) MatlabEngineFunctionsMenuFactory.populateMenu(scriptSegmentEditor, blockEditPart, functionsMenuManager);
			if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) PythonEngineFunctionsMenuFactory.populateMenu(scriptSegmentEditor, blockEditPart, functionsMenuManager);
		}
	}
	
	public static Path computeAbsolutePath(Object context) {
		if(context instanceof Process) {
			Process process = (Process)context;
			DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
			String functionsAbsolutePath = "";
			if(dacqConfiguration instanceof ADWinDACQConfiguration) functionsAbsolutePath = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
			if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) functionsAbsolutePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
			Path path = new Path(functionsAbsolutePath);
			String suffix = "";
			if (process instanceof ADWinProcess) suffix = "ADWinFunctions";
			if (process instanceof ArduinoUnoProcess) suffix = "ArduinoUnoFunctions";
			path = (Path) path.removeLastSegments(1).append(suffix);
			return path;
		}
		if(context instanceof Script) {
			String functionsAbsolutePath = "";
			String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
			if(MathEnginePreferencesConstants.MATH_ENGINE_MATLAB.equals(mathEngine)) functionsAbsolutePath =  Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPT_LOCATION);
			if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) functionsAbsolutePath =  Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_SCRIPT_LOCATION);
			Path path = new Path(functionsAbsolutePath);
			String suffix = "";
			if(MathEnginePreferencesConstants.MATH_ENGINE_MATLAB.equals(mathEngine)) suffix = "MatlabFunctions";
			if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) suffix = "PythonFunctions";
			path = (Path) path.removeLastSegments(1).append(suffix);
			return path;
		}
		return null;
	}

	public static String getProperty(Object context, String functionFileName, String key) {
		Path path = computeAbsolutePath(context);
		try {
			Properties properties = new Properties();
			properties.load(new InputStreamReader(new FileInputStream(path.append(functionFileName).toOSString()), Charset.forName("UTF-8")));
			if(properties.containsKey(key + countrySuffix)) return properties.getProperty(key + countrySuffix);
			else return properties.getProperty(key);
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
			return e.getMessage();
		} 
	}
	
	public static String[] getCustomerFunctions(Object context) {
		IPath path = computeAbsolutePath(context);
		path = path.append(CustomerFunction.CUSTOMER_FUNCTIONS_PATH);
		File functionsFilesFolder = new File(path.toOSString());
		String[] files = functionsFilesFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				String value = getProperty(context, CustomerFunction.CUSTOMER_FUNCTIONS_PATH + name, USER_FUNCTION_KEY);
				return value != null && ("YES".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
			}
		});
		return files==null?new String[0]:files;
	}

	public static boolean isCustomerFunction(Object context, String functionName) {
		String[] customerFunctions = getCustomerFunctions(context);
		return Arrays.asList(customerFunctions).contains(functionName);
	}

}
