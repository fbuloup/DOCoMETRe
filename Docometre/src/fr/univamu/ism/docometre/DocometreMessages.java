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
package fr.univamu.ism.docometre;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

public final class DocometreMessages extends NLS {
	
	private static final String BUNDLE_NAME = "fr.univamu.ism.docometre.messages";//$NON-NLS-1$
	
	/* General */
	public static String OperationCanceledByUser;

	/*Experiment View*/
	public static String ExperimentsViewTitle;
	public static String NoAssociatedProcessFile;
	
	/*Messages View*/
	public static String MessagesViewTitle;
	public static String ClearMessagesText;
	public static String ScrollLockText;
	
	/*Description View*/
	public static String DescriptionViewTitle;
	public static String EditActionTitle;
	public static String DescriptionHasBeenSaved;
	public static String SaveDescriptionDialogTitle;
	public static String SaveDescriptionDialogMessage;
	public static String SelectedResourceNone;
	public static String SelectedResource;
	public static String DescriptionHasBeenRestored;

	/*Preferences*/
	public static String GeneralPreferences_Description;
	public static String GeneralPreferences_UndoLimit;
	public static String GeneralPreferences_ConfirmUndo;
	public static String GeneralPreferences_ShowWorkspaceDialog;
	public static String GeneralPreferences_ShowTraditionalTabs;
	public static String GeneralPreferences_WineFileLocation;
	
	
	public static String RootPreferencePage_Label;
	public static String DefaultADWinSystemPreference_Description;
	public static String DefaultArduinoUnoSystemPreference_Description;
	public static String ArduinoDelayTimeAfterSerialPrint_Label;
//	public static String ADWinDefaultSystemPreferencePage_Label;
//	public static String NIDefaultSystemPreferencePage_Label;
	
	/*Undo/Redo prompt user*/
	public static String Undo;
	public static String Redo;
	public static String ConfirmUndo;
	public static String Title;
	public static String DoNotConfirm;
	
	/*Actions*/
	public static String DeleteAction_Text;
	public static String DeleteAction_Title;
	public static String DeleteAction_Message;
	public static String PasteAction_Text;
	public static String PasteAction_OverwriteTitle;
	public static String PasteAction_OverwriteMessage;
	public static String PasteAction_OverwriteText;
	public static String PasteAction_ResourceNotExistsMessage; 
	public static String RenameAction_Text;
	public static String RenameResourceAction_TrialErrorMessage;
	public static String NewResourceWizard_ErrorMessage2;
	public static String CopyAction_Text;
	public static String CutAction_Text;
	public static String NewResourceAction_Text;
	public static String NewExperimentAction_Text;
	public static String NewExperimentAction_Tooltip;
	public static String NewDACQConfigurationAction_Text;
	public static String NewDACQConfigurationAction_Tooltip;
	public static String NewSubjectAction_Text;
	public static String NewSubjectAction_Tooltip;
	public static String NewProcessAction_Text;
	public static String NewProcessAction_Tooltip;
	public static String NewSessionAction_Text;
	public static String NewSessionAction_Tooltip;
	public static String NewFolderAction_Text;
	public static String NewFolderAction_Tooltip;
	public static String NewTrialAction_Text;
	public static String RestartAction_Text;
	public static String OpenAction_Text;
	public static String OpenWithSystemEditorAction_Text;
	public static String SetDAQConfigurationAsDefaultAction_Text;
	public static String OpenAction_ImpossibleToLoadProcessWhenNoAssociatedDAQ;
	public static String AssociateWithAction_Text;
	public static String CollapseAllAction_Text;
	public static String CompileProcessAction_Text;
	public static String CompileProcessAction_JobTitle;
	public static String CompileProcessAction_JobMessage;
	public static String CompileProcessAction_CompileOK;
	public static String CompileProcessAction_CompileKO;
	public static String CompileProcessAction_ImpossibleToCompileProcessWhenNoAssociatedDAQ;
	public static String OpenProcessWithSystemEditorAction_EditorNotLaunched;
	public static String OpenProcessWithSystemEditorAction_WaitMessage;
	public static String ADWinProcess_CompileCMDLineMessage;
	public static String ADWinProcess_CompileMessage;
	public static String ADWinProcess_GetCompileErrorsMessage;
	public static String ADWinProcess_CompileErrorsMessage;
	public static String ArduinoUnoProcess_CompileCMDLineMessage;
	public static String ArduinoUnoProcess_CompileMessage;
	public static String ArduinoUnoProcess_GetCompileErrorsMessage;
	public static String ArduinoUnoProcess_CompileErrorsMessage;
	public static String ArduinoUnoProcess_DevicePathErrorMessage;
	public static String OpenProcessWithADWinSystemEditorAction_WaitTaskMessage;
	
	

