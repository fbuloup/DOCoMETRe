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
import java.util.List;
import java.util.Map;

import org.eclipse.gef.Request;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dialogs.CommentBlockConfigurationDialog;
import fr.univamu.ism.docometre.dialogs.ConditionalBlockConfigurationDialog;
import fr.univamu.ism.docometre.dialogs.FunctionalBlockConfigurationDialog;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Comment;
import fr.univamu.ism.process.ConditionalBlock;
import fr.univamu.ism.process.Function;

public class EditBlockAction extends SelectionAction {
	
	public static final String EDIT_BLOCK = "EditBlock";
	public static final String REQ_EDIT_CONDITIONAL_BLOCK = "REQ_EDIT_CONDITIONAL_BLOCK";
	public static final String REQ_EDIT_FUNCTIONAL_BLOCK = "REQ_EDIT_FUNCTIONAL_BLOCK";
	public static final String REQ_EDIT_COMMENT_BLOCK = "REQ_EDIT_COMMENT_BLOCK";
	
	private Request requestEditConditionalBlock;
	private Request requestEditFunctionalBlock;
	private Request requestEditCommentBlock;
	private DACQConfiguration dacqConfiguration;
	private Process process;

	public EditBlockAction(IWorkbenchPart part) {
		super(part);
		setId(EDIT_BLOCK);
		setText(DocometreMessages.EditToolTipTitle);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.EDIT_ICON));
		requestEditConditionalBlock = new Request(REQ_EDIT_CONDITIONAL_BLOCK);
		requestEditFunctionalBlock = new Request(REQ_EDIT_FUNCTIONAL_BLOCK);
		requestEditCommentBlock = new Request(REQ_EDIT_COMMENT_BLOCK);
		AbstractScriptSegmentEditor scriptSegmentEditor = (AbstractScriptSegmentEditor)getWorkbenchPart();
		process = (Process)((ResourceEditorInput)scriptSegmentEditor.getEditorInput()).getObject();
		this.dacqConfiguration = process.getDACQConfiguration();
	}
	
	@Override
	public void run() {
		if(!(getSelectedObjects().get(0) instanceof BlockEditPart)) return;
		BlockEditPart selectedBlockEditPart = (BlockEditPart) getSelectedObjects().get(0);
		Block block = selectedBlockEditPart.getModel();
		if(block instanceof ConditionalBlock) {
			ConditionalBlockConfigurationDialog configurationDialog = new ConditionalBlockConfigurationDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), (ConditionalBlock) block, dacqConfiguration.getProposal());
			if(configurationDialog.open() == Window.OK) {
				Map<String, Object> data = new HashMap<>();
				data.put(ConditionalBlock.LEFT_OPERAND, configurationDialog.getLeftOperand());
				data.put(ConditionalBlock.OPERATOR, configurationDialog.getOperator());
				data.put(ConditionalBlock.RIGHT_OPERAND, configurationDialog.getRightOperand());
				requestEditConditionalBlock.setExtendedData(data);
				execute(selectedBlockEditPart.getCommand(requestEditConditionalBlock));
			};
		}
		if(block instanceof Function) {
			Function function = (Function)block;
			if(function.getClassName() != null && !(function.getClassName().equals(""))) {
				FunctionalBlockConfigurationDialog configurationDialog = new FunctionalBlockConfigurationDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), process, function);
				if(configurationDialog.open() == Window.OK) {
					execute(selectedBlockEditPart.getCommand(requestEditFunctionalBlock));
				}
			}
		}
		if(block instanceof Comment) {
			Comment comment = (Comment)block;
			CommentBlockConfigurationDialog configurationDialog = new CommentBlockConfigurationDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),  comment);
			if(configurationDialog.open() == Window.OK) {
				Map<String, Object> data = new HashMap<>();
				data.put(Comment.COMMENT, configurationDialog.getComment());
				requestEditCommentBlock.setExtendedData(data);
				execute(selectedBlockEditPart.getCommand(requestEditCommentBlock));
			}
			
		}
	}

	@Override
	protected boolean calculateEnabled() {
		@SuppressWarnings("rawtypes")
		List selectedObjects = getSelectedObjects();
		if(selectedObjects.size() == 1) {
			if(selectedObjects.get(0) instanceof BlockEditPart) return true;
		}
		return false;
	}

}
