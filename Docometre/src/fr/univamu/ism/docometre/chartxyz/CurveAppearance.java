package fr.univamu.ism.docometre.chartxyz;

/**
 * Apparence d'une courbe dans un graphe à plusieurs courbes : nom affiché
 * dans la légende, couleur, épaisseur et type de trait. Implémentée par {@link Curve1DData},
 * {@link Curve2DData} et {@link Curve3DData}.
 */
public interface CurveAppearance {

    /** Nom affiché dans la légende, ou null. */
    String getName();

    /** Couleur RGB (composantes entre 0 et 1), ou null pour une couleur automatique. */
    float[] getColor();

    /** Épaisseur du trait en pixels, ou 0 pour l'épaisseur par défaut du graphe. */
    float getLineWidth();

    /** Type de tracé (continu, tirets...). */
    LineStyle getLineStyle();
}
