package fr.univamu.ism.docometre.chartxyz;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * OpenGL rendering of real-time 2D curves without history: only the last
 * sample received by each curve is displayed, as a symbol (geometric shape
 * or image, see {@link CursorStyle}). The curves only keep their last
 * sample (history duration imposed on them: {@link #LAST_SAMPLE_ONLY}).
 *
 * Each curve can have its own style ({@link #setCursorStyle(Curve2DData, CursorStyle)});
 * the others use the default style of the chart ({@link #setCursorStyle(CursorStyle)}).
 *
 * Scales: with fixed scales (the usual case: the workspace of the cursor),
 * the axes keep the given bounds. With dynamic scales, the view fits all the
 * positions displayed since the beginning (or since the last R key /
 * {@link #resetView()} / {@link #resetDataBounds()}): it widens when the
 * point goes out of it, so that the point moves in a stable frame.
 *
 * The keys and mouse actions are those of {@link Chart2DRenderer} (zoom,
 * pan, E for the same scale on x and y, C for the crosshair cursor, which
 * designates the displayed points), except P (points) and D (reduction of
 * the number of points), which have no use here. No markers.
 */
public class CursorChart2DRenderer extends Chart2DRenderer {

    /** History duration (s) imposed on the curves: short enough to keep a single sample. */
    public static final double LAST_SAMPLE_ONLY = 1e-9;

    // Styles: default one, and those of the curves that have their own (removed with the curve)
    private volatile CursorStyle defaultStyle = CursorStyle.DEFAULT;
    private final Map<Curve2DData, CursorStyle> styles = new ConcurrentHashMap<>();

    // Positions displayed since the last reset (dynamic scales), rendering thread only
    private boolean boundsEmpty = true;
    private float boundsMinX, boundsMaxX, boundsMinY, boundsMaxY;
    private volatile boolean boundsResetRequested;

    private float pixelScale = 1f; // OpenGL pixels per user interface point (Retina screen with NEWT)
    // Textures of the images, by image (rendering thread only)
    private final Map<CursorStyle.ImageRaster, Integer> textures = new IdentityHashMap<>();

    public CursorChart2DRenderer(Curve2DData... curves) {
        super(LAST_SAMPLE_ONLY, curves);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        super.reshape(drawable, x, y, width, height);
        pixelScale = GLText.surfaceScale(drawable);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        for (int texture : textures.values()) gl.glDeleteTextures(1, new int[]{texture}, 0);
        textures.clear();
        super.dispose(drawable);
    }

    /**
     * Bounding box of the positions displayed since the last reset (from -1
     * to 1 if there is none): the last sample of each curve, at each frame.
     */
    @Override
    protected void computeBounds() {
        if (boundsResetRequested) {
            boundsResetRequested = false;
            boundsEmpty = true;
        }
        for (Curve2DData curve : frameCurves) {
            int n = curve.getSnapshotCount();
            if (n == 0) continue;
            float[] c = curve.getSnapshotCoordinates();
            float x = c[2 * (n - 1)], y = c[2 * n - 1];
            if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
            if (boundsEmpty) {
                boundsMinX = boundsMaxX = x;
                boundsMinY = boundsMaxY = y;
                boundsEmpty = false;
            } else {
                boundsMinX = Math.min(boundsMinX, x); boundsMaxX = Math.max(boundsMaxX, x);
                boundsMinY = Math.min(boundsMinY, y); boundsMaxY = Math.max(boundsMaxY, y);
            }
        }
        if (boundsEmpty) {
            dataMinX = dataMinY = -1f;
            dataMaxX = dataMaxY = 1f;
        } else {
            dataMinX = boundsMinX; dataMaxX = boundsMaxX;
            dataMinY = boundsMinY; dataMaxY = boundsMaxY;
        }
    }

    /**
     * Forgets the positions displayed so far: with dynamic scales, the view
     * fits again the next positions only. Usable from any thread.
     */
    public void resetDataBounds() {
        boundsResetRequested = true;
    }

    /** Initial view (R key); with dynamic scales, also forgets the positions displayed so far. */
    @Override
    public void resetView() {
        super.resetView();
        resetDataBounds();
    }

    /** The crosshair cursor (C key) only designates the displayed point: the last one. */
    @Override
    protected int firstCursorPoint(Curve2DData curve) {
        return Math.max(0, curve.getSnapshotCount() - 1);
    }

    @Override
    protected boolean supportsMarkers() { return false; }

    @Override
    protected void curveRemoved(Curve2DData curve, int index) {
        super.curveRemoved(curve, index);
        styles.remove(curve);
    }

    /** P (points) and D (reduction of the number of points) have no effect on this chart. */
    @Override
    public void keyPressed(char key) {
        if (key == 'p' || key == 'd') return;
        super.keyPressed(key);
    }

    // ------------------------------------------------------------------
    // Drawing

    /** Symbol of the last sample of each curve, in pixels of the plot area (the size does not depend on the zoom). */
    @Override
    protected void drawCurves(GL2 gl, double origin) {
        double scaleX = plotWidth / (viewMaxX - viewMinX); // pixels per unit
        double scaleY = plotHeight / (viewMaxY - viewMinY);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0, plotWidth, 0, plotHeight, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        for (Curve2DData curve : frameCurves) {
            int n = curve.getSnapshotCount();
            if (n == 0) continue;
            float[] c = curve.getSnapshotCoordinates();
            float x = c[2 * (n - 1)], y = c[2 * n - 1];
            if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
            float px = (float) ((x - viewMinX) * scaleX);
            float py = (float) ((y - viewMinY) * scaleY);
            CursorStyle style = getCursorStyle(curve);
            if (style.isImage()) drawImage(gl, style, px, py);
            else drawShape(gl, style, CurveStyles.colorOf(curve), px, py);
            drawnPoints++;
        }

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        deleteUnusedTextures(gl);
    }

    /**
     * Shape filled (triangle fan from its center: all the shapes are
     * star-shaped around it), then outlined; without outline, a thin line in
     * the fill color smooths its edges.
     */
    private void drawShape(GL2 gl, CursorStyle style, float[] curveColor, float px, float py) {
        float[] contour = contour(style.getShape(), style.getSize() * pixelScale / 2f);
        CurveColor fillColor = style.fillColor();
        float[] fill = (fillColor != null) ? fillColor.gl : curveColor;
        gl.glColor3f(fill[0], fill[1], fill[2]);
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glVertex2f(px, py);
        for (int k = 0; k < contour.length; k += 2) gl.glVertex2f(px + contour[k], py + contour[k + 1]);
        gl.glVertex2f(px + contour[0], py + contour[1]);
        gl.glEnd();

        CurveColor outlineColor = style.outlineColor();
        if (outlineColor != null) {
            gl.glColor3f(outlineColor.gl[0], outlineColor.gl[1], outlineColor.gl[2]);
            gl.glLineWidth(style.getOutlineWidth() * pixelScale);
        } else {
            gl.glLineWidth(1f);
        }
        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int k = 0; k < contour.length; k += 2) gl.glVertex2f(px + contour[k], py + contour[k + 1]);
        gl.glEnd();
        gl.glLineWidth(1f);
    }

    /**
     * Contour of a shape centered on (0, 0), counterclockwise, as interleaved
     * coordinates {x0, y0, x1, y1...}, contained in the square [-h, h]².
     */
    static float[] contour(CursorShape shape, float h) {
        switch (shape) {
            case SQUARE:
                return new float[]{-h, -h, h, -h, h, h, -h, h};
            case DIAMOND:
                return new float[]{0, -h, h, 0, 0, h, -h, 0};
            case TRIANGLE:
                return new float[]{-h, -h, h, -h, 0, h};
            case TRIANGLE_DOWN:
                return new float[]{0, -h, h, h, -h, h};
            case PLUS:
                return plus(h, 0);
            case CROSS:
                // Plus rotated by 45 degrees, enlarged so that the ends of its arms touch the square:
                // the half extent of the rotated plus is (arm + half thickness) / sqrt(2) = arm * (4/3) / sqrt(2)
                return plus(h * 3f / (2f * (float) Math.sqrt(2)), Math.PI / 4);
            case STAR:
                return star(h, 0.4f * h, 5);
            case CIRCLE:
            default:
                int segments = Math.max(16, Math.min(96, Math.round(2 * h)));
                float[] c = new float[2 * segments];
                for (int i = 0; i < segments; i++) {
                    double a = 2 * Math.PI * i / segments;
                    c[2 * i] = (float) (h * Math.cos(a));
                    c[2 * i + 1] = (float) (h * Math.sin(a));
                }
                return c;
        }
    }

    /** Plus whose arms measure arm from the center and arm * 2/3 wide, rotated by angle radians. */
    private static float[] plus(float arm, double angle) {
        float t = arm / 3f; // half thickness of the arms
        float[] c = {t, -arm, t, -t, arm, -t, arm, t, t, t, t, arm,
                     -t, arm, -t, t, -arm, t, -arm, -t, -t, -t, -t, -arm};
        if (angle != 0) {
            float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
            for (int k = 0; k < c.length; k += 2) {
                float x = c[k], y = c[k + 1];
                c[k] = x * cos - y * sin;
                c[k + 1] = x * sin + y * cos;
            }
        }
        return c;
    }

    /** Star with branches points, the first one upwards, alternating the outer and inner radii. */
    private static float[] star(float outer, float inner, int branches) {
        float[] c = new float[4 * branches];
        for (int i = 0; i < 2 * branches; i++) {
            double a = Math.PI / 2 + Math.PI * i / branches;
            float r = (i % 2 == 0) ? outer : inner;
            c[2 * i] = (float) (r * Math.cos(a));
            c[2 * i + 1] = (float) (r * Math.sin(a));
        }
        return c;
    }

    /** Image centered on the point, its largest dimension scaled to the size of the style. */
    private void drawImage(GL2 gl, CursorStyle style, float px, float py) {
        CursorStyle.ImageRaster raster = style.imageRaster();
        Integer texture = textures.get(raster);
        if (texture == null) {
            texture = createTexture(gl, raster);
            textures.put(raster, texture);
        }
        float scale = style.getSize() * pixelScale / Math.max(raster.width, raster.height);
        float w = raster.width * scale, h = raster.height * scale;
        // Integer corner: an image at its natural size keeps each of its pixels on a screen pixel
        float x0 = Math.round(px - w / 2f), y0 = Math.round(py - h / 2f);
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(0, 0); gl.glVertex2f(x0, y0);
        gl.glTexCoord2f(1, 0); gl.glVertex2f(x0 + w, y0);
        gl.glTexCoord2f(1, 1); gl.glVertex2f(x0 + w, y0 + h);
        gl.glTexCoord2f(0, 1); gl.glVertex2f(x0, y0 + h);
        gl.glEnd();
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private static int createTexture(GL2 gl, CursorStyle.ImageRaster raster) {
        int[] id = new int[1];
        gl.glGenTextures(1, id, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, id[0]);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, raster.width, raster.height, 0,
                        GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, java.nio.ByteBuffer.wrap(raster.rgba));
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        return id[0];
    }

    /** Releases the textures of the images that no style uses any more. */
    private void deleteUnusedTextures(GL2 gl) {
        if (textures.isEmpty()) return;
        Set<CursorStyle.ImageRaster> used = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        if (defaultStyle.isImage()) used.add(defaultStyle.imageRaster());
        for (CursorStyle style : styles.values()) if (style.isImage()) used.add(style.imageRaster());
        for (Iterator<Map.Entry<CursorStyle.ImageRaster, Integer>> it = textures.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<CursorStyle.ImageRaster, Integer> entry = it.next();
            if (used.contains(entry.getKey())) continue;
            gl.glDeleteTextures(1, new int[]{entry.getValue()}, 0);
            it.remove();
        }
    }

    // ------------------------------------------------------------------
    // Styles

    /**
     * Style of the curves that do not have their own (default:
     * {@link CursorStyle#DEFAULT}). Usable from any thread; null goes back
     * to the default style.
     */
    public void setCursorStyle(CursorStyle style) {
        defaultStyle = (style != null) ? style : CursorStyle.DEFAULT;
    }

    /** Style of the curves that do not have their own. */
    public CursorStyle getCursorStyle() { return defaultStyle; }

    /**
     * Own style of a curve of the chart (removed with the curve); null makes
     * it use the default style of the chart again. Usable from any thread.
     *
     * @throws IllegalArgumentException if the curve is not displayed in this chart
     */
    public void setCursorStyle(Curve2DData curve, CursorStyle style) {
        if (curve == null || !curve.isOwnedBy(this)) {
            throw new IllegalArgumentException("The curve is not displayed in this chart.");
        }
        if (style != null) styles.put(curve, style);
        else styles.remove(curve);
    }

    /** Style actually used for a curve: its own style, otherwise the default style of the chart. */
    public CursorStyle getCursorStyle(Curve2DData curve) {
        CursorStyle style = (curve != null) ? styles.get(curve) : null;
        return (style != null) ? style : defaultStyle;
    }

    /** True if the curve has its own style. */
    public boolean hasOwnCursorStyle(Curve2DData curve) {
        return curve != null && styles.containsKey(curve);
    }
}