	/*Menus*/
	public static String Window;
	public static String Show;
	public static String Help;
	public static String Edit;
	public static String File;
	public static String New;

	/*Dialogs*/
	public static String RenameDialogTitle;
	public static String RenameDialogMessage;
	public static String IfBlockConfigurationMessage_1;
	public static String IfBlockConfigurationMessage_2;
	public static String DoBlockConfigurationMessage;
	public static String UseCtrlSpaceProposal;
	public static String When;
	public static String DeleteProcessTestDialogTitle;
	public static String DeleteProcessTestDialogMessage;

	/*Trials label and start mode*/
	public static String Trial;
	public static String Manual;
	public static String Auto;

	/*New Resource Wizard*/
	public static String NewResourceWizardWindowTitle;
	public static String NewResourceWizard_DefaultName;
	public static String NewResourceWizard_ErrorMessage;
	public static String NewResourceWizard_DecorationErrorMessage;
	public static String NewResourceWizard_NameLabel;
	public static String NewResourceWizard_DefaultTrialName;
	public static String AlsoRenameDataFiles;
	
	/*New Experiment Page*/
	public static String NewExperimentWizard_PageName;
	public static String NewExperimentWizard_PageTitle;
	public static String ExperimentDescriptionLabel;
	public static String NewExperimentWizard_PageMessage;

	/*New DAQ Configuration Page*/
	public static String NewDAQConfigurationWizard_PageName;
	public static String NewDAQConfigurationWizard_PageTitle;
	public static String NewDAQConfigurationWizard_PageMessage;
	public static String DAQConfigurationLabel;
	public static String NewDAQConfigurationWizard_ErrorMessage;
	public static String NewDAQConfigurationWizard_Default_Button_Label;

	/*New Process Page*/
	public static String NewProcessWizard_PageName;
	public static String NewProcessWizard_PageTitle;
	public static String NewProcessWizard_PageMessage;
	public static String NewProcessWizard_AssociatedDAQConfiguration;
	public static String NewProcessWizard_ErrorMessage;

	/*New Subject Page*/
	public static String NewSubjectWizard_PageName;
	public static String NewSubjectWizard_PageTitle;
	public static String NewSubjectWizard_PageMessage;

	/*New Session Page*/
	public static String NewSessionWizard_PageName;
	public static String NewSessionWizard_PageTitle;
	public static String NewSessionWizard_PageMessage;
	public static String TrialsNumberLabel;
	public static String NewSessionWizard_ErrorMessage;
	public static String NewSessionWizard_ErrorMessage2;
	public static String UsePrefixForDataFilesNamesLabel;
	public static String UseSessionNameAsSuffixForDataFilesNamesLabel;
	public static String UseTrialNumberAsSuffixForDataFilesNamesLabel;
	public static String ModifyResourceTitle;
	public static String ModifySessionPageTitle;
	public static String ModifySessionMessage;
	public static String ForDataFilesNamesLabel;
	public static String ExampleLabel;
	public static String OrganizeSessionWizardPageTitle;
	public static String OrganizeSessionWizardPageDescription;
	
	/*Import resource Wizard*/
	public static String ImportResourceWizardWindowTitle;
	public static String ImportResourceWizardTitle;
	public static String ImportResourceWizardMessage;
	public static String ImportResourceWizardErrorMessage1;
	public static String ImportResourceWizardErrorMessage2;
	public static String ImportResourceWizardErrorMessage3;
	public static String NewSubjectFromADWDataFileLabel;
	public static String ImportingExperimentFromCompressedFile;
	public static String CreatingNewExperimentInWorkspace;
	public static String RefreshingWorkspace;
	public static String AddingProjectToBuilderAndRefreshingExperiment;
	public static String ApplyingProperty;
	public static String UnzippingFile;
	public static String ImportErrorMessage;
	public static String ImportErrorDialogTitle;
	public static String DataProcessingTitle;
	
	/*New Folder Page*/
	public static String NewFolderWizard_PageName;
	public static String NewFolderWizard_PageTitle;
	public static String NewFolderWizard_PageMessage;

	/*New Trial Page*/
	public static String NewTrialWizard_PageName;
	public static String NewTrialWizard_PageTitle;
	public static String NewTrialWizard_PageMessage;
	public static String NewTrialWizard_ErrorMessage;
	
	/*New Parameters File Page*/
	public static String NewParametersFileWizard_PageName;
	public static String NewParametersFileWizard_PageTitle;
	public static String NewParametersFileWizard_PageMessage;

	/*Workspace Dialog*/
	public static String ChooseWorkspaceDialog_dialogTitle;
	public static String ChooseWorkspaceDialog_dialogMessage;
	public static String UnsupportedVM_message;
	public static String ChooseWorkspaceDialog_defaultProductName;
	public static String ChooseWorkspaceDialog_dialogName;
	public static String ChooseWorkspaceDialog_workspaceEntryLabel;
	public static String ChooseWorkspaceDialog_browseLabel;
	public static String ChooseWorkspaceDialog_directoryBrowserTitle;
	public static String ChooseWorkspaceDialog_directoryBrowserMessage;
	public static String ChooseWorkspaceDialog_useDefaultMessage;
	
