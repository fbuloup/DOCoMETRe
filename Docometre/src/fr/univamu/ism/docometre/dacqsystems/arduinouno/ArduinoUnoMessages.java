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
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import org.eclipse.osgi.util.NLS;

public class ArduinoUnoMessages extends NLS {
	
	private static final String BUNDLE_NAME = "fr.univamu.ism.docometre.dacqsystems.arduinouno.messages";//$NON-NLS-1$

	// DACQ CONFIGURATION
	public static String DevicePath_Label;
	public static String DevicePath_Tooltip;
	public static String DeviceBaudRate_Label;
	public static String DeviceBaudRate_Tooltip;
	public static String GlobalFrequency_Label;
	public static String GlobalFrequency_Tooltip;
	public static String DACQGeneralConfigurationPage_Title;
	public static String DACQGeneralConfigurationPage_PageTitle;
	public static String BuilderPath_Label;
	public static String BuilderPath_Tooltip;
	public static String AVRDudePath_Label;
	public static String AVRDudePath_Tooltip;
	public static String LibrariesAbsolutePath_Label;
	public static String LibrariesAbsolutePath_Tooltip;
	
	// Variables
	public static String Size_Label;
	public static String Size_Tooltip;
	public static String Type_Label;
	public static String Type_Tooltip;
	
	public static String GeneralConfigurationSection_Title;
	public static String GeneralConfigurationSectionDescription;
	
	public static String ModuleConfigurationSection_Title;
	public static String ModulesConfigurationSectionDescription;

	public static String ModuleType_ColumnTitle;
	public static String ModuleType_ColumnTooltip;
	
	public static String ANALOG_INPUT;
	public static String ANALOG_OUTPUT;
	public static String DIO;
	public static String UNKNOWN;
	
	/*
	 * Variables form page
	 */
	public static String VariablesPage_Title;
	public static String VariablesPageExplanationsSection_Title;
	public static String VariablesPageExplanationsSection_Description;
	public static String VariablesTableSection_Title;
	public static String VariablesTableSection_Description;
	public static String DeleteVariable_Tooltip;
	public static String AddVariable_Tooltip;
	public static String DeleteVariablesDialog_Title;
	public static String DeleteVariablesDialog_Message;
	public static String DeleteVariablesOperation_Label;
	public static String AddVariableOperation_Label;
	public static String VariablesExplanations_Text;
	
	public static String Unit_Label;
	public static String Unit_Tooltip;
	public static String UnitMaxValue_Label;
	public static String UnitMaxValue_Tooltip;
	public static String UnitMinValue_Label;
	public static String UnitMinValue_Tooltip;
	public static String AmpMaxValue_Label;
	public static String AmpMaxValue_Tooltip;
	public static String AmpMinValue_Label;
	public static String AmpMinValue_Tooltip;
	
	/*
	 * AnIn form page
	 */
	public static String AnInConfigurationPage_Title;
	public static String AmplitudeMax_Label;
	public static String AmplitudeMax_Tooltip;
	public static String AmplitudeMin_Tooltip;
	public static String AmplitudeMin_Label;
	public static String SeDiff_Label;
	public static String SeDiff_Tooltip;
	public static String DeleteInput_Tooltip;
	public static String AddInput_Tooltip;
	public static String AnInModuleExplanations_Text;
	public static String AnInModuleExplanations_Text2;
	// Analog input module
	public static String AnalogReference_Label;
	public static String AnalogReference_Tooltip;
	public static String Used_Label;
	public static String Used_Tooltip;
	
	/*
	 * AnOut form page
	 */
	public static String ArduinoAnOutConfigurationPage_Title;
	public static String AnOutModuleExplanations_Text;
	public static String AnOutModuleExplanations_Text2;
	
	/*
	 * DIO properties
	 */
	public static String InOut_Label;
	public static String InOut_Tooltip;
	
	/*
	 * DIO form page
	 */
	public static String DigInOutConfigurationPage_Title;
	public static String DigInOutExplanationsSection_Title;
	public static String DigInOutExplanationsSection_Description;
	public static String DigInOutModuleExplanations_Text;
	public static String DigInOutModuleExplanations_Text2;
	
	/*
	 * Refresh device selection dialog
	 */
	public static String Refresh;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, ArduinoUnoMessages.class);
	}

}
