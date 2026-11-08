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
    private int rightPressX, rightPressY;
    private boolean rightDragged; // the right button moved beyond CLICK_TOLERANCE: pan, no context menu

    SWTGLHost(Composite parent, GLCapabilities capabilities, GLChartRenderer renderer) {
        canvas = new GLCanvas(parent, SWT.NO_BACKGROUND, capabilities, null);
        canvas.setData(this);
        animator = new SWTAnimator(canvas, FPS, canvas::display);
        canvas.addGLEventListener(renderer);
        attachInput(renderer);
        canvas.addListener(SWT.Dispose, e -> close());
        animator.start();
    }

    /**
     * Ratio between the OpenGL surface (pixels) and the SWT component (points):
     * 1 on a screen without scaling, 2 on a Retina screen, 1.5 on a Windows
     * screen scaled to 150 %. Read at each event: it follows the component when
     * it moves to a screen of another density.
     */
    private float[] pixelScale() {
        Point size = canvas.getSize();
        return new float[] { canvas.getSurfaceWidth() / (float) Math.max(1, size.x),
                              canvas.getSurfaceHeight() / (float) Math.max(1, size.y) };
    }

    /** Connects the SWT mouse / keyboard events to the renderer commands. */
    private void attachInput(ChartInteraction interaction) {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                lastX = e.x;
                lastY = e.y;
                if (e.button == ChartInteraction.LEFT_BUTTON) leftDown = true;
                if (e.button == ChartInteraction.RIGHT_BUTTON) {
                    rightDown = true;
                    rightPressX = e.x;
                    rightPressY = e.y;
                    rightDragged = false;
                }
                float[] scale = pixelScale();
                int x = (int) (e.x * scale[0]), y = (int) (e.y * scale[1]);
                interaction.press(e.button, modifiers(e), x, y, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == ChartInteraction.LEFT_BUTTON) leftDown = false;
                if (e.button == ChartInteraction.RIGHT_BUTTON) rightDown = false;
                // Received even outside the component: SWT captures the mouse during a drag
                float[] scale = pixelScale();
                int x = (int) (e.x * scale[0]), y = (int) (e.y * scale[1]);
                interaction.release(e.button, x, y, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
                // Right click without drag: context menu
                if (e.button == ChartInteraction.RIGHT_BUTTON && !rightDragged && !leftDown) requestContextMenu(e.x, e.y);
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                float[] scale = pixelScale();
                int x = (int) (e.x * scale[0]), y = (int) (e.y * scale[1]);
                interaction.doubleClick(e.button, modifiers(e), x, y, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
            }
        });

        canvas.addMouseMoveListener(e -> {
            int dx = e.x - lastX;
            int dy = e.y - lastY;
            lastX = e.x;
            lastY = e.y;
            if (rightDown && (Math.abs(e.x - rightPressX) > CLICK_TOLERANCE || Math.abs(e.y - rightPressY) > CLICK_TOLERANCE)) {
                rightDragged = true;
            }
            int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
            float[] scale = pixelScale();
            int x = (int) (e.x * scale[0]), y = (int) (e.y * scale[1]);
            if (button != 0) {
                interaction.dragTo(button, x, y, dx, dy, canvas.getSurfaceWidth(), canvas.getSurfaceHeight(), ChartInteraction.InputScale.SWT);
            } else {
                interaction.move(x, y, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
            }
        });

        canvas.addMouseWheelListener(e -> interaction.wheel(e.count, ChartInteraction.InputScale.SWT));

        // Context menu: with the mouse, opened on the release of the right button (see mouseUp)
        // so that the right drag (pan) keeps working; with the keyboard (menu key, Shift + F10),
        // opened at the mouse position if it is over the chart, otherwise at its center
        canvas.addListener(SWT.MenuDetect, e -> {
            if (!hasContextMenuHandler()) return; // menu of the SWT component, if the application gave one
            e.doit = false;
            if (e.detail != SWT.MENU_KEYBOARD) return;
            Point size = canvas.getSize();
            Point mouse = canvas.toControl(canvas.getDisplay().getCursorLocation());
            boolean inside = mouse.x >= 0 && mouse.y >= 0 && mouse.x < size.x && mouse.y < size.y;
            if (inside) requestContextMenu(mouse.x, mouse.y);
            else requestContextMenu(size.x / 2, size.y / 2);
        });

        // Tab: passed to the renderer if it uses it (cursor on several curves),
        // instead of moving the focus to the next component
        // The arrow keys stay in the chart (pan) instead of moving the focus
        canvas.addListener(SWT.Traverse, e -> {
            if ((e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) && interaction.usesTabKey()) {
                e.doit = false; // the key then arrives as KeyDown
            }
            if (e.detail == SWT.TRAVERSE_ARROW_NEXT || e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
                e.doit = false;
            }
        });

        // Escape and Backspace arrive as the ESCAPE and BACKSPACE characters
        canvas.addListener(SWT.KeyDown, e -> {
            char arrow = toArrow(e.keyCode);
            if (arrow != 0) {
                interaction.keyPressed(arrow);
            } else if (e.character == SWT.TAB) {
                interaction.keyPressed((e.stateMask & SWT.SHIFT) != 0 ? ChartInteraction.BACK_TAB : ChartInteraction.TAB);
            } else {
                interaction.keyPressed(Character.toLowerCase(e.character));
            }
        });

        // Keyboard focus as soon as the mouse enters the chart (useful when several
        // charts share a window), only if our window is active so as not to steal
        // the focus from another window or a dialog
        canvas.addListener(SWT.MouseExit, e -> interaction.exit());

        canvas.addListener(SWT.MouseEnter, e -> {
            if (canvas.getShell() == canvas.getDisplay().getActiveShell()) {
                canvas.forceFocus();
            }
        });

        canvas.setFocus();
    }

    /** SWT key code of an arrow key -> ChartInteraction.ARROW_..., 0 for the other keys. */
    private static char toArrow(int keyCode) {
        switch (keyCode) {
            case SWT.ARROW_LEFT: return ChartInteraction.ARROW_LEFT;
            case SWT.ARROW_RIGHT: return ChartInteraction.ARROW_RIGHT;
            case SWT.ARROW_UP: return ChartInteraction.ARROW_UP;
            case SWT.ARROW_DOWN: return ChartInteraction.ARROW_DOWN;
            default: return 0;
        }
    }

    /** Modifier keys of an SWT mouse event: Ctrl, or Command on macOS. */
    private static int modifiers(MouseEvent e) {
        return ((e.stateMask & (SWT.CTRL | SWT.COMMAND)) != 0) ? ChartInteraction.CTRL : 0;
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