	/*ADWin DAQ General configuration page*/
	public static String SetAsDefaultInPreferences_Tooltip;
	
	/*Activator load/save model error message*/
	public static String ImpossibleToLoadModel;
	public static String ImpossibleToSaveModel;

	/*Activator current workspace message*/
	public static String CurrentWorkspace;
	
	/*Channels properties*/
	public static String Name_Label;
	public static String Name_Tootip;
	public static String SampleFrequency_Label;
	public static String SampleFrequency_Tooltip;
	public static String ChannelNumber_Label;
	public static String ChannelNumber_Tooltip;
	public static String TransfertNumber_Label;
	public static String TransfertNumber_Tooltip;
	public static String BufferSize_Label;
	public static String BufferSize_Tooltip;
	public static String Transfert_Label;
	public static String Transfert_Tooltip;
	public static String AutoTransfert_Tooltip;
	public static String AutoTransfert_Label;
	public static String Record_Tooltip;
	public static String Record_Label;
	
	/*
	 * Module form page
	 */
	// General configuration module section title and description
	public static String GeneralConfigurationModuleSection_Title;
	public static String GeneralConfigurationModuleSection_Description;
	//General configuration module section title and description
	public static String ChannelsConfigurationModuleSection_Title;
	public static String ChannelsConfigurationModuleSection_Description;
	
	/* Process editor */
	public static String InitializeSegmentEditorTitle;
	public static String FinalizeSegmentEditorTitle;
	public static String EventSegmentEditorTitle;
	public static String ADBasicSourceCodeEditorTitle;
	public static String PaletteDrawerTitle;
	public static String ConnectionToolTitle;
	public static String ConnectionToolDescription;
	public static String IfBlockToolTitle;
	public static String IfBlockToolDescription;
	public static String DoBlockToolTitle;
	public static String DoBlockToolDescription;
	public static String FunctionBlockToolTitle;
	public static String FunctionBlockToolDescription;
	public static String CommentBlockToolTitle;
	public static String CommentBlockToolDescription;
	public static String EditToolTipTitle;
	public static String ZoomToTitle;
	public static String ZoomToFitTitle;
	public static String ShowGridTitle;
	public static String BlockAlignmentHelperTitle;
	public static String ModifyBlockText;
	public static String AssignTitle;
	public static String BeginEditorTitle;
	public static String LoopEditorTitle;
	public static String EndEditorTitle;
	public static String ArduinoUnoSourceCodeEditorTitle;
	public static String ToggleActivatedState;

	/* Dialogs */
	public static String BlockDialogShellTitle;
	public static String ConditionalBlockConfigurationTitle;
	public static String ConditionalBlockConfigurationMessage;
	public static String LoopBlockConfigurationTitle;
	public static String LoopBlockConfigurationMessage;
	public static String CommentBlockConfigurationTitle ;
	public static String CommentBlockConfigurationMessage;
	public static String NewChartDialogShellTitle;
	public static String NewChartDialogTitle;
	public static String NewChartDialogMessage;
	public static String ConfigureChartsLayoutDialogShellTitle;
	public static String ConfigureChartsLayoutDialogTitle;
	public static String ConfigureChartsLayoutDialogMessage;
	public static String ChartTypeLabel_Title;
	public static String ChartLayoutDialogErrorMessage_Title;
	public static String DeleteColumn_Title;
	public static String AddColumn_Title;
	
	/* Charts configuration layout */
	public static String HSpan_Label;
	public static String HSpan_Tooltip;
	public static String VSpan_Label;
	public static String VSpan_Tooltip;
	
	/* Charts Configuration page*/
	public static String ChartsConfigurationPage_PageTitle;
	public static String ChartsConfigurationPage_Title;
	public static String OscilloChartLabel;
	public static String ScrollChartLabel;
	public static String XYChartLabel;
	public static String GaugeChartLabel;
	public static String TankChartLabel;
	
	
	public static String rangeMaxAmplitude_Title;
	public static String rangeMaxAmplitude_Tooltip;
	public static String rangeMinAmplitude_Title;
	public static String rangeMinAmplitude_Tooltip;
	public static String ShowLow_Title;
	public static String ShowLow_Tooltip;
	public static String ShowLowLow_Title;
	public static String ShowLowLow_Tooltip;
	public static String ShowHigh_Title;
	public static String ShowHigh_Tooltip;
	public static String ShowHighHigh_Title;
	public static String ShowHighHigh_Tooltip;;
	public static String LevelLow_Title;
	public static String LevelLow_Tooltip;
	public static String LevelLowLow_Title;
	public static String LevelLowLow_Tooltip;
	public static String LevelHigh_Title;
	public static String LevelHigh_Tooltip;
	public static String LevelHighHigh_Title;
	public static String LevelHighHigh_Tooltip;
	
