package fr.univamu.ism.docometre.chartxyz.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import fr.univamu.ism.docometre.chartxyz.Surface3DRenderer;
import fr.univamu.ism.docometre.chartxyz.SurfaceData;

public class Surface {
	
	private GLCanvas glCanvas;
	private FPSAnimator animator;
	
	public Surface(Composite container, SurfaceData surfaceData) {
		GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(true);
        glCapabilities.setDepthBits(24);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);

        glCanvas = new GLCanvas(container, SWT.NO_BACKGROUND, glCapabilities, null);
        
        animator = new FPSAnimator(glCanvas, 60, true);

        Surface3DRenderer renderer = new Surface3DRenderer(surfaceData);
        glCanvas.addGLEventListener(renderer);

        SurfaceCameraController cameraController = new SurfaceCameraController(renderer, glCanvas);
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
