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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoVariable;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoVariableProperties;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.SelectAllButtonHandler;

public class ArduinoUnoVariablesPage extends ArduinoUnoModulePage {
	
	public static String PAGE_ID = "VariablesFormPage";
	
	private class VariablesComparator extends Comparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			ArduinoUnoVariable var1 = (ArduinoUnoVariable) e1;
			ArduinoUnoVariable var2 = (ArduinoUnoVariable) e2;
			switch (sortingColumnNumber) {
			case 0:
				e1 = var1.getProperty(ChannelProperties.NAME);
				e2 = var2.getProperty(ChannelProperties.NAME);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 1:
				e1 = var1.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				e2 = var2.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 2:
				e1 = var1.getProperty(ChannelProperties.TRANSFER);
				e2 = var2.getProperty(ChannelProperties.TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			/*case 3:
				e1 = var1.getProperty(ChannelProperties.AUTO_TRANSFER);
				e2 = var2.getProperty(ChannelProperties.AUTO_TRANSFER);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;*/
			case 3:
				e1 = var1.getProperty(ChannelProperties.TRANSFER_NUMBER);
				e2 = var2.getProperty(ChannelProperties.TRANSFER_NUMBER);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			case 4:
				e1 = var1.getProperty(ChannelProperties.RECORD);
				e2 = var2.getProperty(ChannelProperties.RECORD);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			/*case 6:
				e1 = var1.getProperty(ChannelProperties.BUFFER_SIZE);
				e2 = var2.getProperty(ChannelProperties.BUFFER_SIZE);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;*/
			case 5:
				e1 = var1.getProperty(ArduinoUnoVariableProperties.TYPE);
				e2 = var2.getProperty(ArduinoUnoVariableProperties.TYPE);
				result = super.compare(viewer, (String)e1, (String)e2);
				break;
			case 6:
				e1 = var1.getProperty(ArduinoUnoVariableProperties.SIZE);
				e2 = var2.getProperty(ArduinoUnoVariableProperties.SIZE);
				result = super.compare(Double.parseDouble((String)e1), Double.parseDouble((String)e2));
				break;
			default:
				break;
			}
			return super.computeResult(result);
		}
	}
	
	private PartListenerAdapter partListenerAdapter;
	
	public ArduinoUnoVariablesPage(FormEditor editor) {
		super(editor, PAGE_ID, DocometreMessages.VariablesPage_PageTitle, null);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		Channel[] channels = ((ArduinoUnoDACQConfiguration)dacqConfiguration).getVariables();
		for (int i = 0; i < channels.length; i++) channels[i].addObserver(this);
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == getEditor()) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					Channel[] channels = ((ArduinoUnoDACQConfiguration)dacqConfiguration).getVariables();
					for (int i = 0; i < channels.length; i++) channels[i].removeObserver(ArduinoUnoVariablesPage.this);
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().getForm().setText(ArduinoUnoMessages.VariablesPage_Title);
		
		/*
		 * Section 1 : explanations
		 */
		createGeneralConfigurationSection(2, true);
		generalConfigurationSection.setText(ArduinoUnoMessages.VariablesPageExplanationsSection_Title);
		generalConfigurationSection.setDescription(ArduinoUnoMessages.VariablesPageExplanationsSection_Description);
		
		FormText explanationsFormText = managedForm.getToolkit().createFormText(generalconfigurationContainer, false);
		explanationsFormText.setText(ArduinoUnoMessages.VariablesExplanations_Text, true, false);
		explanationsFormText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		/*
		 * Section 2 : variables configuration
		 */
		createTableConfigurationSection(true, true, false, true);
		tableConfigurationSection.setText(ArduinoUnoMessages.VariablesTableSection_Title);
		tableConfigurationSection.setDescription(ArduinoUnoMessages.VariablesTableSection_Description);
		Button[] buttons = getSelectAllButtons();
		for (int i = 0; i < buttons.length; i++) {
			Button button = buttons[i];
			if(button != null) button.addSelectionListener(new SelectAllButtonHandler(button, dacqConfiguration, (ChannelProperties) button.getData(), ((ResourceEditor)getEditor()).getUndoContext()));
		}
		
		DeleteChannelsHandler deleteVariablesHandler = new DeleteChannelsHandler(getSite().getShell(), this, (ArduinoUnoDACQConfiguration) dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()); 
		deleteToolItem.addSelectionListener(deleteVariablesHandler);
		tableViewer.addSelectionChangedListener(deleteVariablesHandler);
		addToolItem.addSelectionListener(new AddChannelsHandler(this, dacqConfiguration, ((ResourceEditor)getEditor()).getUndoContext()));
		
		TableColumnLayout  variablesTableColumnLayout = (TableColumnLayout) tableConfigurationContainer.getLayout();
		createColumn(ChannelProperties.NAME.getTooltip(), variablesTableColumnLayout, ChannelProperties.NAME, 100, 0);
		createColumn(ChannelProperties.SAMPLE_FREQUENCY.getTooltip(), variablesTableColumnLayout, ChannelProperties.SAMPLE_FREQUENCY, defaultColumnWidth, 1);
		createColumn(ChannelProperties.TRANSFER.getTooltip(), variablesTableColumnLayout, ChannelProperties.TRANSFER, defaultColumnWidth, 2);
		//createColumn(ChannelProperties.AUTO_TRANSFER.getTooltip(), variablesTableColumnLayout, ChannelProperties.AUTO_TRANSFER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.TRANSFER_NUMBER.getTooltip(), variablesTableColumnLayout, ChannelProperties.TRANSFER_NUMBER, defaultColumnWidth, 3);
		createColumn(ChannelProperties.RECORD.getTooltip(), variablesTableColumnLayout, ChannelProperties.RECORD, defaultColumnWidth, 4);
		//createColumn(ChannelProperties.BUFFER_SIZE.getTooltip(), variablesTableColumnLayout, ChannelProperties.BUFFER_SIZE, defaultColumnWidth, 6);
		createColumn(ArduinoUnoVariableProperties.TYPE.getTooltip(), variablesTableColumnLayout, ArduinoUnoVariableProperties.TYPE, defaultColumnWidth, 5);
		createColumn(ArduinoUnoVariableProperties.SIZE.getTooltip(), variablesTableColumnLayout, ArduinoUnoVariableProperties.SIZE, defaultColumnWidth, 6);
		
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
					return channel.getProperty(ChannelProperties.TRANSFER);
				/*case 3:
					return channel.getProperty(ChannelProperties.AUTO_TRANSFER);*/
				case 3:
					return channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
				case 4:
					return channel.getProperty(ChannelProperties.RECORD);
				/*case 6:
					return channel.getProperty(ChannelProperties.BUFFER_SIZE);*/
				case 5:
					return channel.getProperty(ArduinoUnoVariableProperties.TYPE);
				case 6:
					return channel.getProperty(ArduinoUnoVariableProperties.SIZE);
				default:
					return "";
				}
			}
			public Image getColumnImage(Object element, int columnIndex) {
				Channel channel = (Channel)element;
				String value = "false";
				switch (columnIndex) {
				case 2:
					value = channel.getProperty(ChannelProperties.TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				/*case 3:
					value = channel.getProperty(ChannelProperties.AUTO_TRANSFER);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;*/
				case 4:
					value = channel.getProperty(ChannelProperties.RECORD);
					return value.equals("true") ? ModulePage.checkedImage : ModulePage.uncheckedImage;
				default:
					return null;
				}
			}
		});
		configureSorter(new VariablesComparator(), tableViewer.getTable().getColumn(0));
		tableViewer.setInput(((ArduinoUnoDACQConfiguration)dacqConfiguration).getVariables());
		
	}
	
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		super.update(property, newValue, oldValue, element);
		if(property == ChannelProperties.TRANSFER) {
			ArduinoUnoVariable arduinoVariable = (ArduinoUnoVariable)element;
			
			/*boolean isParameter = "true".equals(adwinVariable.getProperty(ArduinoUnoVariableProperties.PARAMETER));*/
			boolean isTransfered = "true".equals(arduinoVariable.getProperty(ChannelProperties.TRANSFER));
			/*boolean isAutoTransfered = "true".equals(adwinVariable.getProperty(ChannelProperties.AUTO_TRANSFER));
			if(isParameter && isTransfered) adwinVariable.setProperty(ChannelProperties.TRANSFER, "false");
			if(isParameter && isAutoTransfered) adwinVariable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");*/
			
			/*boolean isString = ADWinVariableProperties.STRING.equals(adwinVariable.getProperty(ADWinVariableProperties.TYPE));
			if(isString && isTransfered) adwinVariable.setProperty(ChannelProperties.TRANSFER, "false");
			if(isString && isAutoTransfered) adwinVariable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");*/

			boolean isArray = Integer.parseInt(arduinoVariable.getProperty(ArduinoUnoVariableProperties.SIZE)) > 1;
			if(isArray && isTransfered) {
				arduinoVariable.setProperty(ChannelProperties.TRANSFER, "false");
				MessageDialog.openInformation(getSite().getShell(), DocometreMessages.TransferInfo_MessageTitle, DocometreMessages.TransferInfo_MessageContent); 
			}
			/*if(isArray && isAutoTransfered) adwinVariable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");*/
			
		}
		if(property == ArduinoUnoVariableProperties.TYPE) {
			/*ArduinoUnoVariable adwinVariable = (ArduinoUnoVariable)element;
			boolean isTransfered = "true".equals(adwinVariable.getProperty(ChannelProperties.TRANSFER));
			boolean isAutoTransfered = "true".equals(adwinVariable.getProperty(ChannelProperties.AUTO_TRANSFER));
			boolean isString = ArduinoUnoVariableProperties.STRING.equals(adwinVariable.getProperty(ArduinoUnoVariableProperties.TYPE));
			if(isString && isTransfered) adwinVariable.setProperty(ChannelProperties.TRANSFER, "false");
			if(isString && isAutoTransfered) adwinVariable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");*/
			((ArduinoUnoDACQConfiguration)dacqConfiguration).updateChannelsTransferNumber();
		}
		
		if(property == ArduinoUnoVariableProperties.SIZE) {
			ArduinoUnoVariable adwinVariable = (ArduinoUnoVariable)element;
			boolean isTransfered = "true".equals(adwinVariable.getProperty(ChannelProperties.TRANSFER));
			boolean isAutoTransfered = "true".equals(adwinVariable.getProperty(ChannelProperties.AUTO_TRANSFER));
			boolean isArray = Integer.parseInt(adwinVariable.getProperty(ArduinoUnoVariableProperties.SIZE)) > 1;
			if(isArray && isTransfered) adwinVariable.setProperty(ChannelProperties.TRANSFER, "false");
			if(isArray && isAutoTransfered) adwinVariable.setProperty(ChannelProperties.AUTO_TRANSFER, "false");
			((ArduinoUnoDACQConfiguration)dacqConfiguration).updateChannelsTransferNumber();
		}
		
		if(property instanceof ArduinoUnoVariableProperties) {
			tableViewer.refresh();
			tableConfigurationSectionPart.markDirty();
		}
	}

	@Override
	public String getPageTitle() {
		return DocometreMessages.VariablesPage_PageTitle;
	}

}
