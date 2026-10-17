package fr.univamu.ism.docometre.chartxyz;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;

/**
 * Textes des valeurs courantes (la dernière valeur de chaque courbe, une
 * ligne par courbe), propres à un renderer.
 *
 * Les valeurs sont rafraîchies au plus REFRESH_PERIOD_NS (10 fois par
 * seconde) : à 60 images/s, des chiffres changeant à chaque image seraient
 * illisibles. Tant qu'un nouveau texte n'est pas prêt (préparation
 * asynchrone du texte avec NEWT, voir {@link GLText}), le texte précédent
 * reste affiché : la valeur ne clignote pas.
 */
public final class CurrentValues {

    private static final long REFRESH_PERIOD_NS = 100_000_000L; // 0,1 s

    /** Par courbe : {texte affiché, texte en attente}. */
    private final Map<Object, String[]> texts = new WeakHashMap<>();
    private long lastRefresh = Long.MIN_VALUE / 2;

    /**
     * Textes à afficher, un par courbe (null pour une courbe sans données).
     *
     * @param value valeur courante formatée de la courbe de rang k, ou null si elle n'a pas de données
     */
    public String[] update(GLText text, ChartFont font, List<? extends CurveAppearance> curves, IntFunction<String> value) {
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
                state[0] = state[1]; // nouveau texte prêt : il remplace l'ancien
            }
            result[k] = state[0];
        }
        return result;
    }

    /** Formate une valeur avec 4 chiffres significatifs (notation scientifique pour les très grandes ou très petites valeurs). */
    public static String format(double value) {
        if (value == 0) return "0";
        double magnitude = Math.abs(value);
        if (magnitude >= 1e5 || magnitude < 1e-3) return String.format(Locale.US, "%.3e", value);
        return String.format(Locale.US, "%.4g", value);
    }
}
