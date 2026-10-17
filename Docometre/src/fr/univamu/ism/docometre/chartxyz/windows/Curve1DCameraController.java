package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import fr.univamu.ism.docometre.chartxyz.Curve1DRenderer;

/**
 * Relie les événements souris/clavier SWT au {@link Curve1DRenderer} :
 * - Clic gauche (ou droit) + glisser : panoramique vertical
 * - Survol             : donne le focus clavier au graphe
 * - Molette               : zoom vertical
 * - Touche R : réinitialiser la vue (ajustement automatique)
 * - Touche A : afficher/masquer la droite y = 0
 * - Touche P : afficher/masquer les points
 * - Touche G : afficher/masquer la grille
 * - Touche L : afficher/masquer les graduations
 * - Touche N : afficher/masquer la légende
 * - Touche F : afficher/masquer la cadence d'affichage (images/s)
 * - Touche V : afficher/masquer les valeurs courantes des courbes
 * - Espace   : pause / reprise de l'affichage
 * - Touche M : bascule balayage (oscilloscope) / défilement
 */
public class Curve1DCameraController {

    private final Curve1DRenderer renderer;
    private final Control control;

    private int lastX, lastY;
    private boolean dragging = false;

    public Curve1DCameraController(Curve1DRenderer renderer, Control control) {
        this.renderer = renderer;
        this.control = control;
    }

    public void attach() {
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                lastX = e.x;
                lastY = e.y;
                if (e.button == 1 || e.button == 3) dragging = true;
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == 1 || e.button == 3) dragging = false;
            }
        });

        control.addMouseMoveListener(e -> {
            int dx = e.x - lastX;
            int dy = e.y - lastY;
            lastX = e.x;
            lastY = e.y;

            if (dragging) {
                Point size = control.getSize();
                renderer.pan((float) dx / Math.max(1, size.x), (float) dy / Math.max(1, size.y));
            }
        });

        control.addMouseWheelListener(e -> renderer.zoom(Math.signum(e.count)));

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
                    case ' ': renderer.togglePause(); break;
                    case 'm': renderer.toggleDisplayMode(); break;
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
