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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;

public class SelectAllButtonHandler extends SelectionAdapter {
	
	private Button button;
	private AbstractElement element;
	private ChannelProperties property;
	private ObjectUndoContext undoContext;

	public SelectAllButtonHandler(Button button, AbstractElement element, ChannelProperties property, ObjectUndoContext undoContext) {
		this.button = button;
		this.element = element;
		this.property = property;
		this.undoContext = undoContext;
	}
	
	@Override
	public void widgetSelected(SelectionEvent event) {
		boolean isArduino  = false;
		Channel[] channels = new Channel[0];
		if(element instanceof DACQConfiguration) {
			channels = ((DACQConfiguration)element).getVariables();
			isArduino = element instanceof ArduinoUnoDACQConfiguration;
		}
		if(element instanceof Module) {
			channels = ((Module)element).getChannels();
			isArduino = ((Module)element).getDACQConfiguration() instanceof ArduinoUnoDACQConfiguration;
		} 
		if(channels.length == 0) return;
		boolean value = Boolean.parseBoolean(channels[0].getProperty(property));
		boolean different = false;
		for (Channel channel : channels) {
			boolean test = !isArduino && Boolean.parseBoolean(channel.getProperty(property)) != value;
			test = test || isArduino && Boolean.parseBoolean(channel.getProperty(property)) != value && Boolean.parseBoolean(channel.getProperty(ArduinoUnoChannelProperties.USED));
			if(test) {
				different = true;
				break;
			}
		}
		value = different?true:!value;
		for (Channel channel : channels) {
			try {
				String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, property.getLabel() + " : " + channel.toString());
				IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
				ModifyPropertyOperation modifyPropertyOperation = new ModifyPropertyOperation(button, label, property, channel, Boolean.toString(value), undoContext);
				operationHistory.execute(modifyPropertyOperation, null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			} 
		}
	}

}
