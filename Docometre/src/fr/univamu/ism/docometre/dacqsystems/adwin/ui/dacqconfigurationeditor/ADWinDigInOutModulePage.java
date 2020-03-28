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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ADWinDigInOutModulePage extends ADWinModulePage {
	
	private class DIOComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			Channel cha1 = (Channel) e1;
			Channel cha2 = (Channel) e2;
			switch (columnNumber) {
			case 0:
				e1 = cha1.getProperty(ChannelProperties.NAME);
				e2 = cha2.getProperty(ChannelProperties.NAME);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 1:
				e1 = cha1.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				e2 = cha2.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 2:
				e1 = cha1.getProperty(ChannelProperties.CHANNEL_NUMBER);
				e2 = cha2.getProperty(ChannelProperties.CHANNEL_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 3:
				e1 = cha1.getProperty(ChannelProperties.TRANSFER);
				e2 = cha2.getProperty(ChannelProperties.TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 4:
				e1 = cha1.getProperty(ChannelProperties.AUTO_TRANSFER);
				e2 = cha2.getProperty(ChannelProperties.AUTO_TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 5:
				e1 = cha1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = cha2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 6:
				e1 = cha1.getProperty(ChannelProperties.RECORD);
				e2 = cha2.getProperty(ChannelProperties.RECORD);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 7:
				e1 = cha1.getProperty(ChannelProperties.BUFFER_SIZE);
				e2 = cha2.getProperty(ChannelProperties.BUFFER_SIZE);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 8:
				e1 = cha1.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
				e2 = cha2.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 9:
				e1 = cha1.getProperty(ADWinDigInOutChannelProperties.STIMULUS);
				e2 = cha2.getProperty(ADWinDigInOutChannelProperties.STIMULUS);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}

	public ADWinDigInOutModulePage(FormEditor editor, int id, Module module) {
		super(editor, Integer.toString(id), "Temporary Title", module);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ADWinMessages.ADWinDigInOutConfigurationPage_Title);
		
		
		/*
		 * General configuration section
		 */
		createGeneralConfigurationSection(2, true);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ADWinMessages.DigInOutModuleExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		generalConfigurationSection.setText(ADWinMessages.ADWinDigInOutExplanationsSection_Title);
		generalConfigurationSection.setDescription(ADWinMessages.ADWinDigInOutExplanationsSection_Description);
		
		/*
		 * Channels configuration
		 */
		createTableConfigurationSection(true, true, true, true);
		
		deleteToolItem.setToolTipText(ADWinMessages.DeleteDIO_Tooltip);
		DeleteChannelsHandler deleteChannelsHandler = new DeleteChannelsHandler(getSite().getShell(), this, (ADWinDACQConfiguration) dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()); 
		deleteToolItem.addSelectionListener(deleteChannelsHandler);
		tableViewer.addSelectionChangedListener(deleteChannelsHandler);
		addToolItem.setToolTipText(ADWinMessages.AddDIO_Tooltip);
		addToolItem.addSelectionListener(new AddChannelsHandler(this, module, ((ResourceEditor)getEditor()).getUndoContext()));
		
		FormText explanationsFormText2 = managedForm.getToolkit().createFormText(explanationsContainer, false);
		explanationsFormText2.setText(ADWinMessages.DigInOutModuleExplanations_Text2, true, false);
		
		TableColumnLayout  channelsTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ChannelProperties.NAME.getTooltip(), channelsTableColumnLayout, ChannelProperties.NAME, 100, 0);
		createColumn(ChannelProperties.SAMPLE_FREQUENCY.getTooltip(), channelsTableColumnLayout, ChannelProperties.SAMPLE_FREQUENCY, defaultColumnWidth, 1);
		createColumn(ChannelProperties.CHANNEL_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.CHANNEL_NUMBER, defaultColumnWidth, 2);
		createColumn(ChannelProperties.TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.AUTO_TRANSFER.getTooltip(), channelsTableColumnLayout, ChannelProperties.AUTO_TRANSFER, defaultColumnWidth, 4);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), channelsTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 5);
		createColumn(ChannelProperties.RECORD.getTooltip(), channelsTableColumnLayout, ChannelProperties.RECORD, defaultColumnWidth, 6);
		createColumn(ChannelProperties.BUFFER_SIZE.getTooltip(), channelsTableColumnLayout, ChannelProperties.BUFFER_SIZE, defaultColumnWidth, 7);
		createColumn(ADWinDigInOutChannelProperties.IN_OUT.getTooltip(), channelsTableColumnLayout, ADWinDigInOutChannelProperties.IN_OUT, defaultColumnWidth, 8);
		createColumn(ADWinDigInOutChannelProperties.STIMULUS.getTooltip(), channelsTableColumnLayout, ADWinDigInOutChannelProperties.STIMULUS, defaultColumnWidth, 9);
		
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
					return channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
				case 9:
					return channel.getProperty(ADWinDigInOutChannelProperties.STIMULUS);
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
				case 9:
					value = channel.getProperty(ADWinDigInOutChannelProperties.STIMULUS);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				default:
					return null;
				}
			}
		});
		configureSorter(new DIOComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(module.getChannels());
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
		if(property instanceof ADWinDigInOutChannelProperties) {
			tableViewer.refresh();
			tableConfigurationSectionPart.markDirty();
		}
	}
	
}
