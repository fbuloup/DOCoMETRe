package fr.univamu.ism.docometre.analyse.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;

public class SaveModifiedSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private static SaveModifiedSubjectsHandler saveModifiedSubjectsHandler;
	private ArrayList<IResource> modifiedSubjects = new ArrayList<>();
	
	
	public SaveModifiedSubjectsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart subjectsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
		if(subjectsView != null) selectionChanged(subjectsView, subjectsView.getSite().getSelectionProvider().getSelection());
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler = this; 
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		for (IResource subject : modifiedSubjects) {
			MathEngineFactory.getMathEngine().saveSubject(subject);
		}
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		modifiedSubjects.clear();
		if(part instanceof SubjectsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				for (Object element : elements) {
					if(element instanceof IResource && ResourceType.isSubject((IResource) element)) {
						IResource subject = (IResource)element;
						try {
							Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
							if(value != null) {
								boolean modified = (boolean)value;
								if(modified) modifiedSubjects.add(subject);
							}
						} catch (CoreException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
			}
		}
		updateBaseEnabled();
	}
	
	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}
	
	private void updateBaseEnabled() {
		boolean enabled = false;
		for (IResource subject : modifiedSubjects) {
			try {
				Object value = subject.getSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN);
				if(value != null) {
					boolean modified = (boolean)value;
					enabled = enabled || modified;
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		setBaseEnabled(enabled);
	}
	
	public static void refresh() {
		SaveModifiedSubjectsHandler.saveModifiedSubjectsHandler.updateBaseEnabled();
	}

}
