package fr.univamu.ism.docometre.chartxyz.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.datamodel.XYZChart;
import fr.univamu.ism.docometre.analyse.editors.Chart2D3DBehaviour;
import fr.univamu.ism.docometre.analyse.editors.FrontEndCutsHandler;
import fr.univamu.ism.docometre.analyse.editors.MarkersChangeListener;
import fr.univamu.ism.docometre.analyse.editors.RangeHandler;
import fr.univamu.ism.docometre.analyse.editors.TrialsEditor;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.GLChart3D;
import fr.univamu.ism.docometre.chartxyz.Point3D;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

/**
 * XYZ chart editor with an OpenGL chart (chartxyz): OpenGL version of
 * XYZChartEditor.
 *
 * Curves: one curve per triplet of signals (x, y, z) and per selected trial,
 * named "key.trial"; the front and end cuts are applied by building the
 * curves from the samples between the cuts.
 *
 * Scales: the automatic scale fits the box to the data; otherwise the
 * bounds of the data model are applied as fixed scales (what leaves the box
 * is clipped).
 *
 * Point of view: the camera state is saved in the data model
 * (XYZChart.getCameraState()) and restored when the editor is opened; any
 * change of the camera (mouse, keyboard, toolbar) makes the editor dirty.
 *
 * Markers: the markers of the x, y and z signals are only displayed, as
 * circles on the points of the curves (one group (curve, label) per series).
 * They follow the changes made in the other editors (see
 * {@link #markersChanged(Channel)}).
 */
public class GLXYZChartEditor extends EditorPart implements ISelectionChangedListener, Chart2D3DBehaviour, TrialsEditor, MarkersChangeListener {

	public static String ID = "Docometre.GLXYZChartEditor";

