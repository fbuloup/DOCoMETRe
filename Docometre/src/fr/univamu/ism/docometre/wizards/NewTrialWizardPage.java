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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class NewTrialWizardPage extends NewResourceWizardPage {

	private IResource selectedProcessResource;
	
	protected NewTrialWizardPage() {
		super(DocometreMessages.NewTrialWizard_PageName, ResourceType.TRIAL);
		setTitle(DocometreMessages.NewTrialWizard_PageTitle);
		setMessage(DocometreMessages.NewTrialWizard_PageMessage);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.NEW_RESOURCE_BANNER));
		setResourceNameRegularExpression("^" + DocometreMessages.Trial + "[1-9]\\d*(:[1-9]\\d*)?$");
		setResourceNameErrorMessage(DocometreMessages.NewTrialWizard_ErrorMessage);
		resourceType = ResourceType.TRIAL;
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite container = (Composite) getControl();
		
		CLabel associatedProcessLabel = new CLabel(container, SWT.NORMAL);
		associatedProcessLabel.setText(DocometreMessages.AssociatedProcessLabel);
		associatedProcessLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		ComboViewer processesComboViewer = new ComboViewer(container);
		processesComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		processesComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		processesComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(!(element instanceof IResource)) return "null";
				return GetResourceLabelDelegate.getLabel((IResource)element);
			}
		});
		IContainer subject = ((NewResourceWizard)getWizard()).getParentResource();
		IContainer project = subject.getProject();
		IResource[] processes = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, project);
		processesComboViewer.setInput(processes);
		processesComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedProcessResource = (IResource) processesComboViewer.getStructuredSelection().getFirstElement();
			}
		});
		
		setPageComplete(false);
	}
	
	public IResource getAssociatedProcess() {
		return selectedProcessResource;
	}
	
	
	
}
