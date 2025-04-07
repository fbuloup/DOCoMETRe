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
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;

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
		
		// MATLAB_LICENCE_FILE
		StringFieldEditor matlabLicenceFieldEditor = new StringFieldEditor(GeneralPreferenceConstants.MATLAB_LICENCE_FILE, DocometreMessages.MatlabLicenceLocation, container);
		matlabLicenceFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		matlabLicenceFieldEditor.getTextControl(container).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		addField(matlabLicenceFieldEditor);
		
		// MATLAB_SCRIPTS_LOCATION
		DirectoryFieldEditor matlabScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, DocometreMessages.MatlabEngineScriptLocation, container);
		matlabScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		matlabScriptsLocationFieldEditor.setEnabled(false, container);
		addField(matlabScriptsLocationFieldEditor);
		
		// MATLAB_USER_SCRIPTS_LOCATION
		DirectoryFieldEditor matlabUserScriptsLocationFieldEditor = new DirectoryFieldEditor(GeneralPreferenceConstants.MATLAB_USER_SCRIPTS_LOCATION, DocometreMessages.MatlabEngineUserScriptLocation, container);
		matlabUserScriptsLocationFieldEditor.getLabelControl(container).setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		addField(matlabUserScriptsLocationFieldEditor);
	}
	
	@Override
	public boolean performOk() {
		boolean value = super.performOk();
		FunctionsView.refresh(false);
		return value;
	}

}
