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
package fr.univamu.ism.docometre.analyse.wizard;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class ExportScriptWizardPage extends WizardPage {
	
	private static String lastFolderPath;
	
	private String folderPath;
	private String fileName;
	private Text fileNameText;
	private Text fileDestinationText;
	private IResource batchResource;

	protected ExportScriptWizardPage(IResource batchResource) {
		super("ExportScriptWizardPage");
		setTitle(DocometreMessages.ExportScriptWizardPageTitle);
		setDescription(DocometreMessages.ExportScriptWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor("org.eclipse.ui", "icons/full/wizban/export_wiz.png"));
		setPageComplete(false);
		this.batchResource = batchResource;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		setControl(container);
		
		Label fileDestinationLabel = new Label(container, SWT.NORMAL);
		fileDestinationLabel.setText(DocometreMessages.DestinationLabel);
		fileDestinationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		fileDestinationText = new Text(container, SWT.READ_ONLY | SWT.BORDER);
		fileDestinationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileDestinationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		});
		if(lastFolderPath != null && !"".equals(lastFolderPath)) fileDestinationText.setText(lastFolderPath);
		Button fileButton =  new Button(container, SWT.FLAT);
		fileButton.setText(DocometreMessages.Browse);
		fileButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(ExportScriptWizardPage.this.getShell());
				String scriptFileFullPath = directoryDialog.open();
				if(scriptFileFullPath != null) fileDestinationText.setText(scriptFileFullPath);
			}
		});
		Label fileNameLabel = new Label(container, SWT.NORMAL);
		fileNameLabel.setText(DocometreMessages.FileName);
		fileNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		fileNameText = new Text(container, SWT.NORMAL | SWT.BORDER);
		fileNameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		fileNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		});
		String extension = ".m";
		if(MathEngineFactory.isPython()) extension = ".py"; 
		fileNameText.setText(GetResourceLabelDelegate.getLabel(batchResource) + extension);
	}
	
	private void checkPageComplete() {
		fileName = fileNameText.getText();
		folderPath = fileDestinationText.getText();
		lastFolderPath = folderPath;
		setErrorMessage(null);
		setPageComplete(false);
		if("".equals(folderPath)) {
			setErrorMessage(DocometreMessages.ScriptExportError1);
			return;
		}
		if("".equals(fileName)) {
			setErrorMessage(DocometreMessages.ScriptExportError2);
			return;
		}
		setPageComplete(true);
	}

	public String getScriptFileFullPath() {
		return folderPath + File.separator + fileName;
	}

}
