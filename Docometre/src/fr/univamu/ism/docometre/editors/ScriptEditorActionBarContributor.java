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

import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

public class ScriptEditorActionBarContributor extends ActionBarContributor {
	
	public ScriptEditorActionBarContributor() {
		// TODO Auto-generated constructor stub
	}

//	@Override
	protected void buildActions() {
		addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());
        addRetargetAction(new DeleteRetargetAction());
        addRetargetAction((RetargetAction) ActionFactory.COPY.create(getPage().getWorkbenchWindow()));
        addRetargetAction((RetargetAction) ActionFactory.PASTE.create(getPage().getWorkbenchWindow()));
        addRetargetAction((RetargetAction) ActionFactory.SELECT_ALL.create(getPage().getWorkbenchWindow()));
//        addRetargetAction(new ZoomInRetargetAction());
//		addRetargetAction(new ZoomOutRetargetAction());
//		
//		addRetargetAction(new RetargetAction(
//				GEFActionConstants.TOGGLE_RULER_VISIBILITY,
//				"TOGGLE_RULER_VISIBILITY", IAction.AS_CHECK_BOX));
//
//		addRetargetAction(new RetargetAction(
//				GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY,
//				"TOGGLE_SNAP_TO_GEOMETRY", IAction.AS_CHECK_BOX));
//
//		addRetargetAction(new RetargetAction(
//				GEFActionConstants.TOGGLE_GRID_VISIBILITY,
//				"TOGGLE_GRID_VISIBILITY", IAction.AS_CHECK_BOX));
        
	}
	
	@Override
	protected void declareGlobalActionKeys() {
//		addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
//		addGlobalActionKey(ActionFactory.COPY.getId());
//		addGlobalActionKey(ActionFactory.PASTE.getId());
//		addGlobalActionKey(ActionFactory.DELETE.getId());
//		addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
//		addGlobalActionKey(ActionFactory.DELETE.getId());
	}
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
//		toolBarManager.add(getAction(ActionFactory.UNDO.getId()));
//        toolBarManager.add(getAction(ActionFactory.REDO.getId()));
//        toolBarManager.add(getAction(ActionFactory.DELETE.getId()));  
//        toolBarManager.add(getAction(GEFActionConstants.ZOOM_IN));
//        toolBarManager.add(getAction(GEFActionConstants.ZOOM_OUT));
//        toolBarManager.add(new Separator());
//		String[] zoomStrings = new String[] { ZoomManager.FIT_ALL,
//				ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH };
//		toolBarManager.add(new ZoomComboContributionItem(getPage(), zoomStrings));                
//        toolBarManager.add(new ZoomComboContributionItem(getPage()));
	}
	
//	@Override
//	public void contributeToMenu(IMenuManager menuManager) {
//		MenuManager viewMenu = new MenuManager("&View");
//		viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
//		viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
//		viewMenu.add(new Separator());
//		viewMenu.add(getAction(GEFActionConstants.TOGGLE_RULER_VISIBILITY));
//		viewMenu.add(getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY));
//		viewMenu.add(getAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY));
//		viewMenu.add(new Separator());
//		viewMenu.add(getAction(GEFActionConstants.MATCH_SIZE));
//		viewMenu.add(getAction(GEFActionConstants.MATCH_WIDTH));
//		viewMenu.add(getAction(GEFActionConstants.MATCH_HEIGHT));
//		menuManager.add(viewMenu);
//		menuManager.insertAfter(IWorkbenchActionConstants.M_EDIT, viewMenu);
//	}

}
