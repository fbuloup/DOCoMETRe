package fr.univamu.ism.docometre.analyse.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class ChannelsContentProvider implements IStructuredContentProvider {

	private boolean signals;
	private boolean categories;
	private boolean events;
	private boolean markers;
	private boolean features;
	private boolean fromBegToEnd;
	private boolean frontEndCut;

	public ChannelsContentProvider(boolean signals, boolean categories, boolean events, boolean markers, boolean features, boolean fromBegToEnd, boolean frontEndCut) {
		this.signals = signals;
		this.categories = categories;
		this.events = events;
		this.markers = markers;
		this.features = features;
		this.fromBegToEnd = fromBegToEnd;
		this.frontEndCut = frontEndCut;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof IProject) {
			List<IResource> elements = new ArrayList<IResource>();
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					String[] loadedSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
					boolean alreadyDone = false;
					for (String loadedSubject : loadedSubjects) {
						if("".equals(loadedSubject)) continue;
						String path = loadedSubject.replaceAll("\\.", "/");
						IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
						if(fromBegToEnd && !alreadyDone) {
							elements.add(Channel.fromBeginningChannel);
							elements.add(Channel.toEndChannel);
							alreadyDone = true;
						}
						if(signals) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getSignals(resource)));
						if(categories) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getCategories(resource)));
						if(events) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getEvents(resource)));
						if(markers) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getMarkers(resource)));
						if(features) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getFeatures(resource)));
						
						if(frontEndCut) elements.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getFrontEndCuts(resource)));
					}
				}
			});
			return elements.toArray();
		}
		return null;
	}	
	
}
