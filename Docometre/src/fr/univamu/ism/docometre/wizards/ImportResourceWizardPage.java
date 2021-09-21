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
package fr.univamu.ism.docometre.wizards;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceType;

public class ImportResourceWizardPage extends WizardPage {

	private TreeViewer resourceTreeViewer;
	private ResourceType selectedResourceType;

	private class ValidatFiles implements FilenameFilter {
		
		private Object selection;

		public ValidatFiles(Object selection) {
			this.selection = selection;
		}

		@Override
		public boolean accept(File dir, String name) {
			boolean valid = false;
			File file = new File(dir, name);
			// If it's a directory, accept only if it contains process or dacq or ...
			if(file.isDirectory()) valid = validateFolder(file, name);
			else valid = validateFile(file, name);
			return valid;
		}
		
		private boolean validateFolder(File folder, String name) {
			boolean valid = false;
			String[] filesNames = folder.list();
			if(filesNames == null) return false;
			for (String fileName : filesNames) {
				File file = new File(folder, fileName);
				// If it's a directory, accept only if it contains process or dacq or ...
				if(file.isDirectory()) valid = valid || validateFolder(file, name);
				else valid = valid || validateFile(file, name);
			}
			return valid;
		}
		
		private boolean validateFile(File file, String name) {
			boolean valid = false;
			if(selection == ResourceType.EXPERIMENT || selection == ResourceType.SUBJECT) {
				valid = name.matches("^[a-zA-Z][a-zA-Z0-9_]*.zip$") || name.matches("^[a-zA-Z][a-zA-Z0-9_]*.tar$");
			} else {
				String extension = Activator.daqFileExtension;
				if(selection == ResourceType.PROCESS) extension = Activator.processFileExtension;
				if(selection == ResourceType.ADW_DATA_FILE) extension = Activator.adwFileExtension;
				if(selection == ResourceType.DATA_PROCESSING) extension = Activator.dataProcessingFileExtension;
				valid = name.matches("^[a-zA-Z][a-zA-Z0-9_]*" + extension + "$");
			}
			return valid;
		}
		
	}
	
