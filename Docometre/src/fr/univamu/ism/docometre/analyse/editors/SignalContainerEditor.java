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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.handlers.UpdateWorkbenchDelegate;

public class SignalContainerEditor extends Composite implements ISelectionChangedListener, TrialNavigator, IMarkersManager {
	
	//private static String[] graphicalSymbols = new String[] {"\u25A1", "\u25C7", "\u25B3", "\u25CB", "\u2606", "+"};
	
	private ChannelEditor channelEditor;
	private ListViewer trialsListViewer;
	private InteractiveChart chart;
	private int nbTrials;
	
	private MarkersManager markersManager;
	private ComboViewer markersGroupComboViewer;
	private Label markerXValueLabel;
	private Label markerYValueLabel;
	
	private Label featureValueLabel;
	private ComboViewer featuresComboViewer;
	private Spinner trialFeatureSpinner;
	private Spinner trialSelectionSpinner;
	private Label frontCutLabelValue;
	private Label endCutLabelValue;
	private Label samplesNumberLabelValue;
	private Label durationLabelValue;
	private Button useSameColorButton;
	private ComboViewer markersGroupsComboViewer;
	private Label labelNbMarkers;
	private Button keepScaleButton;
	private Button showSamplesButton;
	private Label sfLabel;

	public SignalContainerEditor(Composite parent, int style, ChannelEditor channelEditor) {
		super(parent, style);
		this.channelEditor = channelEditor;
		setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout)getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 2;
		
		Composite channelContainer = new Composite(this, SWT.NORMAL);
		GridLayout gl2 = new GridLayout(2, false);
		gl2.horizontalSpacing = 0;
		gl2.verticalSpacing = 0;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		channelContainer.setLayout(gl2);
		channelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		chart = ChannelEditorWidgetsFactory.createChart(channelContainer, 1);
		markersManager = new MarkersManager(this);
		
