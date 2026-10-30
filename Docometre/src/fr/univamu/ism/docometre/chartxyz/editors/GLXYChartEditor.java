package fr.univamu.ism.docometre.chartxyz.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.TimeChannel;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.editors.Chart2D3DBehaviour;
import fr.univamu.ism.docometre.analyse.editors.FrontEndCutsHandler;
import fr.univamu.ism.docometre.analyse.editors.MarkersChangeListener;
import fr.univamu.ism.docometre.analyse.editors.RangeHandler;
import fr.univamu.ism.docometre.analyse.editors.TrialsEditor;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLChart2D;
import fr.univamu.ism.docometre.chartxyz.Point2D;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

/**
 * XY chart editor with an OpenGL chart (chartxyz): OpenGL version of
 * XYChartEditor.
 *
 * Curves: one curve per couple of signals (x, y) and per selected trial,
 * named "key.trial" as the SWTChart series; the front and end cuts are
 * applied by building the curves from the samples between the cuts.
 *
 * Scales: the automatic scale is the initial view of the chart (fit to the
 * data); fixed bounds are applied as a zoom window. Any change of the view
 * by the user (wheel, rectangle, pan, keys) disables the automatic scale and
 * updates the bounds, as with SWTChart; going back to the initial view (R
 * key, zoomToFit) enables it again.
 *
 * Markers: as in the SWTChart version, the markers of the x signal (if it is
 * not the time) and of the y signal are only displayed, as circles on the
 * points of the curves (one group (curve, label) per series): the mouse
 * actions on the markers of the chart are disabled. They follow the changes
 * made in the other editors (see {@link #markersChanged(Channel)}).
 */
public class GLXYChartEditor extends EditorPart implements ISelectionChangedListener, Chart2D3DBehaviour, TrialsEditor, MarkersChangeListener {

	public static String ID = "Docometre.GLXYChartEditor";

