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

import org.eclipse.osgi.util.NLS;

public class ADWinMessages extends NLS {

	private static final String BUNDLE_NAME = "fr.univamu.ism.docometre.dacqsystems.adwin.messages";//$NON-NLS-1$

	/*
	 *  DACQ General configuration page
	 */
	public static String ADBasicCompilerPath_Label;
	public static String ADBasicCompilerPath_Tooltip;
	public static String BootloaderPath_Label;
	public static String BootloaderPath_Tooltip;
	public static String IPAddress_Label;
	public static String IPAddress_Tooltip;
	public static String DeviceNumber_Label;
	public static String DeviceNumber_Tooltip;
	public static String TCPIPServerDeviceNumber_Label;
	public static String TCPIPServerDeviceNumber_Tooltip;
	public static String TCPIPDevicePortNumber_Label;
	public static String TCPIPDevicePortNumber_Tooltip;
	public static String TimeOut_Label;
	public static String TimeOut_Tooltip;
	public static String SytemType_Label;
	public static String SystemType_Tooltip;
	public static String CPUType_Label;
	public static String CPUType_Tooltip;
	public static String ADBasicVersion_Label;
	public static String ADBasicVersion_Tooltip;
	public static String LibrariesAbsolutePath_Label;
	public static String LibrariesAbsolutePath_Tooltip;
	public static String DACQGeneralConfigurationPage_Title;
	public static String DACQGeneralConfigurationPage_PageTitle;
	public static String FoldersAndVersionConfigurationSection_Title;
	public static String DeviceConfigurationSection_Title;
	public static String ModuleConfigurationSection_Title;
	public static String ErrorTCPIPServerNotValid;
	public static String ErrorTCPIPDeviceNotValid;
	public static String ErrorPortNumberDeviceNotValid;
	public static String TimeOutDeviceNotValid;
	public static String GlobalFrequencyNotValid;
	public static String ErrorDeviceNumberNotValid;
	public static String FoldersAndVersionConfigurationSectionDescription;
	public static String DeviceConfigurationSectionDescription;
	public static String ModulesConfigurationSectionDescription;
	public static String ANALOG_INPUT;
	public static String ANALOG_OUTPUT;
	public static String DIO;
	public static String RS232;
	public static String CAN;
	public static String UNKNOWN;
	public static String DeleteModule_Tooltip;
	public static String AddModule_Tooltip;
	
	// ADD module dialog
	public static String AddModuleDialog_ShellTitle;
	public static String AddModuleDialog_Title;
	public static String AddModuleDialog_Message;
	// Delete modules Dialog
	public static String DeleteModuleDialog_Title;
	public static String DeleteModuleDialog_Message;
	// Add modules operation label
	public static String AddModulesOperation_Label;
	// Delete modules operation label
	public static String DeleteModulesOperation_Label;
	public static Object ModuleType_ColumnTitle;
	public static String ModuleType_ColumnTooltip;
	
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

	/*
	 * AnIn form page
	 */
	public static String ADWinAnInConfigurationPage_Title;
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

	/*
	 * AnOut form page
	 */
	public static String ADWinAnOutConfigurationPage_Title;
	public static String AnOut_Stimulus_Label;
	public static String AnOut_Stimulus_Tooltip;
	public static String DeleteOutput_Tooltip;
	public static String AddOutput_Tooltip;
	public static String AnOutModuleExplanations_Text;
	public static String AnOutModuleExplanations_Text2;
	
	/*
	 * Stimulus label and tooltip
	 */
	public static String Stimulus_Path_Label;
	public static String Stimulus_Path_Tooltip;

	/*
	 * DIO form page
	 */
	public static String ADWinDigInOutConfigurationPage_Title;
	public static String ADWinDigInOutExplanationsSection_Title;
	public static String ADWinDigInOutExplanationsSection_Description;
	public static String DeleteDIO_Tooltip;
	public static String AddDIO_Tooltip;
	public static String DigInOutModuleExplanations_Text;
	public static String DigInOutModuleExplanations_Text2;
	
	/*
	 * CAN form page
	 */
	public static String ADWinCANPage_PageTitle; 
	public static String ADWinCANModuleExplanations_Text;
	public static String ADWinCANName_Label;
	public static String ADWinCANName_Tooltip;
	public static String ADWinCANInterfaceNumber_Label;
	public static String ADWinCANInterfaceNumber_Tooltip;
	public static String ADWinCANSystemType_Label;
	public static String ADWinCANSystemType_Tooltip;
	public static String ADWinCANFrequency_Label;
	public static String ADWinCANFrequency_Tooltip;
	public static String ADWinCAN_NB_SENSORS_Label;
	public static String ADWinCAN_NB_SENSORS_Toolip;
	public static String ADWinCAN_MODE_Label;
	public static String ADWinCAN_MODE_Toolip;
	public static String ADWinCAN_MESSAGE_OBJECT_Label;
	public static String ADWinCAN_MESSAGE_OBJECT_Toolip;
	public static String ADWinCAN_MESSAGE_OBJECT_ErrorMessage;
	public static String ADWinCAN_MESSAGE_ID_Label;
	public static String ADWinCAN_MESSAGE_ID_Toolip;
	public static String ADWinCAN_MESSAGE_ID_ErrorMessage;
	public static String ADWinCAN_MESSAGE_ID_LENGTH_Label;
	public static String ADWinCAN_MESSAGE_ID_LENGTH_Toolip;
	
