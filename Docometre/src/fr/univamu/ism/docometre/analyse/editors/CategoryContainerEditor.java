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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class CategoryContainerEditor extends Composite implements ISelectionChangedListener, TrialNavigator, IMarkersManager {


	private InteractiveChart chart;
	private ListViewer trialsListViewer;
	private ChannelEditor channelEditor;
	private ListViewer signalsListViewer;

	public CategoryContainerEditor(Composite parent, int style, ChannelEditor channelEditor) {
		super(parent, style);
		this.channelEditor = channelEditor;
		
		setLayout(new GridLayout(3, false));
		GridLayout gl = (GridLayout)getLayout();
		gl.horizontalSpacing = 5;
		gl.verticalSpacing = 0;
		gl.marginHeight = 5;
		gl.marginWidth = 5;
		
		chart = ChannelEditorWidgetsFactory.createChart(this, 1);
		ChannelEditorWidgetsFactory.createSeparator(this, false, true, 1, SWT.VERTICAL);
		
		new MarkersManager(this);
		
		Composite categoryContainer = new Composite(this, SWT.NORMAL);
		categoryContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		categoryContainer.setLayout(new GridLayout(2, false));
		GridLayout gl2 = (GridLayout)categoryContainer.getLayout();
		gl2.horizontalSpacing = 5;
		gl2.verticalSpacing = 0;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		
		ChannelEditorWidgetsFactory.createLabel(categoryContainer, DocometreMessages.CategoryCriteriaLabel, SWT.LEFT, false);
		String criteria = MathEngineFactory.getMathEngine().getCriteriaForCategory(channelEditor.getChannel());
		ChannelEditorWidgetsFactory.createLabel(categoryContainer, criteria, SWT.LEFT, true);
		
		SashForm sashForm = new SashForm(categoryContainer, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		trialsListViewer = new ListViewer(sashForm, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		Integer[] trials = MathEngineFactory.getMathEngine().getTrialsListForCategory(channelEditor.getChannel());
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				return DocometreMessages.Trial + trial;
			}
		});
		trialsListViewer.setInput(trials);
		trialsListViewer.addSelectionChangedListener(this);
		
		signalsListViewer = new ListViewer(sashForm, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		signalsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		signalsListViewer.setContentProvider(new ArrayContentProvider());
		signalsListViewer.setLabelProvider(new LabelProvider());
		Channel[] signals = MathEngineFactory.getMathEngine().getSignals(channelEditor.getChannel().getParent());
		signalsListViewer.setInput(signals);
		signalsListViewer.addSelectionChangedListener(this);
		
		sashForm.setWeights(new int[] {70,30});
		
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// Retrieve selected trials and signals
		if(trialsListViewer.getStructuredSelection().isEmpty()) {
			removeAllSeries();
			return;
		}
		if(signalsListViewer.getStructuredSelection().isEmpty()) {
			removeAllSeries();
			return;
		}
		List<Integer> selectedTrialsNumbers = trialsListViewer.getStructuredSelection().toList();
		List<Channel> selectedSignals = signalsListViewer.getStructuredSelection().toList();
		// Remove series from chart if not in selection
		Set<Integer> trialsNumbersInChart = getTrialsInChart();
		Set<Channel> signalsInChart = getSignalsInChart();
		for (Integer trialNumberInChart : trialsNumbersInChart) {
			for (Channel signalInChart : signalsInChart) {
				boolean trialSelected = selectedTrialsNumbers.contains(trialNumberInChart);
				boolean signalSelected = selectedSignals.contains(signalInChart);
				if(!trialSelected || !signalSelected) removeSeriesFromChart(signalInChart, trialNumberInChart);
			}
		}
		// Add series
		for (Integer selectedTrialNumber : selectedTrialsNumbers) {
			for (Channel selectedSignal : selectedSignals) {
				if(!chartHasAlreadyThisTrial(selectedSignal, selectedTrialNumber)) {
					addSeriesToChart(selectedSignal, selectedTrialNumber);
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

	private Set<Channel> getSignalsInChart() {
		Set<Channel> signals = new HashSet<Channel>();
		ISeries[] series = chart.getSeriesSet().getSeries();
		for (ISeries aSeries : series) {
			String[] segments = aSeries.getId().split("\\.");
			String channelName = segments[segments.length - 2];
			Channel channel = MathEngineFactory.getMathEngine().getChannelWithName(channelEditor.getChannel().getParent(), channelName);
			signals.add(channel);
		}
		return signals;
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
	
	private void removeSeriesFromChart(Channel signalInChart, Integer trialNumber) {
		String seriesID = signalInChart.getFullName() + "." + trialNumber;
		if(chart.getSeriesSet().getSeries(seriesID) != null) {
			chart.getSeriesSet().deleteSeries(seriesID);
		}
	}
	
	private void addSeriesToChart(Channel signal, Integer trialNumber) {
		// Get x and Y values for this signal and trial
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(signal, trialNumber);
		double[] xValues = MathEngineFactory.getMathEngine().getTimeValuesForSignal(signal, trialNumber);
		// Add Series
		String seriesID = signal.getFullName() + "." + trialNumber;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
		series.setAntialias(SWT.ON);
		series.setSymbolType(PlotSymbolType.NONE);
		Byte index = getSeriesIndex(series);
		series.setLineColor(ColorUtil.getColor(index));
		series.setLineWidth(3);
	}
	
	private Byte getSeriesIndex(ILineSeries series) {
		ISeries[] seriesArray = chart.getSeriesSet().getSeries();
		for (int i = 0; i < seriesArray.length; i++) {
			if(series == seriesArray[i]) return (byte) i;
		}
		return 0;
	}
	
	private boolean chartHasAlreadyThisTrial(Channel signalInChart, Integer trialNumber) {
		String seriesID = signalInChart.getFullName() + "." + trialNumber;
		return chart.getSeriesSet().getSeries(seriesID) != null;
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
			}
			return;
		}
		int maxSelectedTrialNumber = Collections.max(selectedTrialsNumbers);
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
		if(Collections.min(selectedTrialsNumbers) > 1) {
			for (int i = 0; i < selectedTrialsNumbers.size(); i++) {
				int trialNumber = selectedTrialsNumbers.get(i);
				trialNumber--;
				selectedTrialsNumbers.set(i, trialNumber);
			}
			trialsListViewer.setSelection(new StructuredSelection(selectedTrialsNumbers), true);
		}
	}

	@Override
	public InteractiveChart getChart() {
		return chart;
	}

	@Override
	public void updateMarkersGroup(String markersGroupLabel) {
		// TODO Nothing
	}

}
