package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;

import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ArduinoUnoADS1115ModulePage extends ArduinoUnoModulePage {

	private Combo addressCombo;
	private static String[] availableAddresses;

	public ArduinoUnoADS1115ModulePage(FormEditor editor, String id, String title, Module module) {
		super(editor, id, title, module);
	}
	
	private void computeAvailableAddresses() {
		ArrayList<String> addresses = new ArrayList<String>(Arrays.asList( ArduinoUnoADS1115ModuleProperties.ADDRESS.getAvailableValues()));
		Module[] modules = dacqConfiguration.getModules();
		for (Module aModule : modules) {
			if(aModule instanceof ArduinoUnoADS1115Module && aModule != module) {
				if(aModule.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) != null)
					addresses.remove(aModule.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS));
			}
		}
		availableAddresses = addresses.toArray(new String[addresses.size()]);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ArduinoUnoMessages.ADS1115Page_PageTitle);
		
		computeAvailableAddresses();
		String value = "";
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, false);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ArduinoUnoMessages.AnInModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.address_label, ArduinoUnoMessages.address_tooltip);
		value = module.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
		addressCombo = createCombo(generalconfigurationContainer, availableAddresses, value, 1, 1);
		addressCombo.addModifyListener(new ModifyPropertyHandler(ArduinoUnoADS1115ModuleProperties.ADDRESS, module, addressCombo, ArduinoUnoADS1115ModuleProperties.ADDRESS.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		/*
		 * Channels configuration
		 */
		createTableConfigurationSection(false, true, false, true);
	}

}
