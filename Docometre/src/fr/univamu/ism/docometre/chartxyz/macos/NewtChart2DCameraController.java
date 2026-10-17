package fr.univamu.ism.docometre.chartxyz.macos;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;

import fr.univamu.ism.docometre.chartxyz.Chart2DRenderer;

/**
 * Version NEWT de {@link fr.univamu.ism.docometre.chartxyz.windows.Chart2DCameraController}
 * (mêmes commandes souris et clavier).
 */
public class NewtChart2DCameraController {

    private final Chart2DRenderer renderer;
    private final GLWindow glWindow;

    private int lastX, lastY;
    private boolean dragging = false;

    public NewtChart2DCameraController(Chart2DRenderer renderer, GLWindow glWindow) {
        this.renderer = renderer;
        this.glWindow = glWindow;
    }

    public void attach() {
        glWindow.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) dragging = true;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Focus clavier dès que la souris entre dans le graphe (plusieurs graphes par fenêtre)
                glWindow.requestFocus();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) dragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();

                if (dragging) {
                    // getX()/getY() et getWidth()/getHeight() sont dans les mêmes unités (fenêtre)
                    renderer.pan((float) dx / Math.max(1, glWindow.getWidth()), (float) dy / Math.max(1, glWindow.getHeight()));
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                float rotation = e.getRotation()[1];
                if (rotation != 0) renderer.zoom(Math.signum(rotation));
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
                    case 'f': renderer.toggleFps(); break;
                    case 'v': renderer.toggleCurrentValues(); break;
                    case 'd': renderer.toggleDecimation(); break;
                    case 'e': renderer.toggleEqualAspect(); break;
                    default: break;
                }
            }
        });
    }
}
