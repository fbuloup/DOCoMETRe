package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.rtswtchart.IRTSWTOscilloChart;

/**
 * Graphe de signaux temps réel y(t) (voir {@link Chart1DRenderer}). La
 * durée de la fenêtre temporelle appartient au graphe et s'impose à toutes
 * ses courbes.
 */
public class GLChart1D extends AbstractGLPlotChart<Curve1DData, Chart1DRenderer> implements IRTSWTOscilloChart {

    /**
     * Crée le graphe avec une ou plusieurs courbes (d'autres peuvent être
     * ajoutées ensuite avec addCurve), avec une échelle verticale dynamique :
     * elle s'ajuste en continu aux valeurs des courbes.
     *
     * @param windowTimeWidth durée de la fenêtre affichée, en secondes
     * @throws IllegalStateException si une courbe est déjà affichée dans un autre graphe
     */
    public GLChart1D(Composite container, GLToolkit toolkit, double windowTimeWidth, Curve1DData... curves) {
        super(container, toolkit, new Chart1DRenderer(windowTimeWidth, curves));
    }

    /**
     * Crée le graphe avec une échelle fixe pour l'axe vertical : elle reste
     * égale aux bornes fournies quelles que soient les valeurs des courbes
     * (ce qui dépasse est coupé). setDynamicScales() permet de revenir à
     * l'échelle dynamique.
     *
     * @param windowTimeWidth durée de la fenêtre affichée, en secondes
     * @throws IllegalArgumentException si yMin n'est pas strictement inférieur à yMax
     */
    public GLChart1D(Composite container, GLToolkit toolkit, double windowTimeWidth, double yMin, double yMax,
                     Curve1DData... curves) {
        super(container, toolkit, withScales(new Chart1DRenderer(windowTimeWidth, curves), yMin, yMax));
    }

    private static Chart1DRenderer withScales(Chart1DRenderer renderer, double yMin, double yMax) {
        renderer.setFixedScales(yMin, yMax);
        return renderer;
    }

    /** Choisit le mode d'affichage : balayage de type oscilloscope (par défaut) ou défilement. */
    public void setDisplayMode(Chart1DRenderer.DisplayMode displayMode) {
        renderer.setDisplayMode(displayMode);
    }

    /** Échelle fixe pour l'axe vertical (voir le constructeur correspondant). */
    public void setFixedScales(double yMin, double yMax) {
        renderer.setFixedScales(yMin, yMax);
    }

    /** Durée de la fenêtre affichée, en secondes, imposée à toutes les courbes du graphe. */
    @Override
    public void setWindowTimeWidth(double value) {
        renderer.setWindowTimeWidth(value);
    }

    public double getWindowTimeWidth() {
        return renderer.getWindowTimeWidth();
    }

    // --- IRTSWTOscilloChart : réglages non gérés ---

    @Override
    public void setShowCurrentValue(boolean value) {
    }

    @Override
    public void dispose() {
    }
}
