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

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;

import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.ADwinDACQGeneralConfigurationPage;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor.ArduinoUnoDACQGeneralConfigurationPage;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor.ArduinoUnoVariablesPage;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.ADWinVariablesPage;

import fr.univamu.ism.docometre.dacqsystems.charts.ChartsConfigurationPage;

public abstract class ModulePage extends FormPage implements PropertyObserver {

	public static int defaultColumnWidth = 70;
	public static Image checkedImage = Activator.getImageDescriptor(IImageKeys.CHECK_BOX_CHECKED_ICON).createImage();
	public static Image uncheckedImage = Activator.getImageDescriptor(IImageKeys.CHECK_BOX_UNCHECKED_ICON).createImage();
	
	public class ModuleSectionPart extends SectionPart {

		public ModuleSectionPart(Section section) {
			super(section);
		}
		
		@Override
		public void commit(boolean onSave) {
			if(onSave) super.commit(true);
		}
		
	}
	
	private class GeneralConfigurationModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			generalConfigurationSectionPart.markDirty();
		}
	}
	
	private class tableConfigurationModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			tableConfigurationSectionPart.markDirty();
		}
	}
	
	protected class Comparator extends ViewerComparator {
		protected int columnNumber;
		protected boolean ascendingDirection = true; 
		public Comparator() {
			super();
		}
		public void setSortingColumn(int columnNumber) {
			if(this.columnNumber == columnNumber) {
				ascendingDirection = !ascendingDirection;
			} else {
				this.columnNumber = columnNumber;
				ascendingDirection = true;
			}
		}
		protected int compare(double value1, double value2) {
			return (value1 == value2 ? 0 : ( value1 > value2 ? 1 : -1));
		}
		public int getDirection() {
			return ascendingDirection == true ? SWT.UP : SWT.DOWN;
		}
		public int computeResult(int result) {
			if(!ascendingDirection) return -result;
			return result;
		}
	}
	
	private IManagedForm managedForm;
	
	protected DACQConfiguration dacqConfiguration;

	protected Module module;
	
	protected Section generalConfigurationSection;
	protected ModuleSectionPart generalConfigurationSectionPart;
	protected Composite generalconfigurationContainer;
	
	protected Section tableConfigurationSection;
	protected ModuleSectionPart tableConfigurationSectionPart;
	protected Composite explanationsContainer;
	protected Composite tableConfigurationContainer;
	protected TableViewer tableViewer;
	
	protected ToolItem deleteToolItem;
	protected ToolItem addToolItem;
	private GeneralConfigurationModifyListener generalConfigurationModifyListener;
	private tableConfigurationModifyListener tableConfigurationModifyListener;
	private PartListenerAdapter partListener;
	private EditingSupport editingSupport;
	private Button selectAll_T_Button;
	private Button selectAll_AT_Button;
	private Button selectAll_R_Button;

	public ModulePage(FormEditor editor, String id, String title, Module module) {
		super(editor, id, title);
		this.module = module;
		generalConfigurationModifyListener = new GeneralConfigurationModifyListener();
		tableConfigurationModifyListener = new tableConfigurationModifyListener();
	}
	
	public GeneralConfigurationModifyListener getGeneralConfigurationModifyListener() {
		return generalConfigurationModifyListener;
	}

	public tableConfigurationModifyListener getTableConfigurationModifyListener() {
		return tableConfigurationModifyListener;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)input;
		dacqConfiguration = (DACQConfiguration)(resourceEditorInput.getObject());
