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
package fr.univamu.ism.docometre.scripteditor.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;

import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.BlocksListener;
import fr.univamu.ism.process.Comment;
import fr.univamu.ism.process.ConditionalBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.Operator;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.SizeAndLocationListener;
import fr.univamu.ism.docometre.scripteditor.figures.BlockFigure;
import fr.univamu.ism.docometre.scripteditor.actions.EditBlockAction;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.AssignFunctionAction;
import fr.univamu.ism.docometre.scripteditor.commands.AssignFunctionBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.CreateConnectionCommand;
import fr.univamu.ism.docometre.scripteditor.commands.DeleteBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.DeleteConnectionCommand;
import fr.univamu.ism.docometre.scripteditor.commands.ModifyCommentBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.ModifyConditionalBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.ModifyFunctionalBlockCommand;
import fr.univamu.ism.docometre.scripteditor.connections.model.BlocksConnection;

/*
 * Edit part associated to blocks 
 */
public class BlockEditPart extends AbstractGraphicalEditPart implements SizeAndLocationListener, BlocksListener, NodeEditPart {
	
	private BlockFigure figure;
	
	public BlockEditPart(Block block) {
		setModel(block);
	}
	
	@Override
	public Block getModel() {
		return (Block) super.getModel();
	}
	
	@Override
	protected IFigure createFigure() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput) ((DefaultEditDomain) getRoot().getViewer().getEditDomain()).getEditorPart().getEditorInput();
		Process process = (Process) resourceEditorInput.getObject();
		figure = new BlockFigure(getModel().getName(process), this) ;
		return figure ;
	}
	
	public void updateFigureLabel() {
		figure.updateLabel();
	}
	
	/*
	 * Return all connections starting from this block
	 */
	@Override
	protected List<BlocksConnection> getModelSourceConnections() {
		List<BlocksConnection> blocksConnections = new ArrayList<BlocksConnection>(0);
		Block block = getModel();
		Block[] nextBlocks = block.getNextBlocks();
		for (int i = 0; i < nextBlocks.length; i++) 
			if(nextBlocks[i] != null) blocksConnections.add(new BlocksConnection(block, nextBlocks[i]));
		return blocksConnections;
	}
	
	/*
	 * Return all connections ending at this block
	 */
	@Override
	protected List<BlocksConnection> getModelTargetConnections() {
		List<BlocksConnection> blocksConnections = new ArrayList<BlocksConnection>(0);
		Block block = getModel();
		List<Block> previousBlocks = block.getPreviousBlocks();
		for (int i = 0; i < previousBlocks.size(); i++) 
			blocksConnections.add(new BlocksConnection(previousBlocks.get(i), block));
		return blocksConnections;
	}
	
	@Override
	protected void refreshVisuals() {
		Block block = getModel();
		((GraphicalEditPart)getParent()).setLayoutConstraint(this, getFigure(), block.getSizeAndLocation());
//		super.refreshVisuals();
	}
	
	/*
	 * Activate part for resize and location changes
	 */
	@Override
	public void activate() {
		getModel().addSizeAndLocationListener(this);
		getModel().getScript().addBlocksListener(this);
		super.activate();
	}
	
	/*
	 * Deactivate part for resize and location changes
	 */
	@Override
	public void deactivate() {
		getModel().removeSizeAndLocationListener(this);
		getModel().getScript().removeBlocksListener(this);
		super.deactivate();
	}
	
	/*
	 * Install all policies related to :
	 * - delete block
	 * - change connection
	 * - create connection
	 */
	@Override
	protected void createEditPolicies() {
		// Block deletion
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
			@Override
			protected Command createDeleteCommand(GroupRequest deleteRequest) {
				Block block = getModel();
				Block[] nextBlocks =  block.getNextBlocks();
				ArrayList<BlocksConnectionEditPart> blocksConnectionsEditParts = new ArrayList<BlocksConnectionEditPart>(0);
				for (int i = 0; i < nextBlocks.length; i++) {
					if(nextBlocks[i] != null) {
						BlocksConnection blocksConnection = new BlocksConnection(block, nextBlocks[i]);
						BlocksConnectionEditPart blocksConnectionEditPart = (BlocksConnectionEditPart) getViewer().getEditPartRegistry().get(blocksConnection);
						blocksConnectionsEditParts.add(blocksConnectionEditPart);
					}
				}
				List<Block> previousBlocks = block.getPreviousBlocks();
				for (int i = 0; i < previousBlocks.size(); i++) {
					BlocksConnection blocksConnection = new BlocksConnection(previousBlocks.get(i), block);
					BlocksConnectionEditPart blocksConnectionEditPart = (BlocksConnectionEditPart) getViewer().getEditPartRegistry().get(blocksConnection);
					blocksConnectionsEditParts.add(blocksConnectionEditPart);
				}
				Command command = null;
				ScriptSegment scriptSegment = (ScriptSegment) getParent().getModel();
				if(blocksConnectionsEditParts.size() > 0) command = new DeleteConnectionCommand(scriptSegment, blocksConnectionsEditParts.get(0).getModel());
				for (int i = 1; i < blocksConnectionsEditParts.size(); i++) command = command.chain(new DeleteConnectionCommand(scriptSegment, blocksConnectionsEditParts.get(i).getModel()));
				if(command != null) command = command.chain(new DeleteBlockCommand(scriptSegment, block));
				else command = new DeleteBlockCommand(scriptSegment, block); 
				return command;
			}
		});
		// Connection creation and modification
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new GraphicalNodeEditPolicy() {
			@Override
			protected Command getReconnectTargetCommand(ReconnectRequest request) {
				if(request.getConnectionEditPart() instanceof BlocksConnectionEditPart) {
					// Get new target block from which come reconnect target
					Block newTargetBlock = BlockEditPart.this.getModel();
					BlocksConnection connection = ((BlocksConnectionEditPart) request.getConnectionEditPart()).getModel();
					// Get current target block
					Block targetBlock = connection.targetBlock;
					if(newTargetBlock != targetBlock) {
						ScriptSegment scriptSegment = (ScriptSegment) getParent().getModel();
						CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, connection.sourceBlock);
						if(createConnectionCommand.isValidTarget(newTargetBlock)) {
							createConnectionCommand.setTarget(newTargetBlock);
							DeleteConnectionCommand deleteConnectionCommand = new DeleteConnectionCommand(scriptSegment, connection);
							Command modifyCommand = deleteConnectionCommand.chain(createConnectionCommand);
							return modifyCommand;
						}
					}
				}
				return null;
			}
			@Override
			protected Command getReconnectSourceCommand(ReconnectRequest request) {
				if(request.getConnectionEditPart() instanceof BlocksConnectionEditPart) {
					// Get current block from which come reconnect source
					Block newSourceBlock = BlockEditPart.this.getModel();
					BlocksConnection connection = ((BlocksConnectionEditPart) request.getConnectionEditPart()).getModel();
					Block sourceBlock = connection.sourceBlock;
					if(newSourceBlock != sourceBlock) {
						ScriptSegment scriptSegment = (ScriptSegment) getParent().getModel();
						CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, newSourceBlock);
						if(createConnectionCommand.isValidTarget(connection.targetBlock)) {
							createConnectionCommand.setTarget(connection.targetBlock);
							DeleteConnectionCommand deleteConnectionCommand = new DeleteConnectionCommand(scriptSegment, connection);
							Command modifyCommand = deleteConnectionCommand.chain(createConnectionCommand);
							return modifyCommand;
						}
					}
				}
				return null;
			}
			@Override
			protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
				ScriptSegment scriptSegment = (ScriptSegment) getParent().getModel();
				if(!(getModel() instanceof Comment)) {
					CreateConnectionCommand createConnectionCommand = new CreateConnectionCommand(scriptSegment, getModel());
					request.setStartCommand(createConnectionCommand);
					return createConnectionCommand;
				}
				return null;
			}
			@Override
			protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
				CreateConnectionCommand createConnectionCommand = (CreateConnectionCommand) request.getStartCommand();
				if(createConnectionCommand.isValidTarget(getModel())) {
					createConnectionCommand.setTarget(getModel());
//					updateBlocks();
					return createConnectionCommand;
				}
				return null;
			}
		});
		installEditPolicy("Snap Feedback", new SnapFeedbackPolicy());
	}
	
	/*
	 * Used to display connections 
	 */
	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		if(!(connection instanceof BlocksConnectionEditPart)) return null;
