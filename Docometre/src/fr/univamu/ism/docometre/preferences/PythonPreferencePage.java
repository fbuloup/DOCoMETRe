package fr.univamu.ism.docometre.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class PythonPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(DocometreMessages.PythonPreferences_Description);

	}

	@Override
	protected void createFieldEditors() {
		// Python preferences
		IntegerFieldEditor pythonTimeOutFieldEditor = new IntegerFieldEditor(GeneralPreferenceConstants.PYTHON_TIME_OUT, DocometreMessages.PythonEngineTimeOut, getFieldEditorParent());
		addField(pythonTimeOutFieldEditor);
		
		Composite container = getFieldEditorParent();
		container.setLayout(new GridLayout(3,false));
		// PYTHON_LOCATION
		FileFieldEditor pythonLocationFieldEditor = new FolderPathFieldEditor(GeneralPreferenceConstants.PYTHON_LOCATION, DocometreMessages.PythonEngineLocation, container);
		pythonLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		addField(pythonLocationFieldEditor);

		// PYTHON_SCRIPTS_LOCATION
		DirectoryFieldEditor pythonScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, DocometreMessages.PythonEngineScriptLocation, container);
		pythonScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		pythonScriptsLocationFieldEditor.setEnabled(false, container);
		addField(pythonScriptsLocationFieldEditor);
		
		// PYTHON_USER_SCRIPTS_LOCATION
		DirectoryFieldEditor pythonUserScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.PYTHON_USER_SCRIPTS_LOCATION, DocometreMessages.PythonEngineUserScriptLocation, container);
		pythonUserScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		pythonUserScriptsLocationFieldEditor.setEnabled(false, container);
		addField(pythonUserScriptsLocationFieldEditor);
	}

}
