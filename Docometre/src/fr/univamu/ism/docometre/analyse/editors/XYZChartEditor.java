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
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtchart.extensions.charts.ChartPropertiesListener;
import org.eclipse.swtchart.extensions.charts.ZoomListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.Settings;
import org.jzy3d.chart.factories.CanvasNewtSWT;
import org.jzy3d.chart.factories.SWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.events.IViewPointChangedListener;
import org.jzy3d.events.ViewPointChangedEvent;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.painters.Font;
import org.jzy3d.plot3d.primitives.Drawable;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewBoundMode;
import org.jzy3d.plot3d.text.DrawableTextWrapper;
import org.jzy3d.plot3d.text.ITextRenderer;
import org.jzy3d.plot3d.text.align.Horizontal;
import org.jzy3d.plot3d.text.align.Vertical;
import org.jzy3d.plot3d.text.renderers.TextBillboardRenderer;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.swt.NewtCanvasSWT;

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
import fr.univamu.ism.docometre.analyse.datamodel.XYZChart;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class XYZChartEditor extends EditorPart implements ISelectionChangedListener, /*IMarkersManager,*/ ZoomListener, TrialsEditor, ChartPropertiesListener, Chart2D3DBehaviour, IViewPointChangedListener, MouseListener {
	
	public static String ID = "Docometre.XYZChartEditor";
	
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
	
	private XYZChart xyzChartData;
	private SashForm container;
	private ListViewer trialsListViewer;
	private Text xMinText;
	private Text xMaxText;
	private Text yMinText;
	private Text yMaxText;
	private Text zMinText;
	private Text zMaxText;
	private Spinner frontCutSpinner;
	private Spinner endCutSpinner;
	private boolean dirty;
	private Button autoScaleButton;
	private Group scaleValuesGroup;
	private Button useSameColorButton;
	private Composite chartContainer;
	private Chart chart;
	private BoundingBox3d bounds;
	private CategoriesChangeListener categoriesChangeListener;
	
	CanvasNewtSWT canvas;
	NewtCanvasSWT newtCanvasSWT;
	Window newtWindow;
//	private Composite container2;

	private CTabFolder trialsCategoriesTabFolder;

	private CTabItem trialsTabItem;

	private CTabItem categoriesTabItem;

	private ListViewer categoriesListViewer;
	

	public XYZChartEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IResource xyChartFile = ObjectsController.getResourceForObject(xyzChartData);
		ObjectsController.serialize((IFile) xyChartFile, xyzChartData);
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
		xyzChartData = ((XYZChart)((ResourceEditorInput)input).getObject());
		IResource resource = ObjectsController.getResourceForObject(xyzChartData);
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

	public Composite getChartContainer() {
		return chartContainer;
	}

	@Override
	public void createPartControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);

		if(!xyzChartData.initialize()) {
			Label errorLabel = new Label(container, SWT.BORDER);
			errorLabel.setText(DocometreMessages.SomethingWentWrong);
			errorLabel.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			container.setSashWidth(0);
			container.setWeights(new int[] {100});
			container.setFocus();
			return;
		}
		chartContainer = new Composite(container, SWT.BORDER);
		chartContainer.setLayout(new FillLayout());
		
		Settings.getInstance().setHardwareAccelerated(true);
		chart = SWTChartFactory.chart(chartContainer, Quality.Nicest());
		chart.getView().setViewPoint(xyzChartData.getViewPoint());
		chart.getView().addViewPointChangedListener(XYZChartEditor.this);
		chart.black();
		ChartLauncher.openChart(chart);
		((CanvasNewtSWT) chart.getCanvas()).addMouseListener(this);
		
		getSite().getPage().addPartListener(new IPartListener2() {
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == XYZChartEditor.this) {
					if(chart != null)/* && chart.getCanvas() != null && !((CanvasNewtSWT) chart.getCanvas()).isDisposed())*/ {
						chart.dispose();
						chart = null;
					}
				}
			}
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == XYZChartEditor.this) {
					if(chart != null) {
						chartContainer.layout();
						return;
					}
					chart = SWTChartFactory.chart(chartContainer, Quality.Nicest());
					chart.getView().setViewPoint(xyzChartData.getViewPoint());
					chart.getView().addViewPointChangedListener(XYZChartEditor.this);
					chart.black();
					ChartLauncher.openChart(chart);
					chartContainer.layout();
					boolean wasDirty = XYZChartEditor.this.isDirty();
					SelectionChangedEvent event = new SelectionChangedEvent(trialsListViewer, trialsListViewer.getSelection());
					XYZChartEditor.this.selectionChanged(event);
					if(!wasDirty) XYZChartEditor.this.setDirty(false);
					((CanvasNewtSWT) chart.getCanvas()).addMouseListener(XYZChartEditor.this);
					
					canvas = ((CanvasNewtSWT) chart.getCanvas());
					newtCanvasSWT = canvas.getCanvas();
					newtWindow = ((CanvasNewtSWT) chart.getCanvas()).getCanvas().getNEWTChild();
				}
			}
		});
		
