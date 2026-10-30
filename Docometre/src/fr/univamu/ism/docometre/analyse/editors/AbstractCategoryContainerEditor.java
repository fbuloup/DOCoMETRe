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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * Common part of the category editors: chart of the selected signals for the
 * selected trials of the category, with the list of the trials of the
 * category and the list of the signals of the subject. Subclasses provide the
 * chart (SWTChart, JOGL...) through the abstract methods; a series of the
 * chart is identified by "signal full name.trial number".
 *
 * Subclasses must call {@link #createContents()} at the end of their
 * constructor, once their own fields are initialized.
 */
public abstract class AbstractCategoryContainerEditor extends Composite implements ISelectionChangedListener, TrialNavigator {

	private final Supplier<Channel> categorySupplier;
	protected ListViewer trialsListViewer;
	protected ListViewer signalsListViewer;
	private boolean sameColor;

	protected AbstractCategoryContainerEditor(Composite parent, int style, Supplier<Channel> categorySupplier) {
		super(parent, style);
		this.categorySupplier = categorySupplier;
	}

	/** Displayed category. */
	protected Channel getCategory() {
		return categorySupplier.get();
	}

	// ------------------------------------------------------------------
	// Chart: provided by the subclasses

	/** Creates the chart in container (first column of the editor). @return false if the chart could not be created */
	protected abstract boolean createChart(Composite container);

	/** Identifiers of the series currently in the chart, in the order they were added. */
	protected abstract List<String> getSeriesIDs();

	protected abstract boolean hasSeries(String seriesID);

	/** Adds the series of a signal for a trial (nothing if there are no values). */
	protected abstract void addSeries(String seriesID, Channel signal, int trialNumber);

	protected abstract void removeSeries(String seriesID);

	protected abstract void removeAllSeries();

	protected abstract void setSeriesColor(String seriesID, Color color);

	/** Title of the abscissa axis (time, frequency or both). */
	protected abstract void setXAxisTitle(String title);

	/** Fits the scales to the series. */
	protected abstract void resetChartScale();

	/** Redraws the chart if it does not do it by itself. */
	protected void redrawChart() {
	}

	public abstract Object getChart();

	// ------------------------------------------------------------------
	// Contents

	/** Builds the editor; to be called at the end of the constructor of the subclasses. */
	protected final void createContents() {
		setLayout(new GridLayout(1, false));
		GridLayout gl = (GridLayout)getLayout();
		gl.horizontalSpacing = 5;
		gl.verticalSpacing = 0;
		gl.marginHeight = 5;
		gl.marginWidth = 5;
		
		// Chart pane (chart and show/hide button) | category container, resizable
		SashForm mainSashForm = new SashForm(this, SWT.HORIZONTAL);
		mainSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ChannelEditorWidgetsFactory.configureSashForm(mainSashForm);
		Composite chartPane = ChannelEditorWidgetsFactory.createChartPane(mainSashForm);
		
		if(!createChart(chartPane)) {
			Activator.logErrorMessage("Error while trying to create chart");
			return;
		}
		Composite categoryContainer = new Composite(mainSashForm, SWT.NORMAL);
		categoryContainer.setLayout(new GridLayout(2, false));
		GridLayout gl2 = (GridLayout)categoryContainer.getLayout();
		gl2.horizontalSpacing = 5;
		gl2.verticalSpacing = 0;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		
		Button sameColorButton = new Button(categoryContainer, SWT.CHECK);
		sameColorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		sameColorButton.setText(DocometreMessages.SameColorForSameTrialLabel);
		sameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sameColor = sameColorButton.getSelection();
				updateSeriesColors();
				redrawChart();
			}
		});
		
		ChannelEditorWidgetsFactory.createLabel(categoryContainer, DocometreMessages.CategoryCriteriaLabel, SWT.LEFT, false);
		String criteria = MathEngineFactory.getMathEngine().getCriteriaForCategory(getCategory());
		ChannelEditorWidgetsFactory.createLabel(categoryContainer, criteria, SWT.LEFT, true);
		
		SashForm sashForm = new SashForm(categoryContainer, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		trialsListViewer = new ListViewer(sashForm, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		Integer[] trials = MathEngineFactory.getMathEngine().getTrialsListForCategory(getCategory());
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return DocometreMessages.Trial + super.getText(element);
			}
		});
		trialsListViewer.setInput(trials);
		trialsListViewer.addSelectionChangedListener(this);
		
		signalsListViewer = new ListViewer(sashForm, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		signalsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		signalsListViewer.setContentProvider(new ArrayContentProvider());
		signalsListViewer.setLabelProvider(new LabelProvider());
		Channel[] signals = MathEngineFactory.getMathEngine().getSignals(getCategory().getParent());
		signalsListViewer.setInput(signals);
		signalsListViewer.addSelectionChangedListener(this);
		
		sashForm.setWeights(new int[] {70,30});
		
		mainSashForm.setWeights(new int[] {80, 20});
		ChannelEditorWidgetsFactory.createShowHidePanelButton(mainSashForm, chartPane, GeneralPreferenceConstants.SHOW_CHANNEL_EDITOR_PANEL);
	}

	// ------------------------------------------------------------------
	// Series

	/** Identifier of the series of a signal for a trial: "signal full name.trial number". */
	protected static String getSeriesID(Channel signal, int trialNumber) {
		return signal.getFullName() + "." + trialNumber;
	}

	/** Trial number of a series, from its identifier. */
	protected static int getTrialNumber(String seriesID) {
		String[] segments = seriesID.split("\\.");
		return Integer.parseInt(segments[segments.length - 1]);
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getSelectedTrialsNumbers() {
		return trialsListViewer.getStructuredSelection().toList();
	}

	@SuppressWarnings("unchecked")
	private List<Channel> getSelectedSignals() {
		return signalsListViewer.getStructuredSelection().toList();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		List<Integer> selectedTrialsNumbers = getSelectedTrialsNumbers();
		List<Channel> selectedSignals = getSelectedSignals();
		if(selectedTrialsNumbers.isEmpty() || selectedSignals.isEmpty()) {
			removeAllSeries();
			updateXAxisTitle(Collections.emptyList());
			redrawChart();
			return;
		}
		// Series to display, by trial then by signal: each selected signal for each selected trial
		Map<String, Object[]> wantedSeries = new LinkedHashMap<>();
		for (Integer trialNumber : selectedTrialsNumbers) {
			for (Channel signal : selectedSignals) wantedSeries.put(getSeriesID(signal, trialNumber), new Object[]{signal, trialNumber});
		}
		// Remove series from chart if not in selection
		for (String seriesID : getSeriesIDs()) {
			if(!wantedSeries.containsKey(seriesID)) removeSeries(seriesID);
		}
		// Add series
		for (Map.Entry<String, Object[]> series : wantedSeries.entrySet()) {
			if(!hasSeries(series.getKey())) addSeries(series.getKey(), (Channel) series.getValue()[0], (int) series.getValue()[1]);
		}
		updateXAxisTitle(selectedSignals);
		updateSeriesColors();
		resetChartScale();
		redrawChart();
	}

	/** Title of the abscissa axis according to the displayed signals: time, frequency, or both. */
	private void updateXAxisTitle(List<Channel> signals) {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean timeChannels = false;
		boolean frequencyChannels = false;
		for (Channel signal : signals) {
			if(mathEngine.isFrequencyDomain(signal)) frequencyChannels = true;
			else timeChannels = true;
		}
		String xAxisLabel = DocometreMessages.XAxisLabelTime;
		if(frequencyChannels && !timeChannels) xAxisLabel = DocometreMessages.XAxisLabelFrequency;
		if(frequencyChannels && timeChannels) xAxisLabel = DocometreMessages.XAxisLabelTimeFrequency;
		setXAxisTitle(xAxisLabel);
	}

	/**
	 * Color of each series: by position in the chart, or by trial (position of
	 * the trial in the selection) if "same color for same trial" is checked.
	 */
	private void updateSeriesColors() {
		List<String> seriesIDs = getSeriesIDs();
		List<Integer> selectedTrialsNumbers = getSelectedTrialsNumbers();
		for (int i = 0; i < seriesIDs.size(); i++) {
			String seriesID = seriesIDs.get(i);
			int index = sameColor ? selectedTrialsNumbers.indexOf(getTrialNumber(seriesID)) : i;
			setSeriesColor(seriesID, DocometreApplication.getColor((byte) index));
		}
	}

	// ------------------------------------------------------------------
	// Trial navigation, among the trials of the category

	@Override
	public void gotoNextTrial() {
		shiftSelectedTrials(1);
	}

	@Override
	public void gotoPreviousTrial() {
		shiftSelectedTrials(-1);
	}

	/**
	 * Moves every selected trial to the next (delta = 1) or previous (delta =
	 * -1) trial of the category; without selection, selects the first trial.
	 */
	private void shiftSelectedTrials(int delta) {
		List<Integer> selectedTrialsNumbers = getSelectedTrialsNumbers();
		Integer[] trialsNumbers = (Integer[]) trialsListViewer.getInput();
		List<Integer> newSelection = new ArrayList<Integer>();
		if(selectedTrialsNumbers.isEmpty()) {
			if(trialsNumbers.length >= 1) {
				newSelection.add(trialsNumbers[0]);
				trialsListViewer.setSelection(new StructuredSelection(newSelection), true);
			}
			return;
		}
		int limitIndex = Arrays.binarySearch(trialsNumbers, (delta > 0) ? Collections.max(selectedTrialsNumbers) : Collections.min(selectedTrialsNumbers));
		if(delta > 0 ? limitIndex < trialsNumbers.length - 1 : limitIndex > 0) {
			for (Integer trialNumber : selectedTrialsNumbers) {
				newSelection.add(trialsNumbers[Arrays.binarySearch(trialsNumbers, trialNumber) + delta]);
			}
			trialsListViewer.setSelection(new StructuredSelection(newSelection), true);
		}
	}
}
