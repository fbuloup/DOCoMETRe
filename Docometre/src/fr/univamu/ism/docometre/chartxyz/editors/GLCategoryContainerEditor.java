package fr.univamu.ism.docometre.chartxyz.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.AbstractCategoryContainerEditor;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditorWidgetsFactory;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;

/**
 * Category editor with an OpenGL chart (chartxyz).
 *
 * Markers: as in the SWTChart version, the markers of each displayed signal
 * (MathEngine) are only displayed, one group (curve, label) per series: the
 * mouse actions on the markers of the chart are disabled. Marker abscissas: MathEngine time = (sample
 * index + front cut) / sf, chart time = sample index / sf.
 */
public class GLCategoryContainerEditor extends AbstractCategoryContainerEditor {

	private GLChart1D chart;
	private final Map<String, Channel> seriesSignals = new HashMap<>(); // signal of each series

	public GLCategoryContainerEditor(Composite parent, int style, GLChannelEditor channelEditor) {
		super(parent, style, channelEditor::getChannel);
		createContents();
	}

	@Override
	protected boolean createChart(Composite container) {
		Object chartObject = ChannelEditorWidgetsFactory.createChart(container, 1, true, false);
		if(!(chartObject instanceof GLChart1D)) return false;
		chart = (GLChart1D) chartObject;
		chart.setMarkersInteractive(false); // markers only displayed: no add, remove or select with the mouse
		return true;
	}

	@Override
	protected List<String> getSeriesIDs() {
		return chart.getCurvesNames();
	}

	@Override
	protected boolean hasSeries(String seriesID) {
		return chart.getCurve(seriesID) != null;
	}

	@Override
	protected void addSeries(String seriesID, Channel signal, int trialNumber) {
		// Get Y values and sampling frequency for this signal and trial
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(signal, trialNumber);
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(signal);
		if(yValues == null || yValues.length < 2 || sf == 0) return;
		float[] floats = new float[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
		    floats[i] = (float) yValues[i];
		}
		Curve1DData curve = new Curve1DData(seriesID, sf, floats);
		seriesSignals.put(seriesID, signal);
		chart.setWindowTimeWidth(curve.getWindowTimeWidth());
		chart.addCurve(curve);
		updateChartMarkers(curve);
	}

	@Override
	protected void removeSeries(String seriesID) {
		chart.getRenderer().removeCurve(seriesID);
		seriesSignals.remove(seriesID);
	}

	@Override
	protected void removeAllSeries() {
		chart.getRenderer().removeAllCurves();
		seriesSignals.clear();
	}

	@Override
	protected void setSeriesColor(String seriesID, Color color) {
		Curve1DData curve = chart.getCurve(seriesID);
		if(curve != null) curve.setColor(color.getRed(), color.getGreen(), color.getBlue());
	}

	@Override
	protected void setXAxisTitle(String title) {
		chart.setXAxisLabel(title);
	}

	@Override
	protected void resetChartScale() {
		chart.getRenderer().resetView();
	}

	@Override
	public Object getChart() {
		return chart;
	}

	// ------------------------------------------------------------------
	// Markers (display only)

	/** Markers of a series: those of the MathEngine for its signal and trial, all groups (SWT thread). */
	private void updateChartMarkers(Curve1DData curve) {
		Channel signal = seriesSignals.get(curve.getName());
		if(signal == null || chart.getCurve(curve.getName()) != curve) return;
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		int trialNumber = getTrialNumber(curve.getName());
		double sf = mathEngine.getSampleFrequency(signal);
		int frontCut = mathEngine.getFrontCut(signal, trialNumber);
		try {
			chart.clearMarkers(curve);
			for (String label : mathEngine.getMarkersGroupsLabels(signal)) {
				List<double[]> coordinates = new ArrayList<>();
				for (double[] marker : mathEngine.getMarkers(label, signal)) {
					// marker = {trial, MathEngine time, value}; chart time from the sample index
					if((int)marker[0] == trialNumber) coordinates.add(new double[]{(Math.round(marker[1] * sf) - frontCut) / sf, marker[2]});
				}
				if(!coordinates.isEmpty()) chart.addMarkersGroup(curve, label, coordinates.toArray(new double[0][]));
			}
		} catch (IllegalArgumentException e) {
			// curve removed from the chart in the meantime
		}
	}
}
