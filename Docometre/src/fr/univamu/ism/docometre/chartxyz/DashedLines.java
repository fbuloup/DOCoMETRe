package fr.univamu.ism.docometre.chartxyz;

import java.util.Map;
import java.util.WeakHashMap;

import com.jogamp.opengl.GL2;

/**
 * Tracé des courbes en tirets (DASH, DASH_DOT, DASH_DASH_DOT) avec un motif
 * ancré sur les données.
 *
 * Le pointillé natif d'OpenGL (glLineStipple) recommence le motif au premier
 * sommet de chaque glBegin. En temps réel, ce premier sommet est le plus
 * ancien échantillon de la fenêtre, qui change à chaque image : les tirets
 * semblent alors glisser le long de la courbe. Ici, chaque échantillon
 * reçoit une fois pour toutes, à son arrivée, sa position dans le motif
 * (longueur cumulée du tracé à l'écran, en pixels) : les tirets restent
 * attachés aux données.
 *
 * Les longueurs sont mesurées à l'échelle d'affichage du moment où les
 * échantillons arrivent. Si l'échelle change ensuite fortement (zoom,
 * ajustement automatique, facteur supérieur à 2), toutes les positions sont
 * recalculées, ce qui provoque un unique saut du motif.
 *
 * Une instance est propre à un renderer et doit être utilisée depuis son
 * thread d'affichage.
 */
public final class DashedLines {

    /** Émet le sommet situé entre les sommets i-1 et i de l'instantané, à la fraction f (0 = i-1, 1 = i). */
    public interface Emitter {
        void vertex(int i, float f);
    }

    /** Longueur à l'écran, en pixels, du segment entre les sommets i-1 et i de l'instantané. */
    public interface SegmentLength {
        double pixels(int i);
    }

    /** Indique si le segment entre les sommets i-1 et i ne doit pas être dessiné. */
    public interface SegmentFilter {
        boolean skip(int i);
    }

    /** Facteur d'échelle au-delà duquel les positions dans le motif sont recalculées. */
    private static final double RESCALE_RATIO = 2.0;

    /** Positions dans le motif des échantillons d'une courbe. */
    private static final class Phases {
        final double[] cumulative; // longueur cumulée (pixels), indexée par numéro absolu d'échantillon % capacité
        long last = -1;            // numéro absolu du dernier échantillon traité
        double referenceScaleX, referenceScaleY;

        Phases(int capacity) {
            cumulative = new double[capacity];
        }
    }

    private final Map<Object, Phases> phasesByCurve = new WeakHashMap<>();

    /**
     * Dessine en tirets une courbe de l'instantané courant.
     *
     * @param curve        la courbe (son type de trait doit être autre que SOLID)
     * @param width        épaisseur du trait en pixels (le motif y est proportionnel)
     * @param capacity     capacité du tampon de la courbe
     * @param totalSamples nombre total d'échantillons reçus au moment de l'instantané
     * @param n            nombre de points de l'instantané
     * @param from         premier point à dessiner (les points précédents gardent leur position dans le motif)
     * @param scaleX       échelle horizontale d'affichage (pixels par unité), sert à détecter les changements d'échelle
     * @param scaleY       échelle verticale d'affichage (pixels par unité)
     * @param length       longueur à l'écran de chaque segment, à l'échelle actuelle
     * @param filter       segments à ne pas dessiner (null = tous)
     * @param emitter      émission des sommets (couleur comprise)
     */
    public void draw(GL2 gl, CurveAppearance curve, float width, int capacity, long totalSamples, int n, int from,
                     double scaleX, double scaleY, SegmentLength length, SegmentFilter filter, Emitter emitter) {
        Phases phases = update(curve, capacity, totalSamples, n, scaleX, scaleY, length);
        if (n < 2) return;

        short pattern = curve.getLineStyle().getPattern();
        double unit = unitLength(width);
        long first = totalSamples - n;
        boolean[] open = {false};
        for (int i = Math.max(from, 0) + 1; i < n; i++) {
            if (filter != null && filter.skip(i)) {
                if (open[0]) {
                    gl.glEnd();
                    open[0] = false;
                }
                continue;
            }
            double p0 = phases.cumulative[index(first + i - 1, capacity)];
            double p1 = phases.cumulative[index(first + i, capacity)];
            walkSegment(gl, pattern, unit, p0, p1 - p0, i, emitter, open);
        }
        if (open[0]) gl.glEnd();
    }

