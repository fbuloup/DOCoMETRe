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
package fr.univamu.ism.docometre.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.handlers.RunStopHandler;

public class ExperimentsView extends ViewPart implements IResourceChangeListener, IPerspectiveListener, IPropertyChangeListener {

	public static String ID = "Docometre.ExperimentsView";
	
	public static IResource currentSelectedResource;
	
	private class ExperimentsViewUndoContext extends UndoContext {
		@Override
		public String getLabel() {
			return "ExperimentsViewUndoContext";
		}
	}
	
	private class AssociateWithProcessAction extends Action {
		private IFolder[] trials;
		private IFile processFile;
		public AssociateWithProcessAction(IFolder[] trials, IFile processFile) {
			this.trials = trials;
			this.processFile = processFile;
			setText(processFile.getFullPath().toOSString().replaceAll(Activator.processFileExtension + "$", ""));
		}
		@Override
		public void run() {
			for (IFolder trial : trials) {
				ResourceProperties.setAssociatedProcessProperty(trial, processFile.getFullPath().toOSString());
			}
			ExperimentsView.refresh(trials[0].getProject(), trials);
//			Activator.refreshEditorsPartName(processesFiles);
		}
	}
	
	private class AssociateDACQAction extends Action {
		private IFile[] processesFiles;
		private IFile daqFile;
		public AssociateDACQAction(IFile[] processesFiles, IFile daqFile) {
			this.processesFiles = processesFiles;
			this.daqFile = daqFile;
			setText(daqFile.getFullPath().toOSString().replaceAll(Activator.daqFileExtension, ""));
		}
		@Override
		public void run() {
			for (IFile processFile : processesFiles) {
				ResourceProperties.setAssociatedDACQConfigurationProperty(processFile, daqFile.getFullPath().toOSString());
			}
			
			ExperimentsView.refresh(processesFiles[0].getProject(), processesFiles);
//			Activator.refreshEditorsPartName(processesFiles);
		}
	}
	
