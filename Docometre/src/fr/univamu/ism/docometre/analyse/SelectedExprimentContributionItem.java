package fr.univamu.ism.docometre.analyse;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.AnalysePerspective;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;

public class SelectedExprimentContributionItem extends StatusLineContributionItem implements IPerspectiveListener, IResourceChangeListener {
	
	public static IResource selectedExperiment;

	public SelectedExprimentContributionItem(String id) {
		super(id);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		updateLabel(perspective);
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		updateLabel(perspective);
	}
	
	private void updateLabel(IPerspectiveDescriptor perspective) {
		if(AnalysePerspective.ID.equals(perspective.getId())) {
			if(selectedExperiment != null) ApplicationActionBarAdvisor.selectedExprimentContributionItem.setText(DocometreMessages.Experiment_Label + selectedExperiment.getName());
		} else ApplicationActionBarAdvisor.selectedExprimentContributionItem.setText("");
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta[] resourceDeltas = event.getDelta().getAffectedChildren();
		for (IResourceDelta resourceDelta : resourceDeltas) {
			if(resourceDelta.getResource() == selectedExperiment && resourceDelta.getKind() == IResourceDelta.REMOVED) selectedExperiment = null; 
		}
	}

}
