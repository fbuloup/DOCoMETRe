package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class DataProcessEditor extends MultiPageEditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.DataProcessEditor";
	
	private CommandStack commandStack;

	private DataProcessScriptEditor dataProcessScriptEditor;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(GetResourceLabelDelegate.getLabel(ObjectsController.getResourceForObject(getDataProcessingScript())));
	}

	@Override
	protected void createPages() {
		try {
			commandStack = new CommandStack();
			
			dataProcessScriptEditor = new DataProcessScriptEditor(commandStack);
			int pageIndex = addPage(dataProcessScriptEditor, getEditorInput());
			setPageText(pageIndex, DocometreMessages.MathEngineEditorTitle);
			
			DataProcessScriptSourceEditor scriptSourceEditor = new DataProcessScriptSourceEditor(commandStack, this);
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