	private class CollapsAllAction extends Action {
		public CollapsAllAction() {
			setToolTipText(DocometreMessages.CollapseAllAction_Text);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.COLLAPSE_ALL));
		}
		@Override
		public void run() {
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					ExperimentsView.this.experimentsTreeViewer.collapseAll();
				}
			});
		}
	}
	
	private class LinkWithEditorAction extends Action implements IPartListener, ISelectionChangedListener {
		
		private boolean ignoreEditorActivation;
		private boolean ignoreSelectionChanged;
		
		private UIJob activateEditorJob = new UIJob("Link with editor Job - Activate part") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				ITreeSelection treeSelection = (ITreeSelection) ExperimentsView.this.experimentsTreeViewer.getSelection();
				if(treeSelection != null && !treeSelection.isEmpty()) {
					Object selectedResource = treeSelection.getFirstElement();
					ignoreEditorActivation = true;
					SafeRunner.run(new SafeRunnable() {
						@Override
						public void run() throws Exception {
							IEditorReference[] editorsReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
							for (IEditorReference editorReference : editorsReferences) {
								ResourceEditorInput resourceEditorInput = (ResourceEditorInput)editorReference.getEditorInput();
								Object object  = selectedResource;
								if(selectedResource instanceof IResource) object = ResourceProperties.getObjectSessionProperty((IResource)selectedResource);
								if(resourceEditorInput.isEditing(object)) {
									PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editorReference.getPart(false));
									ExperimentsView.this.setFocus();
									break;
								}
							}
						}
					});
					ignoreEditorActivation = false;
				}
				return Status.OK_STATUS;
			}
		};

		private UIJob updateSelectionJob = new UIJob("Link with editor Job - Update selection") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				SafeRunner.run(new SafeRunnable() {
					@Override
					public void run() throws Exception {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						if (page != null) {
							IEditorPart editor = page.getActiveEditor();
							if (editor != null) {
								ResourceEditorInput input = (ResourceEditorInput) editor.getEditorInput();
								IResource newSelectedResource;
								if(input.getObject() instanceof IResource) newSelectedResource = (IResource) input.getObject();
								else newSelectedResource = ObjectsController.getResourceForObject(input.getObject());
								IStructuredSelection newSelection = new StructuredSelection(newSelectedResource);
								if (!newSelection.isEmpty()) {
									ignoreSelectionChanged = true;
									ExperimentsView.this.experimentsTreeViewer.setSelection(newSelection, true);
									ignoreSelectionChanged = false;
								}
								editor.setFocus();
							}
						}
					}
				});
				return Status.OK_STATUS;
			}
		};
		
		
		public LinkWithEditorAction() {
			super(DocometreMessages.LinkWithEditorAction_Text, AS_CHECK_BOX);
			setToolTipText(DocometreMessages.LinkWithEditorAction_Text);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.LINK_TO_EDITOR_ICON));
			activateEditorJob.setSystem(true);
			updateSelectionJob.setSystem(true);
		}
		
		@Override
		public void run() {
			firePropertyChange(CHECKED, isChecked(), !isChecked());
			if(isChecked()) {
				ExperimentsView.this.getSite().getPage().addPartListener(this);
				ExperimentsView.this.experimentsTreeViewer.addSelectionChangedListener(this);
				updateSelectionJob.schedule(100);
			} else {
				ExperimentsView.this.getSite().getPage().removePartListener(this);
				ExperimentsView.this.experimentsTreeViewer.removeSelectionChangedListener(this);
			}
		}

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart && !ignoreEditorActivation) updateSelectionJob.schedule(100);
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			if (part instanceof IEditorPart && !ignoreEditorActivation) updateSelectionJob.schedule(100);
		}

		@Override
		public void partClosed(IWorkbenchPart part) {
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {
		}

		@Override
		public void partOpened(IWorkbenchPart part) {
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if(!ignoreSelectionChanged) activateEditorJob.schedule(100);
		}
		
	}
	
	public static ExperimentsViewUndoContext experimentsViewUndoContext;
	private TreeViewer experimentsTreeViewer;
	private MenuManager popUpMenuManager;
	private PartListenerAdapter partListenerAdapter;

	private IMemento memento;

	private LinkWithEditorAction linkWithEditorAction;

	public ExperimentsView() {
		experimentsViewUndoContext = new ExperimentsViewUndoContext();
	}
	
	/*
	 * Refresh experiment view
	 */
	public static void refresh(IResource parentResource, IResource[] resourcesToSelect) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				IViewPart experimentsView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
				if (experimentsView instanceof ExperimentsView)// Check null also !
					((ExperimentsView)experimentsView).refreshInput(parentResource, resourcesToSelect);
			}
		});
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IStructuredSelection sel = (IStructuredSelection) experimentsTreeViewer.getSelection();
		if (sel.isEmpty()) return;
		IMemento childMemento = memento.createChild("tree-selections");
		Iterator<IResource> iter = sel.iterator();
		while (iter.hasNext()) {
			IResource nodeName = iter.next();
			childMemento.createChild("selected-nodes", nodeName.getFullPath().toPortableString());
		}
		memento.putBoolean("LinkWithEditor", linkWithEditorAction.isChecked());
	}
	
	private void restoreState() {
		if(memento == null) return;
		IMemento selectionsMomento = memento.getChild("tree-selections");
		if (selectionsMomento != null) {
			IMemento selectedNodes[] = selectionsMomento.getChildren("selected-nodes");
			if (selectedNodes.length > 0) {
				ArrayList<IResource> selections = new ArrayList<IResource>(selectedNodes.length);
				for (int i = 0; i < selectedNodes.length; i++) {
					String path = selectedNodes[i].getID();
					if (path != null) {
						IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
						if(resource != null) selections.add(resource);
					}
				}
				experimentsTreeViewer.setSelection(new StructuredSelection(selections));
			}
		}
		Boolean linkWithEditor = memento.getBoolean("LinkWithEditor");
		linkWithEditor = linkWithEditor == null ? false:linkWithEditor;
		linkWithEditorAction.setChecked(linkWithEditor);
		linkWithEditorAction.run();
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		experimentsTreeViewer = new TreeViewer(parent);
		experimentsTreeViewer.setContentProvider(new ExperimentsContentProvider());
		ExperimentsLabelProvider experimentsLabelProvider = new ExperimentsLabelProvider();
		ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		experimentsTreeViewer.setLabelProvider(new DecoratingLabelProvider(experimentsLabelProvider, decorator));
		experimentsTreeViewer.setComparator(new ExperimentsViewerSorter());
		experimentsTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				try {
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									Object element = ((IStructuredSelection) experimentsTreeViewer.getSelection()).getFirstElement();
									experimentsTreeViewer.setExpandedState(element, !experimentsTreeViewer.getExpandedState(element));
									ApplicationActionBarAdvisor.openEditorAction.run();
								}
							});
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		});
		experimentsTreeViewer.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				TreeItem item = experimentsTreeViewer.getTree().getItem(new Point(e.x, e.y));
				if(item == null) experimentsTreeViewer.getTree().setSelection(new TreeItem[0]);
			}
		});
		
