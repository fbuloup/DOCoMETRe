package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class DataProcessBatchEditor extends EditorPart implements PartNameRefresher  {
	
	public static String ID = "Docometre.DataProcessBatchEditor";
	private ListViewer processListViewer;
	private PartListenerAdapter partListenerAdapter;

	public DataProcessBatchEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	private BatchDataProcessing getBatchDataProcessing() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (BatchDataProcessing) resourceEditorInput.getObject();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(GetResourceLabelDelegate.getLabel(ObjectsController.getResourceForObject(getBatchDataProcessing())));
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == DataProcessBatchEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
				}
			}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
			}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
			}
			
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void createProcessToolBar(Composite parent) {
		Composite processToolBarContainer = new Composite(parent, SWT.NONE);
		processToolBarContainer.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		processToolBarContainer.setLayout(new GridLayout(3, true));
		GridLayout gl = (GridLayout)processToolBarContainer.getLayout();
		gl.horizontalSpacing = 2;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		
		Button deleteButton = new Button(processToolBarContainer, SWT.FLAT);
		deleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Button addButton = new Button(processToolBarContainer, SWT.FLAT);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Button activateButton = new Button(processToolBarContainer, SWT.FLAT);
		activateButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		activateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
	}
	
	private void createSubjectToolBar(Composite parent) {
		Composite subjectToolBarContainer = new Composite(parent, SWT.NONE);
		subjectToolBarContainer.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		subjectToolBarContainer.setLayout(new GridLayout(3, true));
		GridLayout gl = (GridLayout)subjectToolBarContainer.getLayout();
		gl.horizontalSpacing = 2;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		
		Button deleteButton = new Button(subjectToolBarContainer, SWT.FLAT);
		deleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Button addButton = new Button(subjectToolBarContainer, SWT.FLAT);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Button activateButton = new Button(subjectToolBarContainer, SWT.FLAT);
		activateButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		activateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
	}

	private void createProcessContainer(Composite parent) {
		Composite processContainer = new Composite(parent, SWT.NONE);
		processContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		processContainer.setLayout(new GridLayout(2, false));
		processListViewer = new ListViewer(processContainer);
		processListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite processListToolBarContainer = new Composite(processContainer, SWT.NONE);
		processListToolBarContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		processListToolBarContainer.setLayout(new GridLayout());
		GridLayout gl = (GridLayout)processListToolBarContainer.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 2;
		gl.marginHeight = 0;
		gl.marginWidth = 0;

		Button upButton = new Button(processListToolBarContainer, SWT.FLAT);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button downButton = new Button(processListToolBarContainer, SWT.FLAT);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
	}
	
	private void createSubjectContainer(Composite parent) {
		Composite subjectsContainer = new Composite(parent, SWT.NONE);
		subjectsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subjectsContainer.setLayout(new GridLayout(2, false));
		ListViewer subjectsListViewer = new ListViewer(subjectsContainer);
		subjectsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite subjectListToolBarContainer = new Composite(subjectsContainer, SWT.NONE);
		subjectListToolBarContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		subjectListToolBarContainer.setLayout(new GridLayout());
		GridLayout gl = (GridLayout)subjectListToolBarContainer.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 2;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		
		Button upButton = new Button(subjectListToolBarContainer, SWT.FLAT);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button downButton = new Button(subjectListToolBarContainer, SWT.FLAT);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		GridLayout gl = (GridLayout)container.getLayout();
		gl.marginHeight = 2;
		gl.marginWidth = 5;
		
		Label dataProcessingLabel = new Label(container, SWT.BORDER);
		dataProcessingLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		dataProcessingLabel.setText("TRAITEMENTS");
		
		Label separatorLabel = new Label(container, SWT.SEPARATOR);
		separatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 3));
		
		Label subjectLabel = new Label(container, SWT.BORDER);
		subjectLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		subjectLabel.setText("SUJETS");
		
		createProcessToolBar(container);
		
		createSubjectToolBar(container);
		
		createProcessContainer(container);
		
		createSubjectContainer(container);
		
	}
	
	@Override
	public void dispose() {
		super.dispose();
		ObjectsController.removeHandle(getBatchDataProcessing());
	}

	@Override
	public void setFocus() {
		processListViewer.getList().setFocus();
	}

	@Override
	public void refreshPartName() {
		IResource resource = ObjectsController.getResourceForObject(getBatchDataProcessing());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

}