//		chart = new InteractiveChart(container, SWT.NONE);
//		chart.setBackground(xyzChartData.getBackGroundColor());
//		chart.getPlotArea().setBackground(xyzChartData.getPlotAreaBackGroundColor());
//		innerContainer.setBackground(xyzChartData.getBackGroundColor());
//		new MarkersManager(this);
//		chart.setShowCursor(false);
//		chart.setShowMarker(false);
//		chart.getLegend().setPosition(SWT.BOTTOM);
//		chart.getLegend().setBackground(xyzChartData.getLegendBackGroundColor());
//		chart.getLegend().setForeground(xyzChartData.getLegendForeGroundColor());
//		chart.getLegend().setVisible(xyzChartData.isLegendVisible());
//		chart.getAxisSet().getXAxis(0).getTick().setVisible(xyzChartData.isXAxisVisible());
//		chart.getAxisSet().getYAxis(0).getTick().setVisible(xyzChartData.isYAxisVisible());
//		chart.getAxisSet().getXAxis(0).getTick().setForeground(xyzChartData.getXAxisForeGroundColor());
//		chart.getAxisSet().getYAxis(0).getTick().setForeground(xyzChartData.getYAxisForeGroundColor());
//		chart.setSelectionRectangleColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
//		chart.getTitle().setVisible(false);
//		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
//		chart.getAxisSet().getXAxes()[0].getTitle().setVisible(false);
//		chart.getAxisSet().getXAxis(0).getGrid().setStyle(xyzChartData.getXAxisGridStyle());
//		chart.getAxisSet().getYAxis(0).getGrid().setStyle(xyzChartData.getYAxisGridStyle());
//		chart.getAxisSet().getXAxis(0).getGrid().setForeground(xyzChartData.getXAxisGridColor());
//		chart.getAxisSet().getYAxis(0).getGrid().setForeground(xyzChartData.getYAxisGridColor());
//		
//		chart.addMouseWheelListener(new MouseWheelListener() {
//			@Override
//			public void mouseScrolled(MouseEvent e) {
//				postZoomUpdate();
//			}
//		});
//		chart.addZoomListener(this);
//		chart.addPropertiesListener(this);
//		
//		// Allow data to be copied or moved to the drop target
//		int operations = DND.DROP_COPY;
//		DropTarget target = new DropTarget(chart, operations);