//		daqConfiguration = ((DAQConfigurationHolder)((ResourceEditorInput)getEditorInput()).getResource()).getConfiguration();
		((ResourceEditor)getEditor()).updateTitle(getIndex(), getPageTitle());
		if(module != null) module.addObserver(this);
		dacqConfiguration.addObserver(this);
		Channel[] channels = new Channel[0];
		if(module != null) channels = module.getChannels();
		for (int i = 0; i < channels.length; i++) channels[i].addObserver(this);
		partListener = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == getEditor()) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListener);
					Channel[] channels = new Channel[0];
					if(module != null) {
						ModulePage.this.module.removeObserver(ModulePage.this);
						channels = module.getChannels();
					}
					for (int i = 0; i < channels.length; i++) channels[i].removeObserver(ModulePage.this);
					dacqConfiguration.removeObserver(ModulePage.this);
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListener);
	}

	public abstract String getPageTitle();
	
	public Module getModule() {
		return module;
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.getToolkit().decorateFormHeading(managedForm.getForm().getForm());
		managedForm.getForm().getForm().setImage(Activator.getImage(IImageKeys.DACQ_CONFIGURATION_ICON));
		managedForm.getForm().getBody().setLayout(new GridLayout(1, false));
		this.managedForm = managedForm;
	}
	
	protected void createGeneralConfigurationSection(int nbColumns, boolean showTwistie) {
		/*
		 * Section 1 : general configuration
		 */
		int flags = ExpandableComposite.TITLE_BAR | Section.DESCRIPTION;
		if(showTwistie) flags = flags | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE;
		generalConfigurationSection = managedForm.getToolkit().createSection(managedForm.getForm().getForm().getBody(), flags);
		generalConfigurationSection.setText(DocometreMessages.GeneralConfigurationModuleSection_Title);
		generalConfigurationSection.setDescription(DocometreMessages.GeneralConfigurationModuleSection_Description);
		generalConfigurationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		generalconfigurationContainer = managedForm.getToolkit().createComposite(generalConfigurationSection);
		GridLayout gridLayout = new GridLayout(nbColumns, false);
		gridLayout.horizontalSpacing = 15;
		generalconfigurationContainer.setLayout(gridLayout);
		
		generalConfigurationSection.setClient(generalconfigurationContainer);
		
		generalConfigurationSectionPart = new ModuleSectionPart(generalConfigurationSection);
		managedForm.addPart(generalConfigurationSectionPart);
		
	}
	
	
	protected void createTableConfigurationSection(boolean addToobar, boolean addTransfertButton, boolean addAutoTransfertButton, boolean addRecButton) {
		/*
		 * Section 2 : channels or modules section configuration => table configuration
		 */
		tableConfigurationSection = managedForm.getToolkit().createSection(managedForm.getForm().getForm().getBody(), ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
		tableConfigurationSection.setText(DocometreMessages.ChannelsConfigurationModuleSection_Title);
		tableConfigurationSection.setDescription(DocometreMessages.ChannelsConfigurationModuleSection_Description);
		tableConfigurationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tableConfigurationSectionPart = new ModuleSectionPart(tableConfigurationSection);
		managedForm.addPart(tableConfigurationSectionPart);
		
		if(addToobar) {
			//Tool bar
			ToolBar toolBar = new ToolBar(tableConfigurationSection, SWT.FLAT | SWT.HORIZONTAL);
			deleteToolItem = new ToolItem(toolBar, SWT.NULL);
			deleteToolItem.setImage(Activator.getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE).createImage());
			new ToolItem(toolBar, SWT.SEPARATOR);
			addToolItem = new ToolItem(toolBar, SWT.NULL);
			addToolItem.setImage(Activator.getImage(IImageKeys.ADD_ICON));
			tableConfigurationSection.setTextClient(toolBar);
		}
		
		Composite explanationsAndTableConfigurationContainer = managedForm.getToolkit().createComposite(tableConfigurationSection, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout. numColumns = 2;
		explanationsAndTableConfigurationContainer.setLayout(gridLayout);
		
		explanationsContainer = managedForm.getToolkit().createComposite(explanationsAndTableConfigurationContainer, SWT.NONE);
		explanationsContainer.setLayout(new FillLayout());
		explanationsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		tableConfigurationContainer = managedForm.getToolkit().createComposite(explanationsAndTableConfigurationContainer, SWT.NONE);
		tableConfigurationContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout channelsTableColumnLayout = new TableColumnLayout();
		tableConfigurationContainer.setLayout(channelsTableColumnLayout);
		
		Composite buttonsTableContainer = null;
		if(addTransfertButton || addAutoTransfertButton || addRecButton) {
			buttonsTableContainer = managedForm.getToolkit().createComposite(explanationsAndTableConfigurationContainer, SWT.NONE);
			buttonsTableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			buttonsTableContainer.setLayout(new GridLayout());
		}
		if(addTransfertButton) {
			selectAll_T_Button = createButton(buttonsTableContainer, "Transfert", SWT.PUSH, 1, 1);
			selectAll_T_Button.setImage(Activator.getImage(IImageKeys.SELECT_ALL));
			selectAll_T_Button.setData(ChannelProperties.TRANSFER);
			selectAll_T_Button.addSelectionListener(new SelectAllButtonHandler(selectAll_T_Button, getModule(), ChannelProperties.TRANSFER, ((ResourceEditor)getEditor()).getUndoContext()));
		}
		if(addAutoTransfertButton) {
			selectAll_AT_Button = createButton(buttonsTableContainer, "Auto-T.", SWT.PUSH, 1, 1);
			selectAll_AT_Button.setImage(Activator.getImage(IImageKeys.SELECT_ALL));
			selectAll_AT_Button.setData(ChannelProperties.AUTO_TRANSFER);
			selectAll_AT_Button.addSelectionListener(new SelectAllButtonHandler(selectAll_AT_Button, getModule(), ChannelProperties.AUTO_TRANSFER, ((ResourceEditor)getEditor()).getUndoContext()));
		}
		if(addRecButton) {
			selectAll_R_Button = createButton(buttonsTableContainer, "Rec.", SWT.PUSH, 1, 1);
			selectAll_R_Button.setImage(Activator.getImage(IImageKeys.SELECT_ALL));
			selectAll_R_Button.setData(ChannelProperties.RECORD);
			selectAll_R_Button.addSelectionListener(new SelectAllButtonHandler(selectAll_R_Button, getModule(), ChannelProperties.RECORD, ((ResourceEditor)getEditor()).getUndoContext()));
		}
		
		Table channelsTable = managedForm.getToolkit().createTable(tableConfigurationContainer, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		channelsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		managedForm.getToolkit().paintBordersFor(channelsTable);
		channelsTable.setHeaderVisible(true);
		channelsTable.setLinesVisible(true);
		tableViewer = new TableViewer(channelsTable);

		if(addTransfertButton || addAutoTransfertButton || addRecButton) {
			if(addTransfertButton) selectAll_T_Button.setData("TABLEVIEWER", tableViewer);
			if(addAutoTransfertButton) selectAll_AT_Button.setData("TABLEVIEWER", tableViewer);
			if(addRecButton) selectAll_R_Button.setData("TABLEVIEWER", tableViewer);
		}
		
		tableConfigurationSection.setClient(explanationsAndTableConfigurationContainer);
	}
	
	protected void configureSorter(Comparator comparator, TableColumn tableColumn) {
		ArrayList<TableColumn> columns = new ArrayList<>();
		columns.addAll(Arrays.asList(tableViewer.getTable().getColumns()));
		comparator.setSortingColumn(columns.indexOf(tableColumn)); 
		tableViewer.setComparator(comparator);
		tableViewer.getTable().setSortDirection(SWT.UP);
		tableViewer.getTable().setSortColumn(tableColumn);
	}
	
	/*
	 * Helper method to create label widget
	 */
	public Label createLabel(Composite container, String title, String tooltip) {
		Label label = managedForm.getToolkit().createLabel(container, title);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		label.setToolTipText(tooltip);
		return label;
	}
	
	private void setModuleData(Control control) {
		if(this instanceof ADwinDACQGeneralConfigurationPage) control.setData("module", ADwinDACQGeneralConfigurationPage.PAGE_ID);
		else if(this instanceof ArduinoUnoDACQGeneralConfigurationPage) control.setData("module", ArduinoUnoDACQGeneralConfigurationPage.PAGE_ID);
		else if(this instanceof ADWinVariablesPage) control.setData("module", ADWinVariablesPage.PAGE_ID);
		else if(this instanceof ArduinoUnoVariablesPage) control.setData("module", ArduinoUnoVariablesPage.PAGE_ID);
		else if(this instanceof ChartsConfigurationPage) control.setData("module", ChartsConfigurationPage.PAGE_ID);
		else control.setData("module", getModule());
	}
	
	/*
	 * Helper method to create text widget
	 */
	public Text createText(Composite container, String initialValue, int style, int horspan, int vertspan) {
		Text text = managedForm.getToolkit().createText(container, initialValue, style);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horspan, vertspan));
		setModuleData(text);
		return text;
	}
	
	/*
	 * Helper method to create text combo
	 */
	public Combo createCombo(Composite container, String[] items, String initialValue, int horspan, int vertspan) {
		Combo combo = new Combo(container, SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horspan, vertspan));
		combo.setItems(items);
		combo.select(combo.indexOf(initialValue));
		setModuleData(combo);
		return combo;
	}
	
	/*
	 * Helper method to create button widget
	 */
	public Button createButton(Composite container, String title, int style, int horspan, int vertspan) {
		Button button = managedForm.getToolkit().createButton(container, title, style);//SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, horspan, vertspan));
		setModuleData(button);
		return button;
	}
	
	/*
	 * Helper method to create hyperlink widget
	 */
	public Hyperlink createHyperlink(Composite container, String initialValue, int horspan, int vertspan) {
		Hyperlink hyperlink = managedForm.getToolkit().createHyperlink(container, initialValue, SWT.WRAP);
		hyperlink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, horspan, vertspan));
		setModuleData(hyperlink);
		return hyperlink;
	}
	
	protected TableViewerColumn createColumn(String tooltip, TableColumnLayout variablesTableColumnLayout, Property property, int columnWidth, final int columnNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerColumn.getColumn().setText(property.getLabel());
		viewerColumn.getColumn().setToolTipText(tooltip);
		viewerColumn.getColumn().setMoveable(false);
		viewerColumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator comparator = (Comparator) tableViewer.getComparator();
				if(comparator != null) {
					comparator.setSortingColumn(columnNumber);
					tableViewer.getTable().setSortDirection(comparator.getDirection());
					tableViewer.getTable().setSortColumn(viewerColumn.getColumn());
					tableViewer.refresh();
				}
			}
		});
		variablesTableColumnLayout.setColumnData(viewerColumn.getColumn(), new ColumnPixelData(columnWidth, true, false));
		
		if(this instanceof ADwinDACQGeneralConfigurationPage) tableViewer.getTable().setData("module", ADwinDACQGeneralConfigurationPage.PAGE_ID);
		else if(this instanceof ADWinVariablesPage) tableViewer.getTable().setData("module", ADWinVariablesPage.PAGE_ID);
		else if(this instanceof ChartsConfigurationPage) tableViewer.getTable().setData("module", ChartsConfigurationPage.PAGE_ID);
		else if(this instanceof ArduinoUnoVariablesPage) tableViewer.getTable().setData("module", ArduinoUnoVariablesPage.PAGE_ID);
		else tableViewer.getTable().setData("module", getModule());
		tableViewer.getTable().setData("tableViewer", tableViewer);
		editingSupport = ChannelEditingSupportFactory.getEditingSupport(this, tableViewer, property, dacqConfiguration, (ResourceEditor) getEditor());
		viewerColumn.setEditingSupport(editingSupport);
		
		return viewerColumn;
	}
	
	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == DACQConfigurationProperties.UPDATE_MODULE) {
			if(module != null && oldValue == module) {
				dacqConfiguration.removeObserver(this);
				if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
			}
		}
		if(property == ChannelProperties.SAMPLE_FREQUENCY) {
			if(checkTableViewerOk()) {
				tableViewer.refresh();
				if(tableConfigurationSectionPart != null) tableConfigurationSectionPart.markDirty();
			}
		}
		if(property instanceof ChannelProperties) {
			if(tableConfigurationSectionPart != null) {
				tableViewer.refresh();
				tableConfigurationSectionPart.markDirty();
			}
		}
	}
	
	protected boolean checkTableViewerOk() {
		return tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed();
	}

	public void commit() {
		if(managedForm != null) {
			managedForm.commit(true);
			managedForm.dirtyStateChanged();
		}
	}
	
	public Button[] getSelectAllButtons() {
		return new Button[] {selectAll_T_Button, selectAll_AT_Button, selectAll_R_Button};
	}
}