		trialsListViewer = new ListViewer(channelContainer, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		int nbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(channelEditor.getChannel());
		Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				String category = "";
				Channel signal = channelEditor.getChannel();
				category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(signal, Integer.parseInt(trial));
				return DocometreMessages.Trial + trial + ("".equals(category)?"":" [" + category + "]");
			}
		});
		trialsListViewer.setInput(trials);
		trialsListViewer.addSelectionChangedListener(this);
		
		ChannelEditorWidgetsFactory.createSeparator(this, true, false, 1, SWT.HORIZONTAL);
		Button showHideInfosButton = new Button(this, SWT.FLAT);
		showHideInfosButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		((GridData)showHideInfosButton.getLayoutData()).heightHint = 10;
		((GridData)showHideInfosButton.getLayoutData()).widthHint = 12;
		showHideInfosButton.setImage(Activator.getImage(IImageKeys.HIDE_PANNEL));
		
		Composite infosTrialFeaturesMarkersContainer = new Composite(this, SWT.NORMAL);
		infosTrialFeaturesMarkersContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infosTrialFeaturesMarkersContainer.setLayout(new GridLayout(4, true));
		GridLayout gl3 = (GridLayout) infosTrialFeaturesMarkersContainer.getLayout();
		gl3.horizontalSpacing = 0;
		gl3.verticalSpacing = 0;
		gl3.marginHeight = 0;
		gl3.marginWidth = 0;
		createGeneralInfoGroup(infosTrialFeaturesMarkersContainer);
		createTrialsGroup(infosTrialFeaturesMarkersContainer);
		createFeaturesGroup(infosTrialFeaturesMarkersContainer);
		createMarkersGroup(infosTrialFeaturesMarkersContainer);
		
		showHideInfosButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)infosTrialFeaturesMarkersContainer.getLayoutData();
				boolean exclude = gd.exclude;
				gd.exclude = !exclude;
				infosTrialFeaturesMarkersContainer.setVisible(exclude);
				infosTrialFeaturesMarkersContainer.getParent().layout(true);
				if(gd.exclude) showHideInfosButton.setImage(Activator.getImage(IImageKeys.SHOW_PANNEL));
				else showHideInfosButton.setImage(Activator.getImage(IImageKeys.HIDE_PANNEL));
			}
		});
	}
	

	public InteractiveChart getChart() {
		return chart;
	}

	
	private void createMarkersGroup(Composite infosTrialFeaturesMarkersContainer) {
		Group markersGroupsGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialFeaturesMarkersContainer, DocometreMessages.MarkersGroupTitle);
		markersGroupsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.GroupNameLabel, SWT.LEFT, false);
		Composite deleteAddMarkersGroupContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteAddMarkersGroupContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout(3, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		deleteAddMarkersGroupContainer.setLayout(gl);
		markersGroupsComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteAddMarkersGroupContainer, SWT.FILL, true);
		Button deleteMarkersGroupButton = new Button(deleteAddMarkersGroupContainer, SWT.FLAT);
		deleteMarkersGroupButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteMarkersGroupButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteMarkersGroupButton.setToolTipText(DocometreMessages.DeleteSelectedMarkersGroupTooltip);
		deleteMarkersGroupButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.DeleteMarkersGroupDialogTitle, DocometreMessages.DeleteMarkersGroupDialogMessage)) {
					String[] labels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(channelEditor.getChannel());
					int markersGroupNumber = Arrays.asList(labels).indexOf(markersGroupsComboViewer.getStructuredSelection().getFirstElement()) + 1;
					if(markersGroupNumber > 0) {
						MathEngineFactory.getMathEngine().deleteMarkersGroup(markersGroupNumber, channelEditor.getChannel());
						MathEngineFactory.getMathEngine().setUpdateChannelsCache(channelEditor.getChannel().getSubject(), true);
						markersGroupsComboViewer.refresh();
						UpdateWorkbenchDelegate.updateCharts();
						markersManager.setSelectedMarker(null);
					}
				}
			}
		});
		Button addMarkersGroupButton = new Button(deleteAddMarkersGroupContainer, SWT.FLAT);
		addMarkersGroupButton.setImage(Activator.getImage(IImageKeys.ADD_MARKER_GROUP_ICON));
		addMarkersGroupButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		addMarkersGroupButton.setToolTipText(DocometreMessages.CreateNewMarkersGroupTooltip);
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
					MathEngineFactory.getMathEngine().createNewMarkersGroup(channelEditor.getChannel(), markersGroupLabel);
					MathEngineFactory.getMathEngine().setUpdateChannelsCache(channelEditor.getChannel().getSubject(), true);
					markersGroupsComboViewer.refresh();
				}
			}
		});
		markersGroupsComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				Channel signal = (Channel)inputElement;
				String[] labels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(signal);
				return labels;
			}
		});
		markersGroupsComboViewer.setLabelProvider(new LabelProvider());
		markersGroupsComboViewer.setComparator(new ViewerComparator());
		markersGroupsComboViewer.setInput(channelEditor.getChannel());
		markersGroupsComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int selectionIndex = markersGroupComboViewer.getCombo().getSelectionIndex();
				if(markersGroupsComboViewer.getSelection().isEmpty()) markersManager.setMarkersGroupLabel("");
				else markersManager.setMarkersGroupLabel(((IStructuredSelection)markersGroupsComboViewer.getSelection()).getFirstElement().toString());
				markersGroupComboViewer.refresh();
				markerXValueLabel.setText(DocometreMessages.NotAvailable_Label);
				markerYValueLabel.setText(DocometreMessages.NotAvailable_Label);
				labelNbMarkers.setText(DocometreMessages.NotAvailable_Label);
				
				if(!"".equals(markersManager.getMarkersGroupLabel())) {
					Object element = markersGroupComboViewer.getElementAt(selectionIndex);
					if(element != null) {
						StructuredSelection structuredSelection = new StructuredSelection(element);
						markersGroupComboViewer.setSelection(structuredSelection);
					}
				}
				
			}
		});
		markersGroupsComboViewer.getCombo().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int selectionIndex = markersGroupsComboViewer.getCombo().getSelectionIndex();
				Object element = markersGroupsComboViewer.getElementAt(selectionIndex- e.count);
				if(element != null) {
					StructuredSelection structuredSelection = new StructuredSelection(element);
					markersGroupsComboViewer.setSelection(structuredSelection);
				}
			}
		});
		
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.TrialNumberLabel, SWT.LEFT, false);
		Composite deleteMarkerTrialContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteMarkerTrialContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl2 = new GridLayout(2, false);
		gl2.horizontalSpacing = 0;
		gl2.verticalSpacing = 0;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		deleteMarkerTrialContainer.setLayout(gl2);
		markersGroupComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteMarkerTrialContainer, SWT.FILL, true);
		markersGroupComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				if(markersManager.getMarkersGroupLabel() == null || "".equals(markersManager.getMarkersGroupLabel())) return new Object[0];
				Channel signal = (Channel)inputElement;
				double[][] values = MathEngineFactory.getMathEngine().getMarkers(markersManager.getMarkersGroupLabel(), signal);
				ArrayList<double[]> valuesString = new ArrayList<double[]>();
				for (double[] trialValues : values) {
					valuesString.add(trialValues);
				}
				return valuesString.toArray();
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
		markersGroupComboViewer.setInput(channelEditor.getChannel());
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
				markersManager.setSelectedMarker(values);
				chart.redraw();
				markerXValueLabel.setText(String.valueOf(values[1]));
				markerYValueLabel.setText(String.valueOf(values[2]));
				Channel signal = channelEditor.getChannel();
				double[][] values2 = MathEngineFactory.getMathEngine().getMarkers(markersManager.getMarkersGroupLabel(), signal);
				double[] trialValues = ((double[])((IStructuredSelection)markersGroupComboViewer.getSelection()).getFirstElement());
				int frequency = countFrequencies(values2, (int)trialValues[0]);
				labelNbMarkers.setText(String.valueOf(frequency));
			}
		});
		markersGroupComboViewer.getCombo().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int selectionIndex = markersGroupComboViewer.getCombo().getSelectionIndex();
				Object element = markersGroupComboViewer.getElementAt(selectionIndex- e.count);
				if(element != null) {
					StructuredSelection structuredSelection = new StructuredSelection(element);
					markersGroupComboViewer.setSelection(structuredSelection);
				}
			}
		});
		
		Button deleteMarkerTrialButton = new Button(deleteMarkerTrialContainer, SWT.FLAT);
		deleteMarkerTrialButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteMarkerTrialButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteMarkerTrialButton.setToolTipText(DocometreMessages.DeleteSelectedMarkerTrialTooltip);
		deleteMarkerTrialButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.DeleteMarkerDialogTitle, DocometreMessages.DeleteMarkerDialogMessage)) {
					if(((IStructuredSelection)markersGroupComboViewer.getSelection()).isEmpty()) return;
					
					int selectionIndex = markersGroupComboViewer.getCombo().getSelectionIndex();
					
					Object selectedElement = ((IStructuredSelection)markersGroupComboViewer.getSelection()).getFirstElement();
					double[] values = (double[])selectedElement;
					String markersGroupLabel = (String) ((IStructuredSelection)markersGroupsComboViewer.getSelection()).getFirstElement();
					MathEngineFactory.getMathEngine().deleteMarker(markersGroupLabel, (int)values[0], values[1], values[2], channelEditor.getChannel());
					markersGroupsComboViewer.refresh();
					updateMarkersGroup(markersGroupLabel);
					MathEngineFactory.getMathEngine().setUpdateChannelsCache(channelEditor.getChannel().getSubject(), true);
					UpdateWorkbenchDelegate.updateCharts();
					markersManager.setSelectedMarker(null);
					
					Object element = markersGroupComboViewer.getElementAt(selectionIndex-1);
					if(element != null) {
						StructuredSelection structuredSelection = new StructuredSelection(element);
						markersGroupComboViewer.setSelection(structuredSelection);
					}
				}
			}
		});
		
		/*ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.GraphicalSymbolLabel, SWT.LEFT, false);
		ComboViewer graphicalComboViewer = ChannelEditorWidgetsFactory.createCombo(markersGroupsGroup, SWT.FILL, true);
		graphicalComboViewer.setContentProvider(new ArrayContentProvider());
		graphicalComboViewer.setLabelProvider(new LabelProvider());
		graphicalComboViewer.setInput(graphicalSymbols);*/
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.XMarkerValueLabel, SWT.LEFT, false);
		markerXValueLabel = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.YMarkerValueLabel, SWT.LEFT, false);
		markerYValueLabel = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.MarkersNumberInSelectedTrial, SWT.LEFT, false);
		labelNbMarkers  = ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, false);
		labelNbMarkers.setToolTipText(DocometreMessages.MarkersNumberInSelectedTrialToolTip);
	}
	
	private void createFeaturesGroup(Composite infosTrialFeaturesMarkersContainer) {
		Group featuresGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialFeaturesMarkersContainer, DocometreMessages.FeaturesGroupTitle);
		featuresGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.FeaturesNameLabel, SWT.LEFT, false);
		Composite deleteFeatureContainer = new Composite(featuresGroup, SWT.NORMAL);
		deleteFeatureContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		deleteFeatureContainer.setLayout(gl);
		featuresComboViewer = ChannelEditorWidgetsFactory.createCombo(deleteFeatureContainer, SWT.FILL, true);
		featuresComboViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof Channel)) return new Object[0];
				Channel signal = (Channel)inputElement;
				String[] labels = MathEngineFactory.getMathEngine().getFeaturesLabels(signal);
				return labels;
			}
		});
		featuresComboViewer.setLabelProvider(new LabelProvider());
		featuresComboViewer.setComparator(new ViewerComparator());
		featuresComboViewer.setInput(channelEditor.getChannel());
		featuresComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateFeatureValueHandler();
			}
		});
		
		Button deleteFeatureButton = new Button(deleteFeatureContainer, SWT.FLAT);
		deleteFeatureButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteFeatureButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteFeatureButton.setToolTipText(DocometreMessages.DeleteSelectedFeatureTooltip);
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
		trialFeatureSpinner = ChannelEditorWidgetsFactory.createSpinner(featuresGroup, SWT.FILL, true);
		trialFeatureSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFeatureValueHandler();
			}
		});
		trialFeatureSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				updateFeatureValueHandler();
			}
		});
		
		ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.ValueLabel, SWT.LEFT, false);
		featureValueLabel = ChannelEditorWidgetsFactory.createLabel(featuresGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		trialFeatureSpinner.setMaximum(nbTrials);
		trialFeatureSpinner.setMinimum(nbTrials>0?1:0);
		trialFeatureSpinner.setSelection(1);
		trialFeatureSpinner.notifyListeners(SWT.Selection, new Event());
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
		trialSelectionSpinner = ChannelEditorWidgetsFactory.createSpinner(trialsGroup, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.FrontCutLabel, SWT.LEFT, false);
		frontCutLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.EndCutLabel, SWT.LEFT, false);
		endCutLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.SamplesNumberLabel, SWT.LEFT, false);
		samplesNumberLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.DurationLabel, SWT.LEFT, false);
		durationLabelValue = ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.FILL, true);

		trialSelectionSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTrialsValuesHandler();
			}
		});
		trialSelectionSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				updateTrialsValuesHandler();
			}
		});
		trialSelectionSpinner.setMaximum(nbTrials);
		trialSelectionSpinner.setMinimum(nbTrials>0?1:0);
		trialSelectionSpinner.setSelection(1);
		trialSelectionSpinner.notifyListeners(SWT.Selection, new Event());
	}
	
	private void updateTrialsValuesHandler() {
		int samplesNumber = MathEngineFactory.getMathEngine().getSamplesNumber(channelEditor.getChannel(), trialSelectionSpinner.getSelection());
		int frontCut = MathEngineFactory.getMathEngine().getFrontCut(channelEditor.getChannel(), trialSelectionSpinner.getSelection());
		int endCut =  MathEngineFactory.getMathEngine().getEndCut(channelEditor.getChannel(), trialSelectionSpinner.getSelection());
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(channelEditor.getChannel());
		double duration = 1f*(samplesNumber-1)/sf;
		frontCutLabelValue.setText(Integer.toString(frontCut));
		endCutLabelValue.setText(Integer.toString(endCut));
		samplesNumberLabelValue.setText(Integer.toString(samplesNumber));
		durationLabelValue.setText(Double.toString(duration));
	}

	private void createGeneralInfoGroup(Composite infosTrialsFieldsMarkersContainer) {
		Group infosGroup = ChannelEditorWidgetsFactory.createGroup(infosTrialsFieldsMarkersContainer, DocometreMessages.GeneralInfoGroupLabel);
		infosGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.SignalNameLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, channelEditor.getChannel().getName(), SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.Subject_Label, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, channelEditor.getChannel().getParent().getName(), SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.Experiment_Label, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, channelEditor.getChannel().getParent().getParent().getName(), SWT.LEFT, true);
		
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(channelEditor.getChannel());
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.FrequencyLabel, SWT.LEFT, false);
		sfLabel = ChannelEditorWidgetsFactory.createLabel(infosGroup, Double.toString(sf), SWT.LEFT, true);
		
		nbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(channelEditor.getChannel());
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.TrialsNumberLabel2, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, Integer.toString(nbTrials), SWT.LEFT, true);
		
		useSameColorButton = new Button(infosGroup, SWT.CHECK);
		useSameColorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSeriesColorsHandler();
			}
		});
		
		keepScaleButton = new Button(infosGroup, SWT.CHECK);
		keepScaleButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		keepScaleButton.setText(DocometreMessages.KeepScaleWhenTrialChange);
		
		showSamplesButton = new Button(infosGroup, SWT.CHECK);
		showSamplesButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		showSamplesButton.setText(DocometreMessages.ShowSamples);
		showSamplesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISeries[] allSeries = chart.getSeriesSet().getSeries();
				for (ISeries series : allSeries) {
					if(showSamplesButton.getSelection()) ((ILineSeries)series).setSymbolType(PlotSymbolType.CIRCLE);
					else ((ILineSeries)series).setSymbolType(PlotSymbolType.NONE);
					((ILineSeries)series).setSymbolColor(((ILineSeries)series).getLineColor());
				}
				chart.redraw();
			}
		});
		
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

	protected void updateSeriesColorsHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean sameColor = useSameColorButton.getSelection();
		String[] seriesIDs = getSeriesIDs();
		ArrayList<String> categories = new ArrayList<>();
		for (String seriesID : seriesIDs) {
			ISeries series = chart.getSeriesSet().getSeries(seriesID);
			if(sameColor) {
				try {
					int seriesIDTrial = Integer.parseInt(seriesID.split("\\.")[seriesID.split("\\.").length - 1]); 
					String fullChannelName = seriesID.replaceAll("\\.\\d+$", "");
					String fullSubjectName = fullChannelName.replaceAll("\\.\\w+$", "");
					IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(fullSubjectName.split("\\.")[1]);
					ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					Channel channel = channelsContainer.getChannelFromName(fullChannelName);
					String category = mathEngine.getCategoryForTrialNumber(channel, seriesIDTrial);
					if(categories.indexOf(category) == -1) categories.add(category);
					((ILineSeries)series).setLineColor(ColorUtil.getColor((byte) categories.indexOf(category)));
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else {
				String[] trialNumberString = series.getId().split("\\.");
				int trialNumber = Integer.parseInt(trialNumberString[trialNumberString.length - 1]);
				int index = trialsListViewer.getStructuredSelection().toList().indexOf(trialNumber);
				((ILineSeries)series).setLineColor(ColorUtil.getColor((byte) index));
			}
			((ILineSeries)series).setSymbolColor(((ILineSeries)series).getLineColor());
		}
		chart.redraw();
		
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
		updateSeriesColorsHandler();
		if(!keepScaleButton.getSelection()) chart.getAxisSet().adjustRange();
		chart.redraw();
	}

	private void removeSeriesFromChart(Integer trialNumber) {
		String seriesID = channelEditor.getChannel().getFullName() + "." + trialNumber;
		chart.removeSeries(seriesID);
	}
	
	private void addSeriesToChart(Integer trialNumber) {
		// Get x and Y values for this signal and trial
		Channel signal = channelEditor.getChannel();
		double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(signal, trialNumber);
		double[] xValues = MathEngineFactory.getMathEngine().getTimeValuesForSignal(signal, trialNumber);
		// 
		if(yValues == null || xValues == null || yValues.length ==0 || xValues.length == 0) return;
		// Add Series
		String seriesID = signal.getFullName() + "." + trialNumber;
		ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
		series.setXSeries(xValues);
		series.setYSeries(yValues);
		series.setAntialias(SWT.ON);
		series.setSymbolType(PlotSymbolType.NONE);
		if(showSamplesButton.getSelection()) series.setSymbolType(PlotSymbolType.CIRCLE);
//		Byte index = getSeriesIndex(series);
//		series.setLineColor(ColorUtil.getColor(index));
		series.setLineWidth(3);
	}
	
//	private Byte getSeriesIndex(ILineSeries series) {
//		ISeries[] seriesArray = chart.getSeriesSet().getSeries();
//		for (int i = 0; i < seriesArray.length; i++) {
//			if(series == seriesArray[i]) return (byte) i;
//		}
//		return 0;
//	}
	
	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new HashSet<Integer>();
		ISeries[] series = chart.getSeriesSet().getSeries();
		for (ISeries aSeries : series) {
			String[] segments = aSeries.getId().split("\\.");
			trials.add(Integer.parseInt(segments[segments.length - 1]));
		}
		return trials;
	}

	private void removeAllSeries() {
		ISeries[] series = chart.getSeriesSet().getSeries();
		for (ISeries aSeries : series) {
			chart.getSeriesSet().deleteSeries(aSeries.getId());
		}
	}
	
	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		String seriesID = channelEditor.getChannel().getFullName() + "." + trialNumber;
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
				trialsListViewer.getList().showSelection();
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

	public void updateMarkersGroup(String markersGroupLabel, Object... parameters) {
		markersGroupComboViewer.refresh();
		markerXValueLabel.setText(DocometreMessages.NotAvailable_Label);
		markerYValueLabel.setText(DocometreMessages.NotAvailable_Label);
		labelNbMarkers.setText(DocometreMessages.NotAvailable_Label);
		if(parameters != null) {
			if(parameters.length == 3) {
				int trialNumber = (int)parameters[0]; 
				double x = (double)parameters[1]; 
				double y = (double)parameters[2];
				for(int n = 0; n < markersGroupComboViewer.getCombo().getItemCount(); n++) {
					double[] element = (double[]) markersGroupComboViewer.getElementAt(n);
					boolean test = element[0] == trialNumber && element[1] == x && element[2] == y;
					if(test) {
						markersGroupComboViewer.setSelection(new StructuredSelection(element));
						break;
					}
				}
			}
		}
	}
	
	public void update() {
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(channelEditor.getChannel());
		sfLabel.setText(Double.toString(sf));
		updateTrialsValuesHandler();
		markersGroupsComboViewer.refresh();
		markersGroupComboViewer.refresh();
		featuresComboViewer.refresh();
		refreshChart();
//		chart.redraw();
//		markersGroupComboViewer.setInput(channelEditor.getChannel());
//		featuresComboViewer.setInput(channelEditor.getChannel());
	}


	@SuppressWarnings("unchecked")
	private void refreshChart() {
		List<Integer> selectedTrialsNumbers = trialsListViewer.getStructuredSelection().toList();
		// Remove series from chart
		for (Integer trialNumberInChart : selectedTrialsNumbers) {
			removeSeriesFromChart(trialNumberInChart);
		}
			
		// Add series again
		for (Integer selectedTrialNumber : selectedTrialsNumbers) {
			addSeriesToChart(selectedTrialNumber);
		}
		updateSeriesColorsHandler();
		if(!keepScaleButton.getSelection()) chart.getAxisSet().adjustRange();
		chart.redraw();
	}
	
}
