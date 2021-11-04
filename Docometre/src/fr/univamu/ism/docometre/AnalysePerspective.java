package fr.univamu.ism.docometre;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import fr.univamu.ism.docometre.analyse.views.MathEngineCommandView;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.MessagesView;

public class AnalysePerspective implements IPerspectiveFactory {
	
	public static final String id = "Docometre.analyseperspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.addView(SubjectsView.ID, IPageLayout.LEFT, .25f, layout.getEditorArea());
//		layout.getViewLayout(ExperimentsView.ID).setCloseable(false);
//		layout.addView(DescriptionView.ID, IPageLayout.BOTTOM, .75f, ExperimentsView.ID);
		layout.addView(MessagesView.ID, IPageLayout.BOTTOM, .75f, layout.getEditorArea());
		layout.addView(MathEngineCommandView.ID, IPageLayout.RIGHT, .5f, MessagesView.ID);
		if(System.getProperty("Sleak") != null) {
			IFolderLayout folder = layout.createFolder("Sleak folder",  IPageLayout.RIGHT, .5f, layout.getEditorArea());
			folder.addView("org.eclipse.swt.tools.views.SleakView");
			folder.addView("org.eclipse.swt.tools.views.SpyView");
		}
	}

}
