package fr.univamu.ism.docometre.analyse.datamodel;

import org.eclipse.core.resources.IFolder;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public final class ChannelsContainer {
	
	private IFolder subject;
	
	public ChannelsContainer(IFolder subject) {
		this.subject = subject;
	}

	public Channel[] getChannels() {
		return MathEngineFactory.getMathEngine().getChannels(subject);
	}

	
}
