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
package fr.univamu.ism.docometre.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class CopyResourcesAction extends Action implements ISelectionListener, IWorkbenchAction {

	private IResource[] resources;
	private IResource[] copiedResources;
	private IWorkbenchWindow window;

	public CopyResourcesAction(IWorkbenchWindow window) {
		setId("CopyResourcesAction"); //$NON-NLS-1$
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.CopyAction_Text);
		setToolTipText(DocometreMessages.CopyAction_Text);
        ISharedImages sharedImages = window.getWorkbench().getSharedImages();
        setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
	}
	
	@Override
	public void run() {
		try {
			// Remove from selected resources those which are children of the others
			ArrayList<IResource> cleanedResources = new ArrayList<IResource>(0);
			// First path to get all selected containers
			for (int i = 0; i < resources.length; i++) if(resources[i] instanceof IContainer) cleanedResources.add(resources[i]);
			ArrayList<IResource> files = new ArrayList<>();
			// Second path to get all files
			for (int i = 0; i < resources.length; i++) if(resources[i] instanceof IFile) files.add(resources[i]);
			//Third path to remove all contained files
			if(cleanedResources.size() > 0) files = removeContainedFiles(cleanedResources, files);
			// Add these files to copied resources
			cleanedResources.addAll(files);
			// Convert to array
			copiedResources = cleanedResources.toArray(new IResource[cleanedResources.size()]);
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
			copiedResources = null;
		}
	}

	private ArrayList<IResource> removeContainedFiles(ArrayList<IResource> containers, ArrayList<IResource> files) throws CoreException {
		ArrayList<IResource> cleanedFiles = new ArrayList<>();
		// First path remove all file contained in those containers
		for (IResource file : files) for (IResource container : containers) {
			if(((IContainer)container).findMember(file.getFullPath().lastSegment()) == null) {
				if(!cleanedFiles.contains(file)) cleanedFiles.add(file);
			}
		}
		// Second path : get all containers in those containers
		ArrayList<IResource> newContainers = new ArrayList<>();
		for (IResource container : containers) {
			IResource[] members = ((IContainer)container).members();
			for (IResource member : members) if(member instanceof IContainer) newContainers.add(member);
		}
		// Third path : restart with these new containers
		if(newContainers.size() > 0) cleanedFiles = removeContainedFiles(newContainers, cleanedFiles);
		return cleanedFiles;
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			resources = null;
			if (selection instanceof IStructuredSelection) {
				Object[] objects = ((IStructuredSelection) selection).toArray();
				resources = new IResource[objects.length];
				for (int i = 0; i < objects.length; i++) resources[i] = (IResource) objects[i];
			}
			setEnabled(resources != null && resources.length > 0);
		}
	}
	
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}
	
	public IResource[] getCopiedResources() {
		return copiedResources;
	}

	public boolean notEmpty() {
		return copiedResources != null && copiedResources.length > 0;
	}

}
