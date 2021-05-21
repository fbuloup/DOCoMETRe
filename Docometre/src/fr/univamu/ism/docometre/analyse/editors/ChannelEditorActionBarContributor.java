package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;

import fr.univamu.ism.docometre.AcquirePerspective;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;

public class ChannelEditorActionBarContributor extends EditorActionBarContributor implements IPerspectiveListener {
	
	private class NextTrialAction extends Action {
		public NextTrialAction() {
			super("Next trial", Activator.getImageDescriptor(IImageKeys.NEXT_ICON));
		}
		
		@Override
		public void run() {
			editor.gotoNextTrial();
		}
	}
	
	private class PreviousTrialAction extends Action {
		public PreviousTrialAction() {
			super("Previous trial", Activator.getImageDescriptor(IImageKeys.PREVIOUS_ICON));
		}
		@Override
		public void run() {
			editor.gotoPreviousTrial();
		}
	}

	private ActionContributionItem nextTrialActionContributionItem;
	private ActionContributionItem previousTrialActionContributionItem;
	private SubToolBarManager subToolBarManager;
	private TrialsEditor editor;

	public ChannelEditorActionBarContributor() {
		nextTrialActionContributionItem = new ActionContributionItem(new NextTrialAction());
		previousTrialActionContributionItem = new ActionContributionItem(new PreviousTrialAction());
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		subToolBarManager = new SubToolBarManager(toolBarManager);
		
		subToolBarManager.add(previousTrialActionContributionItem);
		subToolBarManager.add(nextTrialActionContributionItem);
	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}
	
	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		editor = null;
		if(!(targetEditor instanceof TrialsEditor)) return;
		editor = (TrialsEditor) targetEditor;
		
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		boolean actionsVisibility = !perspective.getId().equals(AcquirePerspective.ID);
		
		nextTrialActionContributionItem.setVisible(actionsVisibility);
		previousTrialActionContributionItem.setVisible(actionsVisibility);
		
		subToolBarManager.setVisible(actionsVisibility);
		
		nextTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		previousTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		
		subToolBarManager.update(true);
		
		if(subToolBarManager.getParent() != null) {
			subToolBarManager.getParent().update(true);
		}
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub
		
	}

}
