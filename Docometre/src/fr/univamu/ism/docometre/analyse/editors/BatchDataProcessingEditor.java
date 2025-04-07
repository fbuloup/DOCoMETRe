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
package fr.univamu.ism.docometre.analyse.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.EnableDisableHandler;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.AddHandler;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingItem;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessingProperties;
import fr.univamu.ism.docometre.analyse.datamodel.MoveDownHandler;
import fr.univamu.ism.docometre.analyse.datamodel.MoveUpHandler;
import fr.univamu.ism.docometre.analyse.datamodel.RemoveHandler;
import fr.univamu.ism.docometre.analyse.handlers.RunBatchDataProcessingDelegate;
import fr.univamu.ism.docometre.analyse.handlers.UpdateWorkbenchDelegate;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.analyse.wizard.ExportScriptWizard;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class BatchDataProcessingEditor extends EditorPart implements PartNameRefresher, IPropertyChangeListener  {
	
	private class SaveScriptAction extends Action {
		public SaveScriptAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.SAVE_AS_ICON));
			setText(DocometreMessages.SaveProcessingScript);
		}
		@Override
		public void run() {
			Object object = ((ResourceEditorInput)getEditorInput()).getObject();
			IResource batchResource = ObjectsController.getResourceForObject(object);
			ExportScriptWizard exportScriptWizard = new ExportScriptWizard(getBatchDataProcessing(), batchResource);
			WizardDialog wizardDialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), exportScriptWizard);
			wizardDialog.open();
		}
	}
	
	public static String ID = "Docometre.BatchDataProcessingEditor";
	
	private TableViewer processesTableViewer;
	private PartListenerAdapter partListenerAdapter;
	private boolean dirty;
	private TableViewer subjectsTableViewer;
	private FormToolkit formToolkit;
	private ObjectUndoContext resourceEditorUndoContext;
	private UndoRedoActionGroup undoRedoActionGroup;
	private ToolItem runButton;
	private Button runInMainThreadButton;

	public BatchDataProcessingEditor() {
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ObjectsController.serialize(getBatchDataProcessing());
		IResource resource = ObjectsController.getResourceForObject(getBatchDataProcessing());
		ResourceProperties.setRunInMainThread(resource, runInMainThreadButton.getSelection());
		SubjectsView.refresh(resource.getParent(), new IResource[] {resource});
		ExperimentsView.refresh(resource.getParent(), new IResource[] {resource});
		setDirty(false);
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	public BatchDataProcessing getBatchDataProcessing() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (BatchDataProcessing) resourceEditorInput.getObject();
	}
	
	public ObjectUndoContext getUndoContext() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		IResource resource = ObjectsController.getResourceForObject(object);
		String fullName = resource.getFullPath().toOSString();
		if(resourceEditorUndoContext == null) resourceEditorUndoContext = new ObjectUndoContext(this, "ResourceEditorUndoContext_" + fullName);
		return resourceEditorUndoContext;
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(GetResourceLabelDelegate.getLabel(ObjectsController.getResourceForObject(getBatchDataProcessing())));
		setTitleToolTip(getEditorInput().getToolTipText());
		
		undoRedoActionGroup = new UndoRedoActionGroup(getSite(), getUndoContext(), true);
		undoRedoActionGroup.fillActionBars(getEditorSite().getActionBars());
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == BatchDataProcessingEditor.this) {
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
		refreshPartName();
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
		deleteButton.addSelectionListener(new RemoveHandler(this, ResourceType.DATA_PROCESSING));
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem addButton = new ToolItem(toolBar, SWT.NULL);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setToolTipText(DocometreMessages.Add_Tooltip);
		addButton.addSelectionListener(new AddHandler(this, ResourceType.DATA_PROCESSING));
		
		ToolItem enableDisableButton = new ToolItem(toolBar, SWT.NULL);
		enableDisableButton.setImage(Activator.getImage(IImageKeys.ENABLE_DISABLE_ICON));
		enableDisableButton.setToolTipText(DocometreMessages.EnableDisable_Tooltip);
		enableDisableButton.addSelectionListener(new EnableDisableHandler(this, ResourceType.DATA_PROCESSING));
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem upButton = new ToolItem(toolBar, SWT.NULL);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setToolTipText(DocometreMessages.Up_Label);
		upButton.addSelectionListener(new MoveUpHandler(this, ResourceType.DATA_PROCESSING));
		
		ToolItem downButton = new ToolItem(toolBar, SWT.NULL);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setToolTipText(DocometreMessages.Down_Label);
		downButton.addSelectionListener(new MoveDownHandler(this, ResourceType.DATA_PROCESSING));
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		runButton = new ToolItem(toolBar, SWT.NULL);
		runButton.setImage(Activator.getImage(IImageKeys.RUN_ICON));
		runButton.setToolTipText(DocometreMessages.RunSelectedProcess);
		runButton.setEnabled(false);
		runButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if(!MathEngineFactory.getMathEngine().isStarted()) return;
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					pmd.run(false, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							BatchDataProcessing batchDataProcessing = getBatchDataProcessing();
							BatchDataProcessingItem batchDataProcessingItem = (BatchDataProcessingItem) processesTableViewer.getStructuredSelection().getFirstElement();
							String message = NLS.bind(DocometreMessages.RunningScriptLabel, batchDataProcessingItem.getPath().replaceAll(Activator.dataProcessingFileExtension, ""));
							monitor.beginTask(message, IProgressMonitor.UNKNOWN);
							batchDataProcessing.setRunSingleProcess(true);
							batchDataProcessing.setSelectedProcess(batchDataProcessingItem);
							RunBatchDataProcessingDelegate.run(getBatchDataProcessing(), monitor);
							batchDataProcessing.setRunSingleProcess(false);
							monitor.done();
							UpdateWorkbenchDelegate.update();
						}
					});
				} catch (InterruptedException | InvocationTargetException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
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
		deleteButton.addSelectionListener(new RemoveHandler(this, ResourceType.SUBJECT));
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem addButton = new ToolItem(toolBar, SWT.NULL);
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setToolTipText(DocometreMessages.Add_Tooltip);
		addButton.addSelectionListener(new AddHandler(this, ResourceType.SUBJECT));
		
		ToolItem enableDisableButton = new ToolItem(toolBar, SWT.NULL);
		enableDisableButton.setImage(Activator.getImage(IImageKeys.ENABLE_DISABLE_ICON));
		enableDisableButton.setToolTipText(DocometreMessages.EnableDisable_Tooltip);
		enableDisableButton.addSelectionListener(new EnableDisableHandler(this, ResourceType.SUBJECT));
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem upButton = new ToolItem(toolBar, SWT.NULL);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setToolTipText(DocometreMessages.Up_Label);
		upButton.addSelectionListener(new MoveUpHandler(this, ResourceType.SUBJECT));
		
		ToolItem downButton = new ToolItem(toolBar, SWT.NULL);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setToolTipText(DocometreMessages.Down_Label);
		downButton.addSelectionListener(new MoveDownHandler(this, ResourceType.SUBJECT));
		
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
		processesTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				runButton.setEnabled(event.getStructuredSelection().size() == 1);
			}
		});
		
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
		
		form.getToolBarManager().add(new SaveScriptAction());
		form.getToolBarManager().update(true);
		
		Composite container = form.getBody();
		container.setLayout(new GridLayout(2, true));
		GridLayout gl = (GridLayout)container.getLayout();
		gl.marginHeight = 2;
		gl.marginWidth = 5;
		
		Section introSection = formToolkit.createSection(container, Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE);
		introSection.setText(DocometreMessages.Introduction);
		introSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		FormText formText = formToolkit.createFormText(introSection, false);
		formText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		formText.setWhitespaceNormalized(true);
		formText.setImage("ADD", Activator.getImage(IImageKeys.ADD_ICON));
		formText.setImage("DEL", Activator.getImage(IImageKeys.DELETE_ICON));
		formText.setImage("UP", Activator.getImage(IImageKeys.UP_ICON));
		formText.setImage("DOWN", Activator.getImage(IImageKeys.DOWN_ICON));
		formText.setImage("ENDIS", Activator.getImage(IImageKeys.ENABLE_DISABLE_ICON));
		formText.setImage("DEACT", Activator.getImage(IImageKeys.DEACTIVATE_ICON));
		formText.setImage("RUN", Activator.getImage(IImageKeys.RUN_ICON));
		formText.setText(DocometreMessages.Explanation, true, false);
		
		introSection.setClient(formText);
		
		Button loadSubjectButton = formToolkit.createButton(container, DocometreMessages.AutoLoadSubjectTitle, SWT.CHECK);
		loadSubjectButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		loadSubjectButton.setSelection("true".equalsIgnoreCase(getBatchDataProcessing().getProperty(BatchDataProcessingProperties.AUTO_LOAD_SUBJECT)));
		loadSubjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = loadSubjectButton.getSelection();
				getBatchDataProcessing().setProperty(BatchDataProcessingProperties.AUTO_LOAD_SUBJECT, checked?"true":"false");
				setDirty(true);
			}
		});
		
		Button unloadSubjectButton = formToolkit.createButton(container, DocometreMessages.AutoUnLoadSubjectTitle, SWT.CHECK);
		unloadSubjectButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		unloadSubjectButton.setSelection("true".equalsIgnoreCase(getBatchDataProcessing().getProperty(BatchDataProcessingProperties.AUTO_UNLOAD_SUBJECT)));
		unloadSubjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = unloadSubjectButton.getSelection();
				getBatchDataProcessing().setProperty(BatchDataProcessingProperties.AUTO_UNLOAD_SUBJECT, checked?"true":"false");
				setDirty(true);
			}
		});
		
		runInMainThreadButton = formToolkit.createButton(container, DocometreMessages.RunInMainThreadButtonTitle, SWT.CHECK);
		runInMainThreadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		runInMainThreadButton.setSelection(ResourceProperties.isRunInMainThread(ObjectsController.getResourceForObject(getBatchDataProcessing())));
		runInMainThreadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		runInMainThreadButton.setVisible(MathEngineFactory.isPython());
		
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
		
		getSite().setSelectionProvider(processesTableViewer);
		
	}
	
	@Override
	public void dispose() {
		ObjectsController.removeHandle(getBatchDataProcessing());
		formToolkit.dispose();
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(this);
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

	public BatchDataProcessingItem[] getSelectedProcesses() {
		IStructuredSelection selection = (IStructuredSelection)processesTableViewer.getSelection();
		Object[] items = selection.toArray();
		return Arrays.asList(items).toArray(new BatchDataProcessingItem[items.length]);
	}
	
	public BatchDataProcessingItem[] getSelectedSubjects() {
		IStructuredSelection selection = (IStructuredSelection)subjectsTableViewer.getSelection();
		Object[] items = selection.toArray();
		return Arrays.asList(items).toArray(new BatchDataProcessingItem[items.length]);
	}
	
	public void refreshProcesses() {
		processesTableViewer.refresh();
		setDirty(true);
	}

	public void refreshSubjects() {
		subjectsTableViewer.refresh();
		setDirty(true);
	}
	
	private void updateUI() {
		runInMainThreadButton.setVisible(MathEngineFactory.isPython());
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		updateUI();
	}

}
