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

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import fr.univamu.ism.docometre.scripteditor.commands.DeleteConnectionCommand;
import fr.univamu.ism.docometre.scripteditor.commands.SwitchEndDoBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.SwitchIfBranchesBlockCommand;
import fr.univamu.ism.docometre.scripteditor.connections.model.BlocksConnection;
import fr.univamu.ism.docometre.scripteditor.figures.ConnectionFigure;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.IfBlock;
import fr.univamu.ism.process.ScriptSegment;

/**
 * Part associated with connection
 * @author frank
 *
 */
public class BlocksConnectionEditPart extends AbstractConnectionEditPart {
	
	private IFigure blocksLayer;
	private ScriptSegment scriptSegment;

	public BlocksConnectionEditPart(IFigure blocksLayer, BlocksConnection blocksConnection, ScriptSegment scriptSegment) {
		setModel(blocksConnection);
		this.blocksLayer = blocksLayer;
		this.scriptSegment = scriptSegment;
	}

	@Override
	public BlocksConnection getModel() {
		return (BlocksConnection) super.getModel();
	}
	
	@Override
	protected IFigure createFigure() {
		return new ConnectionFigure(blocksLayer, getModel());
	}
	
	/**
	 * Install policy related to connection delete
	 */
	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ConnectionEditPolicy() {
			@Override
			protected Command getDeleteCommand(GroupRequest request) {
				return new DeleteConnectionCommand(scriptSegment, getModel());
			}
		});
	}
	
	@Override
	public void performRequest(Request request) {
		if(request.getType() == RequestConstants.REQ_OPEN) {
			BlocksConnection blocksConnection = getModel();
			if(blocksConnection.sourceBlock instanceof IfBlock) {
				CommandStack commandStack = getRoot().getViewer().getEditDomain().getCommandStack();
				IfBlock ifBlock = (IfBlock) blocksConnection.sourceBlock;
				Command command = new SwitchIfBranchesBlockCommand(ifBlock, scriptSegment);
				commandStack.execute(command);
			}
			if(blocksConnection.targetBlock instanceof DoBlock) {
				CommandStack commandStack = getRoot().getViewer().getEditDomain().getCommandStack();
				DoBlock doBlock = (DoBlock) blocksConnection.targetBlock;
				Block block = blocksConnection.sourceBlock;
				Command command = new SwitchEndDoBlockCommand(doBlock, block, scriptSegment);
				commandStack.execute(command);
			}
		}
	}

}
