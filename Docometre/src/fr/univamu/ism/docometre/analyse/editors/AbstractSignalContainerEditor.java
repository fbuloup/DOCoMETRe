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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.pgroup.PGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.handlers.UpdateWorkbenchDelegate;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * Common part of the signal editors: chart of the selected trials, trials
 * list, and groups of information (general, trials, features, markers).
 * Subclasses provide the chart (SWTChart, JOGL...) through the abstract
 * methods; a series of the chart is identified by "channel full name.trial number".
 *
 * Subclasses must call {@link #createContents()} at the end of their
 * constructor, once their own fields are initialized.
 */
public abstract class AbstractSignalContainerEditor extends Composite implements ISelectionChangedListener, TrialNavigator {

	private final Supplier<Channel> channelSupplier;
	protected boolean isFrequencyDomain;
	protected ListViewer trialsListViewer;
	private int nbTrials;

	private ComboViewer markersGroupsComboViewer;
	private ComboViewer markersGroupComboViewer;
	private Label markerXValueLabel;
	private Label markerYValueLabel;
	private Label labelNbMarkers;

	private ComboViewer featuresComboViewer;
	private Spinner trialFeatureSpinner;
	private Label featureValueLabel;

	private Spinner trialSelectionSpinner;
	private Label frontCutLabelValue;
	private Label endCutLabelValue;
	private Label samplesNumberLabelValue;
	private Label durationLabelValue;
	private Label sfLabel;
	private Button useSameColorButton;
	private Button keepScaleButton;
	private Button showSamplesButton;

	protected AbstractSignalContainerEditor(Composite parent, int style, Supplier<Channel> channelSupplier) {
		super(parent, style);
		this.channelSupplier = channelSupplier;
	}

	/** Displayed channel. */
	protected Channel getChannel() {
		return channelSupplier.get();
	}

	// ------------------------------------------------------------------
	// Chart: provided by the subclasses

	/** Creates the chart in container (first column of the editor). @return false if the chart could not be created */
	protected abstract boolean createChart(Composite container);

	/** Identifiers of the series currently in the chart. */
	protected abstract List<String> getSeriesIDs();

	protected abstract boolean hasSeries(String seriesID);

	/** Adds the series of a trial (nothing if the trial has no values). */
	protected abstract void addSeries(String seriesID, Channel signal, int trialNumber);

	protected abstract void removeSeries(String seriesID);

	protected abstract void removeAllSeries();

	protected abstract void setSeriesColor(String seriesID, Color color);

	/** Shows or hides the samples of all the series. */
	protected abstract void showSamples(boolean show);

	/** Fits the scales to the series (unless "keep scale" is checked). */
	protected abstract void resetChartScale();

	/** Redraws the chart if it does not do it by itself. */
	protected void redrawChart() {
	}

	// ------------------------------------------------------------------
	// Markers: hooks for the charts that display them

	/** Label of the markers group displayed in the chart, or null if the chart does not display markers. */
	protected String getMarkersGroupLabel() {
		return null;
	}

	protected void markersGroupLabelChanged(String markersGroupLabel) {
	}

	/** A marker {trial, x, y} was selected (null: no more selected marker). */
	protected void markerSelected(double[] marker) {
	}

	/** Markers were deleted with the editor (marker or whole group): the chart can update its markers. */
	protected void markersChanged() {
	}

	/** Selects a markers group in the editor (for example chosen in the chart); null or unknown: no selection. */
	protected void selectMarkersGroup(String markersGroupLabel) {
		if(markersGroupLabel == null) markersGroupsComboViewer.setSelection(StructuredSelection.EMPTY);
		else markersGroupsComboViewer.setSelection(new StructuredSelection(markersGroupLabel), true);
	}

	// ------------------------------------------------------------------
	// Contents

	/** Builds the editor; to be called at the end of the constructor of the subclasses. */
	protected final void createContents() {
		isFrequencyDomain = MathEngineFactory.getMathEngine().isFrequencyDomain(getChannel());
		setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout)getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 2;
		
		// Chart pane (chart and show/hide button) | trials list, resizable
		SashForm channelContainer = new SashForm(this, SWT.HORIZONTAL);
		channelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		ChannelEditorWidgetsFactory.configureSashForm(channelContainer);
		Composite chartPane = new Composite(channelContainer, SWT.NORMAL);
		chartPane.setLayout(createCompactGridLayout(2));
		
		if(!createChart(chartPane)) {
			Activator.logErrorMessage("Error while trying to create chart");
			return;
		}
		
		trialsListViewer = new ListViewer(channelContainer, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		int trialsNumber = MathEngineFactory.getMathEngine().getTrialsNumber(getChannel());
		Integer[] trials = IntStream.rangeClosed(1, trialsNumber).boxed().toArray(Integer[]::new);
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				String category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(getChannel(), Integer.parseInt(trial));
				return DocometreMessages.Trial + trial + ("".equals(category)?"":" [" + category + "]");
			}
		});
		trialsListViewer.setInput(trials);
		trialsListViewer.addSelectionChangedListener(this);
		channelContainer.setWeights(new int[] {85, 15});
		ChannelEditorWidgetsFactory.createShowHidePanelButton(channelContainer, chartPane, GeneralPreferenceConstants.SHOW_CHANNEL_EDITOR_PANEL);
		
		PGroup pGroupInfos = new PGroup(this, SWT.SMOOTH);
		pGroupInfos.setText(DocometreMessages.ChannelEditorGroupLabel);
		pGroupInfos.setExpanded(false);
		pGroupInfos.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		pGroupInfos.setLayout(new GridLayout(4, true));
		GridLayout gl3 = (GridLayout) pGroupInfos.getLayout();
		gl3.horizontalSpacing = 0;
		gl3.verticalSpacing = 0;
		gl3.marginHeight = 3;
		gl3.marginWidth = 3;
		createGeneralInfoGroup(pGroupInfos);
		createTrialsGroup(pGroupInfos);
		createFeaturesGroup(pGroupInfos);
		createMarkersGroup(pGroupInfos);
	}

	private static GridLayout createCompactGridLayout(int columns) {
		GridLayout gl = new GridLayout(columns, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		return gl;
	}

	/** The mouse wheel moves the selection of the combo. */
	private static void addMouseWheelSelection(ComboViewer comboViewer) {
		comboViewer.getCombo().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int selectionIndex = comboViewer.getCombo().getSelectionIndex();
				Object element = comboViewer.getElementAt(selectionIndex - e.count);
				if(element != null) comboViewer.setSelection(new StructuredSelection(element));
			}
		});
	}

	private static Button createIconButton(Composite parent, String imageKey, String toolTip) {
		Button button = new Button(parent, SWT.FLAT);
		button.setImage(Activator.getImage(imageKey));
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		button.setToolTipText(toolTip);
		return button;
	}

	private void createMarkersGroup(Composite infosTrialFeaturesMarkersContainer) {
		Group markersGroupsGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialFeaturesMarkersContainer, DocometreMessages.MarkersGroupTitle);
		markersGroupsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.GroupNameLabel, SWT.LEFT, false);
		Composite deleteAddMarkersGroupContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteAddMarkersGroupContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		deleteAddMarkersGroupContainer.setLayout(createCompactGridLayout(3));
		markersGroupsComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteAddMarkersGroupContainer, SWT.FILL, true);
		Button deleteMarkersGroupButton = createIconButton(deleteAddMarkersGroupContainer, IImageKeys.DELETE_ICON, DocometreMessages.DeleteSelectedMarkersGroupTooltip);
		deleteMarkersGroupButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.DeleteMarkersGroupDialogTitle, DocometreMessages.DeleteMarkersGroupDialogMessage)) {
					String[] labels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(getChannel());
					int markersGroupNumber = Arrays.asList(labels).indexOf(markersGroupsComboViewer.getStructuredSelection().getFirstElement()) + 1;
					if(markersGroupNumber > 0) {
						MathEngineFactory.getMathEngine().deleteMarkersGroup(markersGroupNumber, getChannel());
						MathEngineFactory.getMathEngine().setUpdateChannelsCache(getChannel().getSubject(), true);
						markersGroupsComboViewer.refresh();
						UpdateWorkbenchDelegate.updateCharts();
						markerSelected(null);
						markersChanged();
					}
				}
			}
		});
		Button addMarkersGroupButton = createIconButton(deleteAddMarkersGroupContainer, IImageKeys.ADD_MARKER_GROUP_ICON, DocometreMessages.CreateNewMarkersGroupTooltip);
		addMarkersGroupButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IInputValidator validator = new IInputValidator() {
					@Override
					public String isValid(String newText) {
						if(!Pattern.matches("[a-zA-Z][a-zA-Z0-9_]*", newText))
							return NLS.bind(DocometreMessages.MarkerLabelInvalid, newText);
						return null;
					}
				};
				InputDialog labelGroupMarkersInputDialog = new InputDialog(getShell(), DocometreMessages.CreateNewMarkersGroupDialogTitle, DocometreMessages.CreateNewMarkersGroupDialogMessage, "Label", validator);
				if(labelGroupMarkersInputDialog.open() == Window.OK) {
					String markersGroupLabel = labelGroupMarkersInputDialog.getValue();
					MathEngineFactory.getMathEngine().createNewMarkersGroup(getChannel(), markersGroupLabel);
					MathEngineFactory.getMathEngine().setUpdateChannelsCache(getChannel().getSubject(), true);
					markersGroupsComboViewer.refresh();
				}
			}
		});
		markersGroupsComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				return MathEngineFactory.getMathEngine().getMarkersGroupsLabels((Channel)inputElement);
			}
		});
		markersGroupsComboViewer.setLabelProvider(new LabelProvider());
		markersGroupsComboViewer.setComparator(new ViewerComparator());
		markersGroupsComboViewer.setInput(getChannel());
		markersGroupsComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int selectionIndex = markersGroupComboViewer.getCombo().getSelectionIndex();
				if(markersGroupsComboViewer.getSelection().isEmpty()) markersGroupLabelChanged("");
				else markersGroupLabelChanged(((IStructuredSelection)markersGroupsComboViewer.getSelection()).getFirstElement().toString());
				markersGroupComboViewer.refresh();
				markerXValueLabel.setText(DocometreMessages.NotAvailable_Label);
				markerYValueLabel.setText(DocometreMessages.NotAvailable_Label);
				labelNbMarkers.setText(DocometreMessages.NotAvailable_Label);
				
				String markersGroupLabel = getMarkersGroupLabel();
				if(markersGroupLabel != null && !"".equals(markersGroupLabel)) {
					Object element = markersGroupComboViewer.getElementAt(selectionIndex);
					if(element != null) markersGroupComboViewer.setSelection(new StructuredSelection(element));
				}
			}
		});
		addMouseWheelSelection(markersGroupsComboViewer);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.TrialNumberLabel, SWT.LEFT, false);
		Composite deleteMarkerTrialContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteMarkerTrialContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		deleteMarkerTrialContainer.setLayout(createCompactGridLayout(2));
		markersGroupComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteMarkerTrialContainer, SWT.FILL, true);
		markersGroupComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				String markersGroupLabel = getMarkersGroupLabel();
				if(markersGroupLabel == null || "".equals(markersGroupLabel)) return new Object[0];
				double[][] values = MathEngineFactory.getMathEngine().getMarkers(markersGroupLabel, (Channel)inputElement);
				return new ArrayList<double[]>(Arrays.asList(values)).toArray();
			}
		});
		markersGroupComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof double[])) return "";
				double[] values = (double[])element;
				return String.valueOf((int)values[0]);
			}
		});
		markersGroupComboViewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				if(element instanceof double[]) {
					double[] values = (double[])element;
					return (int)values[0]; 
				}
				return super.category(element);
			}
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				int result = super.compare(viewer, e1, e2);
				if(result == 0)
					if(e1 instanceof double[] && e2 instanceof double[]) {
						double[] value1 = (double[])e1;
						double[] value2 = (double[])e2;
						return (int)Math.signum(value1[1] - value2[1]); 
					}
				return result;
			}
		});
		markersGroupComboViewer.setInput(getChannel());
		markersGroupComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			private int countFrequencies(double[][] trialsValues, int trialNumber) {
				int count = 0;
				for(int index = 0; index < trialsValues.length; index++) {
					if(trialsValues[index][0] == trialNumber) count++;
				}
				return count;
			}
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if(((IStructuredSelection)markersGroupComboViewer.getSelection()).isEmpty()) return;
				double[] values = (double[])((IStructuredSelection)markersGroupComboViewer.getSelection()).getFirstElement();
				markerSelected(values);
				markerXValueLabel.setText(String.valueOf(values[1]));
				markerYValueLabel.setText(String.valueOf(values[2]));
				double[][] groupValues = MathEngineFactory.getMathEngine().getMarkers(getMarkersGroupLabel(), getChannel());
				labelNbMarkers.setText(String.valueOf(countFrequencies(groupValues, (int)values[0])));
			}
		});
		addMouseWheelSelection(markersGroupComboViewer);
		
		Button deleteMarkerTrialButton = createIconButton(deleteMarkerTrialContainer, IImageKeys.DELETE_ICON, DocometreMessages.DeleteSelectedMarkerTrialTooltip);
		deleteMarkerTrialButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.DeleteMarkerDialogTitle, DocometreMessages.DeleteMarkerDialogMessage)) {
					if(((IStructuredSelection)markersGroupComboViewer.getSelection()).isEmpty()) return;
					int selectionIndex = markersGroupComboViewer.getCombo().getSelectionIndex();
					double[] values = (double[])((IStructuredSelection)markersGroupComboViewer.getSelection()).getFirstElement();
					String markersGroupLabel = (String) ((IStructuredSelection)markersGroupsComboViewer.getSelection()).getFirstElement();
					MathEngineFactory.getMathEngine().deleteMarker(markersGroupLabel, (int)values[0], values[1], values[2], getChannel());
					markersGroupsComboViewer.refresh();
					updateMarkersGroup(markersGroupLabel);
					MathEngineFactory.getMathEngine().setUpdateChannelsCache(getChannel().getSubject(), true);
					UpdateWorkbenchDelegate.updateCharts();
					markerSelected(null);
					markersChanged();
					
					Object element = markersGroupComboViewer.getElementAt(selectionIndex-1);
					if(element != null) markersGroupComboViewer.setSelection(new StructuredSelection(element));
				}
			}
		});
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.XMarkerValueLabel, SWT.LEFT, false);
		markerXValueLabel = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.YMarkerValueLabel, SWT.LEFT, false);
		markerYValueLabel = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.MarkersNumberInSelectedTrial, SWT.LEFT, false);
		labelNbMarkers  = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, false);
		labelNbMarkers.setToolTipText(DocometreMessages.MarkersNumberInSelectedTrialToolTip);
	}

	/** Refreshes the markers of the selected group and selects the marker {trial, x, y} if it is given. */
	public void updateMarkersGroup(String markersGroupLabel, Object... parameters) {
		markersGroupComboViewer.refresh();
		markerXValueLabel.setText(DocometreMessages.NotAvailable_Label);
		markerYValueLabel.setText(DocometreMessages.NotAvailable_Label);
		labelNbMarkers.setText(DocometreMessages.NotAvailable_Label);
		if(parameters != null && parameters.length == 3) {
			int trialNumber = (int)parameters[0]; 
			double x = (double)parameters[1]; 
			double y = (double)parameters[2];
			for(int n = 0; n < markersGroupComboViewer.getCombo().getItemCount(); n++) {
				double[] element = (double[]) markersGroupComboViewer.getElementAt(n);
				if(element[0] == trialNumber && element[1] == x && element[2] == y) {
					markersGroupComboViewer.setSelection(new StructuredSelection(element));
					break;
				}
			}
		}
	}

	private void createFeaturesGroup(Composite infosTrialFeaturesMarkersContainer) {
		Group featuresGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialFeaturesMarkersContainer, DocometreMessages.FeaturesGroupTitle);
		featuresGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.FeaturesNameLabel, SWT.LEFT, false);
		Composite deleteFeatureContainer = new Composite(featuresGroup, SWT.NORMAL);
		deleteFeatureContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		deleteFeatureContainer.setLayout(createCompactGridLayout(2));
		featuresComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteFeatureContainer, SWT.FILL, true);
		featuresComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				return MathEngineFactory.getMathEngine().getFeaturesLabels((Channel)inputElement);
			}
		});
		featuresComboViewer.setLabelProvider(new LabelProvider());
		featuresComboViewer.setComparator(new ViewerComparator());
		featuresComboViewer.setInput(getChannel());
		featuresComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateFeatureValueHandler();
			}
		});
		
		Button deleteFeatureButton = createIconButton(deleteFeatureContainer, IImageKeys.DELETE_ICON, DocometreMessages.DeleteSelectedFeatureTooltip);
		deleteFeatureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.DeleteFeatureDialogTitle, DocometreMessages.DeleteFeatureDialogMessage)) {
					int featureNumber = featuresComboViewer.getCombo().getSelectionIndex() + 1;
					if(featureNumber > 0) {
						Channel channel = (Channel) featuresComboViewer.getInput();
						MathEngineFactory.getMathEngine().deleteFeature(featureNumber,channel);
						MathEngineFactory.getMathEngine().setUpdateChannelsCache(channel.getSubject(), true);
						featuresComboViewer.refresh();
					}
				}
			}
		});
		
		ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.TrialNumberLabel, SWT.LEFT, false);
		trialFeatureSpinner = createTrialSpinner(featuresGroup, this::updateFeatureValueHandler);
		
		ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.ValueLabel, SWT.LEFT, false);
		featureValueLabel = ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		initTrialSpinner(trialFeatureSpinner);
	}

	/** Trial spinner: handler called on selection and mouse wheel. */
	private static Spinner createTrialSpinner(Composite parent, Runnable handler) {
		Spinner spinner = ChannelEditorWidgetsFactory.createSpinner(parent, SWT.FILL, true);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handler.run();
			}
		});
		spinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				handler.run();
			}
		});
		return spinner;
	}

	/** Range 1..number of trials, first trial selected (listeners notified). */
	private void initTrialSpinner(Spinner spinner) {
		spinner.setMaximum(nbTrials);
		spinner.setMinimum(nbTrials>0?1:0);
		setTrialSpinner(spinner, 1);
	}

	private static void setTrialSpinner(Spinner spinner, int trialNumber) {
		spinner.setSelection(trialNumber);
		spinner.notifyListeners(SWT.Selection, new Event());
	}

	private void updateFeatureValueHandler() {
		if(!featuresComboViewer.getSelection().isEmpty()) {
			int trialNumber = trialFeatureSpinner.getSelection();
			String featureLabel = ((IStructuredSelection)featuresComboViewer.getSelection()).getFirstElement().toString();
			double[][] values = MathEngineFactory.getMathEngine().getFeature(featureLabel, (Channel) featuresComboViewer.getInput());
			if(values.length > 0) {
				if(values[0].length == 1) featureValueLabel.setText(String.valueOf(values[trialNumber - 1][0]));
				else  featureValueLabel.setText(Arrays.toString(values[trialNumber - 1]));
			}
		} else {
			featureValueLabel.setText(DocometreMessages.NotAvailable_Label);
		}
	}

	private void createTrialsGroup(Composite infosTrialsFieldsMarkersContainer) {
		Group trialsGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialsFieldsMarkersContainer, DocometreMessages.TrialsGroupLabel);
		trialsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.Trial, SWT.LEFT, false);
		trialSelectionSpinner = createTrialSpinner(trialsGroup, this::updateTrialsValuesHandler);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.FrontCutLabel, SWT.LEFT, false);
		frontCutLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.EndCutLabel, SWT.LEFT, false);
		endCutLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.SamplesNumberLabel, SWT.LEFT, false);
		samplesNumberLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, isFrequencyDomain ? DocometreMessages.MaxFrequencyLabel : DocometreMessages.DurationLabel, SWT.LEFT, false);
		durationLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);

		initTrialSpinner(trialSelectionSpinner);
	}

	private void updateTrialsValuesHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		Channel channel = getChannel();
		int trialNumber = trialSelectionSpinner.getSelection();
		int samplesNumber = mathEngine.getSamplesNumber(channel, trialNumber);
		int frontCut = mathEngine.getFrontCut(channel, trialNumber);
		int endCut = mathEngine.getEndCut(channel, trialNumber);
		double sf = mathEngine.getSampleFrequency(channel);
		double duration = samplesNumber > 0 ? 1f*(samplesNumber-1)/sf : 0.0;
		if(isFrequencyDomain) {
			if(samplesNumber % 2 == 1) duration = (samplesNumber - 1)/(2*sf);
			else duration = (samplesNumber - 2)/(2*sf);
		}
		duration = duration < 0 ? 0 : duration;
		setValue(frontCutLabelValue, Integer.toString(frontCut));
		setValue(endCutLabelValue, Integer.toString(endCut));
		setValue(samplesNumberLabelValue, Integer.toString(samplesNumber));
		setValue(durationLabelValue, Double.toString(duration));
	}

	private static void setValue(Label label, String value) {
		label.setText(value);
		label.setToolTipText(value);
	}

	private void createGeneralInfoGroup(Composite infosTrialsFieldsMarkersContainer) {
		Group infosGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialsFieldsMarkersContainer, DocometreMessages.GeneralInfoGroupLabel);
		infosGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.SignalNameLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, getChannel().getName(), SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.Subject_Label, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, getChannel().getParent().getName(), SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.Experiment_Label, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, getChannel().getParent().getParent().getName(), SWT.LEFT, true);
		
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(getChannel());
		if(!isFrequencyDomain) {
			ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.FrequencyLabel, SWT.LEFT, false);
			sfLabel = ChannelEditorWidgetsFactory.createLabel(infosGroup, Double.toString(sf), SWT.LEFT, true);
		} else {
			ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.FrequencyStepLabel, SWT.LEFT, false);
			sfLabel = ChannelEditorWidgetsFactory.createLabel(infosGroup, Double.toString(1/sf), SWT.LEFT, true);
		}
		
		nbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(getChannel());
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.TrialsNumberLabel2, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, Integer.toString(nbTrials), SWT.LEFT, true);
		
		useSameColorButton = createCheckButton(infosGroup, DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSeriesColorsHandler();
			}
		});
		
		keepScaleButton = createCheckButton(infosGroup, DocometreMessages.KeepScaleWhenTrialChange);
		
		showSamplesButton = createCheckButton(infosGroup, DocometreMessages.ShowSamples);
		showSamplesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showSamples(showSamplesButton.getSelection());
			}
		});
	}

	private static Button createCheckButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		button.setText(text);
		button.setToolTipText(text);
		return button;
	}

	/** State of the "show samples" check box. */
	protected boolean isShowSamplesChecked() {
		return showSamplesButton.getSelection();
	}

	/** Updates the "show samples" check box (for example when the chart changes it); SWT thread only, no notification. */
	protected void setShowSamplesChecked(boolean checked) {
		if(!showSamplesButton.isDisposed()) showSamplesButton.setSelection(checked);
	}

	// ------------------------------------------------------------------
	// Series

	/** Identifier of the series of a trial: "channel full name.trial number". */
	protected String getSeriesID(int trialNumber) {
		return getChannel().getFullName() + "." + trialNumber;
	}

	/** Trial number of a series, from its identifier. */
	protected static int getTrialNumber(String seriesID) {
		String[] segments = seriesID.split("\\.");
		return Integer.parseInt(segments[segments.length - 1]);
	}

	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new HashSet<Integer>();
		for (String seriesID : getSeriesIDs()) trials.add(getTrialNumber(seriesID));
		return trials;
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getSelectedTrialsNumbers() {
		return trialsListViewer.getStructuredSelection().toList();
	}

	protected void updateSeriesColorsHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean sameColor = useSameColorButton.getSelection();
		ArrayList<String> categories = new ArrayList<>();
		for (String seriesID : getSeriesIDs()) {
			if(sameColor) {
				try {
					int seriesIDTrial = getTrialNumber(seriesID); 
					String fullChannelName = seriesID.replaceAll("\\.\\d+$", "");
					String fullSubjectName = fullChannelName.replaceAll("\\.\\w+$", "");
					IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(fullSubjectName.split("\\.")[1]);
					ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					Channel channel = channelsContainer.getChannelFromName(fullChannelName);
					String category = mathEngine.getCategoryForTrialNumber(channel, seriesIDTrial);
					if(categories.indexOf(category) == -1) categories.add(category);
					setSeriesColor(seriesID, DocometreApplication.getColor((byte) categories.indexOf(category)));
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else {
				int index = getSelectedTrialsNumbers().indexOf(getTrialNumber(seriesID));
				setSeriesColor(seriesID, DocometreApplication.getColor((byte) index));
			}
		}
		redrawChart();
	}

	private void addSeriesToChart(int trialNumber) {
		addSeries(getSeriesID(trialNumber), getChannel(), trialNumber);
	}

	private void updateChartScale() {
		if(!keepScaleButton.getSelection()) resetChartScale();
		redrawChart();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if(trialsListViewer.getStructuredSelection().isEmpty()) {
			removeAllSeries();
		} else {
			List<Integer> selectedTrialsNumbers = getSelectedTrialsNumbers();
			// Remove series from chart if not in selection
			for (Integer trialNumberInChart : getTrialsInChart()) {
				if(!selectedTrialsNumbers.contains(trialNumberInChart)) removeSeries(getSeriesID(trialNumberInChart));
			}
			// Add series
			for (Integer selectedTrialNumber : selectedTrialsNumbers) {
				if(!hasSeries(getSeriesID(selectedTrialNumber))) addSeriesToChart(selectedTrialNumber);
			}
		}
		updateSeriesColorsHandler();
		updateChartScale();
	}

	public void update() {
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(getChannel());
		sfLabel.setText(Double.toString(sf));
		updateTrialsValuesHandler();
		markersGroupsComboViewer.refresh();
		markersGroupComboViewer.refresh();
		featuresComboViewer.refresh();
		refreshChart();
	}

	private void refreshChart() {
		List<Integer> selectedTrialsNumbers = getSelectedTrialsNumbers();
		// Remove series from chart, then add them again
		for (Integer trialNumberInChart : selectedTrialsNumbers) removeSeries(getSeriesID(trialNumberInChart));
		for (Integer selectedTrialNumber : selectedTrialsNumbers) addSeriesToChart(selectedTrialNumber);
		updateSeriesColorsHandler();
		updateChartScale();
	}

	// ------------------------------------------------------------------
	// Trial navigation

	@Override
	public void gotoNextTrial() {
		List<Integer> selectedTrialsNumbers = new ArrayList<>(getSelectedTrialsNumbers());
		int maxTrialNumber = trialsListViewer.getList().getItemCount();
		if(selectedTrialsNumbers.isEmpty()) {
			if(maxTrialNumber >= 1) selectTrials(Collections.singletonList(1), true);
			return;
		}
		if(Collections.max(selectedTrialsNumbers) < maxTrialNumber) shiftSelectedTrials(selectedTrialsNumbers, 1);
	}

	@Override
	public void gotoPreviousTrial() {
		List<Integer> selectedTrialsNumbers = new ArrayList<>(getSelectedTrialsNumbers());
		int maxTrialNumber = trialsListViewer.getList().getItemCount();
		if(selectedTrialsNumbers.isEmpty()) {
			if(maxTrialNumber >= 1) {
				selectTrials(Collections.singletonList(maxTrialNumber), true);
				trialSelectionSpinner.setSelection(maxTrialNumber);
			}
			return;
		}
		if(Collections.min(selectedTrialsNumbers) > 1) shiftSelectedTrials(selectedTrialsNumbers, -1);
	}

	/** Moves every selected trial by delta, and the spinners to the first one. */
	private void shiftSelectedTrials(List<Integer> selectedTrialsNumbers, int delta) {
		for (int i = 0; i < selectedTrialsNumbers.size(); i++) selectedTrialsNumbers.set(i, selectedTrialsNumbers.get(i) + delta);
		selectTrials(selectedTrialsNumbers, false);
		int minSelectedTrialNumber = Collections.min(selectedTrialsNumbers);
		setTrialSpinner(trialSelectionSpinner, minSelectedTrialNumber);
		setTrialSpinner(trialFeatureSpinner, minSelectedTrialNumber);
	}

	private void selectTrials(List<Integer> trialsNumbers, boolean showSelection) {
		trialsListViewer.setSelection(new StructuredSelection(trialsNumbers), true);
		if(showSelection) trialsListViewer.getList().showSelection();
	}
	
	public abstract Object getChart();
}
