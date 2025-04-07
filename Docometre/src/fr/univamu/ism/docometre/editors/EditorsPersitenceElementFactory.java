package fr.univamu.ism.docometre.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.nswtchart.Window;

public class EditorsPersitenceElementFactory implements IElementFactory {
	
	public static String ID = "Docometre.EditorsPersitenceElementFactory";
	public static String ResourceFullPath = "ResourceFullPath";
	public static String otherResourcesFullPath = "otherResourcesFullPath";
	public static String dataEditorWindow = "dataEditorWindow";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String fullPath = memento.getString(ResourceFullPath);
		if(fullPath == null) return null;
		IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fullPath)) ;
		if(resource == null) return null;
		Object object = ResourceProperties.getObjectSessionProperty(resource);
		if(object == null) {
			object = ObjectsController.deserialize(resource);
			if(object !=  null) ResourceProperties.setObjectSessionProperty(resource, object);
			else object = resource;
		}
		ResourceEditorInput resourceEditorInput = new ResourceEditorInput(object);
		
		String otherFullPath = memento.getString(otherResourcesFullPath);
		if(otherFullPath != null && !"".equals(otherFullPath)) {
			String[] otherFullPathResource = otherFullPath.split(";");
			for (String resourceString : otherFullPathResource) {
				if(!"".equals(resourceString)) {
					resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceString)) ;
					resourceEditorInput.addEditedObject(resource);
				}
			}
		}
		
		String windowString = memento.getString(dataEditorWindow);
		if(windowString != null && !"".equals(windowString)) {
			Window window = Window.fromString(windowString); 
			resourceEditorInput.setWindow(window);
		}
		
		return resourceEditorInput;
	}

}
