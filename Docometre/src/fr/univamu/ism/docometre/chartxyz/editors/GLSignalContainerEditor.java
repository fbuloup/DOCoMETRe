package fr.univamu.ism.docometre.chartxyz.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.AbstractSignalContainerEditor;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditorWidgetsFactory;
import fr.univamu.ism.docometre.analyse.handlers.UpdateWorkbenchDelegate;
import fr.univamu.ism.docometre.chartxyz.Curve1DData;
import fr.univamu.ism.docometre.chartxyz.DisplayOption;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;

/**
 * Signal editor with an OpenGL chart (chartxyz).
 *
 * Markers: the markers groups of the channel (MathEngine) are displayed with
 * the markers of the chart, one group (curve, label) per displayed trial.
 * In the chart, in cursor mode (C key):
 * <ul>
 * <li>Ctrl + left double click adds a marker of the group selected in the
 *     editor, on the sample designated by the cursor;</li>
 * <li>a double click on a marker label deletes the marker;</li>
 * <li>Ctrl + click on a marker label selects its group and this marker in the
 *     editor (trial number, X and Y values).</li>
 * </ul>
 * The marker selected in the editor is highlighted in the chart.
 * The MathEngine stays the reference: after each change, the markers of the
 * chart are read again from it. Marker abscissas: MathEngine time =
 * (sample index + front cut) / sf, chart time = sample index / sf (time
 * since the first displayed sample).
 */
public class GLSignalContainerEditor extends AbstractSignalContainerEditor {

	/**
	 * Set while the editor changes the markers or the current label of the chart itself: the chart listeners,
	 * called in the thread that makes the change, must ignore these changes.
	 */
	private static final ThreadLocal<Boolean> UPDATING_CHART_MARKERS = ThreadLocal.withInitial(() -> false);

	private GLChart1D chart;
	private String markersGroupLabel = "";

	public GLSignalContainerEditor(Composite parent, int style, GLChannelEditor channelEditor) {
		super(parent, style, channelEditor::getChannel);
		createContents();
	}

	@Override
	protected boolean createChart(Composite container) {
		Object chartObject = ChannelEditorWidgetsFactory.createChart(container, 1, !isFrequencyDomain, isFrequencyDomain);
		if(!(chartObject instanceof GLChart1D)) return false;
		chart = (GLChart1D) chartObject;
		// The chart listeners may be called outside the SWT thread (NEWT): the editor is updated with asyncExec
		Display display = getDisplay();
		// P key in the chart: the check box follows
		chart.addDisplayOptionListener((option, enabled) -> {
			if(DisplayOption.POINTS == option) display.asyncExec(() -> {
				if(!isDisposed()) setShowSamplesChecked(enabled);
			});
		});
		// Ctrl + double click in cursor mode: marker added in the chart
		chart.addMarkerAddedListener((curve, label, x, y) -> {
			if(UPDATING_CHART_MARKERS.get()) return;
			display.asyncExec(() -> {
				if(!isDisposed()) markerAddedInChart(curve, label, x);
			});
		});
		// Double click on a marker label: marker removed from the chart
		chart.addMarkerRemovedListener((curve, label, x, y) -> {
			if(UPDATING_CHART_MARKERS.get()) return;
			if(chart.getCurve(curve.getName()) != curve) return; // curve removed from the chart: its markers are not deleted
			display.asyncExec(() -> {
				if(!isDisposed()) markerRemovedInChart(curve, label, x, y);
			});
		});
		// Ctrl + click on a marker label: the marker is selected in the editor (after its group, see below)
		chart.addMarkerSelectedListener((curve, label, x, y) -> display.asyncExec(() -> {
			if(!isDisposed()) markerSelectedInChart(curve, label, x, y);
		}));
		// Ctrl + click on a marker label: its group becomes the selected group
		chart.addCurrentMarkerLabelListener(label -> {
			if(UPDATING_CHART_MARKERS.get()) return;
			display.asyncExec(() -> {
				if(!isDisposed() && !label.equals(markersGroupLabel)) selectMarkersGroup(label);
			});
		});
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
		if(yValues == null || yValues.length ==0 || sf == 0) return;
		float[] floats = new float[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
		    floats[i] = (float) yValues[i];
		}
		Curve1DData curve = new Curve1DData(seriesID, sf, floats);
		chart.setWindowTimeWidth(curve.getWindowTimeWidth());
		chart.addCurve(curve);
		chart.getRenderer().setPointsVisible(isShowSamplesChecked());
		updateChartMarkers(curve);
	}

	@Override
	protected void removeSeries(String seriesID) {
		chart.getRenderer().removeCurve(seriesID);
	}

	@Override
	protected void removeAllSeries() {
		chart.getRenderer().removeAllCurves();
	}

	@Override
	protected void setSeriesColor(String seriesID, Color color) {
		Curve1DData curve = chart.getCurve(seriesID);
		if(curve != null) curve.setColor(color.getRed(), color.getGreen(), color.getBlue());
	}

