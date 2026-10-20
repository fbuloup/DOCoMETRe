package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * Composant OpenGL intégré dans une interface SWT (graphe ou surface) :
 * mise en page et cycle de vie.
 */
public interface GLComponent {

    /** Arrête l'animation et détruit le composant. */
    void close();

    boolean isDisposed();

    void moveAbove(Control control);

    void setLayoutData(GridData gridData);
}
