package org.jzy3d.plot3d.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Utils;
import org.jzy3d.painters.IPainter;
import org.jzy3d.plot3d.primitives.symbols.SymbolHandler;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.transform.Transform;
import org.jzy3d.plot3d.transform.space.SpaceTransformer;

/**
 * Color works as follow:
 * <ul>
 * <li>If wireframe color is null (default), uses each point color and performs
 * color interpolation
 * <li>Otherwise apply a uniform wireframe color.
 * </ul>
 * 
 * 
 * Dotted line are built using
 * 
 * http://www.glprogramming.com/red/images/Image35.gif
 * 
 * @author Martin Pernollet
 */
public class LineStrip extends Wireframeable {
	public LineStrip() {
		this(2);
	}

	public LineStrip(int n) {
		points = new ArrayList<Point>(n);
//		markersIndexes = new HashMap<>();
//		markersLabels = new HashMap<>();
		bbox = new BoundingBox3d();
		// symbolHandler = new MaskImageSymbolHandler(n);
		markersLabels = new ArrayList<>();
		setWireframeColor(null);
	}
	
	public void addMarkerAndLabel(Drawable[] drawables) {
		markersLabels.addAll(Arrays.asList(drawables));
	}
	
	public Drawable[] getMarkersAndLabels() {
		return markersLabels.toArray(new Drawable[markersLabels.size()]);
	}

	public LineStrip(Coord3d... coords) {
		this(Arrays.asList(coords));
	}

	public LineStrip(Color color, Coord3d... coords) {
		this(Arrays.asList(coords));
		setWireframeColor(color);
	}

	public LineStrip(List<Coord3d> coords) {
		this(coords.size());

		addAllPoints(coords);
	}

	public LineStrip(Point c1, Point c2) {
		this();
		add(c1);
		add(c2);
	}

	/** Set the wireframe color. */
	@Override
	public void setWireframeColor(Color color) {
		super.setWireframeColor(color);
		if (color != null && points != null) {
			for (Point p : points) {
				p.setColor(color);
			}
		}
	}

	/**
	 * A convenient shortcut for {@link #setWireframeColor}
	 * 
	 * @param color
	 */
	public void setColor(Color color) {
		setWireframeColor(color);
	}

	/**
	 * A convenient shortcut for {@link #getWireframeColor}
	 */
	public Color getColor() {
		return getWireframeColor();
	}

	/* */

	@Override
	public void draw(IPainter painter) {
		doTransform(painter);

		// Draw a line (or point if there is a single point in this line)
		if (points.size() > 1) {
			drawLine(painter);
		} else if (points.size() == 1 && !showPoints) {
			drawPoints(painter);
		}

		if (showSymbols && symbolHandler != null) {
			symbolHandler.drawSymbols(painter);
		}

		drawPointsIfEnabled(painter);
		
//		if(showMarkers) drawMarkers(painter);
//		else System.out.println("dont draw markers");
	}

	public void drawLine(IPainter painter) {
		// painter.glLineWidth(wfwidth);

		if (stipple) {
			painter.glPolygonMode(PolygonMode.BACK, PolygonFill.LINE);
			painter.glEnable_LineStipple();
			painter.glLineStipple(stippleFactor, stipplePattern);
		}

		painter.glLineWidth(wireframeWidth);
		painter.glBegin_LineStrip();

		if (wireframeColor == null) {
			for (int i = frontCut - baseFrontCut; i < endCut - baseFrontCut && i < points.size(); i++) {
				Point p = points.get(i);
				painter.color(p.rgb);
				painter.vertex(p.xyz, spaceTransformer);
			}
		} else {
			for (int i = frontCut - baseFrontCut; i < endCut - baseFrontCut && i < points.size(); i++) {
				Point p = points.get(i);
				painter.color(wireframeColor);
				painter.vertex(p.xyz, spaceTransformer);
			}
		}
		painter.glEnd();

		if (stipple) {
			painter.glDisable_LineStipple();
		}
	}

	public void drawPointsIfEnabled(IPainter painter) {
		if (showPoints) {
			drawPoints(painter);
		}
	}

