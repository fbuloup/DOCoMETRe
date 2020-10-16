package fr.univamu.ism.docometre.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class MathEnginePrefrencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public MathEnginePrefrencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		ComboFieldEditor mathEngineFieldEditor = new ComboFieldEditor(MathEnginePreferencesConstants.MATH_ENGINE, DocometreMessages.MathEngineLabel, MathEnginePreferencesConstants.MATH_ENGINE_VALUES, getFieldEditorParent());
		addField(mathEngineFieldEditor);
		
		BooleanFieldEditor alwaysLoadFromSavedDataFieldEditor = new BooleanFieldEditor(MathEnginePreferencesConstants.ALWAYS_LOAD_FROM_SAVED_DATA,
				DocometreMessages.AlwaysLoadFromSavedData, getFieldEditorParent());
		alwaysLoadFromSavedDataFieldEditor.getDescriptionControl(getFieldEditorParent()).setToolTipText(DocometreMessages.AlwaysLoadFromSavedData_Tooltip);
		addField(alwaysLoadFromSavedDataFieldEditor);
	}

}
