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
package fr.univamu.ism.docometre.editors;

import java.nio.file.Path;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

public class ResourceEditorInput implements IEditorInput {

	private Object object;
	private String tooltip = null;
	private HashSet<Object> editedObjects;

	public ResourceEditorInput(Object object) {
		this.object = object;
		if(!(object instanceof IResource)) ObjectsController.addHandle(object);
		editedObjects = new HashSet<>();
	}
	
	public void addEditedObject(Object object) {
		if(object == null) return;
		if(this.object.equals(object)) return;
		editedObjects.add(object);
	}
	
	public boolean removeEditedObject(Object object) {
		if(this.object.equals(object)) {
			this.object = null;
			Object[] objects = editedObjects.toArray();
			if(objects.length > 0) {
				this.object = objects[0];
				editedObjects.remove(this.object);
			} 
			return true;
		} else return editedObjects.remove(object);
	}
	
	public boolean canCloseEditor() {
		return object == null && editedObjects.size() == 0;
	}
	
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adapter) {
		return null;
	}

	public boolean exists() {
		return (object == null ? false:true);
	}

	public ImageDescriptor getImageDescriptor() {
		IResource resource = ObjectsController.getResourceForObject(object);
		if(resource == null && object instanceof IResource) resource = (IResource)object;
		if(ResourceType.isDACQConfiguration(resource)) return Activator.getImageDescriptor(IImageKeys.DACQ_CONFIGURATION_ICON);
		if(ResourceType.isProcess(resource)) return Activator.getImageDescriptor(IImageKeys.PROCESS_ICON);
		if(ResourceType.isLog(resource)) return Activator.getImageDescriptor(IImageKeys.DIARY_ICON);
		return null;
	}

	private IPath getResourceFullPath() {
		IResource resource = ObjectsController.getResourceForObject(object);
		if(resource == null && object instanceof IResource) resource = (IResource)object;
		return resource.getFullPath();
	}
	
	public String getName() {
		IPath path = getResourceFullPath();
		if(path == null) return null;
		return path.lastSegment();
	}

	public String getToolTipText() {
		if(tooltip != null) return tooltip;
		IPath path = getResourceFullPath();
		if(path == null) return null;
		return path.toOSString();
	}
	
	public Object getObject() {
		return object;
	}
	
	public void setObject(Object object) {
		this.object = object;
	}
	
	public IPersistableElement getPersistable() {
		return null;
	}
	
	public boolean isEditing(Object object) {
		if(this.object.equals(object)) return true;
		Object[] objects = editedObjects.toArray();
		for (Object localObject : objects) {
			if(localObject.equals(object)) return true;
		}
		if(this.object instanceof IFile && object instanceof IFile) {
			IFile localFile = (IFile)this.object;
			IFile file = (IFile)object;
			String pathLocalFile = "";
			if(localFile.getLocation() != null) pathLocalFile = localFile.getLocation().toPortableString();
			else pathLocalFile = localFile.getFullPath().toPortableString();
			boolean localFileIsCustomerFunction = FunctionFactory.isCustomerFunction(Path.of(pathLocalFile));
			String pathFile = "";
			if(file.getLocation() != null) pathFile = file.getLocation().toPortableString();
			else pathFile = file.getFullPath().toPortableString();
			boolean fileIsCustomerFunction = FunctionFactory.isCustomerFunction(Path.of(pathFile));
			if(localFileIsCustomerFunction && fileIsCustomerFunction) {
				if(pathLocalFile.equals(pathFile)) return true;
			}
		}
		return false;
	}

}
