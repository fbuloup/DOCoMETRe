package fr.univamu.ism.docometre.views;

import java.util.ArrayList;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

public class CachedLogger implements ILogListener {
	
	private ArrayList<IStatus> cachedLog = new ArrayList<>();
	
	private static CachedLogger cachedLogger = new CachedLogger();
	
	public static CachedLogger getInstance() {
		if(cachedLogger == null) cachedLogger = new CachedLogger();
		return cachedLogger;
	}
	
	private CachedLogger() {
	}

	@Override
	public void logging(IStatus status, String plugin) {
		cachedLog.add(status);
	}
	
	public IStatus get() {
		if(cachedLog.size() == 0) return null;
		IStatus status = cachedLog.get(0);
		cachedLog.remove(0);
		return status;
	}

}
