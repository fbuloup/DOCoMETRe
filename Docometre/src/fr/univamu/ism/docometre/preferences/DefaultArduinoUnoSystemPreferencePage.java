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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionDialog;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import fr.univamu.ism.docometre.dialogs.DialogSelectionHandler;

public class DefaultArduinoUnoSystemPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static String DEFAULT_ARDUINO_UNO_SYSTEM_PREFERENCE_INITIALIZED = "DEFAULT_ARDUINO_UNO_SYSTEM_PREFERENCE_INITIALIZED";

	private Text builderPathText;
	private Text avrDUDEPathText;
	private Text devicePathText;
	private Combo baudRateCombo;
	private Text libraryPathText;
	private Spinner delayTimeSpinner;
	private Text userLibraryPathText;
	private Text docoUserLibrariesAbsolutePathText;
	
	public static ArduinoUnoDACQConfiguration getDefaultDACQConfiguration() {
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		if(!preferenceStore.getBoolean(DEFAULT_ARDUINO_UNO_SYSTEM_PREFERENCE_INITIALIZED)) performDefaultsPreferencesValues();
		ArduinoUnoDACQConfiguration arduinoUnoDACQConfiguration = new ArduinoUnoDACQConfiguration();
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey()));
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey()));
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey()));
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey()));
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		arduinoUnoDACQConfiguration.setProperty(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH, preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey()));
		return arduinoUnoDACQConfiguration;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(DocometreMessages.DefaultArduinoUnoSystemPreference_Description);
	}
	
	private static void performDefaultsPreferencesValues() {
		ArduinoUnoDACQConfiguration arduinoUnoDACQConfiguration = new ArduinoUnoDACQConfiguration();
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey(), arduinoUnoDACQConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH));
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey(), arduinoUnoDACQConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH));
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey(), arduinoUnoDACQConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH));
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey(), arduinoUnoDACQConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE));
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), arduinoUnoDACQConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH));
		preferenceStore.putValue(DEFAULT_ARDUINO_UNO_SYSTEM_PREFERENCE_INITIALIZED, "true");
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		
		performDefaultsPreferencesValues();
		
		libraryPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		userLibraryPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey()));
		builderPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey()));
		avrDUDEPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey()));
		devicePathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey()));
		baudRateCombo.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey()));
		
	}
	
	@Override
	public boolean performOk() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey(), builderPathText.getText());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey(), avrDUDEPathText.getText());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey(), devicePathText.getText());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey(), baudRateCombo.getText());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), libraryPathText.getText());
		preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey(), userLibraryPathText.getText());
		preferenceStore.putValue(GeneralPreferenceConstants.ARDUINO_DELAY_TIME_AFTER_SERIAL_PRINT, delayTimeSpinner.getText());
		preferenceStore.putValue(GeneralPreferenceConstants.ARDUINO_USER_LIBRARIES_ABSOLUTE_PATH, docoUserLibrariesAbsolutePathText.getText());
		boolean value = super.performOk();
		FunctionsView.refresh(false);
		return value;
	}

	@Override
	protected Control createContents(Composite parent) {
		
		if(!getPreferenceStore().getBoolean(DEFAULT_ARDUINO_UNO_SYSTEM_PREFERENCE_INITIALIZED)) performDefaultsPreferencesValues();
		
		Composite container = new Composite(parent, SWT.NORMAL);
		container.setLayout(new GridLayout(3,false));
		
		//LIBRARIES_ABSOLUTE_PATH
		Label libraryPathLabel = new Label(container, SWT.NORMAL);
		libraryPathLabel.setText(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getLabel());
		libraryPathLabel.setToolTipText(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getTooltip());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(libraryPathLabel);
		libraryPathText = new Text(container, SWT.BORDER);
		libraryPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey()));
		libraryPathText.setToolTipText(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getTooltip());
		libraryPathText.setEditable(false);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(libraryPathText);
		Button browseLibraryPathButton = new Button(container, SWT.FLAT);
		browseLibraryPathButton.setText("Browse...");
		browseLibraryPathButton.addSelectionListener(new DialogSelectionHandler(libraryPathText, true, getShell()));
		browseLibraryPathButton.setEnabled(false);
		GridDataFactory.fillDefaults().applyTo(browseLibraryPathButton);
		
		//USER LIBRARIES_ABSOLUTE_PATH
		Label userLibraryPathLabel = new Label(container, SWT.NORMAL);
		userLibraryPathLabel.setText(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getLabel());
		userLibraryPathLabel.setToolTipText(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getTooltip());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(userLibraryPathLabel);
		userLibraryPathText = new Text(container, SWT.BORDER);
		userLibraryPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey()));
		userLibraryPathText.setToolTipText(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getTooltip());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(userLibraryPathText);
		Button browseUserLibraryPathButton = new Button(container, SWT.FLAT);
		browseUserLibraryPathButton.setText("Browse...");
		browseUserLibraryPathButton.addSelectionListener(new DialogSelectionHandler(userLibraryPathText, true, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseUserLibraryPathButton);
		
		// ARDUINO_USER_LIBRARIES_ABSOLUTE_PATH 
		Label docoUserLibrariesAbsolutePathLabel = new Label(container, SWT.NORMAL);
		docoUserLibrariesAbsolutePathLabel.setText(DocometreMessages.ArduinoUserLibrariesAbsolutePath_Label);
		docoUserLibrariesAbsolutePathLabel.setToolTipText(DocometreMessages.ArduinoUserLibrariesAbsolutePath_Tooltip);
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(docoUserLibrariesAbsolutePathLabel);
		docoUserLibrariesAbsolutePathText = new Text(container, SWT.BORDER);
		docoUserLibrariesAbsolutePathText.setText(getPreferenceStore().getString(GeneralPreferenceConstants.ARDUINO_USER_LIBRARIES_ABSOLUTE_PATH));
		docoUserLibrariesAbsolutePathText.setToolTipText(DocometreMessages.ArduinoUserLibrariesAbsolutePath_Tooltip);
		docoUserLibrariesAbsolutePathText.setEditable(false);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(docoUserLibrariesAbsolutePathText);
		Button browseDocoUserLibrariesAbsolutePathButton = new Button(container, SWT.FLAT);
		browseDocoUserLibrariesAbsolutePathButton.setEnabled(true);
		browseDocoUserLibrariesAbsolutePathButton.setText("Browse...");
		browseDocoUserLibrariesAbsolutePathButton.addSelectionListener(new DialogSelectionHandler(docoUserLibrariesAbsolutePathText, true, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseDocoUserLibrariesAbsolutePathButton);
		
		//BUILDER_PATH
		Label builderPathLabel = new Label(container, SWT.NORMAL);
		builderPathLabel.setText(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(builderPathLabel);
		builderPathText = new Text(container, SWT.BORDER);
		builderPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(builderPathText);
		Button browseBuilderPathButton = new Button(container, SWT.FLAT);
		browseBuilderPathButton.setText("Browse...");
		browseBuilderPathButton.addSelectionListener(new DialogSelectionHandler(builderPathText, false, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseBuilderPathButton);
		
		//AVRDUDE_PATH
		Label avrDUDEPathLabel = new Label(container, SWT.NORMAL);
		avrDUDEPathLabel.setText(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(avrDUDEPathLabel);
		avrDUDEPathText = new Text(container, SWT.BORDER);
		avrDUDEPathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(avrDUDEPathText);
		Button browseavrDUDEPathButton = new Button(container, SWT.FLAT);
		browseavrDUDEPathButton.setText("Browse...");
		browseavrDUDEPathButton.addSelectionListener(new DialogSelectionHandler(avrDUDEPathText, false, getShell()));
		GridDataFactory.fillDefaults().applyTo(browseavrDUDEPathButton);
		
		//DEVICE_PATH
		Label devicePathLabel = new Label(container, SWT.NORMAL);
		devicePathLabel.setText(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(devicePathLabel);
		devicePathText = new Text(container, SWT.BORDER);
		devicePathText.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH.getKey()));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(devicePathText);
		Button devicePathButton = new Button(container, SWT.FLAT);
		devicePathButton.setText("Browse...");
		GridDataFactory.fillDefaults().applyTo(devicePathButton);
		devicePathButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DeviceSelectionDialog deviceSelectionDialog = new DeviceSelectionDialog(getShell(), DeviceType.USB);
				if(deviceSelectionDialog.open() == Dialog.OK) {
					devicePathText.setText(deviceSelectionDialog.getSelection());
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		//BAUD_RATE
		Label baudRateLabel = new Label(container, SWT.NORMAL);
		baudRateLabel.setText(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getLabel());
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(baudRateLabel);
		baudRateCombo = new Combo(container, SWT.READ_ONLY);
		baudRateCombo.setItems(ArduinoUnoDACQConfigurationProperties.BAUD_RATES);
		baudRateCombo.setText(getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.BAUD_RATE.getKey()));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(baudRateCombo);
		
		// Delay time
		Label delayTimeLabel = new Label(container, SWT.NONE);
		delayTimeLabel.setText(DocometreMessages.ArduinoDelayTimeAfterSerialPrint_Label);
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(delayTimeLabel);
		delayTimeSpinner = new Spinner(container, SWT.BORDER);
		delayTimeSpinner.setMinimum(0);
		delayTimeSpinner.setMaximum(10000);
		delayTimeSpinner.setSelection(getPreferenceStore().getInt(GeneralPreferenceConstants.ARDUINO_DELAY_TIME_AFTER_SERIAL_PRINT));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(delayTimeSpinner);
		delayTimeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = delayTimeSpinner.getSelection() + e.count;
				delayTimeSpinner.setSelection(value); 
			}
		});
		
		return container;
	}

}
