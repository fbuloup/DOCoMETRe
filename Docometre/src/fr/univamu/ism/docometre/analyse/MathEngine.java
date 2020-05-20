package fr.univamu.ism.docometre.analyse;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface MathEngine {
	IStatus startEngine(IProgressMonitor monitor);
	IStatus stopEngine(IProgressMonitor monitor);
	boolean isStarted();
	void addListener(MathEngineListener listener);
	boolean isSubjectLoaded(IResource subject);
}
