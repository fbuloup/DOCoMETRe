package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;

/**
 * OpenGL rendering of one or several real-time 2D curves ({@link Curve2DData}).
 *
 * The view automatically fits the data of the time window (or the fixed
 * scales); zoom and pan apply on top of it, as well as the rectangle zoom
 * (see {@link PlotRenderer}).
 * Cursor (C key, see {@link PlotRenderer}): it designates the point of the
 * trajectory closest to the mouse.
 * Keyboard command specific to 2D: E, same scale on x and y.
 */
public class Chart2DRenderer extends PlotRenderer<Curve2DData> {

    private static final float DEFAULT_LINE_WIDTH = 2f; // width of the curves without an explicit width (pixels)

    // Bounding box of all the curves, recomputed at each frame
    private float dataMinX = -1f, dataMaxX = 1f, dataMinY = -1f, dataMaxY = 1f;

    private volatile boolean equalAspect = false; // same scale on x and y (useful for trajectories)

    /**
     * @param historySize history duration (length of the trails), in seconds, imposed on all the curves
     */
    public Chart2DRenderer(double historySize, Curve2DData... curves) {
        super(5f, curves);
        setWindowTimeWidth(historySize);
    }

    /** Chart in which each curve keeps its own history duration (static data, for example). */
    public Chart2DRenderer(Curve2DData... curves) {
        super(5f, curves);
    }

    @Override
    protected void updateData() {
        snapshotCurves();
        computeBounds();
    }

    /** Bounding box of all the non-empty curves (from -1 to 1 if there is none). */
    private void computeBounds() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean empty = true;
        for (Curve2DData curve : frameCurves) {
            if (curve.getSnapshotCount() == 0) continue;
            empty = false;
            minX = Math.min(minX, curve.getMinX()); maxX = Math.max(maxX, curve.getMaxX());
            minY = Math.min(minY, curve.getMinY()); maxY = Math.max(maxY, curve.getMaxY());
        }
        if (empty) {
            minX = minY = -1f;
            maxX = maxY = 1f;
        }
        dataMinX = minX; dataMaxX = maxX;
        dataMinY = minY; dataMaxY = maxY;
    }

    /**
     * Visible window: the zoom window if it exists, otherwise the fixed
     * scales or the data (with margin), zoom and pan included; then, if
     * "same scale" is active, widening of the tightest axis.
     */
    @Override
    protected void computeView() {
        double halfX, halfY, centerX, centerY;
        double[] window = zoomWindow();
        double[] fixed = getFixedScaleBounds();
        if (window != null) { // zoom window: exactly the chosen rectangle
            halfX = (window[1] - window[0]) / 2;
            halfY = (window[3] - window[2]) / 2;
            centerX = (window[0] + window[1]) / 2;
            centerY = (window[2] + window[3]) / 2;
        } else {
            double baseCenterX, baseCenterY;
            if (fixed != null) { // fixed scales: exactly the given bounds, without margin
                halfX = (fixed[1] - fixed[0]) / 2;
                halfY = (fixed[3] - fixed[2]) / 2;
                baseCenterX = (fixed[0] + fixed[1]) / 2;
                baseCenterY = (fixed[2] + fixed[3]) / 2;
            } else {
                halfX = halfExtent(dataMinX, dataMaxX);
                halfY = halfExtent(dataMinY, dataMaxY);
                baseCenterX = (dataMinX + dataMaxX) / 2f;
                baseCenterY = (dataMinY + dataMaxY) / 2f;
            }
            centerX = baseCenterX + panX * 2 * halfX;
            centerY = baseCenterY + panY * 2 * halfY;
            halfX /= zoom;
            halfY /= zoom;
        }

        if (equalAspect) {
            // Same number of units per pixel on both axes: the "tightest" axis is widened
            double unitsPerPixel = Math.max(2 * halfX / plotWidth, 2 * halfY / plotHeight);
            halfX = unitsPerPixel * plotWidth / 2;
            halfY = unitsPerPixel * plotHeight / 2;
        }

        viewMinX = centerX - halfX; viewMaxX = centerX + halfX;
        viewMinY = centerY - halfY; viewMaxY = centerY + halfY;
    }

    @Override
    protected boolean hasVerticalAxis() { return true; }

    /** Point i of the trajectory: its coordinates (x, y). */
    @Override
    protected boolean cursorPoint(Curve2DData curve, int i, double[] xy) {
        float[] c = curve.getSnapshotCoordinates();
        xy[0] = c[2 * i];
        xy[1] = c[2 * i + 1];
        return true;
    }

    @Override
    protected void drawCurves(GL2 gl, double origin) {
        double scaleX = plotWidth / (viewMaxX - viewMinX);   // pixels per unit
        double scaleY = plotHeight / (viewMaxY - viewMinY);
        double left = viewMinX, bottom = viewMinY;
        drawTrajectories(gl, DEFAULT_LINE_WIDTH, (x, y, z, pixel) -> {
            pixel[0] = (x - left) * scaleX;
            pixel[1] = (y - bottom) * scaleY;
            return true;
        }, scaleX, scaleY);
        if (showPoints) drawTrajectoryPoints(gl);
    }

    @Override
    public void keyPressed(char key) {
        if (key == 'e') toggleEqualAspect();
        else super.keyPressed(key);
    }

    /**
     * Fixed scales: the axes go exactly from xMin to xMax and from yMin to
     * yMax, whatever the curve values (what exceeds them is clipped by the
     * frame). Zoom, pan and "same scale" (E key) remain possible; R goes
     * back to these scales.
     */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        setFixedScaleBounds(xMin, xMax, yMin, yMax);
    }

    public void toggleEqualAspect() { equalAspect = !equalAspect; }
}
