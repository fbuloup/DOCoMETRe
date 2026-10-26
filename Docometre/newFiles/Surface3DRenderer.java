package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * OpenGL rendering of the 3D surface: axes, graduated reference box,
 * triangulated surface with smooth shading (per-vertex normals) and a color
 * gradient according to the height (y), solid / wireframe / both toggle.
 * The orbit camera is shared with {@link Chart3DRenderer} (see {@link OrbitCamera}).
 *
 * Keyboard commands, in addition to those of {@link GLChartRenderer}:
 * <ul>
 * <li>S: show/hide the filled (solid) surface</li>
 * <li>W: show/hide the wireframe mesh</li>
 * <li>H: enable/disable lighting</li>
 * </ul>
 *
 * The labels use a GLUT bitmap font (glRasterPos3f + glutBitmapString),
 * which does not depend on AWT.
 */
public class Surface3DRenderer extends GLChartRenderer {

    private final SurfaceData surfaceData;
    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();
    private final OrbitCamera camera = new OrbitCamera(30f, -40f);

    private volatile boolean showSolid = true;
    private volatile boolean showWireframe = false;
    private volatile boolean lightingEnabled = true;

    public Surface3DRenderer(SurfaceData surfaceData) {
        super(true, false);
        this.surfaceData = surfaceData;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_SMOOTH);

        // Simple lighting: one directional light + a little global ambient light
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[]{0.9f, 0.9f, 0.9f, 1f}, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.3f, 0.3f, 0.3f, 1f}, 0);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[]{0.25f, 0.25f, 0.27f, 1f}, 0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[]{0.2f, 0.2f, 0.2f, 1f}, 0);
        gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 16f);
        
        gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE); // both sides of the surface are lit correctly

        if (lightingEnabled) gl.glEnable(GL2.GL_LIGHTING);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        
        BoundingBox bounds = surfaceData.getBoundingBox();
        // near / far enclose the scene at the current camera distance (zoom included)
        float aspect = (float) this.width / this.height;
        float radius = bounds.radius;
        float distance = camera.distance(radius);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, aspect, Math.max(distance - 2f * radius, distance * 0.01f), distance + 2f * radius);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        camera.lookAt(glu, radius);

        // Light fixed in camera space: it "follows" the view, always well lit
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{0.4f, 0.6f, 1f, 0f}, 0);

        camera.orbit(gl, bounds);

        gl.glDisable(GL2.GL_LIGHTING);
        if (showGrid) {
            ReferenceBox box = new ReferenceBox(bounds, true, camera.positionInDataSpace(bounds));
            box.drawGrid(gl, this::drawBitmapLabel);
            if (showLabel) box.drawTickLabels(gl, this::drawBitmapLabel);
        }
        if (showAxes) ReferenceBox.drawOriginAxes(gl, bounds.radius);

        if (lightingEnabled) gl.glEnable(GL2.GL_LIGHTING);

        if (showSolid) {
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            drawSurface(gl, true);
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
        }

        if (showWireframe) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor4f(0f, 0f, 0f, 0.35f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawSurface(gl, false);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    	super.reshape(drawable, x, y, width, height);
        drawable.getGL().getGL2().glViewport(0, 0, this.width, this.height);
    }

    /**
     * Draws the surface as triangles. Filled (shaded): shaded, with a color
     * gradient according to the height (y); otherwise in the current color
     * (wireframe).
     */
    private void drawSurface(GL2 gl, boolean shaded) {
        int rows = surfaceData.getRows();
        int cols = surfaceData.getCols();
        float minY = surfaceData.getMinY();
        float range = Math.max(surfaceData.getMaxY() - minY, 1e-6f);

        gl.glBegin(GL2.GL_TRIANGLES);
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                // Diagonal (r,c)-(r+1,c+1), see SurfaceData
                emitVertex(gl, r, c, shaded, minY, range);
                emitVertex(gl, r + 1, c, shaded, minY, range);
                emitVertex(gl, r + 1, c + 1, shaded, minY, range);

                emitVertex(gl, r, c, shaded, minY, range);
                emitVertex(gl, r + 1, c + 1, shaded, minY, range);
                emitVertex(gl, r, c + 1, shaded, minY, range);
            }
        }
        gl.glEnd();
    }

    private void emitVertex(GL2 gl, int r, int c, boolean shaded, float minY, float range) {
        Point3D p = surfaceData.point(r, c);
        if (shaded) {
            float[] n = surfaceData.normal(r, c);
            float t = (p.y - minY) / range;
            // Gradient from blue (low) to orange/red (high)
            gl.glColor3f(0.15f + 0.75f * t, 0.4f + 0.25f * (1 - t), 0.95f - 0.75f * t);
            gl.glNormal3f(n[0], n[1], n[2]);
        }
        gl.glVertex3f(p.x, p.y, p.z);
    }

    /** Sets the raster position at (x, y, z) and draws a string there in a bitmap font. */
    private void drawBitmapLabel(GL2 gl, String text, float x, float y, float z, boolean axisName) {
        gl.glRasterPos3f(x, y, z);
        glut.glutBitmapString(axisName ? GLUT.BITMAP_HELVETICA_18 : GLUT.BITMAP_HELVETICA_12, text);
    }

    // --- Navigation ---

    @Override
    public void drag(int button, int dx, int dy, int width, int height, InputScale scale) {
        camera.drag(button, dx, dy, scale, surfaceData.getBoundingRadius());
    }

    @Override
    public void wheel(float amount, InputScale scale) {
        camera.wheel(amount, scale);
    }

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case 's': toggleSolid(); break;
            case 'w': toggleWireframe(); break;
            case 'h': toggleLighting(); break;
            default: super.keyPressed(key); break;
        }
    }

    public void rotate(float deltaX, float deltaY) { camera.rotate(deltaX, deltaY); }
    public void pan(float deltaX, float deltaY) { camera.pan(deltaX, deltaY, surfaceData.getBoundingRadius()); }
    public void zoom(float amount) { camera.zoom(amount); }

    @Override
    public void resetView() { camera.reset(); }

    /** Shows or hides the solid surface (also with the S key). */
    public void setSolidVisible(boolean visible) { setOption(DisplayOption.SOLID, visible, () -> showSolid, v -> showSolid = v); }
    public boolean isSolidVisible() { return showSolid; }
    public void toggleSolid() { toggleOption(DisplayOption.SOLID, () -> showSolid, v -> showSolid = v); }

    /** Shows or hides the wireframe (also with the W key). */
    public void setWireframeVisible(boolean visible) { setOption(DisplayOption.WIREFRAME, visible, () -> showWireframe, v -> showWireframe = v); }
    public boolean isWireframeVisible() { return showWireframe; }
    public void toggleWireframe() { toggleOption(DisplayOption.WIREFRAME, () -> showWireframe, v -> showWireframe = v); }

    /** Enables or disables the lighting (also with the H key). */
    public void setLightingEnabled(boolean enabled) { setOption(DisplayOption.LIGHTING, enabled, () -> lightingEnabled, v -> lightingEnabled = v); }
    public boolean isLightingEnabled() { return lightingEnabled; }
    public void toggleLighting() { toggleOption(DisplayOption.LIGHTING, () -> lightingEnabled, v -> lightingEnabled = v); }
}
