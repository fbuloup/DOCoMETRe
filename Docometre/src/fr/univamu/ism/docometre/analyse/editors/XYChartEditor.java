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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.extensions.charts.ChartPropertiesListener;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;
import org.eclipse.swtchart.extensions.charts.ZoomListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class XYChartEditor extends EditorPart implements ISelectionChangedListener, IMarkersManager, ZoomListener, TrialsEditor, ChartPropertiesListener, Chart2D3DBehaviour {
	
	public static String ID = "Docometre.XYChartEditor";
	
	
	private class CategoriesChangeListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			HashSet<Integer> trialsToSelect = new HashSet<>();
			IStructuredSelection structuredSelection = (IStructuredSelection) categoriesListViewer.getSelection();
			Object[] selections = structuredSelection.toArray();
			for (Object selection : selections) {
				Channel category = (Channel)selection;
				Integer[] trials = MathEngineFactory.getMathEngine().getTrialsListForCategory(category);
				trialsToSelect.addAll(Arrays.asList(trials));
			}
			IStructuredSelection selection = new StructuredSelection(trialsToSelect.toArray());
			trialsListViewer.setSelection(selection);
		}
		
	}
	
	private XYChart xyChartData;
	private SashForm container;
	private ListViewer trialsListViewer;
	private InteractiveChart chart;
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Spinner frontCutSpinner;
	private Spinner endCutSpinner;
	private boolean dirty;
	private Button autoScaleButton;
	private Group scaleValuesGroup;
	private Button useSameColorButton;
	private ListViewer categoriesListViewer;
	private CategoriesChangeListener categoriesChangeListener;
	private CTabFolder trialsCategoriesTabFolder;
	private CTabItem trialsTabItem;
	private CTabItem categoriesTabItem;

	public XYChartEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IResource xyChartFile = ObjectsController.getResourceForObject(xyChartData);
		ObjectsController.serialize((IFile) xyChartFile, xyChartData);
		dirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
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
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);
		
		if(!xyChartData.initialize()) {
			Label errorLabel = new Label(container, SWT.BORDER);
			errorLabel.setText(DocometreMessages.SomethingWentWrong);
			errorLabel.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			container.setSashWidth(0);
			container.setWeights(new int[] {100});
			container.setFocus();
			return;
		}
		
		chart = new InteractiveChart(container, SWT.BORDER);
		chart.setBackground(xyChartData.getBackGroundColor());
		chart.getPlotArea().setBackground(xyChartData.getPlotAreaBackGroundColor());
		new MarkersManager(this);
		chart.setShowCursor(false);
		chart.setShowMarker(false);
		chart.getLegend().setPosition(SWT.BOTTOM);
		chart.getLegend().setBackground(xyChartData.getLegendBackGroundColor());
		chart.getLegend().setForeground(xyChartData.getLegendForeGroundColor());
		chart.getLegend().setVisible(xyChartData.isLegendVisible());
		chart.getAxisSet().getXAxis(0).getTick().setVisible(xyChartData.isXAxisVisible());
		chart.getAxisSet().getYAxis(0).getTick().setVisible(xyChartData.isYAxisVisible());
		chart.getAxisSet().getXAxis(0).getTick().setForeground(xyChartData.getXAxisForeGroundColor());
		chart.getAxisSet().getYAxis(0).getTick().setForeground(xyChartData.getYAxisForeGroundColor());
		chart.setSelectionRectangleColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
		chart.getAxisSet().getXAxes()[0].getTitle().setVisible(false);
		chart.getAxisSet().getXAxis(0).getGrid().setStyle(xyChartData.getXAxisGridStyle());
		chart.getAxisSet().getYAxis(0).getGrid().setStyle(xyChartData.getYAxisGridStyle());
		chart.getAxisSet().getXAxis(0).getGrid().setForeground(xyChartData.getXAxisGridColor());
		chart.getAxisSet().getYAxis(0).getGrid().setForeground(xyChartData.getYAxisGridColor());
		
		chart.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				postZoomUpdate();
			}
		});
		chart.addZoomListener(this);
		chart.addPropertiesListener(this);
		
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
						Channel xSignal = (Channel)items[0];
						Channel ySignal = (Channel)items[1];
						xyChartData.addCurve(xSignal, ySignal);
						refreshTrialsListFrontEndCutsCategories();
						setDirty(true);
					}
				 }
			}
		});
		
		Composite container2 = new Composite(container, SWT.BORDER);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 1;
		gl.marginWidth = 1;
		gl.verticalSpacing = 1;
		container2.setLayout(gl);
		container2.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		trialsCategoriesTabFolder = new CTabFolder(container2, SWT.BOTTOM | SWT.FLAT | SWT.BORDER | SWT.MULTI);
		trialsCategoriesTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		trialsTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
		trialsTabItem.setText(DocometreMessages.TrialsGroupLabel);
		
		categoriesTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
		categoriesTabItem.setText(DocometreMessages.Categories);
		
		createTrialsTabItem(trialsCategoriesTabFolder, trialsTabItem, container2);
		
		createCategoriesTabItem(trialsCategoriesTabFolder, categoriesTabItem, container2);
		
		trialsCategoriesTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(trialsTabItem == trialsCategoriesTabFolder.getSelection()) categoriesListViewer.removeSelectionChangedListener(categoriesChangeListener);
				else categoriesListViewer.addSelectionChangedListener(categoriesChangeListener);
			}
		});
		

	}
	
	private void createCategoriesTabItem(CTabFolder trialsCategoriesTabFolder, CTabItem categoriesTabItem, Composite container2) {
		categoriesListViewer = new ListViewer(trialsCategoriesTabFolder, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		categoriesListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		categoriesListViewer.setContentProvider(new ArrayContentProvider());
		categoriesListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof Channel)) return "??";
				Channel channel = (Channel)element;
				Integer[] trialsList = MathEngineFactory.getMathEngine().getTrialsListForCategory(channel);
				String trialsListString = Arrays.toString(trialsList);
				return element.toString() + " " + trialsListString;
			}
		});
		categoriesListViewer.setComparator(new ViewerComparator());
		categoriesChangeListener  = new CategoriesChangeListener();
		categoriesTabItem.setControl(categoriesListViewer.getList());
		
		updateCategories();
		
	}
	
	private void updateCategories() {
		if(categoriesListViewer == null) return;
		Object[] channelsTuple = xyChartData.getChannels().toArray();
		HashSet<Channel> allCategories = new HashSet<Channel>();
		for (Object channelTuple : channelsTuple) {
			if(!(channelTuple instanceof Channel[])) continue;
			Channel[] channels = (Channel[])channelTuple;
			String fullXChannelName = channels[0].getFullName();//seriesID.split("\\(")[0];
			String fullSubjectName = fullXChannelName.replaceAll("\\.\\w+$", "");
			IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(fullSubjectName.split("\\.")[1]);
			Channel[] categories = MathEngineFactory.getMathEngine().getCategories(subject);
			allCategories.addAll(Arrays.asList(categories));
		}
		categoriesListViewer.setInput(allCategories.toArray());
	}

	private void createTrialsTabItem(CTabFolder trialsCategoriesTabFolder, CTabItem trialsTabItem, Composite container2) {
		
		trialsListViewer = new ListViewer(trialsCategoriesTabFolder, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				String category = "";
				Set<String> keys = xyChartData.getCurvesIDs();
				for (String key : keys) {
					Channel[] channels = xyChartData.getXYChannels(key);
					category = MathEngineFactory.getMathEngine().getCategoryForTrialNumber(channels[0], Integer.parseInt(trial));
					break;
				}
				return DocometreMessages.Trial + trial + ("".equals(category)?"":" [" + category + "]");
			}
		});
		trialsListViewer.addSelectionChangedListener(this);
		trialsTabItem.setControl(trialsListViewer.getList());
		trialsCategoriesTabFolder.setSelection(trialsTabItem);
		
		// Graphical Front End cuts
		Group frontEndCutValuesGroup = new Group(container2, SWT.NONE);
		frontEndCutValuesGroup.setText(DocometreMessages.GraphicalCutsTitle);
		frontEndCutValuesGroup.setLayout(new GridLayout(2, false));
		frontEndCutValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label frontCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		frontCutLabel.setText(DocometreMessages.FrontCutLabel);
		frontCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frontCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
		frontCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		frontCutSpinner.setMaximum(1000000000);
		frontCutSpinner.setSelection(xyChartData.getFrontCut());
		frontCutSpinner.setData("frontCut");
		FrontEndCutsHandler frontCutHandler = new FrontEndCutsHandler(frontCutSpinner, this);
		frontCutSpinner.addMouseWheelListener(frontCutHandler);
		frontCutSpinner.addTraverseListener(frontCutHandler);
		frontCutSpinner.addSelectionListener(frontCutHandler);
		
		Label endCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
		endCutLabel.setText(DocometreMessages.EndCutLabel);
		endCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		endCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
		endCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		endCutSpinner.setMaximum(1000000000);
		endCutSpinner.setSelection(xyChartData.getEndCut());
		endCutSpinner.setData("endCut");
		FrontEndCutsHandler endCutHandler = new FrontEndCutsHandler(endCutSpinner, this);
		endCutSpinner.addMouseWheelListener(endCutHandler);
		endCutSpinner.addTraverseListener(endCutHandler);
		endCutSpinner.addSelectionListener(endCutHandler);
		
		// Scales
		scaleValuesGroup = new Group(container2, SWT.NONE);
		scaleValuesGroup.setText(DocometreMessages.ScaleValueTitle);
		scaleValuesGroup.setLayout(new GridLayout(4, false));
		scaleValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		scaleValuesGroup.setEnabled(!xyChartData.isAutoScale());
		
		Label xMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMinLabel.setText("X min. :");
		xMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMinText = new Text(scaleValuesGroup, SWT.BORDER);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMinText.setData("xMin");
		xMinText.addTraverseListener(new RangeHandler(xMinText, this));
		xMinText.setToolTipText(DocometreMessages.PressEnter);
		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMaxLabel.setText("X max. :");
		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMaxText.setData("xMax");
		xMaxText.addTraverseListener(new RangeHandler(xMaxText, this));
		xMaxText.setToolTipText(DocometreMessages.PressEnter);

		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMinLabel.setText("Y min. :");
		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMinText.setData("yMin");
		yMinText.addTraverseListener(new RangeHandler(yMinText, this));
		yMinText.setToolTipText(DocometreMessages.PressEnter);
		
		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMaxLabel.setText("Y max. :");
		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMaxText.setData("yMax");
		yMaxText.addTraverseListener(new RangeHandler(yMaxText, this));
		yMaxText.setToolTipText(DocometreMessages.PressEnter);
		
		Composite bottomContainer = new Composite(container2, SWT.NONE);
		bottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout();
		gl.numColumns = 3;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 0;
		gl.marginRight = 0;
		bottomContainer.setLayout(gl);
		
		Button showMarkersButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
		showMarkersButton.setSelection(xyChartData.isShowMarkers());
		showMarkersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkers = showMarkersButton.getSelection();
				xyChartData.setShowMarkers(showMarkers);
				chart.redraw();
				setDirty(true);
			}
		});
		
		Label sizeLabel = new Label(bottomContainer, SWT.NONE);
		sizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
		sizeLabel.setText(DocometreMessages.MarkersSizeTitle);
		Spinner sizeSpinner = new Spinner(bottomContainer, SWT.BORDER);
		sizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		sizeSpinner.setMaximum(10);
		sizeSpinner.setMinimum(3);
		sizeSpinner.setSelection(xyChartData.getMarkersSize());
		sizeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = sizeSpinner.getSelection() + e.count;
				sizeSpinner.setSelection(value);
				xyChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.redraw();
				setDirty(true);
			}
		});
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setMarkersSize(sizeSpinner.getSelection());
				chart.redraw();
				setDirty(true);
			}
		});
		
		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
		showMarkersLabelsButton.setSelection(xyChartData.isShowMarkersLabels());
		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
				xyChartData.setShowMarkersLabels(showMarkersLabels);
				chart.redraw();
				setDirty(true);
			}
		});
		
		Composite bottomContainer2 = new Composite(container2, SWT.NONE);
		bottomContainer2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout();
		gl.numColumns = 1;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginBottom = 5;
		gl.marginRight = 5;
		bottomContainer2.setLayout(gl);
		
		autoScaleButton = new Button(bottomContainer2, SWT.CHECK);
		autoScaleButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		autoScaleButton.setText(DocometreMessages.AutoScale_Title);
		autoScaleButton.setSelection(xyChartData.isAutoScale());
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean autoScale = autoScaleButton.getSelection();
				xyChartData.setAutoScale(autoScale);
				scaleValuesGroup.setEnabled(!autoScale); 
				if(autoScale) {
					chart.getAxisSet().adjustRange();
					updateRange();
					chart.redraw();
				}
				setDirty(true);
			}
		});
		
		useSameColorButton = new Button(bottomContainer2, SWT.CHECK);
		useSameColorButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.setSelection(xyChartData.isUseSameColorForSameCategory());
		useSameColorButton.setToolTipText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyChartData.setUseSameColorForSameCategory(useSameColorButton.getSelection());
				updateSeriesColorsHandler();
				setDirty(true);
			}
		});
		
		container.setSashWidth(3);
		container.setWeights(new int[] {75, 25});
		
		refreshTrialsListFrontEndCutsCategories();
		trialsListViewer.setSelection(new StructuredSelection(xyChartData.getSelectedTrialsNumbers()));
		setDirty(false);
		
	}
	
	protected void updateSeriesColorsHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean sameColor = xyChartData.isUseSameColorForSameCategory();
		String[] seriesIDs = getSeriesIDs();
		ArrayList<String> categories = new ArrayList<>();
		for (String seriesID : seriesIDs) {
			ISeries series = chart.getSeriesSet().getSeries(seriesID);
			if(sameColor) {
				try {
					int seriesIDTrial = Integer.parseInt(seriesID.split("\\.")[seriesID.split("\\.").length - 1]); 
					String fullYChannelName = seriesID.split("\\(")[0];
					String fullSubjectName = fullYChannelName.replaceAll("\\.\\w+$", "");
					IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(fullSubjectName.split("\\.")[1]);
					ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					Channel channel = channelsContainer.getChannelFromName(fullYChannelName);
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
		}
//		chart.redraw();
	}
	
	@Override
	public void refreshTrialsListFrontEndCutsCategories() {
		updateCategories();
		if(xyChartData.getNbCurves() == 0) {
			trialsListViewer.setInput(null);
			return;
		}
		Collection<Channel[]> xyChannels = xyChartData.getChannels();
		int nbTrials = 0;
		int baseFrontCut = Integer.MAX_VALUE;
		int baseEndCut = 0;
		for (Channel[] xyChannel : xyChannels) {
			Channel xChannel = xyChannel[0];
			Channel yChannel = xyChannel[1];
			nbTrials = Math.max(MathEngineFactory.getMathEngine().getTrialsNumber(xChannel), nbTrials);
			baseFrontCut = Math.min(MathEngineFactory.getMathEngine().getFrontCut(xChannel, 1), baseFrontCut);
			baseEndCut = Math.max(MathEngineFactory.getMathEngine().getEndCut(yChannel, 1), baseEndCut);
		}
		Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
		trialsListViewer.setInput(trials);
		
		frontCutSpinner.setMinimum(baseFrontCut);
		frontCutSpinner.setMaximum(baseEndCut);
		String message = NLS.bind(DocometreMessages.PressEnterFrontCut, baseFrontCut, baseEndCut);
		frontCutSpinner.setToolTipText(message);
		
		endCutSpinner.setMinimum(baseFrontCut);
		endCutSpinner.setMaximum(baseEndCut);
		message = NLS.bind(DocometreMessages.PressEnterEndCut, baseFrontCut, baseEndCut);
		endCutSpinner.setToolTipText(message);
		
		if(xyChartData.getFrontCut() == -1 && xyChartData.getEndCut() == -1) {
			xyChartData.setFrontCut(baseFrontCut);
			xyChartData.setEndCut(baseEndCut);
			endCutSpinner.setSelection(baseEndCut);
		}
		

	}
	
	@Override
	public String[] getSeriesIDs() {
		ISeries[] series = chart.getSeriesSet().getSeries();
		Set<String> ids = new HashSet<>();
		for (ISeries iSeries : series) {
			String id = iSeries.getId();
//			id = id.replaceAll("\\.\\d+$", "");
			ids.add(id);
		}
		return ids.toArray(new String[ids.size()]);
	}
	
	@Override
	public void updateFrontEndCutsChartHandler() {
		int value = frontCutSpinner.getSelection();
		xyChartData.setFrontCut(value);
		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setFrontCut(value);
		value = endCutSpinner.getSelection();
		xyChartData.setEndCut(value);
		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setEndCut(value);
		chart.redraw();
	}

	@Override
	public void setFocus() {
		if(chart != null && !chart.isDisposed()) chart.setFocus();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if(trialsCategoriesTabFolder.getSelection() == trialsTabItem && categoriesListViewer != null) {
			categoriesListViewer.setSelection(StructuredSelection.EMPTY);
			categoriesListViewer.refresh();
		}
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
			xyChartData.setSelectedTrialsNumbers(trialsListViewer.getStructuredSelection().toList());
		}
		
		if(xyChartData.isAutoScale()) {
			chart.getAxisSet().adjustRange();
			updateRange();
		}
		else {
			chart.getAxisSet().getXAxes()[0].setRange(new Range(xyChartData.getxMin(), xyChartData.getxMax()));
			chart.getAxisSet().getYAxes()[0].setRange(new Range(xyChartData.getyMin(), xyChartData.getyMax()));
		}
		trialsListViewer.refresh();
		updateSeriesColorsHandler();
		chart.redraw();
		setDirty(true);
	}
	
	private void updateRange() {
		xyChartData.setxMin(chart.getAxisSet().getXAxes()[0].getRange().lower);
		xyChartData.setxMax(chart.getAxisSet().getXAxes()[0].getRange().upper);
		xyChartData.setyMin(chart.getAxisSet().getYAxes()[0].getRange().lower);
		xyChartData.setyMax(chart.getAxisSet().getYAxes()[0].getRange().upper);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
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
		ArrayList<Integer> trialsList = new ArrayList<>(trials);
		Collections.sort(trialsList);
		return new HashSet<>(trialsList);
	}
	
	private void removeSeriesFromChart(Integer trialNumber) {
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			chart.removeSeries(seriesID);
		}
	}
	
	private void addSeriesToChart(Integer trialNumber) {
		// Get x and Y values for this signal and trial
		Set<String> keys = xyChartData.getCurvesIDs();
		for (String key : keys) {
			Channel[] channels = xyChartData.getXYChannels(key);
			double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[0], trialNumber);
			double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[1], trialNumber);
			// 
			if(yValues == null || xValues == null || yValues.length == 0 || xValues.length == 0) return;
			// Add Series
			String seriesID = key + "." + trialNumber;
			ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesID);
			series.setXSeries(xValues);
			series.setYSeries(yValues);
			int frontCut = xyChartData.getFrontCut();
			int endCut = xyChartData.getEndCut();
			series.setBaseFrontCut(frontCutSpinner.getMinimum());
			series.setFrontCut(frontCut);
			series.setEndCut(endCut);
			series.setAntialias(SWT.ON);
			series.setSymbolType(PlotSymbolType.NONE);
			series.setLineWidth(3);
		}
