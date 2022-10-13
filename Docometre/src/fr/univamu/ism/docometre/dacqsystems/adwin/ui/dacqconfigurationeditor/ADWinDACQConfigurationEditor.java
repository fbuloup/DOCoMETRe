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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
//import org.eclipse.ui.operations.UndoRedoActionGroup;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCANModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232Module;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartsConfigurationPage;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class ADWinDACQConfigurationEditor extends ResourceEditor implements PropertyObserver {
	
	public static String ID = "Docometre.ADWinDAQConfigurationEditor";
	
	private ModulePage adwinGeneralConfigurationPage;
	private ModulePage variablesPage;
	private ChartsConfigurationPage adwinChartsConfigurationPage;
	
//	private DAQConfiguration daqConfiguration;
	private PartListenerAdapter partListenerAdapter;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
//		UndoRedoActionGroup undoRedoActionGroup = new UndoRedoActionGroup(site, getUndoContext(), true);
//		undoRedoActionGroup.fillActionBars(site.getActionBars());
		IResource resource = ObjectsController.getResourceForObject(getDACQConfiguration());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		getDACQConfiguration().addObserver(this);
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == ADWinDACQConfigurationEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					getDACQConfiguration().removeObserver(ADWinDACQConfigurationEditor.this);
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
			adwinGeneralConfigurationPage = new ADwinDACQGeneralConfigurationPage(this);
			addPage(adwinGeneralConfigurationPage);
			adwinChartsConfigurationPage = new ChartsConfigurationPage(this);
			addPage(adwinChartsConfigurationPage);
			variablesPage = new ADWinVariablesPage(this);
			addPage(variablesPage);
			updateModulesPages();
		} catch (PartInitException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void updateModulesPages() {
		//Remove or create related form page
		Module[] modules = getDACQConfiguration().getModules(); 
		ArrayList<Module> modulesObserved = new ArrayList<>();
		//Loop over modules to create freshly added
		for (Module localModule : modules) {
			//Find if localModule is held by an existing form page
			int id = System.identityHashCode(localModule);
			IFormPage moduleFormPage = findPage(Integer.toString(id));
			//If not, create a new form page and assign this module to it
			if(moduleFormPage == null) {
				createNewFormPage(localModule, id);
				if(localModule instanceof ADWinRS232Module) {
					modulesObserved.add(localModule);
				}
			}
			
		}
		// All module pages must observe modulesObserved
		for (Module localModule : modules) {
			if(!modulesObserved.contains(localModule)) {
				for (Module moduleObserved : modulesObserved) {
					localModule.addObserver(moduleObserved);
				}
			}
		}
		modulesObserved.clear();
		//Loop over modules form pages
		//If one of these form pages hold a module that is not contained by the new modules : remove it
		int i = 0;
		while (i < getFormPageCount()) {
			IFormPage page = getPage(i);
			if(!(page instanceof ADwinDACQGeneralConfigurationPage) && !(page instanceof ADWinVariablesPage) && !(page instanceof ChartsConfigurationPage)) {
				ModulePage moduleFormPage = (ModulePage)getPage(i);
				Module localModule = moduleFormPage.getModule();
				getDACQConfiguration().removeObserver(localModule);
				getDACQConfiguration().removeObserver(moduleFormPage);
				if(!getDACQConfiguration().containModule(localModule)) {
					removePage(i);
					if(localModule instanceof ADWinRS232Module) {
						for (Module module : modules) {
							module.removeObserver(localModule);
						}
					}
					i = 0;
				} else i++;
			} else i++;
		}
	}

	private void createNewFormPage(Module module, int id) {
		try {
			FormPage newModuleFormPage = null;
			if(module instanceof ADWinAnInModule) newModuleFormPage = new ADWinAnInModulePage(this, id, module);
			if(module instanceof ADWinAnOutModule) newModuleFormPage = new ADWinAnOutModulePage(this, id, module);
			if(module instanceof ADWinDigInOutModule) newModuleFormPage = new ADWinDigInOutModulePage(this, id, module);
			if(module instanceof ADWinCANModule) newModuleFormPage = new ADWinCANModulePage(this, id, module); 
			if(module instanceof ADWinRS232Module) newModuleFormPage = new ADWinRS232ModulePage(this, id, module); 
			addPage(newModuleFormPage);
		} catch (PartInitException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == DACQConfigurationProperties.UPDATE_MODULE) {
			updateModulesPages();
		}
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		ObjectsController.serialize(getDACQConfiguration());
		for (int i = 0; i < getPageCount(); i++) {
			((ModulePage)getPage(i)).commit();
		}
	}
	
}