	public static String ChartsConfigurationSection_Title;
	public static String ChartsConfigurationSectionDescription;
	public static String DeleteChartsToolItem_Tooltip; 
	public static String AddChartToolItem_Tooltip;
	public static String MoveChartUpToolItem_Tooltip;
	public static String MoveChartDownToolItem_Tooltip;
	public static String ChartLayoutToolItem_Tooltip;
	public static String CurvesConfigurationSection_Title;
	public static String CurvesConfigurationSectionDescription;
	public static String AddCurveToolItem_Tooltip;
	public static String DeleteCurvesToolItem_Tooltip;
	public static String TimeWidth_Title;
	public static String TimeWidth_Tooltip;
	public static String AutoScale_Title;
	public static String AutoScale_Tooltip;
	public static String MaxAmplitude_Title;
	public static String MaxAmplitude_Tooltip;
	public static String MinAmplitude_Title;
	public static String MinAmplitude_Tooltip;
	public static String Font_Title;
	public static String Font_Tooltip;
	public static String DisplayValues_Title;
	public static String DisplayValues_Tooltip;
	public static String DisplayValuesCurves_Title;
	public static String DisplayValuesCurves_Tooltip;

	public static String xMaxAmplitude_Title;
	public static String xMaxAmplitude_Tooltip;
	public static String xMinAmplitude_Title;
	public static String xMinAmplitude_Tooltip;
	public static String yMaxAmplitude_Title;
	public static String yMaxAmplitude_Tooltip;
	public static String yMinAmplitude_Title;
	public static String yMinAmplitude_Tooltip;
	
	public static String DecreaseVSpan_Tooltip;
	public static String IncreaseVSpan_Tooltip;
	public static String DecreaseHSpan_Tooltip;
	public static String IncreaseHSpan_Tooltip;
	public static String MoveChartLeft_Tooltip;
	public static String MoveChartRight_Tooltip;
	public static String Color_Label;
	public static String Color_Tooltip;
	public static String Style_Label;
	public static String Style_Tooltip;
	public static String Width_Label;
	public static String Width_Tooltip;
	public static String ChannelName_Label;
	public static String ChannelName_Tooltip;
	public static String DeleteChartsDialog_Title;
	public static String DeleteChartsDialog_Message;
	public static String DeleteChartsOperation_Label;
	public static String AddChartOperation_Label;
	public static String MoveChartOperation_Label;
	public static String Down_Label;
	public static String Up_Label;
	public static String AddCurveOperation_Label;
	public static String DeleteCurvesOperation_Label;
	public static String DeleteCurvesDialog_Title;
	public static String DeleteCurvesDialog_Message;
	public static String AddCurvesDialog_ShellTitle;
	public static String AddCurvesDialog_Title;
	public static String AddCurvesDialog_Message;
	public static String ErrorValueAlreadyUsed;
	public static String ReferenceValueDialogTitle;
	public static String ReferenceValueDialogMessage;
	public static String AssociatedProcessLabel;
	public static String XChannelName_Label;
	public static String XChannelName_Tooltip;
	public static String YChannelName_Label;
	public static String YChannelName_Tooltip;
	
	/* Charts preference */
	public static String SHOW_CURSOR;
	public static String SHOW_MARKER;
	public static String UseSameColorForSameCategory;
	public static String Categories;
	
	/* Redirect console preferences */
	public static String REDIRECT_GROUP_TITLE;
	public static String REDIRECT_BUTTON_TITLE;
	public static String REDIRECT_FILE_ABSOLUTE_PATH;
	public static String REDIRECT_LABEL_TITLE;
	
	/* Acquire perspective toolbar */
	public static String ItemToRun_Label;
	public static String RunAction_Tooltip;
	public static String StopAction_Tooltip;
	
	/* Analyse perspective toolbar */
	public static String StartMathEngine_Tooltip;
	public static String StopMathEngine_Tooltip;
	
	/* Acquire perspective status bar */
	public static String Experiment_Label;
	public static String Subject_Label;
	public static String Session_Label;
	public static String Trial_Label;
	public static String Process_Label;
	public static String NotAvailable_Label;

	/* Select item to launch dialog */
	public static String SelectItemDialogTitle;
	public static String SelectItemDialogShellTitle;
	public static String SelectItemDialogMessage;
	
	/* Copy log and data files dialog */
	public static String CopyLogAndDataFilesDialogTitle; 
	public static String CopyLogAndDataFilesDialogQuestion; 
			