//		// refresh Series Colors
//		updateSeriesColorsHandler();
//		ISeries[] seriesList = chart.getSeriesSet().getSeries();
//		byte i = 0;
//		for (ISeries series : seriesList) {
//			((ILineSeries)series).setLineColor(ColorUtil.getColor(i));
//			i++;
//		}
	}
	
	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			if(chart.getSeriesSet().getSeries(seriesID) != null) return true;
		} 
		return false;
	}
	
	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}
	
	@Override
	public void dispose() {
		ObjectsController.removeHandle(xyChartData);
		super.dispose();
	}

	@Override
	public InteractiveChart getChart() {
		return chart;
	}

	@Override
	public void updateMarkersGroup(String markersGroupLabel) {
		// TODO Nothing
	}

//	public XYChart getXYChartData() {
//		return xyChartData;
//	}

	@Override
	public void postZoomUpdate() {
		xyChartData.setxMin(chart.getAxisSet().getXAxes()[0].getRange().lower);
		xyChartData.setxMax(chart.getAxisSet().getXAxes()[0].getRange().upper);
		xyChartData.setyMin(chart.getAxisSet().getYAxes()[0].getRange().lower);
		xyChartData.setyMax(chart.getAxisSet().getYAxes()[0].getRange().upper);
		xMinText.setText(Double.toString(xyChartData.getxMin()));
		xMaxText.setText(Double.toString(xyChartData.getxMax()));
		yMinText.setText(Double.toString(xyChartData.getyMin()));
		yMaxText.setText(Double.toString(xyChartData.getyMax()));
		xyChartData.setAutoScale(false);
		autoScaleButton.setSelection(false);
		scaleValuesGroup.setEnabled(true);
		setDirty(true);
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

	@Override
	public void updateChartProperties() {
		xyChartData.setBackGroundColor(chart.getBackground());
		chart.getParent().getParent().setBackground(chart.getBackground());
		xyChartData.setPlotAreaBackGroundColor(chart.getBackgroundInPlotArea());
		xyChartData.setLegendBackGroundColor(chart.getLegend().getBackground());
		xyChartData.setLegendForeGroundColor(chart.getLegend().getForeground());
		xyChartData.setLegendVisible(chart.getLegend().isVisible());
		xyChartData.setXAxisForeGroundColor(chart.getAxisSet().getXAxis(0).getTick().getForeground());
		xyChartData.setYAxisForeGroundColor(chart.getAxisSet().getYAxis(0).getTick().getForeground());
		xyChartData.setXAxisVisibility(chart.getAxisSet().getXAxis(0).getTick().isVisible());
		xyChartData.setYAxisVisibility(chart.getAxisSet().getYAxis(0).getTick().isVisible());
		xyChartData.setXAxisGridColor(chart.getAxisSet().getXAxis(0).getGrid().getForeground());
		xyChartData.setXAxisGridStyle(chart.getAxisSet().getXAxis(0).getGrid().getStyle().name());
		xyChartData.setYAxisGridColor(chart.getAxisSet().getYAxis(0).getGrid().getForeground());
		xyChartData.setYAxisGridStyle(chart.getAxisSet().getYAxis(0).getGrid().getStyle().name());
		setDirty(true);
	}

	@Override
	public XYChart getChartData() {
		return xyChartData;
	}

	@Override
	public void redraw() {
		chart.redraw();
	}

	@Override
	public void updateXAxisRange(double min, double max) {
		chart.getAxisSet().getXAxis(0).setRange(new Range(min, max));
	}

	@Override
	public void updateYAxisRange(double min, double max) {
		chart.getAxisSet().getYAxis(0).setRange(new Range(min, max));
	}

	@Override
	public void updateZAxisRange(double min, double max) {
		// this not a 3D chart, nothing to do.
	}

	@Override
	public void removeSeries(String seriesID) {
		chart.removeSeries(seriesID);
		xyChartData.removeCurve(seriesID);
	}
	
}
