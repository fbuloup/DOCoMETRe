package fr.univamu.ism.docometre.analyse.views;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.AnalysePerspective;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.views.ExperimentsLabelProvider;

public class SubjectsView extends ViewPart implements IResourceChangeListener, IPerspectiveListener {
	
	public static String ID = "Docometre.SubjectsView";
	
	private TreeViewer subjectsTreeViewer;
	private Composite parent;
	private Composite imageMessageContainer;

	public SubjectsView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		updateInput();
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}
	
	private void createMessageInfo() {
		// Remove tree viewer
		if(subjectsTreeViewer != null) {
			if(subjectsTreeViewer.getTree() != null && !subjectsTreeViewer.getTree().isDisposed()) subjectsTreeViewer.getTree().dispose();
			getSite().setSelectionProvider(null);
			subjectsTreeViewer = null;
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		}
		// Create infos
		imageMessageContainer = new Composite(parent, SWT.NORMAL);
		imageMessageContainer.setLayout(new GridLayout());
		Label imageSelectLabel = new Label(imageMessageContainer, SWT.CENTER);
		imageSelectLabel.setImage(Activator.getImage(IImageKeys.SELECT_ICON));
		imageSelectLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		Label messageLabel = new Label(imageMessageContainer, SWT.WRAP | SWT.CENTER);
		messageLabel.setText(DocometreMessages.SelectExperimentToProcessDataFilesMessage);
		messageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
	}

	private void createViewer() {
		// Remove infos
		if(imageMessageContainer != null && !imageMessageContainer.isDisposed()) imageMessageContainer.dispose();
		// Create tree viewer
		if(subjectsTreeViewer == null) {
			subjectsTreeViewer = new TreeViewer(parent);
			subjectsTreeViewer.setContentProvider(new SubjectsContentProvider());
			subjectsTreeViewer.setLabelProvider(new ExperimentsLabelProvider());
			getSite().setSelectionProvider(subjectsTreeViewer);
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
		}
		subjectsTreeViewer.setInput(null);
		subjectsTreeViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
	}

	public void updateInput() {
		if(SelectedExprimentContributionItem.selectedExperiment != null) {
			// Create viewer
			createViewer();
			// Update part name
			String partName = getPartName();
			partName = partName.replaceAll("\\s\\[.*\\]", "");
			setPartName(partName + " [" + SelectedExprimentContributionItem.selectedExperiment.getName() + "]");
		} else {
			// Create infos
			createMessageInfo();
			// Update part name
			String partName = getPartName();
			partName = partName.replaceAll("\\s\\[.*\\]", "");
			setPartName(partName);
		}
		// Layout
		parent.layout();
	}

	@Override
	public void setFocus() {
		if(subjectsTreeViewer != null) subjectsTreeViewer.getTree().setFocus();
	}

	private void refreshInput(IResourceChangeEvent event) {
		if(SelectedExprimentContributionItem.selectedExperiment == null) return;
		if(event != null && event.getDelta().findMember(SelectedExprimentContributionItem.selectedExperiment.getFullPath()) == null) return;
		if(subjectsTreeViewer != null) subjectsTreeViewer.refresh();
	}
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		refreshInput(event);
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		if(perspective.getId().equals(AnalysePerspective.ID)) refreshInput(null);
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
	}

	public static void refresh() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				SubjectsView subjectsView = (SubjectsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().findView(SubjectsView.ID);
				if (subjectsView != null)
					subjectsView.refreshInput(null);
			}
		});
		
	}
	
}
