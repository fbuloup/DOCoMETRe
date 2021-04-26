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
package fr.univamu.ism.docometre.editors;

import java.util.EventObject;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PaletteToolbar;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.scripteditor.actions.CopyAction;
import fr.univamu.ism.docometre.scripteditor.actions.DeactivateBlockAction;
import fr.univamu.ism.docometre.scripteditor.actions.EditBlockAction;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.docometre.scripteditor.actions.PasteAction;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.docometre.scripteditor.editparts.ScriptEditPartFactory;
import fr.univamu.ism.docometre.scripteditor.editparts.ScriptSegmentEditPart;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

public abstract class AbstractScriptSegmentEditor extends GraphicalEditorWithFlyoutPalette {
	
	class ToggleSnapToGridAction extends Action {
		
		private GraphicalViewer graphicalViewer;
		private boolean state;
		
		public ToggleSnapToGridAction(GraphicalViewer graphicalViewer) {
			this.graphicalViewer = graphicalViewer;
			this.state = AbstractScriptSegmentEditor.this.getAlignmentHelperState();
			setChecked(state);
		}
		
		@Override
		public void run() {
			state = !state;
			AbstractScriptSegmentEditor.this.setAlignmentHelperState(state);
//			AbstractScriptSegmentEditor.this.setGridState(state);
//			graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, new Boolean(state));
//			graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, new Boolean(state));
			graphicalViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, Boolean.valueOf(state));
			
		}
	}
	
	class ShowGridAction extends Action {
		
		private GraphicalViewer graphicalViewer;
		private boolean state;

		public ShowGridAction(GraphicalViewer graphicalViewer) {
			this.graphicalViewer = graphicalViewer;
			this.state = AbstractScriptSegmentEditor.this.getGridState();
			setChecked(state);
		}
		
		@Override
		public void run() {
			state = !state;
			AbstractScriptSegmentEditor.this.setGridState(state);
			graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(state));
			graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(state));
		}
	}
	
	class ZoomToSpecificScaleAction extends Action {
		
		private AbstractScriptSegmentEditor scriptSegmentEditor;
		private double scaleValue;
		
		public ZoomToSpecificScaleAction(AbstractScriptSegmentEditor scriptSegmentEditor, double scaleValue, String label) {
			setText(DocometreMessages.ZoomToTitle + label);
			if(scaleValue > 1) setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ZOOM_IN));
			if(scaleValue == 1) setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ZOOM_SCALE_1));
			if(scaleValue < 1) setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ZOOM_OUT));
			this.scriptSegmentEditor = scriptSegmentEditor;
			this.scaleValue = scaleValue;
		}
		
		@Override
		public void run() {
			ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) scriptSegmentEditor.getGraphicalViewer().getRootEditPart();
			ScalableFreeformLayeredPane scalableLayerPane = (ScalableFreeformLayeredPane) rootEditPart.getZoomManager().getScalableFigure();
			scalableLayerPane.setScale(scaleValue);
		}
	}
	
	class ZoomToFitPageAction extends Action {
		
		private AbstractScriptSegmentEditor scriptSegmentEditor;

		public ZoomToFitPageAction(AbstractScriptSegmentEditor scriptSegmentEditor) {
			setText(DocometreMessages.ZoomToFitTitle);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ZOOM_TO_FIT));
			this.scriptSegmentEditor = scriptSegmentEditor;
		}
		
		@Override
		public void run() {
			ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) scriptSegmentEditor.getGraphicalViewer().getRootEditPart();
			ScalableFreeformLayeredPane scalableLayerPane = (ScalableFreeformLayeredPane) rootEditPart.getZoomManager().getScalableFigure();
			FreeformViewport viewport = (FreeformViewport) scalableLayerPane.getParent().getParent();
			scalableLayerPane.setScale(1);
			Rectangle extend = scalableLayerPane.getFreeformExtent().union(0, 0);
			double wScale = ((double)viewport.getClientArea().width() / extend.width());
			double hScale = ((double)viewport.getClientArea().height() / extend.height);
			double newScale = Math.min(wScale, hScale);
			scalableLayerPane.setScale(newScale);
		}
	}
	
	class ScriptSegmentEditorContextMenuProvider extends ContextMenuProvider {

		public ScriptSegmentEditorContextMenuProvider(EditPartViewer viewer) {
			super(viewer);
		}

		@Override
		public void buildContextMenu(IMenuManager menuManager) {
			
			GEFActionConstants.addStandardActionGroups(menuManager);
			IAction action;
			 
			action = getActionRegistry().getAction(copyAction.getId());
	        menuManager.appendToGroup(GEFActionConstants.GROUP_COPY, action);
	        action = getActionRegistry().getAction(pasteAction.getId());
	        menuManager.appendToGroup(GEFActionConstants.GROUP_COPY, action);
			
	        action = getActionRegistry().getAction(ActionFactory.UNDO.getId());
	        menuManager.appendToGroup(GEFActionConstants.GROUP_UNDO, action);
	        action = getActionRegistry().getAction(ActionFactory.REDO.getId());
	        menuManager.appendToGroup(GEFActionConstants.GROUP_UNDO, action);
	        
	        action = getActionRegistry().getAction(EditBlockAction.EDIT_BLOCK);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
	        IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
	        if(selection.size() == 1) {
	        		if(selection.getFirstElement() instanceof BlockEditPart) {
	        			BlockEditPart blockEditPart = (BlockEditPart) selection.getFirstElement();
	        			if(blockEditPart.getModel() instanceof Function) {
		        			 MenuManager functionsMenuManager = new MenuManager(DocometreMessages.AssignTitle);
		        			 FunctionFactory.populateMenu(AbstractScriptSegmentEditor.this, blockEditPart, functionsMenuManager);
		        			 menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, functionsMenuManager);
		        		}
	        		}
	        		
	        }
	        menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, new Separator());
	        action = getActionRegistry().getAction(DeactivateBlockAction.DEACTIVATE_BLOCK);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
	        
	        menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, new Separator());
	        action = getActionRegistry().getAction(ActionFactory.DELETE.getId());
	        menuManager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
	        
	        
	        action = new ZoomToFitPageAction(AbstractScriptSegmentEditor.this);
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ZoomToSpecificScaleAction(AbstractScriptSegmentEditor.this, 0.25, "25%");
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ZoomToSpecificScaleAction(AbstractScriptSegmentEditor.this, 0.5, "50%");
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ZoomToSpecificScaleAction(AbstractScriptSegmentEditor.this, 1, "100%");
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ZoomToSpecificScaleAction(AbstractScriptSegmentEditor.this, 2, "200%");
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ZoomToSpecificScaleAction(AbstractScriptSegmentEditor.this, 3, "300%");
//	        getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ShowGridAction(getGraphicalViewer());
	        action.setText(DocometreMessages.ShowGridTitle);
