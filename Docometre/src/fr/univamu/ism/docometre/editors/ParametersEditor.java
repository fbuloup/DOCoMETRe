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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.text.Document;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;

public class ParametersEditor extends MultiPageEditorPart implements PartNameRefresher, IPageChangedListener {
	
	public static String ID = "Docometre.ParametersEditor";
	
//	private SourceViewer sourceViewer;
	private boolean dirty;
	protected Document document;
//	private PartListenerAdapter partListenerAdapter;
//	private ArrayList<Font> fontsArrayList = new ArrayList<>();
	private ParametersRawEditor parametersRawEditor;

private ParametersTableEditor parametersTableEditor;
	
	public ParametersEditor() {
	}
	
	public Document getDocument() {
		if(document == null) document = new Document();
		return document;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			IResource paramsFile = ((IResource)((ResourceEditorInput)getEditorInput()).getObject());
			Files.write(Paths.get(paramsFile.getLocationURI()), document.get().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
			setDirty(false);
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		IResource resource = ((IResource)((ResourceEditorInput)input).getObject());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		addPageChangedListener(this);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void setFocus() {
		if(getActivePage() == 0) parametersRawEditor.setFocus();
		if(getActivePage() == 1) parametersTableEditor.setFocus();
	}

	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		setPartName(GetResourceLabelDelegate.getLabel((IResource) object));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

	@Override
	protected void createPages() {
		try {
			parametersRawEditor = new ParametersRawEditor(this);
			int pageIndex = addPage(parametersRawEditor, getEditorInput());
			setPageText(pageIndex, "Fichier brut");
			
			parametersTableEditor = new ParametersTableEditor(this);
			pageIndex = addPage(parametersTableEditor, getEditorInput());
			setPageText(pageIndex, "Tableau");
			
		} catch (PartInitException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if(event.getSelectedPage() == parametersRawEditor) {
			System.out.println("parametersRawEditor");
			parametersRawEditor.update();
			setDirty(false);
		}
		if(event.getSelectedPage() == parametersTableEditor) {
			System.out.println("parametersTableEditor");
			parametersTableEditor.update();
		}
	}

}
