package fr.univamu.ism.docometre.analyse.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class DataProcessBatchEditor extends EditorPart implements PartNameRefresher  {
	
	public static String ID = "Docometre.DataProcessBatchEditor";
	
	private final class ProcessesSubjectsLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if(element instanceof IResource) {
				if(ResourceType.isDataProcessing((IResource) element)) return ((IResource)element).getName().replaceAll(Activator.dataProcessingFileExtension, ""); 
				else return ((IResource)element).getName();
			}
			return super.getText(element);
		}
	}
	
	private TableViewer processesTableViewer;
	private PartListenerAdapter partListenerAdapter;
	private IResource[] resources = null;
	private boolean dirty;
	private TableViewer subjectsTableViewer;

	public DataProcessBatchEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ObjectsController.serialize(getBatchDataProcessing());
		setDirty(false);
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
		return dirty;
	}
	
	private void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void createProcessToolBar(Composite parent) {
		Composite processToolBarContainer = new Composite(parent, SWT.NORMAL);
		processToolBarContainer.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		processToolBarContainer.setLayout(new GridLayout(3, true));
		GridLayout gl = (GridLayout)processToolBarContainer.getLayout();
		gl.horizontalSpacing = 2;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		
		Button deleteButton = new Button(processToolBarContainer, SWT.FLAT);
		deleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		deleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = processesTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					getBatchDataProcessing().removeProcesses(Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]));
					processesTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
		
		Button addButton = new Button(processToolBarContainer, SWT.FLAT);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		addButton.setToolTipText(DocometreMessages.Add_Tooltip);
		ResourceEditorInput editorInput = (ResourceEditorInput) getEditorInput();
		IResource resource = ObjectsController.getResourceForObject(editorInput.getObject());
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(getSite().getShell());
					progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							resources = ResourceProperties.getAllTypedResources(ResourceType.DATA_PROCESSING, resource.getProject(), monitor);
						}
					});
					ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getSite().getShell(), new ProcessesSubjectsLabelProvider());
					elementListSelectionDialog.setMultipleSelection(true);
					elementListSelectionDialog.setMessage(DocometreMessages.SelectProcessDialogMessage);
					elementListSelectionDialog.setTitle(DocometreMessages.SelectProcessDialogTitle);
					elementListSelectionDialog.setElements(resources);
					if(elementListSelectionDialog.open() == Dialog.OK) {
						IResource[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new IResource[elementListSelectionDialog.getResult().length]);
						getBatchDataProcessing().addProcesses(selection);
						processesTableViewer.refresh();
						setDirty(true);
						refreshPartName();
					}
				} catch (InvocationTargetException | InterruptedException e1) {
					e1.printStackTrace();
					Activator.logErrorMessageWithCause(e1);
				}
			}
		});
		
		Button activateDeactivateButton = new Button(processToolBarContainer, SWT.FLAT);
		activateDeactivateButton.setImage(Activator.getImage(IImageKeys.ACTIVATE_DEACTIVATE_ICON));
		activateDeactivateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		activateDeactivateButton.setToolTipText(DocometreMessages.ActivateDeactivate_Tooltip);
		activateDeactivateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = processesTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					BatchDataProcessingItem[] batchDataProcessingItems = Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						batchDataProcessingItem.setActivated(!batchDataProcessingItem.isActivated());
					}
					processesTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
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
		deleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = subjectsTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					getBatchDataProcessing().removeSubjects(Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]));
					subjectsTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
		
		Button addButton = new Button(subjectToolBarContainer, SWT.FLAT);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		addButton.setToolTipText(DocometreMessages.Add_Tooltip);
		ResourceEditorInput editorInput = (ResourceEditorInput) getEditorInput();
		IResource resource = ObjectsController.getResourceForObject(editorInput.getObject());
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(getSite().getShell());
					progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							resources = ResourceProperties.getAllTypedResources(ResourceType.SUBJECT, resource.getProject(), monitor);
						}
					});
					ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getSite().getShell(), new ProcessesSubjectsLabelProvider());
					elementListSelectionDialog.setMultipleSelection(true);
					elementListSelectionDialog.setMessage(DocometreMessages.SelectSubjectDialogMessage);
					elementListSelectionDialog.setTitle(DocometreMessages.SelectSubjectDialogTitle);
					elementListSelectionDialog.setElements(resources);
					if(elementListSelectionDialog.open() == Dialog.OK) {
						IResource[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new IResource[elementListSelectionDialog.getResult().length]);
						getBatchDataProcessing().addSubjects(selection);
						subjectsTableViewer.refresh();
						setDirty(true);
						refreshPartName();
					}
				} catch (InvocationTargetException | InterruptedException e1) {
					e1.printStackTrace();
					Activator.logErrorMessageWithCause(e1);
				}
			}
		});
		
		Button activateDeactivateButton = new Button(subjectToolBarContainer, SWT.FLAT);
		activateDeactivateButton.setImage(Activator.getImage(IImageKeys.ACTIVATE_DEACTIVATE_ICON));
		activateDeactivateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		activateDeactivateButton.setToolTipText(DocometreMessages.ActivateDeactivate_Tooltip);
		activateDeactivateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = subjectsTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					BatchDataProcessingItem[] batchDataProcessingItems = Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						batchDataProcessingItem.setActivated(!batchDataProcessingItem.isActivated());
					}
					subjectsTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
	}

	private void createProcessContainer(Composite parent) {
		Composite processContainer = new Composite(parent, SWT.NONE);
		processContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		processContainer.setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout)processContainer.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		processesTableViewer = new TableViewer(processContainer);
		processesTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		processesTableViewer.setContentProvider(new ArrayContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(inputElement instanceof BatchDataProcessing) return ((BatchDataProcessing)inputElement).getProcesses();
				return super.getElements(inputElement);
			}
		});
		processesTableViewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if(element instanceof BatchDataProcessingItem) {
					BatchDataProcessingItem batchDataProcessingItem = (BatchDataProcessingItem)element;
					if(batchDataProcessingItem.isActivated()) return Activator.getImage(IImageKeys.ACTIVATE_ICON);
					else return Activator.getImage(IImageKeys.DEACTIVATE_ICON);
				}
				return super.getImage(element);
			}
			
			@Override
			public String getText(Object element) {
				if(element instanceof BatchDataProcessingItem) {
					String path = ((BatchDataProcessingItem) element).getPath();
					path = path.replaceAll("\\" + Activator.dataProcessingFileExtension, "");
					return path;
				}
				return super.getText(element);
			}
		});
		processesTableViewer.setInput(getBatchDataProcessing());
		
		Composite processListToolBarContainer = new Composite(processContainer, SWT.NONE);
		processListToolBarContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		processListToolBarContainer.setLayout(new GridLayout());
		GridLayout gl2 = (GridLayout)processListToolBarContainer.getLayout();
		gl2.horizontalSpacing = 0;
		gl2.verticalSpacing = 2;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;

		Button upButton = new Button(processListToolBarContainer, SWT.FLAT);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		upButton.setToolTipText(DocometreMessages.Up_Label);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = processesTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					BatchDataProcessingItem[] batchDataProcessingItems = Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						getBatchDataProcessing().moveProcessUp(batchDataProcessingItem);
					}
					processesTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
		
		Button downButton = new Button(processListToolBarContainer, SWT.FLAT);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		downButton.setToolTipText(DocometreMessages.Down_Label);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = processesTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					List<Object> list = Arrays.asList(selection.toArray());
					Collections.reverse(list);
					BatchDataProcessingItem[] batchDataProcessingItems = list.toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						getBatchDataProcessing().moveProcessDown(batchDataProcessingItem);
					}
					processesTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
	}
	
	private void createSubjectContainer(Composite parent) {
		Composite subjectsContainer = new Composite(parent, SWT.NONE);
		subjectsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subjectsContainer.setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout)subjectsContainer.getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		subjectsTableViewer = new TableViewer(subjectsContainer);
		subjectsTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subjectsTableViewer.setContentProvider(new ArrayContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(inputElement instanceof BatchDataProcessing) return ((BatchDataProcessing)inputElement).getSubjects();
				return super.getElements(inputElement);
			}
		});
		subjectsTableViewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if(element instanceof BatchDataProcessingItem) {
					BatchDataProcessingItem batchDataProcessingItem = (BatchDataProcessingItem)element;
					if(batchDataProcessingItem.isActivated()) return Activator.getImage(IImageKeys.ACTIVATE_ICON);
					else return Activator.getImage(IImageKeys.DEACTIVATE_ICON);
				}
				return super.getImage(element);
			}
			@Override
			public String getText(Object element) {
				if(element instanceof BatchDataProcessingItem) {
					String path = ((BatchDataProcessingItem) element).getPath();
					return path;
				}
				return super.getText(element);
			}
		});
		subjectsTableViewer.setInput(getBatchDataProcessing());
		
		Composite subjectListToolBarContainer = new Composite(subjectsContainer, SWT.NONE);
		subjectListToolBarContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		subjectListToolBarContainer.setLayout(new GridLayout());
		GridLayout gl2 = (GridLayout)subjectListToolBarContainer.getLayout();
		gl2.horizontalSpacing = 0;
		gl2.verticalSpacing = 2;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		
		Button upButton = new Button(subjectListToolBarContainer, SWT.FLAT);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		upButton.setToolTipText(DocometreMessages.Up_Label);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = subjectsTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					BatchDataProcessingItem[] batchDataProcessingItems = Arrays.asList(selection.toArray()).toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						getBatchDataProcessing().moveSubjectUp(batchDataProcessingItem);
					}
					subjectsTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
		
		Button downButton = new Button(subjectListToolBarContainer, SWT.FLAT);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		downButton.setToolTipText(DocometreMessages.Down_Label);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = subjectsTableViewer.getStructuredSelection();
				if(!selection.isEmpty()) {
					List<Object> list = Arrays.asList(selection.toArray());
					Collections.reverse(list);
					BatchDataProcessingItem[] batchDataProcessingItems = list.toArray(new BatchDataProcessingItem[selection.size()]);
					for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
						getBatchDataProcessing().moveSubjectDown(batchDataProcessingItem);
					}
					subjectsTableViewer.refresh();
					setDirty(true);
					refreshPartName();
				}
			}
		});
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
		dataProcessingLabel.setText(DocometreMessages.Processes);
		
		Label separatorLabel = new Label(container, SWT.SEPARATOR);
		separatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 4));
		
		Label subjectLabel = new Label(container, SWT.BORDER);
		subjectLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		subjectLabel.setText(DocometreMessages.Subjects);
		
		Label separatorLabel2 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		separatorLabel2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		Label separatorLabel3 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		separatorLabel3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
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
		processesTableViewer.getTable().setFocus();
	}

	@Override
	public void refreshPartName() {
		IResource resource = ObjectsController.getResourceForObject(getBatchDataProcessing());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

}
