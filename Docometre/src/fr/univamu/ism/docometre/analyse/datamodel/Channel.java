package fr.univamu.ism.docometre.analyse.datamodel;

import org.eclipse.core.resources.IResource;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class Channel {
	
	private String name;
	private IResource subject;
	
	public Channel(IResource subject, String name) {
		this.subject = subject;
		this.name = name;
	}

	private String getName() {
		return name;
	}
	
	private String getFullName() {
		return subject.getFullPath().toString().replaceFirst("/", "").replaceAll("/", ".") + "." + getName();
	}
	
	@Override
	public String toString() {
		if(isCategory()) {
			return getName() + " [" + MathEngineFactory.getMathEngine().getCriteriaForCategory(getFullName()) + "]";
		}
		return getName();
	}
	
	public boolean isSignal() {
		return MathEngineFactory.getMathEngine().isSignal(getFullName());
	}
	
	public boolean isCategory() {
		return MathEngineFactory.getMathEngine().isCategory(getFullName());
	}
	
	public boolean isEvent() {
		return MathEngineFactory.getMathEngine().isEvent(getFullName());
	}

}
