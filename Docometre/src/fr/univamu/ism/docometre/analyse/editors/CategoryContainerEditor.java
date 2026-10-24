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
import java.util.List;

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

/** Category editor with an SWTChart chart, which displays the markers of the signals (MarkersManager). */
public class CategoryContainerEditor extends AbstractCategoryContainerEditor implements IMarkersManager {

	private InteractiveChart chart;

	public CategoryContainerEditor(Composite parent, int style, ChannelEditor channelEditor) {
		super(parent, style, channelEditor::getChannel);
		createContents();
	}

	@Override
	protected boolean createChart(Composite container) {
		Object chartObject = ChannelEditorWidgetsFactory.createChart(container, 1, true, false);
		if(!(chartObject instanceof InteractiveChart)) return false;
		chart = (InteractiveChart) chartObject;
		new MarkersManager(this);
		return true;
	}

	@Override
	protected List<String> getSeriesIDs() {
		List<String> ids = new ArrayList<>();
		for (ISeries series : chart.getSeriesSet().getSeries()) ids.add(series.getId());
		return ids;
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
		if(yValues == null || xValues == null) return;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
		series.setAntialias(SWT.ON);
		series.setSymbolType(PlotSymbolType.NONE);
		series.setLineWidth(3);
	}

	@Override
	protected void removeSeries(String seriesID) {
		if(chart.getSeriesSet().getSeries(seriesID) != null) chart.getSeriesSet().deleteSeries(seriesID);
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
		if(series != null) series.setLineColor(color);
	}

	@Override
	protected void setXAxisTitle(String title) {
		chart.getAxisSet().getXAxes()[0].getTitle().setText(title);
	}

	@Override
	protected void resetChartScale() {
		chart.getAxisSet().adjustRange();
	}

	@Override
	protected void redrawChart() {
		chart.redraw();
	}

	@Override
	public InteractiveChart getChart() {
		return chart;
	}

	@Override
	public void updateMarkersGroup(String markersGroupLabel, Object... parameters) {
		// The markers are only displayed in a category editor
	}
}
