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

/**
 * Constant definitions for plug-in preferences
 */
public class GeneralPreferenceConstants {

	public static final String PREF_UNDO_LIMIT = "PREF_UNDO_LIMIT";
	public static final String PREF_CONFIRM_UNDO = "PREF_CONFIRM_UNDO";
	public static final String SHOW_WORKSPACE_SELECTION_DIALOG = "SHOW_WORKSPACE_SELECTION_DIALOG";
	public static final String MAX_RECENT_WORKSPACES = "MAX_RECENT_WORKSPACES";
	public static final String RECENT_WORKSPACES = "RECENT_WORKSPACES";
	public static final String SELECTED_WORKSPACE = "SELECTED_WORKSPACE";
	public static final String SHOW_TRADITIONAL_STYLE_TABS = "SHOW_TRADITIONAL_STYLE_TABS";
	public static final String WINE_FULL_PATH = "WINE_FULL_PATH";
	
	public static final String STOP_TRIAL_NOW = "STOP_TRIAL_NOW";
	public static final String USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW = "USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW";
	public static final String AUTO_VALIDATE_TRIALS = "AUTO_VALIDATE_TRIALS";
	public static final String AUTO_START_TRIALS = "AUTO_START_TRIALS";
	
	public static final String MATH_ENGINE = "MATH_ENGINE";
	
	public static final String MATH_ENGINE_MATLAB = "Matlab";
	public static final String MATH_ENGINE_PYTHON = "Python";
	//public static final String MATH_ENGINE_OCTAVE = "Octave";
	
	public static final String[][] MATH_ENGINE_VALUES = new String[][] { {MATH_ENGINE_MATLAB, MATH_ENGINE_MATLAB}, {MATH_ENGINE_PYTHON, MATH_ENGINE_PYTHON}/*, {MATH_ENGINE_OCTAVE, MATH_ENGINE_OCTAVE}*/}; 
	
	public static final String SHOW_MATLAB_WINDOW = "SHOW_MATLAB_WINDOW";
	public static final String MATLAB_LOCATION = "MATLAB_LOCATION";
	public static final String MATLAB_TIME_OUT = "MATLAB_TIME_OUT";
	public static final String MATLAB_SCRIPT_LOCATION = "MATLAB_SCRIPT_LOCATION";
	
	public static final String PYTHON_LOCATION = "PYTHON_LOCATION";
	public static final String PYTHON_TIME_OUT = "PYTHON_TIME_OUT";
	public static final String PYTHON_SCRIPT_LOCATION = "PYTHON_SCRIPT_LOCATION";
	
	
	
}