//		experimentsTreeViewer.addFilter(new ViewerFilter() {
//			@Override
//			public boolean select(Viewer viewer, Object parentElement, Object element) {
//				String currentPerspectiveID = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
//				boolean isDesignPerspective = currentPerspectiveID.equals(DesignPerspective.ID);
//				boolean isAcquirePerspective = currentPerspectiveID.equals(AcquirePerspective.id);
//				if(isDesignPerspective || isAcquirePerspective) return true;
//				return false;
//			}
//		});
		
		DragSource dragSource = new DragSource(experimentsTreeViewer.getTree(), DND.DROP_COPY);
		Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
		dragSource.setTransfer(types);
		
		dragSource.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				event.doit = false;
				if(!experimentsTreeViewer.getSelection().isEmpty()) {
					IResource resource = (IResource)((IStructuredSelection)experimentsTreeViewer.getSelection()).getFirstElement();
					event.doit = ResourceType.isSamples(resource);
				}
			}
			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					IResource resource = (IResource) ((IStructuredSelection) experimentsTreeViewer.getSelection()).getFirstElement();
					event.data = resource.getFullPath().toString();
				}
			}
			public void dragFinished(DragSourceEvent event) {
			}
		});
		
		
		getSite().setSelectionProvider(experimentsTreeViewer);

		UndoRedoActionGroup undoRedoActionGroup = new UndoRedoActionGroup(getSite(), experimentsViewUndoContext, true);
		undoRedoActionGroup.fillActionBars(getViewSite().getActionBars());
		
		getViewSite().getActionBars().getToolBarManager().add(new CollapsAllAction());
		linkWithEditorAction = new LinkWithEditorAction();
		getViewSite().getActionBars().getToolBarManager().add(linkWithEditorAction);
		
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), ApplicationActionBarAdvisor.deleteResourcesAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), ApplicationActionBarAdvisor.copyResourcesAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), ApplicationActionBarAdvisor.pasteResourcesAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.RENAME.getId(), ApplicationActionBarAdvisor.renameResourceAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), ApplicationActionBarAdvisor.refreshResourceAction);
		makePopupMenu();
		
		
		experimentsTreeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		experimentsTreeViewer.getTree().addSelectionListener(new SelectionListener() {
//			private boolean fireSecondSelectionEvent = true;

			@Override
			public void widgetSelected(SelectionEvent e) {
				currentSelectedResource = null;
				if(((IStructuredSelection)experimentsTreeViewer.getSelection()).size() == 1) {
					Object element  = ((IStructuredSelection)experimentsTreeViewer.getSelection()).getFirstElement();
					if(element instanceof IResource) currentSelectedResource = (IResource)element;
					RunStopHandler.refresh();
				}
//				if(fireSecondSelectionEvent) {
//					fireSecondSelectionEvent = false;
//					experimentsTreeViewer.getTree().notifyListeners(SWT.Selection, new Event());
//				} else fireSecondSelectionEvent = true;
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == ExperimentsView.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					ResourcesPlugin.getWorkspace().removeResourceChangeListener(ExperimentsView.this);
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(ExperimentsView.this);
				}
			}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			private void update(IWorkbenchPartReference partRef) {

			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		
		restoreState();
	}

	

	private void makePopupMenu() {
		popUpMenuManager = new MenuManager("ExperimentsViewPopUpMenu");
		experimentsTreeViewer.getTree().setMenu(popUpMenuManager.createContextMenu(experimentsTreeViewer.getTree()));
		getSite().registerContextMenu(popUpMenuManager, experimentsTreeViewer);
		popUpMenuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				MenuManager associateWithItemMenuManager = (MenuManager)popUpMenuManager.find("AssociateWith");
//				MenuManager setTypeItemMenuManager = (MenuManager)popUpMenuManager.find("SetType");
				IStructuredSelection selection = (IStructuredSelection)getSelection();
				associateWithItemMenuManager.setVisible(false);
//				setTypeItemMenuManager.setVisible(true);
				if(!selection.isEmpty()) {
					Object[] elements = ((IStructuredSelection)selection).toArray();
					IResource project = ((IResource) selection.getFirstElement()).getProject();
					boolean onlyProcesses = true;
					boolean onlyTrialsOrProcessesTests = true;
					boolean sameProject = true;
					boolean sameSystem = true;
//					boolean onlyUntypedResources = true; 
					String systemType = null;
					for (Object element : elements) {
//						onlyUntypedResources = onlyUntypedResources && ResourceType.isAnyTest((IResource)element);
						IResource associatedDACQ = ResourceProperties.getAssociatedDACQConfiguration((IResource)element);
						if(associatedDACQ != null) {
							systemType = ResourceProperties.getSystemPersistentProperty(associatedDACQ);
							if(systemType != null) break;
						}
					}
					for (Object element : elements) {
						onlyProcesses = onlyProcesses && ResourceType.isProcess((IResource)element);
						onlyTrialsOrProcessesTests = onlyTrialsOrProcessesTests && (ResourceType.isTrial((IResource)element) || ResourceType.isProcessTest((IResource)element));
						sameProject = sameProject && ((IResource)element).getProject().equals(project);
						IResource associatedDACQ = ResourceProperties.getAssociatedDACQConfiguration((IResource)element);
						if(associatedDACQ != null && systemType != null) sameSystem = sameSystem && systemType.equals(ResourceProperties.getSystemPersistentProperty(associatedDACQ));
					}
					if(onlyProcesses && sameProject && sameSystem) {
						ArrayList<IFile> processesFiles = new ArrayList<>();
						for (Object element : elements) processesFiles.add((IFile)element);
						IResource[] daqFiles = ResourceProperties.getAllTypedResources(ResourceType.DACQ_CONFIGURATION, processesFiles.get(0).getProject(), null);
						if(daqFiles.length > 0) associateWithItemMenuManager.setVisible(true);
						for (IResource daqFile : daqFiles) {
							if(ResourceProperties.getSystemPersistentProperty(daqFile) != null && (ResourceProperties.getSystemPersistentProperty(daqFile).equals(systemType) || systemType == null))
							associateWithItemMenuManager.add(new AssociateDACQAction(processesFiles.toArray(new IFile[processesFiles.size()]), (IFile)daqFile));
						}
					}
					if(onlyTrialsOrProcessesTests && sameProject && sameSystem) {
						ArrayList<IFolder> trialsFiles = new ArrayList<>();
						for (Object element : elements) trialsFiles.add((IFolder)element);
						IResource[] processesFiles = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, trialsFiles.get(0).getProject(), null);
						if(processesFiles.length > 0) associateWithItemMenuManager.setVisible(true);
						for (IResource processFile : processesFiles) {
							associateWithItemMenuManager.add(new AssociateWithProcessAction(trialsFiles.toArray(new IFolder[trialsFiles.size()]), (IFile)processFile));
						}
					}
					
					//if(onlyUntypedResources) setTypeItemMenuManager.setVisible(true);
				}
			}
		});
	}

	public void refreshInput(Object parentResource, Object[] newResourcesToSelect) {
		experimentsTreeViewer.refresh(parentResource, true);
		PlatformUI.getWorkbench().getDecoratorManager().update(ExperimentsLabelDecorator.ID);
		if(newResourcesToSelect != null) {
			List<Object> list = new LinkedList<>(Arrays.asList(newResourcesToSelect));
			for (Object object : newResourcesToSelect) {
				IResource resource = (IResource)object;
				if(!resource.exists()) list.remove(object);
			}
			StructuredSelection structuredSelection = new StructuredSelection(list.toArray());
			experimentsTreeViewer.setSelection(structuredSelection, true);
//			experimentsTreeViewer.getTree().notifyListeners(SWT.Selection, new Event());
		}
	}
	
	public ISelection getSelection() {
		return experimentsTreeViewer.getSelection();
	}

	@Override
	public void setFocus() {
		if(experimentsTreeViewer != null) experimentsTreeViewer.getTree().setFocus();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if(delta == null) return;
		if(delta.getResource() != null) 
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					experimentsTreeViewer.refresh(delta.getResource());
				}
			});
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		experimentsTreeViewer.refresh();
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		experimentsTreeViewer.refresh();
		
	}
	
	@Override
	public void dispose() {
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		ExperimentsView.refresh(null, null);
	}

}