	/* Experiment scheduler message */
	public static String CantFindPreviousTrialMessage;
	public static String CantFindLastTrialMessage;
	public static String Warning;
	public static String CantFindNextTrialMessage;
	public static String CantFindFirstTrialMessage;
	public static String RunningTrials;
	public static String RunningProcess;
	public static String Error;
	public static String Information;
	public static String NoAssociatedProcess;
	public static String SchedulerStopped;
	
	/* Processes Log files */
	public static String Diary;
	
	/* Parameters file and other... */
	public static String ModifyParametersFile;
	public static String ParameterNameNotfound;
	public static String ParameterValueNotfound;
	public static String NoParameterFileFound;
	
	/* Stop trial dialog */
	public static String StopTrialDialogTitle;
	public static String StopTrialDialogMessage;
	public static String StopTrialDialogStopNow;
	public static String StopTrialDialogWait;
	public static String TrialDialogUseChoiceDontAsk;
	public static String StopTrialImmediatlyWhenAsked;
	public static String StopTrialDontAsk;
	
	/* Validate trial dialog */
	public static String ValidateTrialDialogTitle;
	public static String AutoValidateTrial;
	public static String AutoStartTrial;
	public static String DoYouWantValidTrial;
	public static String RedoTrialNow;
	public static String ContinueLabel;
	
	/* Next trial dialog */
	public static String NextTrialDialogTitle;
	public static String DoYouWantToStartNextTrial;
	public static String AutoStartTrialDontAsk;
	
	/* Block titles et tooltip */
	public static String NewIfBlockName;
	public static String NewDoBlockName;
	public static String NewFunctionName;
	public static String NewFunctionTooltip;
	public static String NewCommentName;
	
	/* Functions configuration dialog */
	public static String FunctionConfigurationDialog;
	public static String FunctionConfigurationDialog_Title;
	public static String FunctionConfigurationDialog_Message;
	
	/* Functions comment */
	public static String Comment;
	
	/* ADwin Analog Input Function configuration dialog */
	public static String SelectedAnalogInputLabel;
	
	/* ADwin Analog Output Function configuration dialog */
	public static String SelectedAnalogOutputLabel;
	
	/* ADwin Digital Output Function configuration dialog */
	public static String SelectedDigitalOutputLabel;
	public static String ValueLabel;
	
	/* ADwin Digital Input Function configuration dialog */
	public static String SelectedDigitalInputLabel;
	
	/* ADwin filtered derivative and averaging filter Function configuration dialog */
	public static String InputLabel;
	public static String OutputLabel;
	public static String aValueLabel;
	public static String fcValueLabel;
	public static String feValueLabel;
	
	/* ADwin averaging filter Function configuration dialog */
	public static String bValueLabel;
	
	
	/* ADwin serial output function */
	public static String NoSerialModuleFound;
	public static String ModuleNumberLabel;
	public static String PortNumberLabel;
	public static String StringValueLabel;
	public static String ASCIICodeValueLabel;
	public static String CRLFValueLabel;
	
	/* ADwin serial output function */
	public static String ASCIICodeVariableNameLabel;
	public static String ByteValuesLabel;
	
	/* ADWin float/long serial output function */
	public static String SerialPortDialogTitle;
	public static String NoSerialPortDefinedLabel;
	public static String FloatValueLabel;
	public static String LongValueLabel;
	public static String FloatValueNotValidLabel;
	public static String LongValueNotValidLabel;
	public static String ByteValueNotValidLabel;
	
	/* ADWin JVL MAC141 mode function */
	public static String ModeValueLabel;
	
	/* ADWin JVL MAC141 read/write register function */
	public static String RegisterTypeLabel;
	
	/* ADWin JVL MAC141 read register function */
	public static String RegisterValueLabel;
	
	/* Find dialog */
	public static String FindDialogShellTitle;
	public static String FindDialogLabelTitle;
	public static String FindDialogButtonTitle;
	public static String FindDialogSearchBackwardButtonTitle;
	public static String FindDialogCaseSensitiveButtonTitle;
	public static String FindDialogWholeWordButtonTitle;
	
	/* Parameters Dialog */
	public static String ParametersDialogShellTitle;
	public static String ParametersDialogTitle;
	public static String ParametersDialogMessage;
	public static String ParametersDialogParamNameColumn;
	public static String ParametersDialogParamValueColumn;
	public static String ProcessLaunchCanceled;
	
	/* Calibration monitoring */
	public static String ShowCalibratedValues;
	public static String AmpMinTooltip;
	public static String AmpMaxTooltip;
	public static String UnitMinTooltip;
	public static String UnitMaxTooltip;
	public static String CaptureValueMin;
	public static String CaptureValueMax;
	public static String CalibrationMonitoringDialogShellTitle;
	public static String CalibrationMonitoringDialogTitle;
	public static String CalibrationMonitoringDialogMessage;
	public static String Inputs;
	public static String Outputs;
	
