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
package fr.univamu.ism.docometre;

import org.eclipse.core.commands.operations.IOperationApprover;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * An operation approver that prompts the user to see whether an undo or redo
 * should continue. An example preference is checked to determine if prompting
 * should occur.
 */
public final class UndoRedoUserApprover implements IOperationApprover {

	private IUndoContext context;

	/*
	 * Create the operation approver.
	 */
	public UndoRedoUserApprover(IUndoContext context) {
		super();
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.IOperationApprover#proceedRedoing
	 * (org.eclipse.core.commands.operations.IUndoableOperation,
	 * org.eclipse.core.commands.operations.IOperationHistory,
	 * org.eclipse.core.runtime.IAdaptable)
	 */
	public IStatus proceedRedoing(IUndoableOperation operation, IOperationHistory history, IAdaptable uiInfo) {

		// return immediately if the operation is not relevant
		if (!operation.hasContext(context))
			return Status.OK_STATUS;

		// allow the operation if we are not prompting
		boolean prompt = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.PREF_CONFIRM_UNDO);
		if (!prompt)
			return Status.OK_STATUS;
		return prompt(false, operation, uiInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.IOperationApprover#proceedUndoing
	 * (org.eclipse.core.commands.operations.IUndoableOperation,
	 * org.eclipse.core.commands.operations.IOperationHistory,
	 * org.eclipse.core.runtime.IAdaptable)
	 */
	public IStatus proceedUndoing(IUndoableOperation operation, IOperationHistory history, IAdaptable uiInfo) {

		// return immediately if the operation is not relevant
		if (!operation.hasContext(context)) return Status.OK_STATUS;

		// allow the operation if we are not prompting
		boolean prompt = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.PREF_CONFIRM_UNDO);
		if (!prompt) return Status.OK_STATUS;
		return prompt(true, operation, uiInfo);
	}

	/*
	 * Prompt the user as to whether to continue the undo or redo, and return an
	 * OK_STATUS if we should continue, or a CANCEL_STATUS if we should not.
	 */
	private IStatus prompt(boolean undoing, IUndoableOperation operation, IAdaptable uiInfo) {
		boolean createdShell = false;
		Shell shell = getShell(uiInfo);
		if (shell == null) {
			if (shell == null) {
				createdShell = true;
				shell = new Shell();
			}
		}
		String command = undoing ? DocometreMessages.Undo : DocometreMessages.Redo;
		String message = NLS.bind(DocometreMessages.ConfirmUndo, command, operation.getLabel());
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(shell, DocometreMessages.Title, message,
				DocometreMessages.DoNotConfirm, false, null, null);
		Activator.getDefault().getPreferenceStore().setValue(GeneralPreferenceConstants.PREF_CONFIRM_UNDO, !dialog.getToggleState());

		if (createdShell)
			shell.dispose();
		if (dialog.getReturnCode() == Window.OK)
			return Status.OK_STATUS;
		return Status.CANCEL_STATUS;
	}

	/*
	 * Return the shell described by the supplied uiInfo, or null if no shell is
	 * described.
	 */
	Shell getShell(IAdaptable uiInfo) {
		if (uiInfo != null) {
			Shell shell = (Shell) uiInfo.getAdapter(Shell.class);
			if (shell != null)
				return shell;
		}
		return null;
	}
}
