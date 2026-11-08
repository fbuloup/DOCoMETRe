package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * OpenGL surface in a NEWT window embedded by NewtCanvasSWT (macOS), animated
 * by an FPSAnimator (its own thread).
 */
final class NewtGLHost extends GLHost {

	private final GLWindow glWindow;
	private final FPSAnimator animator;
	private final NewtCanvasSWT canvas;

	// Mouse state
	private int lastX, lastY;
	private boolean leftDown, rightDown;
	private int rightPressX, rightPressY;
	private boolean rightDragged; // the right button moved beyond CLICK_TOLERANCE: pan, no context menu
	

    // macOS may deliver the same key press twice (key equivalent + key down) :
    // ignore a repeated key within a short delay
    private long lastForwardedWhen = -1;
    private short lastForwardedKeyCode = 0;

	NewtGLHost(Composite parent, GLCapabilities capabilities, GLChartRenderer renderer) {
		glWindow = GLWindow.create(capabilities);
		NewtCanvasSWT created;
		try {
			glWindow.addGLEventListener(renderer);
			attachMouseInput(renderer);
			attachNewtKeyboard(renderer);
			created = NewtCanvasSWT.create(parent, SWT.NO_BACKGROUND, glWindow);
			if (created == null)
				throw new IllegalStateException("Unable to embed the NEWT window in SWT.");
			// Set after NewtCanvasSWT.create, which resets the focus action of the NEWT
			// window :
			// the NEWT window must not request the keyboard focus itself, it would steal
			// the
			// keyboard from SWT (shortcuts of the application, Escape to stop a trial...)
			// and,
			// on macOS, raise the application above the other ones. The focus is given to
			// the
			// SWT canvas instead (see attachKeyboardInput).
			glWindow.setFocusAction(() -> {
				NewtCanvasSWT swtCanvas = created;
				if (swtCanvas != null && !swtCanvas.isDisposed()) {
					swtCanvas.getDisplay().asyncExec(() -> {
						if (!swtCanvas.isDisposed())
							swtCanvas.setFocus();
					});
				}
				return true; // the NEWT window shall not request the focus
			});
		} catch (RuntimeException | Error e) {
			glWindow.destroy(); // the native window must not outlive the failure
			throw e;
		}
		canvas = created;
		canvas.setData(this);
		attachKeyboardInput(renderer);
		animator = new FPSAnimator(glWindow, FPS, true);
		canvas.addListener(SWT.Dispose, e -> close());
		try {
			animator.start();
		} catch (RuntimeException | Error e) {
			close();
			throw e;
		}
	}

	/**
	 * Connects the NEWT mouse events to the renderer commands. The NEWT window
	 * covers the SWT canvas (z-order), so the mouse events are received by NEWT :
	 * they are kept there and processed in the OpenGL thread.
	 */
	private void attachMouseInput(ChartInteraction interaction) {
		glWindow.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastX = e.getX();
				lastY = e.getY();
				if (e.getButton() == MouseEvent.BUTTON1)
					leftDown = true;
				if (e.getButton() == MouseEvent.BUTTON3) {
					rightDown = true;
					rightPressX = e.getX();
					rightPressY = e.getY();
					rightDragged = false;
				}
				int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getSurfaceWidth(),
						h = glWindow.getSurfaceHeight();
				// Like the drag, in the OpenGL thread: press, drag and release are processed
				// there in order
				int modifiers = modifiers(e);
				if (button != 0)
					glWindow.invoke(false, d -> {
						interaction.press(button, modifiers, x, y, w, h);
						return true;
					});
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// Keyboard focus as soon as the mouse enters the chart (several charts per
				// window),
				// on the SWT canvas (not on the NEWT window), only if our window is active so
				// as
				// not to steal the focus from another window or a dialog
				NewtCanvasSWT swtCanvas = canvas;
				if (swtCanvas == null || swtCanvas.isDisposed())
					return;
				swtCanvas.getDisplay().asyncExec(() -> {
					if (!swtCanvas.isDisposed() && swtCanvas.getShell() == swtCanvas.getDisplay().getActiveShell()) {
						swtCanvas.setFocus();
					}
				});
			}