	// DACQ configuration editor undo/redo modify operation label
	public static String ErrorFileFolderNotExists;
	public static String ModifyPropertyOperation_Label;
	public static String Browse;
	public static String ErrorFileFolderNotValid;
	

	public static String GlobalFrequency_Label;
	public static String GlobalFrequency_Tooltip;
	
	//Change Frequency Dialog
	public static String ChangeFrequencyDialogTitle;
	public static String ChangeFrequencyDialogMessage;
	public static String ChangeFrequencyDialogError;
	
	// Variables page
	public static String VariablesPage_PageTitle;
	public static String TransferInfo_MessageTitle;
	public static String TransferInfo_MessageContent;
	
	// Arduino Device selection dialog
	public static String DeviceSelectionDialog_Title;
	
	/*
	 * Status line
	 */
	public static String Workload_Time;
	public static String PausePending;
	
	// Import Wizard
	public static String ResourceTypeLabel;
	public static String DestinationLabel;
	public static String ParentFolderLabel;
	
	// Rename data files Job
	public static String RenameDataFileJobTitle;
	public static String CollectDataFileTaskTitle;
	public static String RenameDataFileTaskTitle;
	
	// Remove Log and data files Job
	public static String RemoveLogAndDataFileJobTitle;
	public static String RemoveLogAndDataFileTaskTitle;
	public static String RemoveLogAndDataDialogTitle;
	public static String RemoveLogAndDataDialogMessage;
	public static String RemoveLogAndDataDialogMessage2;
	
	// Customer function error message
	public static String IsNotAValidValue;
	public static String CustomerFunctionsMenuLabel;
	
	// Math Engine
	public static String MathEngineStartStop;
	public static String MathEngineStarting;
	public static String MathEngineStarted;
	public static String MathEngineStopping;
	public static String MathEngineStopped;
	public static String WaitingForMathEngine;
	public static String PleaseWait;
	public static String UpdateCacheChannels;
	
	// Math Engine Preferences
	public static String MathEngineLabel;
	public static String AlwaysLoadFromSavedData;
	public static String AlwaysLoadFromSavedData_Tooltip;
	public static String LoadSubjectFromRawDataDialog_Title;
	public static String LoadSubjectFromRawDataDialog_Message;
	
	// Matlab preferences 
	public static String MatlabPreferences_Description;
	public static String MatlabEngineShowMatlabWindow;
	public static String MatlabEngineTimeOut;
	public static String MatlabEngineScriptLocation;
	public static String MatlabEngineLocation;
	
	// Python preferences 
	public static String PythonPreferences_Description;
	public static String PythonEngineTimeOut;
	public static String PythonEngineScriptLocation;
	public static String PythonEngineLocation;
	
	// Export to zip or tar
	public static String DataTransfer_errorExporting;
	public static String FileSystemExportOperation_problemsExporting;
	public static String ZipExport_cannotOpen;
	public static String DataTransfer_exportingTitle;
	public static String ZipExport_cannotClose;
	public static String DataTransfer_export;
	public static String CompressLabel;
	public static String SaveInZipFormatLabel;
	public static String SaveInTarFormatLabel;
	public static String OptionsGroupLabel;
	public static String DestinationFolderLabel;
	public static String ProjectToExportLabel;
	public static String ExportResourceWizardPageTitle;
	public static String ExportResourceWizardPageDescription;
	public static String ExportResourceWizardTitle;
	public static String ExportExperimentTaskTitle;
	public static String createPropertiesFile;
	public static String WritingProperties;
	public static String ExportOnlySelectedSubjectTitle;
	public static String ExportWithDataTitle;
	public static String PleaseSelectAnExperimentMessage;
	public static String PleaseSelectADestinationMessage;
	public static String DestinationAlreadyExistsMessage;
	public static String ExportWarningMessage;
	
	//
	public static String UndoneTrials;
	