	@Override
	protected void showSamples(boolean show) {
		chart.getRenderer().setPointsVisible(show);
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
	// Markers

	@Override
	protected String getMarkersGroupLabel() {
		return markersGroupLabel;
	}

	/** The group selected in the editor is the group of the markers added in the chart. */
	@Override
	protected void markersGroupLabelChanged(String markersGroupLabel) {
		this.markersGroupLabel = (markersGroupLabel == null) ? "" : markersGroupLabel;
		chart.clearSelectedMarker(); // the selected marker belongs to the previous group
		UPDATING_CHART_MARKERS.set(true);
		try {
			chart.setCurrentMarkerLabel(this.markersGroupLabel); // empty: "Default", which is not a group of the channel
		} finally {
			UPDATING_CHART_MARKERS.set(false);
		}
	}

	/** Marker {trial, MathEngine time, value} selected in the editor: highlighted in the chart (null: none). */
	@Override
	protected void markerSelected(double[] marker) {
		Curve1DData curve = (marker != null) ? chart.getCurve(getSeriesID((int)marker[0])) : null;
		if(curve == null) chart.clearSelectedMarker(); // no marker, or trial not displayed
		else chart.setSelectedMarker(curve, markersGroupLabel, chartTime((int)marker[0], marker[1]), marker[2]);
	}

	@Override
	protected void markersChanged() {
		updateAllChartMarkers();
	}

	/** Marker added in the chart (SWT thread): saved in the MathEngine if a group is selected, otherwise removed. */
	private void markerAddedInChart(Curve1DData curve, String label, double x) {
		Channel signal = getChannel();
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		int trialNumber = getTrialNumber(curve.getName());
		boolean isGroup = Arrays.asList(mathEngine.getMarkersGroupsLabels(signal)).contains(label);
		double[] yValues = mathEngine.getYValuesForSignal(signal, trialNumber);
		double sf = mathEngine.getSampleFrequency(signal);
		int index = (int) Math.round(x * sf);
		if(isGroup && yValues != null && index >= 0 && index < yValues.length) {
			double xValue = mathEngineTime(index + mathEngine.getFrontCut(signal, trialNumber), sf);
			mathEngine.addMarker(label, trialNumber, xValue, yValues[index], signal);
			mathEngine.setUpdateChannelsCache(signal.getSubject(), true);
			updateMarkersGroup(markersGroupLabel, trialNumber, xValue, yValues[index]);
			UpdateWorkbenchDelegate.updateMarkers(signal); // XY and XYZ charts of this signal
		} else if(!isGroup) {
			Activator.logWarningMessage("Select or create a markers group before adding a marker.");
		}
		updateChartMarkers(curve); // the chart shows exactly the markers of the MathEngine
	}

	/** Marker removed from the chart (SWT thread): deleted from the MathEngine. */
	private void markerRemovedInChart(Curve1DData curve, String label, double x, double y) {
		Channel signal = getChannel();
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		int trialNumber = getTrialNumber(curve.getName());
		double sf = mathEngine.getSampleFrequency(signal);
		double xValue = mathEngineTime((int) Math.round(x * sf) + mathEngine.getFrontCut(signal, trialNumber), sf);
		mathEngine.deleteMarker(label, trialNumber, xValue, y, signal);
		mathEngine.setUpdateChannelsCache(signal.getSubject(), true);
		updateMarkersGroup(markersGroupLabel);
		if(chart.getCurve(curve.getName()) == curve) updateChartMarkers(curve);
		UpdateWorkbenchDelegate.updateMarkers(signal); // XY and XYZ charts of this signal
	}

	/** Chart time of a MathEngine time: time since the first displayed sample, from the sample index. */
	private double chartTime(int trialNumber, double mathEngineTime) {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		double sf = mathEngine.getSampleFrequency(getChannel());
		return (Math.round(mathEngineTime * sf) - mathEngine.getFrontCut(getChannel(), trialNumber)) / sf;
	}

	/**
	 * Marker selected in the chart (SWT thread, after the selection of its group): the same marker of the
	 * MathEngine (same trial, sample and value) is selected in the editor, which shows its values.
	 */
	private void markerSelectedInChart(Curve1DData curve, String label, double x, double y) {
		if(!label.equals(markersGroupLabel)) return; // group unknown to the editor
		Channel signal = getChannel();
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		int trialNumber = getTrialNumber(curve.getName());
		double sf = mathEngine.getSampleFrequency(signal);
		long sample = Math.round(x * sf) + mathEngine.getFrontCut(signal, trialNumber);
		for (double[] marker : mathEngine.getMarkers(label, signal)) {
			// marker = {trial, MathEngine time, value}
			if((int)marker[0] == trialNumber && Math.round(marker[1] * sf) == sample && Math.abs(marker[2] - y) <= 1e-9 * Math.max(1, Math.abs(y))) {
				updateMarkersGroup(label, (int)marker[0], marker[1], marker[2]); // selects it: values shown, marker highlighted
				return;
			}
		}
	}

	/** Time of a sample in the MathEngine, computed as in MathEngine.getTimeValuesForSignal(). */
	private static double mathEngineTime(int sampleIndex, double sf) {
		return 1f*sampleIndex/sf;
	}

	private void updateAllChartMarkers() {
		for (Curve1DData curve : chart.getCurves()) updateChartMarkers(curve);
	}

	/** Markers of the curve of a trial: those of the MathEngine for this trial, all groups (SWT thread). */
	private void updateChartMarkers(Curve1DData curve) {
		if(chart.getCurve(curve.getName()) != curve) return;
		Channel signal = getChannel();
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		int trialNumber = getTrialNumber(curve.getName());
		UPDATING_CHART_MARKERS.set(true);
		try {
			chart.clearMarkers(curve);
			for (String label : mathEngine.getMarkersGroupsLabels(signal)) {
				List<double[]> coordinates = new ArrayList<>();
				for (double[] marker : mathEngine.getMarkers(label, signal)) {
					// marker = {trial, MathEngine time, value}; chart time from the sample index
					if((int)marker[0] == trialNumber) coordinates.add(new double[]{chartTime(trialNumber, marker[1]), marker[2]});
				}
				if(!coordinates.isEmpty()) chart.addMarkersGroup(curve, label, coordinates.toArray(new double[0][]));
			}
		} catch (IllegalArgumentException e) {
			// curve removed from the chart in the meantime
		} finally {
			UPDATING_CHART_MARKERS.set(false);
		}
	}
}
