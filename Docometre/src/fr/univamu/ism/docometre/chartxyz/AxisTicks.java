package fr.univamu.ism.docometre.chartxyz;

import java.util.Locale;

/**
 * Axis ticks: "round" steps (1, 2 or 5 x 10^n), tick values within an
 * interval and label formatting.
 */
public final class AxisTicks {

    private AxisTicks() {
    }

    /** "Round" tick step (1, 2 or 5 x 10^n) closest to rawStep. */
    public static double niceStep(double rawStep) {
        if (!(rawStep > 0)) return 1;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;
        double nice = (normalized < 1.5) ? 1 : (normalized < 3.5) ? 2 : (normalized < 7.5) ? 5 : 10;
        return nice * magnitude;
    }

    /** Tick values that are multiples of step within [min, max] (at most 100). */
    public static double[] ticks(double min, double max, double step) {
        long first = (long) Math.ceil(min / step);
        int n = (int) Math.max(0, Math.min(100, Math.floor(max / step) - first + 1));
        double[] values = new double[n];
        for (int i = 0; i < n; i++) values[i] = (first + i) * step;
        return values;
    }

    /** Formats a tick with just enough decimals for the step used, avoiding "-0". */
    public static String format(double value, double step) {
        if (Math.abs(value) < step * 1e-6) value = 0;
        if (step >= 1e6 || step < 1e-4) return String.format(Locale.US, "%.3g", value);
        int decimals = (int) Math.max(0, -Math.floor(Math.log10(step)));
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    /** Formats a value with 2 decimals, avoiding the display of "-0.00" (ticks of the 3D boxes). */
    public static String format2(float value) {
        if (Math.abs(value) < 0.005f) value = 0f;
        return String.format(Locale.US, "%.2f", value);
    }
}
