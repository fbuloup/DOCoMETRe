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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;

/**
 * Router that smooths the path computed by a delegate router 
 * (ShortestPathConnectionRouter, BendpointConnectionRouter...).
 * The delegate points are used as control points :
 * - two points (no obstacle) give a cubic Bézier "S" curve whose tangents 
 *   follow the main direction of the connection,
 * - more points give a Catmull-Rom spline passing through each of them, 
 *   converted to cubic Bézier segments.
 * When an end uses a PerpendicularAnchor, the curve leaves or reaches the 
 * figure perpendicular to its side.
 * Bézier segments are available with getBezierSegments() so that the 
 * connection can draw them exactly (see ConnectionFigure.outlineShape()).
 * The curve is also sampled into the connection polyline so that hit testing, 
 * arrow decoration orientation and labels locators keep working unchanged.
 */
public class CurvedConnectionRouter extends AbstractRouter {
	
	// Approximative length in pixels of each sampled segment
	private static final double SAMPLING_STEP = 12;
	// Tangents length ratio for two points connections and perpendicular ends
	private static final double TANGENT_RATIO = 0.5;
	// Minimum tangent length in pixels for perpendicular ends
	private static final double MINIMUM_TANGENT_LENGTH = 20;
	
	private class Curve {
		// Each segment is {x0, y0, cx1, cy1, cx2, cy2, x3, y3}
		private List<double[]> bezierSegments = new ArrayList<>();
		private PointList points = new PointList();
	}
	
	private ConnectionRouter delegate;
	// Last computed curve for each connection, to avoid smoothing an already smoothed path
	private Map<Connection, Curve> curves = new HashMap<>();
	
	public CurvedConnectionRouter(ConnectionRouter delegate) {
		this.delegate = delegate;
	}
	
	public ConnectionRouter getDelegate() {
		return delegate;
	}
	
	/**
	 * Get cubic Bézier segments of the connection. Each segment is 
	 * {x0, y0, cx1, cy1, cx2, cy2, x3, y3}.
	 * @return the segments or null if connection points have not been computed by this router 
	 */
	public List<double[]> getBezierSegments(Connection connection) {
		Curve curve = curves.get(connection);
		if(curve == null || !Arrays.equals(curve.points.toIntArray(), connection.getPoints().toIntArray())) return null;
		return curve.bezierSegments;
	}

	@Override
	public void route(Connection connection) {
		delegate.route(connection);
		PointList points = connection.getPoints();
		Curve curve = curves.get(connection);
		// Delegate did not change the path (e.g. ShortestPathConnectionRouter not dirty) : nothing to do
		if(curve != null && Arrays.equals(curve.points.toIntArray(), points.toIntArray())) return;
		if(points.size() < 2) return;
		curve = computeCurve(connection, points.getCopy());
		curves.put(connection, curve);
		connection.setPoints(curve.points.getCopy());
	}
	
	@Override
	public Object getConstraint(Connection connection) {
		return delegate.getConstraint(connection);
	}
	
	@Override
	public void setConstraint(Connection connection, Object constraint) {
		delegate.setConstraint(connection, constraint);
	}
	
	@Override
	public void invalidate(Connection connection) {
		// Keep last curve : delegate may not recompute path on next route call
		delegate.invalidate(connection);
	}
	
	@Override
	public void remove(Connection connection) {
		curves.remove(connection);
		delegate.remove(connection);
	}
	
