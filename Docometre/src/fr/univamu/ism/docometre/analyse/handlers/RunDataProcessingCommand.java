package fr.univamu.ism.docometre.analyse.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class RunDataProcessingCommand extends AbstractHandler implements ISelectionListener {
	
	private Set<IResource>  selectedDataProcesses = new HashSet<IResource>(0);

	public RunDataProcessingCommand() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logInfoMessage(DocometreMessages.PleaseStartMathEngineFirst, getClass());
			return null;
		}
		for (IResource dataProcessing : selectedDataProcesses) {
			boolean removeHandle = false;
			Object object = ResourceProperties.getObjectSessionProperty(dataProcessing);
			if(object == null) {
				object = ObjectsController.deserialize((IFile)dataProcessing);
				ResourceProperties.setObjectSessionProperty(dataProcessing, object);
				ObjectsController.addHandle(object);
				removeHandle = true;
			}
			if(object instanceof Script) {
				try {
					Script script = (Script)object;
					String code = script.getLoopCode(object, ScriptSegmentType.LOOP);
					MathEngineFactory.getMathEngine().runScript(code);
					IResource[] modifiedSubjects = MathEngineFactory.getMathEngine().getCreatedOrModifiedSubjects();
					for (IResource modifiedSubject : modifiedSubjects) {
						ResourceProperties.setSubjectModified(modifiedSubject, true);
						ExperimentsView.refresh(modifiedSubject, null);
						SubjectsView.refresh(modifiedSubject, null);
					}
				} catch (Exception e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(removeHandle) ObjectsController.removeHandle(object);
		}
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedDataProcesses.clear();
		if(!(selection instanceof StructuredSelection)) return;
		StructuredSelection structuredSelection = (StructuredSelection)selection;
		for (Object element : structuredSelection) {
			if(element instanceof IResource) {
				IResource resource = (IResource)element;
				if(ResourceType.isDataProcessing(resource)) selectedDataProcesses.add(resource);
			}
		}
		setBaseEnabled(selectedDataProcesses.size() > 0);
	}

}
