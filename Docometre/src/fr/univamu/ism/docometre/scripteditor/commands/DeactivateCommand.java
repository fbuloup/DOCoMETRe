package fr.univamu.ism.docometre.scripteditor.commands;

import org.eclipse.gef.commands.Command;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegment;

public class DeactivateCommand extends Command {
	
	private Block block;
	private ScriptSegment scriptSegment;
	private BlockEditPart blockEditPart;
	
	public DeactivateCommand(ScriptSegment scriptSegment, BlockEditPart blockEditPart) {
		this.blockEditPart = blockEditPart;
		this.block = blockEditPart.getModel();
		this.scriptSegment = scriptSegment;
	}
	
	@Override
	public void execute() {
		activateSegmentProcessEditor();
		block.setActivated(!block.isActivated());
		blockEditPart.refresh();
	}
	
	@Override
	public void undo() {
		activateSegmentProcessEditor();
		block.setActivated(!block.isActivated());
		blockEditPart.refresh();
	}
	
	private void activateSegmentProcessEditor() {
		Activator.activateScriptSegmentEditor(scriptSegment);
	}

}
