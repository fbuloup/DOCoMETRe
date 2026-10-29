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
package fr.univamu.ism.docometre.scripteditor.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dialogs.GlobalDeclarationsDialog;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.commands.ModifyGlobalDeclarationsCommand;
import fr.univamu.ism.docometre.scripteditor.figures.GlobalDeclarationsFigure;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.BlocksListener;
import fr.univamu.ism.process.GlobalDeclarations;
import fr.univamu.ism.process.ScriptSegment;

/**
 * Part associated with global declarations in INITIALIZE segment editor.
 * It has no component policy, so it cannot be deleted, and no graphical node policy, so it cannot be connected.
 * Its location is fixed.
 */
public class GlobalDeclarationsEditPart extends AbstractGraphicalEditPart implements BlocksListener {
	
	// Maximum number of libraries, constants or subroutines listed in tooltip
	private static final int MAX_TOOLTIP_ITEMS = 5;
	
	public GlobalDeclarationsEditPart(GlobalDeclarations globalDeclarations) {
		setModel(globalDeclarations);
	}
	
	@Override
	public GlobalDeclarations getModel() {
		return (GlobalDeclarations) super.getModel();
	}

	@Override
	protected IFigure createFigure() {
		return new GlobalDeclarationsFigure();
	}

	@Override
	protected void createEditPolicies() {
		// Only selection is allowed, see ScriptSegmentEditPart layout policy
	}
	
	@Override
	public void activate() {
		getModel().getScript().addBlocksListener(this);
		super.activate();
	}
	
	@Override
	public void deactivate() {
		getModel().getScript().removeBlocksListener(this);
		super.deactivate();
	}
	
	@Override
	protected void refreshVisuals() {
		GlobalDeclarations globalDeclarations = getModel();
		GlobalDeclarationsFigure figure = (GlobalDeclarationsFigure) getFigure();
		List<String> libraries = new ArrayList<>();
		for (GlobalDeclarations.Library library : globalDeclarations.getLibraries()) libraries.add(library.getFile());
		List<String> constants = new ArrayList<>();
		for (GlobalDeclarations.Constant constant : globalDeclarations.getConstants()) constants.add(constant.getName() + " = " + constant.getValue());
		List<String> subroutines = new ArrayList<>();
		for (GlobalDeclarations.Subroutine subroutine : globalDeclarations.getSubroutines()) subroutines.add(subroutine.getName() + "(" + subroutine.getParameters() + ")");
		String toolTip = DocometreMessages.GlobalDeclarationsFigureToolTip + "\n";
		toolTip = toolTip + getToolTipSection(DocometreMessages.GlobalDeclarationsLibrariesTab, libraries);
		toolTip = toolTip + getToolTipSection(DocometreMessages.GlobalDeclarationsConstantsTab, constants);
		toolTip = toolTip + getToolTipSection(DocometreMessages.GlobalDeclarationsSubroutinesTab, subroutines);
		figure.update(globalDeclarations.getLibraries().size(), globalDeclarations.getConstants().size(), globalDeclarations.getSubroutines().size(), toolTip);
		Rectangle bounds = new Rectangle(GlobalDeclarations.X, GlobalDeclarations.Y, figure.getPreferredSize().width, GlobalDeclarations.HEIGHT);
		((GraphicalEditPart)getParent()).setLayoutConstraint(this, figure, bounds);
	}
	
	/*
	 * Tooltip is a summary : when it is too high to be displayed below the cursor, draw2d displays it above the cursor, 
	 * partly out of the screen as global declarations are at the top of the editor (see ToolTipHelper)
	 */
	private static String getToolTipSection(String title, List<String> items) {
		String section = "\n" + title + " : " + items.size();
		for (int i = 0; i < Math.min(items.size(), MAX_TOOLTIP_ITEMS); i++) section = section + "\n   " + items.get(i);
		if (items.size() > MAX_TOOLTIP_ITEMS) section = section + "\n   " + NLS.bind(DocometreMessages.GlobalDeclarationsMoreItems, items.size() - MAX_TOOLTIP_ITEMS);
		return section;
	}
	
	@Override
	public void performRequest(Request request) {
		if(request.getType() == RequestConstants.REQ_OPEN) edit();
	}
	
	/**
	 * Open global declarations dialog and apply modifications as an undoable command
	 */
	public void edit() {
		GlobalDeclarationsDialog dialog = new GlobalDeclarationsDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getContext(), getModel().getContent());
		if(dialog.open() == Window.OK && !dialog.getContent().equals(getModel().getContent())) {
			ScriptSegment scriptSegment = (ScriptSegment) getParent().getModel();
			getViewer().getEditDomain().getCommandStack().execute(new ModifyGlobalDeclarationsCommand(scriptSegment, dialog.getContent()));
		}
	}

	/**
	 * @return the process edited
	 */
	public Object getContext() {
		return ((ResourceEditorInput)((DefaultEditDomain)getViewer().getEditDomain()).getEditorPart().getEditorInput()).getObject();
	}

	@Override
	public void updateBlocks() {
		refreshVisuals();
	}

	@Override
	public void selectBlocks(Block[] blocks) {
	}

}