//			getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        action = new ToggleSnapToGridAction(getGraphicalViewer());
	        action.setText(DocometreMessages.BlockAlignmentHelperTitle);
//			getActionRegistry().registerAction(action);
	        menuManager.appendToGroup(GEFActionConstants.GROUP_REST, action);
	        
		}
	}
	
	private CopyAction copyAction;
	private PasteAction pasteAction;
	private EditBlockAction editBlockAction;
	private DeactivateBlockAction deactivateBlockAction;

	private PaletteDrawer paletteDrawer;
	private RulerComposite rulerComp;
//	protected Process process;
	private ScriptSegmentType scriptSegmentType;
	
	public AbstractScriptSegmentEditor(CommandStack commandStack, ScriptSegmentType scriptSegmentType) {
		this.scriptSegmentType = scriptSegmentType;
		DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
		defaultEditDomain.setCommandStack(commandStack);
		setEditDomain(defaultEditDomain);
	}
	
	private boolean getGridState() {
		boolean state = false;
		IResource resource = ObjectsController.getResourceForObject(((ResourceEditorInput)getEditorInput()).getObject());
		if(resource == null && ((ResourceEditorInput)getEditorInput()).getObject() instanceof IResource) resource = (IResource) ((ResourceEditorInput)getEditorInput()).getObject();
		QualifiedName key = new QualifiedName(AbstractScriptSegmentEditor.this.getClass().getCanonicalName(), "gridState");
		try {
			String gridStateString = resource.getPersistentProperty(key);
			state = Boolean.parseBoolean(gridStateString);
		} catch (CoreException e) {
			setGridState(false);
		}
		return state;
	}
	
	private void setGridState(boolean state) {
		try {
			IResource resource = ObjectsController.getResourceForObject(((ResourceEditorInput)getEditorInput()).getObject());
			if(resource == null && ((ResourceEditorInput)getEditorInput()).getObject() instanceof IResource) resource = (IResource) ((ResourceEditorInput)getEditorInput()).getObject();
			QualifiedName key = new QualifiedName(AbstractScriptSegmentEditor.this.getClass().getCanonicalName(), "gridState");
			resource.setPersistentProperty(key, Boolean.toString(state));
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
	}
	
	private boolean getAlignmentHelperState() {
		boolean state = false;
		IResource resource = ObjectsController.getResourceForObject(((ResourceEditorInput)getEditorInput()).getObject());
		if(resource == null && ((ResourceEditorInput)getEditorInput()).getObject() instanceof IResource) resource = (IResource) ((ResourceEditorInput)getEditorInput()).getObject();
		QualifiedName key = new QualifiedName(AbstractScriptSegmentEditor.this.getClass().getCanonicalName(), "snapState");
		try {
			String alignmentHelperString = resource.getPersistentProperty(key);
			state = alignmentHelperString == null ? false :Boolean.parseBoolean(alignmentHelperString);
			state = Boolean.parseBoolean(alignmentHelperString);
		} catch (CoreException e) {
			setGridState(false);
		}
		return state;
	}
	
	private void setAlignmentHelperState(boolean state) {
		try {
			IResource resource = ObjectsController.getResourceForObject(((ResourceEditorInput)getEditorInput()).getObject());
			if(resource == null && ((ResourceEditorInput)getEditorInput()).getObject() instanceof IResource) resource = (IResource) ((ResourceEditorInput)getEditorInput()).getObject();
			QualifiedName key = new QualifiedName(AbstractScriptSegmentEditor.this.getClass().getCanonicalName(), "snapState");
			resource.setPersistentProperty(key, Boolean.toString(state));
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}
	
	@Override
	protected void createGraphicalViewer(Composite parent) {
		rulerComp = new RulerComposite(parent, SWT.NONE);
		super.createGraphicalViewer(rulerComp);
		rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
		getEditDomain().addViewer(getGraphicalViewer());
		getGraphicalViewer().getControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> editPartMap = getGraphicalViewer().getEditPartRegistry();
				ScriptSegmentEditPart scriptSegmentEditPart = null;
				if(scriptSegmentType.equals(ScriptSegmentType.INITIALIZE)) scriptSegmentEditPart = (ScriptSegmentEditPart) editPartMap.get(getScript().getInitializeBlocksContainer());
				if(scriptSegmentType.equals(ScriptSegmentType.LOOP)) scriptSegmentEditPart = (ScriptSegmentEditPart) editPartMap.get(getScript().getLoopBlocksContainer());
				if(scriptSegmentType.equals(ScriptSegmentType.FINALIZE)) scriptSegmentEditPart = (ScriptSegmentEditPart) editPartMap.get(getScript().getFinalizeBlocksContainer());
//				if(scriptSegmentType.equals(ScriptSegmentType.DATA_PROCESSING)) scriptSegmentEditPart = (ScriptSegmentEditPart) editPartMap.get(getScript().getLoopBlocksContainer());
				if(((StructuredSelection)scriptSegmentEditPart.getViewer().getSelection()).getFirstElement() instanceof BlockEditPart) editBlockAction.run();
//				try {
//					if(scriptSegmentType.equals(ScriptSegmentType.INITIALIZE)) System.out.println(process.getScript().getInitializeCode(process));
//					if(scriptSegmentType.equals(ScriptSegmentType.LOOP)) System.out.println(process.getScript().getLoopCode(process));
//					if(scriptSegmentType.equals(ScriptSegmentType.FINALIZE)) System.out.println(process.getScript().getFinalizeCode(process));
//				} catch (Exception e) {
//					e.printStackTrace();
//					Activator.logErrorMessageWithCause(e);
//				}
				
			}
		});
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(getEditorInput() != null && part.getSite().getPage().getActiveEditor() != null)
			if(getEditorInput().equals(part.getSite().getPage().getActiveEditor().getEditorInput())) {
				updateActions(getSelectionActions());
			}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void createActions() {
		super.createActions();
//		ScriptSegment scriptSegment = null;
//		if(scriptSegmentType.equals(ScriptSegmentType.INITIALIZE)) scriptSegment = process.getInitializeBlocksContainer();
//		if(scriptSegmentType.equals(ScriptSegmentType.LOOP)) scriptSegment = process.getLoopBlocksContainer();
//		if(scriptSegmentType.equals(ScriptSegmentType.FINALIZE)) scriptSegment = process.getFinalizeBlocksContainer();
//		if(copyAction == null) {
		ActionRegistry registry = getActionRegistry();
		copyAction = new CopyAction(this);
		copyAction.setText(DocometreMessages.CopyAction_Text);
		registry.registerAction(copyAction);
		getSelectionActions().add(copyAction.getId());
		pasteAction = new PasteAction(this, getEditDomain().getCommandStack());
		registry.registerAction(pasteAction);
		getSelectionActions().add(pasteAction.getId());
		editBlockAction = new EditBlockAction(this); 
		registry.registerAction(editBlockAction);
	    getSelectionActions().add(editBlockAction.getId());
	    deactivateBlockAction = new DeactivateBlockAction(this);
	    registry.registerAction(deactivateBlockAction);
	    getSelectionActions().add(deactivateBlockAction.getId());
	    
	    Action action = (Action) registry.getAction(ActionFactory.DELETE.getId());
	    action.setText(DocometreMessages.DeleteAction_Text);
	    
	    action = (Action) registry.getAction(ActionFactory.UNDO.getId());
	    action.setText(DocometreMessages.Undo);
	    
	    action = (Action) registry.getAction(ActionFactory.REDO.getId());
	    action.setText(DocometreMessages.Redo);
	    
//		}
	}
	
	
	
	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		getGraphicalViewer().setEditPartFactory(new ScriptEditPartFactory());
		getGraphicalViewer().setRootEditPart(new ScalableFreeformRootEditPart());
		getGraphicalViewer().setKeyHandler(new GraphicalViewerKeyHandler(getGraphicalViewer()));
		getGraphicalViewer().setContextMenu(new ScriptSegmentEditorContextMenuProvider(getGraphicalViewer()));
		getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);

		getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, getGridState());
		getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, getGridState());
		getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, getAlignmentHelperState());
		
