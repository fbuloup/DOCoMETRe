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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.datamodel.BatchDataProcessing;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.datamodel.XYZChart;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.process.Script;

public class NewResourceWizard extends Wizard implements INewWizard, IPageChangedListener {
	
	public static int CREATE = 1;
	public static int MODIFY = 2;
	
	private ResourceType resourceType;
	protected NewResourceWizardPage newResourceWizardPage;
	private IContainer parentResource;
	private int mode;
	// Must not be null when modifying resource
	private IResource resource;
	private OrganizeSessionWizardPage organizeSessionWizardPage;

	public NewResourceWizard(ResourceType resourceType, IContainer parentResource, int mode) {
		this.resourceType = resourceType;
		this.parentResource = parentResource;
		this.mode = mode; 
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}
	
	// Return null when wizard is in creation mode
	public IResource getResource() {
		return resource;
	}
	
	public int getMode() {
		return mode;
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
	
	@Override
	public String getWindowTitle() {
		return getMode() == NewResourceWizard.CREATE ? DocometreMessages.NewResourceWizardWindowTitle : DocometreMessages.ModifyResourceTitle; 
	}
	
	@Override
	public void addPages() {
		if(resourceType.equals(ResourceType.EXPERIMENT)) newResourceWizardPage = new NewExperimentWizardPage();
		if(resourceType.equals(ResourceType.DACQ_CONFIGURATION)) newResourceWizardPage = new NewDACQConfigurationWizardPage();
		if(resourceType.equals(ResourceType.PROCESS)) newResourceWizardPage = new NewProcessWizardPage();
		if(resourceType.equals(ResourceType.SUBJECT)) newResourceWizardPage = new NewSubjectWizardPage();
		if(resourceType.equals(ResourceType.SESSION)) newResourceWizardPage = new NewSessionWizardPage();
		if(resourceType.equals(ResourceType.FOLDER)) newResourceWizardPage = new NewFolderWizardPage();
		if(resourceType.equals(ResourceType.TRIAL)) newResourceWizardPage = new NewTrialWizardPage();
		if(resourceType.equals(ResourceType.PARAMETERS)) newResourceWizardPage = new NewParametersFileWizardPage();
		if(resourceType.equals(ResourceType.DATA_PROCESSING)) newResourceWizardPage = new NewDataProcessingWizardPage();
		if(resourceType.equals(ResourceType.BATCH_DATA_PROCESSING)) newResourceWizardPage = new NewBatchDataProcessingWizardPage();
		if(resourceType.equals(ResourceType.XYCHART)) newResourceWizardPage = new NewXYZChartWizardPage(ResourceType.XYCHART);
		if(resourceType.equals(ResourceType.XYZCHART)) newResourceWizardPage = new NewXYZChartWizardPage(ResourceType.XYZCHART);
		addPage(newResourceWizardPage);
		if(resourceType.equals(ResourceType.SESSION)) {
			organizeSessionWizardPage = new OrganizeSessionWizardPage();
			addPage(organizeSessionWizardPage);
		}
	}

	public IContainer getProject() {
		return parentResource.getProject();
	}
	
	public IContainer getParentResource() {
		return parentResource;
	}
	
	@Override
	public boolean performFinish() {
		return true;
	}
	
	public String getResourceName() {
		return newResourceWizardPage.getResourceName();
	}

	public String getResourceDescription() {
		return newResourceWizardPage.getResourceDescription();
	}
	
	public int getTrialsNumber() {
		if(resourceType.equals(ResourceType.SESSION)) return ((NewSessionWizardPage)newResourceWizardPage).getTrialsNumber();
		return 0;
	}
	
	public String getSystem() {
		if(resourceType.equals(ResourceType.DACQ_CONFIGURATION)) return ((NewDACQConfigurationWizardPage)newResourceWizardPage).getSystem();
		return null;
	}
	
	public boolean isActive() {
		if(resourceType.equals(ResourceType.DACQ_CONFIGURATION)) return ((NewDACQConfigurationWizardPage)newResourceWizardPage).isDefault();
		return false;
	}
	
	public DACQConfiguration getDAQConfiguration() {
		if(resourceType.equals(ResourceType.DACQ_CONFIGURATION)) return ((NewDACQConfigurationWizardPage)newResourceWizardPage).getDAQConfiguration();
		return null;
	}
	
	public IFile getAssociatedDAQConfiguration() {
		 return ((NewProcessWizardPage)newResourceWizardPage).getAssociatedDAQConfiguration(); 
	}
	
	public Process getProcess() {
		if(resourceType.equals(ResourceType.PROCESS)) return ((NewProcessWizardPage)newResourceWizardPage).getProcess();
		return null;
	}
	
	public IResource getAssociatedProcess() {
		if(resourceType.equals(ResourceType.TRIAL)) return ((NewTrialWizardPage)newResourceWizardPage).getAssociatedProcess();
		return null;
	}
	
	public Script getDataProcessingScript() {
		return new Script();
	}
	
	public boolean getUsePrefix() {
		if(resourceType.equals(ResourceType.SESSION)) return ((NewSessionWizardPage)newResourceWizardPage).getUsePrefix();
		return false;
	}
	
	public String getPrefix() {
		if(!getUsePrefix()) return null;
		return ((NewSessionWizardPage)newResourceWizardPage).getPrefix();
	}
	
	public boolean useSessionNameSuffix() {
		if(resourceType.equals(ResourceType.SESSION)) return ((NewSessionWizardPage)newResourceWizardPage).useSessionNameSuffix();
		return false;
	}
	
	public boolean useTrialNumberSuffix() {
		if(resourceType.equals(ResourceType.SESSION)) return ((NewSessionWizardPage)newResourceWizardPage).useTrialNumberSuffix();
		return false;
	}
	
	public int getMinTrials() {
		if(resourceType.equals(ResourceType.SESSION)) return ((NewSessionWizardPage)newResourceWizardPage).getMinTrials();
		return 0;
	}

	public Object getBatchDataProcessing() {
		return new BatchDataProcessing();
	}
	
	public Object getXYChart() {
		return new XYChart();
	}
	
	public Object getXYZChart() {
		return new XYZChart();
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if(event.getSelectedPage() == organizeSessionWizardPage) {
			organizeSessionWizardPage.updateFocus();
		}
		
	}
	
 	
}
