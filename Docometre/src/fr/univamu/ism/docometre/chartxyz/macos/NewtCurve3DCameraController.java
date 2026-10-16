package fr.univamu.ism.docometre.chartxyz.macos;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;

import fr.univamu.ism.docometre.chartxyz.Curve3DRenderer;

public class NewtCurve3DCameraController {

    private final Curve3DRenderer renderer;
    private final GLWindow glWindow;

    private int lastX, lastY;
    private boolean leftDown = false;
    private boolean rightDown = false;

    public NewtCurve3DCameraController(Curve3DRenderer renderer, GLWindow glWindow) {
        this.renderer = renderer;
        this.glWindow = glWindow;
    }

    public void attach() {
        glWindow.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = true;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = true;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Focus clavier dès que la souris entre dans le graphe (plusieurs graphes par fenêtre)
                glWindow.requestFocus();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) leftDown = false;
                if (e.getButton() == MouseEvent.BUTTON3) rightDown = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();

                if (leftDown) {
                    renderer.rotate(dx * 0.05f, dy * 0.05f);
                } else if (rightDown) {
                    renderer.pan(dx, dy);
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                renderer.zoom(-e.getRotation()[1] * 0.01f);
            }
        });

        glWindow.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (Character.toLowerCase(e.getKeyChar())) {
                    case 'r': renderer.resetView(); break;
                    case 'a': renderer.toggleAxes(); break;
                    case 'p': renderer.togglePoints(); break;
                    case 'g': renderer.toggleGrid(); break;
                    case 'l': renderer.toggleLabel(); break;
                    case 'n': renderer.toggleLegend(); break;
                    default: break;
                }
            }
        });
    }
}
