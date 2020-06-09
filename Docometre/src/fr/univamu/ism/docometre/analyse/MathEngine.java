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
	void load(IResource subject);
	void unload(IResource subject);
	boolean exist(String variableName);
	boolean isStruct(String variableName);
	boolean isField(String variableName, String fieldName);
}
