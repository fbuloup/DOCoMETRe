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
		if(element instanceof IResource && ResourceType.isSubject((IResource) element)) return Activator.getImageDescriptor(IImageKeys.SUBJECT_ICON).createImage();
		if(element instanceof Channel) {
			Channel channel = (Channel)element;
			if(channel.isSignal()) return Activator.getImageDescriptor(IImageKeys.SIGNAL_ICON).createImage();
			if(channel.isCategory()) return Activator.getImageDescriptor(IImageKeys.CATEGORY_ICON).createImage();
			if(channel.isEvent()) return Activator.getImageDescriptor(IImageKeys.EVENT_ICON).createImage();
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if(element instanceof IResource) return GetResourceLabelDelegate.getLabel((IResource)element);
		return element.toString();
	}

}
