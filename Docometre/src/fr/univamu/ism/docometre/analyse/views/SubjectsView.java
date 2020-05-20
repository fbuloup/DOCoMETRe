package fr.univamu.ism.docometre.analyse.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.views.ExperimentsLabelProvider;

public class SubjectsView extends ViewPart {
	
	public static String ID = "Docometre.SubjectsView";
	private TreeViewer subjectsTreeViewer;

	public SubjectsView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		subjectsTreeViewer = new TreeViewer(parent);
		subjectsTreeViewer.setContentProvider(new SubjectsContentProvider());
		subjectsTreeViewer.setLabelProvider(new ExperimentsLabelProvider());
		
		getSite().setSelectionProvider(subjectsTreeViewer);
		
		updateInput();
	}
	
	public void updateInput() {
		subjectsTreeViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
	}

	@Override
	public void setFocus() {
		subjectsTreeViewer.getTree().setFocus();
	}

}
