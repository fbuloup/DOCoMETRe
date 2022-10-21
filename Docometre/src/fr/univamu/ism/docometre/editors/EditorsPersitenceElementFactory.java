package fr.univamu.ism.docometre.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;

public class EditorsPersitenceElementFactory implements IElementFactory {
	
	public static String ID = "Docometre.EditorsPersitenceElementFactory";
	public static String ResourceFullPath = "ResourceFullPath";

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
		return resourceEditorInput;
	}

}
