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
package fr.univamu.ism.docometre.scripteditor.commands;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.gef.commands.Command;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.ScriptSegment;

public class ModifyFunctionalBlockCommand extends Command {

	private ScriptSegment scriptSegment;
	private Function function;
	private HashMap<String, String> newProperties = new HashMap<>();
	private HashMap<String, String> oldProperties = new HashMap<>();
	private BlockEditPart blockEditPart;

	public ModifyFunctionalBlockCommand(BlockEditPart blockEditPart) {
		this.scriptSegment = (ScriptSegment) blockEditPart.getParent().getModel();
		this.function = (Function) blockEditPart.getModel();
		this.blockEditPart = blockEditPart;
		// Copy transient properties in new properties
		Set<String> keySet = function.getTransientProperties().keySet();
		for (String key : keySet) {
			newProperties.put(key, function.getTransientProperties().get(key));
		}
		// Save old properties
		keySet = function.getProperties().keySet();
		for (String key : keySet) {
			oldProperties.put(key, function.getProperties().get(key));
		}
	}
	
	@Override
	public void execute() {
		activateSegmentProcessEditor();
		Set<String> keySet = newProperties.keySet();
		for (String key : keySet) {
			function.setProperty(key, newProperties.get(key));
		}
		blockEditPart.updateFigureLabel();
	}
	
	@Override
	public void undo() {
		activateSegmentProcessEditor();
		Set<String> keySet = oldProperties.keySet();
		if(keySet.isEmpty()) {
			function.getProperties().clear();
		} else for (String key : keySet) {
			function.setProperty(key, oldProperties.get(key));
		}
		blockEditPart.updateFigureLabel();
	}
	
	private void activateSegmentProcessEditor() {
		Activator.activateScriptSegmentEditor(scriptSegment);
	}

}
