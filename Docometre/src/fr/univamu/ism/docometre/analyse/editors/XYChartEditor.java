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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
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
					}
				 }
			}
		});
		
		
		trialsListViewer = new ListViewer(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				return DocometreMessages.Trial + trial;
			}
		});
		
		trialsListViewer.addSelectionChangedListener(this);
		
		
		container.setSashWidth(5);
		container.setWeights(new int[] {90, 10});

	}

	protected void createTrace(Channel xChannel, Channel yChannel) {
		
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
		
		chart.getAxisSet().adjustRange();
		chart.redraw();
		
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
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(yChannel, trialNumber);
		double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(xChannel, trialNumber);
		// 
		if(yValues == null || xValues == null || yValues.length ==0 || xValues.length == 0) return;
		// Add Series
		String seriesID = getSeriesIDPrefix() + "." + trialNumber;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
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
