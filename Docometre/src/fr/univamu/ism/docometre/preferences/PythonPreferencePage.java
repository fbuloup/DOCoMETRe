package fr.univamu.ism.docometre.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
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
		
		FileFieldEditor pythonLocationFieldEditor = new FolderPathFieldEditor(GeneralPreferenceConstants.PYTHON_LOCATION, DocometreMessages.PythonEngineLocation, getFieldEditorParent());
		addField(pythonLocationFieldEditor);
		
		DirectoryFieldEditor pythonScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.PYTHON_SCRIPT_LOCATION, DocometreMessages.PythonEngineScriptLocation, getFieldEditorParent());
		addField(pythonScriptsLocationFieldEditor);
	}

}
