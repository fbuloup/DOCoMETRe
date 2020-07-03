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

import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.functions.CustomerFunction;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.docometre.scripteditor.connections.model.BlocksConnection;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

public class AssignFunctionBlockCommand extends Command {
	
	private BlockEditPart blockEditPart;
	private Function oldFunction;
	private Function newFunction;
	private Script script;
	private ScriptSegment scriptSegment;
	private ArrayList<Command> deleteConnectionsCommands;
	private ArrayList<Command> createConnectionsCommands;
	private CommandStack commandStack;
	private ArrayList<Block> targetBlocks;
	private ArrayList<Block> sourceBlocks;

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
			this.blockEditPart = blockEditPart;
			this.scriptSegment = (ScriptSegment) blockEditPart.getParent().getModel();;
			commandStack = blockEditPart.getRoot().getViewer().getEditDomain().getCommandStack();
			
			// Save target blocks of the block
			targetBlocks = new ArrayList<Block>(0);
			for (Object element : blockEditPart.getSourceConnections()) {
				ConnectionEditPart connectionEditPart = (ConnectionEditPart)element;
				targetBlocks.add(((BlocksConnection)connectionEditPart.getModel()).targetBlock);
			}
			// Save source block of this block
			sourceBlocks = new ArrayList<Block>(0);
			for (Object element : blockEditPart.getTargetConnections()) {
				ConnectionEditPart connectionEditPart = (ConnectionEditPart)element;
				sourceBlocks.add(((BlocksConnection)connectionEditPart.getModel()).sourceBlock);
			}

			// Create connection deletion commands
			deleteConnectionsCommands = new ArrayList<>(0);
			deleteConnectionsCommands();
			
			// Create connections with swapped function
			createConnectionsCommands = new ArrayList<>(0);
			
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private void deleteConnectionsCommands() {
		// Delete source connections : deleteConnectionCommand
		for (Object element : blockEditPart.getSourceConnections()) {
			ConnectionEditPart connectionEditPart = (ConnectionEditPart)element;
			BlocksConnection connection = (BlocksConnection)connectionEditPart.getModel();
			deleteConnectionsCommands.add(new DeleteConnectionCommand(scriptSegment, connection));
		}
		// Delete target connections : deleteConnectionCommand
		for (Object element : blockEditPart.getTargetConnections()) {
			ConnectionEditPart connectionEditPart = (ConnectionEditPart)element;
			BlocksConnection connection = (BlocksConnection)connectionEditPart.getModel();
			deleteConnectionsCommands.add(new DeleteConnectionCommand(scriptSegment, connection));
		}
	}
	
	private void createConnectionsCommands() {
		for (Block sourceBlock : sourceBlocks) {
			CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, sourceBlock);
			createConnectionCommand.setTarget(blockEditPart.getModel());
			createConnectionsCommands.add(createConnectionCommand);
		}
		// Target blocks
		for (Block targetBlock : targetBlocks) {
			CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, blockEditPart.getModel());
			createConnectionCommand.setTarget(targetBlock);
			createConnectionsCommands.add(createConnectionCommand);
		}
	}
	
	private void doChainExecute(ArrayList<Command> commands) {
		// Chain and execute commands
		if(commands.size() > 0) {
			Command command = commands.get(0);
			for (int i = 1; i < commands.size(); i++) {
				command = command.chain(commands.get(i));
			}
			commandStack.execute(command);
		}
	}
	
	private void swappFunctions(Function function1, Function function2) {
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) script.removeInitializeBlock(function1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) script.removeLoopBlock(function1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) script.removeFinalizeBlock(function1);
//		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.DATA_PROCESSING)) script.removeLoopBlock(function1);
		
		blockEditPart.setModel(function2);
		function1.setScript(null);
		function2.setScript(script);
		
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) script.addInitializeBlock(function2);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) script.addLoopBlock(function2);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) script.addFinalizeBlock(function2);
//		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.DATA_PROCESSING)) script.addLoopBlock(function2);
		if(function2 != null) script.selectBlocks(new Block[] {function2});
	}
	
	@Override
	public void execute() {
		activateSegmentProcessEditor();
		doChainExecute(deleteConnectionsCommands);
		swappFunctions(oldFunction, newFunction);
		createConnectionsCommands();
		doChainExecute(createConnectionsCommands);
	}
	
	@Override
	public void redo() {
		activateSegmentProcessEditor();
		commandStack.redo();
		swappFunctions(oldFunction, newFunction);
		commandStack.redo();
	}
	
	@Override
	public void undo() {
		activateSegmentProcessEditor();
		commandStack.undo();
		swappFunctions(newFunction, oldFunction);
		commandStack.undo();
	}
	
	private void activateSegmentProcessEditor() {
		Activator.activateScriptSegmentEditor(scriptSegment);
	}

}
