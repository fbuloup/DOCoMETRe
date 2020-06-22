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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtchart.extensions.charts.Messages;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;

import fr.univamu.ism.docometre.AcquirePerspective;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;

public class DataEditorActionBarContributor extends EditorActionBarContributor implements IPerspectiveListener {
	
	private class EditGraphAction extends Action {
		public EditGraphAction() {
			super("Edit chart", Activator.getImageDescriptor(IImageKeys.EDIT_GRAPH_ICON));
			setToolTipText("Edit chart");
		}
		@Override
		public void run() {
			if(editor.getChart().getMenuItem(null, Messages.PROPERTIES) != null) {
				Event event = new Event();
				event.x = editor.getChart().getCurrentX_Pixel();
				event.type = SWT.Selection;
				event.widget = editor.getChart().getMenuItem(null, Messages.PROPERTIES);
				editor.getChart().handleEvent(event);
				editor.mouseUp(new MouseEvent(event));
			}
		}
	}
	
	private class PanLeftAction extends Action {
		public PanLeftAction() {
			super("Left panning", Activator.getImageDescriptor(IImageKeys.LEFT_PANNING));
			setToolTipText("Left panning");
		}
		@Override
		public void run() {
			Event event = new Event();
			event.x = editor.getChart().getCurrentX_Pixel();
			event.type = SWT.KeyDown;
			event.keyCode = SWT.ARROW_LEFT;
			event.widget = panLeftActionContributionItem.getWidget();
			editor.getChart().handleEvent(event);
			editor.mouseUp(new MouseEvent(event));
		}
	}
	
	private class PanUpAction extends Action {
		public PanUpAction() {
			super("Up panning", Activator.getImageDescriptor(IImageKeys.UP_PANNING));
			setToolTipText("Up panning");
		}
		@Override
		public void run() {
			Event event = new Event();
			event.x = editor.getChart().getCurrentX_Pixel();
			event.type = SWT.KeyDown;
			event.keyCode = SWT.ARROW_UP;
			event.widget = panUpActionContributionItem.getWidget();
			editor.getChart().handleEvent(event);
			editor.mouseUp(new MouseEvent(event));
		}
	}
	
	private class PanDownAction extends Action {
		public PanDownAction() {
			super("Down panning", Activator.getImageDescriptor(IImageKeys.DOWN_PANNING));
			setToolTipText("Down panning");
		}
		@Override
		public void run() {
			Event event = new Event();
			event.x = editor.getChart().getCurrentX_Pixel();
			event.type = SWT.KeyDown;
			event.keyCode = SWT.ARROW_DOWN;
			event.widget = panDownActionContributionItem.getWidget();
			editor.getChart().handleEvent(event);
			editor.mouseUp(new MouseEvent(event));
		}
	}
	
	private class PanRightAction extends Action {
		public PanRightAction() {
			super("Right panning", Activator.getImageDescriptor(IImageKeys.RIGHT_PANNING));
			setToolTipText("Right panning");
		}
		@Override
		public void run() {
			Event event = new Event();
			event.x = editor.getChart().getCurrentX_Pixel();
			event.type = SWT.KeyDown;
			event.keyCode = SWT.ARROW_RIGHT;
			event.widget = panRightActionContributionItem.getWidget();
			editor.getChart().handleEvent(event);
			editor.mouseUp(new MouseEvent(event));
		}
	}
	
	private class ZoomInGraphAction extends Action {
		public ZoomInGraphAction() {
			super("Zoom in", Activator.getImageDescriptor(IImageKeys.ZOOM_IN));
			setToolTipText("Zoom in");
		}
		@Override
		public void run() {
			if(editor.getChart().getMenuItem(null, Messages.ZOOMIN) != null) {
				Event event = new Event();
				event.x = editor.getChart().getCurrentX_Pixel();
				event.type = SWT.Selection;
				event.widget = editor.getChart().getMenuItem(null, Messages.ZOOMIN);
				editor.getChart().handleEvent(event);
				editor.mouseUp(new MouseEvent(event));
			}
		}
	}
	
	private class ZoomOutGraphAction extends Action {
		public ZoomOutGraphAction() {
			super("Zoom out", Activator.getImageDescriptor(IImageKeys.ZOOM_OUT));
			setToolTipText("Zoom out");
		}
		@Override
		public void run() {
			if(editor.getChart().getMenuItem(null, Messages.ZOOMOUT) != null) {
				Event event = new Event();
				event.x = editor.getChart().getCurrentX_Pixel();
				event.type = SWT.Selection;
				event.widget = editor.getChart().getMenuItem(null, Messages.ZOOMOUT);
				editor.getChart().handleEvent(event);
				editor.mouseUp(new MouseEvent(event));
			}
		}
	}

