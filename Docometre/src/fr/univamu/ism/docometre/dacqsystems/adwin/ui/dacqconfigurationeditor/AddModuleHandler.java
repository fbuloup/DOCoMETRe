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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor;

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModulesList;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class AddModuleHandler extends SelectionAdapter implements ISelectionChangedListener {
	
	private Shell shell;
	private ListViewer modulesListViewer;
	private ArrayList<ADWinModulesList> selectedADWinModulesList = new ArrayList<ADWinModulesList>();
	private IOperationHistory operationHistory;
	private DACQConfiguration dacqConfiguration;
	private IUndoContext undoContext;
	
	private class AddModuleDialog extends TitleAreaDialog {

		public AddModuleDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(ADWinMessages.AddModuleDialog_ShellTitle);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle(ADWinMessages.AddModuleDialog_Title);
			setMessage(ADWinMessages.AddModuleDialog_Message);
			setTitleImage(Activator.getImageDescriptor(IImageKeys.MODULE_WIZBAN).createImage());
			Composite container = (Composite) super.createDialogArea(parent);
			
			modulesListViewer = new ListViewer(container);
			modulesListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			modulesListViewer.setContentProvider(new ArrayContentProvider());
			modulesListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ADWinModulesList.getDescription((ADWinModulesList) element);
				}
			});
			modulesListViewer.setInput(ADWinModulesList.values());
			modulesListViewer.addSelectionChangedListener(AddModuleHandler.this);
			
			modulesListViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					buttonPressed(IDialogConstants.OK_ID); 
				}
			});
			
			return container;
		}
		
	}
	
	public AddModuleHandler(Shell shell,  ResourceEditor editor) {
		this.shell = shell;
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)editor.getEditorInput();
		this.dacqConfiguration = (DACQConfiguration)(resourceEditorInput.getObject());
		this.undoContext = editor.getUndoContext();
	}
	
	@Override
	public void widgetSelected(SelectionEvent event) {
		AddModuleDialog addModuleDialog = new AddModuleDialog(shell);
		if(addModuleDialog.open() == Dialog.OK) {
			try {
				operationHistory.execute(new AddModulesOperation(ADWinMessages.AddModulesOperation_Label, dacqConfiguration, selectedADWinModulesList, undoContext), null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		selectedADWinModulesList.clear();
		StructuredSelection selection = (StructuredSelection)modulesListViewer.getSelection();
		selectedADWinModulesList.addAll(selection.toList());
	}
}
