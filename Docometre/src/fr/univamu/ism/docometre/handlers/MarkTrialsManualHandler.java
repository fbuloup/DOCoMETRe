package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.TrialStartMode;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class MarkTrialsManualHandler extends MarkTrialsHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			MarkTrialsAutoHandler.applyValue(resources, TrialStartMode.MANUAL);
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} finally {
			for (IResource resource : resources) {
				ExperimentsView.refresh(resource, null);
			}
		}
		return null;
	}

	
}
