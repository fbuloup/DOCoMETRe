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
package fr.univamu.ism.docometre.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class ExperimentsViewerSorter extends ViewerComparator {
	
	@Override
	public int category(Object element) {
		if(!(element instanceof IResource)) return super.category(element);
		IResource resource = (IResource)element;
		if(resource instanceof IProject && !((IProject) resource).isOpen()) return super.category(element);
		if(ResourceType.isDACQConfiguration(resource)) return 0;
		if(ResourceType.isProcess(resource)) return 1;
		if(ResourceType.isBatchDataProcessing(resource)) return 2;
		if(ResourceType.isDataProcessing(resource)) return 3;
		if(ResourceType.isXYChart(resource)) return 4;
		if(ResourceType.isXYZChart(resource)) return 5;
		if(ResourceType.isFolder(resource)) return 6;
		if(ResourceType.isProcessTest(resource)) return 7;
		if(ResourceType.isLog(resource)) return 8;
		if(ResourceType.isSubject(resource)) return 9;
		if(ResourceType.isSession(resource)) return 10;
		if(ResourceType.isParameters(resource)) return 11;
		if(ResourceType.isTrial(resource)) return 12;
		if(ResourceType.isLog(resource)) return 13;
		if(ResourceType.isSamples(resource)) return 14;
		if(ResourceType.isChannel(resource)) {
			if(MathEngineFactory.getMathEngine().isSignal(resource)) return 15;
			if(MathEngineFactory.getMathEngine().isCategory(resource)) return 16;
			if(MathEngineFactory.getMathEngine().isEvent(resource)) return 17;
		}
		return super.category(element);
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof IResource && e2 instanceof IResource) {
			IResource resource1 = (IResource)e1;
			IResource resource2 = (IResource)e2;
			if(resource1 instanceof IProject && !((IProject) resource1).isOpen()) return super.compare(viewer, e1, e2);
			if(resource2 instanceof IProject && !((IProject) resource2).isOpen()) return super.compare(viewer, e1, e2);
			if(ResourceType.areResourcesSameType(resource1, resource2)) {
				if(resource1.getName().matches("^.*\\d+$") && resource2.getName().matches("^.*\\d+$")) {
					long number = compare(resource1, resource2);
					if(number > Integer.MAX_VALUE) number = Integer.MAX_VALUE;
					if(number < Integer.MIN_VALUE) number = Integer.MIN_VALUE;
					return (int)number;
				}
			}
		}
		return super.compare(viewer, e1, e2);
	}
	
	private long compare(IResource resource1, IResource resource2) {
		String prefix = resource1.getName().replaceAll("\\d+$", "");
		long number1 = Long.parseLong(resource1.getName().replaceAll(prefix, ""));
		prefix = resource2.getName().replaceAll("\\d+$", "");
		long number2 = Long.parseLong(resource2.getName().replaceAll(prefix, ""));
		return number1 - number2;
	}

}
