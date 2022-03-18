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

public interface IImageKeys {
	
	public static final String ERROR_ICON = "icons/error.gif"; //$NON-NLS-1$
	public static final String WARNING_ICON = "icons/warning.gif"; //$NON-NLS-1$
	
	public static final String ERROR_ANNOTATION_ICON = "icons/errorAnnotation.gif"; //$NON-NLS-1$
	public static final String WARNING_ANNOTATION_ICON= "icons/warningAnnotation.gif"; //$NON-NLS-1$
	
	public static final String ERROR_DECORATOR = "ERROR_DECORATOR"; //$NON-NLS-1$
	public static final String WARNING_DECORATOR = "WARNING_DECORATOR"; //$NON-NLS-1$
	
	//Experiments View
	public static final String EXPERIMENT_ICON = "icons/experiment.gif"; //$NON-NLS-1$
	public static final String SUBJECT_ICON = "icons/subject.png"; //$NON-NLS-1$
	public static final String SESSION_ICON = "icons/session.gif"; //$NON-NLS-1$
	public static final String DACQ_CONFIGURATION_ICON = "icons/daqConfiguration.png"; //$NON-NLS-1$
	public static final String FOLDER_ICON = "icons/folder.gif"; //$NON-NLS-1$
	public static final String PROCESS_ICON = "icons/process.gif"; //$NON-NLS-1$
	public static final String EXPERIMENTS_VIEW_ICON = "icons/experimentsView.gif"; //$NON-NLS-1$
	public static final String DAQ_CONFIGURATION_ACTIVE_OVERLAY = "icons/activeDAQConfiguration.gif"; //$NON-NLS-1$
	public static final String SUBJECT_LOADED_OVERLAY = "icons/subjectLoaded.gif"; //$NON-NLS-1$
	public static final String COLLAPSE_ALL = "icons/collapseAll.gif"; //$NON-NLS-1$
	public static final String PARAMETERS_FILE_ICON = "icons/parameters.gif"; //$NON-NLS-1$

	//Messages View
	public static final String MESSAGES_VIEW_ICON = "icons/messagesView.gif"; //$NON-NLS-1$
	public static final String CLEAR_CONSOLE_ICON = "icons/clearConsole.gif"; //$NON-NLS-1$
	public static final String SCROLL_LOCK_ICON = "icons/scrollLock.gif"; //$NON-NLS-1$
	
	//Description View
	public static final String DESCRIPTION_VIEW_ICON = "icons/descriptionView.gif"; //$NON-NLS-1$
	public static final String EDIT_DESCRIPTION_ICON = "icons/editDescription.gif"; //$NON-NLS-1$
	
	//New resource wizard banner
	public static final String NEW_RESOURCE_BANNER = "icons/newResourceWizardBanner.png"; //$NON-NLS-1$
	

	public static final String ADD_ICON = "icons/add.gif"; //$NON-NLS-1$
	public static final String ADD_ALL_ICON = "icons/addAll.png"; //$NON-NLS-1$
	public static final String REMOVE_ICON = "icons/remove.png"; //$NON-NLS-1$
	public static final String REMOVE_ALL_ICON = "icons/removeAll.png"; //$NON-NLS-1$
	public static final String MODULE_WIZBAN = "icons/module.png"; //$NON-NLS-1$
	
	public static final String DOWN_ICON = "icons/down.gif"; //$NON-NLS-1$
	public static final String UP_ICON = "icons/up.gif"; //$NON-NLS-1$
	public static final String CHARTS_LAYOUT_ICON = "icons/chartsLayout.gif"; //$NON-NLS-1$
	
	public static final String CHECK_BOX_CHECKED_ICON = "icons/checkboxChecked.gif"; //$NON-NLS-1$
	public static final String CHECK_BOX_UNCHECKED_ICON = "icons/checkboxUnchecked.gif"; //$NON-NLS-1$
	

	public static final String APPLY_DEFAULT_SETTINGS_ICON = "icons/setting.png"; //$NON-NLS-1$
	
