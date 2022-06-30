package fr.univamu.ism.docometre.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.views.FunctionsView;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.docometre.wizards.NewResourceWizard;

public class NewCustomFunctionHandler implements IHandler, ISelectionListener {
	
	private boolean enabled;
	private IContainer parentResource;
	private java.nio.file.Path functionsViewParentResource;

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(view != null) selectionChanged(view, view.getSite().getSelectionProvider().getSelection());
		else {
			view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SubjectsView.ID);
			if(view != null) selectionChanged(view, view.getSite().getSelectionProvider().getSelection());
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		NewResourceWizard newResourceWizard;
		if(parentResource != null) newResourceWizard = new NewResourceWizard(ResourceType.CUSTOMER_FUNCTION, parentResource, NewResourceWizard.CREATE);
		else newResourceWizard = new NewResourceWizard(ResourceType.CUSTOMER_FUNCTION, functionsViewParentResource, NewResourceWizard.CREATE);
		WizardDialog wizardDialog = new WizardDialog(shell, newResourceWizard);
		if(wizardDialog.open() == Window.OK) {
			try {
				if(parentResource != null) {
					final IFile customFunctionFile = parentResource.getFile(new Path(newResourceWizard.getResourceName() + Activator.customerFunctionFileExtension));
					File newCustomFunctionFile = new File(customFunctionFile.getLocation().toOSString());
					if(newCustomFunctionFile.createNewFile()) {
						customFunctionFile.refreshLocal(IResource.DEPTH_ZERO, null);
						ResourceProperties.setDescriptionPersistentProperty(customFunctionFile, newResourceWizard.getResourceDescription());
						ResourceProperties.setTypePersistentProperty(customFunctionFile, ResourceType.CUSTOMER_FUNCTION.toString());
						try {
							String userFunction = "USER_FUNCTION = YES";
							Files.write(Paths.get(customFunctionFile.getLocationURI()), userFunction.getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
						
						ExperimentsView.refresh(customFunctionFile.getParent(), new IResource[]{customFunctionFile});
						SubjectsView.refresh(customFunctionFile.getParent(), new IResource[]{customFunctionFile});
						FunctionsView.refresh();
					}
				} else if(functionsViewParentResource != null) {
					String fileName = newResourceWizard.getResourceName() + Activator.customerFunctionFileExtension;
					java.nio.file.Path fullFilePath = functionsViewParentResource.resolve(fileName);
					if(fullFilePath.toFile().createNewFile()) {
						try {
							String userFunction = "USER_FUNCTION = YES";
							Files.write(fullFilePath, userFunction.getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
						} catch (IOException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
					FunctionsView.refresh();
				}
				
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

	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		enabled = false;
		parentResource = null;
		functionsViewParentResource = null;
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
				Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
				boolean isProcessTest = ResourceType.isProcessTest((IResource) selectedObject);
				boolean isSubject =  ResourceType.isSubject((IResource) selectedObject);
				if(((IResource) selectedObject) instanceof IContainer && !isProcessTest && !isSubject) parentResource = (IContainer) selectedObject;
			}
		}
		if(part instanceof SubjectsView) {
			if(selection instanceof IStructuredSelection) {
				 if(((IStructuredSelection) selection).isEmpty()) parentResource = (IContainer) SelectedExprimentContributionItem.selectedExperiment;
				 else {
					 Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
					 boolean isSubject =  ResourceType.isSubject((IResource) selectedObject);
					 if(((IResource) selectedObject) instanceof IFolder && !isSubject ) parentResource = (IContainer) selectedObject;
					 else parentResource = ((IResource) selectedObject).getParent();
					 
				 }
			}
		}
		if(part instanceof FunctionsView) {
			if(selection instanceof IStructuredSelection) {
				if(!((IStructuredSelection) selection).isEmpty()) {
					Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
					if(selectedObject instanceof Entry<?, ?>) {
						Entry<String, java.nio.file.Path> entry = (Entry<String, java.nio.file.Path>) selectedObject;
						if(entry.getValue().toFile().isDirectory()) {
							String workspaceRootAbsolutePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
							java.nio.file.Path parentFolderPath = entry.getValue();
							java.nio.file.Path workspaceRootPath =java.nio.file.Path.of(workspaceRootAbsolutePath);
							if(parentFolderPath.startsWith(workspaceRootAbsolutePath)) {
								String relativePath = workspaceRootPath.relativize(parentFolderPath).toString();
								parentResource = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(relativePath);
							} else {
								functionsViewParentResource = entry.getValue();
							}
						}
					}
				}
			}
		}
		enabled = parentResource != null || functionsViewParentResource != null;
	}

}
