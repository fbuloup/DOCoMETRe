package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.docometre.chartxyz.Curve3DRenderer;

public class Curve3DCameraController {

    private final Curve3DRenderer renderer;
    private final Control control;

    private int lastX, lastY;
    private boolean leftDown = false;
    private boolean rightDown = false;

    public Curve3DCameraController(Curve3DRenderer renderer, Control control) {
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
                    default: break;
                }
            }
        });

        control.setFocus();
    }
}
