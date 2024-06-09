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
package fr.univamu.ism.docometre.editors;

import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.operations.UndoRedoActionGroup;

import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;

public abstract class ResourceEditor extends FormEditor implements PartNameRefresher {
	
	private ObjectUndoContext resourceEditorUndoContext;
	private UndoRedoActionGroup undoRedoActionGroup;
	
	public ObjectUndoContext getUndoContext() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		IResource resource = ObjectsController.getResourceForObject(object);
		String fullName = resource.getFullPath().toOSString();
		if(resourceEditorUndoContext == null) resourceEditorUndoContext = new ObjectUndoContext(this, "ResourceEditorUndoContext_" + fullName);
		return resourceEditorUndoContext;
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(getEditorInput().getName());
		setTitleToolTip(getEditorInput().getToolTipText());
		undoRedoActionGroup = new UndoRedoActionGroup(getSite(), getUndoContext(), true);
		undoRedoActionGroup.fillActionBars(getEditorSite().getActionBars());
	}
	
	public UndoRedoActionGroup getUndoRedoActionGroup() {
		return undoRedoActionGroup;
	}
	
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		IResource resource = ObjectsController.getResourceForObject(object);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}
	
	public void updateTitle(int pageIndex, String title) {
		setPageText(pageIndex, title);
	}
	
	public int getFormPageCount() {
		return getPageCount();
	}
	
	public IFormPage getPage(int index) {
		return (IFormPage) pages.get(index);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		ObjectsController.removeHandle(object);
	}
	
}