	/** Rotation of turnLeft() / turnRight(), in degrees. */
	private static final float TURN_STEP = 5f;
	/** Change of the camera distance factor of zoomIn() / zoomOut(). */
	private static final float ZOOM_STEP = 0.3f;

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
		final Channel[] signals; // x, y, z
		final int trialNumber;
		Series(String key, Channel[] signals, int trialNumber) {
			this.key = key;
			this.signals = signals;
			this.trialNumber = trialNumber;
		}
	}

	private XYZChart xyzChartData;
	private SashForm container;
	private ListViewer trialsListViewer;
	private GLChart3D chart;
	private final Map<String, Series> seriesMap = new LinkedHashMap<>(); // series of the chart by curve name, in creation order
	private final Map<String, String> frequencyChecks = new HashMap<>(); // key -> error of sampling frequencies ("" if none), logged once
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Text zMinText;
	private Text zMaxText;
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

	public GLXYZChartEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IResource xyzChartFile = ObjectsController.getResourceForObject(xyzChartData);
		ObjectsController.serialize((IFile) xyzChartFile, xyzChartData);
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
		xyzChartData = ((XYZChart)((ResourceEditorInput)input).getObject());
		IResource resource = ObjectsController.getResourceForObject(xyzChartData);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		// Editor icon from plugin.xml does not follow theme
		setTitleImage(Activator.getImage(IImageKeys.XYZChart_ICON));
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

		if(!xyzChartData.initialize()) {
			Label errorLabel = new Label(container, SWT.BORDER);
			errorLabel.setText(DocometreMessages.SomethingWentWrong);
			errorLabel.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			container.setSashWidth(0);
			container.setWeights(new int[] {100});
			container.setFocus();
			return;
		}

		chart = GLChart3D.createChart(container); // each curve keeps its own duration: all the points are displayed
		chart.setLegendVisible(xyzChartData.isLegendVisible()); // colors and grid styles of the data model: not supported by chartxyz
		chart.setTrailFading(false); // static data: curves with a uniform color
		chart.setMarkersVisible(xyzChartData.isShowMarkers());
		chart.setMarkerLabelsVisible(xyzChartData.isShowMarkersLabels());
		chart.setMarkerSize(xyzChartData.getMarkersSize());
		float[] cameraState = xyzChartData.getCameraState();
		if(cameraState != null) {
			try {
				chart.setCameraState(cameraState); // before the first frame: not notified
			} catch (IllegalArgumentException e) {
				// State not valid (saved by another version): default point of view
			}
		}
		// The camera listener may be called outside the SWT thread (NEWT): the editor is updated with asyncExec
		Display display = parent.getDisplay();
		chart.addCameraListener(state -> display.asyncExec(() -> cameraChanged(state)));
		
		ChartPartListener.install(this, chart);
		
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
		Object[] channelsTuple = xyzChartData.getChannels().toArray();
		HashSet<Channel> allCategories = new HashSet<Channel>();
		for (Object channelTuple : channelsTuple) {
			if(!(channelTuple instanceof Channel[])) continue;
			Channel[] channels = (Channel[])channelTuple;
			String fullXChannelName = channels[0].getFullName();
			String fullSubjectName = fullXChannelName.replaceAll("\\.\\w+$", "");
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
				Set<String> keys = xyzChartData.getCurvesIDs();
				for (String key : keys) {
					Channel[] channels = xyzChartData.getXYZChannels(key);
					category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(channels[0], Integer.parseInt(trial));
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
		frontCutSpinner.setSelection(xyzChartData.getFrontCut());
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
		endCutSpinner.setSelection(xyzChartData.getEndCut());
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
		scaleValuesGroup.setEnabled(!xyzChartData.isAutoScale());

		xMinText = createRangeText("X min. :", "xMin", xyzChartData.getxMin());
		xMaxText = createRangeText("X max. :", "xMax", xyzChartData.getxMax());
		yMinText = createRangeText("Y min. :", "yMin", xyzChartData.getyMin());
		yMaxText = createRangeText("Y max. :", "yMax", xyzChartData.getyMax());
		zMinText = createRangeText("Z min. :", "zMin", xyzChartData.getzMin());
		zMaxText = createRangeText("Z max. :", "zMax", xyzChartData.getzMax());

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
		showMarkersButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
		showMarkersButton.setSelection(xyzChartData.isShowMarkers());
		showMarkersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkers = showMarkersButton.getSelection();
				xyzChartData.setShowMarkers(showMarkers);
				chart.setMarkersVisible(showMarkers);
				setDirty(true);
			}
		});

		Label sizeLabel = new Label(bottomContainer, SWT.NONE);
		sizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
		sizeLabel.setText(DocometreMessages.MarkersSizeTitle);
		Spinner sizeSpinner = new Spinner(bottomContainer, SWT.BORDER);
		sizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		sizeSpinner.setMaximum(50); // radius in pixels: limited to 50 by chartxyz
		sizeSpinner.setMinimum(3);
		sizeSpinner.setSelection(xyzChartData.getMarkersSize());
		sizeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = sizeSpinner.getSelection() + e.count;
				sizeSpinner.setSelection(value);
				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.setMarkerSize(sizeSpinner.getSelection());
				setDirty(true);
			}
		});
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.setMarkerSize(sizeSpinner.getSelection());
				setDirty(true);
			}
		});

		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
		showMarkersLabelsButton.setSelection(xyzChartData.isShowMarkersLabels());
		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
				xyzChartData.setShowMarkersLabels(showMarkersLabels);
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
		autoScaleButton.setSelection(xyzChartData.isAutoScale());
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean autoScale = autoScaleButton.getSelection();
				xyzChartData.setAutoScale(autoScale);
				scaleValuesGroup.setEnabled(!autoScale);
				applyScales();
				setDirty(true);
			}
		});

		useSameColorButton = new Button(bottomContainer2, SWT.CHECK);
		useSameColorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.setSelection(xyzChartData.isUseSameColorForSameCategory());
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyzChartData.setUseSameColorForSameCategory(useSameColorButton.getSelection());
				updateSeriesColorsHandler();
				setDirty(true);
			}
		});

		container.setSashWidth(3);
		container.setWeights(new int[] {80, 20});

		refreshTrialsListFrontEndCutsCategories();
		setDirty(false);
	}

	/** Label and text of a bound of the scales (RangeHandler: Enter applies the value). */
	private Text createRangeText(String title, String key, double value) {
		Label label = new Label(scaleValuesGroup, SWT.NONE);
		label.setText(title);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		Text text = new Text(scaleValuesGroup, SWT.BORDER);
		text.setText(Double.toString(value));
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setData(key);
		text.addTraverseListener(new RangeHandler(text, this));
		text.setToolTipText(DocometreMessages.PressEnter);
		return text;
	}

	protected void updateSeriesColorsHandler() {
		boolean sameColor = xyzChartData.isUseSameColorForSameCategory();
		ArrayList<String> categories = new ArrayList<>();
		List<?> selectedTrials = trialsListViewer.getStructuredSelection().toList();
		for (Map.Entry<String, Series> entry : seriesMap.entrySet()) {
			Series series = entry.getValue();
			int index;
			if(sameColor) {
				// Category of the z signal, as in XYZChartEditor
				String category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(series.signals[2], series.trialNumber);
				if(categories.indexOf(category) == -1) categories.add(category);
				index = categories.indexOf(category);
			} else index = selectedTrials.indexOf(series.trialNumber);
			Color color = DocometreApplication.getColor((byte) index);
			Curve3DData curve = chart.getCurve(entry.getKey());
			if(curve != null) curve.setColor(color.getRed(), color.getGreen(), color.getBlue());
		}
	}

	@Override
	public void refreshTrialsListFrontEndCutsCategories() {
		clearFrequencyChecks(); // curves added or removed
		updateCategories();
		if(xyzChartData.getNbCurves() == 0) {
			trialsListViewer.setInput(null);
			return;
		}
		int nbTrials = 0;
		int baseFrontCut = Integer.MAX_VALUE;
		int baseEndCut = 0;
		for (Channel[] xyzChannel : xyzChartData.getChannels()) {
			Channel xChannel = xyzChannel[0];
			Channel yChannel = xyzChannel[1];
			nbTrials = Math.max(MathEngineFactory.getMathEngine().getTrialsNumber(xChannel), nbTrials);
			baseFrontCut = Math.min(MathEngineFactory.getMathEngine().getFrontCut(xChannel, 1), baseFrontCut);
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

		if(xyzChartData.getFrontCut() == -1 && xyzChartData.getEndCut() == -1) {
			xyzChartData.setFrontCut(baseFrontCut);
			xyzChartData.setEndCut(baseEndCut);
			endCutSpinner.setSelection(baseEndCut);
		}

		trialsListViewer.setSelection(new StructuredSelection(xyzChartData.getSelectedTrialsNumbers()));
	}

	@Override
	public String[] getSeriesIDs() {
		List<String> names = chart.getCurvesNames();
		return names.toArray(new String[names.size()]);
	}

	/** The curves are built again with the samples between the new cuts. */
	@Override
	public void updateFrontEndCutsChartHandler() {
		xyzChartData.setFrontCut(frontCutSpinner.getSelection());
		xyzChartData.setEndCut(endCutSpinner.getSelection());
		Set<Integer> trials = getTrialsInChart();
		removeAllSeries();
		for (Integer trialNumber : trials) addSeriesToChart(trialNumber);
		updateSeriesColorsHandler();
		applyScales();
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
			xyzChartData.setSelectedTrialsNumbers(trialsListViewer.getStructuredSelection().toList());
		}

		applyScales();
		updateSeriesColorsHandler();
		setDirty(true);
	}

	/**
	 * Scales of the chart: fitted to the data (automatic scale, the bounds of
	 * the data model and the texts follow the data), or the bounds of the
	 * data model (fixed scales).
	 */
	private void applyScales() {
		if(xyzChartData.isAutoScale()) {
			chart.getRenderer().setDynamicScales();
			double[] bounds = dataBounds();
			if(bounds != null) updateRange(bounds);
		} else {
			try {
				chart.setFixedScales(xyzChartData.getxMin(), xyzChartData.getxMax(), xyzChartData.getyMin(), xyzChartData.getyMax(),
						xyzChartData.getzMin(), xyzChartData.getzMax());
			} catch (IllegalArgumentException e) {
				// Bounds not usable (minimum not less than maximum): the scales do not change
			}
		}
	}

	/** Bounds {xMin, xMax, yMin, yMax, zMin, zMax} of the points of the curves of the chart, or null if there is none. */
	private double[] dataBounds() {
		double[] b = null;
		for (Curve3DData curve : chart.getCurves()) {
			for (Point3D p : curve.getPoints()) {
				if(b == null) b = new double[] {p.x, p.x, p.y, p.y, p.z, p.z};
				b[0] = Math.min(b[0], p.x); b[1] = Math.max(b[1], p.x);
				b[2] = Math.min(b[2], p.y); b[3] = Math.max(b[3], p.y);
				b[4] = Math.min(b[4], p.z); b[5] = Math.max(b[5], p.z);
			}
		}
		return b;
	}

	/** Bounds of the data model and texts from {xMin, xMax, yMin, yMax, zMin, zMax}. */
	private void updateRange(double[] bounds) {
		xyzChartData.setxMin(bounds[0]);
		xyzChartData.setxMax(bounds[1]);
		xyzChartData.setyMin(bounds[2]);
		xyzChartData.setyMax(bounds[3]);
		xyzChartData.setzMin(bounds[4]);
		xyzChartData.setzMax(bounds[5]);
		xMinText.setText(Double.toString(xyzChartData.getxMin()));
		xMaxText.setText(Double.toString(xyzChartData.getxMax()));
		yMinText.setText(Double.toString(xyzChartData.getyMin()));
		yMaxText.setText(Double.toString(xyzChartData.getyMax()));
		zMinText.setText(Double.toString(xyzChartData.getzMin()));
		zMaxText.setText(Double.toString(xyzChartData.getzMax()));
	}

	/** Camera changed (SWT thread): saved in the data model, the editor becomes dirty. */
	private void cameraChanged(float[] state) {
		if(chart == null || chart.isDisposed()) return;
		if(Arrays.equals(state, xyzChartData.getCameraState())) return;
		xyzChartData.setCameraState(state);
		setDirty(true);
	}

	private void removeAllSeries() {
		chart.removeAllCurves();
		seriesMap.clear();
	}

	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new TreeSet<>();
		for (Series series : seriesMap.values()) trials.add(series.trialNumber);
		return trials;
	}

	private void removeSeriesFromChart(Integer trialNumber) {
		for (String seriesID : xyzChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			chart.removeCurve(seriesID);
			seriesMap.remove(seriesID);
		}
	}

	/** Adds the series of the trial that are not displayed yet (one per triplet of signals). */
	private void addSeriesToChart(Integer trialNumber) {
		for (String key : xyzChartData.getCurvesIDs()) {
			String seriesID = key + "." + trialNumber;
			if(chart.getCurve(seriesID) != null) continue; // already displayed
			Channel[] channels = xyzChartData.getXYZChannels(key);
			if(!sameSampleFrequencies(key, channels)) continue;
			double[][] values = readValues(channels, trialNumber);
			if(values == null) continue;
			int[] baseFrontCuts = baseFrontCuts(channels, trialNumber);
			// Samples between the cuts: index i of the values of a signal = sample baseFrontCut + i
			List<Point3D> points = new ArrayList<>();
			for (int sample = xyzChartData.getFrontCut(); sample <= xyzChartData.getEndCut(); sample++) {
				double[] xyz = valuesAt(values, baseFrontCuts, sample);
				if(xyz != null) points.add(new Point3D((float) xyz[0], (float) xyz[1], (float) xyz[2]));
			}
			if(points.size() < 2) continue;
			double sf = MathEngineFactory.getMathEngine().getSampleFrequency(channels[0]);
			// Add Series
			Curve3DData curve = new Curve3DData(seriesID, (sf > 0) ? sf : 1, points);
			chart.addCurve(curve);
			Series series = new Series(key, channels, trialNumber);
			seriesMap.put(seriesID, series);
			updateChartMarkers(curve, series, values, baseFrontCuts);
		}
	}

	/**
	 * True if the three signals of the curve have the same sampling
	 * frequency; otherwise (chart saved before the check), the curve is not
	 * displayed and the error is logged once.
	 */
	private boolean sameSampleFrequencies(String key, Channel[] channels) {
		if(!frequencyChecks.containsKey(key)) { // checked once per couple of signals
			String error = SampleFrequencies.check(channels);
			if(error != null) Activator.logErrorMessage(error);
			frequencyChecks.put(key, (error != null) ? error : "");
		}
		return frequencyChecks.get(key).isEmpty();
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

	/**
	 * Values of the three signals for the trial, or null if one of them has
	 * no value (not kept by the values cache of the MathEngine).
	 */
	private static double[][] readValues(Channel[] channels, int trialNumber) {
		double[][] values = new double[3][];
		for (int c = 0; c < 3; c++) {
			values[c] = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[c], trialNumber);
			if(values[c] == null || values[c].length == 0) return null;
		}
		return values;
	}

	/** Front cuts of the three signals for the trial: index i of the values of a signal = sample frontCut + i. */
	private int[] baseFrontCuts(Channel[] channels, int trialNumber) {
		int[] baseFrontCuts = new int[3];
		for (int c = 0; c < 3; c++) baseFrontCuts[c] = MathEngineFactory.getMathEngine().getFrontCut(channels[c], trialNumber);
		return baseFrontCuts;
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
			if(!Arrays.asList(series.signals).contains(signal)) continue;
			Curve3DData curve = chart.getCurve(entry.getKey());
			double[][] values = readValues(series.signals, series.trialNumber);
			if(curve != null && values != null) updateChartMarkers(curve, series, values, baseFrontCuts(series.signals, series.trialNumber));
		}
	}

	/** Values {x, y, z} of the three signals at a sample, or null if one of them has no value there. */
	private static double[] valuesAt(double[][] values, int[] baseFrontCuts, int sample) {
		double[] xyz = new double[3];
		for (int c = 0; c < 3; c++) {
			int index = sample - baseFrontCuts[c];
			if(index < 0 || index >= values[c].length) return null;
			xyz[c] = values[c][index];
		}
		return xyz;
	}

	/**
	 * Markers of the x, y and z signals for the trial of this curve, placed
	 * on the points of the curve (one group per label): markers outside the
	 * cuts are skipped.
	 */
	private void updateChartMarkers(Curve3DData curve, Series series, double[][] values, int[] baseFrontCuts) {
		for (String label : chart.getMarkersLabels(curve)) chart.removeMarkersGroup(curve, label);
		Map<String, List<double[]>> groups = new LinkedHashMap<>();
		for (Channel signal : series.signals) {
			double sf = MathEngineFactory.getMathEngine().getSampleFrequency(signal);
			for (Map.Entry<String, double[][]> markersGroup : markersGroups(signal).entrySet()) {
				String label = markersGroup.getKey();
				for (double[] marker : markersGroup.getValue()) {
					if(((int) marker[0]) != series.trialNumber) continue;
					int sample = (int) (sf * marker[1]);
					if(sample <= xyzChartData.getFrontCut() || sample >= xyzChartData.getEndCut()) continue;
					double[] xyz = valuesAt(values, baseFrontCuts, sample);
					if(xyz != null) groups.computeIfAbsent(label, k -> new ArrayList<>()).add(xyz);
				}
			}
		}
		for (Map.Entry<String, List<double[]>> group : groups.entrySet()) {
			chart.addMarkersGroup(curve, group.getKey(), group.getValue().toArray(new double[0][]));
		}
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void dispose() {
		ObjectsController.removeHandle(xyzChartData);
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
		return xyzChartData;
	}

	/** Nothing to do: the chart is drawn continuously (the data are read again by update()). */
	@Override
	public void redraw() {
	}

	@Override
	public void updateXAxisRange(double min, double max) {
		applyScales(); // RangeHandler has already updated the data model
	}

	@Override
	public void updateYAxisRange(double min, double max) {
		applyScales();
	}

	@Override
	public void updateZAxisRange(double min, double max) {
		applyScales();
	}

	@Override
	public void removeSeries(String seriesID) {
		// A whole triplet of signals (key) or a single curve (key.trial)
		for (String name : new ArrayList<>(seriesMap.keySet())) {
			if(name.equals(seriesID) || seriesMap.get(name).key.equals(seriesID)) {
				chart.removeCurve(name);
				seriesMap.remove(name);
			}
		}
		xyzChartData.removeCurve(seriesID);
	}

	// Navigation: as in GLXYChartEditor, left() / right() / up() / down() move the content
	// (the view moves the other way); the camera listener saves the new point of view.

	@Override
	public void left() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().panStep(1, 0);
	}

	@Override
	public void right() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().panStep(-1, 0);
	}

	@Override
	public void up() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().panStep(0, -1);
	}

	@Override
	public void down() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().panStep(0, 1);
	}

	/** Rotation of the scene to the left around the vertical axis. */
	public void turnLeft() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().rotate(-TURN_STEP, 0);
	}

	/** Rotation of the scene to the right around the vertical axis. */
	public void turnRight() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().rotate(TURN_STEP, 0);
	}

	@Override
	public void zoomIn() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().zoom(-ZOOM_STEP);
	}

	@Override
	public void zoomOut() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().zoom(ZOOM_STEP);
	}

	/** Initial point of view (R key). */
	@Override
	public void zoomToFit() {
		if(chart != null && !chart.isDisposed()) chart.getRenderer().resetView();
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
		applyScales();
		trialsListViewer.refresh(); // categories of the trials
		if(categoriesListViewer != null) categoriesListViewer.refresh();
	}

}
