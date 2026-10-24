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
 * OpenGL surface in a NEWT window embedded by NewtCanvasSWT (macOS),
 * animated by an FPSAnimator (its own thread).
 */
final class NewtGLHost extends GLHost {

    private final GLWindow glWindow;
    private final FPSAnimator animator;
    private final NewtCanvasSWT canvas;

    // Mouse state
    private int lastX, lastY;
    private boolean leftDown, rightDown;

    NewtGLHost(Composite parent, GLCapabilities capabilities, GLChartRenderer renderer) {
        glWindow = GLWindow.create(capabilities);
        NewtCanvasSWT created;
        try {
            glWindow.addGLEventListener(renderer);
            attachInput(renderer);
            created = NewtCanvasSWT.create(parent, SWT.NO_BACKGROUND, glWindow);
            if (created == null) throw new IllegalStateException("Unable to embed the NEWT window in SWT.");
        } catch (RuntimeException | Error e) {
            glWindow.destroy(); // the native window must not outlive the failure
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

    /** Connects the NEWT mouse / keyboard events to the renderer commands. */
    private void attachInput(ChartInteraction interaction) {
        glWindow.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = true;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = true;
                int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                // Like the drag, in the OpenGL thread: press, drag and release are processed there in order
                int modifiers = modifiers(e);
                if (button != 0) glWindow.invoke(false, d -> { interaction.press(button, modifiers, x, y, w, h); return true; });
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Keyboard focus as soon as the mouse enters the chart (several charts per window)
                glWindow.requestFocus();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                glWindow.invoke(false, d -> { interaction.exit(); return true; });
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = false;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = false;
                int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                if (button != 0) glWindow.invoke(false, d -> { interaction.release(button, x, y, w, h); return true; });
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                int modifiers = modifiers(e);
                if (button != 0) glWindow.invoke(false, d -> { interaction.doubleClick(button, modifiers, x, y, w, h); return true; });
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
                    // run in the OpenGL thread at the beginning of the next frame: no concurrent access to the renderer
                    glWindow.invoke(false, d -> { interaction.dragTo(button, x, y, dx, dy, w, h, ChartInteraction.InputScale.NEWT); return true; });
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                int x = e.getX(), y = e.getY(), w = glWindow.getWidth(), h = glWindow.getHeight();
                glWindow.invoke(false, d -> { interaction.move(x, y, w, h); return true; });
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

    /** NEWT button -> {@link ChartInteraction#LEFT_BUTTON} / {@link ChartInteraction#RIGHT_BUTTON}, 0 for the others. */
    private static int toButton(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) return ChartInteraction.LEFT_BUTTON;
        if (e.getButton() == MouseEvent.BUTTON3) return ChartInteraction.RIGHT_BUTTON;
        return 0;
    }

    /** Character passed to the renderer: Escape, Backspace, Tab and the arrows by their code, the others in lower case. */
    private static char toKey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) return ChartInteraction.ARROW_LEFT;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) return ChartInteraction.ARROW_RIGHT;
        if (e.getKeyCode() == KeyEvent.VK_UP) return ChartInteraction.ARROW_UP;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) return ChartInteraction.ARROW_DOWN;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) return ChartInteraction.ESCAPE;
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) return ChartInteraction.BACKSPACE;
        if (e.getKeyCode() == KeyEvent.VK_TAB) return e.isShiftDown() ? ChartInteraction.BACK_TAB : ChartInteraction.TAB;
        return Character.toLowerCase(e.getKeyChar());
    }

    /** Modifier keys of a NEWT mouse event: Ctrl, or Command on macOS. */
    private static int modifiers(MouseEvent e) {
        return (e.isControlDown() || e.isMetaDown()) ? ChartInteraction.CTRL : 0;
    }

    @Override
    public Control getControl() { return canvas; }

    @Override
    public void stopAnimation() { animator.stop(); }
    
    @Override
    public void startAnimation() { animator.start(); }

    @Override
    protected void destroy() {
        animator.stop();
        if (!canvas.isDisposed()) canvas.dispose();
        glWindow.destroy();
    }
}
