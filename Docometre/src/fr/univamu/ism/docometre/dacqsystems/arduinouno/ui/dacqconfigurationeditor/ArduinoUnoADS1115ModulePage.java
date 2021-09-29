package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
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
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoADS1115ModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ArduinoUnoADS1115ModulePage extends ArduinoUnoModulePage {

	private Combo addressCombo;
	private Combo modeCombo;
	private Combo dataRateCombo;
	private static String[] availableAddresses;
	
	private class AnalogInputsComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			Channel in1 = (Channel) e1;
			Channel in2 = (Channel) e2;
			switch (columnNumber) {
			case 0:
				e1 = in1.getProperty(ArduinoUnoChannelProperties.USED);
				e2 = in2.getProperty(ArduinoUnoChannelProperties.USED);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 1:
				e1 = in1.getProperty(ChannelProperties.NAME);
				e2 = in2.getProperty(ChannelProperties.NAME);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 2:
				e1 = in1.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				e2 = in2.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;				
			case 3:
				e1 = in1.getProperty(ChannelProperties.CHANNEL_NUMBER);
				e2 = in2.getProperty(ChannelProperties.CHANNEL_NUMBER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 4:
				e1 = in1.getProperty(ChannelProperties.TRANSFER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 5:
				e1 = in1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = in2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 6:
				e1 = in1.getProperty(ChannelProperties.RECORD);
				e2 = in2.getProperty(ChannelProperties.RECORD);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 7:
				e1 = in1.getProperty(ArduinoUnoAnInChannelProperties.UNIT);
				e2 = in2.getProperty(ArduinoUnoAnInChannelProperties.UNIT);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 8:
				e1 = in1.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX);
				e2 = in2.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 9:
				e1 = in1.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN);
				e2 = in2.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 10:
				e1 = in1.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX);
				e2 = in2.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 11:
				e1 = in1.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN);
				e2 = in2.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}

	public ArduinoUnoADS1115ModulePage(FormEditor editor, String id, String title, Module module) {
		super(editor, id, title, module);
	}
	
	private void computeAvailableAddresses() {
		ArrayList<String> addresses = new ArrayList<String>(Arrays.asList( ArduinoUnoADS1115ModuleProperties.ADDRESS.getAvailableValues()));
		Module[] modules = dacqConfiguration.getModules();
		for (Module aModule : modules) {
			if(aModule instanceof ArduinoUnoADS1115Module && aModule != module) {
				if(aModule.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS) != null)
					addresses.remove(aModule.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS));
			}
		}
		availableAddresses = addresses.toArray(new String[addresses.size()]);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ArduinoUnoMessages.ADS1115Page_PageTitle);
		
		computeAvailableAddresses();
		String value = "";
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, false);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ArduinoUnoMessages.ADS1115ModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.address_label, ArduinoUnoMessages.address_tooltip);
		value = module.getProperty(ArduinoUnoADS1115ModuleProperties.ADDRESS);
		addressCombo = createCombo(generalconfigurationContainer, availableAddresses, value, 1, 1);
		addressCombo.addModifyListener(new ModifyPropertyHandler(ArduinoUnoADS1115ModuleProperties.ADDRESS, module, addressCombo, ArduinoUnoADS1115ModuleProperties.ADDRESS.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.mode_label, ArduinoUnoMessages.mode_tooltip);
		value = module.getProperty(ArduinoUnoADS1115ModuleProperties.MODE);
		value = (value == null)?"1":value;
		modeCombo = createCombo(generalconfigurationContainer, ArduinoUnoADS1115ModuleProperties.MODES, value, 1, 1);
		modeCombo.addModifyListener(new ModifyPropertyHandler(ArduinoUnoADS1115ModuleProperties.MODE, module, modeCombo, ArduinoUnoADS1115ModuleProperties.MODE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		createLabel(generalconfigurationContainer, ArduinoUnoMessages.dataRate_label, ArduinoUnoMessages.dataRate_tooltip);
		value = module.getProperty(ArduinoUnoADS1115ModuleProperties.DATA_RATE);
		value = (value == null)?"7":value;
		dataRateCombo = createCombo(generalconfigurationContainer, ArduinoUnoADS1115ModuleProperties.DATA_RATES, value, 1, 1);
		dataRateCombo.addModifyListener(new ModifyPropertyHandler(ArduinoUnoADS1115ModuleProperties.DATA_RATE, module, dataRateCombo, ArduinoUnoADS1115ModuleProperties.DATA_RATE.getRegExp(), "", false, (ResourceEditor)getEditor()));
		
		/*
		 * Channels configuration
		 */
		createTableConfigurationSection(false, true, false, true);
		
		FormText explanationsFormText2 = managedForm.getToolkit().createFormText(explanationsContainer, false);
		explanationsFormText2.setText(ArduinoUnoMessages.AnInModuleExplanations_Text2, true, false);
//		explanationsFormText2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		TableColumnLayout  channelsTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ArduinoUnoChannelProperties.USED.getTooltip(), channelsTableColumnLayout, ArduinoUnoChannelProperties.USED, defaultColumnWidth, 0);
		createColumn(ChannelProperties.NAME.getTooltip(), channelsTableColumnLayout, ChannelProperties.NAME, 100, 1);
		createColumn(ChannelProperties.SAMPLE_FREQUENCY.getTooltip(), channelsTableColumnLayout, ChannelProperties.SAMPLE_FREQUENCY, defaultColumnWidth, 2);
		createColumn(ChannelProperties.CHANNEL_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.CHANNEL_NUMBER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER, defaultColumnWidth, 4);
		//createColumn(ChannelProperties.AUTO_TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.AUTO_TRANSFER, defaultColumnWidth, 4);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 5);
		createColumn(ChannelProperties.RECORD.getTooltip(), channelsTableColumnLayout, ChannelProperties.RECORD, defaultColumnWidth, 6);
		//createColumn(ChannelProperties.BUFFER_SIZE.getTooltip(), channelsTableColumnLayout, ChannelProperties.BUFFER_SIZE, defaultColumnWidth, 7);
		//createColumn(ADWinAnInChannelProperties.GAIN.getTooltip(), channelsTableColumnLayout, ADWinAnInChannelProperties.GAIN, defaultColumnWidth, 8);
		createColumn(ArduinoUnoAnInChannelProperties.UNIT.getTooltip(), channelsTableColumnLayout, ArduinoUnoAnInChannelProperties.UNIT, defaultColumnWidth, 7);
		createColumn(ArduinoUnoAnInChannelProperties.UNIT_MAX.getTooltip(), channelsTableColumnLayout, ArduinoUnoAnInChannelProperties.UNIT_MAX, defaultColumnWidth, 8);
		createColumn(ArduinoUnoAnInChannelProperties.UNIT_MIN.getTooltip(), channelsTableColumnLayout, ArduinoUnoAnInChannelProperties.UNIT_MIN, defaultColumnWidth, 9);
		createColumn(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX.getTooltip(), channelsTableColumnLayout, ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX, defaultColumnWidth, 10);
		createColumn(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN.getTooltip(), channelsTableColumnLayout, ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN, defaultColumnWidth, 11);
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
					return channel.getProperty(ArduinoUnoChannelProperties.USED);
				case 1:
					return channel.getProperty(ChannelProperties.NAME);
				case 2:
					return channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				case 3:
					return channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
				case 4:
					return channel.getProperty(ChannelProperties.TRANSFER);
				case 5:
					return channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
				case 6:
					return channel.getProperty(ChannelProperties.RECORD);
				case 7:
					return channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT);
				case 8:
					return channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MAX);
				case 9:
					return channel.getProperty(ArduinoUnoAnInChannelProperties.UNIT_MIN);
				case 10:
					return channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX);
				case 11:
					return channel.getProperty(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN);
				default:
					return "";
				}
			}
			public Image getColumnImage(Object element, int columnIndex) {
				Channel channel = (Channel)element;
				String value = "false";
				switch (columnIndex) {
				case 0:
					value = channel.getProperty(ArduinoUnoChannelProperties.USED);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 4:
					value = channel.getProperty(ChannelProperties.TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				case 6:
					value = channel.getProperty(ChannelProperties.RECORD);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				default:
					return null;
				}
			}
		});
		AnalogInputsComparator analogInputsComparator = new AnalogInputsComparator();
