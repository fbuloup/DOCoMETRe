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
package fr.univamu.ism.docometre.scripteditor.figures;

import java.util.Arrays;
import java.util.List;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.scripteditor.connections.model.BlocksConnection;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.IfBlock;

public class ConnectionFigure extends PolylineConnection {

	private static final Image trueImage = Activator.getImage(IImageKeys.YES_ICON);
	private static final Image falseImage = Activator.getImage(IImageKeys.NO_ICON);
	private static final Image doLoopImage = Activator.getImage(IImageKeys.DO_LOOP_ICON);
	
	private Label trueLabel = new Label("True", trueImage);
	private Label falseLabel = new Label("False", falseImage);
	private Label doLoopLabel = new Label(doLoopImage);
	// Label currently displayed in the middle of the connection, null if none
	private Label currentLabel;
	private BlocksConnection blocksConnection;
	// Routers are created once : creating them at each update would recompute paths each time
	private CurvedConnectionRouter shortestPathCurvedRouter;
	private CurvedConnectionRouter bendpointCurvedRouter;
	private Point doMiddlePoint;
	// Horizontal distance between do loop return and blocks
	private static final int LOOP_OFFSET = 30;
	
	public ConnectionFigure(IFigure blocksLayer, BlocksConnection blocksConnection) {
		super();
		this.blocksConnection = blocksConnection;
		ShortestPathConnectionRouter shortestPathConnectionRouter = new ShortestPathConnectionRouter(blocksLayer);
		// Give more room around blocks so that curves do not overlap them
		shortestPathConnectionRouter.setSpacing(15);
		shortestPathCurvedRouter = new CurvedConnectionRouter(shortestPathConnectionRouter);
		bendpointCurvedRouter = new CurvedConnectionRouter(new BendpointConnectionRouter());
		setLayoutManager(new DelegatingLayout());
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		decoration.setBackgroundColor(ColorConstants.darkGray);
		setTargetDecoration(decoration);
	}
	
	@Override
	protected void outlineShape(Graphics graphics) {
		graphics.setAntialias(SWT.ON);
		List<double[]> bezierSegments = null;
		if(getConnectionRouter() instanceof CurvedConnectionRouter) bezierSegments = ((CurvedConnectionRouter)getConnectionRouter()).getBezierSegments(this);
		if(bezierSegments == null || bezierSegments.isEmpty()) {
			super.outlineShape(graphics);
			return;
		}
		// Draw exact Bézier curves with floating point coordinates
		Path path = new Path(Display.getCurrent());
		try {
			double[] firstSegment = bezierSegments.get(0);
			path.moveTo((float)firstSegment[0], (float)firstSegment[1]);
			for (double[] s : bezierSegments) path.cubicTo((float)s[2], (float)s[3], (float)s[4], (float)s[5], (float)s[6], (float)s[7]);
			graphics.drawPath(path);
		} catch (RuntimeException e) {
			// Path not supported by this graphics (e.g. no advanced graphics) : draw sampled polyline
			super.outlineShape(graphics);
		} finally {
			path.dispose();
		}
	}
	
	/*
	 * Router and label depend on blocks positions, if branches and do loop end. 
	 * They are updated each time the connection is validated : when one of its blocks moves 
	 * or when a command has been executed, undone or redone (see BlocksConnectionEditPart).
	 */
	@Override
	public void layout() {
		updateConnection();
		super.layout();
	}
	
	/*
	 * Must only change router, routing constraint and label when needed : 
	 * any change revalidates the connection and calls layout() again
	 */
	private void updateConnection() {
		if(blocksConnection.sourceBlock instanceof IfBlock) {
			setIfConnection();
		} else if(blocksConnection.targetBlock instanceof DoBlock) {
			DoBlock doBlock = (DoBlock) blocksConnection.targetBlock;
			Block block = blocksConnection.sourceBlock;
			if(block.isLastBlockOf(doBlock)) {
				setDoConnection();
			} else {
				setSimpleConnection();
				setLabel(null);
			}
		} else {
			setSimpleConnection();
			setLabel(null);
		}
	}
	
	private void setLabel(Label label) {
		if(label == currentLabel) return;
		if(currentLabel != null) remove(currentLabel);
		currentLabel = label;
		if(label != null) add(label, new ConnectionLocator(this, ConnectionLocator.MIDDLE));
	}

	private void setAnchorsSide(int side) {
		if(getSourceAnchor() instanceof PerpendicularAnchor) ((PerpendicularAnchor)getSourceAnchor()).setForcedSide(side);
		if(getTargetAnchor() instanceof PerpendicularAnchor) ((PerpendicularAnchor)getTargetAnchor()).setForcedSide(side);
	}

	private void setSimpleConnection() {
		doMiddlePoint = null;
		setAnchorsSide(PerpendicularAnchor.AUTOMATIC);
		setConnectionRouter(shortestPathCurvedRouter);
	}
	
	private void setIfConnection() {
		setSimpleConnection();
		IfBlock sourceIfBlock = (IfBlock)blocksConnection.sourceBlock;
		if(sourceIfBlock.getNextTrueBranchBlock() == blocksConnection.targetBlock) {
			// Decorate with true icon
			setLabel(trueLabel);
		} else if(sourceIfBlock.getNextFalseBranchBlock() == blocksConnection.targetBlock) {
			// Decorate with false icon
			setLabel(falseLabel);
		} else setLabel(null);
	}
	
	private void setDoConnection() {
		Block source = blocksConnection.sourceBlock;
		Block target = blocksConnection.targetBlock;
		// Loop return leaves last block and reaches do block on the same side (side of the do block, left if aligned), 
		// going around blocks so that it does not overlap the forward connections
		boolean onLeft = target.getX() <= source.getX();
		int x = onLeft ? Math.min(source.getX(), target.getX()) - LOOP_OFFSET : Math.max(source.getX() + source.getWidth(), target.getX() + target.getWidth()) + LOOP_OFFSET;
		int y0 = source.getY() + source.getHeight()/2;
		int y1 = target.getY() + target.getHeight()/2;
		Point middlePoint = new Point(x, (y0+y1)/2);
		setAnchorsSide(onLeft ? PerpendicularAnchor.LEFT : PerpendicularAnchor.RIGHT);
		setConnectionRouter(bendpointCurvedRouter);
		if(!middlePoint.equals(doMiddlePoint)) {
			doMiddlePoint = middlePoint;
			AbsoluteBendpoint absoluteBendpoint = new AbsoluteBendpoint(middlePoint);
			setRoutingConstraint(Arrays.asList(new Bendpoint[] {absoluteBendpoint}));
		}
		// Decorate with loop icon
		setLabel(doLoopLabel);
	}

}
