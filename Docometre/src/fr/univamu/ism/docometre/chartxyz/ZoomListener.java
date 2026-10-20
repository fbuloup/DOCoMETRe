package fr.univamu.ism.docometre.chartxyz;

/**
 * Écouteur des changements de fenêtre de zoom d'un graphe 1D ou 2D (voir
 * {@link PlotRenderer#addZoomListener}).
 *
 * L'appel a lieu dans le thread qui a provoqué le changement : le thread
 * d'affichage OpenGL pour les commandes souris et clavier (thread SWT avec
 * {@link GLToolkit#SWT}, thread de l'animateur avec {@link GLToolkit#NEWT}),
 * le thread appelant pour les méthodes de l'API. Pour agir sur des widgets
 * SWT depuis l'écouteur, passer par Display.asyncExec.
 */
@FunctionalInterface
public interface ZoomListener {

    /**
     * @param window nouvelle fenêtre {xMin, xMax, yMin, yMax} (copie), ou
     *               null pour le retour à la vue initiale. En 1D, mode
     *               défilement, les abscisses sont relatives au dernier
     *               échantillon (négatives) : la fenêtre suit le temps.
     */
    void zoomChanged(double[] window);
}
