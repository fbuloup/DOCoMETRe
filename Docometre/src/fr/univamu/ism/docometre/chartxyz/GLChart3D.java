package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.widgets.Composite;

/**
 * Graphe de courbes 3D temps réel (voir {@link Chart3DRenderer}).
 */
public class GLChart3D extends AbstractGLChart<Curve3DData, Chart3DRenderer> implements IRTSWTXYZChart {

    /**
     * Crée le graphe avec des échelles dynamiques, ajustées en continu aux valeurs des courbes.
     *
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     * @throws IllegalStateException si une courbe est déjà affichée dans un autre graphe
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double historySize, Curve3DData... curves) {
        super(container, toolkit, new Chart3DRenderer(historySize, curves), true);
    }

    /**
     * Crée le graphe avec des échelles fixes pour les axes x, y et z (voir
     * {@link #GLChart3D(Composite, GLToolkit, double, double, double, double, double, double, Curve3DData...)}).
     *
     * @param historySize durée de l'historique (longueur des traînées), en secondes, imposée à toutes les courbes
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double historySize, double xMin, double xMax,
                     double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
        super(container, toolkit, withScales(new Chart3DRenderer(historySize, curves), xMin, xMax, yMin, yMax, zMin, zMax), true);
    }

    /**
     * Crée le graphe avec des échelles dynamiques ; chaque courbe garde sa
     * propre durée d'historique (données statiques, par exemple).
     */
    public GLChart3D(Composite container, GLToolkit toolkit, Curve3DData... curves) {
        super(container, toolkit, new Chart3DRenderer(curves), true);
    }

    /**
     * Crée le graphe avec des échelles fixes pour les axes x, y et z (chaque
     * courbe garde sa propre durée d'historique) : elles
     * restent égales aux bornes fournies quelles que soient les valeurs des
     * courbes (ce qui sort de la boîte est coupé). setDynamicScales() permet
     * de revenir aux échelles dynamiques.
     *
     * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
     */
    public GLChart3D(Composite container, GLToolkit toolkit, double xMin, double xMax, double yMin, double yMax,
                     double zMin, double zMax, Curve3DData... curves) {
        super(container, toolkit, withScales(new Chart3DRenderer(curves), xMin, xMax, yMin, yMax, zMin, zMax), true);
    }

    private static Chart3DRenderer withScales(Chart3DRenderer renderer, double xMin, double xMax, double yMin, double yMax,
                                              double zMin, double zMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
        return renderer;
    }

    /** Échelles fixes pour les axes x, y et z (voir le constructeur correspondant). */
    public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    // --- IRTSWTXYZChart ---

    /** Sans objet en 3D : la légende est toujours en haut à gauche. */
    @Override
    public void setLegendPosition(int position) {
    }

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

	@Override
	public void setFixedScales(double xMin, double xMax, double yMin, double yMax) {
		if(hasFixedScales()) {
			double zMin = renderer.getFixedScaleBounds()[4];
			double zMax = renderer.getFixedScaleBounds()[5];
			renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
		}
		
		
	}
}
