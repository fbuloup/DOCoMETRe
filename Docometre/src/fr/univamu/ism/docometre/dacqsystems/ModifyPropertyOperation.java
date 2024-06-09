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
package fr.univamu.ism.docometre.dacqsystems;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariable;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannel;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoVariable;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ModifyPropertyOperation extends AbstractOperation {

	private Property property;
	private String newValue;
	private String oldValue;
	private AbstractElement element;
	private Object uiElement;
	
	public ModifyPropertyOperation(Object uiElement, String label, Property property, AbstractElement element, String newValue, IUndoContext undoContext) {
		super(label);
		this.uiElement = uiElement;
		this.property = property;
		this.newValue = newValue;
		this.element = element;
		oldValue = element.getProperty(property);
		addContext(undoContext);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		boolean applyChange = true;
		if(ArduinoUnoChannelProperties.USED.equals(property) && "true".equals(newValue)) {
			applyChange = !checkIfAlreadyUsed();
		}
		if(applyChange) {
			element.setProperty(property, newValue);
			updateChannelsTransferNumber();
			boolean globalFrequencyChanged = ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.equals(property);
			globalFrequencyChanged = globalFrequencyChanged || ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.equals(property);
			if(globalFrequencyChanged) changeChannelsFrequencies(true);
			return Status.OK_STATUS;
		}
		return Status.CANCEL_STATUS;
	}

	private void changeChannelsFrequencies(boolean toNewValue) {
		DACQConfiguration dacqConfiguration = (DACQConfiguration)element;
		Module[] modules = dacqConfiguration.getModules();
		double ratio = Double.parseDouble(newValue)/Double.parseDouble(oldValue);
		if(!toNewValue) ratio = 1/ratio;
		for (Module module : modules) {
			Channel[] channels = module.getChannels();
			for (Channel channel : channels) {
				double sf = Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
				double nsf = ratio*sf;
				channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, String.valueOf(nsf));
			}
		}
		Channel[] variables = null;
		if(dacqConfiguration instanceof ADWinDACQConfiguration) {
			variables = ((ADWinDACQConfiguration) dacqConfiguration).getVariables();
		};
		if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) {
			variables = ((ArduinoUnoDACQConfiguration) dacqConfiguration).getVariables();
		}
		for (Channel variable : variables) {
			double sf = Double.parseDouble(variable.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
			double nsf = ratio*sf;
			variable.setProperty(ChannelProperties.SAMPLE_FREQUENCY, String.valueOf(nsf));
		}
	}

	private boolean checkIfAlreadyUsed() {
		if(element instanceof ArduinoUnoChannel) {
			ArduinoUnoChannel arduinoUnoChannel = (ArduinoUnoChannel)element;
			String channelNumber = arduinoUnoChannel.getProperty(ChannelProperties.CHANNEL_NUMBER);
			Module module = arduinoUnoChannel.getModule();
			DACQConfiguration dacqConfiguration = arduinoUnoChannel.getModule().getDACQConfiguration();
			Module[] modules = dacqConfiguration.getModules();
			for (Module otherModule : modules) {
				if(otherModule != module && (otherModule instanceof ArduinoUnoAnOutModule || otherModule instanceof ArduinoUnoDigInOutModule) && !(module instanceof ArduinoUnoADS1115Module)) {
					Channel[] channels = otherModule.getChannels();
					for (Channel channel : channels) {
						if(channel.getProperty(ChannelProperties.CHANNEL_NUMBER).equals(channelNumber) && channel.getProperty(ArduinoUnoChannelProperties.USED).equals("true")) {
							Activator.logWarningMessage("This channel is alread used : " + otherModule.getClass().getSimpleName() + " !");
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		element.setProperty(property, oldValue);
		updateChannelsTransferNumber();
		boolean globalFrequencyChanged = ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY.equals(property);
		globalFrequencyChanged = globalFrequencyChanged || ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY.equals(property);
		if(globalFrequencyChanged) changeChannelsFrequencies(false);
		return Status.OK_STATUS;
	}

	private void updateChannelsTransferNumber() {
		boolean updateChannelsTransferNumber = ChannelProperties.TRANSFER.equals(property);
		updateChannelsTransferNumber = updateChannelsTransferNumber || ChannelProperties.AUTO_TRANSFER.equals(property);
		updateChannelsTransferNumber = updateChannelsTransferNumber || ChannelProperties.RECORD.equals(property);
		updateChannelsTransferNumber = updateChannelsTransferNumber || ArduinoUnoChannelProperties.USED.equals(property);
		if(updateChannelsTransferNumber) {
			if(element instanceof ADWinVariable) {
				((ADWinVariable)element).getDACQConfiguration().updateChannelsTransferNumber();
			} else if(element instanceof ArduinoUnoVariable) { 
				((ArduinoUnoVariable)element).getDACQConfiguration().updateChannelsTransferNumber();
			} else ((Channel)element).getModule().getDACQConfiguration().updateChannelsTransferNumber();
		}
	}
	
	private void setFocus() {
		if(uiElement instanceof Control) {
			Control control = ((Control)uiElement);
			if(control != null && !control.isDisposed()) {
				Object object = (Object) control.getData("module");
				String id = "";
				if(object instanceof Module) id = Integer.toString(System.identityHashCode(object));
				else if(object instanceof String) id = (String)object;
				ResourceEditor activeEditor = (ResourceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				activeEditor.setActivePage(id);
				control.setFocus();			
				if(control instanceof Table || control.getData("TABLEVIEWER") instanceof TableViewer) {
					if(control instanceof Table) {
						object = control.getData(Integer.toString(this.hashCode()));
						if(object instanceof Integer) {
							int lineNumber = (int) object;
							((Table)control).setSelection(lineNumber);
						}
					} else {
						TableViewer tableViewer = ((TableViewer)control.getData("TABLEVIEWER"));
						tableViewer.getTable().setFocus();
						for (int i = 0; i < tableViewer.getTable().getItemCount(); i++) {
							if(tableViewer.getElementAt(i) == element) {
								tableViewer.getTable().setSelection(i);
								break;
							}
						}
					}
				}
			}
		}
	}

}
