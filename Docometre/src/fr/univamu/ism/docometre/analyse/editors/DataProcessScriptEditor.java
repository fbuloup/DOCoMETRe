package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.gef.commands.CommandStack;

import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class DataProcessScriptEditor extends AbstractScriptSegmentEditor {
	
	public static String ID = "Docometre.DataProcessScriptEditor";

	public DataProcessScriptEditor(CommandStack commandStack) {
		super(commandStack, ScriptSegmentType.LOOP);
	}
	
	@Override
	protected Script getScript() {
		return (Script) ((ResourceEditorInput)getEditorInput()).getObject();
	}

}
