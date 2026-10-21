package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Base des graphes plans (1D et 2D) : ajoute la position de la légende, le
 * pilotage du zoom par rectangle et celui du curseur (voir {@link PlotRenderer}).
 */
public abstract class AbstractGLPlotChart<C extends CurveData<C>, R extends PlotRenderer<C>> extends AbstractGLChart<C, R> {

    protected AbstractGLPlotChart(Composite container, GLToolkit toolkit, R renderer) {
        super(container, toolkit, renderer, false);
    }

    /**
     * Position de la légende : LegendPosition.INSIDE (dans le graphe, en haut à
     * gauche, par défaut) ou LegendPosition.BELOW (tout en bas, sous l'axe des abscisses).
     */
    public void setLegendPosition(LegendPosition position) {
        renderer.setLegendPosition(position);
    }

    public LegendPosition getLegendPosition() {
        return renderer.getLegendPosition();
    }

    /** SWT.BOTTOM : légende sous l'axe des abscisses ; toute autre valeur : dans le graphe. */
    public void setLegendPosition(int position) {
        renderer.setLegendPosition(position == SWT.BOTTOM ? LegendPosition.BELOW : LegendPosition.INSIDE);
    }

    // ------------------------------------------------------------------
    // Zoom par rectangle

    /**
     * Applique une fenêtre de zoom, comme un rectangle tracé à la souris
     * (mémorisée dans l'historique). En 1D, mode défilement, les abscisses
     * sont relatives au dernier échantillon (par exemple -2 à -1 pour la
     * seconde avant-dernière) : la fenêtre suit le temps.
     *
     * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
     */
    public void setZoomWindow(double xMin, double xMax, double yMin, double yMax) {
        renderer.setZoomWindow(xMin, xMax, yMin, yMax);
    }

    /** Fenêtre de zoom courante {xMin, xMax, yMin, yMax} (copie), ou null si la vue est la vue initiale. */
    public double[] getZoomWindow() {
        return renderer.getZoomWindow();
    }

    /** Vrai si une fenêtre de zoom est appliquée. */
    public boolean isZoomed() {
        return renderer.isZoomed();
    }

    /** Revient à la fenêtre précédente (également avec U ou Retour arrière) ; false si l'historique est vide. */
    public boolean undoZoom() {
        return renderer.undoZoom();
    }

    /** Revient à la vue initiale et vide l'historique des zooms (également avec R). */
    public void resetZoom() {
        renderer.resetView();
    }

    /** Ajoute un écouteur des changements de fenêtre de zoom (voir {@link ZoomListener} pour le thread d'appel). */
    public void addZoomListener(ZoomListener listener) {
        renderer.addZoomListener(listener);
    }

    public void removeZoomListener(ZoomListener listener) {
        renderer.removeZoomListener(listener);
    }

    // ------------------------------------------------------------------
    // Curseur

    /** Active ou désactive le curseur (également avec la touche C) ; à l'activation, il suit la première courbe. */
    public void setCursorEnabled(boolean enabled) {
        renderer.setCursorEnabled(enabled);
    }

    public boolean isCursorEnabled() {
        return renderer.isCursorEnabled();
    }

    /** Courbe suivie par le curseur, par son rang (0 = première ; également avec Tabulation / Maj + Tabulation). */
    public void setCursorCurveIndex(int index) {
        renderer.setCursorCurveIndex(index);
    }

    public int getCursorCurveIndex() {
        return renderer.getCursorCurveIndex();
    }

    /** Coordonnées {x, y} du point désigné par le curseur (copie), ou null s'il n'y en a pas. */
    public float[] getCursorPosition() {
        return renderer.getCursorPosition();
    }

    /** Ajoute un écouteur du curseur (voir {@link CursorListener} pour le thread d'appel). */
    public void addCursorListener(CursorListener listener) {
        renderer.addCursorListener(listener);
    }

    public void removeCursorListener(CursorListener listener) {
        renderer.removeCursorListener(listener);
    }
}
