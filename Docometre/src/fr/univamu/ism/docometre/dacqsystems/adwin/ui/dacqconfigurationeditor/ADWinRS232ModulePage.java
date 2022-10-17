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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
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
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ADWinRS232ModulePage extends ADWinModulePage {
	
	private class RS232Comparator extends Comparator {
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
				e1 = in1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}

	private Text rs232Nametext;
	private Combo interfaceNumberCombo;
	private Combo systemTypeCombo;
	private Combo frequencyCombo;
	private Combo baudRateCombo;
	private Combo dataBitsCombo;
	private Combo stopBitsCombo;
	private Combo parityCombo;
	private Combo flowControlCombo;

	public ADWinRS232ModulePage(FormEditor editor, int id, Module module) {
		super(editor, Integer.toString(id), "Temporary Title", module);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ADWinMessages.ADWinRS232Page_PageTitle);
		
		String value = "";
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, true);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ADWinMessages.ADWinRS232ModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232Name_Label, ADWinMessages.ADWinRS232Name_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.NAME);
		rs232Nametext = createText(generalconfigurationContainer, value, SWT.BORDER, 1, 1);
		rs232Nametext.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.NAME, module, rs232Nametext, ADWinRS232ModuleProperties.NAME.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232InterfaceNumber_Label, ADWinMessages.ADWinRS232InterfaceNumber_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.INTERFACE_NUMBER);
		String[] items = ADWinRS232ModuleProperties.INTERFACE_NUMBER.getAvailableValues();
		interfaceNumberCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		interfaceNumberCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.INTERFACE_NUMBER, module, interfaceNumberCombo, ADWinRS232ModuleProperties.INTERFACE_NUMBER.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232SystemType_Label, ADWinMessages.ADWinRS232SystemType_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE);
		items = ADWinRS232ModuleProperties.SYSTEM_TYPE.getAvailableValues();
		systemTypeCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		systemTypeCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.SYSTEM_TYPE, module, systemTypeCombo, ADWinRS232ModuleProperties.SYSTEM_TYPE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232Frequency_Label, ADWinMessages.ADWinRS232Frequency_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.FREQUENCY);
		if(value.equals("")) value = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
		frequencyCombo = createCombo(generalconfigurationContainer, dacqConfiguration.getAvailableFrequencies(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)), value, 1, 1);
		frequencyCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.FREQUENCY, module, frequencyCombo, ADWinRS232ModuleProperties.FREQUENCY.getRegExp(), "", false, (ResourceEditor)getEditor()));

		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232BaudRate_Label, ADWinMessages.ADWinRS232BaudRate_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.BAUD_RATE);
		items = ADWinRS232ModuleProperties.BAUD_RATE.getAvailableValues();
		baudRateCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		baudRateCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.BAUD_RATE, module, baudRateCombo, ADWinRS232ModuleProperties.BAUD_RATE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232DataBits_Label, ADWinMessages.ADWinRS232DataBits_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.NB_DATA_BITS);
		items = ADWinRS232ModuleProperties.NB_DATA_BITS.getAvailableValues();
		dataBitsCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		dataBitsCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.NB_DATA_BITS, module, dataBitsCombo, ADWinRS232ModuleProperties.NB_DATA_BITS.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232StopBits_Label, ADWinMessages.ADWinRS232StopBits_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.NB_STOP_BITS);
		items = ADWinRS232ModuleProperties.NB_STOP_BITS.getAvailableValues();
		stopBitsCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		stopBitsCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.NB_STOP_BITS, module, stopBitsCombo, ADWinRS232ModuleProperties.NB_STOP_BITS.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232Parity_Label, ADWinMessages.ADWinRS232Parity_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.PARITY);
		items = ADWinRS232ModuleProperties.PARITY.getAvailableValues();
		parityCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		parityCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.PARITY, module, parityCombo, ADWinRS232ModuleProperties.PARITY.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ADWinMessages.ADWinRS232FlowControl_Label, ADWinMessages.ADWinRS232FlowControl_Tooltip);
		value = module.getProperty(ADWinRS232ModuleProperties.FLOW_CONTROL);
		items = ADWinRS232ModuleProperties.FLOW_CONTROL.getAvailableValues();
		flowControlCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		flowControlCombo.addModifyListener(new ModifyPropertyHandler(ADWinRS232ModuleProperties.FLOW_CONTROL, module, flowControlCombo, ADWinRS232ModuleProperties.FLOW_CONTROL.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		value = module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE);
		if(value.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
			createChannelsConfigurationSection();
		}
		
//		Composite composite = new Composite(generalconfigurationContainer, SWT.BORDER);
//		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
//		composite.setLayout(new GridLayout(1, true));
//		ListViewer listViewer = new ListViewer(composite);
//		listViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		managedForm.getToolkit().createComposite(composite);
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ADWinRS232ModuleProperties propertyKey) {
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
		boolean update = property == ADWinRS232ModuleProperties.NAME || property == ADWinRS232ModuleProperties.INTERFACE_NUMBER; 
		update = update || property == ADWinRS232ModuleProperties.SYSTEM_TYPE || property == ADWinRS232ModuleProperties.FREQUENCY;
		update = update || property == ADWinRS232ModuleProperties.BAUD_RATE || property == ADWinRS232ModuleProperties.NB_DATA_BITS;
		update = update|| property == ADWinRS232ModuleProperties.NB_STOP_BITS || property == ADWinRS232ModuleProperties.PARITY;
		update = update || property == ADWinRS232ModuleProperties.FLOW_CONTROL || property == ChannelProperties.ADD || property == ChannelProperties.REMOVE;
		if(update) {
			if(generalConfigurationSectionPart != null) {
				if(property instanceof ADWinRS232ModuleProperties) {
					if(property == ADWinRS232ModuleProperties.NAME)
						updateWidget(rs232Nametext, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.INTERFACE_NUMBER)
						updateWidget(interfaceNumberCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.SYSTEM_TYPE) {
						updateWidget(systemTypeCombo, (ADWinRS232ModuleProperties) property);
						if(newValue.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE) && !oldValue.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE)) {
							createChannelsConfigurationSection();
							tableConfigurationSection.getParent().layout(true);
						}
						if(!newValue.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE) && oldValue.equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE))
							if(tableConfigurationSection != null && !tableConfigurationSection.isDisposed()) tableConfigurationSection.dispose();
					}
					if(property == ADWinRS232ModuleProperties.FREQUENCY)
						updateWidget(frequencyCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.BAUD_RATE)
						updateWidget(baudRateCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.NB_DATA_BITS)
						updateWidget(dataBitsCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.NB_STOP_BITS)
						updateWidget(stopBitsCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.PARITY)
						updateWidget(parityCombo, (ADWinRS232ModuleProperties) property);
					if(property == ADWinRS232ModuleProperties.FLOW_CONTROL)
						updateWidget(flowControlCombo, (ADWinRS232ModuleProperties) property);
				}
				if(property instanceof ChannelProperties) {
					if(property == ChannelProperties.NAME) {
						tableViewer.setInput(module.getChannels());
						tableConfigurationSectionPart.markDirty();
					}
					if(property == ChannelProperties.REMOVE) {
//						module.removeChannel(oldValue);
						tableViewer.setInput(module.getChannels());
						tableConfigurationSectionPart.markDirty();
					}
					
				}
			}
			
		}
		if(property == ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY) populateAvailableFrequencies();
	}
	
	private void populateAvailableFrequencies() {
		int currentSelectionIndex = frequencyCombo.getSelectionIndex();
		frequencyCombo.setItems(dacqConfiguration.getAvailableFrequencies(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)));
		frequencyCombo.select(currentSelectionIndex);
	}
	
	private void createChannelsConfigurationSection() {
		// Create channels table
		createTableConfigurationSection(true, false, false, false);
		tableConfigurationSection.setDescription(ADWinMessages.ADWinRS232ChannelsConfigurationModuleSection_Description);
		
		deleteToolItem.setToolTipText(ADWinMessages.DeleteChannel_Tooltip);
		deleteToolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] selectedChannels = ((IStructuredSelection)tableViewer.getSelection()).toArray();
				for (int i = 0; i < selectedChannels.length; i++) {
					Channel channel = (Channel) selectedChannels[i];
					module.removeChannel(channel);
				}
				if(selectedChannels.length > 0) {
					tableViewer.setInput(module.getChannels());
					tableConfigurationSectionPart.markDirty();
					tableViewer.refresh();
				}
				
			}
		});

		addToolItem.setToolTipText(ADWinMessages.AddChannel_Tooltip);
		addToolItem.addSelectionListener(new AddRS232ChannelsDialogHandler(getSite().getShell(), (ResourceEditor)getEditor(), this));
		
		TableColumnLayout  channelsTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ChannelProperties.NAME.getTooltip(), channelsTableColumnLayout, ChannelProperties.NAME, 100, 0).setEditingSupport(null);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 1);
		
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
					return channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
				default:
					return "";
				}
			}
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});
		
		configureSorter(new RS232Comparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(module.getChannels());
		tableConfigurationSection.getParent().layout(true);
		
	}

}
