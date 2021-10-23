package fr.univamu.ism.docometre.analyse.editors;

import java.util.ArrayList;
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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.Settings;
import org.jzy3d.chart.controllers.mouse.camera.ICameraMouseController;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.controllers.thread.camera.CameraThreadController;
import org.jzy3d.chart.factories.CanvasNewtSWT;
import org.jzy3d.chart.factories.SWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;

import com.jogamp.newt.event.MouseListener;

import fr.univamu.ism.docometre.Activator;
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

public class XYZChartEditor extends EditorPart implements ISelectionChangedListener, /*IMarkersManager,*/ ZoomListener, TrialsEditor, ChartPropertiesListener, Chart2D3DBehaviour {
	
	public static String ID = "Docometre.XYZChartEditor";
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
//	protected volatile Chart3DPanel chart3DPanel;

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
//		Composite innerContainer = new Composite(parent, SWT.NONE);
//		innerContainer.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
//		FillLayout fl = new FillLayout();
//		fl.marginHeight = 5;
//		innerContainer.setLayout(fl);
		
		container = new SashForm(parent, SWT.HORIZONTAL);

		if(!xyzChartData.initialize()) {
			Label errorLabel = new Label(container, SWT.BORDER);
			errorLabel.setText(DocometreMessages.SomethingWentWrong);
			errorLabel.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			container.setSashWidth(0);
			container.setWeights(new int[] {100});
			return;
		}
//		chartContainer = parent;
		chartContainer = new Composite(container, SWT.NORMAL);
		chartContainer.setLayout(new FillLayout());
		
//		chart = TestJZY3DChart.createSurface(parent);
		
