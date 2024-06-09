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
package fr.univamu.ism.docometre.scripteditor.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gef.Request;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.action.IAction;

import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Function;

public class AssignFunctionAction extends WorkbenchPartAction {
	
	public static final String REQ_CHANGE_FUNCTIONAL_BLOCK = "REQ_CHANGE_FUNCTIONAL_BLOCK";
	public static final String FUNCTIONS_MENU_ACTION = "FunctionsMenuAction";

	private BlockEditPart selectedBlockEditPart;
	private Request requestChangeFunctionalBlock;
	private String functionClass;
	
	public AssignFunctionAction(AbstractScriptSegmentEditor scriptSegmentEditor, BlockEditPart blockEditPart, String title, String toolTip, String functionClass) {
		super(scriptSegmentEditor, IAction.AS_CHECK_BOX);
		setText(title);
		setToolTipText(toolTip);
		requestChangeFunctionalBlock = new Request(REQ_CHANGE_FUNCTIONAL_BLOCK);
		this.functionClass = functionClass;
		selectedBlockEditPart = blockEditPart;
		if(((Function)blockEditPart.getModel()).getClassName() != null) {
			String className = ((Function)blockEditPart.getModel()).getClassName();
			if(className.equals(functionClass)) setChecked(true);
		}
	}
	
	@Override
	public void run() {
		Map<String, Object> data = new HashMap<>();
		data.put(Function.FUNCTION_CLASS_NAME, functionClass);
		data.put(Function.FUNCTION_NAME, getText());
		requestChangeFunctionalBlock.setExtendedData(data);
		execute(selectedBlockEditPart.getCommand(requestChangeFunctionalBlock));
		
		
		ActionRegistry actionRegistry = getWorkbenchPart().getAdapter(ActionRegistry.class);
		actionRegistry.getAction(EditBlockAction.EDIT_BLOCK).run();
		
	}
	
	@Override
	protected boolean calculateEnabled() {
		return isEnabled();
	}


}
