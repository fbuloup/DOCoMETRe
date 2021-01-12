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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
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
	private FormToolkit formToolkit;

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
	
	private void createProcessToolBar(Section parent) {
		
		ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
		
		
		ToolItem deleteButton = new ToolItem(toolBar, SWT.NULL);
		deleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
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
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem addButton = new ToolItem(toolBar, SWT.NULL);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
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
		
		ToolItem activateDeactivateButton = new ToolItem(toolBar, SWT.NULL);
		activateDeactivateButton.setImage(Activator.getImage(IImageKeys.ACTIVATE_DEACTIVATE_ICON));
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
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem upButton = new ToolItem(toolBar, SWT.NULL);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
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
		
		ToolItem downButton = new ToolItem(toolBar, SWT.NULL);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
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
		
		
		parent.setTextClient(toolBar);
		
	}
	
	private void createSubjectToolBar(Section parent) {
		
		ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
		
		ToolItem deleteButton = new ToolItem(toolBar, SWT.NULL);
		deleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
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
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem addButton = new ToolItem(toolBar, SWT.NULL);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
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
		
		ToolItem activateDeactivateButton = new ToolItem(toolBar, SWT.NULL);
		activateDeactivateButton.setImage(Activator.getImage(IImageKeys.ACTIVATE_DEACTIVATE_ICON));
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
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem upButton = new ToolItem(toolBar, SWT.NULL);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
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
		
		ToolItem downButton = new ToolItem(toolBar, SWT.NULL);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
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
		
		parent.setTextClient(toolBar);
	}

	private void createProcessContainer(Composite parent) {
		processesTableViewer = new TableViewer(parent);
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
		
	}
	
	private void createSubjectContainer(Composite parent) {
		subjectsTableViewer = new TableViewer(parent);
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
		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		formToolkit = new FormToolkit(parent.getDisplay());
		Form form = formToolkit.createForm(parent);
		formToolkit.decorateFormHeading(form);
		form.setText(DocometreMessages.OrganizeProcessesAndSubjects);
		
		Composite container = form.getBody();
		container.setLayout(new GridLayout(2, true));
		GridLayout gl = (GridLayout)container.getLayout();
		gl.marginHeight = 2;
		gl.marginWidth = 5;
		
		FormText formText = formToolkit.createFormText(container, false);
		formText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		formText.setWhitespaceNormalized(true);
		formText.setImage("ADD", Activator.getImage(IImageKeys.ADD_ICON));
		formText.setImage("DEL", Activator.getImage(IImageKeys.DELETE_ICON));
		formText.setImage("UP", Activator.getImage(IImageKeys.UP_ICON));
		formText.setImage("DOWN", Activator.getImage(IImageKeys.DOWN_ICON));
		formText.setImage("ACTDEACT", Activator.getImage(IImageKeys.ACTIVATE_DEACTIVATE_ICON));
		formText.setImage("DEACT", Activator.getImage(IImageKeys.DEACTIVATE_ICON));
		formText.setText(DocometreMessages.Explanation, true, false);
		
		Section processingSection = formToolkit.createSection(container, Section.DESCRIPTION | Section.TITLE_BAR);
		processingSection.setText(DocometreMessages.Processes);
		processingSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Section subjectsSection = formToolkit.createSection(container, Section.DESCRIPTION | Section.TITLE_BAR);
		subjectsSection.setText(DocometreMessages.Subjects);
		subjectsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		createProcessToolBar(processingSection);
		createSubjectToolBar(subjectsSection);
		createProcessContainer(container);
		createSubjectContainer(container);
		
	}
	
	@Override
	public void dispose() {
		ObjectsController.removeHandle(getBatchDataProcessing());
		formToolkit.dispose();
		super.dispose();
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
