package fr.univamu.ism.docometre.analyse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;

public class SubjectsContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof IProject) {
			try {
				List<IResource> subjects = new ArrayList<IResource>();
				IResource[] resources = ((IProject)inputElement).members();
				for (IResource resource : resources) {
					if(ResourceType.isSubject(resource)) subjects.add(resource);
				}
				return subjects.toArray();
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof IResource) {
			try {
				IResource subject = (IResource)parentElement;
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(subject)) return null;
				Object sessionProperty = subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				if(sessionProperty != null && sessionProperty instanceof ChannelsContainer) {
					ChannelsContainer channelsContainer = (ChannelsContainer)sessionProperty;
					return channelsContainer.getChannels();
				}
				
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		return null;
		
	}

	@Override
	public Object getParent(Object element) {
		if(element instanceof IResource) return ((IResource)element).getParent();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if(element instanceof IResource) {
			IResource subject = (IResource)element;
			return MathEngineFactory.getMathEngine().isSubjectLoaded(subject);
		}
		return false;
	}

}