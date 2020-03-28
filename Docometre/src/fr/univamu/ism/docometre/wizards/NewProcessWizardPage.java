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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;

public class NewProcessWizardPage extends NewResourceWizardPage {

	private ControlDecoration controlDecoration2;
	private ComboViewer daqConfigurationComboViewer;
	private IFile associatedDAQConfiguration;

	protected NewProcessWizardPage() {
		super(DocometreMessages.NewProcessWizard_PageName, ResourceType.PROCESS);
		setTitle(DocometreMessages.NewProcessWizard_PageTitle);
		setMessage(DocometreMessages.NewProcessWizard_PageMessage);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		resourceType = ResourceType.PROCESS;
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		Composite container = (Composite) getControl();
		CLabel associatedDAQConfigurationLabel = new CLabel(container, SWT.NORMAL);
		associatedDAQConfigurationLabel.setText(DocometreMessages.NewProcessWizard_AssociatedDAQConfiguration);
		associatedDAQConfigurationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		daqConfigurationComboViewer = new ComboViewer(container);
		daqConfigurationComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		IResource[] resources = ResourceProperties.getAllTypedResources(ResourceType.DACQ_CONFIGURATION, newResourceWizard.getProject());
		IResource defaultDAQConfiguration = null;
		String defaultDAQConfigurationFullPath = ResourceProperties.getDefaultDACQPersistentProperty(((NewResourceWizard)getWizard()).getParentResource());
		for (IResource resource : resources) {
			if(resource.getFullPath().toOSString().equals(defaultDAQConfigurationFullPath)) {
				defaultDAQConfiguration = resource;
				break;
			}
		}
		
		daqConfigurationComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		daqConfigurationComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(element instanceof IResource) {
					IResource resource = (IResource)element;
					return ((IResource)element).getName().replaceAll("." + resource.getFileExtension(), "");
				}
				return super.getText(element);
			}
		});
		daqConfigurationComboViewer.setInput(resources);
		if(defaultDAQConfiguration != null) daqConfigurationComboViewer.setSelection(new StructuredSelection(defaultDAQConfiguration));
		daqConfigurationComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				modifyHandler();
			}
		});
		
		controlDecoration2 = new ControlDecoration(daqConfigurationComboViewer.getControl(), SWT.LEFT | SWT.TOP);
		controlDecoration2.setDescriptionText(DocometreMessages.NewProcessWizard_ErrorMessage);
		controlDecoration2.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		
		modifyHandler();
	}
	
	protected void modifyHandler() {
		if(daqConfigurationComboViewer != null && daqConfigurationComboViewer.getCombo() != null) {
			if(daqConfigurationComboViewer.getCombo().getSelectionIndex() == -1) {
				setErrorMessage(DocometreMessages.NewProcessWizard_ErrorMessage);
				controlDecoration2.show();
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				controlDecoration2.hide();
				associatedDAQConfiguration = (IFile) daqConfigurationComboViewer.getStructuredSelection().getFirstElement();
				super.modifyHandler();
			}
		}
	}
	
	public IFile getAssociatedDAQConfiguration() {
		return associatedDAQConfiguration;
	}
	
	public Process getProcess() {
		String system = ResourceProperties.getSystemPersistentProperty(associatedDAQConfiguration); 
		if(system.equals(Activator.ADWIN_SYSTEM)) return new ADWinProcess();
		if(system.equals(Activator.ARDUINO_UNO_SYSTEM)) return new ArduinoUnoProcess();
//		if(system.equals(Activator.NI_600X_SYSTEM)) return null;
		return null;
	}

}