			@Override
			public void mouseExited(MouseEvent e) {
				glWindow.invoke(false, d -> {
					interaction.exit();
					return true;
				});
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1)
					leftDown = false;
				if (e.getButton() == MouseEvent.BUTTON3)
					rightDown = false;
				int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getSurfaceWidth(),
						h = glWindow.getSurfaceHeight();
				if (button != 0)
					glWindow.invoke(false, d -> {
						interaction.release(button, x, y, w, h);
						return true;
					});
				// Right click without drag: context menu, opened in the SWT thread
				if (button == ChartInteraction.RIGHT_BUTTON && !rightDragged && !leftDown)
					showContextMenu(x, y);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2)
					return;
				int button = toButton(e), x = e.getX(), y = e.getY(), w = glWindow.getSurfaceWidth(),
						h = glWindow.getSurfaceHeight();
				int modifiers = modifiers(e);
				if (button != 0)
					glWindow.invoke(false, d -> {
						interaction.doubleClick(button, modifiers, x, y, w, h);
						return true;
					});
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				float[] scale = pixelScale();
				int dx = (int) ((e.getX() - lastX) / scale[0]);
				int dy = (int) ((e.getY() - lastY) / scale[1]);
				lastX = e.getX();
				lastY = e.getY();
				if (rightDown && (Math.abs(e.getX() - rightPressX) / scale[0] > CLICK_TOLERANCE
						|| Math.abs(e.getY() - rightPressY) / scale[1] > CLICK_TOLERANCE)) {
					rightDragged = true;
				}
				int button = leftDown ? ChartInteraction.LEFT_BUTTON : rightDown ? ChartInteraction.RIGHT_BUTTON : 0;
				if (button != 0) {
					int x = e.getX(), y = e.getY(), w = glWindow.getSurfaceWidth(), h = glWindow.getSurfaceHeight();
					// run in the OpenGL thread at the beginning of the next frame: no concurrent
					// access to the renderer
					glWindow.invoke(false, d -> {
						interaction.dragTo(button, x, y, dx, dy, w, h, ChartInteraction.InputScale.NEWT);
						return true;
					});
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				lastX = e.getX();
				lastY = e.getY();
				int x = e.getX(), y = e.getY(), w = glWindow.getSurfaceWidth(), h = glWindow.getSurfaceHeight();
				glWindow.invoke(false, d -> {
					interaction.move(x, y, w, h);
					return true;
				});
			}

			@Override
			public void mouseWheelMoved(MouseEvent e) {
				float amount = e.getRotation()[1];
				glWindow.invoke(false, d -> {
					interaction.wheel(amount, ChartInteraction.InputScale.NEWT);
					return true;
				});
			}
		});
	}

	/**
	 * Connects the SWT keyboard events of the canvas to the renderer commands, like
	 * {@link SWTGLHost} : when the SWT canvas has the keyboard focus (before the
	 * NEWT window takes it, see below), the key events are received by the SWT
	 * canvas.
	 */
	private void attachKeyboardInput(ChartInteraction interaction) {
		// Context menu with the keyboard (menu key, Shift + F10), opened at the mouse
		// position if it is over the chart, otherwise at its center (with the mouse,
		// the menu is opened by the NEWT mouse listener, see mouseReleased)
		canvas.addListener(SWT.MenuDetect, e -> {
			if (!hasContextMenuHandler())
				return; // menu of the SWT component, if the application gave one
			e.doit = false;
			if (e.detail != SWT.MENU_KEYBOARD)
				return;
			Point size = canvas.getSize();
			Point mouse = canvas.toControl(canvas.getDisplay().getCursorLocation());
			boolean inside = mouse.x >= 0 && mouse.y >= 0 && mouse.x < size.x && mouse.y < size.y;
			if (inside)
				requestContextMenu(mouse.x, mouse.y);
			else
				requestContextMenu(size.x / 2, size.y / 2);
		});

		// Tab: passed to the renderer if it uses it (cursor on several curves),
		// instead of moving the focus to the next component
		// The arrow keys stay in the chart (pan) instead of moving the focus
		canvas.addListener(SWT.Traverse, e -> {
			if ((e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
					&& interaction.usesTabKey()) {
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
				interaction
						.keyPressed((e.stateMask & SWT.SHIFT) != 0 ? ChartInteraction.BACK_TAB : ChartInteraction.TAB);
			} else {
				interaction.keyPressed(Character.toLowerCase(e.character));
			}
		});
	}

	/**
	 * Connects the NEWT key events to the renderer commands, and forwards them to
	 * SWT. On macOS, the NEWT window is a native child window that takes the
	 * keyboard focus when the chart is clicked, whatever
	 * {@code Window#setFocusAction} says (it only intercepts the focus requests
	 * made by NEWT itself) : the key events are then received by NEWT, not by SWT.
	 */
	private void attachNewtKeyboard(ChartInteraction interaction) {
		glWindow.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				char key = toKey(e);
				glWindow.invoke(false, d -> {
					interaction.keyPressed(key);
					return true;
				});
				// Forward to SWT the keys typed while the NEWT window has the keyboard focus,
				// so that the shortcuts of the application (Escape to stop a trial, F9...) keep
				// working : posted as a KeyUp event, they are received by the display filters
				// (like a key typed in any SWT control), without triggering the KeyDown
				// listener of the canvas (no double delivery to the renderer)
				if (!e.isAutoRepeat())
					forwardKeyUpToSWT(e);
			}
		});
	}

	/**
	 * Forwards a NEWT key event to the application key listeners (see
	 * {@link GLHost#addApplicationKeyListener(Listener)}), as an SWT KeyUp event,
	 * in the SWT thread : the NEWT window has the keyboard focus, so the display
	 * filters of the application do not see the key.
	 */
    private void forwardKeyUpToSWT(KeyEvent e) {
        short keyCode = e.getKeyCode();
        long when = e.getWhen();
        if (keyCode == lastForwardedKeyCode && when - lastForwardedWhen < 250) return;
        lastForwardedKeyCode = keyCode;
        lastForwardedWhen = when;
        NewtCanvasSWT swtCanvas = canvas;
        if (swtCanvas == null || swtCanvas.isDisposed()) return;
        Event event = new Event();
        event.type = SWT.KeyUp;
        event.display = swtCanvas.getDisplay();
        event.widget = swtCanvas;
        event.keyCode = toSWTKeyCode(e);
        event.character = toSWTCharacter(e);
        event.stateMask = toSWTStateMask(e);
        GLHost.notifyApplicationKeyListeners(event);
    }

	/** NEWT key code (same as the AWT virtual keys) -> SWT key code. */
	private static int toSWTKeyCode(KeyEvent e) {
		short keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_ESCAPE)
			return SWT.ESC;
		if (keyCode == KeyEvent.VK_F9)
			return SWT.F9;
		if (keyCode == KeyEvent.VK_F11)
			return SWT.F11;
		if (keyCode == KeyEvent.VK_PAUSE)
			return SWT.PAUSE;
		if (keyCode == KeyEvent.VK_ENTER)
			return SWT.CR;
		if (keyCode == KeyEvent.VK_BACK_SPACE)
			return SWT.BS;
		if (keyCode == KeyEvent.VK_TAB)
			return SWT.TAB;
		if (keyCode >= 'A' && keyCode <= 'Z')
			return Character.toLowerCase(keyCode);
		return keyCode;
	}

	/**
	 * Character of a NEWT key event -> character of an SWT key event (lower case).
	 */
	private static char toSWTCharacter(KeyEvent e) {
		if (e.isPrintableKey())
			return Character.toLowerCase(e.getKeyChar());
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			return SWT.ESC;
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			return SWT.CR;
		if (e.getKeyCode() == KeyEvent.VK_TAB)
			return SWT.TAB;
		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
			return SWT.BS;
		return 0;
	}

	/** Modifier keys of a NEWT key event -> SWT state mask (Command on macOS). */
	private static int toSWTStateMask(KeyEvent e) {
		int mask = 0;
		if (e.isShiftDown())
			mask |= SWT.SHIFT;
		if (e.isControlDown())
			mask |= SWT.CTRL;
		if (e.isMetaDown())
			mask |= SWT.COMMAND;
		if (e.isAltDown())
			mask |= SWT.ALT;
		return mask;
	}

	/**
	 * Character passed to the renderer: Escape, Backspace, Tab and the arrows by
	 * their code, the others in lower case.
	 */
	private static char toKey(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT)
			return ChartInteraction.ARROW_LEFT;
		if (e.getKeyCode() == KeyEvent.VK_RIGHT)
			return ChartInteraction.ARROW_RIGHT;
		if (e.getKeyCode() == KeyEvent.VK_UP)
			return ChartInteraction.ARROW_UP;
		if (e.getKeyCode() == KeyEvent.VK_DOWN)
			return ChartInteraction.ARROW_DOWN;
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			return ChartInteraction.ESCAPE;
		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
			return ChartInteraction.BACKSPACE;
		if (e.getKeyCode() == KeyEvent.VK_TAB)
			return e.isShiftDown() ? ChartInteraction.BACK_TAB : ChartInteraction.TAB;
		return Character.toLowerCase(e.getKeyChar());
	}

	/**
	 * SWT key code of an arrow key -> ChartInteraction.ARROW_..., 0 for the other
	 * keys.
	 */
	private static char toArrow(int keyCode) {
		switch (keyCode) {
		case SWT.ARROW_LEFT:
			return ChartInteraction.ARROW_LEFT;
		case SWT.ARROW_RIGHT:
			return ChartInteraction.ARROW_RIGHT;
		case SWT.ARROW_UP:
			return ChartInteraction.ARROW_UP;
		case SWT.ARROW_DOWN:
			return ChartInteraction.ARROW_DOWN;
		default:
			return 0;
		}
	}

	/** Requests the context menu at (x, y) (pixels), in the SWT thread. */
	private void showContextMenu(int x, int y) {
		NewtCanvasSWT swtCanvas = canvas;
		if (!hasContextMenuHandler() || swtCanvas == null || swtCanvas.isDisposed())
			return;
		float[] scale = pixelScale();
		int componentX = (int) (x / scale[0]), componentY = (int) (y / scale[1]);
		swtCanvas.getDisplay().asyncExec(() -> requestContextMenu(componentX, componentY));
	}

	/**
	 * Pixel scale of the surface, read at each use: it follows the window when it
	 * moves to a screen of another density.
	 */
	private float[] pixelScale() {
		return glWindow.getCurrentSurfaceScale(new float[2]);
	}

	/**
	 * NEWT button -> {@link ChartInteraction#LEFT_BUTTON} /
	 * {@link ChartInteraction#RIGHT_BUTTON}, 0 for the others.
	 */
	private static int toButton(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1)
			return ChartInteraction.LEFT_BUTTON;
		if (e.getButton() == MouseEvent.BUTTON3)
			return ChartInteraction.RIGHT_BUTTON;
		return 0;
	}

	/** Modifier keys of a NEWT mouse event: Ctrl, or Command on macOS. */
	private static int modifiers(MouseEvent e) {
		return (e.isControlDown() || e.isMetaDown()) ? ChartInteraction.CTRL : 0;
	}

	@Override
	public Control getControl() {
		return canvas;
	}

	@Override
	public void stopAnimation() {
		animator.stop();
	}

	@Override
	public void startAnimation() {
		animator.start();
	}

	@Override
	protected void destroy() {
		animator.stop();
		if (!canvas.isDisposed())
			canvas.dispose();
		glWindow.destroy();
	}
}