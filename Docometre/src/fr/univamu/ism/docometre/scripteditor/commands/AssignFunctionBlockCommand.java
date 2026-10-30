/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.scripteditor.commands;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.Command;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.functions.CustomerFunction;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.docometre.scripteditor.connections.model.BlocksConnection;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

/**
 * Replace the function of a block by a new function, with the same connections.
 * Block edit part is only used to create this command : it is replaced when the function is swapped.
 * Connections commands are executed, undone and redone by this command, not by the command stack,
 * so that undo and redo always apply to this command only.
 */
public class AssignFunctionBlockCommand extends Command {
	
	private Function oldFunction;
	private Function newFunction;
	private Script script;
	private ScriptSegment scriptSegment;
	private List<Command> deleteConnectionsCommands = new ArrayList<>();
	private List<Command> createConnectionsCommands;
	private List<Block> targetBlocks = new ArrayList<>();
	private List<Block> sourceBlocks = new ArrayList<>();
	// DO blocks whose end block is the old function : it must be the new function
	private List<DoBlock> endedDoBlocks = new ArrayList<>();

	public AssignFunctionBlockCommand(BlockEditPart blockEditPart, String functionClassName, String name) {
		try {
			
			ResourceEditorInput resourceEditorInput = (ResourceEditorInput) ((DefaultEditDomain) blockEditPart.getRoot().getViewer().getEditDomain()).getEditorPart().getEditorInput();
			Object process = resourceEditorInput.getObject();
			
			String customerFunctionFileName = functionClassName;
			if(FunctionFactory.isCustomerFunction(process, functionClassName)) 
				functionClassName = CustomerFunction.class.getName();
			
			Class<?> clazz = Class.forName(functionClassName);
			Constructor<?> constructor = clazz.getConstructor();
			newFunction = (Function) constructor.newInstance();
			newFunction.setName(name);
			newFunction.setClassName(functionClassName);
			
			if(FunctionFactory.isCustomerFunction(process, customerFunctionFileName)) 
				((CustomerFunction)newFunction).setFunctionFileName(customerFunctionFileName);
			
			oldFunction = ((Function) blockEditPart.getModel());
			script = oldFunction.getScript();
			newFunction.setSizeAndLocation(oldFunction.getSizeAndLocation());
			this.scriptSegment = (ScriptSegment) blockEditPart.getParent().getModel();
			
			// Save target blocks of the block and create source connections deletion commands
			for (Object element : blockEditPart.getSourceConnections()) {
				BlocksConnection connection = (BlocksConnection)((ConnectionEditPart)element).getModel();
				targetBlocks.add(connection.targetBlock);
				deleteConnectionsCommands.add(new DeleteConnectionCommand(scriptSegment, connection));
				if(connection.targetBlock instanceof DoBlock && oldFunction.isLastBlockOf((DoBlock) connection.targetBlock)) 
					endedDoBlocks.add((DoBlock) connection.targetBlock);
			}
			// Save source blocks of the block and create target connections deletion commands
			for (Object element : blockEditPart.getTargetConnections()) {
				BlocksConnection connection = (BlocksConnection)((ConnectionEditPart)element).getModel();
				sourceBlocks.add(connection.sourceBlock);
				deleteConnectionsCommands.add(new DeleteConnectionCommand(scriptSegment, connection));
			}
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean canExecute() {
		return oldFunction != null && newFunction != null;
	}
	
	/*
	 * Connections of new function are created once it replaces old function in the script
	 */
	private void createConnectionsCommands() {
		createConnectionsCommands = new ArrayList<>();
		for (Block sourceBlock : sourceBlocks) {
			CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, sourceBlock);
			createConnectionCommand.setTarget(newFunction);
			createConnectionsCommands.add(createConnectionCommand);
		}
		for (Block targetBlock : targetBlocks) {
			CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, newFunction);
			createConnectionCommand.setTarget(targetBlock);
			createConnectionsCommands.add(createConnectionCommand);
		}
	}
	
	private void swapFunctions(Function function1, Function function2) {
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) script.removeInitializeBlock(function1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) script.removeLoopBlock(function1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) script.removeFinalizeBlock(function1);
		
		function1.setScript(null);
		function2.setScript(script);
		
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) script.addInitializeBlock(function2);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) script.addLoopBlock(function2);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) script.addFinalizeBlock(function2);
		script.selectBlocks(new Block[] {function2});
	}
	
	@Override
	public void execute() {
		activateSegmentProcessEditor();
		for (Command command : deleteConnectionsCommands) command.execute();
		swapFunctions(oldFunction, newFunction);
		if(createConnectionsCommands == null) createConnectionsCommands();
		for (Command command : createConnectionsCommands) command.execute();
		for (DoBlock doBlock : endedDoBlocks) doBlock.setEndBlock(newFunction);
	}
	
	@Override
	public void undo() {
		activateSegmentProcessEditor();
		for (int i = createConnectionsCommands.size() - 1; i >= 0; i--) createConnectionsCommands.get(i).undo();
		swapFunctions(newFunction, oldFunction);
		// Connections are restored in their initial order, so that next blocks keep their initial order
		for (Command command : deleteConnectionsCommands) command.undo();
		for (DoBlock doBlock : endedDoBlocks) doBlock.setEndBlock(oldFunction);
	}
	
	private void activateSegmentProcessEditor() {
		Activator.activateScriptSegmentEditor(scriptSegment);
	}

}
