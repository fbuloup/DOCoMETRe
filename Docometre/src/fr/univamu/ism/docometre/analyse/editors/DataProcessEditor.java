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
package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class DataProcessEditor extends MultiPageEditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.DataProcessEditor";
	
	private CommandStack commandStack;

	private DataProcessScriptEditor dataProcessScriptEditor;

	private PartListenerAdapter partListenerAdapter;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(GetResourceLabelDelegate.getLabel(ObjectsController.getResourceForObject(getDataProcessingScript())));
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == DataProcessEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
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
			
			
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		addPageChangedListener(new IPageChangedListener() {
			@Override
			public void pageChanged(PageChangedEvent event) {
				IWorkbenchPartReference partRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePartReference();
				update(partRef);
			}
		});
			
	}
	
	private void update(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if(part == DataProcessEditor.this) {
			if(getSelectedPage() instanceof AbstractScriptSegmentEditor)
				((AbstractScriptSegmentEditor)getSelectedPage()).updatePasteAction();
		}
		
	}

	@Override
	protected void createPages() {
		try {
			commandStack = new CommandStack();
			
			dataProcessScriptEditor = new DataProcessScriptEditor(commandStack);
			int pageIndex = addPage(dataProcessScriptEditor, getEditorInput());
			setPageText(pageIndex, DocometreMessages.MathEngineEditorTitle);
			
			DataProcessScriptSourceEditor scriptSourceEditor = new DataProcessScriptSourceEditor(this);
			pageIndex = addPage(scriptSourceEditor, getEditorInput());
			setPageText(pageIndex, DocometreMessages.MathEngineSourceCodeEditorTitle);
		} catch (PartInitException e) {
			Activator.getLogErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private Script getDataProcessingScript() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (Script) resourceEditorInput.getObject();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Object object = getDataProcessingScript();
		IResource processFile = ObjectsController.getResourceForObject(object);
		ObjectsController.serialize((IFile) processFile, object);
		dataProcessScriptEditor.doSave(monitor);
		try {
			Script script = (Script) object;
			System.out.println(script.getLoopCode(object, ScriptSegmentType.LOOP));
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
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
	
	@Override
	public void dispose() {
		super.dispose();
		ObjectsController.removeHandle(getDataProcessingScript());
	}

	@Override
	public void refreshPartName() {
		IResource resource = ObjectsController.getResourceForObject(getDataProcessingScript());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

	public void activateSegmentProcessEditor() {
		 if(getActivePage() != 0) setActivePage(0);
	}

}
