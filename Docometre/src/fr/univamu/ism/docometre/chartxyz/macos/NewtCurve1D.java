package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

import fr.univamu.ism.docometre.chartxyz.ChartFont;
import fr.univamu.ism.docometre.chartxyz.CurveStyles;
import fr.univamu.ism.docometre.chartxyz.LegendPosition;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.Curve1DRenderer;

public class NewtCurve1D {
	
	private FPSAnimator animator;
	private NewtCanvasSWT newtCanvas;
	private GLWindow glWindow;

	private Curve1DRenderer renderer;

	/**
	 * Crée le graphe avec une ou plusieurs courbes (d'autres peuvent être
	 * ajoutées ensuite avec addCurve), avec des échelles dynamiques : elles
	 * s'ajustent en continu aux valeurs des courbes.
	 */
	public NewtCurve1D(Composite container, Curve1DData... curves) {
		this(container, (double[]) null, curves);
	}
	
	/**
	 * Crée le graphe avec des échelles fixes pour l'axe vertical (l'axe du temps est fixé par la fenêtre temporelle) : elles restent égales
	 * aux bornes fournies quelles que soient les valeurs des courbes (ce qui
	 * dépasse est coupé). setDynamicScales() permet de revenir aux échelles dynamiques.
	 *
	 * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
	 */
	public NewtCurve1D(Composite container, double yMin, double yMax, Curve1DData... curves) {
		this(container, CurveStyles.checkScales(yMin, yMax), curves);
	}
	
	/** fixedScales : bornes des échelles fixes, déjà vérifiées, ou null pour des échelles dynamiques. */
	private NewtCurve1D(Composite container, double[] fixedScales, Curve1DData... curves) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);
        
        glWindow = GLWindow.create(glCapabilities);
        
        animator = new FPSAnimator(glWindow, 60, true);
        
        renderer = new Curve1DRenderer(curves);
        if (fixedScales != null) renderer.setFixedScales(fixedScales[0], fixedScales[1]);
        glWindow.addGLEventListener(renderer);
        
        NewtCurve1DCameraController cameraController = new NewtCurve1DCameraController(renderer, glWindow);
        cameraController.attach();
        
        newtCanvas = NewtCanvasSWT.create(container, SWT.NO_BACKGROUND, glWindow);
        
        container.addListener(SWT.Close, e -> {
        	close();
        });
        
        animator.start();
	}
	
	public void close() {
		animator.stop();
        if(newtCanvas != null && !newtCanvas.isDisposed()) newtCanvas.dispose();
        glWindow.destroy();
	}
	
	/** Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread). */
	public void addCurve(Curve1DData curve) {
		renderer.addCurve(curve);
	}
	
	/** Retire une courbe du graphe. */
	public void removeCurve(Curve1DData curve) {
		renderer.removeCurve(curve);
	}
	
	/** Choisit le mode d'affichage : balayage de type oscilloscope (par défaut) ou défilement. */
	public void setDisplayMode(Curve1DRenderer.DisplayMode displayMode) {
		renderer.setDisplayMode(displayMode);
	}
	
	/**
	 * Police des graduations des échelles ; les noms d'axes utilisent la même
	 * famille, en gras et plus grande.
	 *
	 * @param family famille ("Arial", "Helvetica", "Courier New"...) ou null pour la police du système
	 * @param size   taille en points
	 * @param style  SWT.NORMAL, SWT.BOLD, SWT.ITALIC ou SWT.BOLD | SWT.ITALIC
	 */
	public void setScaleFont(String family, int size, int style) {
		renderer.setScaleFont(new ChartFont(family, size, style));
	}
	
	/** Police de la légende (mêmes paramètres que {@link #setScaleFont}). */
	public void setLegendFont(String family, int size, int style) {
		renderer.setLegendFont(new ChartFont(family, size, style));
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
	
	/** Échelles fixes pour l'axe vertical (l'axe du temps est fixé par la fenêtre temporelle) (voir le constructeur correspondant). */
	public void setFixedScales(double yMin, double yMax) {
		renderer.setFixedScales(yMin, yMax);
	}
	
	/** Échelles dynamiques, ajustées en continu aux valeurs des courbes (par défaut). */
	public void setDynamicScales() {
		renderer.setDynamicScales();
	}
	
	/** Vrai si les échelles sont fixes. */
	public boolean hasFixedScales() {
		return renderer.hasFixedScales();
	}
	
	/** Affiche ou masque la légende (également possible avec la touche N quand le graphe a le focus). */
	public void setLegendVisible(boolean visible) {
		renderer.setLegendVisible(visible);
	}
	
	public boolean isLegendVisible() {
		return renderer.isLegendVisible();
	}
	
	/** Cadence d'affichage réelle mesurée, en images par seconde (cible : 60). */
	public double getFps() {
		return renderer.getFps();
	}
	
	/** Affiche ou masque la cadence d'affichage dans le graphe (également avec la touche F). */
	public void setFpsVisible(boolean visible) {
		renderer.setFpsVisible(visible);
	}
	
	public void setLayoutData(GridData gridData) {
		newtCanvas.setLayoutData(gridData);
	}
	
	public boolean isDisposed() {
		return newtCanvas == null || newtCanvas.isDisposed();
	}

	public void moveAbove(Control control) {
		newtCanvas.moveAbove(control);
	}

}
