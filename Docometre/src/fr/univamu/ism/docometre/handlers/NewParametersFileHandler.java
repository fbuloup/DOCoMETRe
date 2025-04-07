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
import org.eclipse.core.resources.ResourcesPlugin;
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

public class NewParametersFileHandler implements IHandler, ISelectionListener {
	
	private boolean enabled;
	private IContainer parentResource;
	
	public NewParametersFileHandler() {
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
		NewResourceWizard newResourceWizard = new NewResourceWizard(ResourceType.PARAMETERS, ResourcesPlugin.getWorkspace().getRoot(), NewResourceWizard.CREATE);
		WizardDialog wizardDialog = new WizardDialog(shell, newResourceWizard);
		if(wizardDialog.open() == Window.OK) {
			try {
				final IFile parametersFile = parentResource.getFile(new Path(newResourceWizard.getResourceName() + Activator.parametersFileExtension));
				File newParametersFile = new File(parametersFile.getLocation().toOSString());
				if(newParametersFile.createNewFile()) {
					parametersFile.refreshLocal(IResource.DEPTH_ZERO, null);
					ResourceProperties.setDescriptionPersistentProperty(parametersFile, newResourceWizard.getResourceDescription());
					ResourceProperties.setTypePersistentProperty(parametersFile, ResourceType.PARAMETERS.toString());
					ExperimentsView.refresh(parametersFile.getParent(), new IResource[]{parametersFile});
					StringBuffer text = new StringBuffer();
					text.append("# Welcome in this parameters file !\n");
					text.append("#\n");
					text.append("# You can use hashtag character (#) to insert comments.\n");
					text.append("# Everything to the right of this hashtag will be processed as a comment.\n");
					text.append("#\n");
					text.append("# Parameters delimiters can be semicolon (;) or comma (,) or colon (:).\n");
					text.append("#\n");
					text.append("# Please note that each uncommented line is associated to a trial of the current session, except for the first uncommented line :\n");
					text.append("# - The first uncommented line will correspond to the parameters names\n");
					text.append("# - The second uncommented line will correspond to the first trial\n");
					text.append("# - The third uncommented line will correspond to the second trial\n");
					text.append("# etc.\n");
					text.append("#\n");
					text.append("# Each uncommented line will be parsed to read the parameter name or value.\n");
					text.append("#\n");
					text.append("# Here is a parameters file example (the third parameter is of type string) :\n");
					text.append("# ParameterName1 ; ParameterName2 ; ParameterName3\n");
					text.append("# 10.25 ; 5 ; condition-1\n");
					text.append("# -10.25 ; 4 ; condition-2\n");
					text.append("# 5 ; 3 ; condition-1\n");
					text.append("# ...");
					Files.write(Paths.get(parametersFile.getLocationURI()), text.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
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
