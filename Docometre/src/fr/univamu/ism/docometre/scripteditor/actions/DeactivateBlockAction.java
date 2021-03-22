package fr.univamu.ism.docometre.scripteditor.actions;

import java.util.List;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;

public class DeactivateBlockAction extends SelectionAction {
	
	public static final String DEACTIVATE_BLOCK = "DeactivateBlock";
	public static final String REQ_DEACTIVATE_BLOCK = "REQ_DEACTIVATE_BLOCK";
	
	private Request requestDeactivateBlock;

	public DeactivateBlockAction(IWorkbenchPart part) {
		super(part);
		setId(DEACTIVATE_BLOCK);
		setText(DocometreMessages.ToggleActivatedState);
		requestDeactivateBlock = new Request(REQ_DEACTIVATE_BLOCK);
	}

	@Override
	protected boolean calculateEnabled() {
		@SuppressWarnings("rawtypes")
		List selectedObjects = getSelectedObjects();
		boolean enabled = true;
		for (Object object : selectedObjects) {
			enabled = enabled && (object instanceof BlockEditPart);
		}
		return enabled;
	}
	
	@Override
	public void run() {
		@SuppressWarnings("rawtypes")
		List selectedObjects = getSelectedObjects();
		Command command = null;
		for (Object object : selectedObjects) {
			BlockEditPart selectedBlockEditPart = (BlockEditPart) object;
			Command newCommand = selectedBlockEditPart.getCommand(requestDeactivateBlock);
			if(command == null) command = newCommand;
			else command = newCommand.chain(command);
			
		}
		execute(command);
	}
	
	

}
