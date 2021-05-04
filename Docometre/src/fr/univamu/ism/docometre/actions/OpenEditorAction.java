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
package fr.univamu.ism.docometre.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditor;
import fr.univamu.ism.docometre.analyse.editors.BatchDataProcessingEditor;
import fr.univamu.ism.docometre.analyse.editors.DataProcessEditor;
import fr.univamu.ism.docometre.analyse.editors.XYChartEditor;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor.ADWinDACQConfigurationEditor;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADWinProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor.ArduinoUnoDACQConfigurationEditor;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoProcessEditor;
import fr.univamu.ism.docometre.editors.DataEditor;
import fr.univamu.ism.docometre.editors.DiaryEditor;
import fr.univamu.ism.docometre.editors.ParametersEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class OpenEditorAction extends Action implements ISelectionListener, IWorkbenchAction {
	
	private static String ID = "OpenEditorAction";
	
	private IWorkbenchWindow window;
	private IResource[] resources;

	public OpenEditorAction(IWorkbenchWindow window) {
		setId(ID); //$NON-NLS-1$
		setActionDefinitionId(ID);
		this.window = window;
		window.getSelectionService().addSelectionListener(this);
		setEnabled(false);
		setText(DocometreMessages.OpenAction_Text);
		setToolTipText(DocometreMessages.OpenAction_Text);
	}
	
	@Override
	public void run() {
		if(resources == null) return;
		for (IResource resource : resources) {
			String system = null;
			String editorID = null;
			
			if(ResourceType.isDACQConfiguration(resource)) {
				system = ResourceProperties.getSystemPersistentProperty(resource);
				if(Activator.ADWIN_SYSTEM.equals(system)) editorID =  ADWinDACQConfigurationEditor.ID;
				if(Activator.ARDUINO_UNO_SYSTEM.equals(system)) editorID =  ArduinoUnoDACQConfigurationEditor.ID;
			}
			
			if(ResourceType.isProcess(resource)) {
				String associatedDAQFullPath = ResourceProperties.getAssociatedDACQConfigurationProperty(resource);
				if(associatedDAQFullPath != null) {
					IResource associatedDAQFile = ResourcesPlugin.getWorkspace().getRoot().findMember(associatedDAQFullPath);
					if(associatedDAQFile != null) {
						system = ResourceProperties.getSystemPersistentProperty(associatedDAQFile); 
						if(Activator.ADWIN_SYSTEM.equals(system)) editorID =  ADWinProcessEditor.ID;
						if(Activator.ARDUINO_UNO_SYSTEM.equals(system)) editorID =  ArduinoUnoProcessEditor.ID;
					} else Activator.logWarningMessage(DocometreMessages.OpenAction_ImpossibleToLoadProcessWhenNoAssociatedDAQ); 
				} else Activator.logWarningMessage(DocometreMessages.OpenAction_ImpossibleToLoadProcessWhenNoAssociatedDAQ); 
			}
			
			if(ResourceType.isLog(resource)) openEditor(resource, DiaryEditor.ID);
			
			if(ResourceType.isParameters(resource)) openEditor(resource, ParametersEditor.ID);
			
			if(ResourceType.isChannel(resource)) openEditor(resource, ChannelEditor.ID);
			
			if(ResourceType.isDataProcessing(resource)) editorID = DataProcessEditor.ID;
			
			if(ResourceType.isBatchDataProcessing(resource)) editorID = BatchDataProcessingEditor.ID;
			
			if(ResourceType.isXYChart(resource)) editorID = XYChartEditor.ID;
			
			if(ResourceType.isSamples(resource)) openEditor(resource, DataEditor.ID);
			else if(editorID != null) {
				Object object = ResourceProperties.getObjectSessionProperty(resource);
				if(object == null) {
					object = ObjectsController.deserialize((IFile)resource);
					ResourceProperties.setObjectSessionProperty(resource, object);
				}
				openEditor(object, editorID);
			}
			
		}
		
	}

	private void openEditor(Object object, String editorID) {
		try {
			IEditorReference[] editors = window.getActivePage().findEditors(null, editorID, IWorkbenchPage.MATCH_ID);
			IEditorReference foundEditorReference = null;
			for (IEditorReference editorReference : editors) {
				ResourceEditorInput editorInput = (ResourceEditorInput)editorReference.getEditorInput();
				if(editorInput.isEditing(object)) {
					foundEditorReference = editorReference;
					break;
				}
			}
			if(foundEditorReference != null) {
				window.getActivePage().activate(foundEditorReference.getPart(true));
			}
			else {
				window.getActivePage().openEditor(new ResourceEditorInput(object), editorID);
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void dispose() {
		window.getSelectionService().removeSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		setEnabled(false);
		if(part instanceof ExperimentsView || part instanceof SubjectsView) {
			resources = null;
			if (selection instanceof IStructuredSelection) {
				Object[] selectedObjects = ((IStructuredSelection) selection).toArray();
				ArrayList<IResource> files = new ArrayList<>();
				for (Object object : selectedObjects) {
					if(object instanceof IFile) {
						boolean canOpen = ResourceType.isDACQConfiguration((IResource) object) || ResourceType.isProcess((IResource) object);
						canOpen = canOpen || ResourceType.isLog((IResource) object);
						canOpen = canOpen || ResourceType.isParameters((IResource) object);
						canOpen = canOpen || ResourceType.isSamples((IResource) object);
						canOpen = canOpen || ResourceType.isChannel((IResource) object);
						canOpen = canOpen || ResourceType.isDataProcessing((IResource) object);
						canOpen = canOpen || ResourceType.isBatchDataProcessing((IResource) object);
						canOpen = canOpen || ResourceType.isXYChart((IResource) object);
						if(canOpen) files.add((IFile) object);
					}
				}
				if(files.size() > 0) resources = files.toArray(new IResource[files.size()]);
			}
			setEnabled(resources != null);
		}
	}
}
