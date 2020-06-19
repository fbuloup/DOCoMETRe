package fr.univamu.ism.docometre.analyse.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.Analyse;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class LoadUnloadSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private Set<IResource> selectedSubjects = new HashSet<IResource>(0);
	
	public LoadUnloadSubjectsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logInfoMessage(DocometreMessages.PleaseStartMathEngineFirst, getClass());
			return null;
		}
		for (IResource subject : selectedSubjects) {
			boolean loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subject);
			if(loaded) {
				Activator.logInfoMessage(DocometreMessages.UnloadingSubject + subject.getName(), getClass());
				MathEngineFactory.getMathEngine().unload(subject);
				Activator.logInfoMessage(DocometreMessages.Done, getClass());
			} else {
				try {
					Activator.logInfoMessage(DocometreMessages.LoadingSubject + subject.getName(), getClass());
					// Get data files 
					String dataFilesList = Analyse.getDataFiles(subject);
					subject.setSessionProperty(ResourceProperties.DATA_FILES_LIST_QN, dataFilesList);
					MathEngineFactory.getMathEngine().load(subject);
					Activator.logInfoMessage(DocometreMessages.Done, getClass());
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			ExperimentsView.refresh(subject, null);
			SubjectsView.refresh(subject, null);
		}
		
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedSubjects.clear();
		if(!(selection instanceof StructuredSelection)) return;
		StructuredSelection structuredSelection = (StructuredSelection)selection;
		for (Object element : structuredSelection) {
			if(element instanceof IResource) {
				IResource resource = (IResource)element;
				if(ResourceType.isSubject(resource)) selectedSubjects.add(resource);
			}
		}
		setBaseEnabled(selectedSubjects.size() > 0);
	}

	
	
}
