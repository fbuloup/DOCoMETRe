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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor;

import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.forms.editor.FormEditor;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoModulesList;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ArduinoUnoModulePage extends ModulePage {

	public ArduinoUnoModulePage(FormEditor editor, String id, String title, Module module) {
		super(editor, id, title, module);
	}
	
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		super.update(property, newValue, oldValue, element);
		if(property == ChannelProperties.ADD || property == ChannelProperties.REMOVE) {
			if(module != null && tableViewer != null) tableViewer.setInput(module.getChannels());
			else if(tableViewer != null) tableViewer.setInput(((ArduinoUnoDACQConfiguration)dacqConfiguration).getVariables());
			if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
		}
		if(property == ArduinoUnoADS1115ModuleProperties.ADDRESS) {
			((ResourceEditor)getEditor()).updateTitle(getIndex(), getPageTitle());
//			if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
		}
		if(property instanceof ChannelProperties) {
			if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
		}

	}

	@Override
	public String getPageTitle() {
		firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
		return ArduinoUnoModulesList.getDescription(module);
		
	}

}
