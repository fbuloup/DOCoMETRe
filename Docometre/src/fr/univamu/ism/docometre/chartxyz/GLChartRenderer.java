package fr.univamu.ism.docometre.chartxyz;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * Base de tous les renderers : état OpenGL initial commun (fond sombre,
 * lignes lissées, transparence), taille de la zone d'affichage, éléments
 * d'habillage activables au clavier (axes, grille, graduations) et
 * commandes clavier communes :
 * <ul>
 * <li>R : réinitialiser la vue</li>
 * <li>A : afficher/masquer les axes</li>
 * <li>G : afficher/masquer la grille</li>
 * <li>L : afficher/masquer les graduations</li>
 * </ul>
 */
public abstract class GLChartRenderer implements GLEventListener, ChartInteraction {

    protected volatile boolean showAxes;
    protected volatile boolean showGrid = true;
    protected volatile boolean showLabel;

    /** Taille de la zone d'affichage (pixels). */
    protected int width = 1, height = 1;

    protected GLChartRenderer(boolean showAxes, boolean showLabel) {
        this.showAxes = showAxes;
        this.showLabel = showLabel;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
    }

    /** Revient à la vue initiale (touche R). */
    public abstract void resetView();

    @Override
    public void keyPressed(char key) {
        switch (key) {
            case 'r': resetView(); break;
            case 'a': toggleAxes(); break;
            case 'g': toggleGrid(); break;
            case 'l': toggleLabel(); break;
            default: break;
        }
    }

    public void toggleAxes() { showAxes = !showAxes; }
    public void toggleGrid() { showGrid = !showGrid; }
    public void toggleLabel() { showLabel = !showLabel; }

    public void setGridVisibility(boolean value) {
        showGrid = value;
    }
}