	private class CategoriesChangeListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			HashSet<Integer> trialsToSelect = new HashSet<>();
			IStructuredSelection structuredSelection = (IStructuredSelection) categoriesListViewer.getSelection();
			Object[] selections = structuredSelection.toArray();
			for (Object selection : selections) {
				Channel category = (Channel)selection;
				Integer[] trials = MathEngineFactory.getMathEngine().getTrialsListForCategory(category);
				trialsToSelect.addAll(Arrays.asList(trials));
			}
			IStructuredSelection selection = new StructuredSelection(trialsToSelect.toArray());
			trialsListViewer.setSelection(selection);
		}
	}

	/** Curve of the chart with its signals and its trial. */
	private static final class Series {
		final String key;
		final Channel xSignal;
		final Channel ySignal;
		final int trialNumber;
		Series(String key, Channel xSignal, Channel ySignal, int trialNumber) {
			this.key = key;
			this.xSignal = xSignal;
			this.ySignal = ySignal;
			this.trialNumber = trialNumber;
		}
	}

	private XYChart xyChartData;
	private SashForm container;
	private ListViewer trialsListViewer;
	private GLChart2D chart;
	private final Map<String, Series> seriesMap = new LinkedHashMap<>(); // series of the chart by curve name, in creation order
	private final Map<String, String> frequencyChecks = new HashMap<>(); // key -> error of sampling frequencies ("" if none), logged once
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Spinner frontCutSpinner;
	private Spinner endCutSpinner;
	private boolean dirty;
	private Button autoScaleButton;
	private Group scaleValuesGroup;
	private Button useSameColorButton;
	private ListViewer categoriesListViewer;
	private CategoriesChangeListener categoriesChangeListener;
	private CTabFolder trialsCategoriesTabFolder;
	private CTabItem trialsTabItem;
	private CTabItem categoriesTabItem;

	public GLXYChartEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IResource xyChartFile = ObjectsController.getResourceForObject(xyChartData);
		ObjectsController.serialize((IFile) xyChartFile, xyChartData);
		dirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		xyChartData = ((XYChart)((ResourceEditorInput)input).getObject());
		IResource resource = ObjectsController.getResourceForObject(xyChartData);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		// Editor icon from plugin.xml does not follow theme
		setTitleImage(Activator.getImage(IImageKeys.XYChart_ICON));
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);

		if(!xyChartData.initialize()) {
			Label errorLabel = new Label(container, SWT.BORDER);
			errorLabel.setText(DocometreMessages.SomethingWentWrong);
			errorLabel.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			container.setSashWidth(0);
			container.setWeights(new int[] {100});
			container.setFocus();
			return;
		}

		chart = GLChart2D.createChart(container); // each curve keeps its own duration: all the points are displayed
		chart.setLegendVisible(xyChartData.isLegendVisible()); // colors and grid styles of the data model: not supported by chartxyz
		chart.setTrailFading(false); // static data: curves with a uniform color
		chart.setMarkersInteractive(false); // markers only displayed: no add, remove or select with the mouse
		chart.setMarkersVisible(xyChartData.isShowMarkers());
		chart.setMarkerLabelsVisible(xyChartData.isShowMarkersLabels());
		chart.setMarkerSize(xyChartData.getMarkersSize());
		if(!xyChartData.isAutoScale()) applyRange(); // before the first frame: the initial view is never notified as current

		ChartPartListener.install(this, chart);
		
		// The view listener may be called outside the SWT thread (NEWT): the editor is updated with asyncExec
		Display display = parent.getDisplay();
		chart.addViewListener((view, automatic) -> display.asyncExec(() -> viewChanged(view, automatic)));

		// Allow data to be copied or moved to the drop target
		int operations = DND.DROP_COPY;
		DropTarget target = new DropTarget(chart.getControl(), operations);
		// The JOGL canvas disposes itself again while it is being disposed: the drop target is released before it
		container.addDisposeListener(e -> target.dispose());

		// Receive data in Text or File format
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		target.setTransfer(new Transfer[] { transfer });

		// Add drag/drop support
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_COPY) != 0) event.detail = DND.DROP_COPY;
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					ISelection selection = transfer.getSelection();
					if(selection instanceof IStructuredSelection) {
						Object[] items = ((IStructuredSelection)selection).toArray();
						Channel xSignal = (Channel)items[0];
						Channel ySignal = (Channel)items[1];
						String error = SampleFrequencies.check(xSignal, ySignal);
						if(error != null) {
							MessageDialog.openError(container.getShell(), DocometreMessages.AddCurvesDialog_ShellTitle, error);
							return;
						}
						xyChartData.addCurve(xSignal, ySignal);
						setDirty(true);
						refreshTrialsListFrontEndCutsCategories();
					}
				 }
			}
		});

		Composite container2 = new Composite(container, SWT.BORDER);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 1;
		gl.marginWidth = 1;
		gl.verticalSpacing = 1;
		container2.setLayout(gl);

		trialsCategoriesTabFolder = new CTabFolder(container2, SWT.BOTTOM | SWT.FLAT | SWT.BORDER | SWT.MULTI);
		trialsCategoriesTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		trialsTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
		trialsTabItem.setText(DocometreMessages.TrialsGroupLabel);

		categoriesTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
		categoriesTabItem.setText(DocometreMessages.Categories);

		createTrialsTabItem(trialsCategoriesTabFolder, trialsTabItem, container2);

		createCategoriesTabItem(trialsCategoriesTabFolder, categoriesTabItem, container2);

		trialsCategoriesTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(trialsTabItem == trialsCategoriesTabFolder.getSelection()) categoriesListViewer.removeSelectionChangedListener(categoriesChangeListener);
				else categoriesListViewer.addSelectionChangedListener(categoriesChangeListener);
			}
		});

	}

	private void createCategoriesTabItem(CTabFolder trialsCategoriesTabFolder, CTabItem categoriesTabItem, Composite container2) {
		categoriesListViewer = new ListViewer(trialsCategoriesTabFolder, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		categoriesListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		categoriesListViewer.setContentProvider(new ArrayContentProvider());
		categoriesListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof Channel)) return "??";
				Channel channel = (Channel)element;
				Integer[] trialsList = MathEngineFactory.getMathEngine().getTrialsListForCategory(channel);
				String trialsListString = Arrays.toString(trialsList);
				return element.toString() + " " + trialsListString;
			}
		});
		categoriesListViewer.setComparator(new ViewerComparator());
		categoriesChangeListener  = new CategoriesChangeListener();
		categoriesTabItem.setControl(categoriesListViewer.getList());

		updateCategories();

	}

	private void updateCategories() {
		if(categoriesListViewer == null) return;
		Object[] channelsTuple = xyChartData.getChannels().toArray();
		HashSet<Channel> allCategories = new HashSet<Channel>();
		for (Object channelTuple : channelsTuple) {
			if(!(channelTuple instanceof Channel[])) continue;
			Channel[] channels = (Channel[])channelTuple;
			String fullYChannelName = channels[1].getFullName();
			String fullSubjectName = fullYChannelName.replaceAll("\\.\\w+$", "");
			IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(fullSubjectName.split("\\.")[1]);
			Channel[] categories = MathEngineFactory.getMathEngine().getCategories(subject);
			allCategories.addAll(Arrays.asList(categories));
		}
		categoriesListViewer.setInput(allCategories.toArray());
	}

	private void createTrialsTabItem(CTabFolder trialsCategoriesTabFolder, CTabItem trialsTabItem, Composite container2) {

		trialsListViewer = new ListViewer(trialsCategoriesTabFolder, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				String category = "";
				Set<String> keys = xyChartData.getCurvesIDs();
				for (String key : keys) {
					Channel[] channels = xyChartData.getXYChannels(key);
					Channel refChannel = channels[0];
					if(refChannel instanceof TimeChannel) refChannel = channels[1];
					category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(refChannel, Integer.parseInt(trial));
					break;
				}
				return DocometreMessages.Trial + trial + ("".equals(category)?"":" [" + category + "]");
			}
		});
		trialsListViewer.addSelectionChangedListener(this);
		trialsTabItem.setControl(trialsListViewer.getList());
		trialsCategoriesTabFolder.setSelection(trialsTabItem);

		// Graphical Front End cuts
		Group frontEndCutValuesGroup = new Group(container2, SWT.NONE);
		frontEndCutValuesGroup.setText(DocometreMessages.GraphicalCutsTitle);
		frontEndCutValuesGroup.setLayout(new GridLayout(2, false));
		frontEndCutValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label frontCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		frontCutLabel.setText(DocometreMessages.FrontCutLabel);
		frontCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frontCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
		frontCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		frontCutSpinner.setMaximum(1000000000);
		frontCutSpinner.setSelection(xyChartData.getFrontCut());
		frontCutSpinner.setData("frontCut");
		FrontEndCutsHandler frontCutHandler = new FrontEndCutsHandler(frontCutSpinner, this);
		frontCutSpinner.addMouseWheelListener(frontCutHandler);
		frontCutSpinner.addTraverseListener(frontCutHandler);
		frontCutSpinner.addSelectionListener(frontCutHandler);

		Label endCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		endCutLabel.setText(DocometreMessages.EndCutLabel);
		endCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		endCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
		endCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		endCutSpinner.setMaximum(1000000000);
		endCutSpinner.setSelection(xyChartData.getEndCut());
		endCutSpinner.setData("endCut");
		FrontEndCutsHandler endCutHandler = new FrontEndCutsHandler(endCutSpinner, this);
		endCutSpinner.addMouseWheelListener(endCutHandler);
		endCutSpinner.addTraverseListener(endCutHandler);
		endCutSpinner.addSelectionListener(endCutHandler);

		// Scales
		scaleValuesGroup = new Group(container2, SWT.NONE);
		scaleValuesGroup.setText(DocometreMessages.ScaleValueTitle);
		scaleValuesGroup.setLayout(new GridLayout(4, false));
		scaleValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		scaleValuesGroup.setEnabled(!xyChartData.isAutoScale());

		Label xMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMinLabel.setText("X min. :");
		xMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMinText = new Text(scaleValuesGroup, SWT.BORDER);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMinText.setData("xMin");
		xMinText.addTraverseListener(new RangeHandler(xMinText, this));
		xMinText.setToolTipText(DocometreMessages.PressEnter);
		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMaxLabel.setText("X max. :");
		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMaxText.setData("xMax");
		xMaxText.addTraverseListener(new RangeHandler(xMaxText, this));
		xMaxText.setToolTipText(DocometreMessages.PressEnter);

		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMinLabel.setText("Y min. :");
		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMinText.setData("yMin");
		yMinText.addTraverseListener(new RangeHandler(yMinText, this));
		yMinText.setToolTipText(DocometreMessages.PressEnter);

		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMaxLabel.setText("Y max. :");
		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMaxText.setData("yMax");
		yMaxText.addTraverseListener(new RangeHandler(yMaxText, this));
		yMaxText.setToolTipText(DocometreMessages.PressEnter);

		Composite bottomContainer = new Composite(container2, SWT.NONE);
		bottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout();
		gl.numColumns = 3;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 0;
		gl.marginRight = 0;
		bottomContainer.setLayout(gl);

		Button showMarkersButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
		showMarkersButton.setSelection(xyChartData.isShowMarkers());
		showMarkersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkers = showMarkersButton.getSelection();
				xyChartData.setShowMarkers(showMarkers);
				chart.setMarkersVisible(showMarkers);
				setDirty(true);
			}
		});

		Label sizeLabel = new Label(bottomContainer, SWT.NONE);
		sizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
		sizeLabel.setText(DocometreMessages.MarkersSizeTitle);
		Spinner sizeSpinner = new Spinner(bottomContainer, SWT.BORDER);
		sizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		sizeSpinner.setMaximum(10);
		sizeSpinner.setMinimum(3);
		sizeSpinner.setSelection(xyChartData.getMarkersSize());
		sizeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = sizeSpinner.getSelection() + e.count;
				sizeSpinner.setSelection(value);
				xyChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.setMarkerSize(sizeSpinner.getSelection());
				setDirty(true);
			}
		});
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.setMarkerSize(sizeSpinner.getSelection());
				setDirty(true);
			}
		});

		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
		showMarkersLabelsButton.setSelection(xyChartData.isShowMarkersLabels());
		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
				xyChartData.setShowMarkersLabels(showMarkersLabels);
				chart.setMarkerLabelsVisible(showMarkersLabels);
				setDirty(true);
			}
		});

		Composite bottomContainer2 = new Composite(container2, SWT.NONE);
		bottomContainer2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout();
		gl.numColumns = 1;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 5;
		gl.marginRight = 5;
		bottomContainer2.setLayout(gl);

		autoScaleButton = new Button(bottomContainer2, SWT.CHECK);
		autoScaleButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		autoScaleButton.setText(DocometreMessages.AutoScale_Title);
		autoScaleButton.setSelection(xyChartData.isAutoScale());
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean autoScale = autoScaleButton.getSelection();
				xyChartData.setAutoScale(autoScale);
				scaleValuesGroup.setEnabled(!autoScale);
				if(autoScale) chart.resetZoom(); // initial view: fit to the data (the bounds follow with the view listener)
				else applyRange();
				setDirty(true);
			}
		});

		useSameColorButton = new Button(bottomContainer2, SWT.CHECK);
		useSameColorButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.setSelection(xyChartData.isUseSameColorForSameCategory());
		useSameColorButton.setToolTipText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setUseSameColorForSameCategory(useSameColorButton.getSelection());
				updateSeriesColorsHandler();
				setDirty(true);
			}
		});

		container.setSashWidth(3);
		container.setWeights(new int[] {75, 25});

		refreshTrialsListFrontEndCutsCategories();
		setDirty(false);

	}

	protected void updateSeriesColorsHandler() {
		boolean sameColor = xyChartData.isUseSameColorForSameCategory();
		ArrayList<String> categories = new ArrayList<>();
		List<?> selectedTrials = trialsListViewer.getStructuredSelection().toList();
		for (Map.Entry<String, Series> entry : seriesMap.entrySet()) {
			Series series = entry.getValue();
			int index;
			if(sameColor) {
				String category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(series.ySignal, series.trialNumber);
				if(categories.indexOf(category) == -1) categories.add(category);
				index = categories.indexOf(category);
			} else index = selectedTrials.indexOf(series.trialNumber);
			Color color = DocometreApplication.getColor((byte) index);
			Curve2DData curve = chart.getCurve(entry.getKey());
			if(curve != null) curve.setColor(color.getRed(), color.getGreen(), color.getBlue());
		}
	}

	@Override
	public void refreshTrialsListFrontEndCutsCategories() {
		clearFrequencyChecks(); // curves added or removed
		updateCategories();
		if(xyChartData.getNbCurves() == 0) {
			trialsListViewer.setInput(null);
			return;
		}
		int nbTrials = 0;
		int baseFrontCut = Integer.MAX_VALUE;
		int baseEndCut = 0;
		for (Channel[] xyChannel : xyChartData.getChannels()) {
			Channel xChannel = xyChannel[0];
			Channel yChannel = xyChannel[1];
			if(xChannel instanceof TimeChannel) {
				nbTrials = Math.max(MathEngineFactory.getMathEngine().getTrialsNumber(yChannel), nbTrials);
				baseFrontCut = Math.min(MathEngineFactory.getMathEngine().getFrontCut(yChannel, 1), baseFrontCut);
			} else {
				nbTrials = Math.max(MathEngineFactory.getMathEngine().getTrialsNumber(xChannel), nbTrials);
				baseFrontCut = Math.min(MathEngineFactory.getMathEngine().getFrontCut(xChannel, 1), baseFrontCut);
			}
			baseEndCut = Math.max(MathEngineFactory.getMathEngine().getEndCut(yChannel, 1), baseEndCut);
		}
		Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
		trialsListViewer.setInput(trials);

		frontCutSpinner.setMinimum(baseFrontCut);
		frontCutSpinner.setMaximum(baseEndCut);
		String message = NLS.bind(DocometreMessages.PressEnterFrontCut, baseFrontCut, baseEndCut);
		frontCutSpinner.setToolTipText(message);

		endCutSpinner.setMinimum(baseFrontCut);
		endCutSpinner.setMaximum(baseEndCut);
		message = NLS.bind(DocometreMessages.PressEnterEndCut, baseFrontCut, baseEndCut);
		endCutSpinner.setToolTipText(message);

		if(xyChartData.getFrontCut() == -1 && xyChartData.getEndCut() == -1) {
			xyChartData.setFrontCut(baseFrontCut);
			xyChartData.setEndCut(baseEndCut);
			endCutSpinner.setSelection(baseEndCut);
		}

		trialsListViewer.setSelection(new StructuredSelection(xyChartData.getSelectedTrialsNumbers()));
	}

	@Override
	public String[] getSeriesIDs() {
		List<String> names = chart.getCurvesNames();
		return names.toArray(new String[names.size()]);
	}

	/** The curves are built again with the samples between the new cuts. */
	@Override
	public void updateFrontEndCutsChartHandler() {
		xyChartData.setFrontCut(frontCutSpinner.getSelection());
		xyChartData.setEndCut(endCutSpinner.getSelection());
		Set<Integer> trials = getTrialsInChart();
		removeAllSeries();
		for (Integer trialNumber : trials) addSeriesToChart(trialNumber);
		updateSeriesColorsHandler();
	}

	@Override
	public void setFocus() {
		if(chart != null && !chart.isDisposed()) chart.getControl().setFocus();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if(trialsCategoriesTabFolder.getSelection() == trialsTabItem && categoriesListViewer != null) {
			categoriesListViewer.setSelection(StructuredSelection.EMPTY);
		}
		if(trialsListViewer.getStructuredSelection().isEmpty()) {
			removeAllSeries();
		} else {
			List<Integer> selectedTrialsNumbers = trialsListViewer.getStructuredSelection().toList();
			// Remove series from chart if not in selection
			Set<Integer> trialsNumbersInChart = getTrialsInChart();
			for (Integer trialNumberInChart : trialsNumbersInChart) {
				boolean trialSelected = selectedTrialsNumbers.contains(trialNumberInChart);
				if(!trialSelected) removeSeriesFromChart(trialNumberInChart);
			}

			// Add the missing series (the series already displayed are kept)
			for (Integer selectedTrialNumber : selectedTrialsNumbers) addSeriesToChart(selectedTrialNumber);
			xyChartData.setSelectedTrialsNumbers(trialsListViewer.getStructuredSelection().toList());
		}

		if(!xyChartData.isAutoScale()) applyRange(); // otherwise the initial view follows the data
		updateSeriesColorsHandler();
		setDirty(true);
	}

	/** Applies the bounds of the data model to the chart, as a zoom window. */
	private void applyRange() {
		try {
			chart.setZoomWindow(xyChartData.getxMin(), xyChartData.getxMax(), xyChartData.getyMin(), xyChartData.getyMax());
		} catch (IllegalArgumentException e) {
			// Bounds not usable (minimum not less than maximum): the view does not change
		}
	}

	/**
	 * View displayed by the chart (SWT thread). The initial view (automatic
	 * fit) enables the automatic scale; any other view disables it, as a
	 * zoom with SWTChart. The bounds of the data model and the texts follow
	 * the view.
	 */
	private void viewChanged(double[] view, boolean automatic) {
		if(chart == null || chart.isDisposed() || xMinText.isDisposed()) return;
		if(automatic && chart.isZoomed()) return; // notification older than the zoom window applied since
		boolean sameRange = same(view[0], xyChartData.getxMin(), view) && same(view[1], xyChartData.getxMax(), view)
				&& same(view[2], xyChartData.getyMin(), view) && same(view[3], xyChartData.getyMax(), view);
		if(sameRange && automatic == xyChartData.isAutoScale()) return;
		updateRange(view);
		if(automatic != xyChartData.isAutoScale()) {
			xyChartData.setAutoScale(automatic);
			autoScaleButton.setSelection(automatic);
			scaleValuesGroup.setEnabled(!automatic);
			setDirty(true);
		} else if(!automatic) setDirty(true); // zoom or pan by the user
	}

	/** Same bound, to the precision of the view computation. */
	private static boolean same(double a, double b, double[] view) {
		double span = Math.max(view[1] - view[0], view[3] - view[2]);
		return Math.abs(a - b) <= 1e-9 * Math.max(span, Math.max(Math.abs(a), Math.abs(b)));
	}

	/** Bounds of the data model and texts from the view {xMin, xMax, yMin, yMax}. */
	private void updateRange(double[] view) {
		xyChartData.setxMin(view[0]);
		xyChartData.setxMax(view[1]);
		xyChartData.setyMin(view[2]);
		xyChartData.setyMax(view[3]);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
	}

	private void removeAllSeries() {
		chart.removeAllCurves();
		seriesMap.clear();
	}

	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new java.util.TreeSet<>();
		for (Series series : seriesMap.values()) trials.add(series.trialNumber);
		return trials;
	}

	private void removeSeriesFromChart(Integer trialNumber) {
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			chart.removeCurve(seriesID);
			seriesMap.remove(seriesID);
		}
	}

	/** Adds the series of the trial that are not displayed yet (one per couple of signals). */
	private void addSeriesToChart(Integer trialNumber) {
		for (String key : xyChartData.getCurvesIDs()) {
			String seriesID = key + "." + trialNumber;
			if(chart.getCurve(seriesID) != null) continue; // already displayed
			Channel[] channels = xyChartData.getXYChannels(key);
			if(!sameSampleFrequencies(key, channels)) continue;
			double[][] values = readValues(channels, trialNumber);
			if(values == null) continue;
			double[] xValues = values[0], yValues = values[1];
			// Samples between the cuts: index i of the values = sample baseFrontCut + i
			Channel refChannel = (channels[0] instanceof TimeChannel) ? channels[1] : channels[0];
			int baseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(refChannel, trialNumber);
			int from = Math.max(0, xyChartData.getFrontCut() - baseFrontCut);
			int to = Math.min(Math.min(xValues.length, yValues.length) - 1, xyChartData.getEndCut() - baseFrontCut);
			if(to - from < 1) continue; // fewer than two points
			List<Point2D> points = new ArrayList<>(to - from + 1);
			for (int i = from; i <= to; i++) points.add(new Point2D((float) xValues[i], (float) yValues[i]));
			double sf = MathEngineFactory.getMathEngine().getSampleFrequency(refChannel);
			// Add Series
			Curve2DData curve = new Curve2DData(seriesID, (sf > 0) ? sf : 1, points);
			chart.addCurve(curve);
			Series series = new Series(key, channels[0], channels[1], trialNumber);
			seriesMap.put(seriesID, series);
			updateChartMarkers(curve, series, xValues, yValues);
		}
	}

	/**
	 * Values {x, y} of the signals for the trial (x: time values if x is the
	 * time), or null if one of them has no value (not kept by the values
	 * cache of the MathEngine).
	 */
	private static double[][] readValues(Channel[] channels, int trialNumber) {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		double[] xValues;
		if(channels[0] instanceof TimeChannel) xValues = ((TimeChannel)channels[0]).getValues(trialNumber);
		else xValues = mathEngine.getYValuesForSignal(channels[0], trialNumber);
		double[] yValues = mathEngine.getYValuesForSignal(channels[1], trialNumber);
		if(yValues == null || xValues == null || yValues.length == 0 || xValues.length == 0) return null;
		return new double[][] {xValues, yValues};
	}

	/**
	 * Markers of the signal changed in another editor (SWT thread): the
	 * markers of the curves of this signal are read again (the curves are
	 * kept).
	 */
	@Override
	public void markersChanged(Channel signal) {
		if(chart == null || chart.isDisposed() || signal == null) return;
		for (Map.Entry<String, Series> entry : seriesMap.entrySet()) {
			Series series = entry.getValue();
			if(!signal.equals(series.xSignal) && !signal.equals(series.ySignal)) continue;
			Curve2DData curve = chart.getCurve(entry.getKey());
			double[][] values = readValues(new Channel[] {series.xSignal, series.ySignal}, series.trialNumber);
			if(curve != null && values != null) updateChartMarkers(curve, series, values[0], values[1]);
		}
	}

	/**
	 * True if the signals of the curve have the same sampling frequency;
	 * otherwise (chart saved before the check), the curve is not displayed
	 * and the error is logged once.
	 */
	private boolean sameSampleFrequencies(String key, Channel[] channels) {
		if(!frequencyChecks.containsKey(key)) { // checked once per couple of signals
			String error = SampleFrequencies.check(channels);
			if(error != null) Activator.logErrorMessage(error);
			frequencyChecks.put(key, (error != null) ? error : "");
		}
		return frequencyChecks.get(key).isEmpty();
	}

	/**
	 * Markers of the x signal (if it is not the time) and of the y signal for
	 * the trial of this curve, placed on the points of the curve (one group
	 * per label), as in MarkersManager: markers outside the cuts are skipped.
	 * xValues and yValues: values of the signals for the trial (x: time
	 * values if x is the time).
	 */
	private void updateChartMarkers(Curve2DData curve, Series series, double[] xValues, double[] yValues) {
		for (String label : chart.getMarkersLabels(curve)) chart.removeMarkersGroup(curve, label);
		Map<String, List<double[]>> groups = new LinkedHashMap<>();
		if(!(series.xSignal instanceof TimeChannel)) collectMarkers(series, true, yValues, groups);
		collectMarkers(series, false, xValues, groups);
		for (Map.Entry<String, List<double[]>> group : groups.entrySet()) {
			chart.addMarkersGroup(curve, group.getKey(), group.getValue().toArray(new double[0][]));
		}
	}

	/** Markers of the x (xMarkers) or y signal; values: values of the other signal, for the other coordinate. */
	private void collectMarkers(Series series, boolean xMarkers, double[] values, Map<String, List<double[]>> groups) {
		Channel xSignal = series.xSignal, ySignal = series.ySignal;
		int trialNumber = series.trialNumber;
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency((xSignal instanceof TimeChannel) ? ySignal : xSignal);
		Channel markersSignal = xMarkers ? xSignal : ySignal;
		// Values of the other signal: index i = sample baseFrontCut + i (time values: front cut of y)
		Channel valuesSignal = (xMarkers || xSignal instanceof TimeChannel) ? ySignal : xSignal;
		int baseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(valuesSignal, trialNumber);
		for (Map.Entry<String, double[][]> group : markersGroups(markersSignal).entrySet()) {
			for (double[] marker : group.getValue()) {
				if(((int) marker[0]) != trialNumber) continue;
				int sampleIndex = (int) (sf * marker[1]);
				if(sampleIndex <= xyChartData.getFrontCut() || sampleIndex >= xyChartData.getEndCut()) continue;
				int index = sampleIndex - baseFrontCut;
				if(index < 0 || index >= values.length) continue;
				double[] point = xMarkers ? new double[] {marker[2], values[index]} : new double[] {values[index], marker[2]};
				groups.computeIfAbsent(group.getKey(), k -> new ArrayList<>()).add(point);
			}
		}
	}

	/** Forgets the checks of the sampling frequencies (curves added or removed, data processed). */
	private void clearFrequencyChecks() {
		frequencyChecks.clear();
	}

	/**
	 * Marker groups of the signal: label -> markers {trial, time, value}, in
	 * the order of the labels (read through the values cache of the
	 * MathEngine, see ChannelsContainer).
	 */
	private static Map<String, double[][]> markersGroups(Channel signal) {
		Map<String, double[][]> groups = new LinkedHashMap<>();
		String[] labels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(signal);
		if(labels == null) return groups;
		for (String label : labels) {
			double[][] markers = MathEngineFactory.getMathEngine().getMarkers(label, signal);
			if(markers != null) groups.put(label, markers);
		}
		return groups;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void dispose() {
		ObjectsController.removeHandle(xyChartData);
		super.dispose();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void gotoNextTrial() {
		List<Integer> selectedTrialsNumbers = trialsListViewer.getStructuredSelection().toList();
		int maxTrialNumber = trialsListViewer.getList().getItemCount();
		if(selectedTrialsNumbers.isEmpty()) {
			if(maxTrialNumber >= 1) {
				List<Integer> newSelection = new ArrayList<Integer>();
				newSelection.add(1);
				trialsListViewer.setSelection(new StructuredSelection(newSelection), true);
				trialsListViewer.getList().showSelection();
			}
			return;
		}
		selectedTrialsNumbers = new ArrayList<>(selectedTrialsNumbers);
		int maxSelectedTrialNumber = java.util.Collections.max(selectedTrialsNumbers);
		if(maxSelectedTrialNumber < maxTrialNumber) {
			for (int i = 0; i < selectedTrialsNumbers.size(); i++) {
				int trialNumber = selectedTrialsNumbers.get(i);
				trialNumber++;
				selectedTrialsNumbers.set(i, trialNumber);
			}
			trialsListViewer.setSelection(new StructuredSelection(selectedTrialsNumbers), true);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void gotoPreviousTrial() {
		List<Integer> selectedTrialsNumbers = trialsListViewer.getStructuredSelection().toList();
		int maxTrialNumber = trialsListViewer.getList().getItemCount();
		if(selectedTrialsNumbers.isEmpty()) {
			if(maxTrialNumber >= 1) {
				List<Integer> newSelection = new ArrayList<Integer>();
				newSelection.add(maxTrialNumber);
				trialsListViewer.setSelection(new StructuredSelection(newSelection), true);
				trialsListViewer.getList().showSelection();
			}
			return;
		}
		selectedTrialsNumbers = new ArrayList<>(selectedTrialsNumbers);
		if(java.util.Collections.min(selectedTrialsNumbers) > 1) {
			for (int i = 0; i < selectedTrialsNumbers.size(); i++) {
				int trialNumber = selectedTrialsNumbers.get(i);
				trialNumber--;
				selectedTrialsNumbers.set(i, trialNumber);
			}
			trialsListViewer.setSelection(new StructuredSelection(selectedTrialsNumbers), true);
		}

	}

	@Override
	public XYChart getChartData() {
		return xyChartData;
	}

	/** Nothing to do: the chart is drawn continuously (the data are read again by update()). */
	@Override
	public void redraw() {
	}

	@Override
	public void updateXAxisRange(double min, double max) {
		double[] view = currentView();
		if(view == null) return;
		setView(min, max, view[2], view[3]);
	}

	@Override
	public void updateYAxisRange(double min, double max) {
		double[] view = currentView();
		if(view == null) return;
		setView(view[0], view[1], min, max);
	}

	@Override
	public void updateZAxisRange(double min, double max) {
		// this not a 3D chart, nothing to do.
	}

	/** Current view of the chart {xMin, xMax, yMin, yMax}: the last frame, or the bounds of the data model before it. */
	private double[] currentView() {
		if(chart == null || chart.isDisposed()) return null;
		double[] view = chart.getView();
		if(view == null) view = new double[] {xyChartData.getxMin(), xyChartData.getxMax(), xyChartData.getyMin(), xyChartData.getyMax()};
		return view;
	}

	/** New view as a zoom window: the view listener updates the bounds and the texts. */
	private void setView(double xMin, double xMax, double yMin, double yMax) {
		try {
			chart.setZoomWindow(xMin, xMax, yMin, yMax);
		} catch (IllegalArgumentException e) {
			// Bounds not usable: the view does not change
		}
	}

	@Override
	public void removeSeries(String seriesID) {
		// A whole couple of signals (key) or a single curve (key.trial)
		for (String name : new ArrayList<>(seriesMap.keySet())) {
			if(name.equals(seriesID) || seriesMap.get(name).key.equals(seriesID)) {
				chart.removeCurve(name);
				seriesMap.remove(name);
			}
		}
		xyChartData.removeCurve(seriesID);
	}

	@Override
	public void left() {
		double[] view = currentView();
		if(view == null) return;
		double delta = (view[1] - view[0])/10;
		setView(view[0] + delta, view[1] + delta, view[2], view[3]);
	}

	@Override
	public void right() {
		double[] view = currentView();
		if(view == null) return;
		double delta = (view[1] - view[0])/10;
		setView(view[0] - delta, view[1] - delta, view[2], view[3]);
	}

	@Override
	public void up() {
		double[] view = currentView();
		if(view == null) return;
		double delta = (view[3] - view[2])/10;
		setView(view[0], view[1], view[2] - delta, view[3] - delta);
	}

	@Override
	public void down() {
		double[] view = currentView();
		if(view == null) return;
		double delta = (view[3] - view[2])/10;
		setView(view[0], view[1], view[2] + delta, view[3] + delta);
	}

	@Override
	public void zoomIn() {
		double[] view = currentView();
		if(view == null) return;
		double deltaX = (view[1] - view[0])/20;
		double deltaY = (view[3] - view[2])/20;
		setView(view[0] + deltaX, view[1] - deltaX, view[2] + deltaY, view[3] - deltaY);
	}

	@Override
	public void zoomOut() {
		double[] view = currentView();
		if(view == null) return;
		double deltaX = (view[1] - view[0])/20;
		double deltaY = (view[3] - view[2])/20;
		setView(view[0] - deltaX, view[1] + deltaX, view[2] - deltaY, view[3] + deltaY);
	}

	/** Initial view (fit to the data): the automatic scale is enabled again by the view listener. */
	@Override
	public void zoomToFit() {
		if(chart != null && !chart.isDisposed()) chart.resetZoom();
	}

	@Override
	/**
	 * Data processed (MathEngine values, markers, categories may have
	 * changed): the values are read again and the series built again.
	 */
	public void update() {
		if(chart == null || chart.isDisposed()) return;
		clearFrequencyChecks();
		Set<Integer> trials = getTrialsInChart();
		removeAllSeries();
		for (Integer trialNumber : trials) addSeriesToChart(trialNumber);
		updateSeriesColorsHandler();
		trialsListViewer.refresh(); // categories of the trials
		if(categoriesListViewer != null) categoriesListViewer.refresh();
	}

}
