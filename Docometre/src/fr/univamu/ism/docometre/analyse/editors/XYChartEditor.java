package fr.univamu.ism.docometre.analyse.editors;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class XYChartEditor extends EditorPart implements ISelectionChangedListener {
	
	public static String ID = "Docometre.XYChartEditor";
	private XYChart xyChartData;
	private SashForm container;
	private Channel xChannel;
	private Channel yChannel;
	private ListViewer trialsListViewer;
	private InteractiveChart chart;
	private double xMax, xMin, yMax, yMin;
	private boolean autoScale;
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Spinner frontCutSpinner;
	private Spinner endCutSpinner;

	public XYChartEditor() {
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

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		xyChartData = ((XYChart)((ResourceEditorInput)input).getObject());
		IResource resource = ObjectsController.getResourceForObject(xyChartData);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
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

	@Override
	public void createPartControl(Composite parent) {
		Composite innerContainer = new Composite(parent, SWT.NONE);
		innerContainer.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		FillLayout fl = new FillLayout();
		fl.marginHeight = 5;
		innerContainer.setLayout(fl);
		container = new SashForm(innerContainer, SWT.HORIZONTAL);
		
		chart = new InteractiveChart(container, SWT.NONE);
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
				xMin = chart.getAxisSet().getXAxes()[0].getRange().lower;
				xMax = chart.getAxisSet().getXAxes()[0].getRange().upper;
				yMin = chart.getAxisSet().getYAxes()[0].getRange().lower;
				yMax = chart.getAxisSet().getYAxes()[0].getRange().upper;
				xMinText.setText(Double.toString(xMin));
				xMaxText.setText(Double.toString(xMax));
				yMinText.setText(Double.toString(yMin));
				yMaxText.setText(Double.toString(yMax));
			}
		});
		
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
						xChannel = (Channel)items[0];
						yChannel = (Channel)items[1];
						int nbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(xChannel);
						Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
						trialsListViewer.setInput(trials);
						int fc = MathEngineFactory.getMathEngine().getFrontCut(xChannel, 0);
						int ec = MathEngineFactory.getMathEngine().getEndCut(xChannel, 0);
						fc = Math.min(fc, MathEngineFactory.getMathEngine().getFrontCut(yChannel, 0));
						ec = Math.max(ec, MathEngineFactory.getMathEngine().getEndCut(yChannel, 0));
						frontCutSpinner.setSelection(fc);
						endCutSpinner.setSelection(ec);
						frontCutSpinner.setMinimum(fc);
						endCutSpinner.setMaximum(ec);
						frontCutSpinner.setMaximum(ec);
						endCutSpinner.setMinimum(fc);
					}
				 }
			}
		});
		
		Composite container2 = new Composite(container, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container2.setLayout(gl);
		container2.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		trialsListViewer = new ListViewer(container2, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
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
		frontCutSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = frontCutSpinner.getSelection() + e.count;
				if(value < endCutSpinner.getSelection()) {
					frontCutSpinner.setSelection(value);
					updateFrontEndCutsChartHandler();
				}
			}
		});
		frontCutSpinner.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.detail == SWT.TRAVERSE_RETURN) {
					if(frontCutSpinner.getSelection() < endCutSpinner.getSelection()) updateFrontEndCutsChartHandler();
					else frontCutSpinner.setSelection(endCutSpinner.getSelection() - 1);
				}
			}
		});
		Label endCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		endCutLabel.setText(DocometreMessages.EndCutLabel);
		endCutLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		endCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
		endCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		endCutSpinner.setMaximum(1000000000);
		endCutSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = endCutSpinner.getSelection() + e.count;
				if(value > frontCutSpinner.getSelection()) {
					endCutSpinner.setSelection(value);
					updateFrontEndCutsChartHandler();
				}
			}
		});
		endCutSpinner.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.detail == SWT.TRAVERSE_RETURN) {
					if(frontCutSpinner.getSelection() < endCutSpinner.getSelection()) updateFrontEndCutsChartHandler();
					else endCutSpinner.setSelection(frontCutSpinner.getSelection() + 1);
				}
			}
		});
		
		// Scales
		Group scaleValuesGroup = new Group(container2, SWT.NONE);
		scaleValuesGroup.setText(DocometreMessages.ScaleValueTitle);
		scaleValuesGroup.setLayout(new GridLayout(4, false));
		scaleValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		scaleValuesGroup.setEnabled(false);
		
		Label xMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMinLabel.setText("X min. :");
		xMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMinText = new Text(scaleValuesGroup, SWT.BORDER);
		xMinText.setText("-10");
		xMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMaxLabel.setText("X max. :");
		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		xMaxText.setText("10");
		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMinLabel.setText("Y min. :");
		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
		yMinText.setText("-10");
		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMaxLabel.setText("Y max. :");
		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		yMaxText.setText("10");
		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite bottomContainer = new Composite(container2, SWT.NONE);
		bottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout();
		gl.numColumns = 2;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 5;
		gl.marginRight = 5;
		bottomContainer.setLayout(gl);
		
		Button autoScaleButton = new Button(bottomContainer, SWT.CHECK);
		autoScaleButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		autoScaleButton.setText(DocometreMessages.AutoScale_Title);
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				autoScale = autoScaleButton.getSelection();
				scaleValuesGroup.setEnabled(!autoScale); 
				if(autoScale) {
					chart.getAxisSet().adjustRange();
					updateRange();
					chart.redraw();
				}
			}
		});
		autoScaleButton.setSelection(true);
		autoScale = true;
		
		Button applyButton = new Button(bottomContainer, SWT.FLAT);
		applyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		applyButton.setText(DocometreMessages.ApplyTitle);
		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xMin = Double.parseDouble(xMinText.getText());
				xMax = Double.parseDouble(xMaxText.getText());
				yMin = Double.parseDouble(yMinText.getText());
				yMax = Double.parseDouble(yMaxText.getText());
				chart.getAxisSet().getXAxes()[0].setRange(new Range(xMin, xMax));
				chart.getAxisSet().getYAxes()[0].setRange(new Range(yMin, yMax));
				chart.redraw(); 
			}
		});
		
		container.setSashWidth(5);
		container.setWeights(new int[] {80, 20});

	}
	
	private void updateFrontEndCutsChartHandler() {
		int value = frontCutSpinner.getSelection();
		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setFrontCut(value);
		value = endCutSpinner.getSelection();
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
		}
		
		if(autoScale) {
			chart.getAxisSet().adjustRange();
			updateRange();
		}
		else {
			chart.getAxisSet().getXAxes()[0].setRange(new Range(xMin, xMax));
			chart.getAxisSet().getYAxes()[0].setRange(new Range(yMin, yMax));
		}
		chart.redraw();
		
	}
	
	private void updateRange() {
		xMin = chart.getAxisSet().getXAxes()[0].getRange().lower;
		xMax = chart.getAxisSet().getXAxes()[0].getRange().upper;
		yMin = chart.getAxisSet().getYAxes()[0].getRange().lower;
		yMax = chart.getAxisSet().getYAxes()[0].getRange().upper;
		xMinText.setText(Double.toString(xMin));
		xMaxText.setText(Double.toString(xMax));
		yMinText.setText(Double.toString(yMin));
		yMaxText.setText(Double.toString(yMax));
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
		String seriesID = getSeriesIDPrefix() + "." + trialNumber;
		chart.removeSeries(seriesID);
	}
	
	private void addSeriesToChart(Integer trialNumber) {
		// Get x and Y values for this signal and trial
		int frontCut = frontCutSpinner.getSelection();
		int endCut = endCutSpinner.getSelection();
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(yChannel, trialNumber);
		double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(xChannel, trialNumber);
		// 
		if(yValues == null || xValues == null || yValues.length ==0 || xValues.length == 0) return;
		// Add Series
		String seriesID = getSeriesIDPrefix() + "." + trialNumber;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
		series.setFrontCut(frontCut);
		series.setEndCut(endCut);
		series.setAntialias(SWT.ON);
		series.setSymbolType(PlotSymbolType.NONE);
		series.setLineColor(ColorUtil.getColor());
		series.setLineWidth(3);
	}
	
	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		String seriesID = getSeriesIDPrefix() + "." + trialNumber;
		return chart.getSeriesSet().getSeries(seriesID) != null;
	}
	
	
	private String getSeriesIDPrefix() {
		return yChannel.getFullName() + "(" + xChannel.getFullName() + ")";
	}
	
}
