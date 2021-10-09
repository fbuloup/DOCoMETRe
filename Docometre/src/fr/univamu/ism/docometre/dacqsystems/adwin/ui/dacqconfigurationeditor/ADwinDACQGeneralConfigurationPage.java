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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor;

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.FrequencyInputValidator;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModulesList;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;
import fr.univamu.ism.docometre.dacqsystems.adwin.calibration.CalibrateMonitorDialog;
import fr.univamu.ism.docometre.dialogs.DialogSelectionHandler;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ADwinDACQGeneralConfigurationPage extends ModulePage {
	
	public static String PAGE_ID = "DACQGeneralConfigurationFormPage";
	
	private class SetAsDefaultConfigurationAction extends Action {
		public SetAsDefaultConfigurationAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.APPLY_DEFAULT_SETTINGS_ICON));
			setToolTipText(DocometreMessages.SetAsDefaultInPreferences_Tooltip);
		}
		
		@Override
		public void run() {
			ArrayList<String> values = new ArrayList<>(0);
			values.add(adBasicCompilerText.getText());
			values.add(btlFileText.getText());
			values.add(librariesText.getText());
			values.add(adBasicVersionCombo.getText());
			values.add(tcpipServerText.getText());
			values.add(ipText.getText());
			values.add(portNumberText.getText());
			values.add(deviceNumberText.getText());
			values.add(timeOutText.getText());
			values.add(systemCombo.getText());
			values.add(cpuCombo.getText());
			values.add(globalFrequencyHyperlink.getText());
			int nbModules = tableViewer.getTable().getItemCount();
			String modulesString = "";
			for (int i = 0; i < nbModules; i++) {
				Module module = (Module)tableViewer.getElementAt(i);
				modulesString = modulesString + tableViewer.getElementAt(i).getClass().getCanonicalName() + "," + module.getProperty(ADWinModuleProperties.MODULE_NUMBER) + "," + module.getProperty(ADWinModuleProperties.REVISION) + ";";
			}
			modulesString = modulesString.replaceAll(";$", "");
			values.add(modulesString);
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			preferences.putValue(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey(), values.get(0));
			preferences.putValue(ADWinDACQConfigurationProperties.BTL_FILE.getKey(), values.get(1));
			preferences.putValue(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), values.get(2));
			preferences.putValue(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey(), values.get(3));
			preferences.putValue(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey(), values.get(4));
			preferences.putValue(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey(), values.get(5));
			preferences.putValue(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey(), values.get(6));
			preferences.putValue(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey(), values.get(7));
			preferences.putValue(ADWinDACQConfigurationProperties.TIME_OUT.getKey(), values.get(8));
			preferences.putValue(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey(), values.get(9));
			preferences.putValue(ADWinDACQConfigurationProperties.CPU_TYPE.getKey(), values.get(10));
			preferences.putValue(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey(), values.get(11));
			preferences.putValue("DEFAULT_MODULES", values.get(12));
		}
	}
	
	private class CalibrateMonitorAction extends Action {
		public CalibrateMonitorAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.CALIBRATION_MONITORING_ICON));
			setToolTipText("Calibration, monitoring");
		}
		
		@Override
		public void run() {
			Shell shell = ADwinDACQGeneralConfigurationPage.this.getSite().getShell();
			CalibrateMonitorDialog calibrateMonitorDialog = new CalibrateMonitorDialog(shell, (ADWinDACQConfiguration) dacqConfiguration);
			calibrateMonitorDialog.open();
		}
	}
	
	public class ModulesComparator extends Comparator {
		public ModulesComparator() {
			super();
		}
		public void setSortingColumn(int columnNumber) {
			if(this.sortingColumnNumber == columnNumber) {
				ascendingDirection = !ascendingDirection;
			} else {
				this.sortingColumnNumber = columnNumber;
				ascendingDirection = true;
			}
		}
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			switch (sortingColumnNumber) {
				case 0: case 2:
					result = super.compare(viewer, e1, e2);
					break;
				case 1:
					Module module1 = (Module)e1;
					Module module2 = (Module)e2;
					e1 = module1.getProperty(ADWinModuleProperties.MODULE_NUMBER);
					e2 = module2.getProperty(ADWinModuleProperties.MODULE_NUMBER);
					result = super.compare(viewer, (String)e1, (String)e2);
					break;
			}
			return super.computeResult(result);
		}
	}

	private Text adBasicCompilerText;
	private Text btlFileText;
	private Text librariesText;
	private Combo adBasicVersionCombo;
	private Text tcpipServerText;
	private Text ipText;
	private Text portNumberText;
	private Text deviceNumberText;
	private Text timeOutText;
	private Combo systemCombo;
	private Combo cpuCombo;
	private Hyperlink globalFrequencyHyperlink;
	private PartListenerAdapter partListenerAdapter;

	public ADwinDACQGeneralConfigurationPage(FormEditor editor) {
		super(editor, PAGE_ID, ADWinMessages.DACQGeneralConfigurationPage_PageTitle, null);
	}
	
