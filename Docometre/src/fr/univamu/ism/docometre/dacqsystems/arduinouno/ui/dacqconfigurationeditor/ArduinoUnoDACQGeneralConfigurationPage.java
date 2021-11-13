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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor;

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scrollable;
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
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.AddModuleHandler;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.DeleteModulesHandler;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.FrequencyInputValidator;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;
import fr.univamu.ism.docometre.dialogs.DialogSelectionHandler;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoModulesList;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ArduinoUnoDACQGeneralConfigurationPage extends ModulePage {
	
	public static String PAGE_ID = "DACQGeneralConfigurationFormPage";

	private PartListenerAdapter partListenerAdapter;
	private Text devicePathText;
	private Combo deviceBaudRateCombo;
	private Hyperlink globalFrequencyHyperlink;
	private Text builderPathText;
	private Text avrDudePathText;
	private Text librariesPathText;
	
	private class SetAsDefaultConfigurationAction extends Action {
		public SetAsDefaultConfigurationAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.APPLY_DEFAULT_SETTINGS_ICON));
			setToolTipText(DocometreMessages.SetAsDefaultInPreferences_Tooltip);
		}
		
		@Override
		public void run() {
			ArrayList<String> values = new ArrayList<>(0);
			values.add(devicePathText.getText());
			values.add(deviceBaudRateCombo.getText());
			values.add(builderPathText.getText());
			values.add(avrDudePathText.getText());
			values.add(librariesPathText.getText());
			values.add(globalFrequencyHyperlink.getText());
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey(), values.get(0));
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey(), values.get(1));
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey(), values.get(2));
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey(), values.get(3));
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), values.get(4));
			preferences.putValue(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey(), values.get(5));
		}
	}

	public ArduinoUnoDACQGeneralConfigurationPage(FormEditor editor) {
		super(editor, PAGE_ID, ArduinoUnoMessages.DACQGeneralConfigurationPage_Title, null);
	}
	
	@Override
	public String getPageTitle() {
		return ArduinoUnoMessages.DACQGeneralConfigurationPage_Title;
	}
	
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
						module.removeObserver(ArduinoUnoDACQGeneralConfigurationPage.this);
					}
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		
		managedForm.getForm().getForm().setText(ArduinoUnoMessages.DACQGeneralConfigurationPage_PageTitle);
		managedForm.getForm().getToolBarManager().add(new SetAsDefaultConfigurationAction());
		managedForm.getForm().getToolBarManager().update(true);
		
		String value = "";
		String regExp = "";
		
		createGeneralConfigurationSection(3, false);
		generalConfigurationSection.setText(ArduinoUnoMessages.GeneralConfigurationSection_Title);
		generalConfigurationSection.setDescription(ArduinoUnoMessages.GeneralConfigurationSectionDescription);
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.LibrariesAbsolutePath_Label, ArduinoUnoMessages.LibrariesAbsolutePath_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
		regExp = ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getRegExp();
		librariesPathText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		librariesPathText.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH, dacqConfiguration, librariesPathText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		librariesPathText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(librariesPathText, true, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.BuilderPath_Label, ArduinoUnoMessages.BuilderPath_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH);
		regExp = ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getRegExp();
		builderPathText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		builderPathText.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH, dacqConfiguration, builderPathText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		builderPathText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(builderPathText, false, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.AVRDudePath_Label, ArduinoUnoMessages.AVRDudePath_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH);
		regExp = ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getRegExp();
		avrDudePathText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		avrDudePathText.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH, dacqConfiguration, avrDudePathText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		avrDudePathText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DialogSelectionHandler(avrDudePathText, false, getSite().getShell()));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.DevicePath_Label, ArduinoUnoMessages.DevicePath_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH);
		regExp = ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getRegExp();
		devicePathText = createText(generalconfigurationContainer, value, SWT.NONE, 1, 1);
		if(Platform.getOS().equals(Platform.OS_WIN32)) devicePathText.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH, dacqConfiguration, devicePathText, regExp, DocometreMessages.ArduinoUnoProcess_DevicePathErrorMessage, false, (ResourceEditor)getEditor()));
		else devicePathText.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH, dacqConfiguration, devicePathText, regExp, DocometreMessages.ErrorFileFolderNotValid, true, (ResourceEditor)getEditor()));
		devicePathText.addModifyListener(getGeneralConfigurationModifyListener());
		createButton(generalconfigurationContainer, DocometreMessages.Browse, SWT.PUSH, 1, 1).addSelectionListener(new DeviceSelectionHandler(devicePathText, getSite().getShell(), DeviceType.USB));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.DeviceBaudRate_Label, ArduinoUnoMessages.DeviceBaudRate_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE);
		regExp = ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getRegExp();
		deviceBaudRateCombo = createCombo(generalconfigurationContainer, ArduinoUnoDACQConfigurationProperties.BAUD_RATES, value, 2 , 1);
		deviceBaudRateCombo.addModifyListener(new ModifyPropertyHandler(ArduinoUnoDACQConfigurationProperties.BAUD_RATE, dacqConfiguration, deviceBaudRateCombo, regExp, "", false, (ResourceEditor)getEditor()));
		deviceBaudRateCombo.addModifyListener(getGeneralConfigurationModifyListener());
		
		createLabel(generalconfigurationContainer, DocometreMessages.GlobalFrequency_Label, DocometreMessages.GlobalFrequency_Tooltip);
		value = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
		regExp = ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.getRegExp();
		globalFrequencyHyperlink = createHyperlink(generalconfigurationContainer, dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY), 2, 1);
		globalFrequencyHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent event) {
				String initialValue = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
				InputDialog changeFrequencyInputDialog = new InputDialog(getSite().getShell(), DocometreMessages.ChangeFrequencyDialogTitle, DocometreMessages.ChangeFrequencyDialogMessage, initialValue, new FrequencyInputValidator(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.getRegExp()));
				if(changeFrequencyInputDialog.open() == Window.OK) {
					globalFrequencyHyperlink.setText(changeFrequencyInputDialog.getValue());
					globalFrequencyHyperlink.getParent().layout();
					try {
						String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.getLabel());
						IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
						IUndoContext undoContext = ((ResourceEditor)getEditor()).getUndoContext();
						operationHistory.execute(new ModifyPropertyOperation(globalFrequencyHyperlink, label, ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY, dacqConfiguration, changeFrequencyInputDialog.getValue(), undoContext), null, null);
						ArduinoUnoDACQGeneralConfigurationPage.this.generalConfigurationSectionPart.markDirty();
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
		tableConfigurationSection.setText(ArduinoUnoMessages.ModuleConfigurationSection_Title);
		tableConfigurationSection.setDescription(ArduinoUnoMessages.ModulesConfigurationSectionDescription);
		
		deleteToolItem.setToolTipText(ADWinMessages.DeleteModule_Tooltip);
		DeleteModulesHandler deleteModulesHandler = new DeleteModulesHandler(getSite().getShell(), dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()); 
		tableViewer.addSelectionChangedListener(deleteModulesHandler);
		deleteToolItem.addSelectionListener(deleteModulesHandler);
		
		addToolItem.setToolTipText(ADWinMessages.AddModule_Tooltip);
		addToolItem.addSelectionListener(new AddModuleHandler(getSite().getShell(), (ResourceEditor)getEditor()));
		
		TableColumnLayout  modulesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		Property moduleTypeColumnTitleProperty = new Property("", (String) ArduinoUnoMessages.ModuleType_ColumnTitle, "" ,"") {
		};
		createColumn(ArduinoUnoMessages.ModuleType_ColumnTooltip, modulesTableColumnLayout, moduleTypeColumnTitleProperty, 200, 0);
//		TableViewerColumn moduleNumberViewerColumn = createColumn(ADWinModuleProperties.MODULE_NUMBER.getTooltip(), modulesTableColumnLayout, ADWinModuleProperties.MODULE_NUMBER, 200, 1);
//		EditingSupport editingSupport = new ModuleNumberEditingSupport(tableViewer, dacqConfiguration, (ResourceEditor) getEditor());
//		moduleNumberViewerColumn.setEditingSupport(editingSupport);
		
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
				if(columnIndex == 0) return ArduinoUnoModulesList.getDescription(module); 
				return "";
			}
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});
		configureSorter(new Comparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(dacqConfiguration.getModules());
		
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ArduinoUnoDACQConfigurationProperties propertyKey) {
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
	
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property instanceof ArduinoUnoDACQConfigurationProperties) {
			if(property == ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH)
				updateWidget(librariesPathText, (ArduinoUnoDACQConfigurationProperties)property);
			if(property == ArduinoUnoDACQConfigurationProperties.BUILDER_PATH)
				updateWidget(builderPathText, (ArduinoUnoDACQConfigurationProperties)property);
			if(property == ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH)
				updateWidget(avrDudePathText, (ArduinoUnoDACQConfigurationProperties)property);
			if(property == ArduinoUnoDACQConfigurationProperties.DEVICE_PATH)
				updateWidget(devicePathText, (ArduinoUnoDACQConfigurationProperties)property);
			if(property == ArduinoUnoDACQConfigurationProperties.BAUD_RATE)
				updateWidget(deviceBaudRateCombo, (ArduinoUnoDACQConfigurationProperties)property);
			if(property == ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY)
				updateWidget(globalFrequencyHyperlink, (ArduinoUnoDACQConfigurationProperties)property);
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
		if(property instanceof ArduinoUnoADS1115ModuleProperties) {
			if(property == ArduinoUnoADS1115ModuleProperties.ADDRESS) {
				tableViewer.refresh();
			}
		}
	}
	
}
