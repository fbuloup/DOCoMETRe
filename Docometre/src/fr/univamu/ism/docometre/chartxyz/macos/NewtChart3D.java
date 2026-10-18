package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import fr.univamu.ism.docometre.chartxyz.IRTSWTXYZChart;
import fr.univamu.ism.rtswtchart.IRTSWTSerie;
import fr.univamu.ism.docometre.chartxyz.Chart3DRenderer;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;

public class NewtChart3D implements IRTSWTXYZChart {
	
	private FPSAnimator animator;
	private NewtCanvasSWT newtCanvas;
	private GLWindow glWindow;

	private Chart3DRenderer renderer;

	/**
	 * Crée le graphe avec une ou plusieurs courbes (d'autres peuvent être
	 * ajoutées ensuite avec addCurve), avec des échelles dynamiques : elles
	 * s'ajustent en continu aux valeurs des courbes.
	 */
	public NewtChart3D(Composite container, Curve3DData... curves) {
		this(container, (double[]) null, curves);
	}
	
	/**
	 * Crée le graphe avec des échelles fixes pour les axes x, y et z : elles restent égales
	 * aux bornes fournies quelles que soient les valeurs des courbes (ce qui
	 * dépasse est coupé). setDynamicScales() permet de revenir aux échelles dynamiques.
	 *
	 * @throws IllegalArgumentException si un minimum n'est pas strictement inférieur à son maximum
	 */
	public NewtChart3D(Composite container, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax, Curve3DData... curves) {
		this(container, CurveStyles.checkScales(xMin, xMax, yMin, yMax, zMin, zMax), curves);
	}
	
	/** fixedScales : bornes des échelles fixes, déjà vérifiées, ou null pour des échelles dynamiques. */
	private NewtChart3D(Composite container, double[] fixedScales, Curve3DData... curves) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setDepthBits(24);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);
        
        glWindow = GLWindow.create(glCapabilities);
        
        animator = new FPSAnimator(glWindow, 60, true);
        
        renderer = new Chart3DRenderer(curves);
        if (fixedScales != null) renderer.setFixedScales(fixedScales[0], fixedScales[1], fixedScales[2], fixedScales[3], fixedScales[4], fixedScales[5]);
        glWindow.addGLEventListener(renderer);
        
        NewtChart3DCameraController cameraController = new NewtChart3DCameraController(renderer, glWindow);
        cameraController.attach();
        
        newtCanvas = NewtCanvasSWT.create(container, SWT.NO_BACKGROUND, glWindow);
        
        newtCanvas.addListener(SWT.Dispose, e -> {
        	close();
        });
        
        animator.start();
	}
	
	public void stopAnimator() {
		animator.stop();
	}
	
	public void close() {
		animator.stop();
        if(newtCanvas != null && !newtCanvas.isDisposed()) newtCanvas.dispose();
        glWindow.destroy();
	}
	
	/** Ajoute une courbe au graphe (utilisable pendant l'affichage, depuis n'importe quel thread). */
	public void addCurve(Curve3DData curve) {
		renderer.addCurve(curve);
	}
	
	/** Retire une courbe du graphe. */
	public void removeCurve(Curve3DData curve) {
		renderer.removeCurve(curve);
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
	
	/** Échelles fixes pour les axes x, y et z (voir le constructeur correspondant). */
	public void setFixedScales(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
		renderer.setFixedScales(xMin, xMax, yMin, yMax, zMin, zMax);
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
	
	/** Affiche ou masque, en haut à droite du graphe, la dernière valeur de chaque courbe (également avec la touche V). */
	public void setCurrentValuesVisible(boolean visible) {
		renderer.setCurrentValuesVisible(visible);
	}
	
	public boolean isCurrentValuesVisible() {
		return renderer.isCurrentValuesVisible();
	}
	
	/**
	 * Active ou désactive la réduction du nombre de points envoyés à OpenGL
	 * (activée par défaut, également avec la touche D). Le dessin est inchangé.
	 */
	public void setDecimationEnabled(boolean enabled) {
		renderer.setDecimationEnabled(enabled);
	}
	
	public boolean isDecimationEnabled() {
		return renderer.isDecimationEnabled();
	}
	
	public boolean isDisposed() {
		return newtCanvas == null || newtCanvas.isDisposed();
	}

	public void moveAbove(Control control) {
		newtCanvas.moveAbove(control);
	}

	@Override
	public GridData getLayoutData() {
		return null;
	}

	@Override
	public void setAutoScale(boolean autoscale) {
		if(autoscale) setDynamicScales();
	}

	@Override
	public void setxMax(double xMax) {
	}

	@Override
	public void setxMin(double xMin) {
	}

	@Override
	public void setyMax(double value) {
	}

	@Override
	public void setyMin(double value) {
	}

	@Override
	public void setGridVisibility(boolean value) {
		renderer.setGridVisibility(value);
	}

	@Override
	public void setLegendVisibility(boolean value) {
		setLegendVisible(value);
	}

	@Override
	public void setLegendPosition(int position) {
		// N.A. in 3D
	}

	@Override
	public void setGridLinesColor(Color color) {
	}

	@Override
	public void setFontColor(Color color) {
	}

	@Override
	public void setHistorySize(double historySize) {
	}

	@Override
	public void setSampleFrequency(double sfx) {
	}

	@Override
	public void setWaitForAllSeriesToRedraw(boolean wait) {
	}

	@Override
	public String getMeanDrawTime() {
		return null;
	}

	@Override
	public IRTSWTSerie createSerie(String serieId, Color color) {
		return null;
	}

	@Override
	public void setzMax(double value) {
	}

	@Override
	public void setzMin(double value) {
	}

}
