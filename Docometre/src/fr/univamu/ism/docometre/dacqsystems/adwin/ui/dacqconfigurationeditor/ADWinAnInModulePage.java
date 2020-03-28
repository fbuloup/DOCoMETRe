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
import org.eclipse.swt.events.ModifyListener;
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
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ADWinAnInModulePage extends ADWinModulePage {
	
	private class AmplitudeMaxModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			if(ampMaxCombo.getText().equals(ADWinAnInModuleProperties.PLUS_10)) {
				if(!ampMinCombo.getText().equals(ADWinAnInModuleProperties.MINUS_10) && !ampMinCombo.getText().equals(ADWinAnInModuleProperties.ZERO))
					ampMinCombo.setText(ADWinAnInModuleProperties.MINUS_10);
			}
			if(ampMaxCombo.getText().equals(ADWinAnInModuleProperties.PLUS_5)) {
				if(!ampMinCombo.getText().equals(ADWinAnInModuleProperties.MINUS_5))
					ampMinCombo.setText(ADWinAnInModuleProperties.MINUS_5);
			}
		}
	}
	
	private class AmplitudeMinModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			if(ampMinCombo.getText().equals(ADWinAnInModuleProperties.ZERO)) {
				if(!ampMaxCombo.getText().equals(ADWinAnInModuleProperties.PLUS_10))
					ampMaxCombo.setText(ADWinAnInModuleProperties.PLUS_10);
			}
			if(ampMinCombo.getText().equals(ADWinAnInModuleProperties.MINUS_5)) {
				if(!ampMaxCombo.getText().equals(ADWinAnInModuleProperties.PLUS_5))
					ampMaxCombo.setText(ADWinAnInModuleProperties.PLUS_5);
			}
			if(ampMinCombo.getText().equals(ADWinAnInModuleProperties.MINUS_10)) {
				if(!ampMaxCombo.getText().equals(ADWinAnInModuleProperties.PLUS_10))
					ampMaxCombo.setText(ADWinAnInModuleProperties.PLUS_10);
			}
		}
	}
	
	private class AnalogInputsComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			Channel in1 = (Channel) e1;
			Channel in2 = (Channel) e2;
			switch (columnNumber) {
			case 0:
				e1 = in1.getProperty(ChannelProperties.NAME);
				e2 = in2.getProperty(ChannelProperties.NAME);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 1:
				e1 = in1.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				e2 = in2.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 2:
				e1 = in1.getProperty(ChannelProperties.CHANNEL_NUMBER);
				e2 = in2.getProperty(ChannelProperties.CHANNEL_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 3:
				e1 = in1.getProperty(ChannelProperties.TRANSFER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 4:
				e1 = in1.getProperty(ChannelProperties.AUTO_TRANSFER);
				e2 = in2.getProperty(ChannelProperties.AUTO_TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 5:
				e1 = in1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 6:
				e1 = in1.getProperty(ChannelProperties.RECORD);
				e2 = in2.getProperty(ChannelProperties.RECORD);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 7:
				e1 = in1.getProperty(ChannelProperties.BUFFER_SIZE);
				e2 = in2.getProperty(ChannelProperties.BUFFER_SIZE);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 8:
				e1 = in1.getProperty(ADWinAnInChannelProperties.GAIN);
				e2 = in2.getProperty(ADWinAnInChannelProperties.GAIN);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 9:
				e1 = in1.getProperty(ADWinAnInChannelProperties.UNIT);
				e2 = in2.getProperty(ADWinAnInChannelProperties.UNIT);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 10:
				e1 = in1.getProperty(ADWinAnInChannelProperties.UNIT_MAX);
				e2 = in2.getProperty(ADWinAnInChannelProperties.UNIT_MAX);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 11:
				e1 = in1.getProperty(ADWinAnInChannelProperties.UNIT_MIN);
				e2 = in2.getProperty(ADWinAnInChannelProperties.UNIT_MIN);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 12:
				e1 = in1.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX);
				e2 = in2.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 13:
				e1 = in1.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN);
				e2 = in2.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}

	private Combo ampMaxCombo;
	private Combo ampMinCombo;
	private Combo seDiffCombo;

	public ADWinAnInModulePage(FormEditor editor, int id, Module module) {
		super(editor, Integer.toString(id), "Temporary Title", module);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ADWinMessages.ADWinAnInConfigurationPage_Title);
		
		String value = "";
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, false);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ADWinMessages.AnInModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		createLabel(generalconfigurationContainer, ADWinMessages.AmplitudeMax_Label, ADWinMessages.AmplitudeMax_Tooltip);
		value = module.getProperty(ADWinAnInModuleProperties.AMPLITUDE_MAX);
		String[] items = ADWinAnInModuleProperties.AMPLITUDE_MAX.getAvailableValues();
		ampMaxCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		createLabel(generalconfigurationContainer, ADWinMessages.AmplitudeMin_Label, ADWinMessages.AmplitudeMin_Tooltip);
		value = module.getProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN);
		items = ADWinAnInModuleProperties.AMPLITUDE_MIN.getAvailableValues();
		ampMinCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		
		ampMaxCombo.addModifyListener(new ModifyPropertyHandler(ADWinAnInModuleProperties.AMPLITUDE_MAX, module, ampMaxCombo, ADWinAnInModuleProperties.AMPLITUDE_MAX.getRegExp(), "", false, (ResourceEditor)getEditor()));
		ampMaxCombo.addModifyListener(new AmplitudeMaxModifyListener());
		
		ampMinCombo.addModifyListener(new ModifyPropertyHandler(ADWinAnInModuleProperties.AMPLITUDE_MIN, module, ampMinCombo, ADWinAnInModuleProperties.AMPLITUDE_MIN.getRegExp(), "", false, (ResourceEditor)getEditor()));
		ampMinCombo.addModifyListener(new AmplitudeMinModifyListener());
		
		createLabel(generalconfigurationContainer, ADWinMessages.SeDiff_Label, ADWinMessages.SeDiff_Tooltip);
		value = module.getProperty(ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL);
		items = ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL.getAvailableValues();
		seDiffCombo = createCombo(generalconfigurationContainer, items, value, 1, 1);
		seDiffCombo.addModifyListener(new ModifyPropertyHandler(ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL, module, seDiffCombo, ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		/*
		 * Channels configuration
		 */
		createTableConfigurationSection(true, true, true, true);
//		channelsTableViewer.addSelectionChangedListener(deleteVariablesHandler);
		
		deleteToolItem.setToolTipText(ADWinMessages.DeleteInput_Tooltip);
		DeleteChannelsHandler deleteChannelsHandler = new DeleteChannelsHandler(getSite().getShell(), this, (ADWinDACQConfiguration) dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()); 
		deleteToolItem.addSelectionListener(deleteChannelsHandler);
		tableViewer.addSelectionChangedListener(deleteChannelsHandler);
		addToolItem.setToolTipText(ADWinMessages.AddInput_Tooltip);
		addToolItem.addSelectionListener(new AddChannelsHandler(this, module, ((ResourceEditor)getEditor()).getUndoContext()));
		
		FormText explanationsFormText2 = managedForm.getToolkit().createFormText(explanationsContainer, false);
		explanationsFormText2.setText(ADWinMessages.AnInModuleExplanations_Text2, true, false);
//		explanationsFormText2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		TableColumnLayout  channelsTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ChannelProperties.NAME.getTooltip(), channelsTableColumnLayout, ChannelProperties.NAME, 175, 0);
		createColumn(ChannelProperties.SAMPLE_FREQUENCY.getTooltip(), channelsTableColumnLayout, ChannelProperties.SAMPLE_FREQUENCY, defaultColumnWidth, 1);
		createColumn(ChannelProperties.CHANNEL_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.CHANNEL_NUMBER, defaultColumnWidth, 2);
		createColumn(ChannelProperties.TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.AUTO_TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.AUTO_TRANSFER, defaultColumnWidth, 4);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 5);
		createColumn(ChannelProperties.RECORD.getTooltip(), channelsTableColumnLayout, ChannelProperties.RECORD, defaultColumnWidth, 6);
		createColumn(ChannelProperties.BUFFER_SIZE.getTooltip(), channelsTableColumnLayout, ChannelProperties.BUFFER_SIZE, defaultColumnWidth, 7);
		createColumn(ADWinAnInChannelProperties.GAIN.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.GAIN, defaultColumnWidth, 8);
		createColumn(ADWinAnInChannelProperties.UNIT.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.UNIT, defaultColumnWidth, 9);
		createColumn(ADWinAnInChannelProperties.UNIT_MAX.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.UNIT_MAX, defaultColumnWidth, 10);
		createColumn(ADWinAnInChannelProperties.UNIT_MIN.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.UNIT_MIN, defaultColumnWidth, 11);
		createColumn(ADWinAnInChannelProperties.AMPLITUDE_MAX.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.AMPLITUDE_MAX, defaultColumnWidth, 12);
		createColumn(ADWinAnInChannelProperties.AMPLITUDE_MIN.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.AMPLITUDE_MIN, defaultColumnWidth, 13);
		
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
					return channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				case 2:
					return channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
				case 3:
					return channel.getProperty(ChannelProperties.TRANSFER);
				case 4:
					return channel.getProperty(ChannelProperties.AUTO_TRANSFER);
				case 5:
					return channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
				case 6:
					return channel.getProperty(ChannelProperties.RECORD);
				case 7:
					return channel.getProperty(ChannelProperties.BUFFER_SIZE);
				case 8:
					return channel.getProperty(ADWinAnInChannelProperties.GAIN);
				case 9:
					return channel.getProperty(ADWinAnInChannelProperties.UNIT);
				case 10:
					return channel.getProperty(ADWinAnInChannelProperties.UNIT_MAX);
				case 11:
					return channel.getProperty(ADWinAnInChannelProperties.UNIT_MIN);
				case 12:
					return channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX);
				case 13:
					return channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN);
				default:
					return "";
				}
			}
			public Image getColumnImage(Object element, int columnIndex) {
				Channel channel = (Channel)element;
				String value = "false";
				switch (columnIndex) {
				case 3:
					value = channel.getProperty(ChannelProperties.TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 4:
					value = channel.getProperty(ChannelProperties.AUTO_TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 6:
					value = channel.getProperty(ChannelProperties.RECORD);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				default:
					return null;
				}
			}
		});
		configureSorter(new AnalogInputsComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(module.getChannels());
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ADWinAnInModuleProperties propertyKey) {
		String value = module.getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		ModifyListener modifyListener = null;
		if(propertyKey == ADWinAnInModuleProperties.AMPLITUDE_MAX || propertyKey == ADWinAnInModuleProperties.AMPLITUDE_MIN) {
			if(widget == ampMaxCombo) {
				modifyListener = new AmplitudeMaxModifyListener();
				ampMaxCombo.addModifyListener(modifyListener);
			}
			if(widget == ampMinCombo) {
				modifyListener = new AmplitudeMinModifyListener();
				ampMinCombo.addModifyListener(modifyListener);
			}
		}
		if(widget instanceof Text) ((Text)widget).setText(value);
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		if(propertyKey == ADWinAnInModuleProperties.AMPLITUDE_MAX || propertyKey == ADWinAnInModuleProperties.AMPLITUDE_MIN) {
			if(widget == ampMaxCombo) {
				ampMaxCombo.removeModifyListener(modifyListener);
			}
			if(widget == ampMinCombo) {
				ampMinCombo.removeModifyListener(modifyListener);
			}
		}
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
		if(property == ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL || property == ADWinAnInModuleProperties.AMPLITUDE_MAX || property == ADWinAnInModuleProperties.AMPLITUDE_MIN) {
			if(generalConfigurationSectionPart != null) {
//				ADWinAnInModuleProperties key = (ADWinAnInModuleProperties) property;
				if(property.equals(ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL))
					updateWidget(seDiffCombo, (ADWinAnInModuleProperties) property);
				if(property.equals(ADWinAnInModuleProperties.AMPLITUDE_MAX))
					updateWidget(ampMaxCombo, (ADWinAnInModuleProperties) property);
				if(property.equals(ADWinAnInModuleProperties.AMPLITUDE_MIN))
					updateWidget(ampMinCombo, (ADWinAnInModuleProperties) property);
			}
			
		}
		if(property instanceof ADWinAnInChannelProperties) {
			if(tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) {
				tableViewer.refresh();
				tableConfigurationSectionPart.markDirty();
				
			}
		}
		
	}

}
