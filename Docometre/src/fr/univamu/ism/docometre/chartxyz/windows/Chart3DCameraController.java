package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.docometre.chartxyz.Chart3DRenderer;

public class Chart3DCameraController {

    private final Chart3DRenderer renderer;
    private final Control control;

    private int lastX, lastY;
    private boolean leftDown = false;
    private boolean rightDown = false;

    public Chart3DCameraController(Chart3DRenderer renderer, Control control) {
        this.renderer = renderer;
        this.control = control;
    }

    public void attach() {
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                lastX = e.x;
                lastY = e.y;
                if (e.button == 1) leftDown = true;
                if (e.button == 3) rightDown = true;
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == 1) leftDown = false;
                if (e.button == 3) rightDown = false;
            }
        });

        control.addMouseMoveListener(e -> {
            int dx = e.x - lastX;
            int dy = e.y - lastY;
            lastX = e.x;
            lastY = e.y;

            if (leftDown) {
                renderer.rotate(dx * 0.5f, dy * 0.5f);
            } else if (rightDown) {
                renderer.pan(dx, dy);
            }
        });

        control.addMouseWheelListener(e -> renderer.zoom(-e.count * 0.1f));

        control.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (Character.toLowerCase(e.character)) {
                    case 'r': renderer.resetView(); break;
                    case 'a': renderer.toggleAxes(); break;
                    case 'p': renderer.togglePoints(); break;
                    case 'g': renderer.toggleGrid(); break;
                    case 'l': renderer.toggleLabel(); break;
                    case 'n': renderer.toggleLegend(); break;
                    case 'f': renderer.toggleFps(); break;
                    case 'v': renderer.toggleCurrentValues(); break;
                    case 'd': renderer.toggleDecimation(); break;
                    default: break;
                }
            }
        });

        // Focus clavier dès que la souris entre dans le graphe (utile quand plusieurs
        // graphes partagent une fenêtre), seulement si notre fenêtre est active pour
        // ne pas voler le focus à une autre fenêtre ou à un dialogue
        control.addListener(SWT.MouseEnter, e -> {
            if (control.getShell() == control.getDisplay().getActiveShell()) {
                control.forceFocus();
            }
        });

        control.setFocus();
    }
}
