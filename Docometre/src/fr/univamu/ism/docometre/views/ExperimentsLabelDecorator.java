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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;

public class ExperimentsLabelDecorator implements ILightweightLabelDecorator {
	
	public static String ID = "Docometre.ExperimentsDecorator";

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if(element instanceof IResource) {
			IResource resource = (IResource)element;
			if(resource instanceof IProject && !((IProject) resource).isOpen()) return;
			if(resource.exists()) {
				if(ResourceType.isDACQConfiguration((IResource) element)) {
					boolean isActive = false;
					String fullPath = ResourceProperties.getDefaultDACQPersistentProperty(resource); 
					if(fullPath != null) isActive = resource.getFullPath().equals(new Path(fullPath));
					if(isActive)
					decoration.addOverlay(Activator.getImageDescriptor(IImageKeys.DAQ_CONFIGURATION_ACTIVE_OVERLAY), IDecoration.BOTTOM_RIGHT);
				}
				if(ResourceType.isSubject((IResource) element)) {
					boolean isLoaded = MathEngineFactory.getMathEngine().isSubjectLoaded((IResource) element);
					if(isLoaded)
					decoration.addOverlay(Activator.getImageDescriptor(IImageKeys.SUBJECT_LOADED_OVERLAY), IDecoration.TOP_RIGHT);
				}
				try {
					int severity = resource.findMaxProblemSeverity(DocometreBuilder.MARKER_ID, true, IResource.DEPTH_INFINITE);
					if(severity == IMarker.SEVERITY_ERROR) decoration.addOverlay(Activator.getImageDescriptor(IImageKeys.ERROR_ICON), IDecoration.BOTTOM_LEFT);
					if(severity == IMarker.SEVERITY_WARNING) decoration.addOverlay(Activator.getImageDescriptor(IImageKeys.WARNING_ICON), IDecoration.BOTTOM_LEFT);	
				} catch (CoreException e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				}
			}
		}
		
	}

	

}
