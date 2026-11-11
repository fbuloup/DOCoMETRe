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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.docometre.wizards.NewResourceWizard;

public class NewImagesFileHandler implements IHandler, ISelectionListener {
	
	private boolean enabled;
	private IContainer parentResource;
	
	public NewImagesFileHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(experimentsView !=  null) selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}
	
	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		NewResourceWizard newResourceWizard = new NewResourceWizard(ResourceType.IMAGES, parentResource, NewResourceWizard.CREATE);
		WizardDialog wizardDialog = new WizardDialog(shell, newResourceWizard);
		if(wizardDialog.open() == Window.OK) {
			try {
				final IFile imagesFile = parentResource.getFile(new Path(newResourceWizard.getResourceName() + Activator.imagesFileExtension));
				File newImagesFile = new File(imagesFile.getLocation().toOSString());
				if(newImagesFile.createNewFile()) {
					imagesFile.refreshLocal(IResource.DEPTH_ZERO, null);
					ResourceProperties.setDescriptionPersistentProperty(imagesFile, newResourceWizard.getResourceDescription());
					ResourceProperties.setTypePersistentProperty(imagesFile, ResourceType.IMAGES.toString());
					ExperimentsView.refresh(imagesFile.getParent(), new IResource[]{imagesFile});
					StringBuffer text = new StringBuffer();
					text.append("# Welcome in this images file !\n");
					text.append("#\n");
					text.append("# It gives the images displayed by image charts, one image per trial of the session.\n");
					text.append("# Its name (without extension) is the generic images files name of the image charts.\n");
					text.append("#\n");
					text.append("# You can use hashtag character (#) to insert comments.\n");
					text.append("# Everything to the right of this hashtag will be processed as a comment.\n");
					text.append("# Comment lines and empty lines are ignored.\n");
					text.append("#\n");
					text.append("# Each uncommented line gives the path of the image of a trial :\n");
					text.append("# - The first uncommented line will correspond to the first trial\n");
					text.append("# - The second uncommented line will correspond to the second trial\n");
					text.append("# etc.\n");
					text.append("# When there are more trials than lines, the last line is used for the remaining trials.\n");
					text.append("#\n");
					text.append("# Paths are absolute, or relative to the workspace folder.\n");
					text.append("#\n");
					text.append("# Here is an images file example :\n");
					text.append("# MyExperiment/images/target_left.png    # trial 1\n");
					text.append("# MyExperiment/images/target_right.png   # trial 2\n");
					text.append("# C:\\Stimuli\\target_center.jpg          # trials 3 and following\n");
					Files.write(Paths.get(imagesFile.getLocationURI()), text.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
					imagesFile.refreshLocal(IResource.DEPTH_ZERO, null);
				};
			} catch (CoreException | IOException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		enabled = false;
		parentResource = null;
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
				Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
				if(ResourceType.isSession((IResource) selectedObject)) parentResource = (IContainer) selectedObject;
			}
			enabled = parentResource != null;
		}
	}

}