//		List<String> zoomContributions = Arrays.asList(new String[] {ZoomManager.FIT_ALL, ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH });
//		((ScalableFreeformRootEditPart)getGraphicalViewer().getRootEditPart()).getZoomManager().setZoomLevelContributions(zoomContributions);
//		IAction zoomIn = new ZoomInAction(((ScalableFreeformRootEditPart)getGraphicalViewer().getRootEditPart()).getZoomManager());
//		IAction zoomOut = new ZoomOutAction(((ScalableFreeformRootEditPart)getGraphicalViewer().getRootEditPart()).getZoomManager());
//		getActionRegistry().registerAction(zoomIn);
//		getActionRegistry().registerAction(zoomOut);
//		getSite().getKeyBindingService().registerAction(zoomIn);
//		getSite().getKeyBindingService().registerAction(zoomOut);
		
//		IAction showRulers = new ToggleRulerVisibilityAction(
//				getGraphicalViewer());
//		getActionRegistry().registerAction(showRulers);
		
//		IAction snapAction = new ToggleSnapToGeometryAction(
//				getGraphicalViewer());
//		getActionRegistry().registerAction(snapAction);

//		IAction showGrid = new ToggleGridAction(getGraphicalViewer());
//		getActionRegistry().registerAction(showGrid);
		
	}
	
	@Override
	protected Control getGraphicalControl() {
		return rulerComp;
	}

	@Override
	protected PaletteRoot getPaletteRoot() {
		PaletteRoot scriptEditorPalette = new PaletteRoot();
		
		PaletteToolbar paletteToolbar = new PaletteToolbar("Tools");
		
		ToolEntry panningTool = new PanningSelectionToolEntry();
		paletteToolbar.add(panningTool);

		ToolEntry marqueeTool = new MarqueeToolEntry();
		paletteToolbar.add(marqueeTool);
		
		scriptEditorPalette.add(paletteToolbar);
		scriptEditorPalette.setDefaultEntry(panningTool);
		
		paletteDrawer = new PaletteDrawer(DocometreMessages.PaletteDrawerTitle);
		
		scriptEditorPalette.add(paletteDrawer);
		
		
		PaletteSeparator sep = new PaletteSeparator("org.eclipse.gef.examples.flow.flowplugin.sep2");
		sep.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
		scriptEditorPalette.add(sep);
//		SimpleFactory connectionFactory = new SimpleFactory(BlocksConnection.class) {
//			@Override
//			public Object getNewObject() {
//				BlocksConnection ifBloc = new BlocksConnection(sourceBlock, targetBlock)
//				return ifBloc;
//			}
//		};
		
		ToolEntry connectionTool = new ConnectionCreationToolEntry(DocometreMessages.ConnectionToolTitle, DocometreMessages.ConnectionToolDescription, null, 
																						Activator.getImageDescriptor(IImageKeys.NEW_CONNECTION_16_ICON), 
																						Activator.getImageDescriptor(IImageKeys.NEW_CONNECTION_32_ICON));
		
		paletteDrawer.add(connectionTool);
		return scriptEditorPalette;
		
	}

	protected void addCreationEntry(String label, String shortDescription, CreationFactory creationFactory, ImageDescriptor smallIcon, ImageDescriptor largeIcon) {
		CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(label, shortDescription, creationFactory, smallIcon, largeIcon);
		paletteDrawer.add(component);
	}
	
	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		//Add specific factories here
		CreationFactory ifCreationFactory = CreateEntryFactories.getIfBlockFactory(getScript());
		CreationFactory doCreationFactory = CreateEntryFactories.getDoBlockFactory(getScript());
		CreationFactory functionCreationFactory = CreateEntryFactories.getFunctionFactory(getScript());
		CreationFactory commentCreationFactory = CreateEntryFactories.getCommentBlockFactory(getScript());
		addCreationEntry(DocometreMessages.IfBlockToolTitle, DocometreMessages.IfBlockToolDescription, ifCreationFactory, Activator.getImageDescriptor(IImageKeys.IF_BLOCK_16_ICON), Activator.getImageDescriptor(IImageKeys.IF_BLOCK_32_ICON));
		addCreationEntry(DocometreMessages.DoBlockToolTitle, DocometreMessages.DoBlockToolDescription, doCreationFactory, Activator.getImageDescriptor(IImageKeys.DO_BLOCK_16_ICON), Activator.getImageDescriptor(IImageKeys.DO_BLOCK_32_ICON));
		addCreationEntry(DocometreMessages.FunctionBlockToolTitle, DocometreMessages.FunctionBlockToolDescription, functionCreationFactory, Activator.getImageDescriptor(IImageKeys.FUNCTION_BLOCK_16_ICON), Activator.getImageDescriptor(IImageKeys.FUNCTION_BLOCK_32_ICON));
		addCreationEntry(DocometreMessages.CommentBlockToolTitle, DocometreMessages.CommentBlockToolDescription, commentCreationFactory, Activator.getImageDescriptor(IImageKeys.COMMENT_BLOCK_16_ICON), Activator.getImageDescriptor(IImageKeys.COMMENT_BLOCK_32_ICON));
		
		if(scriptSegmentType.equals(ScriptSegmentType.INITIALIZE)) getGraphicalViewer().setContents(getScript().getInitializeBlocksContainer());
		if(scriptSegmentType.equals(ScriptSegmentType.LOOP)) getGraphicalViewer().setContents(getScript().getLoopBlocksContainer());
		if(scriptSegmentType.equals(ScriptSegmentType.FINALIZE)) getGraphicalViewer().setContents(getScript().getFinalizeBlocksContainer());
