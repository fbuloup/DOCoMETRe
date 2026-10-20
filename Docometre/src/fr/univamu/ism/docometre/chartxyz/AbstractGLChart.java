package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.rtswtchart.IRTSWTSerie;

/**
 * Base des graphes de courbes temps réel intégrés dans une interface SWT :
 * une surface OpenGL ({@link GLHost}, SWT ou NEWT selon {@link GLToolkit})
 * et un renderer ({@link CurveChartRenderer}) dont les réglages sont exposés ici.
 *
 * Les méthodes des interfaces fr.univamu.ism.rtswtchart communes à tous les
 * graphes sont aussi implémentées ici (les sous-classes déclarent l'interface).
 *
 * @param <C> type des courbes
 * @param <R> type du renderer
 */
public abstract class AbstractGLChart<C extends CurveData<C>, R extends CurveChartRenderer<C>> implements GLComponent {

    protected final R renderer;
    protected final GLHost host;

    /**
     * Crée la surface OpenGL dans container et démarre l'affichage. À la
     * fermeture du graphe, ses courbes en sont retirées : elles peuvent
     * alors être affichées dans un autre graphe.
     *
     * @param renderer    renderer déjà configuré (courbes, échelles)
     * @param depthBuffer true pour une scène 3D
     */
    protected AbstractGLChart(Composite container, GLToolkit toolkit, R renderer, boolean depthBuffer) {
        this.renderer = renderer;
        try {
			this.host = GLHost.create(toolkit, container, depthBuffer, renderer, renderer::removeAllCurves);
		} catch (RuntimeException | Error e) {
			renderer.removeAllCurves();
			throw e;
		}
    }

    /** Renderer du graphe, pour les réglages qui ne sont pas exposés ici. */
    public R getRenderer() { return renderer; }

    @Override
    public void close() {
        host.close();
    }

    /** Arrête l'animation : l'affichage n'est plus rafraîchi. */
    public void stopAnimator() {
        host.stopAnimation();
    }

    @Override
    public boolean isDisposed() {
        return host.isDisposed();
    }

    @Override
    public void moveAbove(Control control) {
        host.getControl().moveAbove(control);
    }

    @Override
    public void setLayoutData(GridData gridData) {
        host.getControl().setLayoutData(gridData);
    }

    public GridData getLayoutData() {
        Object data = host.getControl().getLayoutData();
        return (data instanceof GridData) ? (GridData) data : null;
    }

    /**
     * Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread).
     *
     * @throws IllegalStateException si la courbe est déjà affichée dans un graphe
     */
    public void addCurve(C curve) {
        renderer.addCurve(curve);
    }

    /** Retire une courbe du graphe. */
    public void removeCurve(C curve) {
        renderer.removeCurve(curve);
    }

    /**
     * Police des graduations des échelles ; les noms d'axes utilisent la même
     * famille, en gras et plus grande.
     *
     * @param family famille ("Arial", "Helvetica", "Courier New"...) ou null pour la police du système
     * @param size   taille en points
     * @param style  SWT.NORMAL, SWT.BOLD, SWT.ITALIC ou SWT.BOLD | SWT.ITALIC
     */
    public void setScaleFont(String family, int size, int style) {
        renderer.setScaleFont(new ChartFont(family, size, style));
    }

    /** Police de la légende (mêmes paramètres que {@link #setScaleFont}). */
    public void setLegendFont(String family, int size, int style) {
        renderer.setLegendFont(new ChartFont(family, size, style));
    }

    /** Échelles dynamiques, ajustées en continu aux valeurs des courbes (par défaut). */
    public void setDynamicScales() {
        renderer.setDynamicScales();
    }

    /** Vrai si les échelles sont fixes. */
    public boolean hasFixedScales() {
        return renderer.hasFixedScales();
    }

    /** Affiche ou masque la légende (également possible avec la touche N quand le graphe a le focus). */
    public void setLegendVisible(boolean visible) {
        renderer.setLegendVisible(visible);
    }

    public boolean isLegendVisible() {
        return renderer.isLegendVisible();
    }

    /** Cadence d'affichage réelle mesurée, en images par seconde (cible : {@link GLHost#FPS}). */
    public double getFps() {
        return renderer.getFps();
    }

    /** Affiche ou masque la cadence d'affichage dans le graphe (également avec la touche F). */
    public void setFpsVisible(boolean visible) {
        renderer.setFpsVisible(visible);
    }

    /** Affiche ou masque, en haut à droite du graphe, la dernière valeur de chaque courbe (également avec la touche V). */
    public void setCurrentValuesVisible(boolean visible) {
        renderer.setCurrentValuesVisible(visible);
    }

    public boolean isCurrentValuesVisible() {
        return renderer.isCurrentValuesVisible();
    }

    /**
     * Active ou désactive la réduction du nombre de points envoyés à OpenGL
     * (activée par défaut, également avec la touche D). Le dessin est inchangé.
     */
    public void setDecimationEnabled(boolean enabled) {
        renderer.setDecimationEnabled(enabled);
    }

    public boolean isDecimationEnabled() {
        return renderer.isDecimationEnabled();
    }

    // ------------------------------------------------------------------
    // Méthodes communes des interfaces fr.univamu.ism.rtswtchart

    public void setAutoScale(boolean autoscale) {
        if (autoscale) setDynamicScales();
    }

    public void setGridVisibility(boolean value) {
        renderer.setGridVisibility(value);
    }

    public void setLegendVisibility(boolean value) {
        setLegendVisible(value);
    }

    /** Non géré : les bornes se fixent ensemble avec setFixedScales(). */
    public void setyMax(double value) {
    }

    /** Non géré : les bornes se fixent ensemble avec setFixedScales(). */
    public void setyMin(double value) {
    }

    /** Non géré : couleur fixe. */
    public void setGridLinesColor(Color color) {
    }

    /** Non géré : couleur fixe. */
    public void setFontColor(Color color) {
    }

    /** Non géré : les courbes sont créées par l'appelant puis ajoutées avec addCurve(). */
    public IRTSWTSerie createSerie(String serieId, Color color) {
    	throw new UnsupportedOperationException("utiliser addCurve(...)");
    }
}