    /**
     * Dessine en tirets le segment droit (x0, y0) - (x1, y1), le motif
     * commençant en (x0, y0). Sert à la légende (coordonnées en pixels).
     */
    public static void drawSegment(GL2 gl, LineStyle style, float width, float x0, float y0, float x1, float y1) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        boolean[] open = {false};
        walkSegment(gl, style.getPattern(), unitLength(width), 0, length, 1,
                    (i, f) -> gl.glVertex2f(x0 + f * (x1 - x0), y0 + f * (y1 - y0)), open);
        if (open[0]) gl.glEnd();
    }

    /** Longueur en pixels d'un caractère du motif : proportionnelle à l'épaisseur du trait. */
    private static double unitLength(float width) {
        return LineStyle.BASE_FACTOR * Math.max(1, Math.round(width));
    }

    /** Calcule la position dans le motif des échantillons arrivés depuis l'image précédente. */
    private Phases update(Object curve, int capacity, long totalSamples, int n,
                          double scaleX, double scaleY, SegmentLength length) {
        Phases phases = phasesByCurve.get(curve);
        if (phases == null || phases.cumulative.length != capacity) {
            phases = new Phases(capacity);
            phasesByCurve.put(curve, phases);
        }
        if (n == 0) {
            phases.last = -1;
            return phases;
        }
        long first = totalSamples - n;
        long lastAbs = totalSamples - 1;
        boolean rescaled = ratio(scaleX, phases.referenceScaleX) > RESCALE_RATIO
                        || ratio(scaleY, phases.referenceScaleY) > RESCALE_RATIO;

        long start;
        if (phases.last < 0 || phases.last < first - 1 || phases.last > lastAbs || rescaled) {
            // Premier affichage, échantillons perdus entre deux images, clear() ou changement d'échelle : on repart de zéro
            phases.cumulative[index(first, capacity)] = 0;
            phases.referenceScaleX = scaleX;
            phases.referenceScaleY = scaleY;
            start = first + 1;
        } else {
            start = phases.last + 1;
        }
        for (long abs = start; abs <= lastAbs; abs++) {
            phases.cumulative[index(abs, capacity)] = phases.cumulative[index(abs - 1, capacity)]
                    + length.pixels((int) (abs - first));
        }
        phases.last = lastAbs;
        return phases;
    }

    private static double ratio(double a, double b) {
        if (!(a > 0) || !(b > 0)) return 1; // échelle inconnue : pas de recalcul
        return Math.max(a / b, b / a);
    }

    private static int index(long absoluteSample, int capacity) {
        return (int) (absoluteSample % capacity);
    }

    /**
     * Parcourt un segment de longueur len (pixels) commençant à la position
     * p0 dans le motif, et émet les portions allumées sous forme de
     * GL_LINE_STRIP. open[0] indique si un tronçon est ouvert : il reste
     * ouvert d'un segment au suivant tant que le motif est allumé, pour que
     * les tirets épais n'aient pas de coupure aux sommets.
     */
    private static void walkSegment(GL2 gl, short pattern, double unit, double p0, double len,
                                    int i, Emitter emitter, boolean[] open) {
        if (!(len > 0)) return;
        double period = 16 * unit;
        double pos = p0 % period;
        if (pos < 0) pos += period;
        double done = 0;
        while (true) {
            int u = (int) (pos / unit);
            if (u >= 16) { // garde-fou numérique
                u = 0;
                pos -= period;
            }
            boolean on = ((pattern >> u) & 1) != 0;
            if (on && !open[0]) {
                gl.glBegin(GL2.GL_LINE_STRIP);
                open[0] = true;
                emitter.vertex(i, (float) (done / len));
            } else if (!on && open[0]) {
                gl.glEnd();
                open[0] = false;
            }
            double step = Math.min((u + 1) * unit - pos, len - done);
            done += step;
            pos += step;
            if (done >= len) break;
            // Frontière de caractère atteinte à l'intérieur du segment
            boolean nextOn = ((pattern >> ((u + 1) % 16)) & 1) != 0;
            if (open[0] && !nextOn) {
                emitter.vertex(i, (float) (done / len));
                gl.glEnd();
                open[0] = false;
            }
            if (pos >= period) pos -= period;
        }
        if (open[0]) emitter.vertex(i, 1f);
    }
}
