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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCANModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCANModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ADWinCANModulePage extends ADWinModulePage {
	
	private class CANComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			Channel in1 = (Channel) e1;
			Channel in2 = (Channel) e2;
			switch (sortingColumnNumber) {
			case 0:
				e1 = in1.getProperty(ChannelProperties.NAME);
				e2 = in2.getProperty(ChannelProperties.NAME);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 1:
				e1 = in1.getProperty(ChannelProperties.TRANSFER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 2:
				e1 = in1.getProperty(ChannelProperties.AUTO_TRANSFER);
				e2 = in2.getProperty(ChannelProperties.AUTO_TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 3:
				e1 = in1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 4:
				e1 = in1.getProperty(ChannelProperties.RECORD);
				e2 = in2.getProperty(ChannelProperties.RECORD);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 5:
				e1 = in1.getProperty(ChannelProperties.BUFFER_SIZE);
				e2 = in2.getProperty(ChannelProperties.BUFFER_SIZE);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}

	private Combo interfaceNumberCombo;
	private Combo systemTypeCombo;
	private Text canNametext;
	private Combo frequencyCombo;
	private Combo nbSensorsCombo;
	private Combo modeCombo;
	private Text messageObjectText;
	private Combo messageIDLengthCombo;
	private Text messageIDtext;
	private ModifyPropertyHandler messageObjectModifyPropertyHandler;
	private ModifyPropertyHandler messageIDModifyPropertyHandler;

	public ADWinCANModulePage(FormEditor editor, int id, Module module) {
		super(editor, Integer.toString(id), "Temporary Title", module);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ADWinMessages.ADWinCANPage_PageTitle);
		
		String value = "";
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, true);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ADWinMessages.ADWinCANModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCANName_Label, ADWinMessages.ADWinCANName_Tooltip);
		value = module.getProperty(ADWinCANModuleProperties.NAME);
		canNametext = createText(generalconfigurationContainer, value, SWT.BORDER, 1, 1);
		canNametext.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.NAME, module, canNametext, ADWinCANModuleProperties.NAME.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCANInterfaceNumber_Label, ADWinMessages.ADWinCANInterfaceNumber_Tooltip);
		value = module.getProperty(ADWinCANModuleProperties.INTERFACE_NUMBER);
		String[] items = ADWinCANModuleProperties.INTERFACE_NUMBER.getAvailableValues();
		interfaceNumberCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		interfaceNumberCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.INTERFACE_NUMBER, module, interfaceNumberCombo, ADWinCANModuleProperties.INTERFACE_NUMBER.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCANSystemType_Label, ADWinMessages.ADWinCANSystemType_Tooltip);
		value = module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE);
		items = ADWinCANModuleProperties.SYSTEM_TYPE.getAvailableValues();
		systemTypeCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		systemTypeCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.SYSTEM_TYPE, module, systemTypeCombo, ADWinCANModuleProperties.SYSTEM_TYPE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCANFrequency_Label, ADWinMessages.ADWinCANFrequency_Tooltip);
		value = module.getProperty(ADWinCANModuleProperties.FREQUENCY);
		if(value.equals("")) value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
		frequencyCombo = createCombo(generalconfigurationContainer, dacqConfiguration.getAvailableFrequencies(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)), value, 1, 1);
		frequencyCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.FREQUENCY, module, frequencyCombo, ADWinCANModuleProperties.FREQUENCY.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCAN_NB_SENSORS_Label, ADWinMessages.ADWinCAN_NB_SENSORS_Toolip);
		value = module.getProperty(ADWinCANModuleProperties.NB_SENSORS);
		items = ADWinCANModuleProperties.NB_SENSORS.getAvailableValues();
		nbSensorsCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		nbSensorsCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.NB_SENSORS, module, nbSensorsCombo, ADWinCANModuleProperties.NB_SENSORS.getRegExp(), "", false, (ResourceEditor)getEditor()));
		nbSensorsCombo.setEnabled(false);
		if(module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE)) nbSensorsCombo.setEnabled(true);
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCAN_MODE_Label, ADWinMessages.ADWinCAN_MODE_Toolip);
		value = module.getProperty(ADWinCANModuleProperties.MODE);
		value = value == null ? ADWinCANModuleProperties.RECEIVE: value;
		items = ADWinCANModuleProperties.MODE.getAvailableValues();
		modeCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		modeCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.MODE, module, modeCombo, ADWinCANModuleProperties.MODE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		modeCombo.setEnabled(false);
		if(module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE)) modeCombo.setEnabled(true);
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCAN_MESSAGE_OBJECT_Label, ADWinMessages.ADWinCAN_MESSAGE_OBJECT_Toolip);
		value = module.getProperty(ADWinCANModuleProperties.MESSAGE_OBJECT);
		value = value == null ? "1": value;
		messageObjectText = createText(generalconfigurationContainer, value, SWT.BORDER, 1, 1);
		messageObjectModifyPropertyHandler = new ModifyPropertyHandler(ADWinCANModuleProperties.MESSAGE_OBJECT, module, messageObjectText, ADWinCANModuleProperties.MESSAGE_OBJECT.getRegExp(), ADWinMessages.ADWinCAN_MESSAGE_OBJECT_ErrorMessage, false, (ResourceEditor)getEditor());
		messageObjectText.addModifyListener(messageObjectModifyPropertyHandler);
		messageObjectText.setEnabled(false);
		if(module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE)) messageObjectText.setEnabled(true);
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCAN_MESSAGE_ID_LENGTH_Label, ADWinMessages.ADWinCAN_MESSAGE_ID_LENGTH_Toolip);
		value = module.getProperty(ADWinCANModuleProperties.MESSAGE_ID_LENGTH);
		value = value == null ? ADWinCANModuleProperties.MESSAGE_ID_LENGTH_11: value;
		items = ADWinCANModuleProperties.MESSAGE_ID_LENGTH.getAvailableValues();
		messageIDLengthCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		messageIDLengthCombo.addModifyListener(new ModifyPropertyHandler(ADWinCANModuleProperties.MESSAGE_ID_LENGTH, module, messageIDLengthCombo, ADWinCANModuleProperties.MESSAGE_ID_LENGTH.getRegExp(), "", false, (ResourceEditor)getEditor()));
		messageIDLengthCombo.setEnabled(false);
		if(module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE)) messageIDLengthCombo.setEnabled(true);
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinCAN_MESSAGE_ID_Label, ADWinMessages.ADWinCAN_MESSAGE_ID_Toolip);
		value = module.getProperty(ADWinCANModuleProperties.MESSAGE_ID);
		value = value == null ? "1": value;
		messageIDtext = createText(generalconfigurationContainer, value, SWT.BORDER, 1, 1);
		messageIDModifyPropertyHandler = new ModifyPropertyHandler(ADWinCANModuleProperties.MESSAGE_ID, module, messageIDtext, ADWinCANModuleProperties.MESSAGE_ID.getRegExp(), ADWinMessages.ADWinCAN_MESSAGE_ID_ErrorMessage, false, (ResourceEditor)getEditor());
		messageIDtext.addModifyListener(messageIDModifyPropertyHandler);
		messageIDtext.setEnabled(false);
		if(module.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE)) messageIDtext.setEnabled(true);
		
		modeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateMessageObjectModifyHandler(true);
				updateMessageIDModifyHandler(true);
			}
		});
		updateMessageObjectModifyHandler(false);
		updateMessageIDModifyHandler(false);
		
		createChannelsConfigurationSection();
	}
	
	private void updateMessageObjectModifyHandler(boolean sendEvent) {
		String selectedMode = modeCombo.getText();
		if(ADWinCANModuleProperties.TRANSMIT.equals(selectedMode) || ADWinCANModuleProperties.RECEIVE.equals(selectedMode))
			messageObjectModifyPropertyHandler.setRegExp(ADWinCANModuleProperties.MESSAGE_OBJECT_REGEXP_1);
		else
			messageObjectModifyPropertyHandler.setRegExp(ADWinCANModuleProperties.MESSAGE_OBJECT_REGEXP_2);
		if(sendEvent) {
			Event event = new Event();
			event.widget = modeCombo;
			messageObjectModifyPropertyHandler.modifyText(new ModifyEvent(event));
		}
	}
	
	private void updateMessageIDModifyHandler(boolean sendEvent) {
		String selectedMode = modeCombo.getText();
		if(ADWinCANModuleProperties.TRANSMIT.equals(selectedMode) || ADWinCANModuleProperties.RECEIVE.equals(selectedMode))
			messageIDModifyPropertyHandler.setRegExp(ADWinCANModuleProperties.MESSAGE_ID_REGEXP_1);
		else
			messageIDModifyPropertyHandler.setRegExp(ADWinCANModuleProperties.MESSAGE_ID_REGEXP_2);
		if(sendEvent) {
			Event event = new Event();
			event.widget = modeCombo;
			messageIDModifyPropertyHandler.modifyText(new ModifyEvent(event));
		}
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ADWinCANModuleProperties propertyKey) {
		if(widget == null) return;
		String value = module.getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		if(widget instanceof Text) {
			Text text = (Text)widget;
			int currentPostion = text.getCaretPosition();
			text.setText(value);
			text.setSelection(currentPostion, currentPostion);
			
		}
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		for (Listener listener : listeners) widget.addListener(SWT.Modify , listener);
		generalConfigurationSectionPart.markDirty();
	}
	
	/*
	 * This method is called to reflect any changes between model and UI.
	 * It is called when undo/redo operation are run. It is not possible
	 * to use properties listeners as it will result in cyclic calls and
	 * stack overflow.
	 */
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		super.update(property, newValue, oldValue, element);
		if(property == ADWinCANModuleProperties.NAME || property == ADWinCANModuleProperties.INTERFACE_NUMBER || property == ADWinCANModuleProperties.SYSTEM_TYPE || property == ADWinCANModuleProperties.FREQUENCY || property == ADWinCANModuleProperties.NB_SENSORS) {
			if(generalConfigurationSectionPart != null) {
				if(property == ADWinCANModuleProperties.NAME) {
					updateWidget(canNametext, (ADWinCANModuleProperties) property);
					Channel[] channels = getModule().getChannels();
					for (int i = 0; i < channels.length; i++) {
						Channel channel = channels[i];
						String oldName = channel.getProperty(ChannelProperties.NAME);
						String newName = oldName.replaceFirst((String)oldValue, (String)newValue);
						channel.setProperty(ChannelProperties.NAME, newName);
					}
					if(tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) tableViewer.setInput(module.getChannels());
				}
				if(property == ADWinCANModuleProperties.INTERFACE_NUMBER) 
					updateWidget(interfaceNumberCombo, (ADWinCANModuleProperties) property);
				if(property == ADWinCANModuleProperties.FREQUENCY) {
					updateWidget(frequencyCombo, (ADWinCANModuleProperties) property);
					Channel[] channels = getModule().getChannels();
					for (Channel channel : channels) {
						channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, (String)newValue);
					}
				}
					
				if(property == ADWinCANModuleProperties.NB_SENSORS) {
					int newSensorsNumber = Integer.valueOf((String)newValue);
					int oldSensorsNumber = Integer.valueOf((String)oldValue);
					manageCodamotionChannels(newSensorsNumber, oldSensorsNumber);
					updateWidget(nbSensorsCombo, (ADWinCANModuleProperties) property);
				}
				if(property == ADWinCANModuleProperties.SYSTEM_TYPE) {
					
					boolean wasCoda = ((String)oldValue).contains(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
					boolean isCoda = ((String)newValue).contains(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
					boolean wasGyro = ((String)oldValue).contains(ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
					boolean isGyro = ((String)newValue).contains(ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
					boolean wasTimeStamp = ((String)oldValue).contains(ADWinCANModuleProperties.TIMESTAMP_SYSTEM_TYPE);
					boolean isTimeStamp = ((String)newValue).contains(ADWinCANModuleProperties.TIMESTAMP_SYSTEM_TYPE);
					boolean isNotSpecified = ((String)newValue).contains(ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE);
					
					if(isCoda) {
						nbSensorsCombo.setEnabled(true);
						updateWidget(systemTypeCombo, (ADWinCANModuleProperties) property);
					}
					
					// Clean codamotion channels
					if(wasCoda && !isCoda) {
						getModule().setProperty(ADWinCANModuleProperties.NB_SENSORS, "0");
						nbSensorsCombo.setEnabled(false);
					}
					
					// Clean gyroscope channels
					if(wasGyro && !isGyro) removeGyroscopeChannels();
					
					// Create gyroscope channels
					if(!wasGyro && isGyro) createGyroscopeChannels();
					
					// Clean time stamp channels
					if(wasTimeStamp && !isTimeStamp) removeTimeStampChannels();
					
					// Create time stamp channels
					if(!wasTimeStamp && isTimeStamp) createTimeStampChannels();
					
					modeCombo.setEnabled(isNotSpecified);
					messageObjectText.setEnabled(isNotSpecified);
					messageIDLengthCombo.setEnabled(isNotSpecified);
					messageIDtext.setEnabled(isNotSpecified);
					
				}
				
				if(tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) {
						tableViewer.setInput(getModule().getChannels());
				}
			}
			
		}
		if(property instanceof ChannelProperties) {
			if(tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) tableViewer.refresh();
			if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
		}
		
		if(property == ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY) populateAvailableFrequencies();
		
		if(property == ADWinCANModuleProperties.MODE) updateWidget(modeCombo, (ADWinCANModuleProperties) property);
		if(property == ADWinCANModuleProperties.MESSAGE_OBJECT) updateWidget(messageObjectText, (ADWinCANModuleProperties) property);
		if(property == ADWinCANModuleProperties.MESSAGE_ID_LENGTH) updateWidget(messageIDLengthCombo, (ADWinCANModuleProperties) property);
		if(property == ADWinCANModuleProperties.MESSAGE_ID) updateWidget(messageIDtext, (ADWinCANModuleProperties) property);
		
	}
	
	private void manageCodamotionChannels(int newSensorsNumber, int oldSensorsNumber) {
		if(oldSensorsNumber == 0 && newSensorsNumber > 0) {
			addCodamotionChannels(newSensorsNumber);
		} else if(oldSensorsNumber > 0 && newSensorsNumber == 0) {
			// Remove channels table
			removeCodamotionChannels(oldSensorsNumber);
		} else if(oldSensorsNumber > newSensorsNumber) {
			// Remove some channels
			int nbChannelsToRemove = oldSensorsNumber - newSensorsNumber;
			removeCodamotionChannels(nbChannelsToRemove);
		} else if(oldSensorsNumber < newSensorsNumber) {
			// Add some channels
			int nbSensorsToAdd = newSensorsNumber - oldSensorsNumber;
			addCodamotionChannels(nbSensorsToAdd);
		}
	}
	
	private void addCodamotionChannels(int nbSensorsToAdd) {
		for (int i = 0; i < nbSensorsToAdd; i++) ((ADWinCANModule)getModule()).createCodamotionChannels();
		Channel[] channels = ((ADWinCANModule)getModule()).getCodamotionChannels();
		for (Channel channel : channels) {
			channel.addObserver(this);
		}
	}
	
	private void removeCodamotionChannels(int nbSensorsToRemove) {
		Channel[] channels = ((ADWinCANModule)getModule()).getCodamotionChannels();
		Channel[] channelsToDelete = new Channel[4*nbSensorsToRemove];
		System.arraycopy(channels, channels.length - 4*nbSensorsToRemove, channelsToDelete, 0, 4*nbSensorsToRemove);
		for (int i = 0; i < channelsToDelete.length; i++) {
			getModule().removeChannel(channelsToDelete[i]);
		}
	}
	
	private void removeGyroscopeChannels() {
		Channel[] channelsToDelete = ((ADWinCANModule)getModule()).getGyroscopeChannels();
		for (int i = 0; i < channelsToDelete.length; i++) {
			getModule().removeChannel(channelsToDelete[i]);
		}
	}
	
	private void createGyroscopeChannels() {
		((ADWinCANModule)getModule()).createGyroscopeChannels();
	}
	
	private void removeTimeStampChannels() {
		Channel[] channelsToDelete = ((ADWinCANModule)getModule()).getTimeStampChannels();
		for (int i = 0; i < channelsToDelete.length; i++) {
			getModule().removeChannel(channelsToDelete[i]);
		}
	}
	
	private void createTimeStampChannels() {
		((ADWinCANModule)getModule()).createTimeStampChannels();
	}

	private void createChannelsConfigurationSection() {
		// Create channels table
		createTableConfigurationSection(false, true, true, true);
		
		TableColumnLayout  channelsTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ChannelProperties.NAME.getTooltip(), channelsTableColumnLayout, ChannelProperties.NAME, 175, 0).setEditingSupport(null);
		createColumn(ChannelProperties.TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER, defaultColumnWidth, 1);
		createColumn(ChannelProperties.AUTO_TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.AUTO_TRANSFER, defaultColumnWidth, 2);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.RECORD.getTooltip(), channelsTableColumnLayout, ChannelProperties.RECORD, defaultColumnWidth, 4);
		createColumn(ChannelProperties.BUFFER_SIZE.getTooltip(), channelsTableColumnLayout, ChannelProperties.BUFFER_SIZE, defaultColumnWidth, 5);
		
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new ITableLabelProvider() {
			public void removeListener(ILabelProviderListener listener) {
			}
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			public void dispose() {
			}
			public void addListener(ILabelProviderListener listener) {
			}
			public String getColumnText(Object element, int columnIndex) {
				Channel channel = (Channel)element;
				switch (columnIndex) {
				case 0:
					return channel.getProperty(ChannelProperties.NAME);
				case 1:
					return channel.getProperty(ChannelProperties.TRANSFER);
				case 2:
					return channel.getProperty(ChannelProperties.AUTO_TRANSFER);
				case 3:
					return channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
				case 4:
					return channel.getProperty(ChannelProperties.RECORD);
				case 5:
					return channel.getProperty(ChannelProperties.BUFFER_SIZE);
				default:
					return "";
				}
			}
			public Image getColumnImage(Object element, int columnIndex) {
				Channel channel = (Channel)element;
				String value = "false";
				switch (columnIndex) {
				case 1:
					value = channel.getProperty(ChannelProperties.TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 2:
					value = channel.getProperty(ChannelProperties.AUTO_TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 4:
					value = channel.getProperty(ChannelProperties.RECORD);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				default:
					return null;
				}
			}
		});
		
		configureSorter(new CANComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(module.getChannels());
		tableConfigurationSection.getParent().layout(true);
	}

	

	private void populateAvailableFrequencies() {
		int currentSelectionIndex = frequencyCombo.getSelectionIndex();
		frequencyCombo.setItems(dacqConfiguration.getAvailableFrequencies(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)));
		frequencyCombo.select(currentSelectionIndex);
	}
	
//	private String[] getAvailableFrequencies() {
//		String globalSampleFrequencyString = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
//		ArrayList<String> availableFrequencies = new ArrayList<>();
//		availableFrequencies.add(globalSampleFrequencyString);
//		int n = 2;
//		double fMax = Double.parseDouble(globalSampleFrequencyString);
//		double f = fMax;
//		while (f > .25) {
//			f = fMax / (1.f*n);
//			if(100.0*f - (int)(100.0*f) == 0) availableFrequencies.add(String.valueOf(f));
//			n++;
//		}
//		return availableFrequencies.toArray(new String[availableFrequencies.size()]);
//	}
	

}
