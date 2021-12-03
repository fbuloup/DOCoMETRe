package fr.univamu.ism.docometre.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.views.ExperimentsView;

public abstract class MarkTrialsHandler extends AbstractHandler implements ISelectionListener {
	
	protected IFolder[] resources;

	public MarkTrialsHandler() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
		IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(experimentsView != null) selectionChanged(experimentsView, experimentsView.getSite().getSelectionProvider().getSelection());
	}
	
	@Override
	public boolean isEnabled() {
		return resources != null && resources.length > 0;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part instanceof ExperimentsView) {
			if (selection instanceof IStructuredSelection) {
				Object[] elements = ((IStructuredSelection) selection).toArray();
				resources = new IFolder[0];
				for (int i = 0; i < elements.length; i++) {
					if(!(elements[i] instanceof IFolder)) return;
				}
				resources = new IFolder[elements.length];
				for (int i = 0; i < elements.length; i++) {
					resources[i] = (IFolder)elements[i];
				}
			}
		}
	}
	
	@Override
	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}

}