	public void drawPoints(IPainter painter) {
		painter.glBegin_Point();
		painter.glPointSize(wireframeWidth);

		for (int i = frontCut - baseFrontCut; i < endCut - baseFrontCut; i++) {
			Point p = points.get(i);
			if (wireframeColor == null) painter.color(p.rgb);
			else painter.color(wireframeColor);
			painter.vertex(p.xyz, spaceTransformer);
		}

		painter.glEnd();
	}
	
//	public void drawMarkers(IPainter painter) {
//		
////		painter.glBegin_Polygon();
////		painter.glPointSize(markerSize);
//		
//		Set<Sphere> markers = markersIndexes.keySet();
//		for (Sphere marker : markers) {
//			System.out.println(marker.toString());
//			int index = markersIndexes.get(marker);
//			if(index >= frontCut && index < endCut) {
////				if (wireframeColor == null) painter.color(marker.getColor());
////				else painter.color(wireframeColor);
////				painter.vertex(marker.getPosition(), spaceTransformer);
//				marker.setVolume(1f);
//				marker.setColor(new Color(255, 255, 255));
//				marker.setBoundingBoxDisplayed(true);
//				marker.setBoundingBoxColor(getColor());
//				marker.draw(painter);
//				
//				System.out.println("draw markers");
//				if(showMarkersLabels) {
//					// ...
//				}
//			}
//		}
//
////		painter.glEnd();
//	}

	/* */

	@Override
	public void applyGeometryTransform(Transform transform) {
		for (Point p : points) {
			p.xyz = transform.compute(p.xyz);
		}
		updateBounds();
	}

	@Override
	public void updateBounds() {
		bbox.reset();
		for (Point p : points)
			bbox.add(p);
	}

	public void add(Point point) {
		if (showSymbols) {
			symbolHandler.addSymbolOn(point);
		}

		points.add(point);
		bbox.add(point);
	}

	public void add(Coord3d coord3d) {
		add(new Point(coord3d));
	}

	public void add(List<Coord3d> coords) {
		for (Coord3d c : coords)
			add(c);
	}

	public void addAll(List<Point> points) {
		for (Point p : points)
			add(p);
	}

	public void addAll(LineStrip strip) {
		addAll(strip.getPoints());
	}

	public void addAllPoints(List<Coord3d> coords) {
		for (Coord3d c : coords) {
			Point p = new Point(c);
			add(p);
		}
	}

	public void clear() {
		points.clear();
//		markersIndexes.clear();
//		markersLabels.clear();
		updateBounds();
	}

	public Point get(int p) {
		return points.get(p);
	}

	public Point getLastPoint() {
		int last = points.size() - 1;
		if (last >= 0)
			return points.get(last);
		return null;
	}

	public List<Point> getPoints() {
		return points;
	}

	public int size() {
		return points.size();
	}

	/** A shortcut for {@link #setWireframeWidth} */
	public void setWidth(float width) {
		setWireframeWidth(width);
	}

	/** A shortcut for {@link #getWireframeWidth} */
	public float getWidth() {
		return getWireframeWidth();
	}

