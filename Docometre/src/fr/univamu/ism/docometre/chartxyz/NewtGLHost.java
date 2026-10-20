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
        NewtCanvasSWT created;
        try {
            glWindow.addGLEventListener(renderer);
            attachInput(renderer);
            created = NewtCanvasSWT.create(parent, SWT.NO_BACKGROUND, glWindow);
            if (created == null) throw new IllegalStateException("Impossible d'intégrer la fenêtre NEWT dans SWT.");
        } catch (RuntimeException | Error e) {
            glWindow.destroy(); // la fenêtre native ne doit pas survivre à l'échec
            throw e;
        }
        canvas = created;
        canvas.setData(this);
        animator = new FPSAnimator(glWindow, FPS, true);
        canvas.addListener(SWT.Dispose, e -> close());
        try {
			animator.start();
		} catch (RuntimeException | Error e) {
			close();
			throw e;
		}
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
                int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                // Comme le glisser, dans le thread OpenGL : appui, glisser et relâchement y sont traités dans l'ordre
                if (button != 0) glWindow.invoke(false, d -> { interaction.press(button, x, y, w, h); return true; });
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
                int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                if (button != 0) glWindow.invoke(false, d -> { interaction.release(button, x, y, w, h); return true; });
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
                if (button != 0) {
                    int x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                    // exécuté dans le thread OpenGL au début de l'image suivante : pas d'accès concurrent au renderer
                    glWindow.invoke(false, d -> { interaction.dragTo(button, x, y, dx, dy, w, h, ChartInteraction.InputScale.NEWT); return true; });
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                float amount = e.getRotation()[1];
                glWindow.invoke(false, d -> { interaction.wheel(amount, ChartInteraction.InputScale.NEWT); return true; });
            }
        });

        glWindow.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            	char key = toKey(e);
            	glWindow.invoke(false, d -> { interaction.keyPressed(key); return true; });
            }
        });
    }

    /** Bouton NEWT -> {@link ChartInteraction#LEFT_BUTTON} / {@link ChartInteraction#RIGHT_BUTTON}, 0 pour les autres. */
    private static int toButton(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) return ChartInteraction.LEFT_BUTTON;
        if (e.getButton() == MouseEvent.BUTTON3) return ChartInteraction.RIGHT_BUTTON;
        return 0;
    }

    /** Caractère transmis au renderer : Échap et Retour arrière par leur code, les autres en minuscule. */
    private static char toKey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) return ChartInteraction.ESCAPE;
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) return ChartInteraction.BACKSPACE;
        return Character.toLowerCase(e.getKeyChar());
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