//		analogInputsComparator.setSortingColumn(1);
		configureSorter(analogInputsComparator, tableViewer.getTable().getColumn(1));
		tableViewer.setInput(module.getChannels());
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Scrollable widget, ArduinoUnoADS1115ModuleProperties propertyKey, boolean markDirty) {
		if(widget == null || widget.isDisposed()) return;
		String value = module.getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		if(widget instanceof Text) ((Text)widget).setText(value);
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		for (Listener listener : listeners) widget.addListener(SWT.Modify , listener);
		if(markDirty) generalConfigurationSectionPart.markDirty();
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
		if(property == ArduinoUnoADS1115ModuleProperties.ADDRESS)
			updateWidget(addressCombo, (ArduinoUnoADS1115ModuleProperties)property, true);
		if(property == ArduinoUnoADS1115ModuleProperties.MODE)
			updateWidget(modeCombo, (ArduinoUnoADS1115ModuleProperties)property, true);
		if(property == ArduinoUnoADS1115ModuleProperties.DATA_RATE)
			updateWidget(dataRateCombo, (ArduinoUnoADS1115ModuleProperties)property, true);
//		if(property instanceof ArduinoUnoADS1115ModuleProperties) {
//			if(tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) {
//				tableViewer.refresh();
//				tableConfigurationSectionPart.markDirty();
//				
//			}
//		}
		
	}
	
	public void updateAvailableAddresses() {
		if(addressCombo != null) {
			computeAvailableAddresses();
			addressCombo.setItems(availableAddresses);
			updateWidget(addressCombo, ArduinoUnoADS1115ModuleProperties.ADDRESS, false);
		}
		
	}

}
