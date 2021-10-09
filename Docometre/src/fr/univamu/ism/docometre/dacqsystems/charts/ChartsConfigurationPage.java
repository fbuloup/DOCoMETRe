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
package fr.univamu.ism.docometre.dacqsystems.charts;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dialogs.ConfigureChartsLayoutDialog;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ChartsConfigurationPage extends ModulePage {
	
	public static String PAGE_ID = "ChartsConfigurationPage";
	
	public class CurvesComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			CurveConfiguration in1 = (CurveConfiguration) e1;
			CurveConfiguration in2 = (CurveConfiguration) e2;
			switch (sortingColumnNumber) {
			case 0:
				if(in1 instanceof OscilloCurveConfiguration){
					e1 = in1.getProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME);
					e2 = in2.getProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME);
					result = super.compare(viewer, (String)e1, (String)e2);
				}
				break;
			case 1:
				e1 = in1.getProperty(CurveConfigurationProperties.COLOR);
				e2 = in2.getProperty(CurveConfigurationProperties.COLOR);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}
	
	
	private TreeViewer chartsTreeViewer;

	private ToolItem deleteChartToolItem;
	private ToolItem moveChartUpToolItem;
	private ToolItem moveChartDownToolItem;

	private Composite chartConfigurationContainer;

	private ChartConfiguration currentSelectedChartConfiguration;

	private PartListenerAdapter partListenerAdapter;


	public ChartsConfigurationPage(FormEditor editor) {
		super(editor, PAGE_ID, DocometreMessages.ChartsConfigurationPage_PageTitle, null);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		Charts charts = dacqConfiguration.getCharts();
		charts.addObserver(this);
		ChartConfiguration[] chartsConfigurations = charts.getChartsConfigurations();
		for (ChartConfiguration chartConfiguration : chartsConfigurations) {
			CurveConfiguration[] curvesConfigurations = chartConfiguration.getCurvesConfiguration();
			for (CurveConfiguration curveConfiguration : curvesConfigurations) {
				curveConfiguration.addObserver(this);
			}
		}
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == getEditor()) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					Charts charts = dacqConfiguration.getCharts();
					charts.removeObserver(ChartsConfigurationPage.this);
					ChartConfiguration[] chartsConfigurations = charts.getChartsConfigurations();
					for (ChartConfiguration chartConfiguration : chartsConfigurations) {
						chartConfiguration.removeObserver(ChartsConfigurationPage.this);
						CurveConfiguration[] curvesConfigurations = chartConfiguration.getCurvesConfiguration();
						for (CurveConfiguration curveConfiguration : curvesConfigurations) {
							curveConfiguration.removeObserver(ChartsConfigurationPage.this);
						}
					}
