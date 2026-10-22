package fr.univamu.ism.docometre.chartxyz;

/**
 * Mouse and keyboard commands of a chart, independent of the toolkit that
 * produces the events (SWT or NEWT, see {@link GLHost}). Implemented by the
 * renderers.
 *
 * Mouse coordinates are expressed in window units, origin at the top left;
 * width and height are the size of the component in the same units.
 */
public interface ChartInteraction {

    /** Left mouse button. */
    int LEFT_BUTTON = 1;
    /** Right mouse button. */
    int RIGHT_BUTTON = 3;

    /** Character passed to {@link #keyPressed} for the Escape key. */
    char ESCAPE = '\u001b';
    /** Character passed to {@link #keyPressed} for the Backspace key. */
    char BACKSPACE = '\b';
    /** Character passed to {@link #keyPressed} for the Tab key. */
    char TAB = '\t';
    /** Character passed to {@link #keyPressed} for Shift + Tab (private use character). */
    char BACK_TAB = '\uE000';

    /**
     * Toolkit-specific factors: mouse displacements and wheel steps do not
     * have the same amplitude in each toolkit.
     */
    enum InputScale {
        SWT(0.5f, 0.1f),
        NEWT(0.05f, 0.01f);

        /** Rotation of the 3D camera, in degrees per pixel of displacement. */
        public final float degreesPerPixel;
        /** Change of the 3D camera distance per wheel unit. */
        public final float zoomPerWheelUnit;

        InputScale(float degreesPerPixel, float zoomPerWheelUnit) {
            this.degreesPerPixel = degreesPerPixel;
            this.zoomPerWheelUnit = zoomPerWheelUnit;
        }
    }

    /**
     * Mouse moved with a button pressed.
     *
     * @param button        {@link #LEFT_BUTTON} or {@link #RIGHT_BUTTON} (the left one wins if both are pressed)
     * @param dx            horizontal displacement, in window units
     * @param dy            vertical displacement (downwards), in window units
     * @param width         width of the component, in the same units
     * @param height        height of the component, in the same units
     */
    void drag(int button, int dx, int dy, int width, int height, InputScale scale);

    /**
     * Mouse moved with a button pressed, with its position (x, y) after the
     * move. By default, only the displacement is used ({@link #drag}).
     */
    default void dragTo(int button, int x, int y, int dx, int dy, int width, int height, InputScale scale) {
        drag(button, dx, dy, width, height, scale);
    }

    /** Mouse moved without a button pressed, up to (x, y). No effect by default. */
    default void move(int x, int y, int width, int height) {
    }

    /**
     * True if the Tab key must be passed to {@link #keyPressed} rather than
     * moving the keyboard focus to the next component (checked by the SWT
     * host at each key press). False by default.
     */
    default boolean usesTabKey() {
        return false;
    }

    /** Double click at (x, y) (after the press / release events of both clicks). No effect by default. */
    default void doubleClick(int button, int x, int y, int width, int height) {
    }

    /** Button pressed at (x, y). No effect by default. */
    default void press(int button, int x, int y, int width, int height) {
    }

    /** Button released at (x, y) (possibly outside the component). No effect by default. */
    default void release(int button, int x, int y, int width, int height) {
    }

    /** Wheel rotation, in the toolkit unit: amount > 0 upwards (zoom in). */
    void wheel(float amount, InputScale scale);

    /**
     * Keyboard key (lower-case character, {@link #ESCAPE}, {@link #BACKSPACE},
     * {@link #TAB} or {@link #BACK_TAB}); no effect if it is not handled.
     */
    void keyPressed(char key);
}