	/*
	 * RS232 form page
	 */
	public static String ADWinRS232Page_PageTitle; 
	public static String ADWinRS232ModuleExplanations_Text;
	public static String ADWinRS232Name_Label;
	public static String ADWinRS232Name_Tooltip;
	public static String ADWinRS232InterfaceNumber_Label;
	public static String ADWinRS232InterfaceNumber_Tooltip;
	public static String ADWinRS232SystemType_Label;
	public static String ADWinRS232SystemType_Tooltip;
	public static String ADWinRS232Frequency_Label;
	public static String ADWinRS232Frequency_Tooltip;
	public static String ADWinRS232BaudRate_Label;
	public static String ADWinRS232BaudRate_Tooltip;
	public static String ADWinRS232DataBits_Label;
	public static String ADWinRS232DataBits_Tooltip;
	public static String ADWinRS232StopBits_Label;
	public static String ADWinRS232StopBits_Tooltip;
	public static String ADWinRS232Parity_Label;
	public static String ADWinRS232Parity_Tooltip;
	public static String ADWinRS232FlowControl_Label;
	public static String ADWinRS232FlowControl_Tooltip;
	public static String AddChannel_Tooltip;
	public static String DeleteChannel_Tooltip;
	public static String AddChannelsDialog_Title;
	public static String AddChannelsDialog_Message;
	public static String AddChannelsDialog_ShellTitle;
	public static String AddChannelsOperation_Label;
	public static String ADWinRS232ChannelsConfigurationModuleSection_Description;
	
	
	/*
	 * ADWin variables properties
	 */
	public static String Parameter_Tooltip;
	public static String Parameter_Label;
	public static String Propagate_Label;
	public static String Propagate_Tooltip;
	public static String Size_Label;
	public static String Size_Tooltip;
	public static String Type_Label;
	public static String Type_Tooltip;
	public static String Gain_Label;
	public static String Gain_Tooltip;
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
	 * ADWin modules properties
	 */
	public static String ModuleNumber_Label;
	public static String ModuleNumber_Tooltip;
	public static String Revision_Label;
	public static String Revision_Tooltip;
	
	/*
	 * ADWin DIO properties
	 */
	public static String InOut_Label;
	public static String InOut_Tooltip;
	public static String DIO_Stimulus_Label;
	public static String DIO_Stimulus_Tooltip;
	
	/*
	 * ADWin process properties
	 */
	public static String ProcessName_Label;
	public static String ProcessName_Tooltip;
	public static String ProcessDuration_Label;
	public static String ProcessDuration_Tooltip;
	public static String ProcessDataLoss_Label;
	public static String ProcessDataLoss_Tooltip;
	public static String ProcessEventsDiary_Label;
	public static String ProcessEventsDiary_Tooltip;
	public static String ProcessEventsDiaryFileName_Label;
	public static String ProcessEventsDiaryFileName_Tooltip;
	public static String ProcessNumber_Label;
	public static String ProcessNumber_Tooltip;
	
	/*
	 * Diary
	 */
	public static String ADWinDiary_AlreadyBooted;
	public static String ADWinDiary_Booted;
	public static String ADWinDiary_Loading;
	public static String ADWinDiary_StartingAt;
	public static String ADWinDiary_TimeBetween;
	public static String ADWinDiary_Recovering;
	public static String ADWinDiary_RecoveryTime;
	public static String ADWinDiary_Generating;
	public static String ADWinDiary_GenerationTime;
	public static String ADWinDiary_DisplayTime;
	public static String ADWinDiary_DataLoss;
	public static String ADWinDiary_Ending;
	public static String ADWinDiary_Approximative;
	public static String ADWinDiary_NoMoreToGenerate;
	public static String ADWinDiary_TryIncreaseBufferSize;
	public static String ADWinDiary_Mean;
	public static String ADWinDiary_Total;
	
	/*
	 * Diary scanner
	 */
	public static String ADWinDiary_Header_Start_Scanner;
	public static String ADWinDiary_Header_Footer_End_Scanner;
	public static String ADWinDiary_TimeBetween_Scanner;
	public static String ADWinDiary_Recovering_Scanner;
	public static String ADWinDiary_RecoveryTime_Scanner;
	public static String ADWinDiary_Generating_Scanner;
	public static String ADWinDiary_GenerationTime_Scanner;
	public static String ADWinDiary_DisplayTime_Scanner;
	public static String ADWinDiary_DataLoss_Scanner;
	public static String ADWinDiary_Ending_Scanner;
	public static String ADWinDiary_Approximative_Scanner;
	public static String ADWinDiary_NoMoreToGenerate_Scanner;
	
	// RS232 ADWin module
	public static String RS232ModuleFrequencyEmptyErrorMessage;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, ADWinMessages.class);
	}

}
