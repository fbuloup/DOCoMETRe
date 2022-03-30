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
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ChooseWorkspaceData;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.editors.DataEditor;

public class GeneralPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	private Font font;

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
//		BooleanFieldEditor showTraditionalTabsFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS,
//				DocometreMessages.GeneralPreferences_ShowTraditionalTabs, getFieldEditorParent());
//		addField(showTraditionalTabsFieldEditor);
		
		BooleanFieldEditor xmlSerializationFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.XML_SERIALIZATION,
				DocometreMessages.GeneralPreferences_XMLSerialization, getFieldEditorParent());
		addField(xmlSerializationFieldEditor);
		
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
		
		IntegerFieldEditor undoLimitFieldEditor = new IntegerFieldEditor(GeneralPreferenceConstants.PREF_UNDO_LIMIT,
				DocometreMessages.GeneralPreferences_UndoLimit, getFieldEditorParent());
		undoLimitFieldEditor.setValidRange(0, 1000);
		addField(undoLimitFieldEditor);

		BooleanFieldEditor confirmUndoFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.PREF_CONFIRM_UNDO,
				DocometreMessages.GeneralPreferences_ConfirmUndo, getFieldEditorParent());
		addField(confirmUndoFieldEditor);
		
		Group wineGroup = new Group(getFieldEditorParent(), SWT.NONE);
		wineGroup.setText(DocometreMessages.GeneralPreferences_WineDocker);// "");
		wineGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
		wineGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		// WINE_FULL_PATH
		BooleanFieldEditor useDockerBooleanFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.USE_DOCKER, DocometreMessages.GeneralPreferences_UseDocker, wineGroup);
		addField(useDockerBooleanFieldEditor);
		
		// WINE_FULL_PATH
		FileFieldEditor wineFileFieldEditor = new FolderPathFieldEditor(GeneralPreferenceConstants.WINE_FULL_PATH, DocometreMessages.GeneralPreferences_WineFileLocation, wineGroup);
		addField(wineFileFieldEditor);
		
		Group trialsGroup = new Group(getFieldEditorParent(), SWT.NONE);
		trialsGroup.setText(DocometreMessages.TrialsParameters);
		trialsGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
		trialsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		BooleanFieldEditor stopTrialNowFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.STOP_TRIAL_NOW,
				DocometreMessages.StopTrialImmediatlyWhenAsked, trialsGroup);
		addField(stopTrialNowFieldEditor);
		
		BooleanFieldEditor askForTrialEndingFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.USE_AS_DEFAULT_DO_NOT_ASK_STOP_TRIAL_NOW,
				DocometreMessages.StopTrialDontAsk, trialsGroup);
		addField(askForTrialEndingFieldEditor);
		
		BooleanFieldEditor autoTrialValidation = new BooleanFieldEditor(GeneralPreferenceConstants.AUTO_VALIDATE_TRIALS,
				DocometreMessages.AutoValidateTrial, trialsGroup);
		addField(autoTrialValidation);
		
		BooleanFieldEditor autoTrialStartingFieldEditor = new BooleanFieldEditor(GeneralPreferenceConstants.AUTO_START_TRIALS,
				DocometreMessages.AutoStartTrial, trialsGroup);
		addField(autoTrialStartingFieldEditor);
		
		Group chartOptionsGroup = new Group(getFieldEditorParent(), SWT.NONE);
		chartOptionsGroup.setText(DocometreMessages.Charts2DOptions);
		chartOptionsGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
		chartOptionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		BooleanFieldEditor showCursorFileEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_CURSOR, DocometreMessages.SHOW_CURSOR, chartOptionsGroup);
		addField(showCursorFileEditor);
		
		BooleanFieldEditor showMarkerFileEditor = new BooleanFieldEditor(GeneralPreferenceConstants.SHOW_MARKER, DocometreMessages.SHOW_MARKER, chartOptionsGroup);
		addField(showMarkerFileEditor);
		
		Group redirectOutErrOptionsGroup = new Group(getFieldEditorParent(), SWT.NONE);
		redirectOutErrOptionsGroup.setText(DocometreMessages.REDIRECT_GROUP_TITLE);
		redirectOutErrOptionsGroup.setLayout(new FillLayout(SWT.HORIZONTAL));
		redirectOutErrOptionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		BooleanFieldEditor redirectOption = new BooleanFieldEditor(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE, DocometreMessages.REDIRECT_BUTTON_TITLE, redirectOutErrOptionsGroup);
		addField(redirectOption);
		FileFieldEditor redirectFile = new FileFieldEditor(GeneralPreferenceConstants.STD_ERR_OUT_FILE, DocometreMessages.REDIRECT_FILE_ABSOLUTE_PATH, true, FileFieldEditor.VALIDATE_ON_KEY_STROKE ,redirectOutErrOptionsGroup);
		addField(redirectFile);
		redirectFile.setEnabled(getPreferenceStore().getBoolean(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE), redirectOutErrOptionsGroup); 
		((Button)redirectOption.getDescriptionControl(redirectOutErrOptionsGroup)).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				redirectFile.setEnabled(redirectOption.getBooleanValue(), redirectOutErrOptionsGroup); 
				if(!redirectOption.getBooleanValue()) {
					setErrorMessage(null);
				}
				else {
					String value = redirectFile.getStringValue();
					redirectFile.setStringValue("");
					redirectFile.setStringValue(value);
				}
			}
		});
		Label redirectInfosLabel = new Label(redirectOutErrOptionsGroup, SWT.NORMAL);
		redirectInfosLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		redirectInfosLabel.setText(DocometreMessages.REDIRECT_LABEL_TITLE);
		font = redirectInfosLabel.getFont();
		FontData[] fontData = font.getFontData();
		if(fontData[0] != null) {
			fontData[0].setStyle(SWT.BOLD);
			font = new Font(PlatformUI.getWorkbench().getDisplay(), fontData[0]);
			redirectInfosLabel.setFont(font);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		font.dispose();
	}
	
	@Override
	public boolean performOk() {
		boolean returnValue = super.performOk();
		boolean showCursor = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_CURSOR);
		boolean showMarker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_MARKER);
		IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (IEditorReference editorReference : editorReferences) {
			if(DataEditor.ID.equals(editorReference.getId())) {
				DataEditor dataEditor = (DataEditor)editorReference.getEditor(false);
				dataEditor.setShowCursor(showCursor);
				dataEditor.setShowMarker(showMarker);
			}
		}
		return returnValue;
	}
}
