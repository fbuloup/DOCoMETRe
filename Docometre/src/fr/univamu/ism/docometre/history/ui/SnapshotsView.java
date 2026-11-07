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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [04/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.history.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.history.FileChange;
import fr.univamu.ism.docometre.history.GitResources;
import fr.univamu.ism.docometre.history.Snapshot;
import fr.univamu.ism.docometre.history.SnapshotException;
import fr.univamu.ism.docometre.history.SnapshotManager;
import fr.univamu.ism.docometre.history.SnapshotRepository;
import fr.univamu.ism.docometre.history.SnapshotType;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;

/**
 * Read only view of the history of the experiment selected in experiments or subjects view :
 * current state of experiment (changes since the last snapshot), then all snapshots, the
 * most recent first. Details of selected row show the files it changed.
 * History is read in background jobs.
 */
public class SnapshotsView extends ViewPart implements ISelectionListener, IResourceChangeListener {
	
	public static final String ID = "Docometre.SnapshotsView";
	
	/* First row : experiment content compared to the last snapshot */
	private record CurrentState(List<FileChange> changes, Snapshot lastSnapshot) {
	}
	
	/* History read in background */
	private record HistoryData(IProject project, boolean historized, List<Snapshot> snapshots, CurrentState currentState, String error) {
	}
	
	/* Below this size, the view content scrolls (tables would otherwise get no room at all) */
	private static final int MINIMUM_WIDTH = 450;
	private static final int MINIMUM_TABLE_ROWS = 4;
	
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());
	
	private IProject experiment;
	private HistoryData historyData;
	private final IPropertyChangeListener preferenceListener = this::preferenceChanged;
	private Job loadJob;
	private Job detailsJob;
	
	private ScrolledComposite scrolledComposite;
	private Composite content;
	private SashForm sashForm;
	private Composite details;
	private CLabel headerLabel;
	private TableViewer snapshotsTableViewer;
	private Label titleLabel;
	private Label infoLabel;
	private Text descriptionText;
	private Label changesLabel;
	private TableViewer changesTableViewer;
	private Action refreshAction;
	private Action createSnapshotAction;
	private Action enableHistoryAction;
	private Action disableHistoryAction;
	private Action restoreAction;
	private Action deleteSnapshotAction;
	private Action showDifferencesAction;
	private Action restoreFileAction;
	private Button restoreButton;
	
	/*
	 * Show view and history of experiment
	 */
	public static void show(IProject experiment) {
		Display.getDefault().asyncExec(() -> {
			IWorkbenchPage page = getActivePage();
			if(page == null) return;
			try {
				IViewPart view = page.showView(ID, null, IWorkbenchPage.VIEW_VISIBLE);
				if(view instanceof SnapshotsView && experiment != null) ((SnapshotsView)view).setExperiment(experiment, true);
			} catch (PartInitException e) {
				Activator.logErrorMessageWithCause(e);
			}
		});
	}
	
	/*
	 * Reload history of experiment if view is opened (any thread)
	 */
	public static void refresh(IProject experiment) {
		Display.getDefault().asyncExec(() -> {
			IWorkbenchPage page = getActivePage();
			if(page == null) return;
			IViewPart view = page.findView(ID);
			if(view instanceof SnapshotsView) ((SnapshotsView)view).setExperiment(experiment, true);
		});
	}
	
	/*
	 * Reload history of experiment if view is opened and shows this experiment (any thread)
	 */
	public static void refreshIfShown(IProject experiment) {
		Display.getDefault().asyncExec(() -> {
			IWorkbenchPage page = getActivePage();
			if(page == null) return;
			IViewPart view = page.findView(ID);
			if(view instanceof SnapshotsView && experiment.equals(((SnapshotsView)view).getExperiment())) ((SnapshotsView)view).setExperiment(experiment, true);
		});
	}
	
	private static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window == null ? null : window.getActivePage();
	}
	
	/*
	 * Experiment whose history is shown, null if none
	 */
	public IProject getExperiment() {
		return experiment;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		// When the view is too small, its content scrolls instead of hiding the tables
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		content = new Composite(scrolledComposite, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(content);
		scrolledComposite.setContent(content);
		
		headerLabel = new CLabel(content, SWT.NONE);
		headerLabel.setImage(Activator.getImage(IImageKeys.HISTORY_ICON));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(headerLabel);
		
		sashForm = new SashForm(content, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);
		createSnapshotsTable(sashForm);
		createDetails(sashForm);
		sashForm.setWeights(55, 45);
		
		// Minimum size depends on width (wrapped labels) and on the sash position
		scrolledComposite.addControlListener(ControlListener.controlResizedAdapter(event -> updateMinimumSize()));
		details.addControlListener(ControlListener.controlResizedAdapter(event -> updateMinimumSize()));
		
		createActions();
		
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceListener);
		
		IProject selectedExperiment = HistoryOperations.getExperiment(getSite().getWorkbenchWindow().getSelectionService().getSelection(ExperimentsView.ID));
		setExperiment(selectedExperiment, true);
	}
	
	private void createSnapshotsTable(Composite parent) {
		snapshotsTableViewer = new TableViewer(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		Table table = snapshotsTableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		snapshotsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		createColumn(snapshotsTableViewer, DocometreMessages.SnapshotsView_Number, 50, new SnapshotColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof Snapshot ? String.valueOf(((Snapshot)element).number()) : "";
			}
		});
		createColumn(snapshotsTableViewer, DocometreMessages.SnapshotsView_Name, 200, new SnapshotColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof Snapshot ? ((Snapshot)element).name() : DocometreMessages.SnapshotsView_CurrentState;
			}
			@Override
			public Image getImage(Object element) {
				return element instanceof Snapshot ? Activator.getImage(IImageKeys.SNAPSHOT_ICON) : null;
			}
		});
		createColumn(snapshotsTableViewer, DocometreMessages.SnapshotsView_Date, 150, new SnapshotColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof Snapshot ? dateFormatter.format(((Snapshot)element).date()) : "";
			}
		});
		createColumn(snapshotsTableViewer, DocometreMessages.SnapshotsView_Type, 200, new SnapshotColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element instanceof Snapshot) return getTypeLabel(((Snapshot)element).type());
				CurrentState currentState = (CurrentState) element;
				if(!currentState.changes().isEmpty()) return NLS.bind(DocometreMessages.SnapshotsView_NotSaved, currentState.changes().size());
				return currentState.lastSnapshot() == null ? "" : NLS.bind(DocometreMessages.SnapshotsView_NoChange, currentState.lastSnapshot().number());
			}
		});
		createColumn(snapshotsTableViewer, DocometreMessages.SnapshotsView_Author, 120, new SnapshotColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof Snapshot ? ((Snapshot)element).author() : "";
			}
		});
		snapshotsTableViewer.addSelectionChangedListener(event -> showDetails(((IStructuredSelection)event.getSelection()).getFirstElement()));
	}
	
	/* Current state row is bold */
	private static class SnapshotColumnLabelProvider extends ColumnLabelProvider {
		@Override
		public Font getFont(Object element) {
			return element instanceof CurrentState ? JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT) : null;
		}
	}
	
	private static void createColumn(TableViewer tableViewer, String title, int width, ColumnLabelProvider labelProvider) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.LEFT);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);
		column.getColumn().setResizable(true);
		column.setLabelProvider(labelProvider);
	}
	
	private void createDetails(Composite parent) {
		details = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(details);
		
		titleLabel = new Label(details, SWT.NONE);
		titleLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(titleLabel);
		
		infoLabel = new Label(details, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).hint(100, SWT.DEFAULT).applyTo(infoLabel);
		
		descriptionText = new Text(details, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(100, 3 * descriptionText.getLineHeight()).applyTo(descriptionText);

		changesLabel = new Label(details, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).hint(100, SWT.DEFAULT).applyTo(changesLabel);
		
		changesTableViewer = new TableViewer(details, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		changesTableViewer.getTable().setHeaderVisible(true);
		changesTableViewer.getTable().setLinesVisible(true);
		changesTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(changesTableViewer.getControl());
		
		restoreButton = new Button(details, SWT.PUSH);
		restoreButton.setText(DocometreMessages.History_RestoreAction);
		restoreButton.setImage(Activator.getImage(IImageKeys.HISTORY_ICON));
		restoreButton.setEnabled(false);
		restoreButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> restoreSelectedSnapshot()));
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(restoreButton);
		createColumn(changesTableViewer, DocometreMessages.SnapshotsView_Change, 100, new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return getKindLabel(((FileChange)element).kind());
			}
		});
		createColumn(changesTableViewer, DocometreMessages.SnapshotsView_File, 400, new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return getPathLabel(((FileChange)element).path());
			}
		});
		// Preferred height is the minimum one (see updateMinimumSize), the table grows with the view.
		// Computed once columns exist (header height).
		((GridData) changesTableViewer.getControl().getLayoutData()).heightHint = getTableMinimumHeight(changesTableViewer.getTable());
	}
	
	private void createActions() {
		refreshAction = new Action(DocometreMessages.SnapshotsView_Refresh, Activator.getImageDescriptor("org.eclipse.ui", "icons/full/elcl16/refresh_nav.png")) {
			@Override
			public void run() {
				IProject project = experiment;
				if(project != null) {
					// Files modified by other programs : synchronize workspace, which updates decorations and this view
					Job refreshJob = Job.create(DocometreMessages.SnapshotsView_Refresh, monitor -> {
						project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						HistoryStatusCache.update(project);
					});
					refreshJob.setSystem(true);
					refreshJob.schedule();
				}
				setExperiment(project, true);
			}
		};
		createSnapshotAction = new Action(DocometreMessages.History_CreateSnapshotAction, Activator.getImageDescriptor(IImageKeys.SNAPSHOT_ICON)) {
			@Override
			public void run() {
				HistoryOperations.createSnapshot(getSite().getShell(), experiment);
			}
		};
		enableHistoryAction = new Action(DocometreMessages.History_EnableTitle, Activator.getImageDescriptor(IImageKeys.HISTORY_ICON)) {
			@Override
			public void run() {
				HistoryOperations.enableHistory(getSite().getShell(), experiment);
			}
		};
		// In view menu only, to avoid deleting history by mistake
		disableHistoryAction = new Action(DocometreMessages.History_DisableTitle + "...") {
			@Override
			public void run() {
				HistoryOperations.disableHistory(getSite().getShell(), experiment);
			}
		};
		getViewSite().getActionBars().getMenuManager().add(disableHistoryAction);
		restoreAction = new Action(DocometreMessages.History_RestoreAction, Activator.getImageDescriptor(IImageKeys.HISTORY_ICON)) {
			@Override
			public void run() {
				restoreSelectedSnapshot();
			}
		};
		restoreAction.setEnabled(false);
		deleteSnapshotAction = new Action(DocometreMessages.History_DeleteSnapshotAction, Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE)) {
			@Override
			public void run() {
				Object element = snapshotsTableViewer.getStructuredSelection().getFirstElement();
				if(element instanceof Snapshot) HistoryOperations.deleteSnapshot(getSite().getShell(), experiment, (Snapshot) element);
			}
		};
		deleteSnapshotAction.setEnabled(false);
		MenuManager menuManager = new MenuManager();
		menuManager.add(restoreAction);
		menuManager.add(new Separator());
		menuManager.add(deleteSnapshotAction);
		
		// Files of details : differences and restoration of one file
		showDifferencesAction = new Action(DocometreMessages.History_ShowDiffAction) {
			@Override
			public void run() {
				showSelectedFileDifferences();
			}
		};
		restoreFileAction = new Action(DocometreMessages.History_RestoreFileTitle, Activator.getImageDescriptor(IImageKeys.HISTORY_ICON)) {
			@Override
			public void run() {
				FileChange change = (FileChange) changesTableViewer.getStructuredSelection().getFirstElement();
				Snapshot snapshot = getRestoreTarget(snapshotsTableViewer.getStructuredSelection().getFirstElement());
				if(change != null && snapshot != null) HistoryOperations.restoreFile(getSite().getShell(), experiment, snapshot, change.path());
			}
		};
		MenuManager changesMenuManager = new MenuManager();
		changesMenuManager.setRemoveAllWhenShown(true);
		changesMenuManager.addMenuListener(manager -> {
			FileChange change = (FileChange) changesTableViewer.getStructuredSelection().getFirstElement();
			if(change == null) return;
			manager.add(showDifferencesAction);
			Snapshot snapshot = getRestoreTarget(snapshotsTableViewer.getStructuredSelection().getFirstElement());
			// Restoring a file modifies history : not available if history is disabled in preferences
			if(snapshot != null && HistoryOperations.isHistoryEnabled() && !SnapshotRepository.FOLDERS_LIST_FILE_NAME.equals(change.path())) {
				restoreFileAction.setText(NLS.bind(DocometreMessages.History_RestoreFileAction, snapshot.number()));
				manager.add(new Separator());
				manager.add(restoreFileAction);
			}
		});
		changesTableViewer.getTable().setMenu(changesMenuManager.createContextMenu(changesTableViewer.getTable()));
		changesTableViewer.addDoubleClickListener(event -> showSelectedFileDifferences());
		snapshotsTableViewer.getTable().setMenu(menuManager.createContextMenu(snapshotsTableViewer.getTable()));
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(enableHistoryAction);
		toolBarManager.add(createSnapshotAction);
		toolBarManager.add(refreshAction);
	}
	
	/*
	 * Height of a table showing its header and MINIMUM_TABLE_ROWS rows, with its border and scroll bars
	 */
	private static int getTableMinimumHeight(Table table) {
		int height = table.getHeaderHeight() + MINIMUM_TABLE_ROWS * table.getItemHeight();
		return table.computeTrim(0, 0, 0, height).height;
	}
	
	/*
	 * Minimum size of view content : under it, scroll bars are shown. The snapshots table and the
	 * table of changes keep at least MINIMUM_TABLE_ROWS visible rows, and details keep their labels,
	 * description and button.
	 */
	private void updateMinimumSize() {
		if(scrolledComposite == null || scrolledComposite.isDisposed()) return;
		int width = Math.max(scrolledComposite.getClientArea().width, MINIMUM_WIDTH);
		GridLayout layout = (GridLayout) content.getLayout();
		int headerHeight = headerLabel.computeSize(width - 2 * layout.marginWidth, SWT.DEFAULT).y;
		int[] weights = sashForm.getWeights();
		int detailsWidth = details.getSize().x > 0 ? details.getSize().x : width * weights[1] / Math.max(1, weights[0] + weights[1]);
		int detailsHeight = details.computeSize(detailsWidth, SWT.DEFAULT).y;
		int sashHeight = Math.max(detailsHeight, getTableMinimumHeight(snapshotsTableViewer.getTable()));
		int height = 2 * layout.marginHeight + headerHeight + layout.verticalSpacing + sashHeight;
		// Changing minimum size lays content out again : only when it really changes
		if(scrolledComposite.getMinWidth() != MINIMUM_WIDTH || scrolledComposite.getMinHeight() != height) scrolledComposite.setMinSize(MINIMUM_WIDTH, height);
	}
	
	/*
	 * Snapshot whose files are restored from selected row : the snapshot itself, or the last
	 * snapshot for current state (to cancel a change)
	 */
	private static Snapshot getRestoreTarget(Object row) {
		if(row instanceof Snapshot) return (Snapshot) row;
		if(row instanceof CurrentState) return ((CurrentState)row).lastSnapshot();
		return null;
	}
	
	/*
	 * Differences of selected file : changes saved by selected snapshot, or changes since the last
	 * snapshot for current state
	 */
	private void showSelectedFileDifferences() {
		FileChange change = (FileChange) changesTableViewer.getStructuredSelection().getFirstElement();
		Object row = snapshotsTableViewer.getStructuredSelection().getFirstElement();
		if(change == null || row == null) return;
		if(row instanceof Snapshot) HistoryOperations.showDifferences(getSite().getShell(), experiment, getPreviousSnapshot((Snapshot) row), (Snapshot) row, change.path());
		else HistoryOperations.showDifferences(getSite().getShell(), experiment, ((CurrentState)row).lastSnapshot(), null, change.path());
	}
	
	private void restoreSelectedSnapshot() {
		Object element = snapshotsTableViewer.getStructuredSelection().getFirstElement();
		if(element instanceof Snapshot) HistoryOperations.restoreSnapshot(getSite().getShell(), experiment, (Snapshot) element);
	}
	
	/*
	 * Label of a file path : technical files of history are given a readable name
	 */
	static String getPathLabel(String path) {
		if(SnapshotRepository.FOLDERS_LIST_FILE_NAME.equals(path)) return DocometreMessages.SnapshotsView_FoldersList;
		if(ResourceProperties.metadataFileName.equals("/" + path)) return DocometreMessages.SnapshotsView_ExperimentProperties;
		return path;
	}
	
	private static String getTypeLabel(SnapshotType type) {
		switch (type) {
		case AUTO:
			return DocometreMessages.SnapshotType_AUTO;
		case RESTORE:
			return DocometreMessages.SnapshotType_RESTORE;
		default:
			return DocometreMessages.SnapshotType_MANUAL;
		}
	}
	
	private static String getKindLabel(FileChange.Kind kind) {
		switch (kind) {
		case ADDED:
			return DocometreMessages.FileChange_ADDED;
		case DELETED:
			return DocometreMessages.FileChange_DELETED;
		default:
			return DocometreMessages.FileChange_MODIFIED;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Loading history
	
	/*
	 * Show history of experiment (UI thread). History is read again if reload is true or if
	 * experiment changes.
	 */
	private void setExperiment(IProject newExperiment, boolean reload) {
		if(headerLabel == null || headerLabel.isDisposed()) return;
		boolean changed = newExperiment == null ? experiment != null : !newExperiment.equals(experiment);
		experiment = newExperiment;
		if(changed || reload) load(0);
	}
	
	private void load(long delay) {
		IProject loadedExperiment = experiment;
		if(loadJob != null) loadJob.cancel();
		if(loadedExperiment == null) {
			updateView(new HistoryData(null, false, Collections.emptyList(), null, null));
			return;
		}
		if(historyData == null || !loadedExperiment.equals(historyData.project())) headerLabel.setText(NLS.bind(DocometreMessages.SnapshotsView_Loading, loadedExperiment.getName()));
		loadJob = Job.create(NLS.bind(DocometreMessages.SnapshotsView_Loading, loadedExperiment.getName()), monitor -> {
			HistoryData data = readHistory(loadedExperiment);
			if(monitor.isCanceled()) return;
			Display.getDefault().asyncExec(() -> {
				if(headerLabel.isDisposed() || !loadedExperiment.equals(experiment)) return;
				updateView(data);
			});
		});
		loadJob.setSystem(true);
		// Experiment must not be closed, deleted or modified while its history is read
		loadJob.setRule(loadedExperiment);
		loadJob.schedule(delay);
	}
	
	private static HistoryData readHistory(IProject project) {
		try {
			SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
			if(repository == null) return new HistoryData(project, false, Collections.emptyList(), null, null);
			List<Snapshot> snapshots = repository.getSnapshots();
			CurrentState currentState = new CurrentState(repository.getChanges(), snapshots.isEmpty() ? null : snapshots.get(0));
			return new HistoryData(project, true, snapshots, currentState, null);
		} catch (SnapshotException e) {
			Activator.logErrorMessageWithCause(e);
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			return new HistoryData(project, false, Collections.emptyList(), null, NLS.bind(DocometreMessages.History_ErrorMessage, project.getName(), cause.getLocalizedMessage()));
		}
	}
	
	private void updateView(HistoryData data) {
		Object selectedElement = snapshotsTableViewer.getStructuredSelection().getFirstElement();
		historyData = data;
		updateHeader();
		
		ArrayList<Object> rows = new ArrayList<>();
		if(data.historized()) {
			rows.add(data.currentState());
			rows.addAll(data.snapshots());
		}
		snapshotsTableViewer.setInput(rows);
		
		// Keep selected snapshot if it still exists, otherwise select current state
		Object newSelection = rows.isEmpty() ? null : rows.get(0);
		if(selectedElement instanceof Snapshot) {
			for (Snapshot snapshot : data.snapshots()) {
				if(snapshot.commitId().equals(((Snapshot)selectedElement).commitId())) newSelection = snapshot;
			}
		}
		if(newSelection != null) snapshotsTableViewer.setSelection(new StructuredSelection(newSelection), true);
		else showDetails(null);
		
		updateActionsEnablement();
		refreshAction.setEnabled(data.project() != null);
	}
	
	/*
	 * Header : experiment and number of snapshots, read only mode if history is disabled in preferences
	 */
	private void updateHeader() {
		HistoryData data = historyData;
		if(data == null) return;
		IProject project = data.project();
		String header;
		if(project == null) header = DocometreMessages.SnapshotsView_NoExperiment;
		else if(data.error() != null) header = data.error();
		else if(!data.historized()) header = NLS.bind(DocometreMessages.SnapshotsView_NotHistorized, project.getName());
		else header = NLS.bind(DocometreMessages.SnapshotsView_Header, project.getName(), data.snapshots().size());
		if(project != null && !HistoryOperations.isHistoryEnabled()) header += " - " + DocometreMessages.SnapshotsView_HistoryDisabled;
		headerLabel.setText(header);
		headerLabel.setToolTipText(header);
		updateMinimumSize();
	}
	
	/*
	 * Actions which modify history are only available if history is enabled in preferences :
	 * otherwise, histories can only be viewed
	 */
	private void updateActionsEnablement() {
		HistoryData data = historyData;
		boolean enabled = HistoryOperations.isHistoryEnabled() && data != null;
		boolean historized = enabled && data.historized();
		Object element = snapshotsTableViewer.getStructuredSelection().getFirstElement();
		if(enableHistoryAction != null) enableHistoryAction.setEnabled(enabled && data.project() != null && !data.historized() && data.error() == null);
		if(createSnapshotAction != null) createSnapshotAction.setEnabled(historized);
		if(disableHistoryAction != null) disableHistoryAction.setEnabled(historized);
		if(restoreButton != null) restoreButton.setEnabled(historized && element instanceof Snapshot);
		if(restoreAction != null) restoreAction.setEnabled(historized && element instanceof Snapshot);
		if(deleteSnapshotAction != null) deleteSnapshotAction.setEnabled(historized && element instanceof Snapshot && data.snapshots().size() > 1);
	}
	
	/*
	 * Experiments history enabled or disabled in preferences (any thread)
	 */
	private void preferenceChanged(PropertyChangeEvent event) {
		if(!GeneralPreferenceConstants.HISTORY_ENABLED.equals(event.getProperty())) return;
		Display.getDefault().asyncExec(() -> {
			if(headerLabel == null || headerLabel.isDisposed()) return;
			updateHeader();
			updateActionsEnablement();
		});
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Details of selected row
	
	private void showDetails(Object element) {
		if(detailsJob != null) detailsJob.cancel();
		titleLabel.setText("");
		infoLabel.setText("");
		descriptionText.setText("");
		changesLabel.setText("");
		changesTableViewer.setInput(Collections.emptyList());
		if(element instanceof CurrentState) {
			CurrentState currentState = (CurrentState) element;
			titleLabel.setText(DocometreMessages.SnapshotsView_CurrentState);
			if(currentState.lastSnapshot() == null) changesLabel.setText(DocometreMessages.SnapshotsView_ChangesWithoutSnapshot);
			else changesLabel.setText(NLS.bind(DocometreMessages.SnapshotsView_ChangesSinceLast, currentState.lastSnapshot().number()));
			changesTableViewer.setInput(currentState.changes());
		} else if(element instanceof Snapshot) {
			Snapshot snapshot = (Snapshot) element;
			titleLabel.setText(NLS.bind("{0} {1} - {2}", new Object[] {DocometreMessages.SnapshotsView_Number, snapshot.number(), snapshot.name()}));
			infoLabel.setText(getInformation(snapshot));
			descriptionText.setText(snapshot.description().isEmpty() ? DocometreMessages.SnapshotsView_NoDescription : snapshot.description());
			Snapshot previousSnapshot = getPreviousSnapshot(snapshot);
			if(previousSnapshot == null) changesLabel.setText(DocometreMessages.SnapshotsView_FirstSnapshotChanges);
			else changesLabel.setText(NLS.bind(DocometreMessages.SnapshotsView_SnapshotChanges, previousSnapshot.number()));
			loadChanges(previousSnapshot, snapshot);
		}
		updateActionsEnablement();
		titleLabel.getParent().layout(true, true);
		updateMinimumSize();
	}
	
	private String getInformation(Snapshot snapshot) {
		StringBuilder information = new StringBuilder();
		information.append(dateFormatter.format(snapshot.date()));
		if(!snapshot.author().isBlank()) information.append(" - ").append(snapshot.author());
		information.append(" - ").append(getTypeLabel(snapshot.type()));
		if(snapshot.restoredFromId() != null) {
			for (Snapshot restoredSnapshot : historyData.snapshots()) {
				if(restoredSnapshot.commitId().equals(snapshot.restoredFromId())) information.append(" - ").append(NLS.bind(DocometreMessages.SnapshotsView_RestoredFrom, restoredSnapshot.number()));
			}
		}
		ArrayList<String> sameContentNumbers = new ArrayList<>();
		for (Snapshot otherSnapshot : historyData.snapshots()) {
			if(otherSnapshot != snapshot && otherSnapshot.hasSameContent(snapshot)) sameContentNumbers.add(0, String.valueOf(otherSnapshot.number()));
		}
		if(!sameContentNumbers.isEmpty()) information.append(" - ").append(NLS.bind(DocometreMessages.SnapshotsView_SameContentAs, String.join(", ", sameContentNumbers)));
		return information.toString();
	}
	
	/*
	 * Snapshot preceding snapshot in history (numbers of deleted snapshots are missing)
	 */
	private Snapshot getPreviousSnapshot(Snapshot snapshot) {
		if(historyData == null) return null;
		List<Snapshot> snapshots = historyData.snapshots();
		for (int i = 0; i < snapshots.size() - 1; i++) {
			if(snapshots.get(i).commitId().equals(snapshot.commitId())) return snapshots.get(i + 1);
		}
		return null;
	}
	
	private void loadChanges(Snapshot previousSnapshot, Snapshot snapshot) {
		IProject project = historyData.project();
		HistoryData data = historyData;
		detailsJob = Job.create(snapshot.name(), monitor -> {
			List<FileChange> changes;
			try {
				SnapshotRepository repository = SnapshotManager.getInstance().getRepository(project);
				if(repository == null) return;
				changes = repository.getChanges(previousSnapshot, snapshot);
			} catch (SnapshotException e) {
				Activator.logErrorMessageWithCause(e);
				return;
			}
			if(monitor.isCanceled()) return;
			Display.getDefault().asyncExec(() -> {
				if(changesTableViewer.getControl().isDisposed() || historyData != data) return;
				if(snapshotsTableViewer.getStructuredSelection().getFirstElement() != snapshot) return;
				changesTableViewer.setInput(changes);
			});
		});
		detailsJob.setSystem(true);
		// Experiment must not be closed, deleted or modified while its history is read
		detailsJob.setRule(project);
		detailsJob.schedule();
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Listeners

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(part == this) return;
		IProject selectedExperiment = HistoryOperations.getExperiment(selection);
		if(selectedExperiment != null) setExperiment(selectedExperiment, false);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IProject project = experiment;
		if(project == null || event.getDelta() == null) return;
		IResourceDelta projectDelta = event.getDelta().findMember(project.getFullPath());
		if(projectDelta == null || !GitResources.hasRelevantChanges(projectDelta)) return;
		// Experiment content changed : read current state again, once changes are finished
		Display.getDefault().asyncExec(() -> {
			if(headerLabel.isDisposed() || !project.equals(experiment)) return;
			if(projectDelta.getKind() == IResourceDelta.REMOVED) experiment = null;
			load(1000);
		});
	}

	@Override
	public void setFocus() {
		snapshotsTableViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
		if(loadJob != null) loadJob.cancel();
		if(detailsJob != null) detailsJob.cancel();
		super.dispose();
	}

}
