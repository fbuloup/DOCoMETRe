package fr.univamu.ism.docometre.chartxyz;

/**
 * Type de tracé d'une courbe. Les motifs sont dessinés par {@link DashedLines},
 * ancrés sur les données (ils ne glissent pas le long d'une courbe temps
 * réel), et mis à l'échelle de l'épaisseur du trait : tirets et points
 * gardent les mêmes proportions quelle que soit l'épaisseur.
 */
public enum LineStyle {

    /** Trait continu. */
    SOLID(null),
    /** Tirets :            ________  ________  ________ */
    DASH("1111111100000000"),
    /** Tiret-point :       ________ .  ________ .  ____ */
    DASH_DOT("1111111100011000"),
    /** Tiret-tiret-point : ____ ____ .  ____ ____ .  __ */
    DASH_DASH_DOT("1111001111001000");

    /** Nombre de pixels représentés par un caractère du motif, pour un trait de 1 pixel. */
    static final int BASE_FACTOR = 2;

    private final short pattern;

    /**
     * @param pattern motif de 16 caractères ('1' = allumé, '0' = éteint),
     *                lu de gauche à droite dans le sens du tracé ; null = trait continu
     */
    LineStyle(String pattern) {
        short bits = 0;
        if (pattern != null) {
            // Le motif est lu à partir du bit de poids faible
            for (int i = 0; i < 16; i++) {
                if (pattern.charAt(i) == '1') bits |= (short) (1 << i);
            }
        }
        this.pattern = bits;
    }

    /** Motif 16 bits, le bit 0 étant le premier dessiné (sans objet pour SOLID). */
    public short getPattern() { return pattern; }

    public boolean isSolid() { return this == SOLID; }
}