//					if(currentSelectedChartConfiguration != null) {
//						CurveConfiguration[] curvesConfigurations = currentSelectedChartConfiguration.getCurvesConfiguration();
//						for (CurveConfiguration curveConfiguration : curvesConfigurations) {
//							curveConfiguration.clearObservers();
//						}
//					}
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		
		managedForm.getForm().getForm().setText(DocometreMessages.ChartsConfigurationPage_Title);
		
		/*
		 * Section 1 : charts configurations
		 */
		createGeneralConfigurationSection(5, false);
		GridLayout gl = (GridLayout) generalconfigurationContainer.getLayout();
		gl.makeColumnsEqualWidth = true;
		generalConfigurationSection.setText(DocometreMessages.ChartsConfigurationSection_Title);
		generalConfigurationSection.setDescription(DocometreMessages.ChartsConfigurationSectionDescription);
		GridData gd = (GridData) generalConfigurationSection.getLayoutData();
		gd.grabExcessVerticalSpace = true;
		
		//Charts tool bar
		ToolBar toolBar = new ToolBar(generalConfigurationSection, SWT.FLAT | SWT.HORIZONTAL);
		deleteChartToolItem = new ToolItem(toolBar, SWT.NULL);
		deleteChartToolItem.setEnabled(false);
		deleteChartToolItem.setImage(Activator.getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE).createImage());
		deleteChartToolItem.setToolTipText(DocometreMessages.DeleteChartsToolItem_Tooltip);
		DeleteChartsHandler deleteChartsHandler = new DeleteChartsHandler(getSite().getShell(), this, dacqConfiguration.getCharts(), ((ResourceEditor)getEditor()).getUndoContext()); 
		deleteChartToolItem.addSelectionListener(deleteChartsHandler);
		new ToolItem(toolBar, SWT.SEPARATOR);
		ToolItem addChartToolItem = new ToolItem(toolBar, SWT.NULL);
		addChartToolItem.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addChartToolItem.setToolTipText(DocometreMessages.AddChartToolItem_Tooltip);
		AddChartHandler addChartsHandler = new AddChartHandler(this, dacqConfiguration.getCharts(), ((ResourceEditor)getEditor()).getUndoContext());
		addChartToolItem.addSelectionListener(addChartsHandler);
		new ToolItem(toolBar, SWT.SEPARATOR);
		moveChartUpToolItem = new ToolItem(toolBar, SWT.NULL);
		moveChartUpToolItem.setEnabled(false);
		moveChartUpToolItem.setImage(Activator.getImage(IImageKeys.UP_ICON));
		moveChartUpToolItem.setToolTipText(DocometreMessages.MoveChartUpToolItem_Tooltip);
		MoveChartHandler moveUpChartHandler = new MoveChartHandler(this, dacqConfiguration.getCharts(), ((ResourceEditor)getEditor()).getUndoContext(), SWT.UP);
		moveChartUpToolItem.addSelectionListener(moveUpChartHandler);
		moveChartDownToolItem = new ToolItem(toolBar, SWT.NULL);
		moveChartDownToolItem.setEnabled(false);
		moveChartDownToolItem.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		moveChartDownToolItem.setToolTipText(DocometreMessages.MoveChartDownToolItem_Tooltip);
		MoveChartHandler moveDownChartHandler = new MoveChartHandler(this, dacqConfiguration.getCharts(), ((ResourceEditor)getEditor()).getUndoContext(), SWT.DOWN);
		moveChartDownToolItem.addSelectionListener(moveDownChartHandler);
		new ToolItem(toolBar, SWT.SEPARATOR);
		ToolItem chartsLayoutToolItem = new ToolItem(toolBar, SWT.NULL);
		chartsLayoutToolItem.setImage(Activator.getImage(IImageKeys.CHARTS_LAYOUT_ICON));
		chartsLayoutToolItem.setToolTipText(DocometreMessages.ChartLayoutToolItem_Tooltip);
		chartsLayoutToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfigureChartsLayoutDialog configureChartsLayoutDialog = new ConfigureChartsLayoutDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), dacqConfiguration);
				if(configureChartsLayoutDialog.open() == Window.OK) {
					if(configureChartsLayoutDialog.getModified()) {
						chartsTreeViewer.refresh();
						generalConfigurationSectionPart.markDirty();
					}
				}
			}
		});
		generalConfigurationSection.setTextClient(toolBar);
		
		Tree chartsTree = managedForm.getToolkit().createTree(generalconfigurationContainer, SWT.BORDER | SWT.MULTI);
		chartsTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		managedForm.getToolkit().paintBordersFor(chartsTree);
		chartsTreeViewer = new TreeViewer(chartsTree);
		chartsTreeViewer.addSelectionChangedListener(deleteChartsHandler);
		chartsTreeViewer.addSelectionChangedListener(moveUpChartHandler);
		chartsTreeViewer.addSelectionChangedListener(moveDownChartHandler);
		chartsTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateToolItems();
				updateChartConfiguration();
			}
		});
		chartsTreeViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			@Override
			public Object getParent(Object element) {
				return null;
			}
			@Override
			public Object[] getElements(Object inputElement) {
				Object[] elements = null;
				if(inputElement instanceof DACQConfiguration) {
					elements = ((DACQConfiguration)inputElement).getCharts().getChartsConfigurations();
					for (Object element : elements) {
						((ChartConfiguration)element).addObserver(ChartsConfigurationPage.this);
					}
				}
				return elements;
			}
			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		chartsTreeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				ChartConfiguration[] chartConfigurations = (ChartConfiguration[]) dacqConfiguration.getCharts().getChartsConfigurations();
				List<ChartConfiguration> list = Arrays.asList(chartConfigurations);
				int index = list.indexOf(element) + 1;
				String stringIndex = Integer.toString(index) + ". ";
				if(element instanceof ChartConfiguration) return stringIndex + ((ChartConfiguration)element).getLabel();
				return "";
			}
		});
		chartsTreeViewer.setInput(dacqConfiguration);
		
		chartConfigurationContainer = managedForm.getToolkit().createComposite(generalconfigurationContainer, SWT.NONE);
		chartConfigurationContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		
		/*
		 * Section 2 : curves configurations
		 */
		createTableConfigurationSection(true, false, false, false);
		tableConfigurationSection.setText(DocometreMessages.CurvesConfigurationSection_Title);
		tableConfigurationSection.setDescription(DocometreMessages.CurvesConfigurationSectionDescription);
		
		addToolItem.setToolTipText(DocometreMessages.AddCurveToolItem_Tooltip);
		addToolItem.setEnabled(false);
		AddCurveHandler addCurveHandler = new AddCurveHandler(this, dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext());
		chartsTreeViewer.addSelectionChangedListener(addCurveHandler);
		addToolItem.addSelectionListener(addCurveHandler);
		deleteToolItem.setToolTipText(DocometreMessages.DeleteCurvesToolItem_Tooltip);
		deleteToolItem.setEnabled(false);
		DeleteCurvesHandler deleteCurvesHandler = new DeleteCurvesHandler(this, ((ResourceEditor)getEditor()).getUndoContext());
		chartsTreeViewer.addSelectionChangedListener(deleteCurvesHandler);
		deleteToolItem.addSelectionListener(deleteCurvesHandler);
		tableViewer.addSelectionChangedListener(deleteCurvesHandler);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				deleteToolItem.setEnabled(!((IStructuredSelection)tableViewer.getSelection()).isEmpty());
			}
		});
		
		updateCurvesTable(null);
		
	}
	
	protected void updateChartConfiguration() {
		chartConfigurationContainer.dispose();
		chartConfigurationContainer = getManagedForm().getToolkit().createComposite(generalconfigurationContainer, SWT.NONE);
		chartConfigurationContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		int nbSelectedElements = chartsTreeViewer.getStructuredSelection().size();
		updateCurvesTable(null);
		if(nbSelectedElements == 1) {
//			if(currentSelectedChartConfiguration != null) {
//				CurveConfiguration[] curvesConfigurations = currentSelectedChartConfiguration.getCurvesConfiguration();
//				for (CurveConfiguration curveConfiguration : curvesConfigurations) {
//					curveConfiguration.clearObservers();
//				}
//			}
			currentSelectedChartConfiguration = (ChartConfiguration) chartsTreeViewer.getStructuredSelection().getFirstElement();
			currentSelectedChartConfiguration.populateChartConfigurationContainer(chartConfigurationContainer, this, generalConfigurationSectionPart);
			updateCurvesTable(currentSelectedChartConfiguration);
		} 
		generalConfigurationSection.layout();
	}

	protected void updateToolItems() {
		deleteChartToolItem.setEnabled(true);
		moveChartDownToolItem.setEnabled(true);
		moveChartUpToolItem.setEnabled(true);
		addToolItem.setEnabled(true);
		deleteToolItem.setEnabled(false);
		int nbSelectedElements = chartsTreeViewer.getStructuredSelection().size();
		if(nbSelectedElements == 0) {
			deleteChartToolItem.setEnabled(false);
			moveChartDownToolItem.setEnabled(false);
			moveChartUpToolItem.setEnabled(false);
			addToolItem.setEnabled(false);
			deleteToolItem.setEnabled(false);
		} else if(nbSelectedElements == 1) {
			ChartConfiguration chartConfiguration = (ChartConfiguration) chartsTreeViewer.getStructuredSelection().getFirstElement();
			ChartConfiguration[] chartConfigurations = dacqConfiguration.getCharts().getChartsConfigurations();
			if(chartConfigurations[0] == chartConfiguration) moveChartUpToolItem.setEnabled(false);
			if(chartConfigurations[chartConfigurations.length - 1] == chartConfiguration) moveChartDownToolItem.setEnabled(false);
		} else if(nbSelectedElements > 1) {
			moveChartDownToolItem.setEnabled(false);
			moveChartUpToolItem.setEnabled(false);
			addToolItem.setEnabled(false);
			deleteToolItem.setEnabled(false);
		}
	}

	private void updateCurvesTable(ChartConfiguration chartConfiguration) {
		if(tableViewer == null) return;
		tableViewer.getTable().setVisible(false);
		TableItem[] tableItems = tableViewer.getTable().getItems();
		for (int i = 0; i < tableItems.length; i++) tableItems[i].dispose();
		TableColumn[] columns = tableViewer.getTable().getColumns();
		for (TableColumn tableColumn : columns) tableColumn.dispose();
		if(chartConfiguration == null) {
			TableColumnLayout curvesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
			createColumn(CurveConfigurationProperties.DUMMY.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.DUMMY, defaultColumnWidth, 0);
			tableConfigurationContainer.layout(true);
			return;
		}
		tableViewer.getTable().setVisible(true);
		if(currentSelectedChartConfiguration instanceof OscilloChartConfiguration) createOscilloCurvesSection();			
		if(currentSelectedChartConfiguration instanceof XYChartConfiguration) createXYCurvesSection();
		if(currentSelectedChartConfiguration instanceof MeterChartConfiguration) createMeterCurveSection();			
		
		tableConfigurationContainer.layout(true);
	}

	/*
	 * This method is called to reflect any changes between model and UI.
	 * It is called when undo/redo operation are run but also when changes
	 * happened in model. It is not possible
	 * to use properties listeners as it will result in cyclic calls and
	 * stack overflow.
	 */
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// Move, add or remove chart
		boolean test = property.equals(DACQConfigurationProperties.ADD_CHART) || property.equals(DACQConfigurationProperties.REMOVE_CHART) || property.equals(DACQConfigurationProperties.MOVE_CHART);
		if(test) {
			if(chartsTreeViewer != null) chartsTreeViewer.refresh();
			if(generalConfigurationSectionPart != null) generalConfigurationSectionPart.markDirty();
			if (property.equals(DACQConfigurationProperties.ADD_CHART)) chartsTreeViewer.setSelection(new StructuredSelection(newValue));
			updateToolItems();
		}
		
		// Add or remove curve, change curve color, delete channel used by curve
		test = property == ChartConfigurationProperties.ADD_CURVE || property == ChartConfigurationProperties.REMOVE_CURVE || property == CurveConfigurationProperties.COLOR;
		test = test || property == ChartConfigurationProperties.UNTRANSFERED_CHANNEL;
		test = test || property == ChannelProperties.NAME || property == ChannelProperties.REMOVE;
		if(test) {
			if(property == ChannelProperties.REMOVE || property == ChartConfigurationProperties.UNTRANSFERED_CHANNEL) {
				dacqConfiguration.getCharts().removeCurveConditionaly(oldValue);
			}
			updateCurvesTable(currentSelectedChartConfiguration);
			if(tableViewer != null) {
//				tableViewer.refresh(element);
				if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
			}
		}
		
		// Chart configuration has changed, must reflect it in UI
		if(element instanceof ChartConfiguration) {
			if(element != currentSelectedChartConfiguration) {
				if(chartsTreeViewer != null) chartsTreeViewer.setSelection(new StructuredSelection(new Object[]{element}));
				currentSelectedChartConfiguration = (ChartConfiguration) element;
			}
			currentSelectedChartConfiguration.update(property, newValue, oldValue);
			if(generalConfigurationSectionPart != null) generalConfigurationSectionPart.markDirty();
		}
		
		if(element instanceof CurveConfiguration) {
			updateCurvesTable(currentSelectedChartConfiguration);
			if(generalConfigurationSectionPart != null) generalConfigurationSectionPart.markDirty();
		}
	}
	
	
	@Override
	public String getPageTitle() {
		return DocometreMessages.ChartsConfigurationPage_PageTitle;
	}
	
	private void createOscilloCurvesSection() {
		TableColumnLayout  curvesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		TableViewerColumn channelNameTableViewerColumn = createColumn(OscilloCurveConfigurationProperties.CHANNEL_NAME.getTooltip(), curvesTableColumnLayout, OscilloCurveConfigurationProperties.CHANNEL_NAME, 175, 0);
		channelNameTableViewerColumn.setEditingSupport(null);
		channelNameTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof OscilloCurveConfiguration)) return;
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)element;
				cell.setText(curve.getProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME));
			}
		});
		final TableViewerColumn curveColorTableViewerColumn = createColumn(CurveConfigurationProperties.COLOR.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.COLOR, 175, 1);
		curveColorTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof OscilloCurveConfiguration)) return;
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)element;
				GC gc = new GC(tableViewer.getTable());
		        FontMetrics fm = gc.getFontMetrics();
		        int size = fm.getAscent();
		        gc.dispose();
		        int indent = 6;
		        int extent = 16;
		        extent = tableViewer.getTable().getItemHeight() - 1;
		        if (size > extent) {
					size = extent;
				}
		        int width = indent + size;
		        int height = extent;
		        int xoffset = indent;
		        int yoffset = (height - size) / 2;
		        RGB black = new RGB(0, 0, 0);
		        PaletteData dataPalette = new PaletteData(new RGB[] { black, black, CurveConfigurationProperties.getColor(curve).getRGB() });
		        ImageData data = new ImageData(width, height, 4, dataPalette);
		        data.transparentPixel = 0;
		        int end = size - 1;
		        for (int y = 0; y < size; y++) {
		            for (int x = 0; x < size; x++) {
		                if (x == 0 || y == 0 || x == end || y == end) {
							data.setPixel(x + xoffset, y + yoffset, 1);
						} else {
							data.setPixel(x + xoffset, y + yoffset, 2);
						}
		            }
		        }
		        Image image = new Image(PlatformUI.getWorkbench().getDisplay(), data);
		        cell.setImage(image);
		        curveColorTableViewerColumn.getColumn().addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
						image.dispose();
					}
				});
			}
		});
		
		TableViewerColumn curveStyleTableViewerColumn = createColumn(CurveConfigurationProperties.STYLE.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.STYLE, defaultColumnWidth, 2);
		curveStyleTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof OscilloCurveConfiguration)) return;
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)cell.getElement();
				cell.setText(curve.getProperty(CurveConfigurationProperties.STYLE));
			}
		});
		
		
		TableViewerColumn curveWidthTableViewerColumn = createColumn(CurveConfigurationProperties.WIDTH.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.WIDTH, defaultColumnWidth, 3);
		curveWidthTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof OscilloCurveConfiguration)) return;
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)cell.getElement();
				cell.setText(curve.getProperty(CurveConfigurationProperties.WIDTH));
			}
		});
		
		
		TableViewerColumn curveDisplayValueTableViewerColumn = createColumn(OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES.getTooltip(), curvesTableColumnLayout, OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES, defaultColumnWidth + 100, 4);
		curveDisplayValueTableViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof OscilloCurveConfiguration)) return "";
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)element;
				String value = curve.getProperty(OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES);
				return value == null ? "false":value;
			}
			
			@Override
			public Image getImage(Object element) {
				if(!(element instanceof OscilloCurveConfiguration)) return null;
				OscilloCurveConfiguration curve = (OscilloCurveConfiguration)element;
				String value = "false";
				value = curve.getProperty(OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES);
				return "true".equals(value) ? ModulePage.checkedImage : ModulePage.uncheckedImage;
			}
		});
		
		configureSorter(new CurvesComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(currentSelectedChartConfiguration.getCurvesConfiguration());
		
//		CurveConfiguration[] curvesConfigurations = currentSelectedChartConfiguration.getCurvesConfiguration();
//		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
//			curveConfiguration.initializeObservers();
//		}
	}
	
	private void createXYCurvesSection() {
		TableColumnLayout  curvesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		
		TableViewerColumn xChannelNameTableViewerColumn = createColumn(XYCurveConfigurationProperties.X_CHANNEL_NAME.getTooltip(), curvesTableColumnLayout, XYCurveConfigurationProperties.X_CHANNEL_NAME, 175, 0);
		xChannelNameTableViewerColumn.setEditingSupport(null);
		xChannelNameTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof XYCurveConfiguration)) return;
				XYCurveConfiguration curve = (XYCurveConfiguration)element;
				cell.setText(curve.getProperty(XYCurveConfigurationProperties.X_CHANNEL_NAME));
			}
		});
		
		TableViewerColumn yChannelNameTableViewerColumn = createColumn(XYCurveConfigurationProperties.Y_CHANNEL_NAME.getTooltip(), curvesTableColumnLayout, XYCurveConfigurationProperties.Y_CHANNEL_NAME, 175, 0);
		yChannelNameTableViewerColumn.setEditingSupport(null);
		yChannelNameTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof XYCurveConfiguration)) return;
				XYCurveConfiguration curve = (XYCurveConfiguration)cell.getElement();
				cell.setText(curve.getProperty(XYCurveConfigurationProperties.Y_CHANNEL_NAME));
			}
		});
		
		final TableViewerColumn curveColorTableViewerColumn = createColumn(CurveConfigurationProperties.COLOR.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.COLOR, 175, 1);
		curveColorTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof XYCurveConfiguration)) return;
				XYCurveConfiguration curve = (XYCurveConfiguration)cell.getElement();
				GC gc = new GC(tableViewer.getTable());
		        FontMetrics fm = gc.getFontMetrics();
		        int size = fm.getAscent();
		        gc.dispose();
		        int indent = 6;
		        int extent = 16;
		        extent = tableViewer.getTable().getItemHeight() - 1;
		        if (size > extent) {
					size = extent;
				}
		        int width = indent + size;
		        int height = extent;
		        int xoffset = indent;
		        int yoffset = (height - size) / 2;
		        RGB black = new RGB(0, 0, 0);
		        PaletteData dataPalette = new PaletteData(new RGB[] { black, black, CurveConfigurationProperties.getColor(curve).getRGB() });
		        ImageData data = new ImageData(width, height, 4, dataPalette);
		        data.transparentPixel = 0;
		        int end = size - 1;
		        for (int y = 0; y < size; y++) {
		            for (int x = 0; x < size; x++) {
		                if (x == 0 || y == 0 || x == end || y == end) {
							data.setPixel(x + xoffset, y + yoffset, 1);
						} else {
							data.setPixel(x + xoffset, y + yoffset, 2);
						}
		            }
		        }
		        Image image = new Image(PlatformUI.getWorkbench().getDisplay(), data);
		        cell.setImage(image);
		        curveColorTableViewerColumn.getColumn().addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
						image.dispose();
					}
				});
			}
		});
		
		TableViewerColumn curveStyleTableViewerColumn = createColumn(CurveConfigurationProperties.STYLE.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.STYLE, defaultColumnWidth, 2);
		curveStyleTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof XYCurveConfiguration)) return;
				XYCurveConfiguration curve = (XYCurveConfiguration)cell.getElement();
				cell.setText(curve.getProperty(CurveConfigurationProperties.STYLE));
			}
		});
		
		TableViewerColumn curveWidthTableViewerColumn = createColumn(CurveConfigurationProperties.WIDTH.getTooltip(), curvesTableColumnLayout, CurveConfigurationProperties.WIDTH, defaultColumnWidth, 3);
		curveWidthTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof XYCurveConfiguration)) return;
				XYCurveConfiguration curve = (XYCurveConfiguration)cell.getElement();
				cell.setText(curve.getProperty(CurveConfigurationProperties.WIDTH));
			}
		});
		if(tableViewer.getContentProvider() != null) tableViewer.setInput(null);
		configureSorter(new CurvesComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(currentSelectedChartConfiguration.getCurvesConfiguration());
		
	}
	
	private void createMeterCurveSection() {
		TableColumnLayout  curvesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		TableViewerColumn channelNameTableViewerColumn = createColumn(MeterCurveConfigurationProperties.CHANNEL_NAME.getTooltip(), curvesTableColumnLayout, MeterCurveConfigurationProperties.CHANNEL_NAME, 175, 0);
		channelNameTableViewerColumn.setEditingSupport(null);
		channelNameTableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(!(element instanceof MeterCurveConfiguration)) return;
				MeterCurveConfiguration curve = (MeterCurveConfiguration)element;
				cell.setText(curve.getProperty(MeterCurveConfigurationProperties.CHANNEL_NAME));
			}
		});
		
		TableViewerColumn curveDisplayValueTableViewerColumn = createColumn(MeterCurveConfigurationProperties.DISPLAY_CURRENT_VALUES.getTooltip(), curvesTableColumnLayout, MeterCurveConfigurationProperties.DISPLAY_CURRENT_VALUES, defaultColumnWidth + 100, 4);
		curveDisplayValueTableViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof MeterCurveConfiguration)) return "";
				MeterCurveConfiguration curve = (MeterCurveConfiguration)element;
				String value = curve.getProperty(MeterCurveConfigurationProperties.DISPLAY_CURRENT_VALUES);
				return value == null ? "false":value;
			}
			
			@Override
			public Image getImage(Object element) {
				if(!(element instanceof MeterCurveConfiguration)) return null;
				MeterCurveConfiguration curve = (MeterCurveConfiguration)element;
				String value = "false";
				value = curve.getProperty(MeterCurveConfigurationProperties.DISPLAY_CURRENT_VALUES);
				return "true".equals(value) ? ModulePage.checkedImage : ModulePage.uncheckedImage;
			}
		});
		
		configureSorter(new CurvesComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(currentSelectedChartConfiguration.getCurvesConfiguration());
		
//		CurveConfiguration[] curvesConfigurations = currentSelectedChartConfiguration.getCurvesConfiguration();
//		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
//			curveConfiguration.initializeObservers();
//		}
	}

}