	// Organize session wizard page
	// Left container
	public static String OrganizeSessionWizardPage_availabelProcessesLabel;
	public static String OrganizeSessionWizardPage_addAllButton;
	public static String OrganizeSessionWizardPage_addButton;
	public static String OrganizeSessionWizardPage_selectedProcessesLabel;
	public static String OrganizeSessionWizardPage_removeButton;
	public static String OrganizeSessionWizardPage_removeAllButton;
	// Right container
	public static String OrganizeSessionWizardPage_selectedTrialsTabItem;
	public static String OrganizeSessionWizardPage_selectedTrialsLabel;
	public static String OrganizeSessionWizardPage_trialsListText;
	public static String OrganizeSessionWizardPage_selectedTrialsGroup;
	public static String OrganizeSessionWizardPage_selectedProcessesListButton;
	public static String OrganizeSessionWizardPage_randomDistributionButton1;
	public static String OrganizeSessionWizardPage_randomDistributionButton2;
	public static String OrganizeSessionWizardPage_packTrialsTabItem;
	public static String OrganizeSessionWizardPage_packTrialsSizeLabel;
	public static String OrganizeSessionWizardPage_packTrialsSizeText;
	public static String OrganizeSessionWizardPage_packTrialsGroup;
	public static String OrganizeSessionWizardPage_selectedProcessesListButton2;
	public static String OrganizeSessionWizardPage_randomDistributionButton3;
	public static String OrganizeSessionWizardPage_resultLabel;
	public static String OrganizeSessionWizardPage_applyButton;
	// Applying messages
	public static String BadTrialsList;
	public static String ApplyWait;
	public static String BuildTrialsList;
	public static String RefreshViewers;
	public static String ChooseProcess;
	public static String AssociatingProcess;
	public static String BadPacketTrialsSize;
	
	public static String SelectExperimentDialogMessage;
	public static String SelectExperimentDialogTitle;
	public static String SelectExperimentToProcessDataFilesMessage;
	
	public static String LoadingSubject;
	public static String UnloadingSubject;
	public static String AutoUnLoadSubjectTitle;
	public static String Done;
	
	public static String PleaseStartMathEngineFirst;
	
	// Channel Editor
	public static String MarkersGroupTitle;
	public static String GroupNameLabel;
	public static String GraphicalSymbolLabel;
	public static String TrialNumberLabel;
	public static String XMarkerValueLabel;
	public static String YMarkerValueLabel;
	public static String FeaturesGroupTitle;
	public static String FeaturesNameLabel;
	public static String TrialsGroupLabel;
	public static String FrontCutLabel;
	public static String EndCutLabel;
	public static String SamplesNumberLabel;
	public static String DurationLabel;
	public static String GeneralInfoGroupLabel;
	public static String FrequencyLabel;
	public static String TrialsNumberLabel2;
	public static String SignalNameLabel;
	public static String DeleteSelectedMarkersGroupTooltip;
	public static String CreateNewMarkersGroupTooltip;
	public static String DeleteSelectedMarkerTrialTooltip;
	public static String DeleteSelectedFeatureTooltip;
	public static String CategoryCriteriaLabel;
	public static String CreateNewMarkersGroupDialogTitle;
	public static String CreateNewMarkersGroupDialogMessage;
	public static String DeleteMarkerDialogTitle;
	public static String DeleteMarkerDialogMessage;
	public static String DeleteMarkersGroupDialogTitle;
	public static String DeleteMarkersGroupDialogMessage;
	public static String DeleteFeatureDialogTitle;
	public static String DeleteFeatureDialogMessage;
	public static String MarkerLabelInvalid;
	

	// Data processing editor wizard
	public static String NewDataProcessingWizard_PageTitle;
	public static String NewDataProcessingWizard_PageMessage;
	public static String NewDataProcessingWizard_PageName;
	
	// Batch data processing editor wizard
	public static String NewBatchDataProcessingWizard_PageTitle;
	public static String NewBatchDataProcessingWizard_PageMessage;
	public static String NewBatchDataProcessingWizard_PageName;
	
	// XY Chart 
	public static String NewXYChartWizard_PageTitle;
	public static String NewXYChartWizard_PageMessage;
	public static String NewXYChartWizard_PageName;
	public static String GraphicalCutsTitle;
	public static String ScaleValueTitle;
	public static String ApplyTitle;
	public static String AddNewCurveToolTip;
	public static String RemoveCurveToolTip;
	public static String XAxisSelectionDialogTitle;
	public static String XAxisSelectionDialogMessage;
	public static String YAxisSelectionDialogTitle;
	public static String YAxisSelectionDialogMessage;
	public static String CurvesSelectionDialogTitle;
	public static String CurvesSelectionDialogMessage;
	public static String ShowMarkersLabelsTitle;
	public static String ShowMarkersTitle;
	public static String MarkersSizeTitle;
	public static String SomethingWentWrong;
	public static String PressEnter;
	public static String PressEnterFrontCut;
	public static String PressEnterEndCut;
	public static String SubjectNotLoaded;
	
	// Data processing editor page editor titles
	public static String MathEngineEditorTitle;
	public static String MathEngineSourceCodeEditorTitle;
	
	// Functional Block Configuration Dialog Blocking Message
	public static String FunctionalBlockConfigurationDialogBlockingMessage;
	public static String ImpossibleToFindChannelTitle;
	
	// Saving subject handler
	public static String SavingSubject;
	
	public static String RecordSubjectDialogTitle;
	public static String RecordSubjectDialogMessage;
	
	// Mathengine view
	public static String sendButtonTitle;
	public static String commandTitle;
	public static String RevealEODTitle;
	
