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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;

public class ExportExperimentWizardPage extends WizardPage {
	
	private boolean asZip = true;
	private boolean compress= true;
	private IProject experiment;
	private String destination;
	private boolean includeData = true;
	private boolean exportOnlySelectedSubjects;
	protected IStructuredSelection selectedSubjects;
	private ListViewer subjectsListViewer;
	
	private static class ViewerLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			if(element instanceof IProject) return Activator.getImage(IImageKeys.EXPERIMENT_ICON);
			return super.getImage(element);
		}
		public String getText(Object element) {
			if(!(element instanceof IResource)) return "";
			return ((IResource)element).getName();
		}
	}
	
	private static class ContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof IWorkspaceRoot) return ((IWorkspaceRoot)inputElement).getProjects();
			return new Object[0];
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private static class SubjectsContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof IProject) return ResourceProperties.getAllTypedResources(ResourceType.SUBJECT, (IContainer) inputElement, null);
			return new Object[0];
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	public ExportExperimentWizardPage() {
		super("ExportResourceWizardPage");
		setTitle(DocometreMessages.ExportResourceWizardPageTitle);
		setDescription(DocometreMessages.ExportResourceWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor("org.eclipse.ui", "icons/full/wizban/export_wiz.png"));
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);

		setControl(container);
		container.setLayout(new GridLayout(3, false));
		
		Label exportProjectLabel = new Label(container, SWT.NONE);
		exportProjectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		exportProjectLabel.setText(DocometreMessages.ProjectToExportLabel);
		
		ComboViewer exportProjectComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		exportProjectComboViewer.setComparator(new ViewerComparator());
		Combo exportProjectCombo = exportProjectComboViewer.getCombo();
		exportProjectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		exportProjectComboViewer.setLabelProvider(new ViewerLabelProvider());
		exportProjectComboViewer.setContentProvider(new ContentProvider());
		exportProjectComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				experiment = (IProject)((StructuredSelection)exportProjectComboViewer.getSelection()).getFirstElement();
				if(exportOnlySelectedSubjects) subjectsListViewer.setInput(experiment);
				validate();
			}
		});
		
		Label exportDestinationLabel = new Label(container, SWT.NONE);
		exportDestinationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		exportDestinationLabel.setText(DocometreMessages.DestinationFolderLabel);
		
		Text exportDestinationText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		exportDestinationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		exportDestinationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				destination = exportDestinationText.getText();
				validate();
			}
		});
		
		Button browseButton = new Button(container, SWT.NONE);
		browseButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				String directoryPath = directoryDialog.open();
				if(directoryPath != null) {
					String extension = asZip?".zip":".tar";
					String name = "filename";
					if(experiment != null) name = experiment.getName();
					directoryPath = directoryPath + File.separator + name + extension;
					exportDestinationText.setText(directoryPath);
				}
			}
		});
		browseButton.setText(DocometreMessages.Browse);
		
		Group optionsGroup = new Group(container, SWT.NONE);
		optionsGroup.setText(DocometreMessages.OptionsGroupLabel);
		optionsGroup.setLayout(new GridLayout(1, false));
		optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		
		Button exportAsZipRadioButton = new Button(optionsGroup, SWT.RADIO);
		exportAsZipRadioButton.setSelection(true);
		exportAsZipRadioButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		exportAsZipRadioButton.setText(DocometreMessages.SaveInZipFormatLabel);
		exportAsZipRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				asZip = exportAsZipRadioButton.getSelection();
			}
		});
		
		Button exportAsTarRadioButton = new Button(optionsGroup, SWT.RADIO);
		exportAsTarRadioButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		exportAsTarRadioButton.setText(DocometreMessages.SaveInTarFormatLabel);
		
		Button compressCheckBox = new Button(optionsGroup, SWT.CHECK);
		compressCheckBox.setSelection(true);
		compressCheckBox.setText(DocometreMessages.CompressLabel);
		compressCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				compress = compressCheckBox.getSelection();
			}
		});
		
		Button exportWithDataButton = new Button(optionsGroup, SWT.CHECK);
		exportWithDataButton.setText(DocometreMessages.ExportWithDataTitle);
		exportWithDataButton.setSelection(true);
		exportWithDataButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeData = exportWithDataButton.getSelection();
			}
		});
		
		exportProjectComboViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		
		Button exportOnlySelectedSubjectsButton = new Button(container, SWT.CHECK);
		exportOnlySelectedSubjectsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOnlySelectedSubjects = exportOnlySelectedSubjectsButton.getSelection();
				if(exportOnlySelectedSubjects && experiment != null) subjectsListViewer.setInput(experiment);
				else if (!exportOnlySelectedSubjects) subjectsListViewer.setInput(null);
			}
		});
		exportOnlySelectedSubjectsButton.setText(DocometreMessages.ExportOnlySelectedSubjectTitle);
		exportOnlySelectedSubjectsButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		subjectsListViewer = new ListViewer(container);
		subjectsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		subjectsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(element instanceof IResource) return ((IResource)element).getName();
				return super.getText(element);
			}
		});
		subjectsListViewer.setContentProvider(new SubjectsContentProvider());
		subjectsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedSubjects = (IStructuredSelection) subjectsListViewer.getSelection();
			}
		});
		Label warningLabel = new Label(container, SWT.WRAP);
		warningLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1));
		warningLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
		warningLabel.setText(DocometreMessages.ExportWarningMessage);
		
	}
	
	public boolean validate() {
		setErrorMessage(null);
		if(experiment == null) setErrorMessage(DocometreMessages.PleaseSelectAnExperimentMessage);
		else if(destination == null) setErrorMessage(DocometreMessages.PleaseSelectADestinationMessage);
		else {
			File destinationFile = new File(destination);
			if(destinationFile.exists()) setErrorMessage(DocometreMessages.DestinationAlreadyExistsMessage); 
		}
		setPageComplete(getErrorMessage() == null);
		return getErrorMessage() == null;
	}

	public boolean isAsZip() {
		return asZip;
	}

	public boolean isCompress() {
		return compress;
	}

	public IProject getExperiment() {
		return experiment;
	}

	public String getDestination() {
		return destination;
	}
	
	public boolean isIncludeData() {
		return includeData;
	}
	
	public boolean isOnlyExportSubjects() {
		return exportOnlySelectedSubjects;
	}
	
	@SuppressWarnings("unchecked")
	public List<IResource> getSelectedSubjects() {
		ArrayList<IResource> elements = new ArrayList<>();
		if(selectedSubjects != null) elements.addAll(selectedSubjects.toList());
		return elements;
	}
	
}
