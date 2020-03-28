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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartsConfigurationPage;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class ArduinoUnoDACQConfigurationEditor extends ResourceEditor implements PropertyObserver  {
	
	public static String ID = "Docometre.ArduinoUnoDACQConfigurationEditor";
	
	private PartListenerAdapter partListenerAdapter;
	private ArduinoUnoDACQGeneralConfigurationPage arduinoUnoGeneralConfigurationPage;
	private ChartsConfigurationPage arduinoChartsConfigurationPage;
	private ArduinoUnoVariablesPage variablesPage;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		IResource resource = ObjectsController.getResourceForObject(getDACQConfiguration());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		getDACQConfiguration().addObserver(this);
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == ArduinoUnoDACQConfigurationEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					getDACQConfiguration().removeObserver(ArduinoUnoDACQConfigurationEditor.this);
				}
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}
	
	private DACQConfiguration getDACQConfiguration() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (DACQConfiguration) resourceEditorInput.getObject();
	}

	@Override
	protected void addPages() {
		try {
			arduinoUnoGeneralConfigurationPage = new ArduinoUnoDACQGeneralConfigurationPage(this);
			addPage(arduinoUnoGeneralConfigurationPage);
			arduinoChartsConfigurationPage = new ChartsConfigurationPage(this);
			addPage(arduinoChartsConfigurationPage);
			variablesPage = new ArduinoUnoVariablesPage(this);
			addPage(variablesPage);
			updateModulesPages();
		} catch (PartInitException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ObjectsController.serialize(getDACQConfiguration());
		for (int i = 0; i < getPageCount(); i++) {
			((ModulePage)getPage(i)).commit();
		}
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub
		
	}
	
	private void updateModulesPages() {
		//Remove or create related form page
		Module[] modules = getDACQConfiguration().getModules(); 
		//Loop over modules to create freshly added
		for (Module localModule : modules) {
			//Find if localModule is held by an existing form page
			int id = System.identityHashCode(localModule);
			IFormPage moduleFormPage = findPage(Integer.toString(id));
			//If not, create a new form page and assign this module to it
			if(moduleFormPage == null) createNewFormPage(localModule, id);
		}
		//Loop over modules form pages
		//If one of these form pages hold a module that is not contained by the new modules : remove it
		int i = 0;
		while (i < getFormPageCount()) {
			IFormPage page = getPage(i);
			if(!(page instanceof ArduinoUnoDACQGeneralConfigurationPage) && !(page instanceof ArduinoUnoVariablesPage) && !(page instanceof ChartsConfigurationPage)) {
				ModulePage moduleFormPage = (ModulePage)getPage(i);
				Module localModule = moduleFormPage.getModule();
				if(!getDACQConfiguration().containModule(localModule)) {
					removePage(i);
					i = 0;
				} else i++;
			} else i++;
		}
	}

	private void createNewFormPage(Module module, int id) {
		try {
			FormPage newModuleFormPage = null;
			if(module instanceof ArduinoUnoAnInModule) newModuleFormPage = new ArduinoUnoAnInModulePage(this, Integer.toString(id), "Temporary Title", module);
			if(module instanceof ArduinoUnoAnOutModule) newModuleFormPage = new ArduinoUnoAnOutModulePage(this, Integer.toString(id), "Temporary Title", module);
			if(module instanceof ArduinoUnoDigInOutModule) newModuleFormPage = new ArduinoUnoDigInOutModulePage(this, Integer.toString(id), "Temporary Title", module);
			if(newModuleFormPage != null) addPage(newModuleFormPage);
		} catch (PartInitException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

}
