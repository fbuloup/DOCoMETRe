package fr.univamu.ism.docometre.chartxyz;

/**
 * Commandes souris et clavier d'un graphe, indépendantes de la boîte à
 * outils qui produit les événements (SWT ou NEWT, voir {@link GLHost}).
 * Implémentée par les renderers.
 *
 * Les coordonnées de la souris sont exprimées en unités de la fenêtre,
 * origine en haut à gauche ; width et height sont la taille du composant
 * dans les mêmes unités.
 */
public interface ChartInteraction {

    /** Bouton gauche de la souris. */
    int LEFT_BUTTON = 1;
    /** Bouton droit de la souris. */
    int RIGHT_BUTTON = 3;

    /** Caractère transmis à {@link #keyPressed} pour la touche Échap. */
    char ESCAPE = '\u001b';
    /** Caractère transmis à {@link #keyPressed} pour la touche Retour arrière. */
    char BACKSPACE = '\b';
    /** Caractère transmis à {@link #keyPressed} pour la touche Tabulation. */
    char TAB = '\t';
    /** Caractère transmis à {@link #keyPressed} pour Maj + Tabulation (caractère à usage privé). */
    char BACK_TAB = '\uE000';

    /**
     * Facteurs propres à la boîte à outils : les déplacements de souris et
     * les crans de molette n'y ont pas la même amplitude.
     */
    enum InputScale {
        SWT(0.5f, 0.1f),
        NEWT(0.05f, 0.01f);

        /** Rotation de la caméra 3D, en degrés par pixel de déplacement. */
        public final float degreesPerPixel;
        /** Variation de distance de la caméra 3D par unité de molette. */
        public final float zoomPerWheelUnit;

        InputScale(float degreesPerPixel, float zoomPerWheelUnit) {
            this.degreesPerPixel = degreesPerPixel;
            this.zoomPerWheelUnit = zoomPerWheelUnit;
        }
    }

    /**
     * Déplacement de la souris, bouton enfoncé.
     *
     * @param button        {@link #LEFT_BUTTON} ou {@link #RIGHT_BUTTON} (le gauche l'emporte si les deux sont enfoncés)
     * @param dx            déplacement horizontal, en unités de la fenêtre
     * @param dy            déplacement vertical (vers le bas), en unités de la fenêtre
     * @param width         largeur du composant, dans les mêmes unités
     * @param height        hauteur du composant, dans les mêmes unités
     */
    void drag(int button, int dx, int dy, int width, int height, InputScale scale);

    /**
     * Déplacement de la souris bouton enfoncé, avec sa position (x, y) après
     * le déplacement. Par défaut, seul le déplacement est utilisé ({@link #drag}).
     */
    default void dragTo(int button, int x, int y, int dx, int dy, int width, int height, InputScale scale) {
        drag(button, dx, dy, width, height, scale);
    }

    /** Déplacement de la souris sans bouton enfoncé, jusqu'en (x, y). Sans effet par défaut. */
    default void move(int x, int y, int width, int height) {
    }

    /**
     * Vrai si la touche Tabulation doit être transmise à {@link #keyPressed}
     * plutôt que de faire passer le focus clavier au composant suivant
     * (consulté par l'hôte SWT à chaque appui). Faux par défaut.
     */
    default boolean usesTabKey() {
        return false;
    }

    /** Bouton enfoncé en (x, y). Sans effet par défaut. */
    default void press(int button, int x, int y, int width, int height) {
    }

    /** Bouton relâché en (x, y) (éventuellement hors du composant). Sans effet par défaut. */
    default void release(int button, int x, int y, int width, int height) {
    }

    /** Rotation de la molette, dans l'unité de la boîte à outils : amount > 0 vers le haut (zoom avant). */
    void wheel(float amount, InputScale scale);

    /**
     * Touche du clavier (caractère en minuscule, {@link #ESCAPE},
     * {@link #BACKSPACE}, {@link #TAB} ou {@link #BACK_TAB}) ; sans effet si
     * elle n'est pas gérée.
     */
    void keyPressed(char key);
}
