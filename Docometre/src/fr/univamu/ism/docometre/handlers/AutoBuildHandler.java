package fr.univamu.ism.docometre.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class AutoBuildHandler extends AbstractHandler implements IElementUpdater {
	
	public static String ID = "AutoBuildCommand";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean autoBuild = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.BUILD_AUTOMATICALLY);
		autoBuild = !autoBuild;
		Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.BUILD_AUTOMATICALLY, autoBuild);
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ICommandService.class);
		commandService.refreshElements(ID, null);
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		boolean autoBuild = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.BUILD_AUTOMATICALLY);
		element.setChecked(autoBuild);
	}

}
