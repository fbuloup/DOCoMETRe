package fr.univamu.ism.docometre.chartxyz;

/**
 * Listener of the camera of a 3D chart (see
 * {@link Chart3DRenderer#addCameraListener}): called at each frame whose
 * camera state differs from the previous frame (rotation, distance or pan,
 * by the mouse, the keyboard or the API), from the second frame on.
 *
 * The call takes place in the OpenGL rendering thread (SWT thread with
 * {@link GLToolkit#SWT}, animator thread with {@link GLToolkit#NEWT}). To
 * act on SWT widgets from the listener, use Display.asyncExec.
 */
@FunctionalInterface
public interface CameraListener {

    /** @param state new camera state (copy, see {@link OrbitCamera#getState()}) */
    void cameraChanged(float[] state);
}
