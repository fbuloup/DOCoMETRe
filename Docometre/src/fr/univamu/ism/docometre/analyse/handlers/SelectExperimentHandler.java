package fr.univamu.ism.docometre.analyse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;

public final class SelectExperimentHandler extends AbstractHandler {
	
	private final class ExperimentLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if(element instanceof IResource) return ((IResource)element).getName(); 
			return super.getText(element);
		}
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ElementListSelectionDialog selectedExperimentDialog = new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new ExperimentLabelProvider());
		selectedExperimentDialog.setMessage(DocometreMessages.SelectExperimentDialogMessage);
		selectedExperimentDialog.setTitle(DocometreMessages.SelectExperimentDialogTitle);
		selectedExperimentDialog.setElements(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		if(selectedExperimentDialog.open() == Dialog.OK) {
			SelectedExprimentContributionItem.selectedExperiment = (IResource)selectedExperimentDialog.getResult()[0];
			ApplicationActionBarAdvisor.selectedExprimentContributionItem.setText(DocometreMessages.Experiment_Label + SelectedExprimentContributionItem.selectedExperiment.getName());
			IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
			if(view != null) ((SubjectsView) view).updateInput();
		}
		return null;
	}
	
}