//		if(scriptSegmentType.equals(ScriptSegmentType.DATA_PROCESSING)) getGraphicalViewer().setContents(getScript().getLoopBlocksContainer());
		
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		getCommandStack().markSaveLocation();
	}
	
	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(PROP_DIRTY);
		super.commandStackChanged(event);
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
		if(type == ZoomManager.class) {
			MultiPageEditorPart processEditor = ((MultiPageEditorPart) (getSite().getPage().getActiveEditor()));
			AbstractScriptSegmentEditor abstractSegmentProcessEditor = (AbstractScriptSegmentEditor) processEditor.getSelectedPage();
			GraphicalViewer graphicalViewer = abstractSegmentProcessEditor.getGraphicalViewer();
			return graphicalViewer.getProperty(ZoomManager.class.toString());
		}
		if(type == GraphicalViewer.class) {
			MultiPageEditorPart processEditor = ((MultiPageEditorPart) (getSite().getPage().getActiveEditor()));
			if(processEditor.getSelectedPage() instanceof AbstractScriptSegmentEditor) {
				AbstractScriptSegmentEditor abstractSegmentProcessEditor = (AbstractScriptSegmentEditor) processEditor.getSelectedPage();
				GraphicalViewer graphicalViewer = abstractSegmentProcessEditor.getGraphicalViewer();
				return graphicalViewer;
			}
			
		}
		return super.getAdapter(type);
	}
	
	public void updatePasteAction() {
//		System.out.println("update PasteAction");
		ScriptSegment scriptSegment = null;
		if(scriptSegmentType.equals(ScriptSegmentType.INITIALIZE)) scriptSegment = getScript().getInitializeBlocksContainer();
		if(scriptSegmentType.equals(ScriptSegmentType.LOOP)) scriptSegment = getScript().getLoopBlocksContainer();
		if(scriptSegmentType.equals(ScriptSegmentType.FINALIZE)) scriptSegment = getScript().getFinalizeBlocksContainer();
//		if(scriptSegmentType.equals(ScriptSegmentType.DATA_PROCESSING)) scriptSegment = getScript().getLoopBlocksContainer();
		PasteAction.scriptSegment = scriptSegment;
//		rulerComp.requestLayout();
//		rulerComp.layout(true);
//		rulerComp.redraw();
//		rulerComp.update();
		rulerComp.getParent().update();
		
	}
	
	protected abstract Script getScript();

}
