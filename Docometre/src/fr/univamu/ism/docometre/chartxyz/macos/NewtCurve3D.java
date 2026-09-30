package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

import fr.univamu.ism.docometre.chartxyz.Curve3DRenderer;
import fr.univamu.ism.docometre.chartxyz.CurveData;

public class NewtCurve3D {
	
	private FPSAnimator animator;
	private NewtCanvasSWT newtCanvas;
	private GLWindow glWindow;

	public NewtCurve3D(Composite container, CurveData curveData) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setDepthBits(24);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);
        
        glWindow = GLWindow.create(glCapabilities);
        
        animator = new FPSAnimator(glWindow, 60, true);
        
        Curve3DRenderer renderer = new Curve3DRenderer(curveData);
        glWindow.addGLEventListener(renderer);
        
        NewtCurve3DCameraController cameraController = new NewtCurve3DCameraController(renderer, glWindow);
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
	
	public boolean isDisposed() {
		return newtCanvas == null || newtCanvas.isDisposed();
	}

	public void moveAbove(Control control) {
		newtCanvas.moveAbove(control);
	}

}