		getSite().getPage().addPartListener(new IPartListener2() {
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == XYZChartEditor.this) {
					System.out.println("partHidden " + partRef.getPartName());
					if(chart != null && chart.getCanvas() != null && !((CanvasNewtSWT) chart.getCanvas()).isDisposed()) {
						((CanvasNewtSWT) chart.getCanvas()).dispose();
						chart = null;
					}
				}
			}
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == XYZChartEditor.this) {
					System.out.println("partVisible " + partRef.getPartName());
					if(chart != null) {
						chartContainer.layout();
						return;
					}
					Mapper mapper = new Mapper() {
						@Override
						public double f(double x, double y) {
							return x * Math.sin(x * y);
						}
					};
					// Define range and precision for the function to plot
					Range range = new Range(-3, 3);
					int steps = 100;
					// Create the object to represent the function over the given range.
					final Shape surface = new SurfaceBuilder().orthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
					surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(),
							surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
					surface.setFaceDisplayed(true);
					surface.setWireframeDisplayed(false);
					surface.setLegendDisplayed(false);
					Settings.getInstance().setHardwareAccelerated(true);
					chart = new SWTChartFactory(chartContainer).newChart();
					chart.add(surface);
					ChartLauncher.openChart(chart);
					ICameraMouseController mouse =  ChartLauncher.configureControllers(chart, "JZY3D", true, true);
					chartContainer.layout();
//					Utils.addMouseListener((CanvasNewtSWT) chart.getCanvas());
//					((CanvasNewtSWT) chart.getCanvas()).addMouseListener(new MouseListener() {
//						
//						@Override
//						public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
//							System.out.println("mousePressed");
//							
//						}
//						
//						@Override
//						public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//						
//						@Override
//						public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
//							// TODO Auto-generated method stub
//							
//						}
//					});
					
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
		
		Composite container2 = new Composite(container, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 1;
		gl.marginWidth = 1;
		gl.verticalSpacing = 1;
		container2.setLayout(gl);
		container2.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
//		
//		CTabFolder trialsCategoriesTabFolder = new CTabFolder(container2, SWT.BOTTOM | SWT.FLAT | SWT.BORDER | SWT.MULTI);
//		trialsCategoriesTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		
//		CTabItem trialsTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
//		trialsTabItem.setText(DocometreMessages.TrialsGroupLabel);
//		
//		CTabItem categoriesTabItem = new CTabItem(trialsCategoriesTabFolder, SWT.BORDER);
//		categoriesTabItem.setText(DocometreMessages.Categories);
//		
//		
//		trialsListViewer = new ListViewer(trialsCategoriesTabFolder, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
//		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		trialsListViewer.setContentProvider(new ArrayContentProvider());
//		trialsListViewer.setLabelProvider(new LabelProvider() {
//			@Override
//			public String getText(Object element) {
//				String trial = super.getText(element);
//				return DocometreMessages.Trial + trial;
//			}
//		});
//		trialsListViewer.addSelectionChangedListener(this);
//		trialsTabItem.setControl(trialsListViewer.getList());
//		trialsCategoriesTabFolder.setSelection(trialsTabItem);
//		
//		
//		
//		// Graphical Front End cuts
//		Group frontEndCutValuesGroup = new Group(container2, SWT.NONE);
//		frontEndCutValuesGroup.setText(DocometreMessages.GraphicalCutsTitle);
//		frontEndCutValuesGroup.setLayout(new GridLayout(2, false));
//		frontEndCutValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		Label frontCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
//		frontCutLabel.setText(DocometreMessages.FrontCutLabel);
//		frontCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
//		frontCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
//		frontCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		frontCutSpinner.setMaximum(1000000000);
//		frontCutSpinner.setSelection(xyzChartData.getFrontCut());
//		frontCutSpinner.setData("frontCut");
//		FrontEndCutsHandler frontCutHandler = new FrontEndCutsHandler(frontCutSpinner, this);
//		frontCutSpinner.addMouseWheelListener(frontCutHandler);
//		frontCutSpinner.addTraverseListener(frontCutHandler);
//		frontCutSpinner.addSelectionListener(frontCutHandler);
//		
//		Label endCutLabel = new Label(frontEndCutValuesGroup, SWT.NONE);
//		endCutLabel.setText(DocometreMessages.EndCutLabel);
//		endCutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
//		endCutSpinner = new Spinner(frontEndCutValuesGroup, SWT.BORDER);
//		endCutSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		endCutSpinner.setMaximum(1000000000);
//		endCutSpinner.setSelection(xyzChartData.getEndCut());
//		endCutSpinner.setData("endCut");
//		FrontEndCutsHandler endCutHandler = new FrontEndCutsHandler(endCutSpinner, this);
//		endCutSpinner.addMouseWheelListener(endCutHandler);
//		endCutSpinner.addTraverseListener(endCutHandler);
//		endCutSpinner.addSelectionListener(endCutHandler);
//		
//		// Scales
//		scaleValuesGroup = new Group(container2, SWT.NONE);
//		scaleValuesGroup.setText(DocometreMessages.ScaleValueTitle);
//		scaleValuesGroup.setLayout(new GridLayout(4, false));
//		scaleValuesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		scaleValuesGroup.setEnabled(!xyzChartData.isAutoScale());
//		
//		Label xMinLabel = new Label(scaleValuesGroup, SWT.NONE);
//		xMinLabel.setText("X min. :");
//		xMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		xMinText = new Text(scaleValuesGroup, SWT.BORDER);
//		xMinText.setText(Double.toString(xyzChartData.getxMin()));
//		xMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		xMinText.setData("xMin");
//		xMinText.addTraverseListener(new RangeHandler(xMinText, this));
//		xMinText.setToolTipText(DocometreMessages.PressEnter);
//		Label xMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
//		xMaxLabel.setText("X max. :");
//		xMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		xMaxText = new Text(scaleValuesGroup, SWT.BORDER);
//		xMaxText.setText(Double.toString(xyzChartData.getxMax()));
//		xMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		xMaxText.setData("xMax");
//		xMaxText.addTraverseListener(new RangeHandler(xMaxText, this));
//		xMaxText.setToolTipText(DocometreMessages.PressEnter);
//
//		Label yMinLabel = new Label(scaleValuesGroup, SWT.NONE);
//		yMinLabel.setText("Y min. :");
//		yMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		yMinText = new Text(scaleValuesGroup, SWT.BORDER);
//		yMinText.setText(Double.toString(xyzChartData.getyMin()));
//		yMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		yMinText.setData("yMin");
//		yMinText.addTraverseListener(new RangeHandler(yMinText, this));
//		yMinText.setToolTipText(DocometreMessages.PressEnter);
//		Label yMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
//		yMaxLabel.setText("Y max. :");
//		yMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		yMaxText = new Text(scaleValuesGroup, SWT.BORDER);
//		yMaxText.setText(Double.toString(xyzChartData.getyMax()));
//		yMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		yMaxText.setData("yMax");
//		yMaxText.addTraverseListener(new RangeHandler(yMaxText, this));
//		yMaxText.setToolTipText(DocometreMessages.PressEnter);
//		
//		Label zMinLabel = new Label(scaleValuesGroup, SWT.NONE);
//		zMinLabel.setText("Z min. :");
//		zMinLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		zMinText = new Text(scaleValuesGroup, SWT.BORDER);
//		zMinText.setText(Double.toString(xyzChartData.getzMin()));
//		zMinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		zMinText.setData("zMin");
//		zMinText.addTraverseListener(new RangeHandler(zMinText, this));
//		zMinText.setToolTipText(DocometreMessages.PressEnter);
//		Label zMaxLabel = new Label(scaleValuesGroup, SWT.NONE);
//		zMaxLabel.setText("Z max. :");
//		zMaxLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
//		zMaxText = new Text(scaleValuesGroup, SWT.BORDER);
//		zMaxText.setText(Double.toString(xyzChartData.getzMax()));
//		zMaxText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		zMaxText.setData("zMax");
//		zMaxText.addTraverseListener(new RangeHandler(zMaxText, this));
//		zMaxText.setToolTipText(DocometreMessages.PressEnter);
//		
//		Composite bottomContainer = new Composite(container2, SWT.NONE);
//		bottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		gl = new GridLayout();
//		gl.numColumns = 3;
//		gl.marginHeight = 0;
//		gl.marginWidth = 0;
//		gl.marginBottom = 0;
//		gl.marginRight = 0;
//		bottomContainer.setLayout(gl);
//		
//		Button showMarkersButton = new Button(bottomContainer, SWT.CHECK | SWT.WRAP);
//		showMarkersButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		showMarkersButton.setText(DocometreMessages.ShowMarkersTitle);
//		showMarkersButton.setSelection(xyzChartData.isShowMarkers());
//		showMarkersButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				boolean showMarkers = showMarkersButton.getSelection();
//				xyzChartData.setShowMarkers(showMarkers);
////				chart3DPanel.update();
//				setDirty(true);
//			}
//		});
//		
//		Label sizeLabel = new Label(bottomContainer, SWT.NONE);
//		sizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
//		sizeLabel.setText(DocometreMessages.MarkersSizeTitle);
//		Spinner sizeSpinner = new Spinner(bottomContainer, SWT.BORDER);
//		sizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		sizeSpinner.setMaximum(10);
//		sizeSpinner.setMinimum(3);
//		sizeSpinner.setSelection(xyzChartData.getMarkersSize());
//		sizeSpinner.addMouseWheelListener(new MouseWheelListener() {
//			@Override
//			public void mouseScrolled(MouseEvent e) {
//				int value = sizeSpinner.getSelection() + e.count;
//				sizeSpinner.setSelection(value);
//				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
////				chart3DPanel.update();
//				setDirty(true);
//			}
//		});
//		sizeSpinner.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				xyzChartData.setMarkersSize(sizeSpinner.getSelection());
////				chart3DPanel.update();
//				setDirty(true);
//			}
//		});
//		
//		Button showMarkersLabelsButton = new Button(bottomContainer, SWT.CHECK | SWT.WRAP);
//		showMarkersLabelsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
//		showMarkersLabelsButton.setText(DocometreMessages.ShowMarkersLabelsTitle);
//		showMarkersLabelsButton.setSelection(xyzChartData.isShowMarkersLabels());
//		showMarkersLabelsButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				boolean showMarkersLabels = showMarkersLabelsButton.getSelection();
//				xyzChartData.setShowMarkersLabels(showMarkersLabels);
////				chart3DPanel.update();
//				setDirty(true);
//			}
//		});
//		
//		Composite bottomContainer2 = new Composite(container2, SWT.NONE);
//		bottomContainer2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		gl = new GridLayout();
//		gl.numColumns = 1;
//		gl.marginHeight = 0;
//		gl.marginWidth = 0;
//		gl.marginBottom = 5;
//		gl.marginRight = 5;
//		bottomContainer2.setLayout(gl);
//		
//		autoScaleButton = new Button(bottomContainer2, SWT.CHECK | SWT.WRAP);
//		autoScaleButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
//		autoScaleButton.setText(DocometreMessages.AutoScale_Title);
//		autoScaleButton.setSelection(xyzChartData.isAutoScale());
//		autoScaleButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				boolean autoScale = autoScaleButton.getSelection();
//				xyzChartData.setAutoScale(autoScale);
//				scaleValuesGroup.setEnabled(!autoScale); 
////				chart3DPanel.setAutoScale(autoScale);
//				updateRange();
//				setDirty(true);
//			}
//		});
//		
//		useSameColorButton = new Button(bottomContainer2, SWT.CHECK | SWT.WRAP);
//		useSameColorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		useSameColorButton.setText(DocometreMessages.UseSameColorForSameCategory);
//		useSameColorButton.setSelection(xyzChartData.isUseSameColorForSameCategory());
//		useSameColorButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				xyzChartData.setUseSameColorForSameCategory(useSameColorButton.getSelection());
//				updateSeriesColorsHandler();
//				setDirty(true);
//			}
//		});
//		
		container.setSashWidth(5);
		container.setWeights(new int[] {80, 20});
		
//		WorkbenchJob workbenchJob = new WorkbenchJob("Waiting for the 3D chart to be initialized...") {
//			@Override
//			public IStatus runInUIThread(IProgressMonitor monitor) {
//				try {
//					while(awtThread.isAlive() && !monitor.isCanceled()) {
//						awtThread.join(300);
//					}
//					if(monitor.isCanceled()) return Status.CANCEL_STATUS;
//					if(!awtThread.isAlive()) {
//						chart3DPanel.setAutoScale(xyzChartData.isAutoScale());
//						if(xyzChartData.getViewPoint() != null) chart3DPanel.setViewPoint(xyzChartData.getViewPoint());
//						refreshTrialsListFrontEndCuts();
//						trialsListViewer.setSelection(new StructuredSelection(xyzChartData.getSelectedTrialsNumbers()));
//						setDirty(false);
//					}
//				} catch (InterruptedException e) {
//					Activator.logErrorMessageWithCause(e);
//					e.printStackTrace();
//				} 
//				return Status.OK_STATUS;
//			}
//		};
//		
//		workbenchJob.schedule();
		

	}
	
	protected void updateSeriesColorsHandler() {
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		boolean sameColor = xyzChartData.isUseSameColorForSameCategory();
		String[] seriesIDs = getSeriesIDs();
		ArrayList<String> categories = new ArrayList<>();
//		byte i = 0; 
		for (String seriesID : seriesIDs) {
//			XYZSeries<String> series = chart3DPanel.getSeries(seriesID);
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
//					((ILineSeries)series).setLineColor(ColorUtil.getColor((byte) categories.indexOf(category)));
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else {
//				((ILineSeries)series).setLineColor(ColorUtil.getColor(i));
//				i++;
			}
		}
//		chart3DPanel.update();
	}
	
	@Override
	public void refreshTrialsListFrontEndCuts() {
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
			baseFrontCut = Math.min(MathEngineFactory.getMathEngine().getFrontCut(xChannel, 0), baseFrontCut);
			baseEndCut = Math.max(MathEngineFactory.getMathEngine().getEndCut(yChannel, 0), baseEndCut);
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
//		ISeries[] series = chart.getSeriesSet().getSeries();
//		Set<String> ids = new HashSet<>();
//		for (ISeries iSeries : series) {
//			String id = iSeries.getId();
////			id = id.replaceAll("\\.\\d+$", "");
//			ids.add(id);
//		}
//		return ids.toArray(new String[ids.size()]);
		return new String[0];
//		return chart3DPanel.getSeriesIDs();
	}
	
	@Override
	public void updateFrontEndCutsChartHandler() {
//		int value = frontCutSpinner.getSelection();
//		xyzChartData.setFrontCut(value);
//		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setFrontCut(value);
//		value = endCutSpinner.getSelection();
//		xyzChartData.setEndCut(value);
//		for (ISeries series : chart.getSeriesSet().getSeries()) ((ILineSeries)series).setEndCut(value);
//		chart.redraw();
	}

	@Override
	public void setFocus() {
//		container.setFocus();
//		chartContainer.setFocus();
//		((CanvasNewtSWT)chart.getCanvas()).setFocus();
	}

	private void removeAllSeries() {
//		ISeries[] series = chart.getSeriesSet().getSeries();
//		for (ISeries aSeries : series) {
//			chart.getSeriesSet().deleteSeries(aSeries.getId());
//		}
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
//			chart3DPanel.setXRange(xyzChartData.getxMin(), xyzChartData.getxMax());
//			chart3DPanel.setYRange(xyzChartData.getyMin(), xyzChartData.getyMax());
//			chart3DPanel.setZRange(xyzChartData.getzMin(), xyzChartData.getzMax());
		}
//		chart3DPanel.update();
		setDirty(true);
	}
	
	private void updateRange() {
//		xyzChartData.setxMin(chart3DPanel.getXRange().getMin());
//		xyzChartData.setxMax(chart3DPanel.getXRange().getMax());
//		xyzChartData.setyMin(chart3DPanel.getYRange().getMin());
//		xyzChartData.setyMax(chart3DPanel.getYRange().getMax());
//		xyzChartData.setzMin(chart3DPanel.getZRange().getMin());
//		xyzChartData.setzMax(chart3DPanel.getZRange().getMax());
//		xMinText.setText(Double.toString(chart3DPanel.getXRange().getMin()));
//		xMaxText.setText(Double.toString(chart3DPanel.getXRange().getMax()));
//		yMinText.setText(Double.toString(chart3DPanel.getYRange().getMin()));
//		yMaxText.setText(Double.toString(chart3DPanel.getYRange().getMax()));
//		zMinText.setText(Double.toString(chart3DPanel.getZRange().getMin()));
//		zMaxText.setText(Double.toString(chart3DPanel.getZRange().getMax()));
	}
	

	
	private Set<Integer> getTrialsInChart() {
		Set<Integer> trials = new HashSet<Integer>();
		String[] seriesIDs = new String[0];//chart3DPanel.getSeriesIDs();
		
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
//			chart3DPanel.removeSeries(seriesID);
		}
	}
	
	private void addSeriesToChart(Integer trialNumber) {
//		 Get x and Y values for this signal and trial
		Set<String> keys = xyzChartData.getCurvesIDs();
		for (String key : keys) {
			Channel[] channels = xyzChartData.getXYZChannels(key);
			double[] xValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[0], trialNumber);
			double[] yValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[1], trialNumber);
			double[] zValues = MathEngineFactory.getMathEngine().getYValuesForSignal(channels[2], trialNumber);
			// 
			if(xValues == null || yValues == null || zValues == null || xValues.length == 0 || yValues.length == 0 || zValues.length == 0) return;
			// Add Series
