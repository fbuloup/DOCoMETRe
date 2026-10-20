package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Base des graphes plans (1D et 2D) : ajoute la position de la légende.
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
}
