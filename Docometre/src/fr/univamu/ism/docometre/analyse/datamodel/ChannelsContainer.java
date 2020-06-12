package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

public final class ChannelsContainer {
	
	private List<Channel> channels;
	
	public ChannelsContainer(IResource subject, String[] channelsNames) {
		channels = new ArrayList<Channel>(0);
		for (String channelName : channelsNames) {
			if(!"Categories".equals(channelName)) {
				Channel channel = new Channel(subject, channelName);
				channels.add(channel);
			}
			
		}
	}

	public Channel[] getChannels() {
		return channels.toArray(new Channel[channels.size()]);
	}

	
}
