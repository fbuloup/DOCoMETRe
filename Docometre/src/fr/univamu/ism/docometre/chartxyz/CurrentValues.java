package fr.univamu.ism.docometre.chartxyz;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;

/**
 * Texts of the current values (the last value of each curve, one row per
 * curve), specific to a renderer.
 *
 * The values are refreshed at most every REFRESH_PERIOD_NS (10 times per
 * second): at 60 frames/s, digits changing at each frame would be
 * unreadable. As long as a new text is not ready (asynchronous text
 * preparation with NEWT, see {@link GLText}), the previous text remains
 * displayed: the value does not flicker.
 */
public final class CurrentValues {

    private static final long REFRESH_PERIOD_NS = 100_000_000L; // 0.1 s

    /** Per curve: {displayed text, pending text}. */
    private final Map<Object, String[]> texts = new WeakHashMap<>();
    private long lastRefresh = Long.MIN_VALUE / 2;

    /**
     * Texts to display, one per curve (null for a curve without data).
     *
     * @param value formatted current value of the curve at position k, or null if it has no data
     */
    String[] update(GLText text, ChartFont font, List<? extends CurveAppearance> curves, IntFunction<String> value) {
        long now = System.nanoTime();
        boolean refresh = now - lastRefresh >= REFRESH_PERIOD_NS;
        if (refresh) lastRefresh = now;

        String[] result = new String[curves.size()];
        for (int k = 0; k < curves.size(); k++) {
            CurveAppearance curve = curves.get(k);
            String[] state = texts.computeIfAbsent(curve, c -> new String[2]);
            if (refresh || state[1] == null) {
                String v = value.apply(k);
                state[1] = v;
            }
            if (state[1] == null) {
                state[0] = null;
            } else if (!state[1].equals(state[0]) && text.prepare(state[1], font)) {
                state[0] = state[1]; // new text ready: it replaces the old one
            }
            result[k] = state[0];
        }
        return result;
    }

    /** Formats a value with 4 significant digits (scientific notation for very large or very small values). */
    public static String format(double value) {
        if (value == 0) return "0";
        double magnitude = Math.abs(value);
        if (magnitude >= 1e5 || magnitude < 1e-3) return String.format(Locale.US, "%.3e", value);
        return String.format(Locale.US, "%.4g", value);
    }
}