//	@Override
//	public void init(IEditorSite site, IEditorInput input) {
//		super.init(site, input);
//	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
		super.dispose();
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		
		Module[] modules = dacqConfiguration.getModules();
		for (Module module : modules) {
			module.addObserver(this);
		}
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == getEditor()) {
					Module[] modules = dacqConfiguration.getModules();
					for (Module module : modules) {
						module.removeObserver(ADwinDACQGeneralConfigurationPage.this);
					}
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		
		managedForm.getForm().getForm().setText(ADWinMessages.DACQGeneralConfigurationPage_Title);
		managedForm.getForm().getToolBarManager().add(new SetAsDefaultConfigurationAction());
		managedForm.getForm().getToolBarManager().add(new CalibrateMonitorAction());
		managedForm.getForm().getToolBarManager().update(true);
		
		/*
		 * Section 1
		 */
		createGeneralConfigurationSection(3, false);
		generalConfigurationSection.setText(ADWinMessages.FoldersAndVersionConfigurationSection_Title);
		generalConfigurationSection.setDescription(ADWinMessages.FoldersAndVersionConfigurationSectionDescription);
		
		String value = "";
		String regExp = "";
		
		/*
		 * Part 1 : folders and version configuration
		 */
		createLabel(generalconfigurationContainer, ADWinMessages.ADBasicCompilerPath_Label, ADWinMessages.ADBasicCompilerPath_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.ADBASIC_COMPILER);
		regExp = ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getRegExp();
		adBasicCompilerText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		adBasicCompilerText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.ADBASIC_COMPILER, dacqConfiguration, adBasicCompilerText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		adBasicCompilerText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(adBasicCompilerText, false, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.BootloaderPath_Label, ADWinMessages.BootloaderPath_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.BTL_FILE);
		regExp = ADWinDACQConfigurationProperties.BTL_FILE.getRegExp();
		btlFileText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		btlFileText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.BTL_FILE, dacqConfiguration, btlFileText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		btlFileText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(btlFileText, false, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.LibrariesAbsolutePath_Label, ADWinMessages.LibrariesAbsolutePath_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
		regExp = ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getRegExp();
		librariesText = createText(generalconfigurationContainer, value, SWT.READ_ONLY, 1, 1);
		librariesText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH, dacqConfiguration, librariesText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		librariesText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(librariesText, true, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADBasicVersion_Label, ADWinMessages.ADBasicVersion_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION);
		regExp = ADWinDACQConfigurationProperties.ADBASIC_VERSION.getRegExp();
		adBasicVersionCombo = createCombo(generalconfigurationContainer, ADWinDACQConfigurationProperties.ADBasicVersions, value, 2 , 1);
		adBasicVersionCombo.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.ADBASIC_VERSION, dacqConfiguration, adBasicVersionCombo, regExp, "", false, (ResourceEditor)getEditor()));
		adBasicVersionCombo.addModifyListener(getGeneralConfigurationModifyListener());
		
		/*
		 * Part 2 : device configuration
		 */
		createLabel(generalconfigurationContainer, ADWinMessages.TCPIPServerDeviceNumber_Label, ADWinMessages.TCPIPServerDeviceNumber_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER);
		regExp = ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getRegExp();
		tcpipServerText = createText(generalconfigurationContainer, value, SWT.NONE, 2, 1);
		tcpipServerText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER, dacqConfiguration, tcpipServerText, regExp, ADWinMessages.ErrorTCPIPServerNotValid, false, (ResourceEditor)getEditor()));
		tcpipServerText.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.IPAddress_Label, ADWinMessages.IPAddress_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.IP_ADDRESS);
		regExp = ADWinDACQConfigurationProperties.IP_ADDRESS.getRegExp();
		ipText = createText(generalconfigurationContainer, value, SWT.NONE, 2, 1);
		ipText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.IP_ADDRESS, dacqConfiguration, ipText, regExp, ADWinMessages.ErrorTCPIPDeviceNotValid, false, (ResourceEditor)getEditor()));
		ipText.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.TCPIPDevicePortNumber_Label, ADWinMessages.TCPIPDevicePortNumber_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.PORT_NUMBER);
		regExp = ADWinDACQConfigurationProperties.PORT_NUMBER.getRegExp();
		portNumberText = createText(generalconfigurationContainer, value, SWT.NONE, 2, 1);
		portNumberText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.PORT_NUMBER, dacqConfiguration, portNumberText, regExp, ADWinMessages.ErrorPortNumberDeviceNotValid, false, (ResourceEditor)getEditor()));
		portNumberText.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.DeviceNumber_Label, ADWinMessages.DeviceNumber_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.DEVICE_NUMBER);
		regExp = ADWinDACQConfigurationProperties.DEVICE_NUMBER.getRegExp();
		deviceNumberText = createText(generalconfigurationContainer, value, SWT.NONE, 2, 1);
		deviceNumberText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.DEVICE_NUMBER, dacqConfiguration, deviceNumberText, regExp, ADWinMessages.ErrorDeviceNumberNotValid, false, (ResourceEditor)getEditor()));
		deviceNumberText.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.TimeOut_Label, ADWinMessages.TimeOut_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.TIME_OUT);
		regExp = ADWinDACQConfigurationProperties.TIME_OUT.getRegExp();;
		timeOutText = createText(generalconfigurationContainer, value, SWT.NONE, 2, 1);
		timeOutText.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.TIME_OUT, dacqConfiguration, timeOutText, regExp, ADWinMessages.TimeOutDeviceNotValid, false, (ResourceEditor)getEditor()));
		timeOutText.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.SytemType_Label, ADWinMessages.SystemType_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
		regExp = ADWinDACQConfigurationProperties.SYSTEM_TYPE.getRegExp();;
		systemCombo = createCombo(generalconfigurationContainer, ADWinDACQConfigurationProperties.SystemsTypes, value, 2 ,1);
		systemCombo.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.SYSTEM_TYPE, dacqConfiguration, systemCombo, regExp, "", false, (ResourceEditor)getEditor()));
		systemCombo.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.CPUType_Label, ADWinMessages.CPUType_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
		regExp = ADWinDACQConfigurationProperties.CPU_TYPE.getRegExp();
		cpuCombo = createCombo(generalconfigurationContainer, ADWinDACQConfigurationProperties.CPUTypes, value, 2 ,1);
		cpuCombo.addModifyListener(new ModifyPropertyHandler(ADWinDACQConfigurationProperties.CPU_TYPE, dacqConfiguration, cpuCombo, regExp, "", false, (ResourceEditor)getEditor()));
		cpuCombo.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, DocometreMessages.GlobalFrequency_Label, DocometreMessages.GlobalFrequency_Tooltip);
		value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
		regExp = ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getRegExp();
		globalFrequencyHyperlink = createHyperlink(generalconfigurationContainer, dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY), 2, 1);
		globalFrequencyHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent event) {
				String initialValue = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
				InputDialog changeFrequencyInputDialog = new InputDialog(getSite().getShell(), DocometreMessages.ChangeFrequencyDialogTitle, DocometreMessages.ChangeFrequencyDialogMessage, initialValue, new FrequencyInputValidator(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getRegExp()));
				if(changeFrequencyInputDialog.open() == Window.OK) {
					globalFrequencyHyperlink.setText(changeFrequencyInputDialog.getValue());
					globalFrequencyHyperlink.getParent().layout();
					try {
						String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getLabel());
						IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
						IUndoContext undoContext = ((ResourceEditor)getEditor()).getUndoContext();
						operationHistory.execute(new ModifyPropertyOperation(globalFrequencyHyperlink, label, ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY, dacqConfiguration, changeFrequencyInputDialog.getValue(), undoContext), null, null);
						ADwinDACQGeneralConfigurationPage.this.generalConfigurationSectionPart.markDirty();
					} catch (ExecutionException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					} 
					
				}
			}
		});
		
		
		/*
		 * Section 2 : modules configuration
		 */
		createTableConfigurationSection(true, false, false, false);
		tableConfigurationSection.setText(ADWinMessages.ModuleConfigurationSection_Title);
		tableConfigurationSection.setDescription(ADWinMessages.ModulesConfigurationSectionDescription);

		deleteToolItem.setToolTipText(ADWinMessages.DeleteModule_Tooltip);
		DeleteModulesHandler deleteModulesHandler = new DeleteModulesHandler(getSite().getShell(), dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()); 
		tableViewer.addSelectionChangedListener(deleteModulesHandler);
		deleteToolItem.addSelectionListener(deleteModulesHandler);
		
		addToolItem.setToolTipText(ADWinMessages.AddModule_Tooltip);
		addToolItem.addSelectionListener(new AddModuleHandler(getSite().getShell(), (ResourceEditor)getEditor()));
		
		TableColumnLayout  modulesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		
		Property moduleTypeColumnTitleProperty = new Property("", (String) ADWinMessages.ModuleType_ColumnTitle, "" ,"") {
		};
		createColumn(ADWinMessages.ModuleType_ColumnTooltip, modulesTableColumnLayout, moduleTypeColumnTitleProperty, 200, 0);
		TableViewerColumn moduleNumberViewerColumn = createColumn(ADWinModuleProperties.MODULE_NUMBER.getTooltip(), modulesTableColumnLayout, ADWinModuleProperties.MODULE_NUMBER, 200, 1);
		EditingSupport editingSupport = new ModuleNumberEditingSupport(tableViewer, dacqConfiguration, (ResourceEditor) getEditor());
		moduleNumberViewerColumn.setEditingSupport(editingSupport);
		
		TableViewerColumn revisionViewerColumn = createColumn(ADWinModuleProperties.REVISION.getTooltip(), modulesTableColumnLayout, ADWinModuleProperties.REVISION, 200, 1);
		EditingSupport revisionEditingSupport = new ModuleRevisionEditingSupport(tableViewer, dacqConfiguration, (ResourceEditor) getEditor());
		revisionViewerColumn.setEditingSupport(revisionEditingSupport);
		
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection)event.getSelection()).getFirstElement();
				int id = System.identityHashCode(element);
				getEditor().setActivePage(Integer.toString(id));
			}
		});
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new ITableLabelProvider() {
			public void removeListener(ILabelProviderListener listener) {
			}
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			public void dispose() {
			}
			public void addListener(ILabelProviderListener listener) {
			}
			public String getColumnText(Object element, int columnIndex) {
				Module module = (Module)element;
				if(columnIndex == 0) return ADWinModulesList.getDescription(module); 
				if(columnIndex == 1) return module.getProperty(ADWinModuleProperties.MODULE_NUMBER);	
				if(columnIndex == 2) return module.getProperty(ADWinModuleProperties.REVISION);		
				return "";
			}
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});
		configureSorter(new ModulesComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(dacqConfiguration.getModules());
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ADWinDACQConfigurationProperties propertyKey) {
		String value = dacqConfiguration.getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		if(widget instanceof Text) {
			Text text = (Text) widget;
			if(!text.getText().equals(value)) text.setText(value);
		}
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		for (Listener listener : listeners) widget.addListener(SWT.Modify , listener);
		generalConfigurationSectionPart.markDirty();
	}

	/*
	 * This method is called to reflect any changes between model and UI.
	 * It is called when undo/redo operation are run. It is not possible
	 * to use properties listeners as it will result in cyclic calls and
	 * stack overflow.
	 */
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property instanceof ADWinDACQConfigurationProperties) {
			if(property == ADWinDACQConfigurationProperties.ADBASIC_COMPILER)
				updateWidget(adBasicCompilerText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.BTL_FILE)
				updateWidget(btlFileText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH)
				updateWidget(librariesText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.ADBASIC_VERSION)
				updateWidget(adBasicVersionCombo, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.IP_ADDRESS)
				updateWidget(ipText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.DEVICE_NUMBER)
				updateWidget(deviceNumberText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER)
				updateWidget(tcpipServerText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.PORT_NUMBER)
				updateWidget(portNumberText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.TIME_OUT)
				updateWidget(timeOutText, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.SYSTEM_TYPE)
				updateWidget(systemCombo, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.CPU_TYPE)
				updateWidget(cpuCombo, (ADWinDACQConfigurationProperties)property);
			if(property == ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)
				updateWidget(globalFrequencyHyperlink, (ADWinDACQConfigurationProperties)property);
		}
		if(property instanceof DACQConfigurationProperties) {
			if(property == DACQConfigurationProperties.UPDATE_MODULE) {
				tableViewer.setInput(dacqConfiguration.getModules());
				tableConfigurationSectionPart.markDirty();
				if(newValue != null) {
					((Module)newValue).addObserver(this);
				} else if(oldValue != null) {
					((Module)oldValue).removeObserver(this);
				}
			}
			if(property == DACQConfigurationProperties.CHARTS_LAYOUT_COLUMNS_NUMBER);
		}
		if(property instanceof ADWinModuleProperties) {
			if(property == ADWinModuleProperties.MODULE_NUMBER || property == ADWinModuleProperties.REVISION) {
				tableViewer.refresh();
				tableConfigurationSectionPart.markDirty();
			}
		}
	}

	@Override
	public String getPageTitle() {
		return ADWinMessages.DACQGeneralConfigurationPage_PageTitle;
	}
	
}
