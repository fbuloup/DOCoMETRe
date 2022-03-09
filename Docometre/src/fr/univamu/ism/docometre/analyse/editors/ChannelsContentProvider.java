/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
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