//		// Receive data in Text or File format
//		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
//		target.setTransfer(new Transfer[] { transfer });
//
//		// Add drag/drop support
//		target.addDropListener(new DropTargetListener() {
//			public void dragEnter(DropTargetEvent event) {
//				if ((event.operations & DND.DROP_COPY) != 0) event.detail = DND.DROP_COPY;
//			}
//
//			public void dragOver(DropTargetEvent event) {
//				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
//			}
//
//			public void dragOperationChanged(DropTargetEvent event) {
//
//			}
//
//			public void dragLeave(DropTargetEvent event) {
//			}
//
//			public void dropAccept(DropTargetEvent event) {
//			}
//
//			public void drop(DropTargetEvent event) {
//				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
//					ISelection selection = transfer.getSelection();
//					if(selection instanceof IStructuredSelection) {
//						Object[] items = ((IStructuredSelection)selection).toArray();
//						Channel xSignal = (Channel)items[0];
//						Channel ySignal = (Channel)items[1];
//						xyzChartData.addCurve(xSignal, ySignal);
//						refreshTrialsListFrontEndCuts();
//						setDirty(true);
//					}
//				 }
//			}
//		});
		
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
		Object[] channelsTuple = xyzChartData.getChannels().toArray();
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
				Set<String> keys = xyzChartData.getCurvesIDs();
				for (String key : keys) {
					Channel[] channels = xyzChartData.getXYChannels(key);
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
		frontCutSpinner.setSelection(xyzChartData.getFrontCut());
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
		endCutSpinner.setSelection(xyzChartData.getEndCut());
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
		scaleValuesGroup.setEnabled(!xyzChartData.isAutoScale());
		
		Label xMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMinLabel.setText("X min. :");
		xMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMinText = new Text(scaleValuesGroup, SWT.BORDER);
		xMinText.setText(Double.toString(xyzChartData.getxMin()));
		xMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMinText.setData("xMin");
		xMinText.addTraverseListener(new RangeHandler(xMinText, this));
		xMinText.setToolTipText(DocometreMessages.PressEnter);
		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		xMaxLabel.setText("X max. :");
		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		xMaxText.setText(Double.toString(xyzChartData.getxMax()));
		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xMaxText.setData("xMax");
		xMaxText.addTraverseListener(new RangeHandler(xMaxText, this));
		xMaxText.setToolTipText(DocometreMessages.PressEnter);

		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMinLabel.setText("Y min. :");
		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
		yMinText.setText(Double.toString(xyzChartData.getyMin()));
		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMinText.setData("yMin");
		yMinText.addTraverseListener(new RangeHandler(yMinText, this));
		yMinText.setToolTipText(DocometreMessages.PressEnter);
		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		yMaxLabel.setText("Y max. :");
		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		yMaxText.setText(Double.toString(xyzChartData.getyMax()));
		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yMaxText.setData("yMax");
		yMaxText.addTraverseListener(new RangeHandler(yMaxText, this));
		yMaxText.setToolTipText(DocometreMessages.PressEnter);
		
		Label zMinLabel = new Label(scaleValuesGroup, SWT.NONE);
		zMinLabel.setText("Z min. :");
		zMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		zMinText = new Text(scaleValuesGroup, SWT.BORDER);
		zMinText.setText(Double.toString(xyzChartData.getzMin()));
		zMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		zMinText.setData("zMin");
		zMinText.addTraverseListener(new RangeHandler(zMinText, this));
		zMinText.setToolTipText(DocometreMessages.PressEnter);
		Label zMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
		zMaxLabel.setText("Z max. :");
		zMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		zMaxText = new Text(scaleValuesGroup, SWT.BORDER);
		zMaxText.setText(Double.toString(xyzChartData.getzMax()));
		zMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		zMaxText.setData("zMax");
		zMaxText.addTraverseListener(new RangeHandler(zMaxText, this));
		zMaxText.setToolTipText(DocometreMessages.PressEnter);
		
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
		showMarkersButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
		showMarkersButton.setSelection(xyzChartData.isShowMarkers());
		showMarkersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkers = showMarkersButton.getSelection();
				xyzChartData.setShowMarkers(showMarkers);
				List<Drawable> drawables = chart.getScene().getGraph().getAll();
				for (Drawable drawable : drawables) {
					if(drawable instanceof Point) {
						drawable.setDisplayed(xyzChartData.isShowMarkers());
					}
					if(drawable instanceof DrawableTextWrapper) {
						drawable.setDisplayed(xyzChartData.isShowMarkersLabels() && xyzChartData.isShowMarkers());
					}
				}
				setDirty(true);
				redraw();
			}
		});
		
		Label sizeLabel = new Label(bottomContainer, SWT.NONE);
		sizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
		sizeLabel.setText(DocometreMessages.MarkersSizeTitle);
		Spinner sizeSpinner = new Spinner(bottomContainer, SWT.BORDER);
		sizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		sizeSpinner.setMaximum(1000);
		sizeSpinner.setMinimum(3);
		sizeSpinner.setSelection(xyzChartData.getMarkersSize());
		sizeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int value = sizeSpinner.getSelection() + e.count;
				sizeSpinner.setSelection(value);
				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
				List<Drawable> drawables = chart.getScene().getGraph().getAll();
				for (Drawable drawable : drawables) {
					if(drawable instanceof Point) {
						((Point) drawable).setWidth(xyzChartData.getMarkersSize());
					}
				}
				setDirty(true);
				redraw();
			}
		});
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
				List<Drawable> drawables = chart.getScene().getGraph().getAll();
				for (Drawable drawable : drawables) {
					if(drawable instanceof Point) {
						((Point) drawable).setWidth(xyzChartData.getMarkersSize());
					}
				}
				setDirty(true);
				redraw();
			}
		});
		
		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK);
		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
		showMarkersLabelsButton.setSelection(xyzChartData.isShowMarkersLabels());
		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
				xyzChartData.setShowMarkersLabels(showMarkersLabels);
				List<Drawable> drawables = chart.getScene().getGraph().getAll();
				for (Drawable drawable : drawables) {
					if(drawable instanceof DrawableTextWrapper) {
						drawable.setDisplayed(xyzChartData.isShowMarkers()?xyzChartData.isShowMarkersLabels():false);
					}
				}
				setDirty(true);
				redraw();
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
		autoScaleButton.setSelection(xyzChartData.isAutoScale());
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean autoScale = autoScaleButton.getSelection();
				xyzChartData.setAutoScale(autoScale);
				scaleValuesGroup.setEnabled(!autoScale); 
				if(autoScale) {
					chart.getView().setBoundMode(ViewBoundMode.AUTO_FIT);
					updateRange();
				} else {
					chart.getView().setBoundMode(ViewBoundMode.MANUAL);
				}
				setDirty(true);
			}
		});
		autoScaleButton.setEnabled(false);
		
		useSameColorButton = new Button(bottomContainer2, SWT.CHECK);
		useSameColorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
		useSameColorButton.setSelection(xyzChartData.isUseSameColorForSameCategory());
		useSameColorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				xyzChartData.setUseSameColorForSameCategory(useSameColorButton.getSelection());
				updateSeriesColorsHandler();
				setDirty(true);
			}
		});
		
		container.setSashWidth(3);
		container.setWeights(new int[] {80, 20});
		
		refreshTrialsListFrontEndCutsCategories();
		trialsListViewer.setSelection(new StructuredSelection(xyzChartData.getSelectedTrialsNumbers()));
		setDirty(false);
	}

	private LineStrip getLineStripFromID(String id) {
		List<Drawable> drawables = chart.getScene().getGraph().getAll();
		for (Drawable drawable : drawables) {
			if(!(drawable instanceof LineStrip)) continue;
			if(((LineStrip)drawable).getId().equals(id)) return (LineStrip)drawable;
		}
		return null;
	}
	
	protected void updateSeriesColorsHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean sameColor = xyzChartData.isUseSameColorForSameCategory();
		String[] seriesIDs = getSeriesIDs();
		ArrayList<String> categories = new ArrayList<>();
		for (String seriesID : seriesIDs) {
			LineStrip lineStrip = getLineStripFromID(seriesID);
			org.eclipse.swt.graphics.Color color  = ColorUtil.getColor((byte) 0);
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
					color = ColorUtil.getColor((byte) categories.indexOf(category));
					lineStrip.setWireframeColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else {
				String[] trialNumberString = seriesID.split("\\.");
				int trialNumber = Integer.parseInt(trialNumberString[trialNumberString.length - 1]);
				int index = trialsListViewer.getStructuredSelection().toList().indexOf(trialNumber);
				color  = ColorUtil.getColor((byte)index);
				lineStrip.setWireframeColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
			}
			Drawable[] drawables = lineStrip.getMarkersAndLabels();
			for (Drawable drawable : drawables) {
				if(drawable instanceof Point) {
					((Point) drawable).setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
				}
//				if(drawable instanceof DrawableTextWrapper) {
//					((DrawableTextWrapper) drawable).setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
//				}
				
			}
		}

		redraw();
	}
	
	@Override
	public void refreshTrialsListFrontEndCutsCategories() {
		updateCategories();
		if(xyzChartData.getNbCurves() == 0) {
			trialsListViewer.setInput(null);
			return;
		}
		Collection<Channel[]> xyzChannels = xyzChartData.getChannels();
		int nbTrials = 0;
		int baseFrontCut = Integer.MAX_VALUE;
		int baseEndCut = 0;
		for (Channel[] xyzChannel : xyzChannels) {
			Channel xChannel = xyzChannel[0];
			Channel yChannel = xyzChannel[1];
//			Channel zChannel = xyzChannel[2];
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
		
		if(xyzChartData.getFrontCut() == -1 && xyzChartData.getEndCut() == -1) {
			xyzChartData.setFrontCut(baseFrontCut);
			xyzChartData.setEndCut(baseEndCut);
			endCutSpinner.setSelection(baseEndCut);
		}

	}
	
	@Override
	public String[] getSeriesIDs() {
		Set<String> ids = new HashSet<>();
		List<Drawable> lineStrips = chart.getScene().getGraph().getAll();
		for (Drawable lineStrip : lineStrips) {
			if(!(lineStrip instanceof LineStrip)) continue;
			String id = ((LineStrip)lineStrip).getId();
			if(id != null) {
				ids.add(id);
			}
			
		}
		return ids.toArray(new String[ids.size()]);
	}
	
	@Override
	public void updateFrontEndCutsChartHandler() {
		int value = frontCutSpinner.getSelection();
		xyzChartData.setFrontCut(value);
		List<Drawable> drawables = chart.getScene().getGraph().getAll();
		for (Drawable drawable : drawables) {
			if(drawable instanceof LineStrip) ((LineStrip)drawable).setFrontCut(value);
		}
		value = endCutSpinner.getSelection();
		xyzChartData.setEndCut(value);
		for (Drawable drawable : drawables) {
			if(drawable instanceof LineStrip) ((LineStrip)drawable).setEndCut(value);
		}
		redraw();
	}

	@Override
	public void setFocus() {
		if(trialsListViewer != null) trialsListViewer.getList().setFocus();
	}

	private void removeAllSeries() {
		int nbDrawables = chart.getScene().getGraph().getAll().size();
		for (int i = 0; i < nbDrawables; i++) {
			chart.getScene().getGraph().remove(chart.getScene().getGraph().getAll().get(0));
		}
		xyzChartData.setSelectedTrialsNumbers(new ArrayList<>(0));
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
			xyzChartData.setSelectedTrialsNumbers(trialsListViewer.getStructuredSelection().toList());
		}
		
		if(xyzChartData.isAutoScale()) {
			updateRange();
		}
		else {
			Range xRange = new Range((float)xyzChartData.getxMin(), (float)xyzChartData.getxMax());
			Range yRange = new Range((float)xyzChartData.getyMin(), (float)xyzChartData.getyMax());
			Range zRange = new Range((float)xyzChartData.getzMin(), (float)xyzChartData.getzMax());
			BoundingBox3d bounds = new BoundingBox3d(xRange, yRange, zRange);
			chart.getView().getAxis().setAxe(bounds);
		}
		redraw();
		setDirty(true);
	}
	
	private void updateRange() {
		chart.getView().updateBoundsForceUpdate(true);
		BoundingBox3d bounds = chart.getView().getAxis().getBounds();
		xyzChartData.setxMin(bounds.getXmin());
		xyzChartData.setxMax(bounds.getXmax());
		xyzChartData.setyMin(bounds.getYmin());
		xyzChartData.setyMax(bounds.getYmax());
		xyzChartData.setzMin(bounds.getZmin());
		xyzChartData.setzMax(bounds.getZmax());
		xMinText.setText(Double.toString(bounds.getXmin()));
		xMaxText.setText(Double.toString(bounds.getXmax()));
		yMinText.setText(Double.toString(bounds.getYmin()));
		yMaxText.setText(Double.toString(bounds.getYmax()));
		zMinText.setText(Double.toString(bounds.getZmin()));
		zMaxText.setText(Double.toString(bounds.getZmax()));
	}
	
	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new HashSet<Integer>();
		String[] seriesIDs = getSeriesIDs();
		
		for (String seriesID : seriesIDs) {
			String[] segments = seriesID.split("\\.");
			trials.add(Integer.parseInt(segments[segments.length - 1]));
		}
		ArrayList<Integer> trialsList = new ArrayList<>(trials);
		Collections.sort(trialsList);
		return new HashSet<>(trialsList);
	}
	
	private void removeSeriesFromChart(Integer trialNumber) {
		for (String seriesID : xyzChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			List<Drawable> lineStrips = chart.getScene().getGraph().getAll();
			for (int i = 0; i < lineStrips.size(); i++) {
				if(!(lineStrips.get(i) instanceof LineStrip)) continue;
				LineStrip lineStrip = (LineStrip) lineStrips.get(i);
				if(seriesID.equals(((LineStrip)lineStrip).getId())) {
					Drawable[] markers = lineStrip.getMarkersAndLabels();
					for (Drawable marker : markers) {
						chart.getScene().getGraph().remove(marker, false);
					}
					chart.getScene().getGraph().remove(lineStrip, false);
				}
			}
		}
	}
	
	private void addSeriesToChart(Integer trialNumber) {
		// Get x and Y values for this signal and trial
		Set<String> keys = xyzChartData.getCurvesIDs();
		for (String key : keys) {
			Channel[] channels = xyzChartData.getXYZChannels(key);
			double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[0], trialNumber);
			double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[1], trialNumber);
			double[] zValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[2], trialNumber);
			// 
			if(xValues == null || yValues == null || zValues == null || xValues.length == 0 || yValues.length == 0 || zValues.length == 0) return;
			if(xValues.length != yValues.length || xValues.length != zValues.length || yValues.length != zValues.length) return;
			// Add Series
			String seriesID = key + "." + trialNumber;
			
			LineStrip trajectory = new LineStrip(xValues.length);
			trajectory.setId(seriesID);
			for (int i = 0; i < xValues.length; i++) {
				Coord3d coord3d = new Coord3d(xValues[i], yValues[i], zValues[i]);
				trajectory.add(coord3d);
			}
			
			trajectory.setWireframeColor(Color.RED);
			trajectory.setDisplayed(true);
			trajectory.setFaceDisplayed(true);
			trajectory.setWireframeDisplayed(false);
			trajectory.setWireframeWidth(3);
			int frontCut = xyzChartData.getFrontCut();
			int endCut = xyzChartData.getEndCut();
			trajectory.setFrontCut(frontCut);
			trajectory.setBaseFrontCut(frontCutSpinner.getMinimum());
			trajectory.setEndCut(endCut);
			chart.getScene().getGraph().add(trajectory, false);
			// Add markers

			int xBaseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(channels[0], trialNumber);
			int yBaseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(channels[1], trialNumber);
			int zBaseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(channels[2], trialNumber);
			double sf = MathEngineFactory.getMathEngine().getSampleFrequency(channels[0]);
			addMarkers(trajectory, xValues, yValues, zValues, xBaseFrontCut, yBaseFrontCut, zBaseFrontCut, sf);
		}
		// refresh Series Colors
		updateSeriesColorsHandler();
	}
	
	private void addMarkers(LineStrip trajectory, double[] xValues, double[] yValues, double[] zValues, int xBaseFrontCut, int yBaseFrontCut, int zBaseFrontCut, double sf) {
		String id = trajectory.getId();
		String[] idSplitted = id.split("\\.");
		int trialNumber = Integer.parseInt(idSplitted[idSplitted.length - 1]);
		String seriesID = id.replaceAll("\\.\\d+$", "");
		
		Channel[] channels = xyzChartData.getXYZChannels(seriesID);
		Channel xChannel = channels[0];
		Channel yChannel = channels[1];
		Channel zChannel = channels[2];

		addMarkersFromChannel(trajectory, xChannel, trialNumber, sf, xValues, yValues, zValues, xBaseFrontCut, yBaseFrontCut, zBaseFrontCut);
		addMarkersFromChannel(trajectory, yChannel, trialNumber, sf, xValues, yValues, zValues, xBaseFrontCut, yBaseFrontCut, zBaseFrontCut);
		addMarkersFromChannel(trajectory, zChannel, trialNumber, sf, xValues, yValues, zValues, xBaseFrontCut, yBaseFrontCut, zBaseFrontCut);
		
	}
	
	private void addMarkersFromChannel(LineStrip trajectory, Channel channel, int trialNumber, double sf, double[] xValues, double[] yValues, double[] zValues, int xBaseFrontCut, int yBaseFrontCut, int zBaseFrontCut) {
		// Markers from Channel 
		String[] markersGroupsLabels = new String[0]; 
		markersGroupsLabels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(channel);
		for (String markersGroupLabel : markersGroupsLabels) {
			double[][] markers = MathEngineFactory.getMathEngine().getMarkers(markersGroupLabel, channel);
			for (int i = 0; i < markers.length; i++) {
				if(((int)markers[i][0]) == trialNumber) {
					int sampleIndex = (int) (sf * markers[i][1]);
					double xValue = xValues[sampleIndex - xBaseFrontCut];
					double yValue = yValues[sampleIndex - yBaseFrontCut];
					double zValue = zValues[sampleIndex - zBaseFrontCut];
					Coord3d coord3d = new Coord3d(xValue, yValue, zValue);
					Point marker = new Point(coord3d);
					marker.setColor(trajectory.getColor());
					marker.setWidth(xyzChartData.getMarkersSize());
					ITextRenderer textRender = new TextBillboardRenderer();
					DrawableTextWrapper label = new DrawableTextWrapper(markersGroupLabel, coord3d, Color.WHITE, textRender);
					label.setHalign(Horizontal.RIGHT);
					label.setValign(Vertical.TOP);
					label.setDefaultFont(Font.Helvetica_10);
					label.setSceneOffset(new Coord3d(1000, 1000, 1000));
					trajectory.addMarkerAndLabel(new Drawable[] {marker, label});
					chart.getScene().getGraph().add(marker);
					chart.getScene().getGraph().add(label);
				}
			}
		}
	}

	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		String[] seriesIDs = getSeriesIDs();
		for (String seriesID : xyzChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
			if(Arrays.asList(seriesIDs).contains(seriesID)) return true;
		} 
		return false;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}
	
	@Override
	public void dispose() {
		ObjectsController.removeHandle(xyzChartData);
		super.dispose();
	}

