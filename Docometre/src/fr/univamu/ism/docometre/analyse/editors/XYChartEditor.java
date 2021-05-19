package fr.univamu.ism.docometre.analyse.editors;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;
import org.eclipse.swtchart.extensions.charts.ZoomListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class XYChartEditor extends EditorPart implements ISelectionChangedListener, IMarkersManager, ZoomListener {
	
	public static String ID = "Docometre.XYChartEditor";
	private XYChart xyChartData;
	private SashForm container;
	private ListViewer trialsListViewer;
	private InteractiveChart chart;
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Spinner frontCutSpinner;
	private Spinner endCutSpinner;
	private boolean dirty;
	private Button autoScaleButton;
	private Group scaleValuesGroup;

	public XYChartEditor() {
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
		xyChartData.initialize();
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
		Composite innerContainer = new Composite(parent, SWT.NONE);
		innerContainer.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		FillLayout fl = new FillLayout();
		fl.marginHeight = 5;
		innerContainer.setLayout(fl);
		container = new SashForm(innerContainer, SWT.HORIZONTAL);
		
		chart = new InteractiveChart(container, SWT.NONE);
		new MarkersManager(this);
		chart.setShowCursor(false);
		chart.setShowMarker(false);
		chart.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		chart.getPlotArea().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.getLegend().setPosition(SWT.BOTTOM);
		chart.getLegend().setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		chart.getLegend().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		chart.setSelectionRectangleColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		chart.getTitle().setVisible(false);
		IAxis[] axes = chart.getAxisSet().getAxes();
		for (IAxis axe : axes) {
			axe.getTick().setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		}
		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
		chart.getAxisSet().getXAxes()[0].getTitle().setVisible(false);
		chart.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				postZoomUpdate();
			}
		});
		chart.addZoomListener(this);
		
		// Allow data to be copied or moved to the drop target
		int operations = DND.DROP_COPY;
		DropTarget target = new DropTarget(chart, operations);

		// Receive data in Text or File format
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		target.setTransfer(new Transfer[] { transfer });

		// Add drag/drop support
		target.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_COPY) != 0) event.detail = DND.DROP_COPY;
			}

			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			public void dragOperationChanged(DropTargetEvent event) {

			}

			public void dragLeave(DropTargetEvent event) {
			}

			public void dropAccept(DropTargetEvent event) {
			}

			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					ISelection selection = transfer.getSelection();
					if(selection instanceof IStructuredSelection) {
						Object[] items = ((IStructuredSelection)selection).toArray();
						Channel xSignal = (Channel)items[0];
						Channel ySignal = (Channel)items[1];
						xyChartData.addCurve(xSignal, ySignal);
						refreshTrialsListFrontEndCuts();
						setDirty(true);
					}
				 }
			}
		});
		
		Composite container2 = new Composite(container, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 1;
		container2.setLayout(gl);
		container2.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite addRemoveButtonsContainer = new Composite(container2, SWT.NONE);
		addRemoveButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		addRemoveButtonsContainer.setLayout(new GridLayout(2, true));
		GridLayout gl2 = (GridLayout) addRemoveButtonsContainer.getLayout();
		gl2.marginHeight = 1;
		gl2.marginWidth = 1;
		Button addButton = new Button(addRemoveButtonsContainer, SWT.FLAT);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setToolTipText(DocometreMessages.AddNewCurveToolTip);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!MathEngineFactory.getMathEngine().isStarted()) return;
				String[] loadedSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
				Set<Channel> signals = new HashSet<>();
				for (String loadedSubject : loadedSubjects) {
					IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(loadedSubject.split("\\.")[1]);
					signals.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getSignals(subject)));
				}
				ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getSite().getShell(), new LabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Channel)element).getFullName();
					}
				});
				elementListSelectionDialog.setMultipleSelection(false);
				elementListSelectionDialog.setElements(signals.toArray(new Channel[signals.size()]));
				elementListSelectionDialog.setTitle(DocometreMessages.XAxisSelectionDialogTitle);
				elementListSelectionDialog.setMessage(DocometreMessages.XAxisSelectionDialogMessage);
				if(elementListSelectionDialog.open() == Dialog.OK) {
					Object[] selection = elementListSelectionDialog.getResult();
					Channel xSignal = (Channel) selection[0];
					elementListSelectionDialog.setTitle(DocometreMessages.YAxisSelectionDialogTitle);
					elementListSelectionDialog.setMessage(DocometreMessages.YAxisSelectionDialogMessage);
					if(elementListSelectionDialog.open() == Dialog.OK) {
						selection = elementListSelectionDialog.getResult();
						Channel ySignal = (Channel) selection[0];
						xyChartData.addCurve(xSignal, ySignal);
						refreshTrialsListFrontEndCuts();
						setDirty(true);
					}
				}
			}
		});
		Button removeButton = new Button(addRemoveButtonsContainer, SWT.FLAT);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		removeButton.setImage(Activator.getImage(IImageKeys.REMOVE_ICON));
		removeButton.setToolTipText(DocometreMessages.RemoveCurveToolTip);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!MathEngineFactory.getMathEngine().isStarted()) return;
				ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getSite().getShell(), new LabelProvider());
				elementListSelectionDialog.setTitle(DocometreMessages.CurvesSelectionDialogTitle);
				elementListSelectionDialog.setMessage(DocometreMessages.CurvesSelectionDialogMessage);
				elementListSelectionDialog.setMultipleSelection(true);
				elementListSelectionDialog.setElements(xyChartData.getSeriesIDsPrefixes());
				if(elementListSelectionDialog.open() == Dialog.OK) {
					String[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new String[elementListSelectionDialog.getResult().length]);
					String[] seriesIDs = getSeriesIDs();
					for (String seriesID : seriesIDs) {
						for (String item : selection) {
							if(seriesID.startsWith(item)) {
								chart.removeSeries(seriesID);
								xyChartData.removeCurve(item);
							}
						}
					}
					chart.redraw();
					refreshTrialsListFrontEndCuts();
					setDirty(true);
				}
			}
		});
		
		trialsListViewer = new ListViewer(container2, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				return DocometreMessages.Trial + trial;
			}
		});
		trialsListViewer.addSelectionChangedListener(this);
		
		// Graphical Front End cuts
		Group frontEndCutValuesGroup = new Group(container2, SWT.NONE);
		frontEndCutValuesGroup.setText(DocometreMessages.GraphicalCutsTitle);
		frontEndCutValuesGroup.setLayout(new GridLayout(2, false));
		frontEndCutValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label frontCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		frontCutLabel.setText(DocometreMessages.FrontCutLabel);
		frontCutLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
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
		endCutLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
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
		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMaxLabel.setText("X max. :");
		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMaxText.setData("xMax");
		xMaxText.addTraverseListener(new RangeHandler(xMaxText, this));

		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMinLabel.setText("Y min. :");
		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMinText.setData("yMin");
		yMinText.addTraverseListener(new RangeHandler(yMinText, this));
		
		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMaxLabel.setText("Y max. :");
		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMaxText.setData("yMax");
		yMaxText.addTraverseListener(new RangeHandler(yMaxText, this));
		
		Composite bottomContainer = new Composite(container2, SWT.NONE);
		bottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout();
		gl.numColumns = 3;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 5;
		gl.marginRight = 5;
		bottomContainer.setLayout(gl);
		
		Button showMarkersButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
		showMarkersButton.setSelection(xyChartData.isShowMarkers());
		showMarkersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkers = showMarkersButton.getSelection();
				xyChartData.setShowMarkers(showMarkers);
				chart.redraw();
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
				chart.redraw();
				setDirty(true);
			}
		});
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.redraw();
				setDirty(true);
			}
		});
		
		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
		showMarkersLabelsButton.setSelection(xyChartData.isShowMarkersLabels());
		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
				xyChartData.setShowMarkersLabels(showMarkersLabels);
				chart.redraw();
				setDirty(true);
			}
		});
		
		Composite bottomContainer2 = new Composite(container2, SWT.NONE);
		bottomContainer2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout();
		gl.numColumns = 2;
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
				if(autoScale) {
					chart.getAxisSet().adjustRange();
					updateRange();
					chart.redraw();
				}
				setDirty(true);
			}
		});
		
		Button applyButton = new Button(bottomContainer2, SWT.FLAT);
		applyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		applyButton.setText(DocometreMessages.ApplyTitle);
		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setxMin(Double.parseDouble(xMinText.getText()));
				xyChartData.setxMax(Double.parseDouble(xMaxText.getText()));
				xyChartData.setyMin(Double.parseDouble(yMinText.getText()));
				xyChartData.setyMax(Double.parseDouble(yMaxText.getText()));
				chart.getAxisSet().getXAxes()[0].setRange(new Range(xyChartData.getxMin(), xyChartData.getxMax()));
				chart.getAxisSet().getYAxes()[0].setRange(new Range(xyChartData.getyMin(), xyChartData.getyMax()));
				chart.redraw(); 
				setDirty(true);
			}
		});
		
		container.setSashWidth(5);
		container.setWeights(new int[] {80, 20});
		
		refreshTrialsListFrontEndCuts();
		trialsListViewer.setSelection(new StructuredSelection(xyChartData.getSelectedTrialsNumbers()));
		setDirty(false);

	}
	
	private void refreshTrialsListFrontEndCuts() {
		if(xyChartData.getNbCurves() == 0) {
			trialsListViewer.setInput(null);
			return;
		}
		Collection<Channel[]> xyChannels = xyChartData.getXYChannels();
		int nbTrials = 0;
		int fc = frontCutSpinner.getSelection();
		int ec = endCutSpinner.getSelection();
		int baseFrontCut = fc;
		int baseEndCut = ec;
		for (Channel[] xyChannel : xyChannels) {
			Channel xChannel = xyChannel[0];
			Channel yChannel = xyChannel[1];
			nbTrials = Math.max(MathEngineFactory.getMathEngine().getTrialsNumber(xChannel), nbTrials);
			baseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(xChannel, 0);
			baseEndCut = MathEngineFactory.getMathEngine().getEndCut(yChannel, 0);
//			fc = Math.max(fc, MathEngineFactory.getMathEngine().getFrontCut(xChannel, 0));
//			ec = Math.min(ec, MathEngineFactory.getMathEngine().getEndCut(xChannel, 0));
//			fc = Math.max(fc, MathEngineFactory.getMathEngine().getFrontCut(yChannel, 0));
//			ec = Math.min(ec, MathEngineFactory.getMathEngine().getEndCut(yChannel, 0));
		}
		Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
		trialsListViewer.setInput(trials);
		frontCutSpinner.setSelection(fc);
		endCutSpinner.setSelection(ec);
		frontCutSpinner.setMinimum(baseFrontCut);
		frontCutSpinner.setMaximum(baseEndCut);
		endCutSpinner.setMinimum(baseFrontCut);
		endCutSpinner.setMaximum(baseEndCut);
	}
	
	private String[] getSeriesIDs() {
		ISeries[] series = chart.getSeriesSet().getSeries();
		Set<String> ids = new HashSet<>();
		for (ISeries iSeries : series) {
			String id = iSeries.getId();
//			id = id.replaceAll("\\.\\d+$", "");
			ids.add(id);
		}
		return ids.toArray(new String[ids.size()]);
	}
	
	protected void updateFrontEndCutsChartHandler() {
		int value = frontCutSpinner.getSelection();
		xyChartData.setFrontCut(value);
		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setFrontCut(value);
		value = endCutSpinner.getSelection();
		xyChartData.setEndCut(value);
		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setEndCut(value);
		chart.redraw();
	}

	@Override
	public void setFocus() {
		container.setFocus();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
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
			
			// Add series
			for (Integer selectedTrialNumber : selectedTrialsNumbers) {
				if(!chartHasAlreadyThisTrial(selectedTrialNumber)) {
					addSeriesToChart(selectedTrialNumber);
				}
			}
			xyChartData.setSelectedTrialsNumbers(trialsListViewer.getStructuredSelection().toList());
		}
		
		if(xyChartData.isAutoScale()) {
			chart.getAxisSet().adjustRange();
			updateRange();
		}
		else {
			chart.getAxisSet().getXAxes()[0].setRange(new Range(xyChartData.getxMin(), xyChartData.getxMax()));
			chart.getAxisSet().getYAxes()[0].setRange(new Range(xyChartData.getyMin(), xyChartData.getyMax()));
		}
		chart.redraw();
		setDirty(true);
	}
	
	private void updateRange() {
		xyChartData.setxMin(chart.getAxisSet().getXAxes()[0].getRange().lower);
		xyChartData.setxMax(chart.getAxisSet().getXAxes()[0].getRange().upper);
		xyChartData.setyMin(chart.getAxisSet().getYAxes()[0].getRange().lower);
		xyChartData.setyMax(chart.getAxisSet().getYAxes()[0].getRange().upper);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
	}
	
	private void removeAllSeries() {
		ISeries[] series = chart.getSeriesSet().getSeries();
		for (ISeries aSeries : series) {
			chart.getSeriesSet().deleteSeries(aSeries.getId());
		}
	}
	
	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new HashSet<Integer>();
		ISeries[] series = chart.getSeriesSet().getSeries();
		for (ISeries aSeries : series) {
			String[] segments = aSeries.getId().split("\\.");
			trials.add(Integer.parseInt(segments[segments.length - 1]));
		}
		return trials;
	}
	
	private void removeSeriesFromChart(Integer trialNumber) {
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			chart.removeSeries(seriesID);
		}
	}
	
	private void addSeriesToChart(Integer trialNumber) {
		// Get x and Y values for this signal and trial
		Set<String> keys = xyChartData.getCurvesIDs();
		for (String key : keys) {
			Channel[] channels = xyChartData.getXYChannels(key);
			double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[0], trialNumber);
			double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[1], trialNumber);
			// 
			if(yValues == null || xValues == null || yValues.length == 0 || xValues.length == 0) return;
			// Add Series
			String seriesID = key + "." + trialNumber;
			ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
			series.setXSeries(xValues);
			series.setYSeries(yValues);
			int frontCut = xyChartData.getFrontCut();
			int endCut = xyChartData.getEndCut();
			series.setFrontCut(frontCut);
			series.setEndCut(endCut);
			series.setAntialias(SWT.ON);
			series.setSymbolType(PlotSymbolType.NONE);
			Byte index = getSeriesIndex(series);
			series.setLineColor(ColorUtil.getColor(index));
			series.setLineWidth(3);
		}
	}
	
	private Byte getSeriesIndex(ILineSeries series) {
		ISeries[] seriesArray = chart.getSeriesSet().getSeries();
		for (int i = 0; i < seriesArray.length; i++) {
			if(series == seriesArray[i]) return (byte) i;
		}
		return 0;
	}
	
	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			if(chart.getSeriesSet().getSeries(seriesID) != null) return true;
		} 
		return false;
	}
	
	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}
	
	@Override
	public void dispose() {
		ObjectsController.removeHandle(xyChartData);
		super.dispose();
	}

	@Override
	public InteractiveChart getChart() {
		return chart;
	}

	@Override
	public void updateMarkersGroup(String markersGroupLabel) {
		// TODO Nothing
	}

	public XYChart getXYChartData() {
		return xyChartData;
	}

	@Override
	public void postZoomUpdate() {
		xyChartData.setxMin(chart.getAxisSet().getXAxes()[0].getRange().lower);
		xyChartData.setxMax(chart.getAxisSet().getXAxes()[0].getRange().upper);
		xyChartData.setyMin(chart.getAxisSet().getYAxes()[0].getRange().lower);
		xyChartData.setyMax(chart.getAxisSet().getYAxes()[0].getRange().upper);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
		xyChartData.setAutoScale(false);
		autoScaleButton.setSelection(false);
		scaleValuesGroup.setEnabled(true);
		setDirty(true);
	}
	
	
	
}
