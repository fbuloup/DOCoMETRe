package fr.univamu.ism.docometre.chartxyz.macos;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

import fr.univamu.ism.docometre.chartxyz.Surface3DRenderer;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

public class NewtSurface {
	
	private FPSAnimator animator;
	private NewtCanvasSWT newtCanvas;
	private GLWindow glWindow;
	
	public NewtSurface(Composite container, SurfaceData surfaceData) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setDepthBits(24);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);

        glWindow = GLWindow.create(glCapabilities);
        
        animator = new FPSAnimator(glWindow, 60, true);

        Surface3DRenderer renderer = new Surface3DRenderer(surfaceData);
        glWindow.addGLEventListener(renderer);

        NewtSurfaceCameraController cameraController = new NewtSurfaceCameraController(renderer, glWindow);
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