//	@Override
//	public InteractiveChart getChart() {
//		return chart;
//	}
//
//	@Override
//	public void updateMarkersGroup(String markersGroupLabel) {
//	}

	@Override
	public void postZoomUpdate() {
//		xyzChartData.setxMin(chart.getAxisSet().getXAxes()[0].getRange().lower);
//		xyzChartData.setxMax(chart.getAxisSet().getXAxes()[0].getRange().upper);
//		xyzChartData.setyMin(chart.getAxisSet().getYAxes()[0].getRange().lower);
//		xyzChartData.setyMax(chart.getAxisSet().getYAxes()[0].getRange().upper);
//		xMinText.setText(Double.toString(xyzChartData.getxMin()));
//		xMaxText.setText(Double.toString(xyzChartData.getxMax()));
//		yMinText.setText(Double.toString(xyzChartData.getyMin()));
//		yMaxText.setText(Double.toString(xyzChartData.getyMax()));
//		xyzChartData.setAutoScale(false);
//		autoScaleButton.setSelection(false);
//		scaleValuesGroup.setEnabled(true);
//		setDirty(true);
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
//		xyzChartData.setBackGroundColor(chart.getBackground());
//		chart.getParent().getParent().setBackground(chart.getBackground());
//		xyzChartData.setPlotAreaBackGroundColor(chart.getBackgroundInPlotArea());
//		xyzChartData.setLegendBackGroundColor(chart.getLegend().getBackground());
//		xyzChartData.setLegendForeGroundColor(chart.getLegend().getForeground());
//		xyzChartData.setLegendVisible(chart.getLegend().isVisible());
//		xyzChartData.setXAxisForeGroundColor(chart.getAxisSet().getXAxis(0).getTick().getForeground());
//		xyzChartData.setYAxisForeGroundColor(chart.getAxisSet().getYAxis(0).getTick().getForeground());
//		xyzChartData.setXAxisVisibility(chart.getAxisSet().getXAxis(0).getTick().isVisible());
//		xyzChartData.setYAxisVisibility(chart.getAxisSet().getYAxis(0).getTick().isVisible());
//		xyzChartData.setXAxisGridColor(chart.getAxisSet().getXAxis(0).getGrid().getForeground());
//		xyzChartData.setXAxisGridStyle(chart.getAxisSet().getXAxis(0).getGrid().getStyle().name());
//		xyzChartData.setYAxisGridColor(chart.getAxisSet().getYAxis(0).getGrid().getForeground());
//		xyzChartData.setYAxisGridStyle(chart.getAxisSet().getYAxis(0).getGrid().getStyle().name());
//		setDirty(true);
	}

	@Override
	public XYChart getChartData() {
		return xyzChartData;
	}

	@Override
	public void redraw() {
		chart.render(); // Is it ok ?
	}

	@Override
	public void updateXAxisRange(double min, double max) {
		bounds = chart.getView().getAxis().getBounds();
		bounds.setXmax((float) max);
		bounds.setXmin((float) min);
		chart.getView().getAxis().setAxe(bounds);
	}

	@Override
	public void updateYAxisRange(double min, double max) {
		bounds = chart.getView().getAxis().getBounds();
		bounds.setYmax((float) max);
		bounds.setYmin((float) min);
		chart.getView().getAxis().setAxe(bounds);
	}

	@Override
	public void updateZAxisRange(double min, double max) {
		bounds = chart.getView().getAxis().getBounds();
		bounds.setZmax((float) max);
		bounds.setZmin((float) min);
		chart.getView().getAxis().setAxe(bounds);
		redraw();
	}

	@Override
	public void removeSeries(String seriesID) {
		List<Drawable> drawables = chart.getScene().getGraph().getAll();
		for (int i = 0; i < drawables.size(); i++) {
			if(drawables.get(i) instanceof LineStrip) {
				LineStrip lineStrip = (LineStrip) drawables.get(i);
				if(seriesID.equals(((LineStrip)lineStrip).getId())) {
					Drawable[] markers = lineStrip.getMarkersAndLabels();
					for (Drawable marker : markers) {
						chart.getScene().getGraph().remove(marker, false);
					}
					chart.getScene().getGraph().remove(lineStrip, false);
				}
			}
			
		}
	}

	public void zoomIn() {
//        double zoomMultiplier = 0.95; 
//		ViewPoint3D viewPt = chart3DPanel.getViewPoint();
//        double minDistance = chart3DPanel.getMinViewingDistance();
//        double maxDistance = minDistance * chart3DPanel.getMaxViewingDistanceMultiplier();
//        double valRho = Math.max(minDistance, Math.min(maxDistance, viewPt.getRho() * zoomMultiplier));
//        chart3DPanel.getViewPoint().setRho(valRho);
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();

		chart.getView().getCamera().setEye(chart.getView().getCamera().getEye().add(new Coord3d(1, 1, 1)));
		redraw();
      setDirty(true);
				
	}

	public void zoomOut() {
//        double zoomMultiplier = 10.0 / 9.5; 
//		ViewPoint3D viewPt = chart3DPanel.getViewPoint();
//        double minDistance = chart3DPanel.getMinViewingDistance();
//        double maxDistance = minDistance * chart3DPanel.getMaxViewingDistanceMultiplier();
//        double valRho = Math.max(minDistance, Math.min(maxDistance, viewPt.getRho() * zoomMultiplier));
//        chart3DPanel.getViewPoint().setRho(valRho);
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();
//		Coord3d eye = chart.getView().getCamera().getEye();
//		Coord3d target = chart.getView().getCamera().getTarget();
//		Coord3d up = chart.getView().getCamera().getUp();
//		Coord3d scale = chart.getView().getCamera().getScale().mul(new Coord3d(0.5, 0.5, 0.5));
//		chart.getView().getCamera().setPosition(eye, target, up, scale);
//		redraw();
//        setDirty(true);
	}

	public void zoomToFit() {
//		chart3DPanel.zoomToFit();
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//		chart3DPanel.update();
//        setDirty(true);
	}

	public void up() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d.addSelf(0.0f, -0.01f, 0f);
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	public void down() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d.addSelf(0.0f, 0.01f, 0f);
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	public void right() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d.addSelf(-0.01f, 0f, 0f);
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	public void left() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d.addSelf(0.01f, 0f, 0f);
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	public void turnLeft() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d = coord3d.rotate(-0.1f, Coord3d.IDENTITY.normalizeTo(1));
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	public void turnRight() {
		Coord3d coord3d = chart.getViewPoint();
		coord3d = coord3d.rotate(0.1f, Coord3d.IDENTITY.normalizeTo(1));
		chart.setViewPoint(coord3d);
        setDirty(true);
	}

	@Override
	public void viewPointChanged(ViewPointChangedEvent e) {
		xyzChartData.setViewPoint(e.getViewPoint());
        setDirty(true);
	}
	
	@Override
	public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mouseWheelMoved");
	}
	@Override
	public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mouseReleased");
	}
	@Override
	public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mousePressed");
	}
	@Override
	public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
	}
	@Override
	public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mouseExited");
	}
	@Override
	public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
	}
	@Override
	public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mouseDragged");
		System.out.println(e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON1));
		System.out.println(e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON2));
		System.out.println(e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON3));
	}
	@Override
	public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
		System.out.println("mouseClicked");
	}
	
}
