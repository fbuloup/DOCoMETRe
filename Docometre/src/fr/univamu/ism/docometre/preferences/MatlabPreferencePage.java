package fr.univamu.ism.docometre.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
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

public class MatlabPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(DocometreMessages.MatlabPreferences_Description);
	}

	@Override
	protected void createFieldEditors() {
		// Matlab preferences
		
		BooleanFieldEditor showMatlabWindowFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_MATLAB_WINDOW, DocometreMessages.MatlabEngineShowMatlabWindow, getFieldEditorParent());
		addField(showMatlabWindowFieldEditor);
		
		IntegerFieldEditor matlabTimeOutFieldEditor = new IntegerFieldEditor(GeneralPreferenceConstants.MATLAB_TIME_OUT, DocometreMessages.MatlabEngineTimeOut, getFieldEditorParent());
		addField(matlabTimeOutFieldEditor);
		
		Composite container = getFieldEditorParent();
		container.setLayout(new GridLayout(3,false));
		// MATLAB_LOCATION
		FileFieldEditor matlabLocationFieldEditor = new FolderPathFieldEditor(GeneralPreferenceConstants.MATLAB_LOCATION, DocometreMessages.MatlabEngineLocation, container);
		matlabLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		addField(matlabLocationFieldEditor);
		
		// MATLAB_SCRIPTS_LOCATION
		DirectoryFieldEditor matlabScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, DocometreMessages.MatlabEngineScriptLocation, container);
		matlabScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		matlabScriptsLocationFieldEditor.setEnabled(false, container);
		addField(matlabScriptsLocationFieldEditor);
		
		// MATLAB_USER_SCRIPTS_LOCATION
		DirectoryFieldEditor matlabUserScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.MATLAB_USER_SCRIPTS_LOCATION, DocometreMessages.MatlabEngineUserScriptLocation, container);
		matlabUserScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		matlabUserScriptsLocationFieldEditor.setEnabled(false, container);
		addField(matlabUserScriptsLocationFieldEditor);
	}

}
