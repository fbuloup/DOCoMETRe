package fr.univamu.ism.docometre.consoles;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.internal.console.ConsoleMessages;
import org.eclipse.ui.internal.console.OpenConsoleAction;
import org.eclipse.ui.internal.console.PinConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

import fr.univamu.ism.docometre.Activator;

@SuppressWarnings({ "restriction", "unused" })
public class DocometreConsolePageParticipant implements IConsolePageParticipant {
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		IPageSite pageSite = page.getSite();
		IWorkbenchPage workbenchPage = pageSite.getPage();
		IViewPart viewPart = workbenchPage.findView( IConsoleConstants.ID_CONSOLE_VIEW );
		IViewSite viewSite = viewPart.getViewSite();
		IActionBars actionBars = viewSite.getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		IContributionItem[] contributionItems = toolBarManager.getItems();
		contributionItems = toolBarManager.getItems();
		for (int i = 0; i < contributionItems.length; i++) {
			if(!(contributionItems[i] instanceof ActionContributionItem)) continue;
			IAction currentAction = ((ActionContributionItem)contributionItems[i]).getAction();
			if(ConsoleMessages.PinConsoleAction_1.equals(currentAction.getToolTipText())) toolBarManager.remove(contributionItems[i]);
			if(ConsoleMessages.OpenConsoleAction_1.equals(currentAction.getToolTipText())) toolBarManager.remove(contributionItems[i]);
			if(ConsoleMessages.ConsoleDropDownAction_1.equals(currentAction.getToolTipText())) toolBarManager.remove(contributionItems[i]);
		}
		actionBars.updateActionBars();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		

	}

	@Override
	public void activated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivated() {
		// TODO Auto-generated method stub

	}

}
