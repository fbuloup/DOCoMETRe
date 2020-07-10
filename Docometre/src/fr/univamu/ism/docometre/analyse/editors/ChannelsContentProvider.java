package fr.univamu.ism.docometre.analyse.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredContentProvider;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class ChannelsContentProvider implements IStructuredContentProvider {
	
	private boolean signals;
	private boolean categories;
	private boolean events;

	public ChannelsContentProvider(boolean signals, boolean categories, boolean events) {
		this.signals = signals;
		this.categories = categories;
		this.events = events;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof IProject) {
			try {
				List<IResource> elements = new ArrayList<IResource>();
				IResource[] resources = ((IContainer)inputElement).members();
				for (IResource resource : resources) {
					if(ResourceType.isSubject(resource) && MathEngineFactory.getMathEngine().isSubjectLoaded(resource)) {
						if(signals) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getSignals(resource)));
						if(categories) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getCategories(resource)));
						if(events) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getEvents(resource)));
					}
				}
				return elements.toArray();
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		return null;
	}
	
}
