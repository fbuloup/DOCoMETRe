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
package fr.univamu.ism.docometre.dacqsystems.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

public class ProcessEditor extends MultiPageEditorPart implements IPageChangedListener, PartNameRefresher/*, IResourceChangeListener*/ {
	
	protected ArrayList<AbstractScriptSegmentEditor> segmentEditors;
	private PartListenerAdapter partListenerAdapter;
	protected CommandStack commandStack;
	protected SourceEditor sourceEditor;
	
	private Process getProcess() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (Process) resourceEditorInput.getObject();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.PROCESS_ICON, Activator.getImage(IImageKeys.PROCESS_ICON));
		}
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.ERROR_DECORATOR) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.ERROR_DECORATOR, new DecorationOverlayIcon(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON), Activator.getImageDescriptor(IImageKeys.ERROR_ICON), IDecoration.BOTTOM_LEFT));
		}
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.WARNING_DECORATOR) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.WARNING_DECORATOR, new DecorationOverlayIcon(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON), Activator.getImageDescriptor(IImageKeys.WARNING_ICON), IDecoration.BOTTOM_LEFT));
		}
		
		IResource resource = ObjectsController.getResourceForObject(getProcess());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		
		addPageChangedListener(this);
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == ProcessEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					ProcessEditor.this.removePageChangedListener(ProcessEditor.this);
				}
			}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			private void update(IWorkbenchPartReference partRef) {
				IWorkbenchPart part = partRef.getPart(false);
				if(part == ProcessEditor.this) {
					if(getSelectedPage() instanceof AbstractScriptSegmentEditor)
						((AbstractScriptSegmentEditor)getSelectedPage()).updatePasteAction();
				}
				
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}

	@Override
	protected void createPages() {
		commandStack = new CommandStack();
		segmentEditors = new ArrayList<AbstractScriptSegmentEditor>(0);
		updateTitleImage();
	}
	
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		IResource resource = ObjectsController.getResourceForObject(object);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Object object = getProcess();
		IResource processFile = ObjectsController.getResourceForObject(object);
		ObjectsController.serialize((IFile) processFile, object);
		for (AbstractScriptSegmentEditor abstractScriptSegmentEditor : segmentEditors) {
			abstractScriptSegmentEditor.doSave(monitor);
		}
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void activateSegmentProcessEditor(ScriptSegment scriptSegment) {
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) if(getActivePage() != 0) setActivePage(0);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) if(getActivePage() != 1) setActivePage(1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) if(getActivePage() != 2) setActivePage(2);
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		for (AbstractScriptSegmentEditor abstractScriptSegmentEditor : segmentEditors) {
			if(event.getSelectedPage() == abstractScriptSegmentEditor) abstractScriptSegmentEditor.updatePasteAction();
		}
		if(event.getSelectedPage() == sourceEditor) {
			FindDialog.getInstance().setTextViewer(sourceEditor.getSourceViewer());
		}
		if(event.getSelectedPage() == sourceEditor) 
			sourceEditor.update(sourceEditor.getCode());
	}

	@Override
	public void dispose() {
		super.dispose();
		ObjectsController.removeHandle(getProcess().getDACQConfiguration());
		ObjectsController.removeHandle(getProcess());
		getProcess().resetDACQConfiguration();
	}

	public void updateTitleImage() {
		try {
			IResource process = ObjectsController.getResourceForObject(getProcess());
			int severity = process.findMaxProblemSeverity(DocometreBuilder.MARKER_ID, true, IResource.DEPTH_INFINITE);
			if(severity == IMarker.SEVERITY_ERROR) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.ERROR_DECORATOR));
			if(severity == IMarker.SEVERITY_WARNING) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.WARNING_DECORATOR));
			if(severity == -1) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON));
			if(sourceEditor != null) sourceEditor.updateMarkers();
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
	}
}