	// Rename resource operation
	public static String RenameRessourceTaskTitle;
	public static String CheckIsDacqSubTaskTitle;
	public static String ComputeFileExtensionSubTaskTitle;
	public static String RenameAndGetNewResourceSubTaskTitle;
	public static String UpdateDefaultDacqSubTaskTitle;
	public static String UpdateAffectedProcessesSubTaskTitle;
	public static String UpdateAffectedProcessTestsAndTrialsSubTaskTitle;
	public static String UpdateEditorsSubTaskTitle;
	public static String RenameInMathengineSubTaskTitle;
	public static String ErrorRenamingSubjectInMathengine;
	public static String ErrorRenamingEsxperimentInMathengine;
	public static String RefreshExperimentsSubjectsViewsSubTaskTitle;
	public static String ErrorRenamingResource;
	
	// Batch processing editor
	public static String Delete_Tooltip;
	public static String Add_Tooltip;
	public static String EnableDisable_Tooltip;
	public static String Processes;
	public static String Subjects;
	public static String SelectProcessDialogTitle;
	public static String SelectProcessDialogMessage;
	public static String SelectSubjectDialogMessage;
	public static String SelectSubjectDialogTitle;
	public static String OrganizeProcessesAndSubjects;
	public static String Explanation;
	public static String AutoLoadSubjectTitle;
	public static String Introduction;
	public static String AddProcessModifyOperationLabel;
	public static String AddSubjectModifyOperationLabel;
	public static String RemoveProcessModifyOperationLabel;
	public static String RemoveSubjectModifyOperationLabel;
	public static String EnableDisableProcessModifyOperationLabel;
	public static String EnableDisableSubjectModifyOperationLabel;
	public static String MoveUpProcessModifyOperationLabel;
	public static String MoveDownProcessModifyOperationLabel;
	public static String MoveUpSubjectModifyOperationLabel;
	public static String MoveDownSubjectModifyOperationLabel;
	public static String GetAllDataProcessingLabel;
	public static String GenerateGlobalScriptLabel;
	public static String GetAllSubjectsLabel;
	public static String LoadingLabel;
	public static String ProcessingLabel;
	public static String SavingLabel;
	public static String UnloadingLabel;
	public static String RunningScriptLabel;

	// Close workbench
	public static String CloseMathEngineDialogTitle;
	public static String CloseMathEngineDialogMessage;

	// Export data wizard page
	public static String ExportDataWizardPageTitle;
	public static String ExportDataWizardPageDescription;
	public static String ExportDataWizardPageAdd;
	public static String ExportDataWizardPageRemoveSelection;
	public static String ExportDataWizardPageRemoveAll;
	public static String ExportDestination;
	public static String ExportType;
	public static String ExportInSingleFile;
	public static String SingleFileName;
	public static String Separator;
	public static String DataExportError1;
	public static String DataExportError2;
	public static String DataExportError3;
	public static String DataExportError4;
	public static String DataExportError5;
	public static String DataExportMainTaskTitle;
	public static String DataExportSignalSubtaskMessage;
	public static String DataExportErrorOccuredDialogTitle;
	public static String DataExportErrorOccuredDialogMessage;

	

	

//	public static String OrganizeSessionWizardPage_button_1_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_1_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_2_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_3_text;
//	public static String OrganizeSessionWizardPage_grpHjklgljkgl_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_text;
//	public static String OrganizeSessionWizardPage_tbtmNewItem_1_text;
//	public static String OrganizeSessionWizardPage_btnNewButton_text;
//	public static String OrganizeSessionWizardPage_btnNewButton_1_text;
//	public static String OrganizeSessionWizardPage_btnNewButton_2_text;
//	public static String OrganizeSessionWizardPage_btnNewButton_3_text;
//	public static String OrganizeSessionWizardPage_tbtmRpartirParSletion_text;
//	public static String OrganizeSessionWizardPage_tbtmRpartirParPaquet_text;
//	public static String OrganizeSessionWizardPage_lblNewLabel_text;
//	public static String OrganizeSessionWizardPage_lblNewLabel_text;
//	public static String OrganizeSessionWizardPage_text_text;
//	public static String OrganizeSessionWizardPage_group_text;
//	public static String OrganizeSessionWizardPage_btnRadioButton_text;
//	public static String OrganizeSessionWizardPage_btnRadioButton_text;
//	public static String OrganizeSessionWizardPage_btnRadioButton_1_text;
//	public static String OrganizeSessionWizardPage_lblNewLabel_1_text;
//	public static String OrganizeSessionWizardPage_text_1_text;
//	public static String OrganizeSessionWizardPage_group_1_text;
//	public static String OrganizeSessionWizardPage_btnRadioButton_2_text;
//	public static String OrganizeSessionWizardPage_btnRadioButton_3_text;
			
	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(new Locale("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, DocometreMessages.class);
	}

}
