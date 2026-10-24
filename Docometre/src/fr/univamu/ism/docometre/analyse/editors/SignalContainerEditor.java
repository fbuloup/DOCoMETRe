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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

/** Signal editor with an SWTChart chart, which displays the markers. */
public class SignalContainerEditor extends AbstractSignalContainerEditor implements IMarkersManager {
	
	//private static String[] graphicalSymbols = new String[] {"□", "◇", "△", "○", "☆", "+"};
	
	private InteractiveChart chart;
	private MarkersManager markersManager;

	public SignalContainerEditor(Composite parent, int style, ChannelEditor channelEditor) {
		super(parent, style, channelEditor::getChannel);
		createContents();
	}
	
	public InteractiveChart getChart() {
		return chart;
	}

	@Override
	protected boolean createChart(Composite container) {
		Object chartObject = ChannelEditorWidgetsFactory.createChart(container, 1, !isFrequencyDomain, isFrequencyDomain);
		if(!(chartObject instanceof InteractiveChart)) return false;
		chart = (InteractiveChart) chartObject;
		markersManager = new MarkersManager(this);
		return true;
	}

	@Override
	protected List<String> getSeriesIDs() {
		Set<String> ids = new HashSet<>();
		for (ISeries series : chart.getSeriesSet().getSeries()) ids.add(series.getId());
		return new ArrayList<>(ids);
	}

	@Override
	protected boolean hasSeries(String seriesID) {
		return chart.getSeriesSet().getSeries(seriesID) != null;
	}

	@Override
	protected void addSeries(String seriesID, Channel signal, int trialNumber) {
		// Get x and Y values for this signal and trial
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(signal, trialNumber);
		double[] xValues = MathEngineFactory.getMathEngine().getTimeValuesForSignal(signal, trialNumber);
		if(yValues == null || xValues == null || yValues.length ==0 || xValues.length == 0) return;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
		series.setAntialias(SWT.ON);
		series.setSymbolType(isShowSamplesChecked() ? PlotSymbolType.CIRCLE : PlotSymbolType.NONE);
		series.setLineWidth(3);
	}

	@Override
	protected void removeSeries(String seriesID) {
		chart.removeSeries(seriesID);
	}

	@Override
	protected void removeAllSeries() {
		for (ISeries series : chart.getSeriesSet().getSeries()) {
			chart.getSeriesSet().deleteSeries(series.getId());
		}
	}

	@Override
	protected void setSeriesColor(String seriesID, Color color) {
		ILineSeries series = (ILineSeries) chart.getSeriesSet().getSeries(seriesID);
		if(series == null) return;
		series.setLineColor(color);
		series.setSymbolColor(series.getLineColor());
	}

	@Override
	protected void showSamples(boolean show) {
		for (ISeries series : chart.getSeriesSet().getSeries()) {
			ILineSeries lineSeries = (ILineSeries)series;
			lineSeries.setSymbolType(show ? PlotSymbolType.CIRCLE : PlotSymbolType.NONE);
			lineSeries.setSymbolColor(lineSeries.getLineColor());
		}
		chart.redraw();
	}

	@Override
	protected void resetChartScale() {
		chart.getAxisSet().adjustRange();
	}

	@Override
	protected void redrawChart() {
		chart.redraw();
	}

	// Markers: managed by the MarkersManager, which draws them on the chart

	@Override
	protected String getMarkersGroupLabel() {
		return markersManager.getMarkersGroupLabel();
	}

	@Override
	protected void markersGroupLabelChanged(String markersGroupLabel) {
		markersManager.setMarkersGroupLabel(markersGroupLabel);
	}

	@Override
	protected void markerSelected(double[] marker) {
		markersManager.setSelectedMarker(marker);
		if(marker != null) chart.redraw();
	}
}
