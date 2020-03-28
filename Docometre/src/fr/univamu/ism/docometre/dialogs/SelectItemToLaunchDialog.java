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
package fr.univamu.ism.docometre.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsContentProvider;
import fr.univamu.ism.docometre.views.ExperimentsLabelProvider;
import fr.univamu.ism.docometre.views.ExperimentsViewerSorter;

public class SelectItemToLaunchDialog extends TitleAreaDialog {

	private IResource selectedResource;
	
	public SelectItemToLaunchDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle()); 
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DocometreMessages.SelectItemDialogShellTitle);
		
		Rectangle parentShellSize = newShell.getParent().getBounds();
		int d = 6;
		int dw = parentShellSize.width / d;
		int dh = parentShellSize.height / d;
		int x = parentShellSize.x + dw;
		int y = parentShellSize.y + dh;
		Rectangle shellSize = new Rectangle(x, y, (d-2)*dw, (d-2)*dh);
		newShell.setBounds(shellSize);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(DocometreMessages.SelectItemDialogTitle);
		setMessage(DocometreMessages.SelectItemDialogMessage);
		
		TreeViewer experimentsTreeViewer = new TreeViewer(parent);
		experimentsTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		experimentsTreeViewer.setContentProvider(new ExperimentsContentProvider());
		experimentsTreeViewer.setLabelProvider(new ExperimentsLabelProvider());
		experimentsTreeViewer.setComparator(new ExperimentsViewerSorter());
		
		experimentsTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)experimentsTreeViewer.getSelection();
				selectedResource = (IResource) selection.getFirstElement();
				boolean enable = selection.size() == 1;
				enable = enable && (ResourceType.isProcess(selectedResource) || ResourceType.isSession(selectedResource) || ResourceType.isSubject(selectedResource) || ResourceType.isTrial(selectedResource));
				if(enable && ResourceType.isTrial(selectedResource)) enable = enable && (ResourceProperties.getAssociatedProcess(selectedResource) != null);
				SelectItemToLaunchDialog.this.getButton(IDialogConstants.OK_ID).setEnabled(enable);
				if(!enable) selectedResource = null;
			}
		});
		
		experimentsTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection) experimentsTreeViewer.getSelection()).getFirstElement();
				if(element != null) experimentsTreeViewer.setExpandedState(element, !experimentsTreeViewer.getExpandedState(element));
			}
		});
		experimentsTreeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		
		return parent;
	}
	
	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if(id == IDialogConstants.OK_ID) button.setEnabled(false);
		return button;
	}
	
	public IResource getSelectedResource() {
		return selectedResource;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
