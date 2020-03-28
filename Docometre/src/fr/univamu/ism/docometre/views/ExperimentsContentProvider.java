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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class ExperimentsContentProvider implements ITreeContentProvider {

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof IWorkspaceRoot) return ((IWorkspaceRoot)inputElement).getProjects();
		return null;
	}

	public Object[] getChildren(Object parentElement) {
			try {
				if(!((IResource)parentElement).exists()) return null;
				if(parentElement instanceof IContainer) {
					if(((IResource)parentElement).exists()) {
						IResource[] resources = ((IContainer)parentElement).members();
						return processMembers(resources);
					}
					return new Object[0];
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
			}
		return null;
	}

	public Object getParent(Object element) {
		if(element instanceof IResource) return ((IResource)element).getParent();
		return null;
	}

	public boolean hasChildren(Object element) {
		try {
			if(element instanceof IContainer && ((IContainer) element).exists()) {
				if(element instanceof IProject && !((IProject) element).isOpen()) return false;
				IResource[] resources = ((IContainer)element).members();
				return processMembers(resources).length  > 0;
			}
		} catch (CoreException e) {
			Activator.logErrorMessageWithCause(e);
		}
		return false;
	}
	
	private boolean validateResource(IResource resource) {
		boolean valid = !resource.getName().matches("^\\..*$");
		valid = valid && !resource.getName().matches("BinSource");
		if(valid) {
			ResourceType resourceType = ResourceType.getResourceType(resource);
			if(resourceType.equals(ResourceType.ANY)) {
				if(resource.getFileExtension() != null) {
					if(resource.getName().endsWith(Activator.daqFileExtension)) {
						ResourceProperties.setTypePersistentProperty(resource, ResourceType.DACQ_CONFIGURATION.toString());
					}
					if(resource.getName().endsWith(Activator.logFileExtension)) {
						ResourceProperties.setTypePersistentProperty(resource, ResourceType.LOG.toString());
					}
					if(resource.getName().endsWith(Activator.parametersFileExtension)) {
						ResourceProperties.setTypePersistentProperty(resource, ResourceType.PARAMETERS.toString());
					}
					if(resource.getName().endsWith(Activator.processFileExtension)) {
						ResourceProperties.setTypePersistentProperty(resource, ResourceType.PROCESS.toString());
					}
					if(resource.getName().endsWith(Activator.samplesFileExtension)) {
						ResourceProperties.setTypePersistentProperty(resource, ResourceType.SAMPLES.toString());
					}
				}
				
			}
		}
		return valid;
	}
	
	/*
	 * Process members to remove all files or folders starting with '.'
	 */
	private IResource[] processMembers(IResource[] resources) {
		List<IResource> newResources = new ArrayList<>(0);
		for (IResource resource : resources) {
			if(validateResource(resource)) newResources.add(resource);
		}
		return newResources.toArray(new IResource[newResources.size()]);
		 
	}

}