//		BlocksConnectionEditPart blocksConnectionEditPart = (BlocksConnectionEditPart) connection;
//		Block sourceBlock = blocksConnectionEditPart.getModel().sourceBlock;
//		Block targetBlock = blocksConnectionEditPart.getModel().targetBlock;
//		if(targetBlock instanceof DoBlock) {
//			if(((DoBlock)targetBlock).getEndBlock() == sourceBlock) {
//				if(blocksConnectionEditPart.getSource() != null)
//				return new TopLeftAnchor(((GraphicalEditPart)blocksConnectionEditPart.getSource()).getFigure());
//			}
//		}
		return new ChopboxAnchor(getFigure());
	}

	/*
	 * Used to display connections 
	 */
	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		if(!(connection instanceof BlocksConnectionEditPart)) return null;
//		BlocksConnectionEditPart blocksConnectionEditPart = (BlocksConnectionEditPart) connection;
//		Block sourceBlock = blocksConnectionEditPart.getModel().sourceBlock;
//		Block targetBlock = blocksConnectionEditPart.getModel().targetBlock;
//		if(targetBlock instanceof DoBlock) {
//			if(((DoBlock)targetBlock).getEndBlock() == sourceBlock) {
//				if(blocksConnectionEditPart.getTarget() != null)
//				return new BottomLeftAnchor(((GraphicalEditPart)blocksConnectionEditPart.getTarget()).getFigure());
//			}
//		}
		return new ChopboxAnchor(getFigure());
	}

	/*
	 * Used to display connections creation
	 */
	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if(request instanceof CreateConnectionRequest) return new ChopboxAnchor(getFigure());
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		if(request instanceof CreateConnectionRequest) {
			CreateConnectionRequest createConnectionRequest = (CreateConnectionRequest) request;
			Command command = createConnectionRequest.getStartCommand();
			if(command instanceof CreateConnectionCommand) {
				CreateConnectionCommand createConnectionCommand = (CreateConnectionCommand) command;
				if(createConnectionCommand.isValidTarget(getModel())) return new ChopboxAnchor(getFigure());
			}
		}
		return null;
	}

	@Override
	public void updateSizeAndLocation() {
		refreshVisuals();
	}

	/**
	 * Must be called when connections are deleted (this includes when a bloc is deleted as 
	 * its connections are deleted with it) 
	 */
	@Override
	public void updateBlocks() {
		refresh();
	}
	
	@Override
	public Command getCommand(Request request) {
		if(request.getType().equals(EditBlockAction.REQ_EDIT_CONDITIONAL_BLOCK)) {
			String leftOperand = (String) request.getExtendedData().get(ConditionalBlock.LEFT_OPERAND);
			Operator operator = (Operator) request.getExtendedData().get(ConditionalBlock.OPERATOR);
			String rightOperand = (String) request.getExtendedData().get(ConditionalBlock.RIGHT_OPERAND);
			return new ModifyConditionalBlockCommand(this, leftOperand, operator, rightOperand);
		}
		if(request.getType().equals(EditBlockAction.REQ_EDIT_FUNCTIONAL_BLOCK)) {
			return new ModifyFunctionalBlockCommand(this);
		}
		if(request.getType().equals(EditBlockAction.REQ_EDIT_COMMENT_BLOCK)) {
			String comment = (String) request.getExtendedData().get(Comment.COMMENT);
			return new ModifyCommentBlockCommand(this, comment);
		}
		if(request.getType().equals(AssignFunctionAction.REQ_CHANGE_FUNCTIONAL_BLOCK)) {
			String functionClassName = (String) request.getExtendedData().get(Function.FUNCTION_CLASS_NAME);
			String name = (String) request.getExtendedData().get(Function.FUNCTION_NAME);
			return new AssignFunctionBlockCommand(this, functionClassName, name);
		}
		return super.getCommand(request);
	}

	@Override
	public void selectBlocks(Block[] blocks) {
		// Nothing TODO !
	}
	
}
