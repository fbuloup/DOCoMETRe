package fr.univamu.ism.docometre.chartxyz;

/**
 * Display options that can be toggled with the keyboard or the API, reported
 * to the {@link DisplayOptionListener}s (key between parentheses).
 */
public enum DisplayOption {
    /** Axes (A). */
    AXES,
    /** Grid (G). */
    GRID,
    /** Tick labels, and abscissa axis label in 1D (L). */
    LABELS,
    /** Points of the curves (P). */
    POINTS,
    /** Legend (N). */
    LEGEND,
    /** Frame rate (F). */
    FPS,
    /** Current values of the curves (V). */
    CURRENT_VALUES,
    /** Reduction of the number of points drawn (D). */
    DECIMATION,
    /** Cursor mode, 1D and 2D charts (C). */
    CURSOR,
    /** Pause of the display, 1D charts of real-time signals (space). */
    PAUSE,
    /** Solid surface, surfaces (S). */
    SOLID,
    /** Wireframe, surfaces (W). */
    WIREFRAME,
    /** Lighting, surfaces (H). */
    LIGHTING
}
