package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.rtswtchart.IRTSWTXYChart;

/**
 * Graphe de courbes 2D temps réel (voir {@link Chart2DRenderer}).
 */
public class GLChart2D extends AbstractGLPlotChart<Curve2DData, Chart2DRenderer> implements IRTSWTXYChart {

    /**
     * Crée le graphe avec des échelles dynamiques, ajustées en continu aux valeurs des courbes.
     *
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     * @throws IllegalStateException si une courbe est déjà affichée dans un autre graphe
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double historySize, Curve2DData... curves) {
        super(container, toolkit, new Chart2DRenderer(historySize, curves));
    }

    /**
     * Crée le graphe avec des échelles fixes pour les axes x et y (voir
     * {@link #GLChart2D(Composite, GLToolkit, double, double, double, double, Curve2DData...)}).
     *
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double historySize, double xMin, double xMax,
                     double yMin, double yMax, Curve2DData... curves) {
        super(container, toolkit, withScales(new Chart2DRenderer(historySize, curves), xMin, xMax, yMin, yMax));
    }

    /**
     * Crée le graphe avec des échelles dynamiques ; chaque courbe garde sa
     * propre durée d'historique (données statiques, par exemple).
     */
    public GLChart2D(Composite container, GLToolkit toolkit, Curve2DData... curves) {
        super(container, toolkit, new Chart2DRenderer(curves));
    }

    /**
     * Crée le graphe avec des échelles fixes pour les axes x et y (chaque
     * courbe garde sa propre durée d'historique) : elles
     * restent égales aux bornes fournies quelles que soient les valeurs des
     * courbes (ce qui dépasse est coupé). setDynamicScales() permet de
     * revenir aux échelles dynamiques.
     *
     * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
     */
    public GLChart2D(Composite container, GLToolkit toolkit, double xMin, double xMax, double yMin, double yMax,
                     Curve2DData... curves) {
        super(container, toolkit, withScales(new Chart2DRenderer(curves), xMin, xMax, yMin, yMax));
    }

    private static Chart2DRenderer withScales(Chart2DRenderer renderer, double xMin, double xMax, double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
        return renderer;
    }

    /** Échelles fixes pour les axes x et y (voir le constructeur correspondant). */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax);
    }

    // --- IRTSWTXYChart ---

    /** Durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes du graphe. */
    @Override
    public void setHistorySize(double historySize) {
        renderer.setWindowTimeWidth(historySize);
    }

    /** Durée de l'historique imposée aux courbes, en secondes (0 si chaque courbe garde la sienne). */
    public double getHistorySize() {
        return renderer.getWindowTimeWidth();
    }

    /** Non géré : la fréquence d'échantillonnage est propre à chaque courbe. */
    @Override
    public void setSampleFrequency(double sfx) {
    }

    @Override
    public void setWaitForAllSeriesToRedraw(boolean wait) {
    }

    @Override
    public String getMeanDrawTime() {
        return null;
    }
}
