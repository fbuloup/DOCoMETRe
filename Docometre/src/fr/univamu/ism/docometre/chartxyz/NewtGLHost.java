package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Surface OpenGL dans une fenêtre NEWT intégrée par NewtCanvasSWT (macOS),
 * animée par un FPSAnimator (thread propre).
 */
final class NewtGLHost extends GLHost {

    private final GLWindow glWindow;
    private final FPSAnimator animator;
    private final NewtCanvasSWT canvas;

    // État de la souris
    private int lastX, lastY;
    private boolean leftDown, rightDown;

    NewtGLHost(Composite parent, GLCapabilities capabilities, GLChartRenderer renderer) {
        glWindow = GLWindow.create(capabilities);
        animator = new FPSAnimator(glWindow, FPS, true);
        glWindow.addGLEventListener(renderer);
        attachInput(renderer);
        canvas = NewtCanvasSWT.create(parent, SWT.NO_BACKGROUND, glWindow);
        canvas.addListener(SWT.Dispose, e -> close());
        animator.start();
    }

    /** Relie les événements souris / clavier NEWT aux commandes du renderer. */
    private void attachInput(ChartInteraction interaction) {
        glWindow.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = true;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = true;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Focus clavier dès que la souris entre dans le graphe (plusieurs graphes par fenêtre)
                glWindow.requestFocus();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = false;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
                if (button != 0) {
                    // getX()/getY() et getWidth()/getHeight() sont dans les mêmes unités (fenêtre)
                    interaction.drag(button, dx, dy, glWindow.getWidth(), glWindow.getHeight(), ChartInteraction.InputScale.NEWT);
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                interaction.wheel(e.getRotation()[1], ChartInteraction.InputScale.NEWT);
            }
        });

        glWindow.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                interaction.keyPressed(Character.toLowerCase(e.getKeyChar()));
            }
        });
    }

    @Override
    public Control getControl() { return canvas; }

    @Override
    public void stopAnimation() { animator.stop(); }

    @Override
    protected void destroy() {
        animator.stop();
        if (!canvas.isDisposed()) canvas.dispose();
        glWindow.destroy();
    }
}