	private Curve computeCurve(Connection connection, PointList points) {
		Curve curve = new Curve();
		curve.points.addPoint(points.getFirstPoint());
		int n = points.size();
		Point first = points.getFirstPoint();
		Point last = points.getLastPoint();
		// Unit tangents at both ends, pointing outward from source and target figures, null if anchor is not perpendicular
		double[] sourceTangent = getPerpendicularTangent(connection, connection.getSourceAnchor(), first);
		double[] targetTangent = getPerpendicularTangent(connection, connection.getTargetAnchor(), last);
		if(n == 2) {
			double dx = last.x - first.x;
			double dy = last.y - first.y;
			// Without perpendicular anchor, tangents follow the main direction : vertical for top-bottom flow, horizontal otherwise
			if(sourceTangent == null) sourceTangent = Math.abs(dy) >= Math.abs(dx) ? new double[] {0, Math.signum(dy)} : new double[] {Math.signum(dx), 0};
			if(targetTangent == null) targetTangent = new double[] {-sourceTangent[0], -sourceTangent[1]};
			double length = TANGENT_RATIO*Math.max(Math.abs(dx), Math.abs(dy));
			double sourceLength = getTangentLength(connection.getSourceAnchor(), length);
			double targetLength = getTangentLength(connection.getTargetAnchor(), length);
			addBezier(curve, new double[] {first.x, first.y, 
										   first.x + sourceLength*sourceTangent[0], first.y + sourceLength*sourceTangent[1], 
										   last.x + targetLength*targetTangent[0], last.y + targetLength*targetTangent[1], 
										   last.x, last.y});
		} else {
			// Catmull-Rom spline converted to cubic Bézier segments
			for (int i = 0; i < n - 1; i++) {
				Point pPrevious = points.getPoint(Math.max(i - 1, 0));
				Point p1 = points.getPoint(i);
				Point p2 = points.getPoint(i + 1);
				Point pNext = points.getPoint(Math.min(i + 2, n - 1));
				double[] segment = {p1.x, p1.y, 
								    p1.x + (p2.x - pPrevious.x)/6d, p1.y + (p2.y - pPrevious.y)/6d, 
								    p2.x - (pNext.x - p1.x)/6d, p2.y - (pNext.y - p1.y)/6d, 
								    p2.x, p2.y};
				double length = TANGENT_RATIO*p1.getDistance(p2);
				if(i == 0 && sourceTangent != null) {
					length = getTangentLength(connection.getSourceAnchor(), length);
					segment[2] = p1.x + length*sourceTangent[0];
					segment[3] = p1.y + length*sourceTangent[1];
				}
				if(i == n - 2 && targetTangent != null) {
					length = getTangentLength(connection.getTargetAnchor(), length);
					segment[4] = p2.x + length*targetTangent[0];
					segment[5] = p2.y + length*targetTangent[1];
				}
				addBezier(curve, segment);
			}
		}
		return curve;
	}
	
	/*
	 * Outward normal of the figure side at location if anchor is a PerpendicularAnchor, null otherwise 
	 */
	private static double[] getPerpendicularTangent(Connection connection, ConnectionAnchor anchor, Point location) {
		if(!(anchor instanceof PerpendicularAnchor)) return null;
		Point absoluteLocation = location.getCopy();
		connection.translateToAbsolute(absoluteLocation);
		int[] normal = ((PerpendicularAnchor)anchor).getNormal(absoluteLocation);
		return new double[] {normal[0], normal[1]};
	}
	
	private static double getTangentLength(ConnectionAnchor anchor, double length) {
		if(anchor instanceof PerpendicularAnchor) return Math.max(MINIMUM_TANGENT_LENGTH, length);
		return length;
	}
	
	/*
	 * Add cubic Bézier segment {x0, y0, cx1, cy1, cx2, cy2, x3, y3} to curve 
	 * and append its sampled points, except first one, to curve points 
	 */
	private static void addBezier(Curve curve, double[] s) {
		curve.bezierSegments.add(s);
		double length = Math.hypot(s[2] - s[0], s[3] - s[1]) + Math.hypot(s[4] - s[2], s[5] - s[3]) + Math.hypot(s[6] - s[4], s[7] - s[5]);
		int nbSteps = Math.max(2, (int) Math.ceil(length / SAMPLING_STEP));
		for (int i = 1; i <= nbSteps; i++) {
			double t = (double) i / nbSteps;
			double u = 1 - t;
			double a = u*u*u, b = 3*u*u*t, c = 3*u*t*t, d = t*t*t;
			int x = (int) Math.round(a*s[0] + b*s[2] + c*s[4] + d*s[6]);
			int y = (int) Math.round(a*s[1] + b*s[3] + c*s[5] + d*s[7]);
			Point last = curve.points.getLastPoint();
			if(i < nbSteps && last.x == x && last.y == y) continue;
			curve.points.addPoint(x, y);
		}
	}

}
