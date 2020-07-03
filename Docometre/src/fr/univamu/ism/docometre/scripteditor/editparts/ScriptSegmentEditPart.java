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

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.jface.viewers.StructuredSelection;

import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.BlocksListener;
import fr.univamu.ism.process.ScriptSegmentType;
import fr.univamu.ism.docometre.scripteditor.commands.CreateBlockCommand;
import fr.univamu.ism.docometre.scripteditor.commands.MoveAndResizeBlocCommand;

public class ScriptSegmentEditPart extends AbstractGraphicalEditPart implements BlocksListener {
	
	private IFigure blocksLayer;
	
	private ScriptSegment scriptSegment;

	public ScriptSegmentEditPart(ScriptSegment scriptSegment) {
		this.scriptSegment = scriptSegment;
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) setModel(scriptSegment.getScript().getInitializeBlocksContainer());
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) setModel(scriptSegment.getScript().getLoopBlocksContainer());
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) setModel(scriptSegment.getScript().getFinalizeBlocksContainer());
//		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.DATA_PROCESSING)) setModel(scriptSegment.getScript().getLoopBlocksContainer());
	}
	
	@Override
	public ScriptSegment getModel() {
		return (ScriptSegment) super.getModel();
	}
	
	@Override
	protected IFigure createFigure() {
		blocksLayer = new FreeformLayer();
		blocksLayer.setLayoutManager(new FreeformLayout());
		return blocksLayer;
	}
	
	public IFigure getBlocksLayer() {
		return blocksLayer;
	}
	
	@Override
	protected List<Block> getModelChildren() {
		ScriptSegment blocksContainer =  getModel();
		List<Block> blocks = blocksContainer.getBlocks();
		return blocks;
	}
	
	@Override
	public void activate() {
		getModel().getScript().addBlocksListener(this);
		super.activate();
	}
	
	@Override
	public void deactivate() {
		getModel().getScript().removeBlocksListener(this);
		super.deactivate();
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new XYLayoutEditPolicy() {
			@Override
			protected Command createChangeConstraintCommand(ChangeBoundsRequest request, EditPart child, Object constraint) {
				return new MoveAndResizeBlocCommand( scriptSegment, (Block) child.getModel(), (Rectangle)constraint);
			}
			@Override
			protected Command getCreateCommand(CreateRequest request) {
				if(ScriptSegmentEditPart.this.getViewer().findObjectAt(request.getLocation()) instanceof ScalableFreeformRootEditPart) {
					Object object = request.getNewObject();
					if(object instanceof Block) {
						Block block = (Block) object;
						request.setSize(new Dimension(block.getWidth(), block.getHeight()));
						Rectangle rectangle = (Rectangle) getConstraintFor(request);
						
						return new CreateBlockCommand(scriptSegment, block, rectangle);
					}
				}
				return null;
			}
		});
		installEditPolicy("Snap Feedback", new SnapFeedbackPolicy());
		
	}

	/**
	 * Graph must be updated when a bloc is deleted. 
	 */
	@Override
	public void updateBlocks() {
		refresh();
	}

	@Override
	public void selectBlocks(Block[] blocks) {
		// From model to edit parts
		ArrayList<Object> editParts = new ArrayList<>();
		for (Block block : blocks) {
			Object editPart = getViewer().getEditPartRegistry().get(block);
			if(editPart != null) editParts.add(editPart);
		}
		if(editParts.size() > 0)
			getViewer().setSelection(new StructuredSelection(editParts));
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
		if (key == SnapToHelper.class) {
	        List<SnapToHelper> helpers = new ArrayList<SnapToHelper>();
	        if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED))) {
	            helpers.add(new SnapToGeometry(this));
	        }
	        if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED))) {
	            helpers.add(new SnapToGrid(this));
	        }
	        if(helpers.size()==0) {
	            return null;
	        } else {
	            return new CompoundSnapToHelper(helpers.toArray(new SnapToHelper[0]));
	        }
	    }
		return super.getAdapter(key);
	}
}