	public boolean isShowPoints() {
		return showPoints;
	}

	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}

	public boolean isShowSymbols() {
		return showSymbols;
	}

	public void setShowSymbols(boolean showSymbols) {
		if (!showSymbols) {
			if (symbolHandler != null)
				symbolHandler.clear();
		} else {
			if (symbolHandler != null) {
				symbolHandler.clear();

				for (Point point : getPoints()) {
					symbolHandler.addSymbolOn(point);
				}
			}
		}
		this.showSymbols = showSymbols;
	}

	/**
	 * Indicates if stippled rendering is enabled for this line.
	 * 
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public boolean isStipple() {
		return stipple;
	}

	/**
	 * Enable or disable stippled rendering.
	 * 
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public void setStipple(boolean stipple) {
		this.stipple = stipple;
	}

	/**
	 * Stippled line factor.
	 * 
	 * @see http://www.glprogramming.com/red/images/Image35.gif
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public int getStippleFactor() {
		return stippleFactor;
	}

	/**
	 * Stippled line factor.
	 * 
	 * @see http://www.glprogramming.com/red/images/Image35.gif
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public void setStippleFactor(int stippleFactor) {
		this.stippleFactor = stippleFactor;
	}

	/**
	 * Stippled line pattern.
	 * 
	 * @see http://www.glprogramming.com/red/images/Image35.gif
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public short getStipplePattern() {
		return stipplePattern;
	}

	/**
	 * Stippled line pattern.
	 * 
	 * @see http://www.glprogramming.com/red/images/Image35.gif
	 * @see http://www.glprogramming.com/red/chapter02.html (Stippled line section)
	 */
	public void setStipplePattern(short stipplePattern) {
		this.stipplePattern = stipplePattern;
	}

	@Override
	public double getDistance(Camera camera) {
		return getBarycentre().distance(camera.getEye());
	}

	@Override
	public double getShortestDistance(Camera camera) {
		double min = Float.MAX_VALUE;
		double dist = 0;
		for (Point point : points) {
			dist = point.getDistance(camera);
			if (dist < min)
				min = dist;
		}
		return min;
	}

	@Override
	public double getLongestDistance(Camera camera) {
		double max = 0;
		double dist = 0;
		for (Point point : points) {
			dist = point.getDistance(camera);
			if (dist < max)
				max = dist;
		}
		return max;
	}

	/**
	 * Merge lines by selecting the most relevant connection point: A-B to C-D if
	 * distance BC is shorter than distance DA C-D to A-B
	 */
	public static LineStrip merge(LineStrip strip1, LineStrip strip2) {
		Coord3d a = strip1.get(0).xyz;
		Coord3d b = strip1.get(strip1.size() - 1).xyz;
		Coord3d c = strip2.get(0).xyz;
		Coord3d d = strip2.get(strip2.size() - 1).xyz;

		double bc = b.distance(c);
		double da = d.distance(a);

		if (bc > da) {
			strip1.addAll(strip2);
			return strip1;
		} else {
			strip2.addAll(strip1);
			return strip2;
		}
	}

	public SymbolHandler getSymbolHandler() {
		return symbolHandler;
	}

	public void setSymbolHandler(SymbolHandler symbolHandler) {
		this.symbolHandler = symbolHandler;
	}

	@Override
	public void setSpaceTransformer(SpaceTransformer spaceTransformer) {
		super.setSpaceTransformer(spaceTransformer);
		if (showSymbols && symbolHandler != null) {
			symbolHandler.setSpaceTransformer(spaceTransformer);
		}
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setFrontCut(int frontCut) {
		this.frontCut = frontCut;
	}

	public void setEndCut(int endCut) {
		this.endCut = endCut;
	}

	public void setBaseFrontCut(int baseFrontCut) {
		this.baseFrontCut = baseFrontCut;
	}

//	public Set<Sphere> getMarkers() {
//		return markersIndexes.keySet();
//	}
//	
//	public void addMarker(Sphere marker, int markerIndex, String markersGroupLabel) {
//		markersIndexes.put(marker, markerIndex);
//		markersLabels.put(marker, markersGroupLabel);
//	}
//	
//	public void setShowMarkers(boolean showMarkers) {
//		this.showMarkers = showMarkers;
//	}
//	
//	public void setMarkerSize(int markerSize) {
//		this.markerSize = markerSize;
//	}
//
//	public void setShowMarkersLabels(boolean showMarkersLabels) {
//		this.showMarkersLabels = showMarkersLabels;
//		
//	}

	/**********************************************************************/

	@Override
	public String toString(int depth) {
		return (Utils.blanks(depth) + "(LineStrip) #points:" + points.size());
	}

	/**********************************************************************/

	protected List<Point> points;
	// protected float width;
	protected boolean showPoints = false;
	protected boolean showSymbols = false;

	protected boolean stipple = false;
	protected int stippleFactor = 4;
	protected short stipplePattern = (short) 0xAAAA;
	protected SymbolHandler symbolHandler = null;

	private String id;
	private int baseFrontCut;
	private int frontCut;
	private int endCut;
	private ArrayList<Drawable> markersLabels;
//	private boolean showMarkers;
//	private boolean showMarkersLabels;
//	private HashMap<Sphere, Integer> markersIndexes;
//	private HashMap<Sphere, String> markersLabels;
//	private int markerSize = 3;
}
