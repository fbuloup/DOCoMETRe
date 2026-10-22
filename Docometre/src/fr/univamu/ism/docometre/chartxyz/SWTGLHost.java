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
 * OpenGL surface in an SWT GLCanvas (Windows, Linux), animated in the user
 * interface thread by {@link SWTAnimator}.
 */
final class SWTGLHost extends GLHost {

    private final GLCanvas canvas;
    private final SWTAnimator animator;

    // Mouse state
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

    /** Connects the SWT mouse / keyboard events to the renderer commands. */
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
                // Received even outside the component: SWT captures the mouse during a drag
                Point size = canvas.getSize();
                interaction.release(e.button, e.x, e.y, size.x, size.y);
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                Point size = canvas.getSize();
                interaction.doubleClick(e.button, e.x, e.y, size.x, size.y);
            }
        });

        canvas.addMouseMoveListener(e -> {
            int dx = e.x - lastX;
            int dy = e.y - lastY;
            lastX = e.x;
            lastY = e.y;
            int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
            Point size = canvas.getSize();
            if (button != 0) {
                interaction.dragTo(button, e.x, e.y, dx, dy, size.x, size.y, ChartInteraction.InputScale.SWT);
            } else {
                interaction.move(e.x, e.y, size.x, size.y);
            }
        });

        canvas.addMouseWheelListener(e -> interaction.wheel(e.count, ChartInteraction.InputScale.SWT));

        // Tab: passed to the renderer if it uses it (cursor on several curves),
        // instead of moving the focus to the next component
        canvas.addListener(SWT.Traverse, e -> {
            if ((e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) && interaction.usesTabKey()) {
                e.doit = false; // the key then arrives as KeyDown
            }
        });

        // Escape and Backspace arrive as the ESCAPE and BACKSPACE characters
        canvas.addListener(SWT.KeyDown, e -> {
            if (e.character == SWT.TAB) {
                interaction.keyPressed((e.stateMask & SWT.SHIFT) != 0 ? ChartInteraction.BACK_TAB : ChartInteraction.TAB);
            } else {
                interaction.keyPressed(Character.toLowerCase(e.character));
            }
        });

        // Keyboard focus as soon as the mouse enters the chart (useful when several
        // charts share a window), only if our window is active so as not to steal
        // the focus from another window or a dialog
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
    public void startAnimation() { animator.start(); }

    @Override
    protected void destroy() {
        animator.stop();
        if (!canvas.isDisposed()) canvas.dispose();
    }
}
