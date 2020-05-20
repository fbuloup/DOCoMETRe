package fr.univamu.ism.docometre;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.MessagesView;

public class AnalysePerspective implements IPerspectiveFactory {
	
	public static String ID = "Docometre.analyseperspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.addView(SubjectsView.ID, IPageLayout.LEFT, .25f, layout.getEditorArea());
//		layout.getViewLayout(ExperimentsView.ID).setCloseable(false);
//		layout.addView(DescriptionView.ID, IPageLayout.BOTTOM, .75f, ExperimentsView.ID);
		layout.addView(MessagesView.ID, IPageLayout.BOTTOM, .75f, layout.getEditorArea());
	}

}