	//Process editor
	public static final String IF_BLOCK_16_ICON = "icons/IF_16.png"; //$NON-NLS-1$
	public static final String IF_BLOCK_32_ICON = "icons/IF_32.png"; //$NON-NLS-1$
	public static final String DO_BLOCK_16_ICON = "icons/DO_16.png"; //$NON-NLS-1$
	public static final String DO_BLOCK_32_ICON = "icons/DO_32.png"; //$NON-NLS-1$
	public static final String FUNCTION_BLOCK_16_ICON = "icons/FUNCTION_16.png"; //$NON-NLS-1$
	public static final String FUNCTION_BLOCK_32_ICON = "icons/FUNCTION_32.png"; //$NON-NLS-1$
	public static final String COMMENT_BLOCK_16_ICON = "icons/comment_16.png"; //$NON-NLS-1$
	public static final String COMMENT_BLOCK_32_ICON = "icons/comment_32.png"; //$NON-NLS-1$
	public static final String NEW_CONNECTION_16_ICON = "icons/newConnection_16.png"; //$NON-NLS-1$
	public static final String NEW_CONNECTION_32_ICON = "icons/newConnection_32.png"; //$NON-NLS-1$
	public static final String YES_ICON = "icons/yes2.png"; //$NON-NLS-1$
	public static final String NO_ICON = "icons/no2.png"; //$NON-NLS-1$
	public static final String DO_LOOP_ICON = "icons/DoLoop.gif"; //$NON-NLS-1$
	public static final String EDIT_ICON = "icons/edit.gif"; //$NON-NLS-1$
	public static final String ZOOM_TO_FIT = "icons/zoomtofit.gif"; //$NON-NLS-1$
	public static final String ZOOM_IN = "icons/zoom_in.gif"; //$NON-NLS-1$
	public static final String ZOOM_OUT = "icons/zoom_out.gif"; //$NON-NLS-1$
	public static final String ZOOM_SCALE_1 = "icons/zoom_scale.gif"; //$NON-NLS-1$
	

	public static final String SELECT_ALL = "icons/selectAll.gif"; //$NON-NLS-1$
	public static final String RECORD_ALL = "icons/recordAll.gif"; //$NON-NLS-1$
	

	public static final String ADD_COLUMN_ICON = "icons/addColumn.gif"; //$NON-NLS-1$
	public static final String DELETE_COLUMN_ICON = "icons/deleteColumn.gif"; //$NON-NLS-1$
	public static final String RIGHT_ICON = "icons/right.gif"; //$NON-NLS-1$
	public static final String LEFT_ICON = "icons/left.gif"; //$NON-NLS-1$

	public static final String SELECT_ICON = "icons/select.gif"; //$NON-NLS-1$
	public static final String RUN_ICON = "icons/run.gif"; //$NON-NLS-1$
	public static final String STOP_ICON = "icons/stop.gif"; //$NON-NLS-1$
	
	public static final String DONE_ICON = "icons/done.gif"; //$NON-NLS-1$
	public static final String UNDONE_ICON = "icons/undone.gif"; //$NON-NLS-1$

	public static final String DIARY_ICON = "icons/diary.gif"; //$NON-NLS-1$
	public static final String SAMPLES_ICON = "icons/samples.gif"; //$NON-NLS-1$
	
	public static final String CONFIGURE_FUNCTION_WIZBAN = "icons/configureFunctionBanner.gif"; //$NON-NLS-1$
	public static final String COMMENT_WIZBAN = "icons/CommentBanner.png"; //$NON-NLS-1$
	
	public static final String FILTERED_DERIVATOR_WIZBAN = "icons/FilteredDerivatorEq.png"; //$NON-NLS-1$
	
	public static final String AVERAGING_FILTER_WIZBAN = "icons/AveragingFilterEq.png"; //$NON-NLS-1$
	
	public static final String VERTICAL_ZOOM_ICON = "icons/zoomV.gif"; //$NON-NLS-1$
	public static final String HORIZONTAL_ZOOM_ICON = "icons/zoomH.gif"; //$NON-NLS-1$
	public static final String ZOOM_ICON = "icons/zoom.gif"; //$NON-NLS-1$
	public static final String AUTO_SCALE_ICON = "icons/autoscale.gif"; //$NON-NLS-1$
	public static final String EDIT_GRAPH_ICON = "icons/editGraph.gif"; //$NON-NLS-1$
	public static final String PAN_ICON = "icons/pan.png"; //$NON-NLS-1$
	public static final String CALIBRATION_MONITORING_ICON = "icons/calibrationMonitoring.png"; //$NON-NLS-1$
	public static final String ON_ICON = "icons/ON.png"; //$NON-NLS-1$
	public static final String OFF_ICON = "icons/OFF.png"; //$NON-NLS-1$
	public static final String SHOW_GRAPH_ICON = "icons/left.png"; //$NON-NLS-1$
	public static final String HIDE_GRAPH_ICON = "icons/down.png"; //$NON-NLS-1$
	public static final String CAPTURE_VALUE_ICON = "icons/captureValue.png"; //$NON-NLS-1$
	
