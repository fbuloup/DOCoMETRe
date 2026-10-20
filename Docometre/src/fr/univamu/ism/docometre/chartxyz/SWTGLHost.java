package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.swt.GLCanvas;

/**
 * Surface OpenGL dans un GLCanvas SWT (Windows, Linux), animée dans le thread
 * de l'interface par {@link SWTAnimator}.
 */
final class SWTGLHost extends GLHost {

    private final GLCanvas canvas;
    private final SWTAnimator animator;

    // État de la souris
    private int lastX, lastY;
    private boolean leftDown, rightDown;

    SWTGLHost(Composite parent, GLCapabilities capabilities, GLChartRenderer renderer) {
        canvas = new GLCanvas(parent, SWT.NO_BACKGROUND, capabilities, null);
        canvas.setData(this);
        animator = new SWTAnimator(canvas, FPS, canvas::display);
        canvas.addGLEventListener(renderer);
        attachInput(renderer);
        canvas.addListener(SWT.Dispose, e -> close());
        animator.start();
    }

    /** Relie les événements souris / clavier SWT aux commandes du renderer. */
    private void attachInput(ChartInteraction interaction) {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                lastX = e.x;
                lastY = e.y;
                if (e.button == ChartInteraction.LEFT_BUTTON) leftDown = true;
                if (e.button == ChartInteraction.RIGHT_BUTTON) rightDown = true;
                Point size = canvas.getSize();
                interaction.press(e.button, e.x, e.y, size.x, size.y);
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == ChartInteraction.LEFT_BUTTON) leftDown = false;
                if (e.button == ChartInteraction.RIGHT_BUTTON) rightDown = false;
                // Reçu même hors du composant : SWT capture la souris pendant le glisser
                Point size = canvas.getSize();
                interaction.release(e.button, e.x, e.y, size.x, size.y);
            }
        });

        canvas.addMouseMoveListener(e -> {
            int dx = e.x - lastX;
            int dy = e.y - lastY;
            lastX = e.x;
            lastY = e.y;
            int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
            if (button != 0) {
                Point size = canvas.getSize();
                interaction.dragTo(button, e.x, e.y, dx, dy, size.x, size.y, ChartInteraction.InputScale.SWT);
            }
        });

        canvas.addMouseWheelListener(e -> interaction.wheel(e.count, ChartInteraction.InputScale.SWT));

        // Échap et Retour arrière arrivent comme les caractères ESCAPE et BACKSPACE
        canvas.addListener(SWT.KeyDown, e -> interaction.keyPressed(Character.toLowerCase(e.character)));

        // Focus clavier dès que la souris entre dans le graphe (utile quand plusieurs
        // graphes partagent une fenêtre), seulement si notre fenêtre est active pour
        // ne pas voler le focus à une autre fenêtre ou à un dialogue
        canvas.addListener(SWT.MouseEnter, e -> {
            if (canvas.getShell() == canvas.getDisplay().getActiveShell()) {
                canvas.forceFocus();
            }
        });

        canvas.setFocus();
    }

    @Override
    public Control getControl() { return canvas; }

    @Override
    public void stopAnimation() { animator.stop(); }

    @Override
    protected void destroy() {
        animator.stop();
        if (!canvas.isDisposed()) canvas.dispose();
    }
}
