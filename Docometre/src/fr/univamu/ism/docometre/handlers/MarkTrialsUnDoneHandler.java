package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class MarkTrialsUnDoneHandler extends MarkTrialsHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			MarkTrialsDoneHandler.applyValue(resources, false);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} finally {
			for (IResource resource : resources) {
				ExperimentsView.refresh(resource, null);
			}
		}
		return  null;
	}

}