//			String seriesID = key + "." + trialNumber;
//			chart3DPanel.addSeries(seriesID, xValues, yValues, zValues);
//			int frontCut = xyzChartData.getFrontCut();
//			int endCut = xyzChartData.getEndCut();
//			series.setBaseFrontCut(frontCutSpinner.getMinimum());
//			series.setFrontCut(frontCut);
//			series.setEndCut(endCut);
//			series.setAntialias(SWT.ON);
//			series.setSymbolType(PlotSymbolType.NONE);
//			series.setLineWidth(3);
		}
//		 refresh Series Colors
		updateSeriesColorsHandler();
//		ISeries[] seriesList = chart.getSeriesSet().getSeries();
//		byte i = 0;
//		for (ISeries series : seriesList) {
//			((ILineSeries)series).setLineColor(ColorUtil.getColor(i));
//			i++;
//		}
	}
	
	private boolean chartHasAlreadyThisTrial(Integer trialNumber) {
		for (String seriesID : xyzChartData.getSeriesIDsPrefixes()) {
			seriesID = seriesID + "." + trialNumber;
//			if(chart3DPanel.hasSeriesID(seriesID)) return true;
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
		// TODO Auto-generated method stub
	}

	@Override
	public void updateXAxisRange(double min, double max) {
//		chart3DPanel.setXRange(min, max);
	}

	@Override
	public void updateYAxisRange(double min, double max) {
//		chart3DPanel.setYRange(min, max);
	}

	@Override
	public void updateZAxisRange(double min, double max) {
//		chart3DPanel.setZRange(min, max);
	}

	@Override
	public void removeSeries(String seriesID) {
//		chart3DPanel.removeSeries(seriesID);
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
//        setDirty(true);
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
//        setDirty(true);
	}

	public void zoomToFit() {
//		chart3DPanel.zoomToFit();
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//		chart3DPanel.update();
//        setDirty(true);
	}

	public void up() {
//		double delta = chart3DPanel.getRotateIncrement();
//		chart3DPanel.getViewPoint().moveUpDown(delta);
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//		chart3DPanel.update();
//        setDirty(true);
	}

	public void down() {
//		double delta = chart3DPanel.getRotateIncrement();
//		chart3DPanel.getViewPoint().moveUpDown(-delta);
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//		chart3DPanel.update();
//        setDirty(true);
	}

	public void right() {
//        chart3DPanel.panLeftRight(-chart3DPanel.getPanIncrement());
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();
//        setDirty(true);
	}

	public void left() {
//		chart3DPanel.panLeftRight(chart3DPanel.getPanIncrement());
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();
//        setDirty(true);
	}

	public void turnLeft() {
//        chart3DPanel.getViewPoint().roll(-chart3DPanel.getRollIncrement());
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();
//        setDirty(true);
	}

	public void turnRight() {
//        chart3DPanel.getViewPoint().roll(chart3DPanel.getRollIncrement());
//        xyzChartData.setViewPoint(chart3DPanel.getViewPoint());
//        chart3DPanel.update();
//        setDirty(true);
	}
	
}
