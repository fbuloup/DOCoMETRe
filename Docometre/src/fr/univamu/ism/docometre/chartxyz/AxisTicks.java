package fr.univamu.ism.docometre.chartxyz;

import java.util.Locale;

/**
 * Graduations des axes : pas "ronds" (1, 2 ou 5 x 10^n), valeurs des
 * graduations dans un intervalle et formatage des étiquettes.
 */
public final class AxisTicks {

    private AxisTicks() {
    }

    /** Pas de graduation "rond" (1, 2 ou 5 x 10^n) le plus proche de rawStep. */
    public static double niceStep(double rawStep) {
        if (!(rawStep > 0)) return 1;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;
        double nice = (normalized < 1.5) ? 1 : (normalized < 3.5) ? 2 : (normalized < 7.5) ? 5 : 10;
        return nice * magnitude;
    }

    /** Valeurs des graduations multiples de step comprises dans [min, max] (au plus 100). */
    public static double[] ticks(double min, double max, double step) {
        long first = (long) Math.ceil(min / step);
        int n = (int) Math.max(0, Math.min(100, Math.floor(max / step) - first + 1));
        double[] values = new double[n];
        for (int i = 0; i < n; i++) values[i] = (first + i) * step;
        return values;
    }

    /** Formate une graduation avec juste assez de décimales pour le pas utilisé, en évitant "-0". */
    public static String format(double value, double step) {
        if (Math.abs(value) < step * 1e-6) value = 0;
        if (step >= 1e6 || step < 1e-4) return String.format(Locale.US, "%.3g", value);
        int decimals = (int) Math.max(0, -Math.floor(Math.log10(step)));
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    /** Formate une valeur avec 2 décimales, en évitant l'affichage "-0.00" (graduations des boîtes 3D). */
    public static String format2(float value) {
        if (Math.abs(value) < 0.005f) value = 0f;
        return String.format(Locale.US, "%.2f", value);
    }
}
