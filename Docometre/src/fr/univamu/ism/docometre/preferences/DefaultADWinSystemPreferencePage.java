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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModulesList;
import fr.univamu.ism.docometre.dialogs.DialogSelectionHandler;

public class DefaultADWinSystemPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, ISelectionChangedListener {
	
	private static String DEFAULT_ADWIN_SYSTEM_PREFERENCE_INITIALIZED = "DEFAULT_ADWIN_SYSTEM_PREFERENCE_INITIALIZED";
	
	private class AddModuleDialog extends TitleAreaDialog {

		public AddModuleDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(ADWinMessages.AddModuleDialog_ShellTitle);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle(ADWinMessages.AddModuleDialog_Title);
			setMessage(ADWinMessages.AddModuleDialog_Message);
			setTitleImage(Activator.getImageDescriptor(IImageKeys.MODULE_WIZBAN).createImage());
			Composite container = (Composite) super.createDialogArea(parent);
			
			modulesListViewer = new ListViewer(container);
			modulesListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			modulesListViewer.setContentProvider(new ArrayContentProvider());
			modulesListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ADWinModulesList.getDescription((ADWinModulesList) element);
				}
			});
			modulesListViewer.setInput(ADWinModulesList.values());
			modulesListViewer.addSelectionChangedListener(DefaultADWinSystemPreferencePage.this);
			
			return container;
		}
		
	}
	
	private static class ModuleInfos {
		
		private String className;
		private String number;
		private String revision;
		
		public ModuleInfos(String type, String number, String revision) {
			this.className = type;
			this.number = number;
			this.revision = revision;
		}
		
		public String getModuleType() {
			return ADWinModulesList.getDescription(className);
		}
		
		@Override
		public String toString() {
			return className + "," + number + "," + revision;
		}
		
		public String getClassName() {
			return className;
		}
		
		public String getNumber() {
			return number;
		}
		
		public String getRevision() {
			return revision;
		}
	}
	
	private class RevisionNumberEditingSupport extends EditingSupport {

		private ComboBoxViewerCellEditor revisionNumberEditor;

		public RevisionNumberEditingSupport(ColumnViewer viewer) {
			super(viewer);
			revisionNumberEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			revisionNumberEditor.setContentProvider(new ArrayContentProvider());
			revisionNumberEditor.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return (String) element;
				}
			});
			revisionNumberEditor.setInput(ADWinModuleProperties.REVISION.getAvailableValues());
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return revisionNumberEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((ModuleInfos)element).revision;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if(value != null) {
				ModuleInfos moduleInfo = (ModuleInfos)element;
				moduleInfo.revision = (String)value;
				getViewer().refresh();
			}
		}
	}
	
	private class ModuleNumberEditingSupport extends EditingSupport {

		private String[] moduleNumbers = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		private ComboBoxViewerCellEditor moduleNumberEditor;

		public ModuleNumberEditingSupport(ColumnViewer viewer) {
			super(viewer);
			moduleNumberEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			moduleNumberEditor.setContentProvider(new ArrayContentProvider());
			moduleNumberEditor.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return (String) element;
				}
			});
			moduleNumberEditor.setInput(moduleNumbers);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return moduleNumberEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((ModuleInfos)element).number;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if(value != null) {
				ModuleInfos moduleInfo = (ModuleInfos)element;
				moduleInfo.number = (String)value;
				getViewer().refresh();
			}
		}
	}
	
	private class Comparator extends ViewerComparator {
		protected int columnNumber;
		protected boolean ascendingDirection; 
		public Comparator() {
			super();
		}
		public void setSortingColumn(int columnNumber) {
			if(this.columnNumber == columnNumber) {
				ascendingDirection = !ascendingDirection;
			} else {
				this.columnNumber = columnNumber;
				ascendingDirection = true;
			}
		}
		public int getDirection() {
			return ascendingDirection == true ? SWT.UP : SWT.DOWN;
		}
		public int computeResult(int result) {
			if(!ascendingDirection) return -result;
			return result;
		}
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			return computeResult(super.compare(viewer, e1, e2));
		}
	}

	private Combo adbasicVersionCombo;
	private Text adbasicCompilerText;
	private Text btlFileText;
	private Text ipAddressText;
	private Text deviceNumberText;
	private Text portNumberText;
	private Text tcpipServerDeviceNumberText;
	private Text timeOutText;
	private Combo systemTypeCombo;
	private Combo cpuTypeCombo;
	private Text globalFrequencyText;
	private Text librariesAbsolutePathText;
	private TableViewer modulesTableViewer;
	private ListViewer modulesListViewer;
	private ArrayList<ADWinModulesList> selectedADWinModulesList = new ArrayList<ADWinModulesList>();
	
	public static ADWinDACQConfiguration getDefaultDACQConfiguration() {
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		ADWinDACQConfiguration adWinDACQConfiguration = new ADWinDACQConfiguration();
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.ADBASIC_COMPILER, preferenceStore.getString(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.BTL_FILE, preferenceStore.getString(ADWinDACQConfigurationProperties.BTL_FILE.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION, preferenceStore.getString(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER, preferenceStore.getString(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH, preferenceStore.getString(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.IP_ADDRESS, preferenceStore.getString(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.PORT_NUMBER, preferenceStore.getString(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.DEVICE_NUMBER, preferenceStore.getString(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.TIME_OUT, preferenceStore.getString(ADWinDACQConfigurationProperties.TIME_OUT.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE, preferenceStore.getString(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.CPU_TYPE, preferenceStore.getString(ADWinDACQConfigurationProperties.CPU_TYPE.getKey()));
		adWinDACQConfiguration.setProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY, preferenceStore.getString(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey()));
		
		ModuleInfos[] modulesInfos = createModulesInfos(preferenceStore);
		for (ModuleInfos moduleInfos : modulesInfos) {
			Module module = ADWinModulesList.createModule(moduleInfos.getClassName(), adWinDACQConfiguration);
			module.setProperty(ADWinModuleProperties.MODULE_NUMBER, moduleInfos.getNumber());
			module.setProperty(ADWinModuleProperties.REVISION, moduleInfos.getRevision());
			adWinDACQConfiguration.addModule(module);
		}
		
		return adWinDACQConfiguration;
	}


	public DefaultADWinSystemPreferencePage() {
	}

	public DefaultADWinSystemPreferencePage(String title) {
		super(title);
	}

	public DefaultADWinSystemPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(DocometreMessages.DefaultADWinSystemPreference_Description);
	}
	
	private void performDefaultsPreferencesValues() {
		ADWinDACQConfiguration adwinDAQConfiguration = new ADWinDACQConfiguration();
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.putValue(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.ADBASIC_COMPILER));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.BTL_FILE.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.BTL_FILE));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.IP_ADDRESS));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.PORT_NUMBER));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.DEVICE_NUMBER));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.TIME_OUT.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.TIME_OUT));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.CPU_TYPE.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE));
		preferenceStore.putValue(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey(), adwinDAQConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY));
		preferenceStore.putValue(DEFAULT_ADWIN_SYSTEM_PREFERENCE_INITIALIZED, "true");
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		
		performDefaultsPreferencesValues();
		
		adbasicCompilerText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey()));
		btlFileText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.BTL_FILE.getKey()));
		librariesAbsolutePathText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		adbasicVersionCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey()));
		tcpipServerDeviceNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey()));
		ipAddressText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey()));
		portNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey()));
		deviceNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey()));
		timeOutText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.TIME_OUT.getKey()));
		systemTypeCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey()));
		cpuTypeCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.CPU_TYPE.getKey()));
		globalFrequencyText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey()));
		
	}

	@Override
	public boolean performOk() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.putValue(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey(), adbasicCompilerText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.BTL_FILE.getKey(), btlFileText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), librariesAbsolutePathText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey(), adbasicVersionCombo.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey(), tcpipServerDeviceNumberText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey(), ipAddressText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey(), portNumberText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey(), deviceNumberText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.TIME_OUT.getKey(), timeOutText.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey(), systemTypeCombo.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.CPU_TYPE.getKey(), cpuTypeCombo.getText());
		preferenceStore.putValue(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey(), globalFrequencyText.getText());
		preferenceStore.putValue("DEFAULT_MODULES", createModuleInfosString());
		return super.performOk();
	}
	
	@Override
	protected Control createContents(Composite parent) {		
		if(!getPreferenceStore().getBoolean(DEFAULT_ADWIN_SYSTEM_PREFERENCE_INITIALIZED)) performDefaultsPreferencesValues();
		Composite container = new Composite(parent, SWT.NORMAL);
		container.setLayout(new GridLayout(3,false));

		// ADBASIC_COMPILER
		Label adbasicCompilerLabel = new Label(container, SWT.NORMAL);
		adbasicCompilerLabel.setText(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(adbasicCompilerLabel);
		adbasicCompilerText = new Text(container, SWT.BORDER);
		adbasicCompilerText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.ADBASIC_COMPILER.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(adbasicCompilerText);
		Button browseADBasicCompilerButton = new Button(container, SWT.FLAT);
		browseADBasicCompilerButton.setText("Browse...");
		browseADBasicCompilerButton.addSelectionListener(new DialogSelectionHandler(adbasicCompilerText, false, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseADBasicCompilerButton);
		
		// BTL_FILE
		Label btlFileLabel = new Label(container, SWT.NORMAL);
		btlFileLabel.setText(ADWinDACQConfigurationProperties.BTL_FILE.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(btlFileLabel);
		btlFileText = new Text(container, SWT.BORDER);
		btlFileText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.BTL_FILE.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(btlFileText);
		Button browseBTLFileButton = new Button(container, SWT.FLAT);
		browseBTLFileButton.setText("Browse...");
		browseBTLFileButton.addSelectionListener(new DialogSelectionHandler(btlFileText, false, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseBTLFileButton);
		
		// LIBRARIES_ABSOLUTE_PATH 
		Label librariesAbsolutePathLabel = new Label(container, SWT.NORMAL);
		librariesAbsolutePathLabel.setText(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(librariesAbsolutePathLabel);
		librariesAbsolutePathText = new Text(container, SWT.BORDER);
		librariesAbsolutePathText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(librariesAbsolutePathText);
		Button browseLibrariesAbsolutePathButton = new Button(container, SWT.FLAT);
		browseLibrariesAbsolutePathButton.setText("Browse...");
		browseLibrariesAbsolutePathButton.addSelectionListener(new DialogSelectionHandler(librariesAbsolutePathText, true, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseLibrariesAbsolutePathButton);
		
		// ADBASIC_VERSION
		Label adbasicVersionLabel = new Label(container, SWT.NORMAL);
		adbasicVersionLabel.setText(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(adbasicVersionLabel);
		adbasicVersionCombo = new Combo(container, SWT.READ_ONLY);
		adbasicVersionCombo.setItems(ADWinDACQConfigurationProperties.ADBasicVersions);
		adbasicVersionCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.ADBASIC_VERSION.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adbasicVersionCombo);
		
		// TCPIP_SERVER_DEVICE_NUMBER
		Label tcpipServerDeviceNumberLabel = new Label(container, SWT.NORMAL);
		tcpipServerDeviceNumberLabel.setText(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(tcpipServerDeviceNumberLabel);
		tcpipServerDeviceNumberText = new Text(container, SWT.BORDER);
		tcpipServerDeviceNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.TCPIP_SERVER_DEVICE_NUMBER.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(tcpipServerDeviceNumberText);
		
		// IP_ADDRESS
		Label ipAddressLabel = new Label(container, SWT.NORMAL);
		ipAddressLabel.setText(ADWinDACQConfigurationProperties.IP_ADDRESS.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(ipAddressLabel);
		ipAddressText = new Text(container, SWT.BORDER);
		ipAddressText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.IP_ADDRESS.getKey()));
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(ipAddressText);
		
		// PORT_NUMBER
		Label portNumberLabel = new Label(container, SWT.NORMAL);
		portNumberLabel.setText(ADWinDACQConfigurationProperties.PORT_NUMBER.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(portNumberLabel);
		portNumberText = new Text(container, SWT.BORDER);
		portNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.PORT_NUMBER.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(portNumberText);
		
		// DEVICE_NUMBER
		Label deviceNumberLabel = new Label(container, SWT.NORMAL);
		deviceNumberLabel.setText(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(deviceNumberLabel);
		deviceNumberText = new Text(container, SWT.BORDER);
		deviceNumberText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.DEVICE_NUMBER.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(deviceNumberText);
		
		// TIME_OUT
		Label timeOutLabel = new Label(container, SWT.NORMAL);
		timeOutLabel.setText(ADWinDACQConfigurationProperties.TIME_OUT.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(timeOutLabel);
		timeOutText = new Text(container, SWT.BORDER);
		timeOutText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.TIME_OUT.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(timeOutText);
		
		// SYSTEM_TYPE
		Label systemTypeLabel = new Label(container, SWT.NORMAL);
		systemTypeLabel.setText(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(systemTypeLabel);
		systemTypeCombo = new Combo(container, SWT.READ_ONLY);
		systemTypeCombo.setItems(ADWinDACQConfigurationProperties.SystemsTypes);
		systemTypeCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.SYSTEM_TYPE.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(systemTypeCombo);
		
		// CPU_TYPE
		Label cpuTypeLabel = new Label(container, SWT.NORMAL);
		cpuTypeLabel.setText(ADWinDACQConfigurationProperties.CPU_TYPE.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(cpuTypeLabel);
		cpuTypeCombo = new Combo(container, SWT.READ_ONLY);
		cpuTypeCombo.setItems(ADWinDACQConfigurationProperties.CPUTypes);
		cpuTypeCombo.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.CPU_TYPE.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(cpuTypeCombo);
		
		// GLOBAL_FREQUENCY
		Label globalFrequencyLabel = new Label(container, SWT.NORMAL);
		globalFrequencyLabel.setText(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(globalFrequencyLabel);
		globalFrequencyText = new Text(container, SWT.BORDER);
		globalFrequencyText.setText(getPreferenceStore().getString(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(globalFrequencyText);
		
		// DEFAULT_MODULES
		Label modulesLabel = new Label(container, SWT.NORMAL);
		modulesLabel.setText("Modules :");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).applyTo(modulesLabel);
		
		modulesTableViewer = new TableViewer(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		modulesTableViewer.getTable().setHeaderVisible(true);
		modulesTableViewer.getTable().setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).span(1, 1).applyTo(modulesTableViewer.getTable());
		// Module type column
		final TableViewerColumn moduleTypeColumn = new TableViewerColumn(modulesTableViewer, SWT.NONE);
		moduleTypeColumn.getColumn().setText(ADWinMessages.ModuleType_ColumnTitle.toString());
		moduleTypeColumn.getColumn().setToolTipText(ADWinMessages.ModuleType_ColumnTooltip);
		moduleTypeColumn.getColumn().setMoveable(false);
		moduleTypeColumn.getColumn().setWidth(200);
		moduleTypeColumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator comparator = (Comparator) modulesTableViewer.getComparator();
				comparator.setSortingColumn(0);
				modulesTableViewer.getTable().setSortDirection(comparator.getDirection());
				modulesTableViewer.getTable().setSortColumn(moduleTypeColumn.getColumn());
				modulesTableViewer.refresh();
			}
		});
		// Module number column
		final TableViewerColumn moduleNumberColumn = new TableViewerColumn(modulesTableViewer, SWT.NONE);
		moduleNumberColumn.getColumn().setText(ADWinModuleProperties.MODULE_NUMBER.getLabel());
		moduleNumberColumn.getColumn().setToolTipText(ADWinModuleProperties.MODULE_NUMBER.getTooltip());
		moduleNumberColumn.getColumn().setMoveable(false);
		moduleNumberColumn.getColumn().setWidth(200);
		moduleNumberColumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator comparator = (Comparator) modulesTableViewer.getComparator();
				comparator.setSortingColumn(1);
				modulesTableViewer.getTable().setSortDirection(comparator.getDirection());
				modulesTableViewer.getTable().setSortColumn(moduleNumberColumn.getColumn());
				modulesTableViewer.refresh();
			}
		});
		
		ModuleNumberEditingSupport editingSupport = new ModuleNumberEditingSupport(modulesTableViewer);
		moduleNumberColumn.setEditingSupport(editingSupport);
		// Module revision column
		final TableViewerColumn revisionNumberColumn = new TableViewerColumn(modulesTableViewer, SWT.NONE);
		revisionNumberColumn.getColumn().setText(ADWinModuleProperties.REVISION.getLabel());
		revisionNumberColumn.getColumn().setToolTipText(ADWinModuleProperties.REVISION.getTooltip());
		revisionNumberColumn.getColumn().setMoveable(false);
		revisionNumberColumn.getColumn().setWidth(200);
		revisionNumberColumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator comparator = (Comparator) modulesTableViewer.getComparator();
				comparator.setSortingColumn(1);
				modulesTableViewer.getTable().setSortDirection(comparator.getDirection());
				modulesTableViewer.getTable().setSortColumn(moduleNumberColumn.getColumn());
				modulesTableViewer.refresh();
			}
		});
		
		RevisionNumberEditingSupport revisionEditingSupport = new RevisionNumberEditingSupport(modulesTableViewer);
		revisionNumberColumn.setEditingSupport(revisionEditingSupport);
		
		
		modulesTableViewer.setContentProvider(new ArrayContentProvider());
		modulesTableViewer.setLabelProvider(new ITableLabelProvider() {
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
				if(columnIndex == 0) return ((ModuleInfos)element).getModuleType(); 
				if(columnIndex == 1) return ((ModuleInfos)element).number; 
				return ((ModuleInfos)element).revision; 
			}
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});
		modulesTableViewer.setComparator(new Comparator());
		modulesTableViewer.setInput(createModulesInfos(getPreferenceStore()));
		
		Composite buttonsContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonsContainer);
		buttonsContainer.setLayout(new GridLayout());
		Button addModuleButton = new Button(buttonsContainer, SWT.FLAT);
		addModuleButton.setText("Add module(s)");
		addModuleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddModuleDialog addModuleDialog = new AddModuleDialog(getShell());
				if(addModuleDialog.open() == Dialog.OK) {
					ModuleInfos[] modulesInfos = (ModuleInfos[])modulesTableViewer.getInput();
					ArrayList<ModuleInfos> modulesInfosArrayList = new ArrayList<ModuleInfos>(Arrays.asList(modulesInfos));
					for (ADWinModulesList adWinModulesList : selectedADWinModulesList) modulesInfosArrayList.add(new ModuleInfos(ADWinModulesList.getClassName(adWinModulesList), "1", "A"));
					modulesTableViewer.setInput(modulesInfosArrayList.toArray(new ModuleInfos[modulesInfosArrayList.size()]));
					modulesTableViewer.refresh();
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(addModuleButton);
		Button removeModuleButton = new Button(buttonsContainer, SWT.FLAT);
		removeModuleButton.setText("Remove module(s)");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(removeModuleButton);
		removeModuleButton.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("rawtypes")
			@Override
			public void widgetSelected(SelectionEvent e) {
				ModuleInfos[] modulesInfos = (ModuleInfos[])modulesTableViewer.getInput();
				ArrayList<ModuleInfos> modulesInfosArrayList = new ArrayList<ModuleInfos>(Arrays.asList(modulesInfos));
				IStructuredSelection selection = modulesTableViewer.getStructuredSelection();
				for (Iterator element = selection.iterator(); element.hasNext();) {
					ModuleInfos moduleInfos = (ModuleInfos) element.next();
					modulesInfosArrayList.remove(moduleInfos);
				}
				modulesTableViewer.setInput(modulesInfosArrayList.toArray(new ModuleInfos[modulesInfosArrayList.size()]));
				modulesTableViewer.refresh();
			}
		});
		
		return container;
		
	}
	
	private static ModuleInfos[] createModulesInfos(IPreferenceStore preferenceStore) {
		String defaultModulesString = preferenceStore.getString("DEFAULT_MODULES");
		if(defaultModulesString.equals("")) return new ModuleInfos[0];
		String[] modulesInfosArray = defaultModulesString.split(";");
		ModuleInfos[] modulesInfos = new ModuleInfos[modulesInfosArray.length];
		for (int i = 0; i < modulesInfosArray.length; i++) {
			String[] infos = modulesInfosArray[i].split(",");
			String type = infos[0];
			String number = infos[1];
			String revision = ADWinModuleProperties.REV_A;
			if(infos.length > 2) revision = modulesInfosArray[i].split(",")[2];
			modulesInfos[i] = new ModuleInfos(type, number, revision);
		}
		return modulesInfos;
	}
	
	private String createModuleInfosString() {
		ModuleInfos[] modulesInfos = (ModuleInfos[])modulesTableViewer.getInput();
		String value = "";
		for (ModuleInfos moduleInfos : modulesInfos) {
			value = value + moduleInfos.toString() + ";";
		}
		return value.replaceAll(";$", "");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		selectedADWinModulesList.clear();
		StructuredSelection selection = (StructuredSelection)modulesListViewer.getSelection();
		selectedADWinModulesList.addAll(selection.toList());
	}
	
}
