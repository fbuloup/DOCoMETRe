package fr.univamu.ism.docometre.chartxyz;

/**
 * Écouteur du curseur d'un graphe 1D ou 2D (touche C, voir
 * {@link PlotRenderer#addCursorListener}). Il est appelé chaque fois que le
 * point désigné par le curseur change : déplacement de la souris, changement
 * de courbe (Tabulation), mais aussi arrivée de nouvelles données ou
 * changement de vue sous une souris immobile.
 *
 * L'appel a lieu dans le thread d'affichage OpenGL (thread SWT avec
 * {@link GLToolkit#SWT}, thread de l'animateur avec {@link GLToolkit#NEWT}) :
 * pour agir sur des widgets SWT depuis l'écouteur, passer par Display.asyncExec.
 */
@FunctionalInterface
public interface CursorListener {

    /**
     * @param xy coordonnées {x, y} du point désigné, dans les unités des axes
     *           (en 1D : x = temps affiché sur l'axe, position dans le balayage
     *           en mode balayage), ou null quand le curseur est désactivé ou ne
     *           désigne plus aucun point (courbe vide ou hors de la vue)
     */
    void cursorMoved(float[] xy);
}
