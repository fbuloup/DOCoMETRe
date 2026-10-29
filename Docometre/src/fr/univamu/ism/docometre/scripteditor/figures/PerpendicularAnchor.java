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

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Anchor with two connectors on each side of its owner figure, at one third
 * and two thirds of the side. The side facing the reference point is chosen,
 * then the connector of this side which is the closest to the reference.
 * Connections using this anchor are drawn perpendicular to the side by
 * CurvedConnectionRouter (see getNormal()).
 */
public class PerpendicularAnchor extends AbstractConnectionAnchor {

	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int AUTOMATIC = -1;

	// Outward unit normals of TOP, BOTTOM, LEFT and RIGHT sides
	private static final int[][] NORMALS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

	private int forcedSide = AUTOMATIC;
	
	public PerpendicularAnchor(IFigure owner) {
		super(owner);
	}
	
	/**
	 * Force the side used for location
	 * @param side TOP, BOTTOM, LEFT, RIGHT or AUTOMATIC to use the side facing the reference point
	 */
	public void setForcedSide(int side) {
		forcedSide = side;
	}

	/*
	 * Owner bounds in absolute coordinates, right and bottom sides included
	 */
	private Rectangle getAbsoluteBounds() {
		Rectangle bounds = getOwner().getBounds().getCopy();
		bounds.resize(-1, -1);
		getOwner().translateToAbsolute(bounds);
		return bounds;
	}

	@Override
	public Point getReferencePoint() {
		return getAbsoluteBounds().getCenter();
	}

	@Override
	public Point getLocation(Point reference) {
		Rectangle bounds = getAbsoluteBounds();
		int side = forcedSide == AUTOMATIC ? getSide(bounds, reference) : forcedSide;
		Point[] connectors = getConnectors(bounds, side);
		double distance0 = connectors[0].getDistance(reference);
		double distance1 = connectors[1].getDistance(reference);
		// When both connectors are at the same distance, take the second one :
		// a forward connection and its do loop return then use different connectors
		return distance0 < distance1 - 1 ? connectors[0] : connectors[1];
	}

	/**
	 * Get outward unit normal of the side on which location lies
	 * @param location an anchor location, in absolute coordinates
	 * @return {nx, ny}
	 */
	public int[] getNormal(Point location) {
		Rectangle bounds = getAbsoluteBounds();
		int[] distances = {Math.abs(location.y - bounds.y), Math.abs(location.y - bounds.bottom()),
						   Math.abs(location.x - bounds.x), Math.abs(location.x - bounds.right())};
		int side = TOP;
		for (int i = 1; i < distances.length; i++) if(distances[i] < distances[side]) side = i;
		return NORMALS[side];
	}

	/*
	 * Side facing reference point, taking figure proportions into account
	 */
	private static int getSide(Rectangle bounds, Point reference) {
		Point center = bounds.getCenter();
		double dx = (reference.x - center.x) / (double) Math.max(1, bounds.width);
		double dy = (reference.y - center.y) / (double) Math.max(1, bounds.height);
		if(Math.abs(dx) > Math.abs(dy)) return dx > 0 ? RIGHT : LEFT;
		return dy > 0 ? BOTTOM : TOP;
	}

	private static Point[] getConnectors(Rectangle bounds, int side) {
		int x1 = bounds.x + bounds.width/3;
		int x2 = bounds.x + 2*bounds.width/3;
		int y1 = bounds.y + bounds.height/3;
		int y2 = bounds.y + 2*bounds.height/3;
		switch (side) {
		case TOP:
			return new Point[] {new Point(x1, bounds.y), new Point(x2, bounds.y)};
		case BOTTOM:
			return new Point[] {new Point(x1, bounds.bottom()), new Point(x2, bounds.bottom())};
		case LEFT:
			return new Point[] {new Point(bounds.x, y1), new Point(bounds.x, y2)};
		default:
			return new Point[] {new Point(bounds.right(), y1), new Point(bounds.right(), y2)};
		}
	}

}
