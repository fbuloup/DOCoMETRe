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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ChooseWorkspaceData;
import fr.univamu.ism.docometre.DocometreMessages;

public class GeneralPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public GeneralPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(DocometreMessages.GeneralPreferences_Description);
	}

	@Override
	protected void createFieldEditors() {
		IntegerFieldEditor camerasImageWidthFieldEditor = new IntegerFieldEditor(GeneralPreferenceConstants.PREF_UNDO_LIMIT,
				DocometreMessages.GeneralPreferences_UndoLimit, getFieldEditorParent(), 4);
		addField(camerasImageWidthFieldEditor);

		BooleanFieldEditor confirmUndoFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.PREF_CONFIRM_UNDO,
				DocometreMessages.GeneralPreferences_ConfirmUndo, getFieldEditorParent());
		addField(confirmUndoFieldEditor);
		
		BooleanFieldEditor showWorkspaceDialogFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_WORKSPACE_SELECTION_DIALOG,
				DocometreMessages.GeneralPreferences_ShowWorkspaceDialog, getFieldEditorParent());
		((Button)showWorkspaceDialogFieldEditor.getDescriptionControl(getFieldEditorParent())).addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ChooseWorkspaceData.getInstance().setShowDialog(showWorkspaceDialogFieldEditor.getBooleanValue());
				ChooseWorkspaceData.getInstance().save();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		addField(showWorkspaceDialogFieldEditor);
		
		BooleanFieldEditor showTraditionalTabsFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS,
				DocometreMessages.GeneralPreferences_ShowTraditionalTabs, getFieldEditorParent());
		addField(showTraditionalTabsFieldEditor);
		
		// WINE_FULL_PATH
		FileFieldEditor wineFileFieldEditor = new FolderPathFieldEditor(GeneralPreferenceConstants.WINE_FULL_PATH, DocometreMessages.GeneralPreferences_WineFileLocation, getFieldEditorParent());
		addField(wineFileFieldEditor);
		
		BooleanFieldEditor stopTrialNowFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.STOP_TRIAL_NOW,
				DocometreMessages.StopTrialImmediatlyWhenAsked, getFieldEditorParent());
		addField(stopTrialNowFieldEditor);
		
		BooleanFieldEditor askForTrialEndingFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW,
				DocometreMessages.StopTrialDontAsk, getFieldEditorParent());
		addField(askForTrialEndingFieldEditor);
		
		BooleanFieldEditor autoTrialValidation = new BooleanFieldEditor(GeneralPreferenceConstants.AUTO_VALIDATE_TRIALS,
				DocometreMessages.AutoValidateTrial, getFieldEditorParent());
		addField(autoTrialValidation);
		
		BooleanFieldEditor autoTrialStartingFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.AUTO_START_TRIALS,
				DocometreMessages.AutoStartTrial, getFieldEditorParent());
		addField(autoTrialStartingFieldEditor);
		
		ComboFieldEditor mathEngineFieldEditor = new ComboFieldEditor(GeneralPreferenceConstants.MATH_ENGINE, DocometreMessages.MathEngineLabel, GeneralPreferenceConstants.MATH_ENGINE_VALUES, getFieldEditorParent());
		addField(mathEngineFieldEditor);
		
	}

}
