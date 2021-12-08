package fr.univamu.ism.docometre.analyse.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class SubjectsLabelProvider implements ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {
		if(element instanceof IResource) {
			IResource resource = (IResource)element;
			if(ResourceType.isSubject(resource)) return Activator.getImage(IImageKeys.SUBJECT_ICON);
			if(ResourceType.isChannel(resource)) {
				Channel channel = (Channel)element;
				if(channel.isSignal()) return Activator.getImage(IImageKeys.SIGNAL_ICON);
				if(channel.isCategory()) return Activator.getImage(IImageKeys.CATEGORY_ICON);
				if(channel.isEvent()) return Activator.getImage(IImageKeys.EVENT_ICON);
			}
			if(ResourceType.isFolder(resource)) return Activator.getImage(IImageKeys.FOLDER_ICON);
			if(ResourceType.isDataProcessing(resource)) return Activator.getImage(IImageKeys.DATA_PROCESSING_ICON);
			if(ResourceType.isBatchDataProcessing(resource)) return Activator.getImage(IImageKeys.BATCH_DATA_PROCESSING_ICON);
			if(ResourceType.isXYChart(resource)) return Activator.getImage(IImageKeys.XYChart_ICON);
			if(ResourceType.isXYZChart(resource)) return Activator.getImage(IImageKeys.XYZChart_ICON);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if(element instanceof IResource) return GetResourceLabelDelegate.getLabel((IResource)element);
		return element.toString();
	}

}