	public static final String CALIBRATE_MONITOR_WIZBAN = "icons/calibrateMonitorBanner.gif"; //$NON-NLS-1$
	
	public static final String LEFT_PANNING = "icons/arrow_left.png"; //$NON-NLS-1$
	public static final String UP_PANNING = "icons/arrow_up.png"; //$NON-NLS-1$
	public static final String DOWN_PANNING = "icons/arrow_down.png"; //$NON-NLS-1$
	public static final String RIGHT_PANNING = "icons/arrow_right.png"; //$NON-NLS-1$
	
	public static final String ZIP = "icons/zipFile.png"; //$NON-NLS-1$
	
	public static final String APPLY_ICON = "icons/apply.png"; //$NON-NLS-1$
	
	public static final String ORGANIZE_SESSION_WIZBAN = "icons/organizeSessionWizardBanner.png"; //$NON-NLS-1$
	
	public static final String SIGNAL_ICON = "icons/signal.png"; //$NON-NLS-1$
	public static final String CATEGORY_ICON = "icons/category.png"; //$NON-NLS-1$
	public static final String EVENT_ICON = "icons/event.png"; //$NON-NLS-1$
	public static final String DELETE_ICON = "icons/delete.png"; //$NON-NLS-1$
	public static final String NEXT_ICON = "icons/next.gif"; //$NON-NLS-1$
	public static final String PREVIOUS_ICON = "icons/previous.gif"; //$NON-NLS-1$
	
	

	public static final String SHOW_PANNEL = "icons/up-arrow.png"; //$NON-NLS-1$
	public static final String HIDE_PANNEL = "icons/down-arrow.png"; //$NON-NLS-1$
	
	
	public static final String DATA_PROCESSING_ICON = "icons/dataProcessing.gif"; //$NON-NLS-1$
	
	public static final String ADD_MARKER_GROUP_ICON = "icons/marker_label.png"; //$NON-NLS-1$
	
	public static final String MODIFIED_ICON = "icons/modified.gif"; //$NON-NLS-1$
	
	public static final String NUMPY_ICON = "icons/numpy.png"; //$NON-NLS-1$
	public static final String NUMPY_ICON_2 = "icons/numpy2.png"; //$NON-NLS-1$
	
	public static final String LOAD_UNLOAD_ICON = "icons/LoadUnloadSubjects.gif"; //$NON-NLS-1$
	
	public static final String BATCH_DATA_PROCESSING_ICON = "icons/batchDataProcessing.gif"; //$NON-NLS-1$
	
	public static final String ACTIVATE_ICON = "icons/activate.png"; //$NON-NLS-1$
	public static final String DEACTIVATE_ICON = "icons/deactivate.gif"; //$NON-NLS-1$
	public static final String ENABLE_DISABLE_ICON = "icons/activateDeactivate.png"; //$NON-NLS-1$
	
	public static final String XYChart_ICON = "icons/XYChartIcon.png"; //$NON-NLS-1$
	public static final String XYZChart_ICON = "icons/XYZChartIcon.png"; //$NON-NLS-1$
	public static final String ZOOM_IN2 = "icons/zoomIn.png"; //$NON-NLS-1$
	public static final String ZOOM_OUT2 = "icons/zoomOut.png"; //$NON-NLS-1$
	public static final String ZOOM_FIT = "icons/zoomFit.png"; //$NON-NLS-1$
	public static final String UP = "icons/up.png"; //$NON-NLS-1$
	public static final String DOWN = "icons/down2.png"; //$NON-NLS-1$
	public static final String RIGHT = "icons/right.png"; //$NON-NLS-1$
	public static final String LEFT = "icons/left2.png"; //$NON-NLS-1$
	public static final String TURN_LEFT = "icons/turnLeft.png"; //$NON-NLS-1$
	public static final String TURN_RIGHT = "icons/turnRight.png"; //$NON-NLS-1$
	
	public static final String CUSTOMER_FUNCTION_ICON = "icons/customerFunction.png"; //$NON-NLS-1$
	public static final String MAIN_PROPERTY_ICON = "icons/mainProperty.png"; //$NON-NLS-1$
	public static final String INNER_PROPERTY_ICON = "icons/innerProperty.png"; //$NON-NLS-1$
}
