package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import fr.univamu.ism.docometre.chartxyz.Curve3DRenderer;
import fr.univamu.ism.docometre.chartxyz.CurveData;

public class Curve3D {
	
	private GLCanvas glCanvas;
	private FPSAnimator animator;

	public Curve3D(Composite container, CurveData curveData) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setDepthBits(24);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);
        
        glCanvas = new GLCanvas(container, SWT.NO_BACKGROUND, glCapabilities, null);

        animator = new FPSAnimator(glCanvas, 60, true);
        
        Curve3DRenderer renderer = new Curve3DRenderer(curveData);
        glCanvas.addGLEventListener(renderer);
        
        Curve3DCameraController cameraController = new Curve3DCameraController(renderer, glCanvas);
        cameraController.attach();
        
        container.addListener(SWT.Close, e -> {
        	close();
        });
        
        animator.start();
        
	}
	
	public void close() {
		animator.stop();
        if(glCanvas != null && !glCanvas.isDisposed()) glCanvas.dispose();
	}
	
	public boolean isDisposed() {
		return glCanvas == null || glCanvas.isDisposed();
	}
	
	public void moveAbove(Control control) {
		glCanvas.moveAbove(control);
	}
	
}
