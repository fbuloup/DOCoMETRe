package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;

public final class ChannelsContainer {
	
	private List<Channel> channels;
	
	public ChannelsContainer(IFolder subject, Channel[] channels) {
		this.channels = Arrays.asList(channels);
	}

	public Channel[] getChannels() {
		return channels.toArray(new Channel[channels.size()]);
	}

	
}
