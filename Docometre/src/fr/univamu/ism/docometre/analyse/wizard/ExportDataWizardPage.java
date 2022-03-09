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
	private String destination;
	private String exportType;
	private boolean singleFile;
	private String singleFileName;
	private String separator; 

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
		exportTypeComboViewer.setInput(new String[] {ExportDataWizard.SIGNAL_TYPE, ExportDataWizard.EVENT_TYPE, ExportDataWizard.MARKER_TYPE, ExportDataWizard.FEATURE_TYPE});
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
		checkPageComplete();
		String exportType = exportTypeComboViewer.getCombo().getText();
		if(exportType == null || "".equals(exportType)) return new Object[0];
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
					if("Event".equals(exportType)) channels = channelsContainer.getEvents();
					if("Marker".equals(exportType)) channels = channelsContainer.getMarkers();
					if("Feature".equals(exportType)) channels = channelsContainer.getFeatures();
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
		destination = exportDestinationText.getText();
		exportType = exportTypeComboViewer.getCombo().getText();
		singleFile = exportSingleFileButton.getSelection();
		singleFileName = singleFileNameText.getText();
		separator = separatorText.getText();
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

	public LinkedHashSet<Object> getSelection() {
		return selection;
	}

	public String getDestination() {
		return destination;
	}

	public String getExportType() {
		return exportType;
	}

	public boolean isSingleFile() {
		return singleFile;
	}

	public String getSingleFileName() {
		return singleFileName;
	}

	public String getSeparator() {
		return separator;
	}
	
	
	
}
