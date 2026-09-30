package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.docometre.chartxyz.Surface3DRenderer;

/**
 * Relie les événements souris/clavier SWT au {@link Surface3DRenderer}
 * pour permettre à l'utilisateur de manipuler la caméra et l'affichage :
 * - Clic gauche + glisser : rotation (orbite)
 * - Clic droit + glisser  : panoramique
 * - Molette               : zoom avant/arrière
 * - Touche R : réinitialiser la vue
 * - Touche A : afficher/masquer les axes
 * - Touche G : afficher/masquer la grille au sol
 * - Touche S : afficher/masquer la surface pleine (solide)
 * - Touche W : afficher/masquer le maillage filaire
 * - Touche L : activer/désactiver l'éclairage
 */
public class SurfaceCameraController {

    private final Surface3DRenderer renderer;
    private final Control control;

    private int lastX, lastY;
    private boolean leftDown = false;
    private boolean rightDown = false;

    public SurfaceCameraController(Surface3DRenderer renderer, Control control) {
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
                    case 'g': renderer.toggleGrid(); break;
                    case 's': renderer.toggleSolid(); break;
                    case 'w': renderer.toggleWireframe(); break;
                    case 'h': renderer.toggleLighting(); break;
                    case 'l': renderer.toggleLabel(); break;
                    default: break;
                }
            }
        });

        control.setFocus();
    }
}
