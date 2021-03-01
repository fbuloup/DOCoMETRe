package fr.univamu.ism.docometre.analyse.wizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class ExportDataWizardPage extends WizardPage {

	protected ExportDataWizardPage() {
		super("ExportDataWizardPage");
		setTitle(DocometreMessages.ExportDataWizardPageTitle);
		setDescription(DocometreMessages.ExportDataWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor("org.eclipse.ui", "icons/full/wizban/export_wiz.png"));
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		setControl(container);
		
		// Destination
		Label exportDestinationLabel = new Label(container, SWT.NONE);
		exportDestinationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		exportDestinationLabel.setText("Export destination :");
		Text exportDestinationText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
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
				}
			}
		});
		
		// Export type
		Label exportTypeLabel = new Label(container, SWT.NONE);
		exportTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		exportTypeLabel.setText("Export type :");
		ComboViewer exportTypeComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		exportTypeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		exportTypeComboViewer.setContentProvider(new ArrayContentProvider());
		exportTypeComboViewer.setInput(new String[] {"Signal", "Event", "Marker", "Field"});
		
		// Export in single file
		Button exportSingleFileButton = new Button(container, SWT.CHECK);
		exportSingleFileButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		exportSingleFileButton.setText("Export in single file");
		
		// Single file name
		Label singleFileNameLabel = new Label(container, SWT.NONE);
		singleFileNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		singleFileNameLabel.setText("Single file name :");
		Text singleFileNameText = new Text(container, SWT.BORDER);
		singleFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		singleFileNameText.setText("data.txt");
		singleFileNameText.setEnabled(false);
		
		exportSingleFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				singleFileNameText.setEnabled(exportSingleFileButton.getSelection());
			}
		});
		
		// Data list to export
		ListViewer dataListViewer = new ListViewer(container);
		dataListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		Composite buttonsContainer = new Composite(container, SWT.NORMAL);
		buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.spacing = 5;
		buttonsContainer.setLayout(fillLayout);
		
		Button addButton = new Button(buttonsContainer, SWT.FLAT);
		addButton.setText(DocometreMessages.ExportDataWizardPageAdd);
		
		Button removeButton = new Button(buttonsContainer, SWT.FLAT);
		removeButton.setText(DocometreMessages.ExportDataWizardPageRemoveSelection);
		
		Button removeAllButton = new Button(buttonsContainer, SWT.FLAT);
		removeAllButton.setText(DocometreMessages.ExportDataWizardPageRemoveAll);
		
	}

}
