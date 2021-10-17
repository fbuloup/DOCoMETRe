package fr.univamu.ism.docometre.analyse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
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
		if(inputElement instanceof IContainer) {
			try {
				List<IResource> elements = new ArrayList<IResource>();
				IResource[] resources = ((IContainer)inputElement).members();
				for (IResource resource : resources) {
					if(mustAddElement(resource)) elements.add(resource);
				}
				return elements.toArray();
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		return null;
	}

	private boolean mustAddElement(IResource element/*, List<IResource> elements*/) {
		try {
			
			if(ResourceType.isSubject(element)) return true;
			if(ResourceType.isDataProcessing(element)) return true;
			if(ResourceType.isBatchDataProcessing(element)) return true;
			if(ResourceType.isXYChart(element)) return true;
			if(ResourceType.isXYZChart(element)) return true;
			if(!ResourceType.isFolder(element)) return false;
			
			IResource[] resources = ((IFolder)element).members();
			boolean addElement = resources.length == 0;
			for (IResource resource : resources) {
				addElement = addElement || mustAddElement(resource);
			}

			return addElement;
			
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof IResource) {
			try {
				
				IResource resource = (IResource)parentElement;
				
				if(ResourceType.isSubject(resource)) {
					if(!MathEngineFactory.getMathEngine().isSubjectLoaded(resource)) return null;
					Object sessionProperty = resource.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					if(sessionProperty != null && sessionProperty instanceof ChannelsContainer) {
						ChannelsContainer channelsContainer = (ChannelsContainer)sessionProperty;
						return channelsContainer.getChannels();
					}
				}
				
				if(ResourceType.isFolder(resource)) {
					return getElements(resource);
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
			IResource resource = (IResource)element;
			if(ResourceType.isFolder(resource)) {
				try {
					boolean isEmpty = ((IContainer)resource).members().length == 0;
					return mustAddElement(resource) && !isEmpty;
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(ResourceType.isSubject(resource)) return MathEngineFactory.getMathEngine().isSubjectLoaded(resource);
		}
		return false;
	}

}
