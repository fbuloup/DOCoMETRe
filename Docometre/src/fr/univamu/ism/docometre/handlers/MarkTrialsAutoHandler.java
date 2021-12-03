package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.TrialStartMode;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class MarkTrialsAutoHandler extends MarkTrialsHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			applyValue(resources, TrialStartMode.AUTO);
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
	
	protected static void applyValue(IResource[] resources, TrialStartMode value) throws CoreException {
		for (IResource resource : resources) {
			if(ResourceType.isTrial(resource)) ResourceProperties.setTrialStartMode(resource, value);
			else applyValue(((IContainer)resource).members(), value);
		}
	}
	
}