	private class AutoScaleGraphAction extends Action {
		public AutoScaleGraphAction() {
			super("Autoscale", Activator.getImageDescriptor(IImageKeys.AUTO_SCALE_ICON));
			setToolTipText("Autoscale");
		}
		@Override
		public void run() {
			if(editor.getChart().getMenuItem(null, Messages.ADJUST_AXIS_RANGE) != null) {
				Event event = new Event();
				event.x = editor.getChart().getCurrentX_Pixel();
				event.type = SWT.Selection;
				event.widget = editor.getChart().getMenuItem(null, Messages.ADJUST_AXIS_RANGE);
				editor.getChart().handleEvent(event);
				editor.mouseUp(new MouseEvent(event));
			}
		}
	}
	
	private DataEditor editor;
	private ActionContributionItem editGraphActionContributionItem;
	private ActionContributionItem autoScaleGraphActionContributionItem;
	private ActionContributionItem zoomInGraphActionContributionItem;
	private ActionContributionItem zoomOutGraphActionContributionItem;
	private ActionContributionItem panLeftActionContributionItem;
	private ActionContributionItem panUpActionContributionItem;
	private ActionContributionItem panDownActionContributionItem;
	private ActionContributionItem panRightActionContributionItem;
	private SubToolBarManager subToolBarManager;
	
	public DataEditorActionBarContributor() {
		editGraphActionContributionItem = new ActionContributionItem(new EditGraphAction());
		autoScaleGraphActionContributionItem = new ActionContributionItem(new AutoScaleGraphAction());
		zoomInGraphActionContributionItem = new ActionContributionItem(new ZoomInGraphAction());
		zoomOutGraphActionContributionItem = new ActionContributionItem(new ZoomOutGraphAction());
		panDownActionContributionItem = new ActionContributionItem(new PanDownAction());
		panLeftActionContributionItem = new ActionContributionItem(new PanLeftAction());
		panRightActionContributionItem = new ActionContributionItem(new PanRightAction());
		panUpActionContributionItem = new ActionContributionItem(new PanUpAction());
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		
		subToolBarManager = new SubToolBarManager(toolBarManager);
		
		subToolBarManager.add(editGraphActionContributionItem);
		subToolBarManager.add(new Separator());
		subToolBarManager.add(autoScaleGraphActionContributionItem);
		subToolBarManager.add(new Separator());
		subToolBarManager.add(zoomInGraphActionContributionItem);
		subToolBarManager.add(zoomOutGraphActionContributionItem);
		subToolBarManager.add(new Separator());
		subToolBarManager.add(panLeftActionContributionItem);
		subToolBarManager.add(panUpActionContributionItem);
		subToolBarManager.add(panDownActionContributionItem);
		subToolBarManager.add(panRightActionContributionItem);
	}
	
	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		editor = null;
		if(!(targetEditor instanceof DataEditor)) return;
		editor = (DataEditor) targetEditor;
		
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		boolean actionsVisibility = !perspective.getId().equals(AcquirePerspective.ID);
		
		editGraphActionContributionItem.setVisible(actionsVisibility);
		autoScaleGraphActionContributionItem.setVisible(actionsVisibility);
		zoomInGraphActionContributionItem.setVisible(actionsVisibility);
		zoomOutGraphActionContributionItem.setVisible(actionsVisibility);
		panDownActionContributionItem.setVisible(actionsVisibility);
		panLeftActionContributionItem.setVisible(actionsVisibility);
		panRightActionContributionItem.setVisible(actionsVisibility);
		panUpActionContributionItem.setVisible(actionsVisibility);
		
		subToolBarManager.setVisible(actionsVisibility);
		
		editGraphActionContributionItem.getAction().setEnabled(actionsVisibility);
		autoScaleGraphActionContributionItem.getAction().setEnabled(actionsVisibility);
		zoomInGraphActionContributionItem.getAction().setEnabled(actionsVisibility);
		zoomOutGraphActionContributionItem.getAction().setEnabled(actionsVisibility);
		panDownActionContributionItem.getAction().setEnabled(actionsVisibility);
		panLeftActionContributionItem.getAction().setEnabled(actionsVisibility);
		panRightActionContributionItem.getAction().setEnabled(actionsVisibility);
		panUpActionContributionItem.getAction().setEnabled(actionsVisibility);
		
		subToolBarManager.update(true);
		
		if(subToolBarManager.getParent() != null) {
			subToolBarManager.getParent().update(true);
		}
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub
		
	}

}
