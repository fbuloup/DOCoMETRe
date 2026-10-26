package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * OpenGL component embedded in an SWT user interface (chart or surface):
 * layout and life cycle.
 */
public interface GLComponent {

    /** Stops the animation and destroys the component. */
    void close();

    boolean isDisposed();

    void moveAbove(Control control);

    void setLayoutData(GridData gridData);
}