	protected ImportResourceWizardPage(String pageName) {
		super(pageName);
		setImageDescriptor(Activator.getImageDescriptor("org.eclipse.ui", "icons/full/wizban/import_wiz.png"));
		setTitle(DocometreMessages.ImportResourceWizardTitle);
		setMessage(DocometreMessages.ImportResourceWizardMessage);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		
		// Destination
		Label destinationResourceLabel = new Label(container, SWT.NORMAL);
		destinationResourceLabel.setText(DocometreMessages.DestinationLabel);
		destinationResourceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Text destinationResourceText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		destinationResourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		destinationResourceText.setText(ImportResourceWizard.getSelectedResource().getFullPath().toOSString());
		
		// Resource type
		Label resourceTypeLabel = new Label(container, SWT.NORMAL);
		resourceTypeLabel.setText(DocometreMessages.ResourceTypeLabel);
		resourceTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Combo resourceTypeCombo = new Combo(container, SWT.READ_ONLY);
		ComboViewer resourceTypeComboViewer = new ComboViewer(resourceTypeCombo);
		resourceTypeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		resourceTypeComboViewer.setContentProvider(new ArrayContentProvider());
		resourceTypeComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(element == ResourceType.DACQ_CONFIGURATION) return DocometreMessages.NewDACQConfigurationAction_Text;
				if(element == ResourceType.PROCESS) return DocometreMessages.NewProcessAction_Text;
				if(element == ResourceType.EXPERIMENT) return DocometreMessages.NewExperimentAction_Text + " (*.zip, *.tar)";
				if(element == ResourceType.ADW_DATA_FILE) return DocometreMessages.NewSubjectFromADWDataFileLabel;
				if(element == ResourceType.SUBJECT) return DocometreMessages.Subjects + " (*.zip, *.tar)";
				if(element == ResourceType.DATA_PROCESSING) return DocometreMessages.DataProcessingTitle;
				return super.getText(element);
			}
		});
		if(!ImportResourceWizard.getSelectedResource().equals(ResourcesPlugin.getWorkspace().getRoot())) {
			resourceTypeComboViewer.setInput(new Object[] {ResourceType.SUBJECT, ResourceType.ADW_DATA_FILE, ResourceType.DACQ_CONFIGURATION, ResourceType.PROCESS, ResourceType.DATA_PROCESSING});
			resourceTypeComboViewer.setSelection(new StructuredSelection(ResourceType.SUBJECT));
			selectedResourceType = ResourceType.SUBJECT;
		} else {
			resourceTypeComboViewer.setInput(new Object[] {ResourceType.EXPERIMENT});
			resourceTypeComboViewer.setSelection(new StructuredSelection(ResourceType.EXPERIMENT));
			selectedResourceType = ResourceType.EXPERIMENT;
		}
		
		
		// Parent folder 
		Label parentFolderLabel = new Label(container, SWT.NORMAL);
		parentFolderLabel.setText(DocometreMessages.ParentFolderLabel);
		parentFolderLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		final Text parentFolderText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		parentFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		browseButton.setText(DocometreMessages.Browse);
		
		// Selection
		Label selectionLabel = new Label(container, SWT.NORMAL);
		selectionLabel.setText(DocometreMessages.SelectedResource);
		selectionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		resourceTreeViewer = new TreeViewer(container);
		resourceTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		resourceTreeViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public boolean hasChildren(Object element) {
				if(!(element instanceof File)) return false;
				File file = (File)element;
				File[] files = file.listFiles(new ValidatFiles(resourceTypeComboViewer.getStructuredSelection().getFirstElement()));
				return files != null && files.length > 0;
			}
			
			@Override
			public Object getParent(Object element) {
				if(!(element instanceof File)) return new Object[0];
				return ((File)element).getParentFile();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if(!(inputElement instanceof File)) return new Object[0];
				File inputFile = (File)inputElement;
				return inputFile.listFiles(new ValidatFiles(resourceTypeComboViewer.getStructuredSelection().getFirstElement()));
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				if(!(parentElement instanceof File)) return new Object[0];
				File inputFile = (File)parentElement;
				return inputFile.listFiles(new ValidatFiles(resourceTypeComboViewer.getStructuredSelection().getFirstElement()));
			}
		});
		resourceTreeViewer.setLabelProvider(new ILabelProvider() {
			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			@Override
			public void dispose() {
			}
			@Override
			public void addListener(ILabelProviderListener listener) {
			}
			@Override
			public String getText(Object element) {
				if(!(element instanceof File)) return null;
				if(((File)element).isDirectory()) return ((File)element).getName();
				String label = ((File)element).getName().replaceAll(Activator.daqFileExtension + "$", "").replaceAll(Activator.processFileExtension + "$", "");
				return label.replaceAll(Activator.dataProcessingFileExtension + "$", "");
			}
			@Override
			public Image getImage(Object element) {
				if(!(element instanceof File)) return null;
				File file = (File)element;
				if(file.getName().endsWith(Activator.daqFileExtension)) return Activator.getImageDescriptor(IImageKeys.DACQ_CONFIGURATION_ICON).createImage();
				if(file.getName().endsWith(Activator.processFileExtension)) return Activator.getImageDescriptor(IImageKeys.PROCESS_ICON).createImage();
				if(file.getName().endsWith(".zip") || file.getName().endsWith("*.tar")) return Activator.getImageDescriptor(IImageKeys.ZIP).createImage();
				if(file.getName().endsWith(Activator.adwFileExtension)) return Activator.getImageDescriptor(IImageKeys.SAMPLES_ICON).createImage();
				if(file.getName().endsWith(Activator.dataProcessingFileExtension)) return Activator.getImageDescriptor(IImageKeys.DATA_PROCESSING_ICON).createImage();
				return  Activator.getImageDescriptor(IImageKeys.FOLDER_ICON).createImage();
			}
		});
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				String parentDirectory = directoryDialog.open();
				if(parentDirectory != null) {
					BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
						@Override
						public void run() {
							resourceTreeViewer.setInput(new File(parentDirectory));
							parentFolderText.setText(parentDirectory);
						}
					});
				}
			}
		});
		
		resourceTypeComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resourceTreeViewer.refresh();
				destinationResourceText.setEnabled(true);
				destinationResourceText.setText(ImportResourceWizard.getSelectedResource().getFullPath().toOSString());
				if(resourceTypeComboViewer.getStructuredSelection().getFirstElement().equals(ResourceType.EXPERIMENT)) {
					destinationResourceText.setEnabled(false);
					destinationResourceText.setText("/");
				}
				selectedResourceType = (ResourceType) resourceTypeComboViewer.getStructuredSelection().getFirstElement();
			}
		});
		
		
		resourceTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(false);
				ITreeSelection selection = resourceTreeViewer.getStructuredSelection();
				Object[] elements = selection.toArray();
				for (Object element : elements) {
					File file = (File)element;
					if(!file.isDirectory()) {
						setPageComplete(true);
						break;
					}
				}
			}
		});
		
		setControl(container);
		setPageComplete(false);

	}
	
	public ITreeSelection getSelection() {
		return resourceTreeViewer.getStructuredSelection();
	}
	
	
	public ResourceType getResourceType() {
		return selectedResourceType;
	}
	

}
