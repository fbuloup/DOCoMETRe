package fr.univamu.ism.docometre.analyse.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.functions.FunctionsHelper;

public class ExportDataWizardPage extends WizardPage {
	
	private LinkedHashSet<Object> selection = new LinkedHashSet<>();
	private Text exportDestinationText;
	private ComboViewer exportTypeComboViewer;
	private Button exportSingleFileButton;
	private Text singleFileNameText;
	private Text separatorText;
	private ListViewer dataListViewer; 

	protected ExportDataWizardPage() {
		super("ExportDataWizardPage");
		setTitle(DocometreMessages.ExportDataWizardPageTitle);
		setDescription(DocometreMessages.ExportDataWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor("org.eclipse.ui", "icons/full/wizban/export_wiz.png"));
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		setControl(container);
		
		// Destination
		Label exportDestinationLabel = new Label(container, SWT.NONE);
		exportDestinationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		exportDestinationLabel.setText(DocometreMessages.ExportDestination);
		exportDestinationText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		exportDestinationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button exportDestinationButton = new Button(container, SWT.FLAT);
		exportDestinationButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		exportDestinationButton.setText(DocometreMessages.Browse);
		exportDestinationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				String response = directoryDialog.open();
				if(response != null) {
					exportDestinationText.setText(response);
					checkPageComplete();
				}
			}
		});
		
		// Export type
		Label exportTypeLabel = new Label(container, SWT.NONE);
		exportTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		exportTypeLabel.setText(DocometreMessages.ExportType);
		exportTypeComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		exportTypeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		exportTypeComboViewer.setContentProvider(new ArrayContentProvider());
		exportTypeComboViewer.setInput(new String[] {"Signal", "Event", "Marker", "Feature"});
		exportTypeComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});
		
		// Export in single file
		exportSingleFileButton = new Button(container, SWT.CHECK);
		exportSingleFileButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		exportSingleFileButton.setText(DocometreMessages.ExportInSingleFile);
		exportSingleFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});
		
		// Single file name
		Label singleFileNameLabel = new Label(container, SWT.NONE);
		singleFileNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		singleFileNameLabel.setText(DocometreMessages.SingleFileName);
		singleFileNameText = new Text(container, SWT.BORDER);
		singleFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		singleFileNameText.setText("data.txt");
		singleFileNameText.setEnabled(false);
		singleFileNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		});
		exportSingleFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				singleFileNameText.setEnabled(exportSingleFileButton.getSelection());
				checkPageComplete();
			}
		});
		
		// Separator
		Label separatorLabel = new Label(container, SWT.NONE);
		separatorLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		separatorLabel.setText(DocometreMessages.Separator);
		separatorText = new Text(container, SWT.BORDER);
		separatorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		separatorText.setText(";");
		separatorText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		});
		
		// Data list to export
		dataListViewer = new ListViewer(container);
		dataListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		dataListViewer.setContentProvider(new ArrayContentProvider());
		dataListViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		dataListViewer.setInput(selection);
		Composite buttonsContainer = new Composite(container, SWT.NORMAL);
		buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.spacing = 5;
		buttonsContainer.setLayout(fillLayout);
		
		Button addButton = new Button(buttonsContainer, SWT.FLAT);
		addButton.setText(DocometreMessages.ExportDataWizardPageAdd);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getShell(), FunctionsHelper.createTextProvider());
				elementListSelectionDialog.setMultipleSelection(true);
				elementListSelectionDialog.setElements(computeSelectionItems());
				if(elementListSelectionDialog.open() == Dialog.OK) {
					Object[] result = elementListSelectionDialog.getResult();
					selection.addAll(Arrays.asList(result));
					dataListViewer.refresh();
					checkPageComplete();
				}
			}
		});
		
		Button removeButton = new Button(buttonsContainer, SWT.FLAT);
		removeButton.setText(DocometreMessages.ExportDataWizardPageRemoveSelection);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection structuredSelection = dataListViewer.getStructuredSelection();
				selection.removeAll(structuredSelection.toList());
				dataListViewer.refresh();
				checkPageComplete();
			}
		});
		
		Button removeAllButton = new Button(buttonsContainer, SWT.FLAT);
		removeAllButton.setText(DocometreMessages.ExportDataWizardPageRemoveAll);
		removeAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selection.clear();
				dataListViewer.refresh();
				checkPageComplete();
			}
		});
		
		
	}
	
	private Object[] computeSelectionItems() {
		String exportType = exportTypeComboViewer.getCombo().getText();
		if(exportType == null) return new Object[0];
		if(!MathEngineFactory.getMathEngine().isStarted()) return new Object[0];
		
		ArrayList<Object> elements = new ArrayList<>();
		
		String[] loadedSubjectsString = MathEngineFactory.getMathEngine().getLoadedSubjects();
		for (String loadedSubjectString : loadedSubjectsString) {
			try {
				if("".equals(loadedSubjectString)) continue;
				String path = loadedSubjectString.replaceAll("\\.", "/");
				IResource subject = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				Object object = subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
				if(object instanceof ChannelsContainer) {
					ChannelsContainer channelsContainer = (ChannelsContainer)object;
					Channel[] channels = null;
					if("Signal".equals(exportType)) channels = channelsContainer.getSignals();
					if("Event".equals(exportType)) channelsContainer.getEvents();
					if("Marker".equals(exportType)) channelsContainer.getMarkers();
					if("Feature".equals(exportType)) channelsContainer.getFeatures();
					elements.addAll(Arrays.asList(channels));
				}
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
			
		}
	
		return elements.toArray();
	}
	
	private void checkPageComplete() {
		String destination = exportDestinationText.getText();
		String exportType = exportTypeComboViewer.getCombo().getText();
		boolean singleFile = exportSingleFileButton.getSelection();
		String singleFileName = singleFileNameText.getText();
		String separator = separatorText.getText();
		setErrorMessage(null);
		setPageComplete(false);
		File destinationFolder = new File(destination);
		if(!destinationFolder.exists()) {
			setErrorMessage(DocometreMessages.DataExportError1);
			return;
		}
		if("".equals(exportType)) {
			setErrorMessage(DocometreMessages.DataExportError2);
			return;
		}
		if(singleFile) {
			if("".equals(singleFileName)) {
				setErrorMessage(DocometreMessages.DataExportError3);
				return;
			}
		}
		if("".equals(separator)) {
			setErrorMessage(DocometreMessages.DataExportError4);
			return;
		}
		if(dataListViewer.getList().getItemCount() == 0) {
			setErrorMessage(DocometreMessages.DataExportError5);
			return;
		}
		setPageComplete(true);
	}
}
