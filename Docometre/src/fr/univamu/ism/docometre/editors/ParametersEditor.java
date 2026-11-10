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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.text.Document;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.PartListenerAdapter;

public class ParametersEditor extends MultiPageEditorPart implements PartNameRefresher, IPageChangedListener {
	
	public static String ID = "Docometre.ParametersEditor";
	
//	private SourceViewer sourceViewer;
	private boolean dirty;
	protected Document document;
//	private PartListenerAdapter partListenerAdapter;
//	private ArrayList<Font> fontsArrayList = new ArrayList<>();
	private ParametersRawEditor parametersRawEditor;

private ParametersTableEditor parametersTableEditor;

	// The file has been changed on disk (e.g. propagated parameters written at the end of a trial)
	// while the editor had unsaved modifications : the user chooses to reload it or not (see askReload())
	private boolean fileChangedOnDisk;
	// The user has already been asked to reload the file for this change
	private boolean reloadAsked;
	// Changes of the file on disk (see fileChanged())
	private IResourceChangeListener resourceChangeListener;
	// Activation of this editor (see askReload())
	private PartListenerAdapter partListenerAdapter;
	
	public ParametersEditor() {
	}
	
	public Document getDocument() {
		if(document == null) document = new Document();
		return document;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// The file has been changed on disk since it was read : the user chooses to overwrite it or not
		if(fileChangedOnDisk) {
			String message = NLS.bind(DocometreMessages.ParametersFileChanged_OverwriteMessage, getParametersFile().getName());
			if(!MessageDialog.openQuestion(getSite().getShell(), DocometreMessages.ParametersFileChanged_Title, message)) return;
		}
		try {
			IResource paramsFile = ((IResource)((ResourceEditorInput)getEditorInput()).getObject());
			Files.write(Paths.get(paramsFile.getLocationURI()), document.get().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
			// File written on disk : notify workspace (views, experiments history)
			Activator.refreshWorkspaceFile(Paths.get(paramsFile.getLocationURI()));
			setDirty(false);
			fileChangedOnDisk = false;
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
		// The file can be changed on disk while it is edited : propagated parameters are written at the end of each trial
		resourceChangeListener = new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if(event.getDelta() == null) return;
				IResourceDelta delta = event.getDelta().findMember(getParametersFile().getFullPath());
				if(delta == null || delta.getKind() != IResourceDelta.CHANGED) return;
				if((delta.getFlags() & (IResourceDelta.CONTENT | IResourceDelta.REPLACED)) == 0) return;
				PlatformUI.getWorkbench().getDisplay().asyncExec(() -> fileChanged());
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}
	
	private IResource getParametersFile() {
		return (IResource)((ResourceEditorInput)getEditorInput()).getObject();
	}
	
	/*
	 * Content of the file on disk, as read when the editor is opened (lines separated by \n), null if it can't be read
	 */
	private String readParametersFile() {
		try {
			List<String> lines = Files.readAllLines(Paths.get(getParametersFile().getLocationURI()), StandardCharsets.UTF_8);
			return String.join("\n", lines);
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			return null;
		}
	}
	
	/*
	 * To compare the content of the file and of the document : line separators and final line separators are ignored
	 */
	private static String normalize(String content) {
		return content.replace("\r\n", "\n").replace('\r', '\n').replaceAll("\n+$", "");
	}
	
	/*
	 * The file has been changed on disk : it is reloaded if the editor has no unsaved modification,
	 * otherwise the user is asked (now if this editor is active, at its next activation otherwise)
	 */
	private void fileChanged() {
		if(getContainer() == null || getContainer().isDisposed()) return;
		String fileContent = readParametersFile();
		if(fileContent == null) return;
		// Same content : file saved by this editor
		if(normalize(fileContent).equals(normalize(getDocument().get()))) {
			fileChangedOnDisk = false;
			return;
		}
		if(!isDirty()) {
			reload(fileContent);
			return;
		}
		fileChangedOnDisk = true;
		reloadAsked = false;
		if(getSite().getPage().getActivePart() == this) askReload();
	}
	
	/*
	 * Asks the user, once per change, to reload the file changed on disk
	 */
	private void askReload() {
		if(!fileChangedOnDisk || reloadAsked) return;
		reloadAsked = true;
		String message = NLS.bind(DocometreMessages.ParametersFileChanged_ReloadMessage, getParametersFile().getName());
		if(MessageDialog.openQuestion(getSite().getShell(), DocometreMessages.ParametersFileChanged_Title, message)) {
			String fileContent = readParametersFile();
			if(fileContent != null) reload(fileContent);
		}
	}
	
	/*
	 * Replaces the content of the editor by the content of the file
	 */
	private void reload(String fileContent) {
		parametersRawEditor.setContent(fileContent);
		if(getActivePage() == 1) parametersTableEditor.update();
		setDirty(false);
		fileChangedOnDisk = false;
	}
	
	@Override
	public void dispose() {
		if(resourceChangeListener != null) ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		if(partListenerAdapter != null) getSite().getPage().removePartListener(partListenerAdapter);
		super.dispose();
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
			setPageText(pageIndex, DocometreMessages.ParametersEditor_RawFileTab);
			
			parametersTableEditor = new ParametersTableEditor(this);
			pageIndex = addPage(parametersTableEditor, getEditorInput());
			setPageText(pageIndex, DocometreMessages.ParametersEditor_TableTab);
			
			// File changed on disk while the editor had unsaved modifications : the user is asked when the editor is activated
			partListenerAdapter = new PartListenerAdapter() {
				@Override
				public void partActivated(IWorkbenchPartReference partRef) {
					if(partRef.getPart(false) == ParametersEditor.this) askReload();
				}
			};
			getSite().getPage().addPartListener(partListenerAdapter);
			
		} catch (PartInitException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if(event.getSelectedPage() == parametersRawEditor) {
			boolean isDirty = isDirty();
			parametersRawEditor.update();
			if(!isDirty) setDirty(false);
		}
		if(event.getSelectedPage() == parametersTableEditor) {
			parametersTableEditor.update();
		}
	}

}
